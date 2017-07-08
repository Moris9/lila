import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { Hooks } from 'snabbdom/hooks'

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return {
    insert(vnode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        const res = f(e);
        if (redraw) redraw();
        return res;
      });
    }
  };
}

export function miniBoard(game) {
  return h('a.mini_board.parse_fen.is2d.live_' + game.id, {
    key: game.id,
    attrs: {
      href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
      'data-color': game.color,
      'data-fen': game.fen,
      'data-lastmove': game.lastMove
    },
    hook: {
      insert(vnode) {
        window.lichess.parseFen($(vnode.elm as HTMLElement));
      }
    }
  }, [
    h('div.cg-board-wrap', h('div.cg-board'))
  ]);
}

export function ratio2percent(r: number) {
  return Math.round(100 * r) + '%';
}

export function player(p, asLink?: boolean) {
  let ratingDiff;
  if (p.ratingDiff > 0) ratingDiff = h('span.positive', {
    attrs: { 'data-icon': 'N' }
  }, '' + p.ratingDiff);
  else if (p.ratingDiff < 0) ratingDiff = h('span.negative', {
    attrs: { 'data-icon': 'M' }
  }, '' + -p.ratingDiff);
  const rating = p.rating + p.ratingDiff + (p.provisional ? '?' : ''),
  fullName = (p.title ? p.title + ' ' : '') + p.name;

  return h('a.ulpt.user_link' + (fullName.length > 15 ? '.long' : ''), {
    attrs: asLink ? { href: '/@/' + p.name } : { 'data-href': '/@/' + p.name },
    hook: {
      destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement)
    }
  }, [
    h('span.name', fullName),
    h('span.progress', [rating, ratingDiff])
  ]);
}

export function numberRow(name: string, value, typ?: string) {
  return h('tr', [h('th', name), h('td',
    typ === 'raw' ? value : (typ === 'percent' ? (
      value[1] > 0 ? ratio2percent(value[0] / value[1]) : 0
    ) : window.lichess.numberFormat(value))
  )]);
}

export function spinner(): VNode {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
