var m = require('mithril');
var socket = require('./socket');
var xhr = require('./xhr');

module.exports = function(env) {

  this.data = env.data;

  this.socket = new socket(env.socketSend, this);

  this.vm = {
    initiating: true,
    reloading: false
  };

  this.countActiveIn = function() {
    return this.data.in.filter(function(c) {
      return !c.declined;
    }).length;
  }

  this.update = function(data) {
    this.data = data;
    this.vm.initiating = false;
    this.vm.reloading = false;
    env.setCount(this.countActiveIn());
    this.notifyNew();
    m.redraw();
  }.bind(this);

  this.notifyNew = function() {
    this.data.in.forEach(function(c) {
      if (lichess.once('challenge-' + c.id)) {
        if (!lichess.quietMode) {
          env.show();
          $.sound.newChallenge();
        }
        lichess.desktopNotification(showUser(c.challenger) + ' challenges you!');
      }
    });
  }.bind(this);

  this.decline = function(id) {
    this.data.in.forEach(function(c) {
      if (c.id === id) {
        c.declined = true;
        xhr.decline(id);
      }
    }.bind(this));
  }.bind(this);

  xhr.load().then(this.update);

  var showUser = function(user) {
    var rating = user.rating + (user.provisional ? '?' : '');
    var fullName = (user.title ? user.title + ' ' : '') + user.name;
    return fullName + ' (' + rating + ')';
  };

  this.trans = lichess.trans(env.i18n);
};
