import RacerCtrl from '../ctrl';
import { h } from 'snabbdom';
import { PlayerWithMoves } from '../interfaces';

// to [0,1]
type RelativeMoves = (moves: number) => number;

const trackHeight = 30;

export const renderRace = (ctrl: RacerCtrl) => {
  const players = ctrl.players();
  const minMoves = players.reduce((m, p) => (p.moves < m ? p.moves : m), 999) / 2;
  const maxMoves = players.reduce((m, p) => (p.moves > m ? p.moves : m), 30);
  const delta = maxMoves - minMoves;
  const relative: RelativeMoves = moves => (moves - minMoves) / delta;
  const first = players.reduce((f, p) => (f && f.moves > p.moves ? f : p));
  return h(
    'div.racer__race',
    {
      attrs: {
        style: `height:${players.length * trackHeight + 30}px`,
      },
    },
    h(
      'div.racer__race__tracks',
      players.map(renderTrack(relative, ctrl.player().name, first.moves ? first : undefined))
    )
  );
};

const renderTrack = (relative: RelativeMoves, myName: string, first?: PlayerWithMoves) => (
  player: PlayerWithMoves,
  index: number
) => {
  const isMe = player.name == myName;
  return h(
    'div.racer__race__track',
    {
      class: {
        'racer__race__track--me': isMe,
        'racer__race__track--first': first && first.name == player.name,
      },
    },
    [
      h('div.racer__race__score', player.moves),
      h(
        'div.racer__race__player',
        {
          attrs: {
            style: `transform:translateX(${relative(player.moves) * 95}%)`,
          },
        },
        [
          h(`div.racer__race__player__car.car-${index}`, [0]),
          h('span.racer__race__player__name', playerLink(player, isMe)),
        ]
      ),
    ]
  );
};

export const playerLink = (player: PlayerWithMoves, isMe: boolean) =>
  player.userId
    ? h(
        'a.user-link.ulpt',
        {
          attrs: { href: '/@/' + player.name },
        },
        player.title ? [h('span.utitle', player.title), player.name] : [player.name]
      )
    : h('anonymous', [player.name, isMe ? ' (you)' : undefined]);
