var compact = require('lodash-node/modern/arrays/compact')
var first = require('lodash-node/modern/arrays/first')
var pairs = require('lodash-node/modern/objects/pairs')
var chessground = require('chessground');
var chess = require('./chess');

function str2move(m) {
  return m ? [m.slice(0, 2), m.slice(2, 4), m[4]] : null;
}

function move2str(m) {
  return m.join('');
}

function getPath(obj, ks) {
  if (!obj) return null;
  if (ks.length === 0) return obj;
  return getPath(obj[ks[0]], ks.slice(1));
}

function tryMove(data, move) {
  var tryM = function(m) {
    var newProgress = data.progress.concat([move2str(m)]);
    var newLines = getPath(data.puzzle.lines, newProgress);
    return newLines ? [newProgress, newLines] : false;
  }
  var moves = [null, 'q', 'n', 'r', 'b'].map(function(promotion) {
    return move.concat([promotion]);
  });
  var tries = compact(moves.map(tryM));
  var success = first(tries.filter(function(t) {
    return t[1] != 'retry';
  }));
  if (success) return success;
  if (first(tries)) return first(tries);
  return [data.progress, 'fail'];
}

function getCurrentLines(data) {
  return getPath(data.puzzle.lines, data.progress);
}

function getOpponentNextMove(data) {
  return first(first(pairs(getCurrentLines(data))));
}

module.exports = {
  str2move: str2move,
  move2str: move2str,
  tryMove: tryMove,
  getCurrentLines: getCurrentLines,
  getOpponentNextMove: getOpponentNextMove
};
