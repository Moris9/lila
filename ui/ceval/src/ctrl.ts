import { CevalCtrl, CevalOpts, Work, Step, Hovering, Started } from './types';

import Pool from './pool';
import { median } from './math';
import { prop, storedProp, throttle } from 'common';
import { povChances } from './winningChances';

const li = window.lichess;

export default function(opts: CevalOpts): CevalCtrl {

  const storageKey = function(k: string): string {
    return opts.storageKeyPrefix ? opts.storageKeyPrefix + '.' + k : k;
  };

  const pnaclSupported: boolean = !opts.failsafe && 'application/x-pnacl' in navigator.mimeTypes;
  const wasmSupported = typeof WebAssembly === 'object' && WebAssembly.validate(Uint8Array.of(0x0, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00));
  const minDepth = 6;
  const maxDepth = storedProp<number>(storageKey('ceval.max-depth'), 18);
  const multiPv = storedProp(storageKey('ceval.multipv'), opts.multiPvDefault || 1);
  const threads = storedProp(storageKey('ceval.threads'), Math.ceil((navigator.hardwareConcurrency || 1) / 2));
  const hashSize = storedProp(storageKey('ceval.hash-size'), 128);
  const infinite = storedProp('ceval.infinite', false);
  let curEval: Tree.ClientEval | null = null;
  const enableStorage = li.storage.make(storageKey('client-eval-enabled'));
  const allowed = prop(true);
  const enabled = prop(opts.possible && allowed() && enableStorage.get() == '1' && !document.hidden);
  let started: Started | false = false;
  let lastStarted: Started | false = false; // last started object (for going deeper even if stopped)
  const hovering = prop<Hovering | null>(null);
  const isDeeper = prop(false);

  const sfPath = '/assets/vendor/stockfish/stockfish';
  const pool = new Pool({
    asmjs: li.assetUrl(sfPath + '.js', {sameDomain: true}),
    pnacl: pnaclSupported && li.assetUrl(sfPath + '.nmf'),
    wasm: wasmSupported && li.assetUrl(sfPath + '.wasm.js', {sameDomain: true}),
    onCrash: opts.onCrash
  }, {
    minDepth,
    variant: opts.variant.key,
    threads: pnaclSupported && threads,
    hashSize: pnaclSupported && hashSize
  });

  // adjusts maxDepth based on nodes per second
  const npsRecorder = (function() {
    const values: number[] = [];
    const applies = function(ev: Tree.ClientEval) {
      return ev.knps && ev.depth >= 16 &&
        typeof ev.cp !== 'undefined' && Math.abs(ev.cp) < 500 &&
        (ev.fen.split(/\s/)[0].split(/[nbrqkp]/i).length - 1) >= 10;
    }
    return function(ev: Tree.ClientEval) {
      if (!applies(ev)) return;
      values.push(ev.knps);
      if (values.length >= 5) {
        var depth = 18,
          knps = median(values) || 0;
        if (knps > 100) depth = 19;
        if (knps > 150) depth = 20;
        if (knps > 250) depth = 21;
        if (knps > 500) depth = 22;
        if (knps > 1000) depth = 23;
        if (knps > 2000) depth = 24;
        if (knps > 3500) depth = 25;
        if (knps > 5000) depth = 26;
        if (knps > 7000) depth = 27;
        maxDepth(depth);
        if (values.length > 20) values.shift();
      }
    };
  })();

  let lastEmitFen: string | null = null;

  const onEmit = throttle(500, (ev: Tree.ClientEval, work: Work) => {
    sortPvsInPlace(ev.pvs, (work.ply % 2 === (work.threatMode ? 1 : 0)) ? 'white' : 'black');
    npsRecorder(ev);
    curEval = ev;
    opts.emit(ev, work);
    if (ev.fen !== lastEmitFen) {
      lastEmitFen = ev.fen;
      li.storage.set('ceval.fen', ev.fen);
    }
  });

  const effectiveMaxDepth = function(): number {
    return (isDeeper() || infinite()) ? 99 : parseInt(maxDepth());
  };

  const sortPvsInPlace = function(pvs: Tree.PvData[], color: Color) {
    pvs.sort(function(a, b) {
      return povChances(color, b) - povChances(color, a);
    });
  };

  const start = function(path: Tree.Path, steps: Step[], threatMode: boolean, deeper: boolean) {

    if (!enabled() || !opts.possible) return;

    isDeeper(deeper);
    const maxD = effectiveMaxDepth();

    const step = steps[steps.length - 1];

    const existing = threatMode ? step.threat : step.ceval;
    if (existing && existing.depth >= maxD) return;

    const work: Work = {
      initialFen: steps[0].fen,
      moves: [],
      currentFen: step.fen,
      path,
      ply: step.ply,
      maxDepth: maxD,
      multiPv: parseInt(multiPv()),
      threatMode,
      emit(ev: Tree.ClientEval) {
        if (enabled()) onEmit(ev, work);
      }
    };

    if (threatMode) {
      const c = step.ply % 2 === 1 ? 'w' : 'b';
      const fen = step.fen.replace(/ (w|b) /, ' ' + c + ' ');
      work.currentFen = fen;
      work.initialFen = fen;
    } else {
      // send fen after latest castling move and the following moves
      for (let i = 1; i < steps.length; i++) {
        let s = steps[i];
        if (s.san!.indexOf('O-O') === 0) {
          work.moves = [];
          work.initialFen = s.fen;
        } else work.moves.push(s.uci!);
      }
    }

    pool.start(work);

    started = {
      path,
      steps,
      threatMode
    };
  };

  function goDeeper() {
    const s = started || lastStarted;
    if (s) {
      stop();
      start(s.path, s.steps, s.threatMode, true);
    }
  };

  function stop() {
    if (!enabled() || !started) return;
    pool.stop();
    lastStarted = started;
    started = false;
  };

  // ask other tabs if a game is in progress
  if (enabled()) {
    li.storage.set('ceval.fen', 'start:' + Math.random());
    li.storage.make('round.ongoing').listen(_ => {
      enabled(false);
      opts.redraw();
    });
  }

  return {
    pnaclSupported,
    wasmSupported,
    start,
    stop,
    allowed,
    possible: opts.possible,
    enabled,
    multiPv,
    threads,
    hashSize,
    infinite,
    hovering,
    setHovering(fen: Fen, uci?: Uci) {
      hovering(uci ? {
        fen,
        uci
      } : null);
      opts.setAutoShapes();
    },
    toggle() {
      if (!opts.possible || !allowed()) return;
      stop();
      enabled(!enabled());
      if (document.visibilityState !== 'hidden')
        enableStorage.set(enabled() ? '1' : '0');
    },
    curDepth(): number {
      return curEval ? curEval.depth : 0;
    },
    effectiveMaxDepth,
    variant: opts.variant,
    isDeeper,
    goDeeper,
    canGoDeeper() {
      return !isDeeper() && !infinite() && !pool.isComputing();
    },
    isComputing() {
      return !!started && pool.isComputing();
    },
    engineName() {
      return pool.engineName();
    },
    destroy() { pool.destroy() },
    redraw: opts.redraw
  };
};
