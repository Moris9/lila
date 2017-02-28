var m = require('mithril');
var storedProp = require('common').storedProp;
var partial = require('chessground').util.partial;
var xhr = require('./studyXhr');
var dialog = require('./dialog');
var tours = require('./studyTour');

var modeChoices = [
  ['normal', "Normal analysis"],
  ['practice', "Practice with computer"],
  ['conceal', "Hide next moves"]
];
var fieldValue = function(e, id) {
  var el = e.target.querySelector('#chapter-' + id);
  return el ? el.value : null;
};

module.exports = {
  modeChoices: modeChoices,
  fieldValue: fieldValue,
  ctrl: function(send, chapters, setTab, root) {

    var multiPgnMax = 20;

    var vm = {
      variants: [],
      open: false,
      initial: m.prop(false),
      tab: storedProp('study.form.tab', 'init'),
      editor: null,
      editorFen: m.prop(null)
    };

    var loadVariants = function() {
      if (!vm.variants.length) xhr.variants().then(function(vs) {
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
              variant: d.variant
            };
          }));
          return true;
        }
      }
    };

    var submitSingle = function(d) {
      d.initial = vm.initial();
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
      startTour: partial(tours.chapter, vm.tab),
      multiPgnMax: multiPgnMax
    }
  },
  view: function(ctrl) {

    var activeTab = ctrl.vm.tab();
    var makeTab = function(key, name, title) {
      return m('a.hint--top', {
        class: key + (activeTab === key ? ' active' : ''),
        'data-hint': title,
        onclick: partial(ctrl.vm.tab, key),
      }, name);
    };
    var gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
    var currentChapterSetup = ctrl.root.study.data.chapter.setup;

    return dialog.form({
      onClose: ctrl.close,
      content: [
        activeTab === 'edit' ? null : m('h2', [
          'New chapter',
          m('i.help', {
            'data-icon': '',
            onclick: ctrl.startTour
          })
        ]),
        m('form.chapter_form.material.form', {
          onsubmit: function(e) {
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
          }
        }, [
          m('div.form-group', [
            m('input#chapter-name', {
              required: true,
              minlength: 2,
              maxlength: 80,
              config: function(el, isUpdate) {
                if (!isUpdate && !el.value) {
                  el.value = 'Chapter ' + (ctrl.vm.initial() ? 1 : (ctrl.chapters().length + 1));
                  el.select();
                  el.focus();
                }
              }
            }),
            m('label.control-label[for=chapter-name]', 'Name'),
            m('i.bar')
          ]),
          m('div.study_tabs', [
            makeTab('init', 'Init', 'Start from initial position'),
            makeTab('edit', 'Edit', 'Start from custom position'),
            makeTab('game', 'URL', 'Load a game URL'),
            makeTab('fen', 'FEN', 'Load a FEN position'),
            makeTab('pgn', 'PGN', 'Load a PGN game')
          ]),
          activeTab === 'edit' ? m('div.editor_wrap.is2d', {
            config: function(el, isUpdate, ctx) {
              if (isUpdate) return;
              $.when(
                lichess.loadScript('/assets/compiled/lichess.editor.min.js'),
                $.get('/editor.json', {
                  fen: ctrl.root.vm.node.fen
                })
              ).then(function(a, b) {
                var data = b[0];
                data.embed = true;
                data.options = {
                  inlineCastling: true,
                  onChange: ctrl.vm.editorFen
                };
                ctrl.vm.editor = LichessEditor(el, data);
                ctrl.vm.editorFen(ctrl.vm.editor.getFen());
              });
              ctx.onunload = function() {
                ctrl.vm.editor = null;
              }
            }
          }, m.trust(lichess.spinnerHtml)) : null,
          activeTab === 'game' ? m('div.form-group', [
            m('input#chapter-game', {
              placeholder: 'URL of the game'
            }),
            m('label.control-label[for=chapter-game]', 'Load a game from lichess.org or chessgames.com'),
            m('i.bar')
          ]) : null,
          activeTab === 'fen' ? m('div.form-group.no-label', [
            m('input#chapter-fen', {
              placeholder: 'Initial FEN position'
            }),
            m('i.bar')
          ]) : null,
          activeTab === 'pgn' ? m('div.form-group.no-label', [
            m('textarea#chapter-pgn', {
              placeholder: 'Paste your PGN here, up to ' + ctrl.multiPgnMax + ' games'
            }),
            m('i.bar')
          ]) : null,
          m('div', [
            m('div.form-group.half', [
              m('select#chapter-variant', {
                  disabled: gameOrPgn
                }, gameOrPgn ? [
                  m('option', 'Automatic')
                ] :
                ctrl.vm.variants.map(function(v) {
                  return m('option', {
                    value: v.key,
                    selected: v.key === currentChapterSetup.variant.key
                  }, v.name)
                })),
              m('label.control-label[for=chapter-variant]', 'Variant'),
              m('i.bar')
            ]),
            m('div.form-group.half', [
              m('select#chapter-orientation', {
                onchange: function(e) {
                  ctrl.vm.editor && ctrl.vm.editor.setOrientation(e.target.value);
                }
              }, ['White', 'Black'].map(function(color) {
                var c = color.toLowerCase();
                return m('option', {
                  value: c,
                  selected: c === currentChapterSetup.orientation
                }, color)
              })),
              m('label.control-label[for=chapter-orientation]', 'Orientation'),
              m('i.bar')
            ])
          ]),
          m('div.form-group', [
            m('select#chapter-mode', modeChoices.map(function(c) {
              return m('option', {
                value: c[0]
              }, c[1])
            })),
            m('label.control-label[for=chapter-mode]', 'Analysis mode'),
            m('i.bar')
          ]),
          dialog.button('Create chapter')
        ])
      ]
    });
  }
};
