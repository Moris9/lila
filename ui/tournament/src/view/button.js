var m = require('mithril');
var partial = require('chessground').util.partial;
var xhr = require('../xhr');

function withdraw(ctrl) {
  return m('button.button.right.text', {
    'data-icon': 'b',
    onclick: partial(xhr.withdraw, ctrl)
  }, ctrl.trans('withdraw'));
}

function join(ctrl) {
  return m('button.button.right.text', {
    'data-icon': 'G',
    onclick: partial(xhr.join, ctrl)
  }, ctrl.trans('join'));
}

module.exports = {
  withdraw: withdraw,
  join: join,
  joinWithdraw: function(ctrl) {
    return (!ctrl.userId || ctrl.data.isFinished) ? null : (
      ctrl.data.me && !ctrl.data.me.withdraw ? withdraw(ctrl) : join(ctrl));

  }
};
