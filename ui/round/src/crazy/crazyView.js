var round = require('../round');
var partial = require('chessground').util.partial;
var crazyDrag = require('./crazyDrag');
var game = require('game').game;
var m = require('mithril');

var eventNames = ['mousedown', 'touchstart'];

module.exports = {
  pocket: function(ctrl, color, position) {
    var step = round.plyStep(ctrl.data, ctrl.vm.ply);
    if (!step.crazy) return;
    var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
    var oKeys = Object.keys(pocket);
    var crowded = oKeys.length > 4;
    var usablePos = position == (ctrl.vm.flip ? 'top' : 'bottom');
    var usable = usablePos && !ctrl.replaying() && game.isPlayerPlaying(ctrl.data);
    return m('div', {
        class: 'pocket is2d ' + position + (usable ? ' usable' : '') + (crowded ? ' crowded' : ''),
        config: function(el, isUpdate, ctx) {
          if (ctx.flip === ctrl.vm.flip || !usablePos) return;
          ctx.flip = ctrl.vm.flip;
          var onstart = partial(crazyDrag, ctrl);
          eventNames.forEach(function(name) {
            el.addEventListener(name, onstart);
          });
          ctx.onunload = function() {
            eventNames.forEach(function(name) {
              el.removeEventListener(name, onstart);
            });
          }
        }
      },
      oKeys.map(function(role) {
        return m('piece', {
          'data-role': role,
          'data-color': color,
          'data-nb': pocket[role],
          class: role + ' ' + color
        });
      })
    );
  }
};
