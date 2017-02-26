var m = require('mithril');
var socket = require('./socket');
var variant = require('./variant');
var hookRepo = require('./hookRepo');
var seekRepo = require('./seekRepo');
var makeStore = require('./store');
var xhr = require('./xhr');
var poolRangeStorage = require('./poolRangeStorage');

module.exports = function(env) {

  this.data = env.data;
  this.data.hooks = []; // no longer preloaded!
  this.playban = env.playban;
  this.currentGame = env.currentGame;
  this.perfIcons = env.perfIcons;

  hookRepo.initAll(this);
  seekRepo.initAll(this);

  this.socket = new socket(env.socketSend, this);

  var store = makeStore(this.data.me ? this.data.me.username.toLowerCase() : null);
  var poolInStorage = lichess.storage.make('lobby.pool-in');
  poolInStorage.listen(function() {
    // when another tab joins a pool
    this.leavePool();
    m.redraw();
  }.bind(this));

  this.vm = {
    tab: store.tab.get(),
    mode: store.mode.get(),
    sort: store.sort.get(),
    filterOpen: false,
    stepHooks: this.data.hooks.slice(0),
    stepping: false,
    redirecting: false,
    poolMember: null
  };

  var flushHooksTimeout;

  var doFlushHooks = function() {
    this.vm.stepHooks = this.data.hooks.slice(0);
    if (this.vm.tab === 'real_time') m.redraw();
  }.bind(this);

  this.flushHooks = function(now) {
    clearTimeout(flushHooksTimeout);
    if (now) doFlushHooks();
    else {
      this.vm.stepping = true;
      if (this.vm.tab === 'real_time') m.redraw();
      setTimeout(function() {
        this.vm.stepping = false;
        doFlushHooks();
      }.bind(this), 500);
    }
    flushHooksTimeout = flushHooksSchedule();
  }.bind(this);

  var flushHooksSchedule = lichess.partial(setTimeout, this.flushHooks, 8000);
  flushHooksSchedule();

  this.setTab = function(tab) {
    if (tab !== this.vm.tab) {

      if (tab === 'seeks') xhr.seeks().then(this.setSeeks);

      if (tab === 'real_time') this.socket.realTimeIn();
      else if (this.vm.tab === 'real_time') {
        this.socket.realTimeOut();
        this.data.hooks = [];
      }

      this.vm.tab = store.tab.set(tab);
    }
    this.vm.filterOpen = false;
  }.bind(this);

  this.setMode = function(mode) {
    this.vm.mode = store.mode.set(mode);
    this.vm.filterOpen = false;
  }.bind(this);

  this.setSort = function(sort) {
    this.vm.sort = store.sort.set(sort);
  }.bind(this);

  this.toggleFilter = function() {
    this.vm.filterOpen = !this.vm.filterOpen;
  }.bind(this);

  this.setFilter = function(filter) {
    this.data.filter = filter;
    this.flushHooks(true);
    if (this.vm.tab !== 'real_time') m.redraw();
  }.bind(this);

  this.clickHook = function(id) {
    var hook = hookRepo.find(this, id);
    if (!hook || hook.disabled || this.vm.stepping || this.vm.redirecting) return;
    if (hook.action === 'cancel' || variant.confirm(hook.variant)) this.socket.send(hook.action, hook.id);
  }.bind(this);

  this.clickSeek = function(id) {
    var seek = seekRepo.find(this, id);
    if (!seek || this.vm.redirecting) return;
    if (seek.action === 'cancelSeek' || variant.confirm(seek.variant)) this.socket.send(seek.action, seek.id);
  }.bind(this);

  this.setSeeks = function(seeks) {
    this.data.seeks = seeks;
    seekRepo.initAll(this);
    m.redraw();
  }.bind(this);

  this.clickPool = function(id) {
    if (!this.data.me) {
      xhr.anonPoolSeek(this.data.pools.find(function(p) {
        return p.id === id;
      }));
      this.setTab('real_time');
    } else if (this.vm.poolMember && this.vm.poolMember.id === id) this.leavePool();
    else this.enterPool({
      id: id
    });
  }.bind(this);

  this.enterPool = function(member) {
    poolRangeStorage.set(member.id, member.range);
    this.setTab('pools');
    this.vm.poolMember = member;
    this.poolIn();
    m.redraw();
  }.bind(this);

  this.leavePool = function() {
    if (!this.vm.poolMember) return;
    this.socket.poolOut(this.vm.poolMember);
    this.vm.poolMember = null;
    m.redraw();
  }.bind(this);

  this.poolIn = function() {
    poolInStorage.set(1);
    this.socket.poolIn(this.vm.poolMember);
  }.bind(this);

  this.gameActivity = function(gameId) {
    if (this.data.nowPlaying.filter(function(p) {
      return p.gameId === gameId;
    }).length) xhr.nowPlaying().then(function(povs) {
      this.data.nowPlaying = povs;
      this.startWatching();
    }.bind(this));
  }.bind(this);

  var alreadyWatching = [];
  this.startWatching = function() {
    var newIds = this.data.nowPlaying.map(function(p) {
      return p.gameId;
    }).filter(function(id) {
      return alreadyWatching.indexOf(id) === -1;
    });
    if (newIds.length) {
      setTimeout(function() {
        this.socket.send("startWatching", newIds.join(' '));
      }.bind(this), 2000);
      newIds.forEach(alreadyWatching.push.bind(alreadyWatching));
    }
  }.bind(this);

  this.startWatching();

  this.setRedirecting = function() {
    this.vm.redirecting = true;
    setTimeout(function() {
      this.vm.redirecting = false;
    }.bind(this), 2000);
  }.bind(this);

  this.trans = lichess.trans(env.i18n);

  this.awake = function() {
    switch (this.vm.tab) {
      case 'real_time':
        this.data.hooks = [];
        this.socket.realTimeIn();
        break;
      case 'seeks':
        xhr.seeks().then(this.setSeeks);
        break;
      default:
    }
  }.bind(this);

  if (this.playban) setTimeout(lichess.reload, this.playban.remainingSeconds * 1000);
  else {

    setInterval(function() {
      if (this.vm.poolMember) this.poolIn();
      else if (this.vm.tab === 'real_time' && !this.data.hooks.length) this.socket.realTimeIn();
    }.bind(this), 10 * 1000);

    // new opponent button
    if (location.hash.indexOf('#pool/') === 0) {
      var regex = /^#pool\/(\d+\+\d+)$/
      var match = regex.exec(location.hash);
      var member = {
        id: match[1]
      };
      var range = poolRangeStorage.get(member.id);
      if (range) member.range = range;
      if (match) {
        this.setTab('pools');
        this.enterPool(member);
        history.replaceState(null, null, '/');
      }
    }
  }

  lichess.pubsub.on('socket.open', function() {
    if (this.vm.tab === 'real_time') {
      this.data.hooks = [];
      this.socket.realTimeIn();
    } else if (this.vm.tab === 'pools' && this.vm.poolMember) this.poolIn();
  }.bind(this));

  window.addEventListener('beforeunload', function() {
    if (this.vm.poolMember) this.socket.poolOut(this.vm.poolMember);
  }.bind(this));
};
