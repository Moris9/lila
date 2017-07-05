import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import TournamentController from '../ctrl';
import { player as renderPlayer, ratio2percent, bind } from './util';
import { MaybeVNodes } from '../interfaces';
import * as button from './button';
import * as pagination from '../pagination';

const scoreTagNames = ['score', 'streak', 'double'];

function scoreTag(s) {
  return h(scoreTagNames[(s[1] || 1) - 1], [Array.isArray(s) ? s[0] : s]);
}

function playerTr(ctrl: TournamentController, player) {
  const userId = player.name.toLowerCase(),
  nbScores = player.sheet.scores.length;
  return h('tr', {
    key: userId,
    class: {
      me: ctrl.opts.userId === userId,
      long: nbScores > 35,
      xlong: nbScores > 80
    },
    hook: bind('click', _ => ctrl.showPlayerInfo(player), ctrl.redraw)
  }, [
    h('td.rank', player.withdraw ? h('i', {
      attrs: {
        'data-icon': 'Z',
        'title': ctrl.trans.noarg('pause')
      }
    }) : player.rank),
    h('td.player', renderPlayer(player)),
    h('td.sheet', player.sheet.scores.map(scoreTag)),
    h('td.total', [
      h('strong',
        player.sheet.fire ?
        h('strong.is-gold', { attrs: { 'data-icon': 'Q' } }, player.sheet.total) :
        h('strong', player.sheet.total))
    ])
  ]);
}

function podiumUsername(p) {
  return h('a.text.ulpt.user_link', {
    attrs: { href: '/@/' + p.name }
  }, p.name);
}

function podiumStats(p): MaybeVNodes {
  let ratingDiff;
  if (p.ratingDiff === 0) ratingDiff = h('span', ' =');
  else if (p.ratingDiff > 0) ratingDiff = h('span.positive', {
    attrs: { 'data-icon': 'N' }
  }, p.ratingDiff);
  else if (p.ratingDiff < 0) ratingDiff = h('span.negative', {
    attrs: { 'data-icon': 'M' }
  }, '' + -p.ratingDiff);
  const nb = p.nb;
  return [
    h('span.rating.progress', [
      p.rating + p.ratingDiff,
      ratingDiff
    ]),
    h('table.stats', [
      h('tr', [h('th', 'Games played'), h('td', nb.game)]),
      ...(nb.game ? [
        h('tr', [h('th', 'Win rate'), h('td', ratio2percent(nb.win / nb.game))]),
        h('tr', [h('th', 'Berserk rate'), h('td', ratio2percent(nb.berserk / nb.game))])
      ] : []),
      p.performance ? h('tr', [h('th', 'Performance'), h('td', p.performance)]) : null
    ])
  ];
}

function podiumPosition(p, pos): VNode | undefined {
  if (p) return h('div.' + pos, [
    h('div.trophy'),
    podiumUsername(p),
    ...podiumStats(p)
  ]);
}

let lastBody: MaybeVNodes | undefined;

export function podium(ctrl: TournamentController) {
  return h('div.podium', [
    podiumPosition(ctrl.data.podium[1], 'second'),
    podiumPosition(ctrl.data.podium[0], 'first'),
    podiumPosition(ctrl.data.podium[2], 'third')
  ]);
}

export function standing(ctrl: TournamentController, pag, klass?: string) {
  const tableBody = pag.currentPageResults ?
    pag.currentPageResults.map(res => playerTr(ctrl, res)) : lastBody;
  if (pag.currentPageResults) lastBody = tableBody;
  return h('div.standing_wrap', [
    h('div.controls', [
      h('div.pager', pagination.renderPager(ctrl, pag)),
      button.joinWithdraw(ctrl)
    ]),
    h('table.slist.standing' + (klass ? '.' + klass : ''), {
      class: { loading: !pag.currentPageResults },
    }, [
      h('tbody', {
        hook: {
          insert: vnode => window.lichess.powertip.manualUserIn(vnode.elm as HTMLElement)
        }
      }, tableBody)
    ])
  ]);
}
