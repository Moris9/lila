import * as fs from 'node:fs';
import * as cps from 'node:child_process';
import * as path from 'node:path';
import * as fg from 'fast-glob';
import pluginCopy from 'rollup-plugin-copy';
import pluginReplace from '@rollup/plugin-replace';
import { LichessModule, env } from './main';

export const parseModules = async (): Promise<[Map<string, LichessModule>, Map<string, string[]>]> => {
  const moduleList: LichessModule[] = [];

  for await (const moduleDir of findModules(env.uiDir)) {
    if (moduleDir !== env.uiDir) moduleList.push(await parseModule(moduleDir));
  }
  const modules = new Map(moduleList.map(mod => [mod.name, mod]));
  const moduleDeps = new Map<string, string[]>();

  modules.forEach(mod => {
    const deplist: string[] = [];
    for (const dep in mod.pkg.dependencies) {
      if (modules.has(dep)) deplist.push(dep);
    }
    moduleDeps.set(mod.name, deplist);
    mod.bundle?.forEach(r => {
      if (r.output && ![mod.name, mod.moduleAlias].includes(r.output)) {
        moduleDeps.set(r.output, [mod.name, ...deplist]);
      }
    });
  });
  return [modules, moduleDeps];
};

export const replaceValues = {
  // from site/rollup.config.js
  __info__: JSON.stringify({
    date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
    commit: cps.execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim(),
    message: cps.execSync('git log -1 --pretty=%s', { encoding: 'utf-8' }).trim(),
  }),
};

export async function globArray(glob: string, { cwd = env.uiDir, abs = true } = {}): Promise<string[]> {
  const files: string[] = [];
  for await (const file of fg.stream(glob, { cwd: cwd, absolute: abs })) files.push(file.toString('utf8'));
  return files;
}

async function* findModules(dirpath: string): AsyncGenerator<string> {
  const walkFilter = ['@build', '@types', 'node_modules'];

  for (const file of await fs.promises.readdir(dirpath, { withFileTypes: true })) {
    if (walkFilter.includes(file.name) || file.name[0] === '.') continue;

    const fullpath = path.resolve(dirpath, file.name);
    if (file.isDirectory()) yield* findModules(fullpath);
    else if (file.name === 'package.json' && dirpath !== env.uiDir) yield dirpath;
  }
}

async function parseModule(moduleDir: string): Promise<LichessModule> {
  const pkg = JSON.parse(await fs.promises.readFile(path.resolve(moduleDir, 'package.json'), 'utf8'));
  const copyMeJsonPath = path.join(moduleDir, 'copy-me.json');
  const mod: LichessModule = {
    pkg: pkg,
    name: path.basename(moduleDir),
    root: moduleDir,
    build: [],
    hasTsconfig: fs.existsSync(path.join(moduleDir, 'tsconfig.json')),
    copyMe: fs.existsSync(copyMeJsonPath) && JSON.parse(await fs.promises.readFile(copyMeJsonPath, 'utf8')),
  };
  parseScripts(mod, 'scripts' in pkg ? pkg.scripts : {});
  const rollupConfigPath = path.join(mod.root, 'rollup.config.mjs');

  if (!fs.existsSync(rollupConfigPath)) return mod; // we're done

  mod.bundle = [];
  const rollupConfigStr = await fs.promises.readFile(rollupConfigPath, 'utf8');
  const rollupMatch = /rollupProject\((\{.+})\);/s.exec(rollupConfigStr);

  const rollupObj = parseObject(rollupMatch?.length === 2 ? rollupMatch[1] : null);

  for (const key in rollupObj) {
    const cfg = rollupObj[key];
    if (key === 'main' && cfg.output !== mod.name) mod.moduleAlias = cfg.output; // analyse is analysisBoard

    mod.bundle.push({
      hostMod: mod,
      input: cfg.input,
      output: cfg.output,
      importName: cfg.name ? cfg.name : cfg.output,
      plugins: cfg.plugins,
      onWarn: cfg.onwarn,
      isMain: key === 'main' || cfg.output === mod.name,
    });
  }
  return mod;
}

// context to interpret the objects in rollup.config.js
function parseObject(o: string | null) {
  const copy = pluginCopy;
  const replace = (_: any) =>
    pluginReplace({
      values: replaceValues,
      preventAssignment: true,
    });
  const suppressThisIsUndefined = (warning: any, warn: any) => warning.code !== 'THIS_IS_UNDEFINED' && warn(warning);
  const dirname = path.dirname;
  const execSync = (_: any, __: any) => ''; // can't execSync in an eval
  const require = { resolve: (mod: string) => path.resolve(env.nodeDir, mod) };
  copy, replace, dirname, require, suppressThisIsUndefined, execSync; // suppress unused
  return eval(`(${o})`) || {};
}

// TODO - just subtract yarn/rollup/tsc commands from script contents, don't overparse the string.
// many build steps need shell interpretation via exec/execSync which don't provide array arguments.
function tokenizeArgs(argstr: string): string[] {
  const args: string[] = [];
  const reducer = (a: any[], ch: string) => {
    if (ch !== ' ') return ch === "'" ? [a[0], !a[1]] : [a[0] + ch, a[1]];
    if (a[1]) return [a[0] + ' ', true];
    else if (a[0]) args.push(a[0]);
    return ['', false];
  };
  const lastOne = [...argstr].reduce(reducer, ['', false])[0];
  return lastOne ? [...args, lastOne] : args;
}

// go through package json scripts and get what we need from 'compile', 'dev', and deps
// if some other script is necessary, add it to buildScriptKeys
function parseScripts(module: LichessModule, pkgScripts: any) {
  const buildScriptKeys = ['deps', 'compile', 'dev'];

  for (const script in pkgScripts) {
    if (!buildScriptKeys.includes(script)) continue;
    pkgScripts[script].split(/&&/).forEach((cmd: string) => {
      // no need to support || in a script property yet, we don't even short circuit && properly
      const args = tokenizeArgs(cmd.trim());
      if (args[0] === 'tsc') {
        module.tscOptions = args.flatMap((arg: string) => (arg.startsWith('--') ? [arg.slice(2)] : []));
        // only support flag arguments
      } else if (!['$npm_execpath', 'yarn', 'rollup'].includes(args[0])) {
        module.build.push(args);
      }
    });
  }
}
