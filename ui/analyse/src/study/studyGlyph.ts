import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as xhr from './studyXhr';
import { prop, throttle } from 'common';
import { bind, nodeFullName, spinner } from '../util';
import AnalyseController from '../ctrl';

function renderGlyph(ctrl, node) {
  return function(glyph) {
    return h('a', {
      hook: bind('click', _ => {
        ctrl.toggleGlyph(glyph.id);
        return false;
      }, ctrl.redraw),
      class: {
        active: (node.glyphs && node.glyphs.find(function(g) {
          return g.id === glyph.id;
        }))
      }
    }, [
      h('i', {
        attrs: { 'data-symbol': glyph.symbol }
      }),
      glyph.name
    ]);
  };
}

export function ctrl(root: AnalyseController) {
  const isOpen = prop(false);
  const dirty = prop(true);
  const all = prop<any | null>(null);

  var loadGlyphs = function() {
    if (!all()) xhr.glyphs().then(function(gs) {
      all(gs);
      root.redraw();
    });
  };

  var toggleGlyph = function(id) {
    if (!dirty()) {
      dirty(true);
      root.redraw();
    }
    doToggleGlyph(id);
  };

  var doToggleGlyph = throttle(500, false, function(id) {
    root.study.makeChange('toggleGlyph', root.study.withPosition({
      id: id
    }));
  });

  var open = function() {
    loadGlyphs();
    dirty(true);
    isOpen(true);
  };

  return {
    root,
    all,
    open,
    isOpen,
    dirty,
    toggle() {
      if (isOpen()) isOpen(false);
      else open();
    },
    toggleGlyph,
    redraw: root.redraw
  };
}

export function view(ctrl): VNode | undefined {

  if (!ctrl.isOpen()) return;
  const all = ctrl.all();
  const node = ctrl.root.node;

  return h('div.study_glyph_form.underboard_form', [
    h('p.title', [
      h('button.button.frameless.close', {
        attrs: {
          'data-icon': 'L',
          title: 'Close'
        },
        hook: bind('click', () => ctrl.isOpen(false), ctrl.redraw)
      }),
      'Annotating position after ',
      h('strong', nodeFullName(node)),
      h('span.saved', {
        class: { visible: !ctrl.dirty() }
      }, 'Saved.')
    ]),
    all ? h('div.glyph_form', [
      h('div.move', all.move.map(renderGlyph(ctrl, node))),
      h('div.position', all.position.map(renderGlyph(ctrl, node))),
      h('div.observation', all.observation.map(renderGlyph(ctrl, node)))
    ]) : spinner()
  ]);
}
