import { h } from 'snabbdom';
import { bind } from './util';
import LobbyController from '../ctrl';
import { MaybeVNodes, Tab } from '../interfaces';

function tab(ctrl: LobbyController, key: Tab, active: Tab, content: MaybeVNodes) {
  return h('a', {
    class: {
      active: key === active,
      glow: key !== active && key === 'pools' && !!ctrl.poolMember
    },
    hook: bind('mousedown', _ => ctrl.setTab(key), ctrl.redraw)
  }, content);
}

export default function(ctrl: LobbyController) {
  const myTurnPovsNb = ctrl.data.nowPlaying.filter(function(p) {
    return p.isMyTurn;
  }).length;
  const active = ctrl.tab;
  return [
    tab(ctrl, 'pools', active, [ctrl.trans.noarg('quickPairing')]),
    tab(ctrl, 'real_time', active, [ctrl.trans.noarg('lobby')]),
    tab(ctrl, 'seeks', active, [ctrl.trans.noarg('correspondence')]),
    (active === 'now_playing' || ctrl.data.nbNowPlaying > 0) ? tab(ctrl, 'now_playing', active, [
      ctrl.trans.plural('nbGamesInPlay', ctrl.data.nbNowPlaying),
      myTurnPovsNb > 0 ? h('span.unread', myTurnPovsNb) : null
    ]) : null
  ];
};
