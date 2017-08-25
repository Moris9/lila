import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import { game } from 'game';
import AnalyseCtrl from '../ctrl';
import contextMenu from '../contextMenu';
import { MaybeVNodes, ConcealOf } from '../interfaces';
import { authorText as commentAuthorText } from '../study/studyComments';
import { enrichText, innerHTML } from '../util';
import { path as treePath } from 'tree';
import column from './columnView';
import inline from './inlineView';
import { empty, defined, dropThrottle, storedProp, StoredProp } from 'common';

export interface Ctx {
  ctrl: AnalyseCtrl;
  showComputer: boolean;
  showGlyphs: boolean;
  showEval: boolean;
  truncateComments: boolean;
}

export interface Opts {
  parentPath: Tree.Path;
  isMainline: boolean;
  inline?: Tree.Node;
  withIndex?: boolean;
  truncate?: number;
}

export interface NodeClasses {
  active: boolean;
  context_menu: boolean;
  current: boolean;
  nongame: boolean;
  [key: string]: boolean;
}

export type TreeViewKey = 'column' | 'inline';

export interface TreeView {
  get: StoredProp<TreeViewKey>;
  set(inline: boolean): void;
  toggle(): void;
  inline(): boolean;
}

export function ctrl(): TreeView {
  const value = storedProp<TreeViewKey>('treeView', 'column');
  function inline() {
    return value() === 'inline';
  }
  function set(i: boolean) {
    value(i ? 'inline' : 'column');
  }
  return {
    get: value,
    set,
    toggle() {
      set(!inline());
    },
    inline
  };
}


// entry point, dispatching to selected view
export function render(ctrl: AnalyseCtrl, concealOf?: ConcealOf): VNode {
  return ctrl.treeView.inline() ? inline(ctrl) : column(ctrl, concealOf);
}

export function nodeClasses(c: AnalyseCtrl, path: Tree.Path): NodeClasses {
  const current = (path === c.initialPath && game.playable(c.data)) || (
    c.retro && c.retro.current() && c.retro.current().prev.path === path
  );
  return {
    active: path === c.path,
    context_menu: path === c.contextMenuPath,
    current,
    nongame: !current && !!c.gamePath && treePath.contains(path, c.gamePath) && path !== c.gamePath
  };
}

export function renderInlineCommentsOf(ctx: Ctx, node: Tree.Node, rich?: boolean): MaybeVNodes {
  if (!ctx.ctrl.showComments || empty(node.comments)) return [];
  return node.comments!.map(comment => {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    const by = node.comments![1] ? h('span.by', commentAuthorText(comment.by)) : null;
    return rich ? h('comment', {
      hook: innerHTML(comment.text, text => enrichText(text, true))
    }) : h('comment', [
      by,
      truncateComment(comment.text, 300, ctx)
    ]);
  }).filter(nonEmpty);
}

export function truncateComment(text: string, len: number, ctx: Ctx) {
  return ctx.truncateComments && text.length > len ? text.slice(0, len - 10) + ' [...]' : text;
}

export function mainHook(ctrl: AnalyseCtrl): Hooks {
  return {
    insert: vnode => {
      const el = vnode.elm as HTMLElement;
      if (ctrl.path !== '') autoScroll(ctrl, el);
      el.oncontextmenu = (e: MouseEvent) => {
        const path = eventPath(e);
        if (path !== null) contextMenu(e, {
          path,
          root: ctrl
        });
        ctrl.redraw();
        return false;
      };
      el.addEventListener('mousedown', (e: MouseEvent) => {
        if (defined(e.button) && e.button !== 0) return; // only touch or left click
        const path = eventPath(e);
        if (path) ctrl.userJump(path);
        ctrl.redraw();
      });
    },
    postpatch: (_, vnode) => {
      if (ctrl.autoScrollRequested) {
        autoScroll(ctrl, vnode.elm as HTMLElement);
        ctrl.autoScrollRequested = false;
      }
    }
  };
}

export function retroLine(ctx: Ctx, node: Tree.Node, opts: Opts): VNode | undefined {
  return node.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(node, opts.parentPath) ?
  h('line', 'Learn from this mistake') : undefined;
}

function eventPath(e: MouseEvent): Tree.Path | null {
  return (e.target as HTMLElement).getAttribute('p') ||
  ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('p');
}

const scrollThrottle = dropThrottle(200);

export function autoScroll(ctrl: AnalyseCtrl, el: HTMLElement): void {
  scrollThrottle(() => {
    const cont = el.parentNode as HTMLElement;
    if (!cont) return;
    const target = el.querySelector('.active') as HTMLElement;
    if (!target) {
      cont.scrollTop = ctrl.path ? 99999 : 0;
      return;
    }
    cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
  });
}

export function nonEmpty(x: any): boolean {
  return !!x;
}
