var m = require('mithril');
var util = require('../util');
var assert = require('../assert');
var arrow = util.arrow;

var imgUrl = util.assetUrl + 'images/learn/bowman.svg';

module.exports = {
  key: 'capture',
  title: 'Capture',
  subtitle: 'Take the enemy pieces',
  image: imgUrl,
  intro: 'You are ready for combat! In this level, we will be capturing enemy pieces.',
  illustration: util.roundSvg(imgUrl),
  levels: [{ // rook
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '8/2p2p2/8/8/8/2R5/8/8 w - -',
    nbMoves: 2,
    captures: 2,
    shapes: [arrow('c3c7'), arrow('c7f7')],
    success: assert.extinct('black')
  }, { // bishop
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '8/5r2/8/1r3p2/8/3B4/8/8 w - -',
    nbMoves: 5,
    captures: 3,
    success: assert.extinct('black')
  }, { // queen
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '8/5b2/5p2/3n2p1/8/6Q1/8/8 w - -',
    nbMoves: 7,
    captures: 4,
    success: assert.extinct('white')
  }, { // knight
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '8/3b4/2p2q2/8/3p1N2/8/8/8 w - -',
    nbMoves: 6,
    captures: 4,
    success: assert.extinct('white')
  }, {
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '2n1b3/4pp2/5Q2/2R5/8/8/8/8 w - -',
    nbMoves: 5,
    captures: 4,
    success: assert.extinct('black')
  }, {
    goal: 'Take the black pieces!<br>And don\'t lose yours.',
    fen: '2n5/8/2B3b1/3p4/4p3/8/2q1R3/8 w - -',
    nbMoves: 6,
    captures: 5,
    success: assert.extinct('black')
  }].map(function(l, i) {
    l.pointsForCapture = true;
    return util.toLevel(l, i);
  }),
  complete: 'Congratulations! You know how to fight with chess pieces!'
};
