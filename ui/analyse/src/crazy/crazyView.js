var crazyDrag = require('./crazyDrag');
var partial = require('chessground').util.partial;
var m = require('mithril');

function crazyPocketTag(role, color) {
  return {
    tag: 'div',
    attrs: {
      class: 'no-square'
    },
    children: [{
      tag: 'piece',
      attrs: {
        class: role + ' ' + color
      }
    }]
  };
}

var eventNames = ['mousedown', 'touchstart'];

module.exports = {
  pocket: function(ctrl, color, position) {
    var step = ctrl.vm.step;
    if (!step.crazy) return;
    var pocket = step.crazy.pockets[color === 'white' ? 0 : 1];
    var oKeys = Object.keys(pocket)
    var usable = color === ctrl.chessground.data.movable.color;
    return m('div', {
        class: 'pocket is2d ' + position + (usable ? ' usable' : ''),
        config: function(el, isUpdate, context) {
          if (isUpdate) return;
          var onstart = partial(crazyDrag, ctrl, color);
          eventNames.forEach(function(name) {
            el.addEventListener(name, onstart);
          });
          context.onunload = function() {
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
