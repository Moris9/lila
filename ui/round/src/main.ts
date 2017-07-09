/// <reference types="types/lichess" />
/// <reference types="types/lichess-jquery" />

import { Chessground } from 'chessground';
import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

import { RoundOpts } from './interfaces';
import RoundController from './ctrl';
import MoveOn from './moveOn';
import { main as view } from './view/main';
import boot from './boot';

const patch = init([klass, attributes]);

export interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}

export function app(opts: RoundOpts): RoundApi {

  let vnode: VNode, ctrl: RoundController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new RoundController(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn
  };
};

export { boot };

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
