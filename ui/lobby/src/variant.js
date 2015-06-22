var variantConfirms = {
  'chess960': "This is a Chess960 game!\n\nThe starting position of the pieces on the players' home ranks is randomized.\nRead more: http://wikipedia.org/wiki/Chess960\n\nDo you want to play Chess960?",
  'kingOfTheHill': "This is a King of the Hill game!\n\nThe game can be won by bringing the king to the center.\nRead more: http://lichess.org/king-of-the-hill",
  'threeCheck': "This is a Three-check game!\n\nThe game can be won by checking the opponent 3 times.\nRead more: http://en.wikipedia.org/wiki/Three-check_chess",
  "antichess": "This is an antichess chess game!\n\n If you can take a piece, you must. The game can be won by losing all your pieces.",
  "atomic": "This is an atomic chess game!\n\n. Capturing a piece causes an explosion, taking out your piece and surrounding non-pawns. Win by mating or exploding your opponent's king.",
  "horde": "This is a horde chess game!\n\nWhite must take all black pawns to win. Black must checkmate white king."
};

function storageKey(key) {
  return 'lobby.variant.' + key;
}

module.exports = {
  confirm: function(variant) {
    return Object.keys(variantConfirms).every(function(key) {
      var v = variantConfirms[key]
      if (variant.key === key && !lichess.storage.get(storageKey(key))) {
        var c = confirm(v);
        if (c) lichess.storage.set(storageKey(key), 1);
        return c;
      } else return true;
    })
  }
};
