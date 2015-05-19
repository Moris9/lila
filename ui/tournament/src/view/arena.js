var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var button = require('./button');

function scoreTag(s) {
  return {
    tag: 'span',
    attrs: {
      class: s[1] || ''
    },
    children: [s[0]]
  };
}

function playerTrs(ctrl, maxScore, player) {
  return [{
    tag: 'tr',
    attrs: {
      key: player.id,
      class: ctrl.userId === player.id ? 'me' : ''
    },
    children: [
      m('td.name', [
        player.withdraw ? m('span', {
          'data-icon': 'b',
          'title': ctrl.trans('withdraw')
        }) : (
          (ctrl.data.isFinished && player.rank === 1) ? m('span', {
            'data-icon': 'g',
            'title': ctrl.trans('winner')
          }) : m('span.rank', player.rank)),
        util.player(player)
      ]),
      m('td.sheet', player.sheet.scores.map(scoreTag)),
      m('td.total',
        m('strong',
          player.sheet.fire ? {
            class: 'is-gold',
            'data-icon': 'Q'
          } : {}, player.sheet.total))
    ]
  }, {
    tag: 'tr',
    attrs: {
      key: player.id + '.bar'
    },
    children: [
      m('td', {
        class: 'around-bar',
        colspan: 3
      }, m('div', {
        class: 'bar',
        style: {
          width: Math.ceil(player.sheet.total * 100 / maxScore) + '%'
        }
      }))
    ]
  }];
}

var trophy = m('div.trophy');

module.exports = {
  podium: function(ctrl) {
    return m('div.podium', [
      ctrl.data.players.filter(function(p) {
        return p.rank === 1;
      }).map(function(p) {
        return m('div.first', [
          trophy,
          util.player(p),
          m('p', '8 wins'),
          m('p', '8 draws'),
          m('p', '8 losses'),
          m('p', '8 berserks')
        ]);
      })
    ]);
  },
  standing: function(ctrl) {
    var maxScore = Math.max.apply(Math, ctrl.data.players.map(function(p) {
      return p.sheet.total;
    }));
    return [
      m('thead',
        m('tr', [
          m('th.large', [
            ctrl.trans('standing') + ' (' + ctrl.data.players.length + ')'
          ]),
          m('th.legend[colspan=2]', [
            m('span.streakstarter', 'Streak starter'),
            m('span.double', 'Double points'),
            button.joinWithdraw(ctrl)
          ])
        ])),
      m('tbody', ctrl.data.players.map(partial(playerTrs, ctrl, maxScore)))
    ];
  }
};
