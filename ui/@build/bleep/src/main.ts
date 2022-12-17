import * as ps from 'node:process';
import * as path from 'node:path';
import * as fs from 'node:fs';

import { build, postBuild } from './build';

export function main() {
  const configPath = path.resolve(__dirname, '../bleep.config.json');
  const config = fs.existsSync(configPath) ? JSON.parse(fs.readFileSync(configPath, 'utf8')) : undefined;
  init(path.resolve(__dirname, '../../../..'), config);

  if (ps.argv.includes('--help') || ps.argv.includes('-h')) {
    console.log(fs.readFileSync(path.resolve(__dirname, '../readme'), 'utf8'));
    return;
  }
  env.watch = ps.argv.includes('--watch') || ps.argv.includes('-w');
  env.prod = ps.argv.includes('--prod') || ps.argv.includes('-p');
  if (env.prod && env.watch) {
    console.log('You cannot watch prod builds! Think of the children');
    return;
  }
  build(ps.argv.slice(2).filter(x => !x.startsWith('-')));
}

export interface BleepOpts {
  sass?: boolean; // use dart-sass rather than gulp for scss, default = true
  esbuild?: boolean; // use esbuild rather than rollup, default = true
  tsc?: boolean; // use tsc for type checking, default = true
  log?: {
    heap?: boolean; // show node rss in log statements, default = false
    time?: boolean; // show time in log statements, default = true
    ctx?: boolean; // show context (tsc, rollup, etc), default = true
    color?: any; // set false to disable colors, otherwise leave undefined
  };
}

export interface LichessModule {
  root: string; // absolute path to package.json parentdir (module root)
  name: string; // dirname of module root
  pkg: any; // the entire package.json object
  pre: string[][]; // pre-bundle build steps from package.json scripts
  post: string[][]; // post-bundle build steps from package.json scripts
  hasTsconfig?: boolean; // fileExists('tsconfig.json')
  tscOptions?: string[]; // options from tsc/compile script in package json
  bundle?: LichessBundle[]; // targets from rollup.config.mjs
  copyMe?: Copy[]; // pre-bundle filesystem copies triggered by a copy-me.json file at
  // module root. see ui/site/copy-me.json
}

export interface Copy {
  // same as copy -rf lila/node_modules/${src} lila/public/${dest}
  src: string | string[];
  dest: string;
}

export interface LichessBundle {
  //hostMod: LichessModule;
  input: string; // abs path to source
  output: string; // abs path to bundle destination
  //importName?: string; // might as well be isAnalysisBoard boolean
  //isMain: boolean; // false for plugin bundles
}

export function init(root: string, opts?: BleepOpts) {
  env.rootDir = root;
  env.opts = opts ? opts : { log: {} };
  if (env.opts.log && env.opts.log.color !== false) {
    env.opts.log.color = {
      bleep: 'green',
      sass: 'magenta',
      tsc: 'yellow',
      esbuild: 'blue',
    };
  }
  if (env.opts.sass === false) env.exitCode.set('sass', false);
  if (env.opts.tsc === false) env.exitCode.set('tsc', false);
  if (env.opts.esbuild === false) env.exitCode.set('esbuild', false);
}

export const lines = (s: string): string[] => s.split(/[\n\r\f]+/).filter(x => x.trim());

const colorLines = (text: string, code: string) =>
  lines(text)
    .map(t => (env.opts?.log?.color !== false ? escape(t, code) : t))
    .join('\n');

export const colors = {
  red: (text: string): string => colorLines(text, codes.red),
  green: (text: string): string => colorLines(text, codes.green),
  yellow: (text: string): string => colorLines(text, codes.yellow),
  blue: (text: string): string => colorLines(text, codes.blue),
  magenta: (text: string): string => colorLines(text, codes.magenta),
  cyan: (text: string): string => colorLines(text, codes.cyan),
  grey: (text: string): string => colorLines(text, codes.grey),
  black: (text: string): string => colorLines(text, codes.black),
  error: (text: string): string => colorLines(text, codes.error),
  warn: (text: string): string => colorLines(text, codes.warn),
  good: (text: string): string => colorLines(text, codes.green + ';1'),
  cyanBold: (text: string): string => colorLines(text, codes.cyan + ';1'),
};

class Env {
  rootDir: string; // absolute path to lila project root
  opts: BleepOpts; // configure logging mostly
  watch = false;
  prod = false;
  exitCode = new Map<'sass' | 'tsc' | 'esbuild', number | false>();
  startTime: number | undefined = Date.now();

  get sass(): boolean {
    return this.exitCode.get('sass') !== false;
  }
  get tsc(): boolean {
    return this.exitCode.get('tsc') !== false;
  }
  get esbuild(): boolean {
    return this.exitCode.get('esbuild') !== false;
  }
  get uiDir(): string {
    return path.join(this.rootDir, 'ui');
  }
  get nodeDir(): string {
    return path.join(this.rootDir, 'node_modules');
  }
  get outDir(): string {
    return path.join(this.rootDir, 'public');
  }
  get cssDir(): string {
    return path.join(this.outDir, 'css');
  }
  get jsDir(): string {
    return path.join(this.outDir, 'compiled');
  }
  get bleepDir(): string {
    return path.join(this.uiDir, '@build', 'bleep');
  }
  get tsconfigDir(): string {
    return path.join(this.bleepDir, '.tsconfig');
  }
  warn(d: any, ctx = 'bleep') {
    this.log(d, { ctx: ctx, warn: true });
  }
  error(d: any, ctx = 'bleep') {
    this.log(d, { ctx: ctx, error: true });
  }
  good(ctx = 'bleep') {
    this.log(colors.good('No errors') + env.watch ? ` - ${colors.grey('Watching')}...` : '', { ctx: ctx });
  }
  log(d: any, { ctx = 'bleep', error = false, warn = false } = {}) {
    let text: string =
      typeof d === 'string'
        ? d
        : d instanceof Buffer
        ? d.toString('utf8')
        : Array.isArray(d)
        ? d.join('\n')
        : JSON.stringify(d, undefined, 2);

    const show = this.opts.log;
    const esc = show?.color !== false ? escape : (text: string, _: any) => text;
    const rss = Math.round(ps.memoryUsage.rss() / (1000 * 1000));

    if (show?.color === false) text = stripColorEscapes(text);

    const prefix = (
      (show?.time === false ? '' : prettyTime()) +
      (!ctx || show?.ctx === false ? '' : `[${hasColor(ctx) ? ctx : esc(ctx, colorForCtx(ctx, show?.color))}] `) +
      (show?.heap === true ? `${esc(rss + ' MB', rss > 5000 ? codes.red : codes.grey)} ` : '')
    ).trim();

    lines(text).forEach(line =>
      console.log(
        `${prefix ? prefix + ' - ' : ''}${error ? esc(line, codes.error) : warn ? esc(line, codes.warn) : line}`
      )
    );
  }
  done(code: number, ctx: 'sass' | 'tsc' | 'esbuild'): void {
    this.exitCode.set(ctx, code);
    const err = [...this.exitCode.values()].find(x => x);
    const allDone = this.exitCode.size === 3;

    this.log(`${code === 0 ? 'Done' : colors.red('Failed')}` + (this.watch ? ` - ${colors.grey('Watching')}...` : ''), {
      ctx: ctx,
    });

    if (allDone) {
      if (!err) postBuild();
      if (this.startTime && !err) this.log(`Done in ${colors.green((Date.now() - this.startTime) / 1000 + '')}s`);
      this.startTime = undefined; // it's pointless to time subsequent builds, they are too fast
      if (!env.watch) {
        process.exitCode = err || 0;
      }
    }
  }
}

export const env = new Env();

export const codes: any = {
  black: '30',
  red: '31',
  green: '32',
  yellow: '33',
  blue: '34',
  magenta: '35',
  cyan: '36',
  grey: '90',
  error: '31;41',
  warn: '33;43',
};

const colorForCtx = (ctx: string, color: any): string =>
  color && ctx in color && color[ctx] in codes ? codes[color[ctx]] : codes.grey;

const escape = (text: string, code: string): string => `\x1b[${code}m${stripColorEscapes(text)}\x1b[0m`;

function hasColor(text: string): boolean {
  // eslint-disable-next-line no-control-regex
  return text.match(/\x1b\[[0-9;]*m/) !== null;
}
const pad2 = (n: number) => (n < 10 ? `0${n}` : `${n}`);

function stripColorEscapes(text: string) {
  // eslint-disable-next-line no-control-regex
  return text.replace(/\x1b\[[0-9;]*m/, '');
}

export const errorMark = colors.red('✘ ') + colors.error('[ERROR]');

function prettyTime() {
  const now = new Date();
  return `${pad2(now.getHours())}:${pad2(now.getMinutes())}:${pad2(now.getSeconds())} `;
}

main();
