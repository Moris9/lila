import { winningChances, pv2san } from 'ceval';
import { Eval } from 'ceval';
import { path as treePath } from 'tree';
import { detectThreefold } from '../nodeFinder';
import AnalyseController from '../ctrl';
import { prop } from 'common';

export interface Comment {
  prev: Tree.Node;
  node: Tree.Node;
  path: Tree.Path;
  verdict: 'good' | 'inaccuracy' | 'mistake' | 'blunder';
  best?: {
    uci: Uci;
    san: San;
  }
}

export interface PracticeController {
  [key: string]: any; // #TODO
}

export function make(root: AnalyseController, playableDepth: () => number): PracticeController {

  const running = prop(true);
  const comment = prop<Comment | null>(null);
  const hovering = prop<any>(null);
  const hinting = prop<any>(null);
  const played = prop(false);

  function ensureCevalRunning() {
    if (!root.showComputer()) root.toggleComputer();
    if (!root.ceval.enabled()) root.toggleCeval();
    if (root.threatMode) root.toggleThreatMode();
  }

  function commentable(node: Tree.Node, bonus: number = 0): boolean {
    if (root.gameOver(node)) return true;
    const ceval = node.ceval;
    return ceval ? ((ceval.depth + bonus) >= 15 || (ceval.depth >= 13 && ceval.millis > 3000)) : false;
  }

  function playable(node: Tree.Node): boolean {
    const ceval = node.ceval;
    return ceval ? (
      ceval.depth >= Math.min(ceval.maxDepth || 99, playableDepth()) ||
      (ceval.depth >= 15 && ceval.millis > 5000)
    ) : false;
  };

  const altCastles = {
    e1a1: 'e1c1',
    e1h1: 'e1g1',
    e8a8: 'e8c8',
    e8h8: 'e8g8'
  };

  function makeComment(prev, node: Tree.Node, path: Tree.Path): Comment {
    let verdict, best;
    const over = root.gameOver(node);

    if (over === 'checkmate') verdict = 'good';
    else {
      const nodeEval: Eval = (node.threefold || over === 'draw') ? {
        cp: 0
      } : (node.ceval as Eval);
      const shift = -winningChances.povDiff(root.bottomColor(), nodeEval, prev.ceval);

      best = prev.ceval.pvs[0].moves[0];
      if (best === node.uci || best === altCastles[node.uci]) best = null;

      if (!best) verdict = 'good';
      else if (shift < 0.025) verdict = 'good';
      else if (shift < 0.06) verdict = 'inaccuracy';
      else if (shift < 0.14) verdict = 'mistake';
      else verdict = 'blunder';
    }

    return {
      prev,
      node,
      path,
      verdict,
      best: best ? {
        uci: best,
        san: pv2san(root.data.game.variant.key, prev.fen, false, [best])
      } : undefined
    };
  }

  function isMyTurn(): boolean {
    return root.turnColor() === root.bottomColor();
  };

  function checkCeval() {
    const node = root.node;
    if (!running()) {
      comment(null);
      return root.redraw();
    }
    ensureCevalRunning();
    if (isMyTurn()) {
      const h = hinting();
      if (h && node.ceval) {
        h.uci = node.ceval.pvs[0].moves[0];
        root.setAutoShapes();
      }
    } else {
      comment(null);
      if (node.san && commentable(node)) {
        const parentNode = root.tree.nodeAtPath(treePath.init(root.path));
        if (commentable(parentNode, +1))
        comment(makeComment(parentNode, node, root.path));
      }
      if (!played() && playable(node)) {
        root.playUci(node.ceval!.pvs[0].moves[0]);
        played(true);
      }
    }
  };

  function resume() {
    running(true);
    checkCeval();
  }

  window.lichess.requestIdleCallback(checkCeval);

  return {
    onCeval: checkCeval,
    onJump() {
      played(false);
      hinting(null);
      detectThreefold(root.nodeList, root.node);
      checkCeval();
    },
    isMyTurn,
    comment,
    running,
    hovering,
    hinting,
    resume,
    playableDepth,
    reset() {
      comment(null);
      hinting(null);
    },
    preUserJump(from: Ply, to: Ply) {
      if (from !== to) {
        running(false);
        comment(null);
      }
    },
    postUserJump(from: Ply, to: Ply) {
      if (from !== to && isMyTurn()) resume();
    },
    onUserMove() {
      running(true);
    },
    playCommentBest() {
      const c = comment();
      if (!c) return;
      root.jump(treePath.init(c.path));
      if (c.best) root.playUci(c.best.uci);
    },
    commentShape(enable: boolean) {
      const c = comment();
      if (!enable || !c || !c.best) hovering(null);
      else hovering({
        uci: c.best.uci
      });
      root.setAutoShapes();
    },
    hint() {
      const best = root.node.ceval ? root.node.ceval.pvs[0].moves[0] : null;
      const prev = hinting();
      if (!best || (prev && prev.mode === 'move')) hinting(null);
      else hinting({
        mode: prev ? 'move' : 'piece',
        uci: best
      });
      root.setAutoShapes();
    },
    currentNode: () => root.node,
    bottomColor: root.bottomColor,
    redraw: root.redraw
  };
};
