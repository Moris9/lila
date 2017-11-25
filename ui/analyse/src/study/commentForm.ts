import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { nodeFullName, bind } from '../util';
import { prop, throttle, Prop } from 'common';
import AnalyseCtrl from '../ctrl';

interface Current {
  chapterId: string;
  path: Tree.Path;
  node: Tree.Node;
}

interface CommentForm {
  root: AnalyseCtrl;
  current: Prop<Current | null>;
  focus: Prop<boolean>;
  opening: Prop<boolean>;
  open(chapterId: string, path: Tree.Path, node: Tree.Node): void;
  close(): void;
  submit(text: string): void;
  onSetPath(path: Tree.Path, node: Tree.Node, myself?: boolean): void;
  redraw(): void;
  toggle(chapterId: string, path: Tree.Path, node: Tree.Node): void;
  delete(chapterId: string, path: Tree.Path, id: string): void;
}

export function ctrl(root: AnalyseCtrl): CommentForm {

  const current = prop<Current | null>(null),
  focus = prop(false),
  opening = prop(false);

  function submit(text: string): void {
    if (!current()) return;
    doSubmit(text);
  };

  const doSubmit = throttle(500, (text: string) => {
    const cur = current();
    if (cur) root.study!.makeChange('setComment', {
      ch: cur.chapterId,
      path: cur.path,
      text
    });
  });

  function open(chapterId: string, path: Tree.Path, node: Tree.Node): void {
    opening(true);
    current({
      chapterId,
      path,
      node
    });
    root.userJump(path);
  };

  function close() {
    current(null);
  }

  return {
    root,
    current,
    focus,
    opening,
    open,
    close,
    submit,
    onSetPath(path: Tree.Path, node: Tree.Node, myself: boolean = false): void {
      const cur = current();
      console.log(node.san, cur, path, myself);
      if (cur && cur.path !== path && (myself || !focus())) {
      console.log('set!');
        cur.path = path;
        cur.node = node;
        root.redraw();
      }
    },
    redraw: root.redraw,
    toggle(chapterId: string, path: Tree.Path, node: Tree.Node) {
      if (current()) close();
      else open(chapterId, path, node);
    },
    delete(chapterId: string, path: Tree.Path, id: string) {
      root.study!.makeChange('deleteComment', {
        ch: chapterId,
        path,
        id
      });
    }
  };
}

export function view(ctrl: CommentForm): VNode | undefined {

  const current = ctrl.current();
  if (!current) return;

  function setupTextarea(vnode: VNode) {
    const el = vnode.elm as HTMLInputElement,
    mine = (current!.node.comments || []).find(function(c: any) {
      return c.by && c.by.id && c.by.id === ctrl.root.opts.userId;
    });
    el.value = mine ? mine.text : '';
    if (ctrl.opening() || ctrl.focus()) el.focus();
    ctrl.opening(false);
  }

  return h('div.study_comment_form.underboard_form', {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/material.form.css')
    }
  }, [
    h('p.title', [
      h('button.button.frameless.close', {
        attrs: {
          'data-icon': 'L',
          title: 'Close'
        },
        hook: bind('click', ctrl.close, ctrl.redraw)
      }),
      'Commenting position after ',
      h('button.button', {
        class: { active: ctrl.root.path === current.path },
        hook: bind('click', _ => ctrl.root.userJump(current.path), ctrl.redraw)
      }, nodeFullName(current.node))
    ]),
    h('form.material.form', [
      h('div.form-group', [
        h('textarea#comment-text', {
          hook: {
            insert(vnode) {
              setupTextarea(vnode);
              const el = vnode.elm as HTMLInputElement;
              function onChange() {
                setTimeout(function() {
                  ctrl.submit(el.value);
                }, 50);
              }
              el.onkeyup = el.onpaste = onChange;
              el.onfocus = function() {
                ctrl.focus(true);
                ctrl.redraw();
              };
              el.onblur = function() {
                ctrl.focus(false);
                ctrl.redraw();
              };
              const trap = window.Mousetrap(el);
              trap.bind(['ctrl+enter', 'command+enter'], ctrl.close);
              trap.stopCallback = () => false;
              vnode.data!.trap = trap;
              vnode.data!.path = current.path;
            },
            postpatch(old, vnode) {
              if (old.data!.path !== current.path) setupTextarea(vnode);
              vnode.data!.path = current.path;
              vnode.data!.trap = old.data!.trap;
            },
            destroy: vnode => vnode.data!.trap.reset()
          }
        }),
        h('i.bar')
      ])
    ])
  ]);
}
