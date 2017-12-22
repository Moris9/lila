import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { plyStep } from '../round';
import renderTable from './table';
import * as promotion from '../promotion';
import { render as renderGround } from '../ground';
import { read as fenRead } from 'chessground/fen';
import * as util from '../util';
import * as blind from '../blind';
import * as keyboard from '../keyboard';
import crazyView from '../crazy/crazyView';
import { render as keyboardMove } from '../keyboardMove';
import RoundController from '../ctrl';
import * as cg from 'chessground/types';

function renderMaterial(material: cg.MaterialDiffSide, score: number, checks?: number) {
  const children: VNode[] = [];
  let role: string, i: number;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (i = 0; i < material[role]; i++) content.push(h('mono-piece.' + role));
      children.push(h('tomb', content));
    }
  }
  if (checks) for (i = 0; i < checks; i++) children.push(h('tomb', h('mono-piece.king')));
  if (score > 0) children.push(h('score', '+' + score));
  return h('div.cemetery', children);
}

function wheel(ctrl: RoundController, e: WheelEvent): boolean {
  if (ctrl.isPlaying()) return true;
  e.preventDefault();
  if (e.deltaY > 0) keyboard.next(ctrl);
  else if (e.deltaY < 0) keyboard.prev(ctrl);
  ctrl.redraw();
  return false;
}

function visualBoard(ctrl: RoundController) {
  return h('div.lichess_board_wrap', [
    h('div.lichess_board.' + ctrl.data.game.variant.key + (ctrl.data.pref.blindfold ? '.blindfold' : ''), {
      hook: util.bind('wheel', (e: WheelEvent) => wheel(ctrl, e))
    }, [renderGround(ctrl)]),
    promotion.view(ctrl)
  ]);
}

function blindBoard(ctrl: RoundController) {
  return h('div.lichess_board_blind', [
    h('div.textual', {
      hook: {
        insert: vnode => blind.init(vnode.elm as HTMLElement, ctrl)
      }
    }, [ renderGround(ctrl) ])
  ]);
}

const emptyMaterialDiff: cg.MaterialDiff = {
  white: {},
  black: {}
};

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data,
  cgState = ctrl.chessground && ctrl.chessground.state,
  topColor = d[ctrl.flip ? 'player' : 'opponent'].color,
  bottomColor = d[ctrl.flip ? 'opponent' : 'player'].color;
  let material: cg.MaterialDiff, score: number = 0;
  if (d.pref.showCaptured) {
    var pieces = cgState ? cgState.pieces : fenRead(plyStep(ctrl.data, ctrl.ply).fen);
    material = util.getMaterialDiff(pieces);
    score = util.getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
  } else material = emptyMaterialDiff;
  return h('div.round.cg-512', [
    h('div.lichess_game.gotomove.variant_' + d.game.variant.key, {
      hook: {
        insert: () => window.lichess.pubsub.emit('content_loaded')()
      }
    }, [
      d.blind ? blindBoard(ctrl) : visualBoard(ctrl),
      h('div.lichess_ground', [
        crazyView(ctrl, topColor, 'top') || renderMaterial(material[topColor], -score, d.player.checks),
        renderTable(ctrl),
        crazyView(ctrl, bottomColor, 'bottom') || renderMaterial(material[bottomColor], score, d.opponent.checks)
      ])
    ]),
    h('div.underboard', [
      h('div.center', {
        hook: {
          insert: vnode => {
            if (ctrl.opts.crosstableEl) {
              const el = (vnode.elm as HTMLElement);
              el.insertBefore(ctrl.opts.crosstableEl, el.firstChild);
            }
          }
        }
      }, [
        ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null
      ])
    ])
  ]);
};
