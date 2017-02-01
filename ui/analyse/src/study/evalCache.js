var throttle = require('common').throttle;

var evalPutMinDepth = 20;
var evalPutMinNodes = 4e6;
// var evalPutMinDepth = 16;
// var evalPutMinNodes = 1e6;
var evalPutMaxMoves = 8;

function makeEvalPutData(eval) {
  return {
    fen: eval.fen,
    nodes: eval.nodes,
    depth: eval.depth,
    pvs: eval.pvs.map(function(pv) {
      return {
        cp: pv.cp,
        mate: pv.mate,
        moves: pv.pv.split(' ').slice(0, evalPutMaxMoves).join(' ')
      };
    })
  };
}

module.exports = function(opts) {
  return {
    onCeval: throttle(1000, false, function(eval) {
      if (opts.enabled() && eval.depth >= evalPutMinDepth && eval.nodes > evalPutMinNodes)
        opts.send("evalPut", makeEvalPutData(eval));
    }),
    mutateAnaDestsReq: function(req) {
      if (opts.getCeval().enabled()) req.multiPv = parseInt(opts.getCeval().multiPv());
    }
  };
};
