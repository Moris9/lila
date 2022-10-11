import { Work } from './types';
import { Protocol } from './protocol';
import { Cache } from './cache';
import { readNdJson, CancellableStream } from 'common/ndjson';

export enum CevalState {
  Initial,
  Loading,
  Idle,
  Computing,
  Failed,
}

export interface CevalWorker {
  getState(): CevalState;
  start(work: Work): void;
  stop(): void;
  engineName(): string | undefined;
  destroy(): void;
}

export interface WebWorkerOpts {
  url: string;
}

export class WebWorker implements CevalWorker {
  private failed = false;
  private protocol = new Protocol();
  private worker: Worker | undefined;

  constructor(private opts: WebWorkerOpts) {}

  getState() {
    return !this.worker
      ? CevalState.Initial
      : this.failed
      ? CevalState.Failed
      : !this.protocol.engineName
      ? CevalState.Loading
      : this.protocol.isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  start(work: Work) {
    this.protocol.compute(work);

    if (!this.worker) {
      this.worker = new Worker(lichess.assetUrl(this.opts.url, { sameDomain: true }));
      this.worker.addEventListener('message', e => this.protocol.received(e.data), true);
      this.worker.addEventListener(
        'error',
        err => {
          console.error(err);
          this.failed = true;
        },
        true
      );
      this.protocol.connected(cmd => this.worker?.postMessage(cmd));
    }
  }

  stop() {
    this.protocol.compute(undefined);
  }

  engineName() {
    return this.protocol.engineName;
  }

  destroy() {
    this.worker?.terminate();
  }
}

export interface ThreadedWasmWorkerOpts {
  baseUrl: string;
  module: 'Stockfish' | 'StockfishMv';
  version: string;
  downloadProgress?: (mb: number) => void;
  wasmMemory: WebAssembly.Memory;
  cache?: Cache;
}

interface WasmStockfishModule {
  (opts: {
    wasmBinary?: ArrayBuffer;
    locateFile(path: string): string;
    wasmMemory: WebAssembly.Memory;
  }): Promise<Stockfish>;
}

interface Stockfish {
  addMessageListener(cb: (msg: string) => void): void;
  postMessage(msg: string): void;
}

declare global {
  interface Window {
    Stockfish?: WasmStockfishModule;
    StockfishMv?: WasmStockfishModule;
  }
}

export class ThreadedWasmWorker implements CevalWorker {
  private static failed: { Stockfish: boolean; StockfishMv: boolean } = { Stockfish: false, StockfishMv: false };
  private static protocols = { Stockfish: new Protocol(), StockfishMv: new Protocol() };
  private static sf: { Stockfish?: Promise<void>; StockfishMv?: Promise<void> } = {};

  constructor(private opts: ThreadedWasmWorkerOpts) {}

  getState() {
    return !ThreadedWasmWorker.sf[this.opts.module]
      ? CevalState.Initial
      : ThreadedWasmWorker.failed[this.opts.module]
      ? CevalState.Failed
      : !this.getProtocol().engineName
      ? CevalState.Loading
      : this.getProtocol().isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  private getProtocol(): Protocol {
    return ThreadedWasmWorker.protocols[this.opts.module];
  }

  private async boot(): Promise<Stockfish> {
    const version = this.opts.version;
    const cache = this.opts.cache;

    // Fetch WASM file ourselves, for caching and progress indication.
    let wasmBinary: ArrayBuffer | undefined;
    if (cache) {
      const wasmPath = this.opts.baseUrl + 'stockfish.wasm';
      try {
        const [found, data] = await cache.get(wasmPath, version);
        if (found) wasmBinary = data;
      } catch (e) {
        console.log('ceval: idb cache load failed:', e);
      }
      if (!wasmBinary) {
        wasmBinary = await new Promise((resolve, reject) => {
          const req = new XMLHttpRequest();
          req.open('GET', lichess.assetUrl(wasmPath, { version }), true);
          req.responseType = 'arraybuffer';
          req.onerror = event => reject(event);
          req.onprogress = event => this.opts.downloadProgress?.(event.loaded);
          req.onload = _ => {
            this.opts.downloadProgress?.(0);
            resolve(req.response);
          };
          req.send();
        });
      }
      try {
        await cache.set(wasmPath, version, wasmBinary);
      } catch (e) {
        console.log('ceval: idb cache store failed:', e);
      }
    }

    // Load Emscripten module.
    await lichess.loadScript(this.opts.baseUrl + 'stockfish.js', { version });
    const sf = await window[this.opts.module]!({
      wasmBinary,
      locateFile: (path: string) =>
        lichess.assetUrl(this.opts.baseUrl + path, { version, sameDomain: path.endsWith('.worker.js') }),
      wasmMemory: this.opts.wasmMemory,
    });

    const protocol = this.getProtocol();
    sf.addMessageListener(protocol.received.bind(protocol));
    protocol.connected(msg => sf.postMessage(msg));
    return sf;
  }

  start(work: Work) {
    this.getProtocol().compute(work);

    if (!ThreadedWasmWorker.sf[this.opts.module]) {
      ThreadedWasmWorker.sf[this.opts.module] = this.boot().then(
        () => {},
        err => {
          console.error(err);
          ThreadedWasmWorker.failed[this.opts.module] = true;
        }
      );
    }
  }

  stop() {
    this.getProtocol().compute(undefined);
  }

  engineName() {
    return this.getProtocol().engineName;
  }

  destroy() {
    // Terminated instances to not get freed reliably
    // (https://github.com/lichess-org/lila/issues/7334). So instead of
    // destroying, just stop instances and keep them around for reuse.
    this.stop();
  }
}

export interface ExternalEngine {
  id: string;
  name: string;
  variants: VariantKey[];
  maxThreads: number;
  maxHash: number;
  shallowDepth: number;
  deepDepth: number;
  clientSecret: string;
  officialStockfish?: boolean;
  endpoint: string;
}

interface ExternalEngineOutput {
  time: number;
  depth: number;
  nodes: number;
  pvs: {
    depth: number;
    cp?: number;
    mate?: number;
    moves: Uci[];
  }[];
}

export class ExternalWorker implements CevalWorker {
  private state = CevalState.Initial;
  private session = Math.random().toString(36).slice(2, 12);
  private stream: CancellableStream | undefined;

  constructor(private opts: ExternalEngine) {}

  getState() {
    return this.state;
  }

  start(work: Work) {
    stop();
    this.state = CevalState.Loading;

    const url = new URL(`${this.opts.endpoint}/api/external-engine/${this.opts.id}/analyse`);
    const deep = work.maxDepth >= 99;
    fetch(url.href, {
      method: 'post',
      cache: 'default',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit',
      body: JSON.stringify({
        clientSecret: this.opts.clientSecret,
        work: {
          sessionId: this.session,
          threads: work.threads,
          hash: work.hashSize || 16,
          deep,
          multiPv: work.multiPv,
          variant: work.variant,
          initialFen: work.initialFen,
          moves: work.moves,
        },
      }),
    }).then(
      res => {
        this.stream = readNdJson<ExternalEngineOutput>(line => {
          this.state = CevalState.Computing;
          work.emit({
            fen: work.currentFen,
            maxDepth: deep ? this.opts.deepDepth : this.opts.shallowDepth,
            depth: line.pvs[0]?.depth || 0,
            knps: line.nodes / Math.max(line.time, 1),
            nodes: line.nodes,
            cp: line.pvs[0]?.cp,
            mate: line.pvs[0]?.mate,
            millis: line.time,
            pvs: line.pvs,
          });
        })(res);
        this.stream.end.promise.then(() => (this.state = CevalState.Initial));
      },
      err => {
        console.error(err);
        this.state = CevalState.Failed;
      }
    );
  }

  stop() {
    this.stream?.cancel();
    this.state = CevalState.Initial;
  }

  engineName() {
    return this.opts.name;
  }

  destroy() {
    this.stop();
  }
}
