import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts, Ctrl } from './interfaces'
import { PresetCtrl } from './preset'

import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

export { Ctrl as ChatCtrl, ChatPlugin } from './interfaces';

export default function LichessChat(element: Element, opts: ChatOpts): {
  preset: PresetCtrl
} {
  const patch = init([klass, attributes]);

  const container = element.parentNode as HTMLElement;

  let vnode: VNode, ctrl: Ctrl

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  vnode = patch(element, view(ctrl));

  window.Mousetrap.bind('/', () => {
    (container.querySelector('input.lichess_say') as HTMLElement).focus();
    return false;
  });

  return ctrl;
};
