// these functions must remain on root namespace 

function customFunctionOnPgnGameLoad() {

  $('div.lichess_goodies a.rotate_board').click(function() {
    $('#GameBoard').toggleClass('flip');
    $('#player_links div:first').appendTo($('#player_links'));
    redrawBoardMarks();
    return false;
  });
  redrawBoardMarks();
  $("#GameButtons table").css('width', '514px').find("input").button();
  $("#autoplayButton").click(refreshButtonset);
  $("#GameBoard td").css('background', 'none');
}

function posToSquareId(pos) {
  if (pos.length != 2) return;
  var x = "abcdefgh".indexOf(pos[0]),
    y = 8 - parseInt(pos[1]);
  return "img_tcol" + x + "trow" + y;
}

function customFunctionOnMove() {
  var $comment = $('#GameLastComment');
  var moves = $comment.find('.commentMove').map(function() {
    return $(this).text();
  });
  var ids = $.map(moves, posToSquareId);
  $("#GameBoard img.bestmove").removeClass("bestmove");
  $.each(ids, function() {
    if (this) $("#" + this).addClass("bestmove");
  });
  refreshButtonset();
  var $chart = $("div.adv_chart");
  var chart = $chart.data("chart");
  if (chart) {
    try {
      var index = CurrentPly - 1;
      chart.setSelection([{
          row: index,
          column: 1
        }
      ]);
      var rows = $chart.data('rows');
      $comment.prepend($("<p>").html("White advantage: <strong>" + rows[index][1] + "</strong>"));
    } catch (e) {}
  }
  var turn = Math.round(CurrentPly / 2);
  var $gameText = $("#GameText");
  var $th = $gameText.find("th:eq(" + (turn - 1) + ")");
  if ($th.length) {
    var height = $th.height();
    var y = $th.position().top;
    if (y < height * 3) {
      $gameText.scrollTop($gameText.scrollTop() + y - height * 3);
    } else if (y > (512 - height * 4)) {
      $gameText.scrollTop($gameText.scrollTop() + y + height * 4 - 512);
    }
  }
  var fen = CurrentFEN();
  $('div.undergame_box a.fen_link').each(function() {
    $(this).attr('href', $(this).attr('href').replace(/fen=.*$/, "fen=" + fen));
  });
  // override normal round fen link
  $("a.view_fen").off('click').on('click', function() {
    alert(fen);
    return false;
  });
}

function redrawBoardMarks() {
  $.displayBoardMarks($('#GameBoard'), !$('#GameBoard').hasClass('flip'));
}

function refreshButtonset() {
  $("#autoplayButton").addClass("ui-button ui-widget ui-state-default ui-corner-all");
}
