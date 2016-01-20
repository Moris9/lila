var util = require('chessground').util;
var drag = require('chessground').drag;
var game = require('game').game;

module.exports = function(ctrl, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !game.isPlayerPlaying(ctrl.data)) return;
  var node = e.target.parentNode.parentNode;
  var role = node.getAttribute('data-role'),
    color = node.getAttribute('data-color');
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  var key;
  for (var i in util.allKeys) {
    if (!ctrl.chessground.data.pieces[util.allKeys[i]]) {
      key = util.allKeys[i];
      break;
    }
  }
  if (!key) return;
  var coords = util.key2pos(ctrl.chessground.data.orientation === 'white' ? key : util.invertKey(key));
  var piece = {
    role: role,
    color: color
  };
  var obj = {};
  obj[key] = piece;
  ctrl.chessground.setPieces(obj);
  var bounds = ctrl.chessground.data.bounds();
  var squareBounds = ctrl.vm.element.querySelector('square').getBoundingClientRect();
  var rel = [
    (coords[0] - 1) * squareBounds.width + bounds.left, (8 - coords[1]) * squareBounds.height + bounds.top
  ];
  ctrl.chessground.data.draggable.current = {
    orig: key,
    piece: piece.color + piece.role,
    rel: rel,
    epos: [e.clientX, e.clientY],
    pos: [e.clientX - rel[0], e.clientY - rel[1]],
    dec: [-squareBounds.width / 2, -squareBounds.height / 2],
    bounds: bounds,
    started: true,
    newPiece: true
  };
  drag.processDrag(ctrl.chessground.data);
}
