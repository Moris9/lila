import Lpv from 'lichess-pgn-viewer';

lichess.load.then(() => {
  const eta = $('.tutor__queued').data('eta');
  if (eta) setTimeout(lichess.reload, eta);

  $('.tutor-card--link').on('click', function (this: HTMLElement) {
    const href = this.dataset['href'];
    if (href) lichess.redirect(href);
  });

  $('.tutor__opening .lpv').each(function (this: HTMLElement) {
    Lpv(this, {
      pgn: this.dataset['pgn']!,
      initialPly: 'last',
      showMoves: false,
      showClocks: false,
      showPlayers: false,
      chessground: { coordinates: false },
      menu: {
        getPgn: {
          enabled: true,
          fileName: (this.dataset['title'] || this.dataset['pgn'] || 'opening').replace(' ', '_') + '.pgn',
        },
      },
    });
  });

  $('.tutor__waiting-game').each(function (this: HTMLElement) {
    const lpv = Lpv(this, {
      pgn: this.dataset['pgn']!,
      orientation: this.dataset['pov'] as Color,
      showMoves: false,
      showClocks: false,
      showPlayers: true,
      showControls: false,
      chessground: { coordinates: false },
      drawArrows: false,
    });
    const nbMoves = Array.from(lpv.game.moves.mainline()).length;
    setInterval(() => lpv.goTo('next', false), (60 / nbMoves) * 1000);
  });
});
