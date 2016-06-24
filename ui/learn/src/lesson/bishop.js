var util = require('../util');

module.exports = {
  title: 'The bishop',
  subtitle: 'It moves diagonally.',
  image: util.assetUrl + 'images/learn/pieces/B.svg',
  stages: [{
    goal: 'Go to the castle!',
    fen: '8/8/8/8/8/5B2/8/8 w - - 0 1',
    items: {
      b7: 'flower'
    },
    nbMoves: 1,
    shapes: [{
      brush: 'paleGreen',
      orig: 'f3',
      dest: 'b7'
    }]
  }, {
    goal: 'Grab the star,<br>then go to the castle!',
    fen: '8/8/8/8/8/8/8/2B5 w - - 0 1',
    items: {
      h6: 'apple',
      a1: 'flower'
    },
    nbMoves: 3
  }, {
    goal: 'Grab the stars,<br>then go to the castle!',
    fen: '4B3/8/8/8/8/8/8/8 w - - 0 1',
    items: {
      a4: 'apple',
      b7: 'apple',
      c6: 'apple',
      d1: 'apple',
      h5: 'apple',
      a8: 'flower'
    },
    nbMoves: 6
  }, {
    goal: 'You need two bishops<br>to control all the squares!',
    fen: '8/8/8/8/8/8/8/2b2b2 b - - 0 1',
    items: {
      c4: 'apple',
      c5: 'apple',
      d3: 'apple',
      d4: 'apple',
      d5: 'apple',
      d6: 'apple',
      e3: 'apple',
      e4: 'apple',
      e5: 'apple',
      e6: 'apple',
      f4: 'apple',
      f5: 'apple',
      a1: 'flower'
    },
    nbMoves: 13
  }].map(util.toStage),
  complete: 'Congratulations! You can command a bishop.'
};
