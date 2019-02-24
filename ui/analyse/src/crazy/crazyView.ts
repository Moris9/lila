import { drag } from './crazyCtrl';
import { h } from 'snabbdom'
import { MouchEvent } from 'chessground/types';
import AnalyseCtrl from '../ctrl';

const eventNames = ['mousedown', 'touchstart'];
const oKeys = ['pawn', 'knight', 'bishop', 'rook', 'queen'];

type Position = 'top' | 'bottom';

export default function(ctrl: AnalyseCtrl, color: Color, position: Position) {
  if (!ctrl.node.crazy) return;
  const pocket = ctrl.node.crazy.pockets[color === 'white' ? 0 : 1];
  const dropped = ctrl.justDropped;
  let captured = ctrl.justCaptured;
  if (captured) captured.role = captured.promoted ? 'pawn' : captured.role;
  const activeColor = color === ctrl.turnColor();
  const usable = !ctrl.embed && activeColor;
  return h(`div.pocket.is2d.${position}.pos-${ctrl.bottomColor()}`, {
    class: { usable },
    hook: {
      insert: vnode => {
        if (ctrl.embed) return;
        eventNames.forEach(name => {
          (vnode.elm as HTMLElement).addEventListener(name, e => drag(ctrl, color, e as MouchEvent));
        });
      }
    }
  }, oKeys.map(role => {
    let nb = pocket[role] || 0;
    if (activeColor) {
      if (dropped === role) nb--;
      if (captured && captured.role === role) nb++;
    }
    return h('piece.' + role + '.' + color, {
      attrs: {
        'data-role': role,
        'data-color': color,
        'data-nb': nb
      }
    });
  })
  );
}
