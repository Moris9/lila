import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import AnalyseController from '../../../ctrl';
import { nodeFullName, bind } from '../../../util';
import { MaybeVNodes } from '../../../interfaces';
import { VNode } from 'snabbdom/vnode'
import { throttle } from 'common';
import { path as treePath } from 'tree';

export default function(ctrl: AnalyseController): VNode {

  const study = ctrl.study!,
  isMyMove = ctrl.turnColor() === ctrl.data.orientation,
  isCommented = !!(ctrl.node.comments || []).find(c => c.text.length > 2),
  hasVariation = ctrl.tree.nodeAtPath(treePath.init(ctrl.path)).children.length > 1;

  let content: MaybeVNodes;

  function commentButton(text: string = 'comment') {
    return h('a.button.thin', {
      hook: bind('click', () => {
        study.commentForm.open(study.vm.chapterId, ctrl.path, ctrl.node);
      }, ctrl.redraw),
    }, text);
  }

  if (!ctrl.path) content = [
    h('div.legend.todo', { class: { done: isCommented } }, [
      'Help the player find the initial move, with a ',
      commentButton(),
      '.'
    ])
  ];
  else if (ctrl.onMainline) {
    if (isMyMove) content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Comment the opponent move, and help the player find the next move, with a ',
        commentButton(),
        '.'
      ])
    ];
    else content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Congratulate the player for this correct move, with a ',
        commentButton(),
        '.'
      ]),
      hasVariation ? null : h('div.legend', {
        attrs: { 'data-icon': '' }
      }, 'Add variation moves to explain why specific other moves are wrong.'),
      renderDeviation(ctrl),
      renderHint(ctrl)
    ];
  }
  else content = [
    h('div.legend.todo', { class: { done: isCommented } }, [
      'Explain why this move is wrong in a ',
      commentButton(),
      '.'
    ]),
    h('div.legend',
      'Or promote it as the mainline if it is the right move.')
  ];

  return h('div.gamebook', {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.editor.css')
    }
  }, [
    h('div.editor', [
      h('span.title', [
        'Gamebook editor: ',
        nodeFullName(ctrl.node)
      ]),
      ...content
    ])
  ]);
}

function renderDeviation(ctrl: AnalyseController): VNode {
  const field = 'deviation';
  return h('div.deviation.todo', { class: { done: nodeGamebookValue(ctrl.node, field).length > 2 } }, [
    h('label', {
      attrs: { for: 'gamebook-deviation' }
    }, 'Or, when any other move is played:'),
    h('textarea#gamebook-deviation', {
      attrs: { placeholder: 'Explain why all other moves are wrong' },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

function renderHint(ctrl: AnalyseController): VNode {
  const field = 'hint';
  return h('div.hint', [
    h('label', {
      attrs: { for: 'gamebook-hint' }
    }, 'Optional, on-demand hint for the player:'),
    h('textarea#gamebook-hint', {
      attrs: { placeholder: 'Give the player a tip so they can find the right move' },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

const saveNode = throttle(500, false, (ctrl: AnalyseController, gamebook: Tree.Gamebook) => {
  ctrl.socket.send('setGamebook', {
    path: ctrl.path,
    ch: ctrl.study!.vm.chapterId,
    gamebook: gamebook
  });
  ctrl.redraw();
});

function nodeGamebookValue(node: Tree.Node, field: string): string {
  return (node.gamebook && node.gamebook[field]) || '';
}

function textareaHook(ctrl: AnalyseController, field: string): Hooks {
  const value = nodeGamebookValue(ctrl.node, field);
  return {
    insert(vnode: VNode) {
      const el = vnode.elm as HTMLInputElement;
      el.value = value;
      function onChange() {
        const node = ctrl.node;
        node.gamebook = node.gamebook || {};
        node.gamebook[field] = el.value.trim();
        saveNode(ctrl, node.gamebook, 50);
      }
      el.onkeyup = el.onpaste = onChange;
      vnode.data!.path = ctrl.path;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.path !== ctrl.path) (vnode.elm as HTMLInputElement).value = value;
      vnode.data!.path = ctrl.path;
    }
  }
}
