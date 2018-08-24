function parseFen($elem) {
  if (!$elem || !$elem.jquery) {
    $elem = $('.parse_fen');
  }
  $elem.each(function() {
    var $this = $(this).removeClass('parse_fen');
    var lm = $this.data('lastmove');
    var color = $this.data('color');
    var ground = $this.data('chessground');
    var config = {
      coordinates: false,
      resizable: false,
      drawable: { enabled: false, visible: false },
      viewOnly: true,
      fen: $this.data('fen'),
      lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]]
    };
    if (color) config.orientation = color;
    if (ground) ground.set(config);
    else {
      this.innerHTML = '<div class="cg-board-wrap"></div>';
      $this.data('chessground', Chessground(this.firstChild, config));
    }
  });
}
$(function() {
  var $featured = $('#featured_game');
  var board = function() {
    return $featured.find('.mini_board');
  };
  parseFen(board());
  if (!window.EventSource) {
    return;
  }
  var source = new EventSource($('body').data('stream-url'));
  source.addEventListener('message', function(e) {
    var data = JSON.parse(e.data);
    if (data.t == "featured") {
      $('#featured_game').html(data.d.html).find('a').attr('target', '_blank');
      parseFen(board());
    } else if (data.t == "fen") {
      parseFen(board().data("fen", data.d.fen).data("lastmove", data.d.lm));
    }
  }, false);
});
