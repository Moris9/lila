import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import TournamentController from './ctrl';
import { bind, onInsert } from './view/util';

export function button(ctrl: TournamentController): VNode {
  return h('button.fbt', {
    class: { active: ctrl.searching },
    attrs: {
      'data-icon': ctrl.searching ? 'L' : 'y',
      title: 'Search tournament players'
    },
    hook: bind('mousedown', ctrl.toggleSearch, ctrl.redraw)
  });
}

export function input(ctrl: TournamentController): VNode {
  return h('div.search',
    h('input', {
      hook: onInsert((el: HTMLInputElement) =>
        window.lichess.userComplete().then(uac => {
          uac({
            input: el,
            tour: ctrl.data.id,
            tag: 'span',
            focus: true,
            select(v) {
              ctrl.jumpToPageOf(v.id);
              ctrl.redraw();
              return '';
            }
          });
          el.focus();
        })
      )
    })
  );
}
