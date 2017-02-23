var m = require('mithril');

function player(p) {
  return {
    tag: 'a',
    attrs: {
      class: 'text ulpt user_link online',
      href: '/@/' + p.username
    },
    children: [
      m('i.line' + (p.patron ? '.patron' : '')),
      (p.title ? p.title + ' ' : '') + p.username,
      p.rating ? m('em', p.rating + (p.provisional ? '?' : '')) : null
    ]
  };
}

module.exports = {
  secondsFromNow: function(seconds) {
    var time = moment().add(seconds, 'seconds');
    return m('time.moment-from-now', {
      datetime: time.format()
    }, time.fromNow());
  },
  title: function(ctrl) {
    return m('h1.text[data-icon=|]', [
      ctrl.data.fullName,
      m('span.author', [
        'by ',
        player(ctrl.data.host)
      ])
    ]);
  },
  player: player,
  playerVariant: function(ctrl, p) {
    return ctrl.data.variants.find(function(v) {
      return v.key === p.variant;
    });
  }
};
