import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { game, Player } from 'game';
import RoundController from '../ctrl';

function ratingDiff(player: Player): VNode | undefined {
  if (player.ratingDiff === 0) return h('span.rp.null', '±0');
  if (player.ratingDiff && player.ratingDiff > 0) return h('span.rp.up', '+' + player.ratingDiff);
  if (player.ratingDiff && player.ratingDiff < 0) return h('span.rp.down', '' + player.ratingDiff);
  return;
}

export function aiName(ctrl: RoundController, player: Player) {
  return ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai);
}

export function userHtml(ctrl: RoundController, player: Player) {
  const d = ctrl.data,
  user = player.user,
  perf = user ? user.perfs[d.game.perf] : null,
  rating = player.rating ? player.rating : (perf && perf.rating);
  if (user) {
    const connecting = !player.onGame && ctrl.firstSeconds && user.online;
    return h('div.username.user_link.' + player.color, {
      class: {
        online: player.onGame,
        offline: !player.onGame,
        long: user.username.length > 16,
        connecting
      }
    }, [
      h('i.line' + (user.patron ? '.patron' : ''), {
        attrs: {
          title: connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
        }
      }),
      h('a.text.ulpt', {
        attrs: {
          'data-pt-pos': 's',
          href: '/@/' + user.username,
          target: game.isPlayerPlaying(d) ? '_blank' : '_self'
        }
      }, user.title ? [h('span.title', user.title), ' ', user.username] : [user.username]),
      rating ? h('rating', rating + (player.provisional ? '?' : '')) : null,
      ratingDiff(player),
      player.engine ? h('span', {
        attrs: {
          'data-icon': 'j',
          title: ctrl.trans.noarg('thisPlayerUsesChessComputerAssistance')
        }
      }) : null
    ]);
  }
  const connecting = !player.onGame && ctrl.firstSeconds;
  return h('div.username.user_link', {
    class: {
      online: player.onGame,
      offline: !player.onGame,
      connecting
    }
  }, [
    h('i.line', {
      attrs: {
        title: connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
      }
    }),
    h('name', player.name || 'Anonymous')
  ]);
}

export function userTxt(ctrl: RoundController, player: Player) {
  if (player.user) {
    const perf = player.user.perfs[ctrl.data.game.perf],
    name = (player.user.title ? player.user.title + ' ' : '') + player.user.username,
    rating = player.rating ? player.rating : (perf ? perf.rating : null),
    showRating = rating ? ' (' + rating + (player.provisional ? '?' : '') + ')' : '';
    return name + showRating;
  } else if (player.ai) return aiName(ctrl, player)
  else return 'Anonymous';
}
