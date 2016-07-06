var m = require('mithril');
var util = require('./util');
var makeItems = require('./item').ctrl;
var itemView = require('./item').view;
var makeScenario = require('./scenario');
var makeChess = require('./chess');
var ground = require('./ground');
var scoring = require('./score');
var sound = require('./sound');
var promotion = require('./promotion');

module.exports = function(blueprint, opts) {

  var items = makeItems({
    apples: blueprint.apples
  });

  var vm = {
    initialized: false,
    lastStep: false,
    completed: false,
    failed: false,
    score: 0,
    nbMoves: 0
  };

  var complete = function() {
    setTimeout(function() {
      vm.lastStep = false;
      vm.completed = true;
      sound.levelEnd();
      vm.score += scoring.getLevelBonus(blueprint, vm.nbMoves);
      ground.stop();
      m.redraw();
      if (!blueprint.nextButton) setTimeout(opts.onComplete, 1200);
    }, ground.data().stats.dragged ? 0 : 250);
  };

  // cheat
  Mousetrap.bind(['shift+enter'], complete);

  var failSoundOnce = function() {
    sound.once('failure', opts.stage.key + '/' + blueprint.id);
  };

  var assertData = function() {
    return {
      scenario: scenario,
      chess: chess,
      vm: vm
    };
  }

  var detectFailure = function() {
    var failed = blueprint.failure && blueprint.failure(assertData());
    if (failed) failSoundOnce();
    return failed;
  };

  var detectSuccess = function() {
    if (blueprint.success) return blueprint.success(assertData());
    else return !items.hasItem('apple')
  };

  var detectCapture = function() {
    if (!blueprint.detectCapture) return false;
    var fun = blueprint.detectCapture === 'unprotected' ? 'findUnprotectedCapture' : 'findCapture';
    var move = chess[fun]();
    if (!move) return;
    vm.failed = true;
    ground.stop();
    ground.showCapture(move);
    failSoundOnce();
    return true;
  };

  var sendMove = function(orig, dest, prom) {
    vm.nbMoves++;
    var move = chess.move(orig, dest, prom);
    if (move) ground.fen(chess.fen(), blueprint.color, {});
    else { // moving into check
      vm.failed = true;
      ground.showCheckmate(chess);
      failSoundOnce();
      return m.redraw();
    }
    var took = false,
      inScenario;
    items.withItem(move.to, function(item) {
      if (item === 'apple') {
        vm.score += scoring.apple;
        items.remove(move.to);
        took = true;
      }
    });
    if (!took && move.captured && blueprint.pointsForCapture) {
      if (blueprint.showPieceValues)
        vm.score += scoring.pieceValue(move.captured);
      else
        vm.score += scoring.capture;
      took = true;
    }
    ground.check(chess);
    if (scenario.player(move.from + move.to + (move.promotion || ''))) {
      vm.score += scoring.scenario;
      inScenario = true;
    } else
      vm.failed = vm.failed || detectCapture() || detectFailure();
    if (!vm.failed && detectSuccess()) complete();
    if (vm.completed) return;
    if (took) sound.take();
    else if (inScenario) sound.take();
    else sound.move();
    if (vm.failed) {
      if (blueprint.showFailureFollowUp) setTimeout(function() {
        var rm = chess.playRandomMove();
        ground.fen(chess.fen(), blueprint.color, {}, [rm.orig, rm.dest]);
      }, 600);
    } else if (!inScenario) {
      chess.color(blueprint.color);
      ground.color(blueprint.color, makeChessDests());
    }
    m.redraw();
  };

  var makeChessDests = function() {
    return chess.dests({
      illegal: blueprint.offerIllegalMove
    });
  };

  var onMove = function(orig, dest) {
    var piece = ground.get(dest);
    if (!piece || piece.color !== blueprint.color) return;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  };

  var chess = makeChess(
    blueprint.fen,
    blueprint.emptyApples ? [] : items.appleKeys());

  var scenario = makeScenario(blueprint.scenario, {
    chess: chess,
    makeChessDests: makeChessDests
  });

  ground.set({
    chess: chess,
    offerIllegalMove: blueprint.offerIllegalMove,
    autoCastle: blueprint.autoCastle,
    orientation: blueprint.color,
    onMove: onMove,
    items: {
      render: function(pos, key) {
        return items.withItem(key, itemView);
      }
    },
    shapes: blueprint.shapes
  });

  return {
    blueprint: blueprint,
    items: items,
    vm: vm,
    scenario: scenario,
    start: function() {
      sound.levelStart();
      if (chess.color() !== blueprint.color)
        setTimeout(scenario.opponent, 1000);
    },
    onComplete: opts.onComplete
  };
};
