var m = require('mithril');
var classSet = require('common').classSet;
var util = require('./util');
var ratio2percent = util.ratio2percent;
var button = require('./button');
var pagination = require('../pagination');

var scoreTagNames = ['score', 'streak', 'double'];

function scoreTag(s) {
  return {
    tag: scoreTagNames[(s[1] || 1) - 1],
    children: [Array.isArray(s) ? s[0] : s]
  };
}

function scoreTags(scores) {
  return scores.map(scoreTag);
}

// function scoreTags(scores) {
//   var tags = [];
//   var current = {
//     tagName: null,
//     index: -1,
//     count: 0
//   };
//   for (var i in scores) {
//     var s = scores[i];
//     var tagName = scoreTagNames[(s[1] || 1) - 1];
//     var content = Array.isArray(s) ? s[0] : s;
//     if (tagName === current.tagName && current.count < 50) {
//       tags[current.index].children += content;
//       ++current.count;
//     }
//     else {
//       current = {
//         tagName: tagName,
//         index: current.index + 1,
//         count: 1
//       };
//       tags[current.index] = {
//         tag: tagName,
//         children: '' + content
//       };
//     }
//   }
//   return tags;
// }

function playerTr(ctrl, player) {
  var userId = player.name.toLowerCase();
  return {
    tag: 'tr',
    attrs: {
      key: userId,
      class: classSet({
        'me': ctrl.userId === userId,
        'long': player.sheet.scores.length > 35,
        'xlong': player.sheet.scores.length > 80
      }),
      onclick: lichess.partial(ctrl.showPlayerInfo, player)
    },
    children: [
      m('td.rank', player.withdraw ? m('i', {
        'data-icon': 'b',
        'title': ctrl.trans('withdraw')
      }) : player.rank),
      m('td.player', util.player(player, 'span')),
      m('td.sheet', scoreTags(player.sheet.scores)),
      m('td.total', m('strong',
        player.sheet.fire ? {
          class: 'is-gold',
          'data-icon': 'Q'
        } : {}, player.sheet.total))
    ]
  };
}

var trophy = m('div.trophy');

function podiumUsername(p) {
  return m('a', {
    class: 'text ulpt user_link',
    href: '/@/' + p.name
  }, p.name);
}

function podiumStats(p, data) {
  var ratingDiff;
  if (p.ratingDiff === 0) ratingDiff = m('span', ' =');
  else if (p.ratingDiff > 0) ratingDiff = m('span.positive[data-icon=N]', p.ratingDiff);
  else if (p.ratingDiff < 0) ratingDiff = m('span.negative[data-icon=M]', -p.ratingDiff);
  var nb = p.nb;
  return [
    m('span.rating.progress', [
      p.rating + p.ratingDiff,
      ratingDiff
    ]),
    m('table.stats', [
      m('tr', [m('th', 'Games played'), m('td', nb.game)]),
      nb.game ? [
        m('tr', [m('th', 'Win rate'), m('td', ratio2percent(nb.win / nb.game))]),
        m('tr', [m('th', 'Berserk rate'), m('td', ratio2percent(nb.berserk / nb.game))])
      ] : null,
      p.performance ? m('tr', [m('th', 'Performance'), m('td', p.performance)]) : null
    ])
  ];
}

function podiumPosition(p, data, pos) {
  if (p) return m('div.' + pos, [
    trophy,
    podiumUsername(p),
    podiumStats(p, data)
  ]);
}

var lastBody = null;

module.exports = {
  podium: function(ctrl) {
    return m('div.podium', [
      podiumPosition(ctrl.data.podium[1], ctrl.data, 'second'),
      podiumPosition(ctrl.data.podium[0], ctrl.data, 'first'),
      podiumPosition(ctrl.data.podium[2], ctrl.data, 'third')
    ]);
  },
  standing: function(ctrl, pag, klass) {
    var tableBody = pag.currentPageResults ?
      pag.currentPageResults.map(lichess.partial(playerTr, ctrl)) : lastBody;
    if (pag.currentPageResults) lastBody = tableBody;
    return m('div.standing_wrap',
      m('div.controls',
        m('div.pager', pagination.renderPager(ctrl, pag)),
        button.joinWithdraw(ctrl)
      ),
      m('table.slist.standing' + (klass ? '.' + klass : '') + (pag.currentPageResults ? '' : '.loading'), [
        m('tbody', {
          config: function() {
            // reload user badges
            lichess.pubsub.emit('content_loaded')();
          }
        }, tableBody)
      ])
    );
  }
};
