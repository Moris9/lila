import { h } from 'snabbdom'
import { Api as ChessgroundApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Step, Redraw } from './interfaces';

export type KeyboardMoveHandler = (fen: Fen, dests?: cg.Dests) => void;

export interface KeyboardMove {
  update(step: Step): void;
  registerHandler(h: KeyboardMoveHandler): void
  hasFocus(): boolean;
  setFocus(v: boolean): void;
  san(orig: cg.Key, dest: cg.Key): void;
  select(key: cg.Key): void;
  hasSelected(): cg.Key | undefined;
  usedSan: boolean;
}

export function ctrl(cg: ChessgroundApi, step: Step, redraw: Redraw): KeyboardMove {
  let focus = false;
  let handler: KeyboardMoveHandler | undefined;
  let preHandlerBuffer = step.fen;
  const select = function(key: cg.Key): void {
    if (cg.state.selected === key) cg.cancelMove();
    else cg.selectSquare(key, true);
  };
  let usedSan = false;
  return {
    update(step) {
      if (handler) handler(step.fen, cg.state.movable.dests);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h: KeyboardMoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, cg.state.movable.dests);
    },
    hasFocus: () => focus,
    setFocus(v) {
      focus = v;
      redraw();
    },
    san(orig, dest) {
      usedSan = true;
      cg.cancelMove();
      select(orig);
      select(dest);
    },
    select,
    hasSelected: () => cg.state.selected,
    usedSan
  };
}

export function render(ctrl: KeyboardMove) {
  return h('div.keyboard-move', [
    h('input', {
      attrs: {
        spellcheck: false,
        autocomplete: false
      },
      hook: {
        insert: vnode => {
          window.lichess.loadScript('/assets/javascripts/keyboardMove.js').then(() => {
            ctrl.registerHandler(window.lichess.keyboardMove({
              input: vnode.elm,
              setFocus: ctrl.setFocus,
              select: ctrl.select,
              hasSelected: ctrl.hasSelected,
              san: ctrl.san
            }));
          });
        }
      }
    }),
    ctrl.hasFocus() ?
    h('em', 'Enter SAN (Nc3) or UCI (b1c3) moves, or type / to focus chat') :
    h('strong', 'Press <enter> to focus')
  ]);
}
