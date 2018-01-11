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
      xlong: nbScores > 80,
      active: ctrl.playerInfo.id === userId
    },
    hook: bind('click', _ => ctrl.showPlayerInfo(player), ctrl.redraw)
  }, [
    h('td.rank', player.withdraw ? h('i', {
      attrs: {
        'data-icon': 'Z',
        'title': ctrl.trans.noarg('pause')
      }
    }) : player.rank),
    h('td.player', renderPlayer(player, false, true)),
    h('td.sheet', player.sheet.scores.map(scoreTag)),
    h('td.total', [
      h('strong',
        player.sheet.fire && !ctrl.data.isFinished ?
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

function podiumStats(p, trans): VNode {
  const noarg = trans.noarg, nb = p.nb;
  return h('table.stats', [
    p.performance ? h('tr', [h('th', 'Performance'), h('td', p.performance)]) : null,
    h('tr', [h('th', noarg('gamesPlayed')), h('td', nb.game)]),
    ...(nb.game ? [
      h('tr', [h('th', noarg('winRate')), h('td', ratio2percent(nb.win / nb.game))]),
      h('tr', [h('th', noarg('berserkRate')), h('td', ratio2percent(nb.berserk / nb.game))])
    ] : [])
  ]);
}

function podiumPosition(p, pos, trans): VNode | undefined {
  if (p) return h('div.' + pos, [
    h('div.trophy'),
    podiumUsername(p),
    podiumStats(p, trans)
  ]);
}

let lastBody: MaybeVNodes | undefined;

export function podium(ctrl: TournamentController) {
  return h('div.podium', [
    podiumPosition(ctrl.data.podium[1], 'second', ctrl.trans),
    podiumPosition(ctrl.data.podium[0], 'first', ctrl.trans),
    podiumPosition(ctrl.data.podium[2], 'third', ctrl.trans)
  ]);
}

function preloadUserTips(el: HTMLElement) {
  window.lichess.powertip.manualUserIn(el);
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
          insert: vnode => preloadUserTips(vnode.elm as HTMLElement),
          update(_, vnode) { preloadUserTips(vnode.elm as HTMLElement) }
        }
      }, tableBody)
    ])
  ]);
}
