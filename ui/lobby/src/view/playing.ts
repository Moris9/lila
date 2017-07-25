import { h } from 'snabbdom';
import { Chessground } from 'chessground';
import LobbyController from '../ctrl';

function timer(pov) {
  return h('time.timeago', {
    hook: {
      insert(vnode) {
        (vnode.elm as HTMLElement).setAttribute('datetime', '' + (Date.now() + pov.secondsLeft * 1000));
      }
    }
  });
}

export default function(ctrl: LobbyController) {
  return h('div#now_playing',
    ctrl.data.nowPlaying.map(function(pov) {
      return h('a.mini_board.is2d.' + pov.variant.key + (pov.isMyTurn ? '.my_turn' : ''), {
        key: pov.gameId,
        attrs: { href: '/' + pov.fullId }
      }, [
        h('div', [
          h('div.cg-board-wrap', {
            hook: {
              insert(vnode) {
                const lm = pov.lastMove;
                const config = {
                  coordinates: false,
                  drawable: { enabled: false },
                  resizable: false,
                  viewOnly: true,
                  orientation: pov.variant.key === 'racingKings' ? 'white' : pov.color,
                  fen: pov.fen,
                  lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]]
                };
                Chessground(vnode.elm as HTMLElement, config);
              }
            }
          }, [ h('div.cg-board') ])
        ]),
        h('span.meta', [
          pov.opponent.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', pov.opponent.ai) : pov.opponent.username,
          h('span.indicator',
            pov.isMyTurn ?
            (pov.secondsLeft ? timer(pov) : ctrl.trans('yourTurn')) :
            h('span', {
              hook: {
                insert(vnode) {
                  (vnode.elm as HTMLElement).innerHTML = '&nbsp;';
                }
              }
            }))
        ])
      ]);
    }));
}
