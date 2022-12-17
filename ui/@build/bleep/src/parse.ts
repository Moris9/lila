import * as fs from 'node:fs';
import * as path from 'node:path';
import * as fg from 'fast-glob';
import { LichessModule, env } from './main';

export const parseModules = async (): Promise<[Map<string, LichessModule>, Map<string, string[]>]> => {
  const moduleList: LichessModule[] = [];

  for (const dir of (await globArray('*/package.json')).map(pkg => path.dirname(pkg))) {
    moduleList.push(await parseModule(dir));
  }
  const modules = new Map(moduleList.map(mod => [mod.name, mod]));
  const moduleDeps = new Map<string, string[]>();

  modules.forEach(mod => {
    const deplist: string[] = [];
    for (const dep in mod.pkg.dependencies) {
      if (modules.has(dep)) deplist.push(dep);
    }
    moduleDeps.set(mod.name, deplist);
    // for package.jsons with multiple bundles, subsequent bundles depend on the first
    mod.bundle?.slice(1).forEach(r => {
      moduleDeps.set(r.output, [mod.name, ...deplist]);
    });
  });
  return [modules, moduleDeps];
};

export async function globArray(glob: string, { cwd = env.uiDir, abs = true } = {}): Promise<string[]> {
  const files: string[] = [];
  for await (const file of fg.stream(glob, { cwd: cwd, absolute: abs })) files.push(file.toString('utf8'));
  return files;
}

async function parseModule(moduleDir: string): Promise<LichessModule> {
  const pkg = JSON.parse(await fs.promises.readFile(path.join(moduleDir, 'package.json'), 'utf8'));
  const copyMeJsonPath = path.join(moduleDir, 'copy-me.json');
  const mod: LichessModule = {
    pkg: pkg,
    name: path.basename(moduleDir),
    root: moduleDir,
    pre: [],
    post: [],
    hasTsconfig: fs.existsSync(path.join(moduleDir, 'tsconfig.json')),
    copyMe: fs.existsSync(copyMeJsonPath) && JSON.parse(await fs.promises.readFile(copyMeJsonPath, 'utf8')),
  };
  parseScripts(mod, 'scripts' in pkg ? pkg.scripts : {});

  if ('org_lichess' in pkg && 'bundles' in pkg.org_lichess) {
    mod.bundle = Object.entries(pkg.org_lichess.bundles).map(x => ({ input: x[0], output: x[1] as string }));
  }
  return mod;
}

// TODO - just subtract yarn/rollup/tsc commands from script contents, don't overparse the string.
// build steps need shell interpretation via exec/execSync which don't provide array arguments.
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

// go through package json scripts and get what we need from 'compile', 'dev', and 'deps'
// if some other script is necessary, add it to buildScriptKeys
function parseScripts(module: LichessModule, pkgScripts: any) {
  const buildScriptKeys = ['deps', 'compile', 'dev'].concat(env.prod ? ['prod'] : []);

  for (const script in pkgScripts) {
    if (!buildScriptKeys.includes(script)) continue;
    pkgScripts[script].split(/&&/).forEach((cmd: string) => {
      // no need to support || in a script property yet, we don't even short circuit && properly
      const args = tokenizeArgs(cmd.trim());
      if (args[0] === 'tsc') {
        if (script == 'compile') module.tscOptions = args.slice(1);
      } else if (!['$npm_execpath', 'yarn', 'rollup'].includes(args[0])) {
        script == 'prod' ? module.post.push(args) : module.pre.push(args);
      }
    });
  }
}
