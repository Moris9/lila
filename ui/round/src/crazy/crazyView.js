var round = require('../round');
var crazyDrag = require('./crazyDrag');
var game = require('game').game;
var m = require('mithril');

var eventNames = ['mousedown', 'touchstart'];
var pieceRoles = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

module.exports = {
  pocket: function(ctrl, color, position) {
    var step = round.plyStep(ctrl.data, ctrl.vm.ply);
    if (!step.crazy) return;
    var droppedRole = ctrl.vm.justDropped;
    var preDropRole = ctrl.vm.preDrop;
    var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
    var usablePos = position === (ctrl.vm.flip ? 'top' : 'bottom');
    var usable = usablePos && !ctrl.replaying() && game.isPlayerPlaying(ctrl.data);
    var activeColor = color === ctrl.data.player.color;
    var captured = ctrl.vm.justCaptured;
    if (captured) {
      captured = captured.promoted ? 'pawn' : captured.role;
    }
    return m('div', {
        class: 'pocket is2d ' + position + (usable ? ' usable' : ''),
        config: function(el, isUpdate, ctx) {
          if (ctx.flip === ctrl.vm.flip || !usablePos) return;
          ctx.flip = ctrl.vm.flip;
          var onstart = lichess.partial(crazyDrag, ctrl);
          eventNames.forEach(function(name) {
            el.addEventListener(name, onstart);
          });
        }
      },
      pieceRoles.map(function(role) {
        var nb = pocket[role] || 0;
        if (activeColor) {
          if (droppedRole === role) nb--;
          if (captured === role) nb++;
        }
        return m('piece', {
          'data-role': role,
          'data-color': color,
          'data-nb': nb,
          class: role + ' ' + color + (activeColor && preDropRole === role ? ' premove' : '')
        });
      })
    );
  }
};
