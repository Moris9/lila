var util = require('../util');

var categs = [{
  name: 'Chess pieces',
  stages: [
    require('./rook'),
    require('./bishop'),
    require('./queen'),
    require('./king'),
    require('./knight'),
    require('./pawn'),
  ]
}, {
  name: 'Fundamentals',
  stages: [
    require('./capture'),
    require('./check1'),
    require('./outOfCheck.js'),
    require('./checkmate1'),
  ]
}, {
  name: 'Intermediate',
  stages: [
    require('./setup'),
    require('./castling'),
    require('./enpassant'),
    require('./stalemate'),
  ]
}, {
  name: 'Advanced',
  stages: [
    require('./value'),
    require('./check2'),
  ]
}];

var stageId = 1;
var stages = [];

categs = categs.map(function(c) {
  c.stages = c.stages.map(function(stage) {
    stage.id = stageId++;
    stages.push(stage);
    return stage;
  });
  return c;
});

var stagesByKey = {}
stages.forEach(function(s) {
  stagesByKey[s.key] = s;
});

var stagesById = {}
stages.forEach(function(s) {
  stagesById[s.id] = s;
});

module.exports = {
  list: stages,
  byId: stagesById,
  byKey: stagesByKey,
  categs: categs,
  stageIdToCategId: function(stageId) {
    var stage = stagesById[stageId];
    for (var id in categs)
      if (categs[id].stages.some(function(s) {
        return s.key === stage.key;
      })) return id;
  }
};
