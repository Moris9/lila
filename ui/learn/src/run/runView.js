var m = require('mithril');
var chessground = require('chessground');
var util = require('../util');
var ground = require('../ground');
var congrats = require('../congrats');
var stageStarting = require('./stageStarting');
var stageComplete = require('./stageComplete');
var renderPromotion = require('../promotion').view;
var renderProgress = require('../progress').view;
var makeStars = require('../progress').makeStars;

function renderFailed(ctrl) {
  return m('div.result.failed', {
    onclick: ctrl.restart
  }, [
    m('h2', ctrl.trans.noarg('puzzleFailed')),
    m('button', ctrl.trans.noarg('retry'))
  ]);
}

function renderCompleted(ctrl, level) {
  return m('div.result.completed', {
    class: level.blueprint.nextButton ? 'next' : '',
    onclick: level.onComplete
  }, [
    m('h2', ctrl.trans.noarg(congrats())),
    level.blueprint.nextButton ? m('button', 'Next') : makeStars(level.blueprint, level.vm.score)
  ]);
}

module.exports = function(ctrl) {
  var stage = ctrl.stage;
  var level = ctrl.level;

  return m('div', {
    class: 'lichess_game' + ' ' + stage.cssClass + ' ' + level.blueprint.cssClass +
    (level.vm.starting ? ' starting' : '') +
    (level.vm.completed && !level.blueprint.nextButton ? ' completed' : '') +
    (level.vm.lastStep ? ' last-step' : '') +
    (level.blueprint.showPieceValues ? ' piece-values' : '')
  }, [
    ctrl.vm.stageStarting() ? stageStarting(ctrl) : null,
    ctrl.vm.stageCompleted() ? stageComplete(ctrl) : null,
    m('div.lichess_board_wrap', [
      m('div.lichess_board', chessground.view(ground.instance)),
      renderPromotion(ctrl, level),
    ]),
    m('div.lichess_ground', [
      m('div.title', [
        m('img', {
          src: stage.image
        }),
        m('div.text', [
          m('h2', ctrl.trans.noarg(stage.title)),
          m('p.subtitle', ctrl.trans.noarg(stage.subtitle))
        ])
      ]),
      level.vm.failed ? renderFailed(ctrl) : (
        level.vm.completed ? renderCompleted(ctrl, level) :
        m('div.goal', util.withLinebreaks(ctrl.trans.noarg(level.blueprint.goal)))
      ),
      renderProgress(ctrl.progress)
    ])
  ]);
};
