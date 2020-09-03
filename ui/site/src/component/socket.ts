import { storage as makeStorage } from './storage';
import sri from './sri';
import { reload } from './reload';
import idleTimer from './idle-timer';

type Sri = string;
type Tpe = string;
type Payload = any;
type Version = number;
interface MsgBase {
  t: Tpe;
  d?: Payload;
}
interface MsgIn extends MsgBase {
  v?: Version;
}
interface MsgOut extends MsgBase {
}
interface MsgAck extends MsgOut {
  at: number;
}
type Send = (t: Tpe, d: Payload) => void;
type Timer = ReturnType<typeof setTimeout>;

interface Options {
  idle: boolean;
  pingMaxLag: number; // time to wait for pong before reseting the connection
  pingDelay: number; // time between pong and ping
  autoReconnectDelay: number;
  protocol: string;
  isAuth: boolean;
  debug?: boolean;
}
interface Settings {
  receive?: (t: Tpe, d: Payload) => void;
  events: {
    [tpe: string]: (d: Payload | null, msg: MsgIn) => any;
  };
  params: {
    sri: Sri;
  };
  options: Options;
}


class Ackable {

  currentId = 1; // increment with each ackable message sent
  messages: MsgAck[] = [];

  constructor(readonly send: Send) {
    setInterval(this.resend, 1200);
  }

  resend = () => {
    const resendCutoff = performance.now() - 2500;
    this.messages.forEach(m => {
      if (m.at < resendCutoff) this.send(m.t, m.d);
    });
  }

  register = (t: Tpe, d: Payload) => {
    d.a = this.currentId++;
    this.messages.push({
      t: t,
      d: d,
      at: performance.now()
    });
  }

  onServerAck = (id: number) => {
    this.messages = this.messages.filter(m => m.d.a !== id);
  }
}

// versioned events, acks, retries, resync
export default class StrongSocket {

  pubsub = window.lichess.pubsub;
  settings: Settings;
  options: Options;
  version: number | false;
  ws: WebSocket | undefined;
  pingSchedule: Timer;
  connectSchedule: Timer;
  ackable: Ackable = new Ackable((t, d) => this.send(t, d));
  lastPingTime: number = performance.now();
  pongCount: number = 0;
  averageLag: number = 0;
  tryOtherUrl: boolean = false;
  autoReconnect: boolean = true;
  nbConnects: number = 0;
  storage: LichessStorage = makeStorage.make('surl8');

  static defaults: Settings = {
    events: {},
    params: { sri },
    options: {
      idle: false,
      pingMaxLag: 9000, // time to wait for pong before reseting the connection
      pingDelay: 2500, // time between pong and ping
      autoReconnectDelay: 3500,
      protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
      isAuth: document.body.hasAttribute('user')
    }
  };

  static resolveFirstConnect: (send: Send) => void;
  static firstConnect = new Promise<Send>(r => {
    StrongSocket.resolveFirstConnect = r;
  });

  constructor(readonly url: string, initialVersion: number | false, settings?: any) {
    this.settings = $.extend(true, {}, StrongSocket.defaults, settings);
    this.options = this.settings.options;
    this.version = initialVersion;
    this.pubsub.on('socket.send', this.send);
    window.addEventListener('unload', this.destroy);
    this.connect();
  }

  connect = () => {
    this.destroy();
    this.autoReconnect = true;
    let params = $.param(this.settings.params);
    if (this.version !== false) params += (params ? '&' : '') + 'v=' + this.version;
    const fullUrl = this.options.protocol + '//' + this.baseUrl() + this.url + '?' + params;
    this.debug("connection attempt to " + fullUrl);
    try {
      const ws = this.ws = new WebSocket(fullUrl);
      ws.onerror = e => this.onError(e);
      ws.onclose = () => {
        this.pubsub.emit('socket.close');
        if (this.autoReconnect) {
          this.debug('Will autoreconnect in ' + this.options.autoReconnectDelay);
          this.scheduleConnect(this.options.autoReconnectDelay);
        }
      };
      ws.onopen = () => {
        this.debug("connected to " + fullUrl);
        this.onSuccess();
        const cl = document.body.classList;
        cl.remove('offline');
        cl.add('online');
        cl.toggle('reconnected', this.nbConnects > 1);
        this.pingNow();
        this.pubsub.emit('socket.open');
        this.ackable.resend();
      };
      ws.onmessage = e => {
        if (e.data == 0) return this.pong();
        const m = JSON.parse(e.data);
        if (m.t === 'n') this.pong();
        this.handle(m);
      };
    } catch (e) {
      this.onError(e);
    }
    this.scheduleConnect(this.options.pingMaxLag);
  };

  send = (t: string, d: any, o: any = {}, noRetry = false) => {
    const msg: Partial<MsgOut> = { t };
    if (d !== undefined) {
      if (o.withLag) d.l = Math.round(this.averageLag);
      if (o.millis >= 0) d.s = Math.round(o.millis * 0.1).toString(36);
      msg.d = d;
    }
    if (o.ackable) {
      msg.d = msg.d || {}; // can't ack message without data
      this.ackable.register(t, msg.d); // adds d.a, the ack ID we expect to get back
    }
    const message = JSON.stringify(msg);
    this.debug("send " + message);
    try {
      this.ws!.send(message);
    } catch (e) {
      // maybe sent before socket opens,
      // try again a second later.
      if (!noRetry) setTimeout(() => this.send(t, msg.d, o, true), 1000);
    }
  };

  scheduleConnect = (delay: number) => {
    if (this.options.idle) delay = 10 * 1000 + Math.random() * 10 * 1000;
    // debug('schedule connect ' + delay);
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.connectSchedule = setTimeout(() => {
      document.body.classList.add('offline');
      document.body.classList.remove('online');
      this.tryOtherUrl = true;
      this.connect();
    }, delay);
  }

  schedulePing = (delay: number) => {
    clearTimeout(this.pingSchedule);
    this.pingSchedule = setTimeout(this.pingNow, delay);
  }

  pingNow = () => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    const pingData = (this.options.isAuth && this.pongCount % 8 == 2) ? JSON.stringify({
      t: 'p',
      l: Math.round(0.1 * this.averageLag)
    }) : 'null';
    try {
      this.ws!.send(pingData);
      this.lastPingTime = performance.now();
    } catch (e) {
      this.debug(e, true);
    }
    this.scheduleConnect(this.options.pingMaxLag);
  }

  computePingDelay = () => this.options.pingDelay + (this.options.idle ? 1000 : 0);

  pong = () => {
    clearTimeout(this.connectSchedule);
    this.schedulePing(this.computePingDelay());
    const currentLag = Math.min(performance.now() - this.lastPingTime, 10000);
    this.pongCount++;

    // Average first 4 pings, then switch to decaying average.
    const mix = this.pongCount > 4 ? 0.1 : 1 / this.pongCount;
    this.averageLag += mix * (currentLag - this.averageLag);

    this.pubsub.emit('socket.lag', this.averageLag);
  };

  handle = (m: MsgIn) => {
    if (m.v && this.version !== false) {
      if (m.v <= this.version) {
        this.debug("already has event " + m.v);
        return;
      }
      // it's impossible but according to previous logging, it happens nonetheless
      if (m.v > this.version + 1) return reload();
      this.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        reload();
        break;
      case 'ack':
        this.ackable.onServerAck(m.d);
        break;
      default:
        this.pubsub.emit('socket.in.' + m.t, m.d, m);
        const processed = this.settings.receive && this.settings.receive(m.t, m.d);
        if (!processed && this.settings.events[m.t]) this.settings.events[m.t](m.d || null, m);
    }
  };

  debug = (msg: string, always = false) => {
    if (always || this.options.debug) console.debug(msg);
  };

  destroy = () => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = undefined;
  };

  disconnect = () => {
    const ws = this.ws;
    if (ws) {
      this.debug("Disconnect");
      this.autoReconnect = false;
      ws.onerror = ws.onclose = ws.onopen = ws.onmessage = () => { };
      ws.close();
    }
  };

  onError = (e: Event) => {
    this.options.debug = true;
    this.debug('error: ' + JSON.stringify(e));
    this.tryOtherUrl = true;
    clearTimeout(this.pingSchedule);
  };

  onSuccess = () => {
    this.nbConnects++;
    if (this.nbConnects == 1) {
      StrongSocket.resolveFirstConnect(this.send);
      let disconnectTimeout: Timer | undefined;
      idleTimer(10 * 60 * 1000, () => {
        this.options.idle = true;
        disconnectTimeout = setTimeout(this.destroy, 2 * 60 * 60 * 1000);
      }, () => {
        this.options.idle = false;
        if (this.ws) clearTimeout(disconnectTimeout);
        else location.reload();
      });
    }
  }

  baseUrl = () => {
    const baseUrls = document.body.getAttribute('data-socket-domains')!.split(',');
    let url = this.storage.get();
    if (!url || this.tryOtherUrl) {
      url = baseUrls[Math.floor(Math.random() * baseUrls.length)];
      this.storage.set(url);
    }
    return url;
  };

  pingInterval = () => this.computePingDelay() + this.averageLag;
  getVersion = () => this.version;
}
