/// <reference types="types/lichess" />
import { h } from 'snabbdom'
import LobbyController from '../../ctrl';
import { bind, tds, perfIcons } from '../util';
import * as hookRepo from '../../hookRepo';
import { Hook } from '../../interfaces';

function renderHook(ctrl: LobbyController, hook: Hook) {
  return h('tr.hook.' + hook.action, {
    key: hook.id,
    class: { disabled: hook.disabled },
    attrs: {
      title: hook.disabled ? '' : (
        (hook.action === 'join') ? ctrl.trans('joinTheGame') + ' | ' + hook.perf : ctrl.trans('cancel')
      ),
      'data-id': hook.id
    },
  }, tds([
    h('span.is.is2.color-icon.' + (hook.c || 'random')),
    (hook.rating ? h('span.ulink.ulpt', {
      attrs: { 'data-href': '/@/' + hook.u }
    }, hook.u) : 'Anonymous'),
    (hook.rating ? hook.rating : '') + (hook.prov ? '?' : ''),
    hook.clock,
    h('span', [
      h('span.varicon', {
        attrs: { 'data-icon': perfIcons[hook.perf] }
      }),
      ctrl.trans(hook.ra ? 'rated' : 'casual')
    ])
  ]));
}

function isStandard(value) {
  return function(hook) {
    return (hook.variant === 'standard') === value;
  };
}

function isMine(hook) {
  return hook.action === 'cancel';
}

function isNotMine(hook) {
  return !isMine(hook);
}

export function toggle(ctrl: LobbyController) {
  return h('span.mode_toggle.hint--bottom', {
    key: 'set-mode-chart',
    attrs: { 'data-hint': ctrl.trans('graph') },
    hook: bind('mousedown', _ => ctrl.setMode('chart'), ctrl.redraw)
  }, [
    h('span.chart', {
      attrs: { 'data-icon': '9' }
    })
  ]);
}

export function render(ctrl: LobbyController, allHooks: Hook[]) {
  const mine = allHooks.find(isMine),
  max = mine ? 13 : 14,
  hooks = allHooks.slice(0, max),
  render = (hook: Hook) => renderHook(ctrl, hook),
  standards = hooks.filter(isNotMine).filter(isStandard(true));
  hookRepo.sort(ctrl, standards);
  const variants = hooks.filter(isNotMine).filter(isStandard(false))
    .slice(0, Math.max(0, max - standards.length - 1));
  hookRepo.sort(ctrl, variants);
  const renderedHooks = [
    ...standards.map(render),
    variants.length ? h('tr.variants', {
      key: 'variants',
    }, [
      h('td', {
        attrs: { colspan: 5 }
      }, '— ' + ctrl.trans('variant') + ' —')
    ]) : null,
    ...variants.map(render)
  ];
  if (mine) renderedHooks.unshift(render(mine));
  return h('table.table_wrap', {
    key: 'list'
  }, [
    h('thead',
      h('tr', [
        h('th'),
        h('th', ctrl.trans('player')),
        h('th', {
          class: {
            sortable: true,
            sort: ctrl.sort === 'rating'
          },
          hook: bind('click', _ => ctrl.setSort('rating'), ctrl.redraw)
        }, [h('i.is'), ctrl.trans('rating')]),
        h('th', {
          class: {
            sortable: true,
            sort: ctrl.sort === 'time'
          },
          hook: bind('click', _ => ctrl.setSort('time'), ctrl.redraw)
        }, [h('i.is'), ctrl.trans('time')]),
        h('th', ctrl.trans('mode'))
      ])
    ),
    h('tbody', {
      class: { stepping: ctrl.stepping },
      hook: bind('click', e => {
        let el = e.target as HTMLElement
          do {
            el = el.parentNode as HTMLElement;
            if (el.nodeName === 'TR') return ctrl.clickHook(el.getAttribute('data-id')!);
          }
          while (el.nodeName !== 'TABLE');
      }, ctrl.redraw)
    }, renderedHooks)
  ]);
}
