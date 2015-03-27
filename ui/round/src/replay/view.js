var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var game = require('game').game;
var status = require('game').status;
var renderStatus = require('../view/status');
var m = require('mithril');

function renderTd(move, ply, curPly) {
  return move ? {
    tag: 'td',
    attrs: {
      class: 'move' + (ply === curPly ? ' active' : ''),
      'data-ply': ply
    },
    children: [move]
  } : null;
}

function renderResult(ctrl, asTable) {
  var result;
  if (status.finished(ctrl.root.data)) switch (ctrl.root.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  if (result || status.aborted(ctrl.root.data)) {
    var winner = game.getPlayer(ctrl.root.data, ctrl.root.data.game.winner);
    return asTable ? [
      m('tr', m('td.result[colspan=3]', result)),
      m('tr.status', m('td[colspan=3]', [
        renderStatus(ctrl.root),
        winner ? ', ' + ctrl.root.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ]))
    ] : [
      m('p.result', result),
      m('p.status', [
        renderStatus(ctrl.root),
        winner ? ', ' + ctrl.root.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
      ])
    ];
  }
}

function renderTable(ctrl, curPly) {
  var moves = ctrl.root.data.game.moves;
  var pairs = [];
  for (var i = 0; i < moves.length; i += 2) pairs.push([moves[i], moves[i + 1]]);
  var trs = pairs.map(function(pair, i) {
    return m('tr', [
      m('td.index', i + 1),
      renderTd(pair[0], 2 * i + 1, curPly),
      renderTd(pair[1], 2 * i + 2, curPly)
    ]);
  }).concat(renderResult(ctrl, true));
  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply');
          if (ply) ctrl.jump(parseInt(ply));
        }
      },
      trs));
}

function renderButtons(ctrl, curPly) {
  var root = ctrl.root;
  var nbMoves = root.data.game.moves.length;
  var flipAttrs = {
    class: 'button flip hint--top' + (root.vm.flip ? ' active' : ''),
    'data-hint': root.trans('flipBoard'),
  };
  if (root.data.tv) flipAttrs.href = '/tv' + (root.data.tv.flip ? '' : '?flip=1');
  else if (root.data.player.spectator) flipAttrs.href = root.router.Round.watcher(root.data.game.id, root.data.opponent.color).url;
  else flipAttrs.onclick = root.flip;
  return m('div.buttons', [
    m('a', flipAttrs, m('span[data-icon=B]')), [
      ['first', 'W', 1],
      ['prev', 'Y', curPly - 1],
      ['next', 'X', curPly + 1],
      ['last', 'V', nbMoves]
    ].map(function(b) {
      var enabled = curPly != b[2] && b[2] >= 1 && b[2] <= nbMoves;
      return m('a', {
        class: 'button ' + b[0] + ' ' + classSet({
          disabled: (ctrl.broken || !enabled),
          glowing: ctrl.vm.late && b[0] === 'last'
        }),
        'data-icon': b[1],
        onclick: enabled ? partial(ctrl.jump, b[2]) : null
      });
    }), (game.userAnalysable(root.data) ? m('a', {
      class: 'button hint--top analysis',
      'data-hint': root.trans('analysis'),
      href: root.router.UserAnalysis.game(root.data.game.id, root.data.player.color).url,
    }, m('span[data-icon="A"]')) : null)
  ]);
}

function autoScroll(movelist) {
  var plyEl = movelist.querySelector('.active');
  if (plyEl) movelist.scrollTop = plyEl.offsetTop - movelist.offsetHeight / 2 + plyEl.offsetHeight / 2;
}

module.exports = function(ctrl) {
  var curPly = ctrl.active ? ctrl.ply : ctrl.root.data.game.moves.length;
  var h = curPly + ctrl.root.data.game.moves.join('') + ctrl.root.vm.flip;
  if (ctrl.vm.hash === h) return {
    subtree: 'retain'
  };
  ctrl.vm.hash = h;
  return m('div.replay', [
    renderButtons(ctrl, curPly),
    ctrl.enabledByPref() ? m('div.moves', {
      config: function(el, isUpdate) {
        autoScroll(el);
        if (!isUpdate) setTimeout(partial(autoScroll, el), 100);
      },
    }, renderTable(ctrl, curPly)) : renderResult(ctrl, false)
  ]);
}
