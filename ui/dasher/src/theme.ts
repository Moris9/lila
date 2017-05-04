import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind, header } from './util'

type Theme = string;

interface ThemeDimData {
  current: Theme
  list: Theme[]
}

export interface ThemeData {
  d2: ThemeDimData
  d3: ThemeDimData
}

export interface ThemeCtrl {
  dimension: () => keyof ThemeData
  data: () => ThemeDimData
  set(t: Theme): void
  close: Close
}

export function ctrl(data: ThemeData, dimension: () => keyof ThemeData, redraw: Redraw, close: Close): ThemeCtrl {

  function dimensionData() {
    return data[dimension()];
  }

  return {
    dimension,
    data: dimensionData,
    set(t: Theme) {
      const d = dimensionData();
      d.current = t;
      applyTheme(t, d.list);
      $.post('/pref/theme' + (dimension() === 'd3' ? '3d' : ''), {
        theme: t
      }, window.lichess.reloadOtherTabs);
      redraw();
    },
    close
  };
}

export function view(ctrl: ThemeCtrl): VNode {

  const d = ctrl.data();

  return h('div.sub.theme.' + ctrl.dimension(), [
    header('Theme', ctrl.close),
    h('div.list', {
      attrs: { method: 'post', action: '/pref/soundSet' }
    }, d.list.map(themeView(d.current, ctrl.set)))
  ]);
}

function themeView(current: Theme, set: (t: Theme) => void) {
  return (t: Theme) => h('a', {
    hook: bind('click', () => set(t)),
    class: { active: current === t }
  }, [
    h('span.' + t)
  ]);
}

function applyTheme(t: Theme, list: Theme[]) {
  $('body').removeClass(list.join(' ')).addClass(t);
}
