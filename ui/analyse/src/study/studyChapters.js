var m = require('mithril');
var classSet = require('chessground').util.classSet;
var partial = require('chessground').util.partial;
var chapterForm = require('./chapterForm');

module.exports = {
  ctrl: function(initChapters, send, setTab, root) {

    var confing = m.prop(null); // which chapter is being configured by us
    var list = m.prop(initChapters);

    var form = chapterForm.ctrl(send, list, setTab, root);

    return {
      confing: confing,
      form: form,
      list: list,
      get: function(id) {
        return list().find(function(c) {
          return c.id === id;
        });
      },
      rename: function(id, name) {
        send("renameChapter", {
          id: id,
          name: name
        });
        confing(null);
      },
      delete: function(id) {
        send("deleteChapter", id);
        confing(null);
      },
      sort: function(ids) {
        send("sortChapters", ids);
      }
    };
  },
  view: {
    main: function(ctrl) {

      var configButton = function(chapter, confing) {
        if (ctrl.members.canContribute()) return m('span.action.config', {
          onclick: function(e) {
            ctrl.chapters.confing(confing ? null : chapter.id);
            e.stopPropagation();
          }
        }, m('i', {
          'data-icon': '%'
        }));
      };

      var chapterConfig = function(chapter) {
        return m('div.config', [
          m('input', {
            config: function(el, isUpdate) {
              if (!isUpdate) {
                if (!el.value) {
                  el.value = chapter.name;
                  el.focus();
                }
                $(el).keypress(function(e) {
                  if (e.which == 10 || e.which == 13)
                    ctrl.chapters.rename(chapter.id, $(this).val());
                });
              }
            }
          }),
          m('div.delete', m('a.button.text[data-icon=q]', {
            onclick: function() {
              if (ctrl.chapters.list().length < 2)
                alert('There cannot be less than one chapter.');
              else if (confirm('Delete  ' + chapter.name + '?'))
                ctrl.chapters.delete(chapter.id);
            }
          }, 'Delete this chapter'))
        ]);
      };

      return [
        m('div', {
          key: 'chapters',
          class: 'list chapters',
          config: function(el, isUpdate, ctx) {
            var newCount = ctrl.chapters.list().length;
            if (!isUpdate || !ctx.count || ctx.count !== newCount)
              $(el).scrollTo($(el).find('.active'), 200);
            ctx.count = newCount;
            if (ctrl.members.canContribute() && newCount > 1 && !ctx.sortable) {
              var makeSortable = function() {
                ctx.sortable = Sortable.create(el, {
                  onSort: function() {
                    ctrl.chapters.sort(ctx.sortable.toArray());
                  }
                });
                ctx.onunload = function() {
                  ctx.sortable.destroy();
                };
              };
              if (window.Sortable) makeSortable();
              else lichess.loadScript('/assets/javascripts/vendor/Sortable.min.js').done(makeSortable);
            }
          }
        }, [
          ctrl.chapters.list().map(function(chapter) {
            var confing = ctrl.chapters.confing() === chapter.id;
            var current = ctrl.currentChapter();
            var active = current && current.id === chapter.id;
            var attrs = {
              key: chapter.id,
              'data-id': chapter.id,
              class: classSet({
                elem: true,
                chapter: true,
                active: active,
                confing: confing
              }),
              onclick: function() {
                ctrl.setChapter(chapter.id);
              }
            };
            return [
              m('div', attrs, [
                m('div.left', [
                  m('div.status', (active && ctrl.vm.loading) ? m.trust(lichess.spinnerHtml) : m('i', {
                    'data-icon': active ? 'J' : 'K'
                  })),
                  chapter.name
                ]),
                configButton(chapter, confing)
              ]),
              confing ? chapterConfig(chapter) : null
            ];
          })
        ]),
        ctrl.members.canContribute() ? m('i.add[data-icon=0]', {
          title: 'New chapter',
          'data-icon': 'O',
          onclick: ctrl.chapters.form.toggle
        }) : null
      ];
    }
  }
};
