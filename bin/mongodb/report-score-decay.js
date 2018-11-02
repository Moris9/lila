const atomMinScore = 5;

db.report2.find(
  {open:true,room:'cheat','atoms.score':{$gt: atomMinScore}}, // selector
  {score:1,'atoms.score':1,'atoms.initScore':1,'atoms.at':1} // projection
).forEach(report => {

  // what to update in the report
  const set = {
    score: 0
  };

  report.atoms.forEach((atom, index) => {
    if (!atom.initScore) {
      // save original score if not yet present
      atom.initScore = set[`atoms.${index}.initScore`] = atom.score;
    }
    atom.score = set[`atoms.${index}.score`] = newScore(atom);
    set.score += atom.score;
  });

  if (set.score != report.score) {
    db.report2.update({ _id: report._id}, {$set:set});
  }
});

function newScore(atom) {
  const minScore = Math.min(atom.initScore, atomMinScore);
  return Math.max(minScore, atom.initScore - scoreDecay(atom.at));
}

function scoreDecay(date) {
  return daysSince(date);
}
function daysSince(date) {
  return Math.floor((new Date().getTime()  - date.getTime() ) / 86400000);
}
