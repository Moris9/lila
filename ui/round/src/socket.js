var game = require('game').game;
var throttle = require('common').throttle;
var ground = require('./ground');
var xhr = require('./xhr');
var sound = require('./sound');

module.exports = function(socket, ctrl) {

  this.send = socket.send;

  this.sendLoading = function() {
    ctrl.setLoading(true);
    this.send.apply(this, arguments);
  }.bind(this);

  var reload = function(o, isRetry) {
    // avoid reload if possible!
    if (o && o.t) handlers[o.t](o.d);
    else xhr.reload(ctrl).then(function(data) {
      if (lichess.socket.getVersion() > data.player.version) {
        // race condition! try to reload again
        if (isRetry) lichess.reload(); // give up and reload the page
        else reload(o, true);
      }
      else ctrl.reload(data);
    });
  };

  var handlers = {
    takebackOffers: function(o) {
      ctrl.setLoading(false);
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color];
      ctrl.redraw();
    },
    move: function(o) {
      o.isMove = true;
      ctrl.apiMove(o);
    },
    drop: function(o) {
      o.isDrop = true;
      ctrl.apiMove(o);
    },
    reload: reload,
    redirect: ctrl.setRedirecting,
    clock: function(o) {
      if (ctrl.clock) {
        ctrl.clock.update(o.white, o.black);
        ctrl.redraw();
      }
    },
    crowd: function(o) {
      ['white', 'black'].forEach(function(c) {
        game.setOnGame(ctrl.data, c, o[c]);
      });
      ctrl.redraw();
    },
    end: function(winner) {
      ctrl.data.game.winner = winner;
      ctrl.chessground.stop();
      ctrl.setLoading(true);
      xhr.reload(ctrl).then(ctrl.reload);
      if (!ctrl.data.player.spectator && ctrl.data.game.turns > 1)
        lichess.sound[winner ? (ctrl.data.player.color === winner ? 'victory' : 'defeat') : 'draw']();
    },
    rematchOffer: function(by) {
      ctrl.data.player.offeringRematch = by === ctrl.data.player.color;
      ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color;
      ctrl.redraw();
    },
    rematchTaken: function(nextId) {
      ctrl.data.game.rematch = nextId;
      if (!ctrl.data.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer: function(by) {
      ctrl.data.player.offeringDraw = by === ctrl.data.player.color;
      ctrl.data.opponent.offeringDraw = by === ctrl.data.opponent.color;
      ctrl.redraw();
    },
    berserk: function(color) {
      ctrl.setBerserk(color);
    },
    gone: function(isGone) {
      if (!ctrl.data.opponent.ai) {
        game.setIsGone(ctrl.data, ctrl.data.opponent.color, isGone);
        ctrl.redraw();
      }
    },
    checkCount: function(e) {
      ctrl.data.player.checks = ctrl.data.player.color == 'white' ? e.white : e.black;
      ctrl.data.opponent.checks = ctrl.data.opponent.color == 'white' ? e.white : e.black;
      ctrl.redraw();
    },
    simulPlayerMove: function(gameId) {
      if (
        ctrl.userId &&
        ctrl.data.simul &&
        ctrl.userId == ctrl.data.simul.hostId &&
        gameId !== ctrl.data.game.id &&
        ctrl.moveOn.get() &&
        ctrl.chessground.state.turnColor !== ctrl.chessground.state.movable.color) {
        ctrl.setRedirecting();
        sound.move();
        lichess.hasToReload = true;
        location.href = '/' + gameId;
      }
    }
  };

  this.moreTime = throttle(300, false, () => this.send('moretime'));

  this.outoftime = throttle(500, false, () => {
    this.send('flag', ctrl.data.game.player)
  });

  this.berserk = throttle(200, false, () => this.send('berserk', null, { ackable: true }));

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);

  lichess.pubsub.on('ab.rep', function(n) {
    socket.send('rep', {
      n: n
    });
  });
}
