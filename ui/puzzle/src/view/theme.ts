import { bind, dataIcon } from '../util';
import { Controller } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

export default function theme(ctrl: Controller): VNode {
  return h('div.puzzle__side__theme', [
    h('a', { attrs: { href: '/training/themes' } }, h('h2', ['« ', ctrl.getData().theme.name])),
    h('p', ctrl.getData().theme.desc),
    ctrl.vm.mode != 'view' || ctrl.autoNexting() ? null : editor(ctrl)
  ]);
}

const editor = (ctrl: Controller): VNode => {
  const data = ctrl.getData(),
    votedThemes = ctrl.vm.round?.themes || {};
  const visibleThemes: string[] = data.puzzle.themes.concat(
    Object.keys(votedThemes).filter(t => votedThemes[t] && !data.puzzle.themes.includes(t))
  ).sort()
  return h('div.puzzle__themes', [
    h('div.puzzle__themes_list', {
      hook: bind('click', e => {
        const target = e.target as HTMLElement;
        const theme = target.getAttribute('data-theme');
        if (theme) ctrl.voteTheme(theme, target.classList.contains('vote-up'));
      })
    }, visibleThemes.map(key =>
      h('div.puzzle__themes__list__entry', {
        class: {
          strike: votedThemes[key] === false
        }
      }, [
        h('a', {
          attrs: {
            href: `/training/${key}`,
            title: ctrl.trans.noarg(`${key}Description`)
          }
        }, ctrl.trans.noarg(key)),
        !ctrl.allThemes ? null : h('div.puzzle__themes__votes',
          ctrl.allThemes.static.has(key) ? [
            h('div.puzzle__themes__lock', h('i', {
              attrs: dataIcon('a')
            }))
          ] : [
              h('span.puzzle__themes__vote.vote-up', {
                class: { active: votedThemes[key] },
                attrs: { 'data-theme': key }
              }),
              h('span.puzzle__themes__vote.vote-down', {
                class: { active: votedThemes[key] === false },
                attrs: { 'data-theme': key }
              })
            ])
      ])
    )),
    ctrl.allThemes ? h('select.puzzle__themes__selector', {
      hook: {
        ...bind('change', e => {
          const theme = (e.target as HTMLInputElement).value;
          if (theme) ctrl.voteTheme(theme, true);
          setTimeout(() => {
            ((e.target as HTMLInputElement).parentNode as HTMLSelectElement).value = '';
          }, 500);
        }),
        postpatch(_, vnode) {
          (vnode.elm as HTMLSelectElement).value = '';
        }
      }
    }, [
      h('option', {
        attrs: { value: '', selected: true }
      }, 'Add another theme'),
      ...ctrl.allThemes.dynamic.filter(t => !votedThemes[t]).map(theme =>
        h('option', {
          attrs: {
            value: theme,
            title: ctrl.trans.noarg(`${theme}Description`)
          },
        }, ctrl.trans.noarg(theme))
      )
    ]) : null
  ]);
}
