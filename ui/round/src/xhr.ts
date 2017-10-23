import RoundController from './ctrl';

const headers = {
  'Accept': 'application/vnd.lichess.v3+json'
};

export function reload(ctrl: RoundController) {
  return $.ajax({
    url: ctrl.data.url.round,
    headers
  }).fail(window.lichess.reload);
}

export function whatsNext(ctrl: RoundController) {
  return $.ajax({
    url: '/whats-next/' + ctrl.data.game.id + ctrl.data.player.id,
    headers
  });
}

export function challengeRematch(gameId: string) {
  return $.ajax({
    method: 'POST',
    url: '/challenge/rematch-of/' + gameId,
    headers
  });
}
