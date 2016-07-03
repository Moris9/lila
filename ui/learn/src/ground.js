var chessground = require('chessground');
var partial = chessground.util.partial;
var raf = chessground.util.requestAnimationFrame;
var util = require('./util');

var cg = new chessground.controller();

module.exports = {
  instance: cg,
  set: function(opts) {
    var check = opts.chess.instance.in_check();
    cg.set({
      fen: opts.chess.fen(),
      lastMove: null,
      orientation: opts.orientation,
      coordinates: true,
      squareKey: true,
      turnColor: opts.chess.color(),
      check: check,
      movable: {
        free: false,
        color: opts.chess.color(),
        dests: opts.chess.dests({
          legal: false
        })
      },
      events: {
        move: opts.onMove
      },
      items: opts.items,
      premovable: {
        enabled: true
      },
      drawable: {
        enabled: true,
        eraseOnClick: true
      },
      highlight: {
        lastMove: true,
        dragOver: true
      },
      animation: {
        enabled: false, // prevent piece animation during transition
        duration: 200
      },
      disableContextMenu: true
    });
    setTimeout(function() {
      cg.set({
        animation: {
          enabled: true
        }
      });
    }, 200);
    if (opts.shapes) cg.setShapes(opts.shapes);
    return cg;
  },
  stop: cg.stop,
  color: function(color, dests) {
    cg.set({
      turnColor: color,
      movable: {
        color: color,
        dests: dests
      }
    });
  },
  fen: function(fen, color, dests, lastMove) {
    var config = {
      turnColor: color,
      fen: fen,
      movable: {
        color: color,
        dests: dests
      }
    };
    if (lastMove) config.lastMove = lastMove;
    cg.set(config);
  },
  check: function(chess) {
    var checks = chess.checks();
    cg.set({
      check: !!checks
    });
    if (checks) cg.setShapes(checks.map(function(move) {
      return util.arrow(move.orig + move.dest, 'yellow');
    }));
  },
  promote: function(key, role) {
    var pieces = {};
    var piece = cg.data.pieces[key];
    if (piece && piece.role === 'pawn') {
      pieces[key] = {
        color: piece.color,
        role: role
      };
      cg.setPieces(pieces);
    }
  },
  data: function() {
    return cg.data;
  },
  pieces: function() {
    return cg.data.pieces;
  },
  get: function(key) {
    return cg.data.pieces[key];
  },
  showCapture: function(move) {
    raf(function() {
      var $square = $('#learn_app square[data-key=' + move.orig + ']');
      $square.addClass('wriggle');
      setTimeout(function() {
        $square.removeClass('wriggle');
        cg.setShapes([]);
        cg.apiMove(move.orig, move.dest);
      }, 600);
    });
  },
  showCheckmate: function(chess) {
    var turn = chess.instance.turn() === 'w' ? 'b' : 'w';
    var fen = [cg.getFen(), turn, '- - 0 1'].join(' ');
    chess.instance.load(fen);
    var kingKey = chess.kingKey(turn === 'w' ? 'black' : 'white');
    var shapes = chess.instance.moves({verbose:true}).filter(function(m) {
      return m.to === kingKey;
    }).map(function(m) {
      return util.arrow(m.from + m.to, 'red');
    });
    cg.set({check: shapes.length ? kingKey : null});
    cg.setShapes(shapes);
  },
  resetShapes: function() {
    cg.setShapes([]);
  }
};
