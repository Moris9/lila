import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ChatPlugin } from 'chat'
import { justIcon } from './util'

export interface TourStandingCtrl extends ChatPlugin {
  set(data: TourPlayer[]): void;
}

export interface TourPlayer {
  n: string; // name
  s: number; // score
  t?: string; // title
  f: boolean; // fire
  w: boolean; // withdraw
}

export function tourStandingCtrl(data: TourPlayer[], name: string): TourStandingCtrl {
  return {
    set(d: TourPlayer[]) { data = d },
    tab: {
      key: 'tourStanding',
      name: name
    },
    view(): VNode {
      return h('table.slist',
        h('tbody', data.map((p: TourPlayer, i: number) => {
          return h('tr.' + p.n, [
            h('td.name', [
              p.w ? h('span', justIcon('Z')) : h('span.rank', '' + (i + 1)),
              h('a.user_link.ulpt', {
                attrs: { href: `/@/${p.n}` }
              }, (p.t ? p.t + ' ' : '') + p.n)
            ]),
            h('td.total', p.f ? {
              class: { 'is-gold': true },
              attrs: { 'data-icon': 'Q' }
            } : {}, '' + p.s)
          ])
        }))
      );
    }
  };
}
