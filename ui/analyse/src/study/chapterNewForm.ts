import { h } from 'snabbdom'
import { storedProp, Prop } from 'common';
import { bind, spinner } from '../util';
import { variants as xhrVariants } from './studyXhr';
import * as dialog from './dialog';
import { chapter as chapterTour } from './studyTour';
import { StudyChapter } from './interfaces';
import AnalyseController from '../ctrl';

export const modeChoices = [
  ['normal', "Normal analysis"],
  ['practice', "Practice with computer"],
  ['conceal', "Hide next moves"]
];

export function fieldValue(e: Event, id: string) {
  const el = (e.target as HTMLElement).querySelector('#chapter-' + id);
  return el ? (el as HTMLInputElement).value : null;
};

export function ctrl(send, chapters: Prop<StudyChapter[]>, setTab: () => void, root: AnalyseController) {

  const multiPgnMax = 20;

  const vm = {
    variants: [],
    open: false,
    initial: m.prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorFen: m.prop(null)
  };

  var loadVariants = function() {
    if (!vm.variants.length) xhrVariants().then(function(vs) {
      vm.variants = vs;
      m.redraw();
    });
  };

  var open = function() {
    vm.open = true;
    loadVariants();
    vm.initial(false);
  };
  var close = function() {
    vm.open = false;
  };

  var identity = function(x) { return x; }

    var submitMultiPgn = function(d) {
      if (d.pgn) {
        var lines = d.pgn.split('\n');
        var parts = lines.map(function(l, i) {
          // ensure 2 spaces after each game
          if (!l.trim() && i && lines[i - 1][0] !== '[') return '\n';
          return l;
        }).join('\n').split('\n\n\n').map(function(part) {
          // remove empty lines in each game
          return part.split('\n').filter(identity).join('\n');
        }).filter(identity); // remove empty games
        if (parts.length > 1) {
          if (parts.length > multiPgnMax && !confirm('Import the first ' + multiPgnMax + ' of the ' + parts.length + ' games?')) return;
          var step = function(ds) {
            if (ds.length) {
              send('addChapter', ds[0]);
              setTimeout(function() {
                step(ds.slice(1));
              }, 600);
            } else {}
          };
          var firstIt = vm.initial() ? 1 : (chapters().length + 1);
          step(parts.slice(0, multiPgnMax).map(function(pgn, i) {
            return {
              initial: !i && vm.initial(),
              mode: d.mode,
              name: 'Chapter ' + (firstIt + i),
              orientation: d.orientation,
              pgn: pgn,
              variant: d.variant,
              sticky: root.study.vm.mode.sticky
            };
          }));
          return true;
        }
      }
    };

    var submitSingle = function(d) {
      d.initial = vm.initial();
      d.sticky = root.study.vm.mode.sticky;
      send("addChapter", d)
    };

    return {
      vm: vm,
      open: open,
      root: root,
      openInitial: function() {
        open();
        vm.initial(true);
      },
      close: close,
      toggle: function() {
        if (vm.open) close();
        else open();
      },
      submit: function(d) {
        if (!submitMultiPgn(d)) submitSingle(d);
        close();
        setTab();
      },
      chapters: chapters,
      startTour: () => chapterTour(tab => {
        vm.tab(tab);
        root.redraw();
      }),
      multiPgnMax: multiPgnMax
    }
}

export function view(ctrl) {

  const activeTab = ctrl.vm.tab();
  const makeTab = function(key: string, name: string, title: string) {
    return h('a.hint--top.' + key, {
      class: { active: activeTab === key },
      attrs: { 'data-hint': title },
      hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw)
    }, name);
  };
  const gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
  const currentChapterSetup = ctrl.root.study.data.chapter.setup;

  return dialog.form({
    onClose: ctrl.close,
    content: [
      activeTab === 'edit' ? null : h('h2', [
        'New chapter',
        h('i.help', {
          attrs: { 'data-icon': '' },
          hook: bind('click', ctrl.startTour)
        })
      ]),
      h('form.chapter_form.material.form', {
        hook: bind('submit', e => {
          ctrl.submit({
            name: fieldValue(e, 'name'),
            game: fieldValue(e, 'game'),
            variant: fieldValue(e, 'variant'),
            fen: fieldValue(e, 'fen') || (activeTab === 'edit' ? ctrl.vm.editorFen() : null),
            pgn: fieldValue(e, 'pgn'),
            orientation: fieldValue(e, 'orientation'),
            mode: fieldValue(e, 'mode')
          });
          e.stopPropagation();
          return false;
        })
      }, [
        h('div.form-group', [
          h('input#chapter-name', {
            attrs: {
              required: true,
              minlength: 2,
              maxlength: 80
            },
            hook: {
              insert: vnode => {
                const el = vnode.elm as HTMLInputElement;
                if (!el.value) {
                  el.value = 'Chapter ' + (ctrl.vm.initial() ? 1 : (ctrl.chapters().length + 1));
                  el.select();
                  el.focus();
                }
              }
            }
          }),
          h('label.control-label', {
            attrs: {for: 'chapter-name' }
          }, 'Name'),
          h('i.bar')
        ]),
        h('div.study_tabs', [
          makeTab('init', 'Init', 'Start from initial position'),
          makeTab('edit', 'Edit', 'Start from custom position'),
          makeTab('game', 'URL', 'Load a game URL'),
          makeTab('fen', 'FEN', 'Load a FEN position'),
          makeTab('pgn', 'PGN', 'Load a PGN game')
        ]),
        activeTab === 'edit' ? h('div.editor_wrap.is2d', {
          hook: {
            insert: vnode => {
              $.when(
                window.lichess.loadScript('/assets/compiled/lichess.editor.min.js'),
                $.get('/editor.json', {
                  fen: ctrl.root.vm.node.fen
                })
              ).then(function(_, b) {
                const data = b[0];
                data.embed = true;
                data.options = {
                  inlineCastling: true,
                  onChange: ctrl.vm.editorFen
                };
                ctrl.vm.editor = window['LichessEditor'](vnode.elm as HTMLElement, data);
                ctrl.vm.editorFen(ctrl.vm.editor.getFen());
              });
            },
            destroy: _ => {
              ctrl.vm.editor = null;
            }
          }
        }, [spinner()]) : null,
        activeTab === 'game' ? h('div.form-group', [
          h('input#chapter-game', {
            attrs: { placeholder: 'URL of the game' }
          }),
          h('label.control-label', {
            attrs: { 'for': 'chapter-game' }
          }, 'Load a game from lichess.org or chessgames.com'),
          h('i.bar')
        ]) : null,
        activeTab === 'fen' ? h('div.form-group.no-label', [
          h('input#chapter-fen', {
            attrs: { placeholder: 'Initial FEN position' }
          }),
          h('i.bar')
        ]) : null,
        activeTab === 'pgn' ? h('div.form-group.no-label', [
          h('textarea#chapter-pgn', {
            attrs: { placeholder: 'Paste your PGN here, up to ' + ctrl.multiPgnMax + ' games' }
          }),
          h('i.bar')
        ]) : null,
        h('div', [
          h('div.form-group.half', [
            h('select#chapter-variant', {
              attrs: { disabled: gameOrPgn }
            }, gameOrPgn ? [
              h('option', 'Automatic')
            ] :
            ctrl.vm.variants.map(v => h('option', {
              attrs: {
                value: v.key,
                selected: v.key === currentChapterSetup.variant.key
              }
            }, v.name))),
            h('label.control-label', {
              attrs: { 'for': 'chapter-variant' }
            }, 'Variant'),
            h('i.bar')
          ]),
          h('div.form-group.half', [
            h('select#chapter-orientation', {
              hook: bind('change', e => {
                ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value);
              })
            }, ['White', 'Black'].map(function(color) {
              const c = color.toLowerCase();
              return h('option', {
                attrs: {
                  value: c,
                  selected: c === currentChapterSetup.orientation
                }
              }, color)
            })),
            h('label.control-label', {
              attrs: { 'for': 'chapter-orientation' }
            }, 'Orientation'),
            h('i.bar')
          ])
        ]),
        h('div.form-group', [
          h('select#chapter-mode', modeChoices.map(c => h('option', {
            attrs: { value: c[0] }
          }, c[1]))),
          h('label.control-label', {
            attrs: { 'for': 'chapter-mode' }
          }, 'Analysis mode'),
          h('i.bar')
        ]),
        dialog.button('Create chapter')
      ])
    ]
  });
}
