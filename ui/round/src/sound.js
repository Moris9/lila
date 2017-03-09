var throttle = require('common').throttle;

function throttled(sound) {
  return throttle(100, false, function() {
    lichess.sound[sound]()
  });
}

module.exports = {
  move: throttled('move'),
  capture: throttled('capture'),
  check: throttled('check'),
  explode: throttled('explode')
}
