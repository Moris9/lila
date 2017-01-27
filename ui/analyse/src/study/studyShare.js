var m = require('mithril');
var dialog = require('./dialog');
var renderIndexAndMove = require('../moveView').renderIndexAndMove;

var baseUrl = 'https://lichess.org/study/';

function fromPly(ctrl) {
  var node = ctrl.currentNode();
  return m('div.ply-wrap', m('label.ply', [
    m('input[type=checkbox]', {
      onchange: m.withAttr("checked", ctrl.withPly)
    }),
    'Start at ',
    m('strong', renderIndexAndMove({
      withDots: true,
    }, node))
  ]));
}

module.exports = {
  ctrl: function(data, currentChapter, currentNode) {
    var open = m.prop(false);
    var withPly = m.prop(false);
    return {
      open: open,
      toggle: function() {
        open(!open());
      },
      studyId: data.id,
      chapter: currentChapter,
      isPublic: function() {
        return data.visibility === 'public';
      },
      currentNode: currentNode,
      withPly: withPly
    }
  },
  view: function(ctrl) {
    if (!ctrl.open()) return;
    var studyId = ctrl.studyId;
    var chapter = ctrl.chapter();
    var fullUrl = baseUrl + studyId + '/' + chapter.id;
    var embedUrl = baseUrl + 'embed/' + studyId + '/' + chapter.id;
    if (ctrl.withPly()) {
      var p = ctrl.currentNode().ply;
      fullUrl += '#' + p;
      embedUrl += '#' + p;
    }
    return dialog.form({
      onClose: function() {
        ctrl.open(false);
      },
      content: [
        m('h2', 'Share & export'),
        m('form.material.form.share', [
          m('div.form-group', [
            m('input.has-value.autoselect', {
              readonly: true,
              value: baseUrl + studyId
            }),
            m('label.control-label', 'Study URL'),
            m('i.bar')
          ]),
          m('div.form-group', [
            m('input.has-value.autoselect', {
              readonly: true,
              value: fullUrl
            }),
            fromPly(ctrl),
            m('p.form-help.text', {
              'data-icon': ''
            }, 'You can paste this in the forum to embed the chapter.'),
            m('label.control-label', 'Current chapter URL'),
            m('i.bar')
          ]),
          m('div.form-group', [
            m('input.has-value.autoselect', {
              readonly: true,
              disabled: !ctrl.isPublic(),
              value: ctrl.isPublic() ? '<iframe width=600 height=371 src="' + embedUrl + '" frameborder=0></iframe>' : 'Only public studies can be embedded.'
            }),
            fromPly(ctrl),
            m('a.form-help.text', {
              href: '/developers#embed-study',
              target: '_blank',
              'data-icon': ''
            }, 'Read more about embedding a study chapter'),
            m('label.control-label', 'Embed current chapter in your website or blog'),
            m('i.bar')
          ]),
          m('div.downloads', [
            m('a.button.text.hint--top', {
              'data-icon': 'x',
              href: '/study/' + studyId + '.pgn'
            }, 'Study PGN'),
            m('a.button.text.hint--top', {
              'data-icon': 'x',
              href: '/study/' + studyId + '/' + chapter.id + '.pgn'
            }, 'Current chapter PGN')
          ])
        ])
      ]
    });
  }
};
