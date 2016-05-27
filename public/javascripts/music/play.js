function lichessPlayMusic() {

  var orchestra;

  lichess.loadScript('/assets/javascripts/music/orchestra.js').then(function() {
    orchestra = lichessOrchestra();
    $.sound.disable();
  });

  var isPawn = function(san) {
    return san[0] !== san[0].toLowerCase();
  };

  var hasCastle = function(san) {
    return san.indexOf('O-O') === 0;
  };
  var hasCheck = function(san) {
    return san.indexOf('+') !== -1;
  };
  var hasMate = function(san) {
    return san.indexOf('#') !== -1;
  };
  var hasCapture = function(san) {
    return san.indexOf('x') !== -1;
  };

  // a -> 0
  // c -> 2
  var fileToInt = function(file) {
    return 'abcdefgh'.indexOf(file);
  };

  // c7 = 2 * 8 + 7 = 23
  var keyToInt = function(key) {
    return fileToInt(key[0]) * 8 + parseInt(key[1]) - 1;
  };

  var uciBase = 64;
  var uciToInt = function(uci) {
    return keyToInt(uci.slice(2));
  };

  var uciToPitch = function(uci) {
    return uciToInt(uci) / (uciBase / 23)
  };

  var jump = function(node) {
    if (node.san) {
      var pitch = uciToPitch(node.uci);
      orchestra.play(isPawn(node.san) ? 'clav' : 'celesta', pitch);
      if (hasCastle(node.san)) orchestra.play('swells', pitch);
      else if (hasCheck(node.san)) orchestra.play('swells', pitch);
      else if (hasCapture(node.san)) orchestra.play('swells', pitch);
      else if (hasMate(node.san)) orchestra.play('swells', pitch);
    } else {
      orchestra.play('swells', 0);
    }
  };

  return {
    jump: function(node) {
      if (!orchestra) return;
      jump(node);
    }
  };
};
