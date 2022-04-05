import StrongSocket from './component/socket';
import { requestIdleCallback, escapeHtml } from './component/functions';
import makeChat from './component/chat';
import once from './component/once';
import { spinnerHtml } from 'common/spinner';
import sri from './component/sri';
import { storage, tempStorage } from './component/storage';
import powertip from './component/powertip';
import {
  assetUrl,
  loadCss,
  loadCssPath,
  jsModule,
  loadScript,
  hopscotch,
  userComplete,
  loadModule,
  loadIife,
} from './component/assets';
import widget from './component/widget';
import pubsub from './component/pubsub';
import { unload, redirect, reload } from './component/reload';
import announce from './component/announce';
import trans from './component/trans';
import sound from './component/sound';
import * as miniBoard from 'common/mini-board';
import * as miniGame from './component/mini-game';
import { format as timeago, formatter as dateFormat } from './component/timeago';
import watchers from './component/watchers';

export default () => {
  const l = window.lichess;
  l.StrongSocket = StrongSocket;
  l.requestIdleCallback = requestIdleCallback;
  l.sri = sri;
  l.storage = storage;
  l.tempStorage = tempStorage;
  l.once = once;
  l.powertip = powertip;
  l.widget = widget;
  l.spinnerHtml = spinnerHtml;
  l.assetUrl = assetUrl;
  l.loadCss = loadCss;
  l.loadCssPath = loadCssPath;
  l.jsModule = jsModule;
  l.loadScript = loadScript;
  l.loadModule = loadModule;
  l.loadIife = loadIife;
  l.hopscotch = hopscotch;
  l.userComplete = userComplete;
  l.makeChat = makeChat;
  l.pubsub = pubsub;
  l.unload = unload;
  l.redirect = redirect;
  l.reload = reload;
  l.watchers = watchers;
  l.escapeHtml = escapeHtml;
  l.announce = announce;
  l.trans = trans;
  l.sound = sound;
  l.miniBoard = miniBoard;
  l.miniGame = miniGame;
  l.timeago = timeago;
  l.dateFormat = dateFormat;
  l.contentLoaded = (parent?: HTMLElement) => pubsub.emit('content-loaded', parent);
};
