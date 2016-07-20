var m = require('mithril');
var chessground = require('chessground');
var raf = chessground.util.requestAnimationFrame;
var ground = require('../ground');
var classSet = chessground.util.classSet;
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
    m('h2', 'Puzzle failed!'),
    m('button', 'Retry')
  ]);
}

function renderCompleted(level) {
  return m('div.result.completed', {
    class: level.blueprint.nextButton ? 'next' : '',
    onclick: level.onComplete
  }, [
    m('h2', congrats()),
    level.blueprint.nextButton ? m('button', 'Next') : makeStars(level.blueprint, level.vm.score)
  ]);
}

module.exports = function(ctrl) {
  var stage = ctrl.stage;
  var level = ctrl.level;

  return m('div', {
    class: classSet({
      'lichess_game': true,
      'starting': level.vm.starting,
      'completed': level.vm.completed && !level.blueprint.nextButton,
      'last-step': level.vm.lastStep,
      'piece-values': level.blueprint.showPieceValues
    }) + ' ' + stage.cssClass + ' ' + level.blueprint.cssClass
  }, [
    ctrl.vm.stageStarting() ? stageStarting(ctrl) : null,
    ctrl.vm.stageCompleted() ? stageComplete(ctrl) : null,
    m('div.lichess_board_wrap', [
      m('div.lichess_board', chessground.view(ground.instance)),
      renderPromotion(level),
    ]),
    m('div.lichess_ground', [
      m('div.title', [
        m('img', {
          src: stage.image
        }),
        m('div.text', [
          m('h2', stage.title),
          m('p.subtitle', stage.subtitle)
        ])
      ]),
      level.vm.failed ? renderFailed(ctrl) : (
        level.vm.completed ? renderCompleted(level) : m('div.goal', m.trust(level.blueprint.goal))
      ),
      renderProgress(ctrl.progress)
    ])
  ]);
};
