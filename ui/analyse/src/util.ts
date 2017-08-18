import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import { Attrs } from 'snabbdom/modules/attributes'
import { fixCrazySan } from 'chess';
import { AnalyseData } from './interfaces';

export function plyColor(ply: number): Color {
  return (ply % 2 === 0) ? 'white' : 'black';
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return {
    insert: vnode => {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        const res = f(e);
        if (res === false) {
          if (e.preventDefault) e.preventDefault();
          else e.returnValue = false; // ie
        }
        if (redraw) redraw();
        return res;
      });
    }
  };
}
export function bindSubmit(f: (e: Event) => any, redraw?: () => void): Hooks {
  return bind('submit', e => {
    e.preventDefault();
    return f(e);
  }, redraw);
}

export function readOnlyProp<A>(value: A): () => A {
  return function(): A {
    return value;
  };
}

export function dataIcon(icon: string): Attrs {
  return {
    'data-icon': icon
  };
}

export function iconTag(icon: string) {
  return h('i', { attrs: dataIcon(icon) });
}

export function plyToTurn(ply: number): number {
  return Math.floor((ply - 1) / 2) + 1;
}

export function synthetic(data: AnalyseData): boolean {
  return data.game.id === 'synthetic';
}

export function nodeFullName(node: Tree.Node) {
  if (node.san) return plyToTurn(node.ply) + (
    node.ply % 2 === 1 ? '.' : '...'
  ) + ' ' + fixCrazySan(node.san);
  return 'Initial position';
}

export function plural(noun: string, nb: number): string {
  return nb + ' ' + (nb === 1 ? noun : noun + 's');
}

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  const name = split.length == 1 ? split[0] : split[1];
  return name.toLowerCase();
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}

export function innerHTML<A>(a: A, toHtml: (a: A) => string): Hooks {
  return {
    insert(vnode) {
      (vnode.elm as HTMLElement).innerHTML = toHtml(a);
      vnode.data!.cachedA = a;
    },
    postpatch(old, vnode) {
      if (old.data!.cachedA !== a) {
        (vnode.elm as HTMLElement).innerHTML = toHtml(a);
      }
      vnode.data!.cachedA = a;
    }
  };
}

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
export function autolink(str: string, callback: (str: string) => string): string {
  const pattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
  return str.replace(pattern, function(_, space, url) {
    return "" + space + callback(url);
  });
}
