import { game } from 'game';
import { dragNewPiece } from 'chessground/drag';
import RoundController from '../ctrl';
import * as cg from 'chessground/types';

export function drag(ctrl: RoundController, e: cg.MouchEvent) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ctrl.replaying() || !game.isPlayerPlaying(ctrl.data)) return;
  const el = e.target as HTMLElement,
  role = el.getAttribute('data-role'),
    color = el.getAttribute('data-color'),
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  dragNewPiece(ctrl.chessground.state, { color, role }, e);
}

export function valid(data, role, key) {

  if (!game.isPlayerTurn(data)) return false;

  if (role === 'pawn' && (key[1] === '1' || key[1] === '8')) return false;

  var dropStr = data.possibleDrops;

  if (typeof dropStr === 'undefined' || dropStr === null) return true;

  var drops = dropStr.match(/.{2}/g) || [];

  return drops.indexOf(key) !== -1;
}
