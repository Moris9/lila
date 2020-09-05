import StrongSocket from "./component/socket";
import { requestIdleCallback, formAjax, numberFormat, escapeHtml } from "./component/functions";
import makeChat from './component/chat';
import once from './component/once';
import hasTouchEvents from './component/touchEvents';
import spinnerHtml from './component/spinner';
import sri from './component/sri';
import { storage, tempStorage } from "./component/storage";
import powertip from "./component/powertip";
import { assetUrl, soundUrl, loadCss, loadCssPath, jsModule, loadScript, hopscotch, slider } from "./component/assets";
import widget from "./component/widget";
import idleTimer from "./component/idle-timer";
import pubsub from "./component/pubsub";
import { unload, redirect, reload } from "./component/reload";
import announce from "./component/announce";
import trans from "./component/trans";
import sound from "./component/sound";
import soundBox from "./component/soundbox";
import userAutocomplete from "./component/user-autocomplete";
import miniBoard from "./component/mini-board";
import miniGame from "./component/mini-game";
import timeago from "./component/timeago";
import watchers from "./component/watchers";

export default function() {
  const l = window.lichess;
  l.StrongSocket = StrongSocket;
  l.requestIdleCallback = requestIdleCallback;
  l.hasTouchEvents = hasTouchEvents;
  l.sri = sri;
  l.storage = storage;
  l.tempStorage = tempStorage;
  l.once = once;
  l.powertip = powertip;
  l.widget = widget;
  l.spinnerHtml = spinnerHtml;
  l.assetUrl = assetUrl;
  l.soundUrl = soundUrl;
  l.loadCss = loadCss;
  l.loadCssPath = loadCssPath;
  l.jsModule = jsModule;
  l.loadScript = loadScript;
  l.hopscotch = hopscotch;
  l.slider = slider;
  l.makeChat = makeChat;
  l.formAjax = formAjax;
  l.numberFormat = numberFormat;
  l.idleTimer = idleTimer;
  l.pubsub = pubsub;
  l.unload = unload;
  l.redirect = redirect;
  l.reload = reload;
  l.watchers = watchers;
  l.escapeHtml = escapeHtml;
  l.announce = announce;
  l.trans = trans;
  l.sound = sound;
  l.soundBox = soundBox;
  l.userAutocomplete = userAutocomplete;
  l.miniBoard = miniBoard;
  l.miniGame = miniGame;
  l.timeago = timeago;
}
