var m = require('mithril');
var partial = require('chessground').util.partial;
var pgnExport = require('../pgnExport');
var treePath = require('../path');

module.exports = function(ctrl) {
  var fctrl = ctrl.forecast;
  var cSteps = ctrl.analyse.getStepsAfterPly(ctrl.vm.path, fctrl.gamePly());
  var isCandidate = fctrl.isCandidate(cSteps);
  return m('div.forecast', [
    m('div.box', [
      m('div.top', 'Conditional premoves'),
      m('div.list', fctrl.list().map(function(steps, i) {
        return m('div.entry', {
          'data-icon': 'G',
          class: 'text',
          onclick: function() {
            ctrl.userJump(ctrl.analyse.addSteps(steps, treePath.default(fctrl.gamePly())));
          }
        }, [
          m('a', {
            class: 'del',
            onclick: function(e) {
              fctrl.removeIndex(i);
              e.stopPropagation();
            }
          }, 'x'),
          m.trust(pgnExport.renderStepsHtml(steps))
        ])
      })),
      m('button', {
        class: 'add button text',
        'data-icon': isCandidate ? 'O' : "",
        'disabled': !isCandidate,
        onclick: function() {
          fctrl.addSteps(cSteps);
        }
      }, isCandidate ? [
        m('span', 'Add current variation'),
        m('span', m.trust(pgnExport.renderStepsHtml(cSteps)))
      ] : [
        m('span', 'Play a variation to create'),
        m('span', 'conditional premoves')
      ])
    ]),
    m('div.back_to_game',
      m('a', {
        class: 'button text',
        href: '/' + ctrl.data.game.id + '/' + ctrl.data.player.id,
        'data-icon': 'i'
      }, ctrl.trans('backToGame'))
    )
  ]);
};
