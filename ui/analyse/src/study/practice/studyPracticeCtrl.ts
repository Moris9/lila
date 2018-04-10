import * as xhr from '../studyXhr';
import { prop, storedProp } from 'common';
import makeSuccess from './studyPracticeSuccess';
import makeSound from './sound';
import { readOnlyProp } from '../../util';
import { StudyPracticeData, Goal, StudyPracticeCtrl } from './interfaces';
import { StudyData, StudyCtrl } from '../interfaces';
import AnalyseCtrl from '../../ctrl';

export default function(root: AnalyseCtrl, studyData: StudyData, data: StudyPracticeData): StudyPracticeCtrl {

  const goal = prop<Goal>(root.data.practiceGoal!),
  nbMoves = prop(0),
  // null = ongoing, true = win, false = fail
  success = prop<boolean | null>(null),
  sound = makeSound(),
  analysisUrl = prop(''),
  autoNext = storedProp('practice-auto-next', true);

  function onLoad() {
    root.showAutoShapes = readOnlyProp(true);
    root.showGauge = readOnlyProp(true);
    root.showComputer = readOnlyProp(true);
    goal(root.data.practiceGoal!);
    nbMoves(0);
    success(null);
    const chapter = studyData.chapter;
    history.replaceState(null, chapter.name, data.url + '/' + chapter.id);
    analysisUrl('/analysis/standard/' + root.node.fen.replace(/ /g, '_') + '?color=' + root.bottomColor());
  }
  onLoad();

  function computeNbMoves(): number {
    let plies = root.node.ply - root.tree.root.ply;
    if (root.bottomColor() !== root.data.player.color) plies--;
    return Math.ceil(plies / 2);
  }

  function getStudy(): StudyCtrl {
    return root.study!;
  }

  function checkSuccess(): void {
    const gamebook = getStudy().gamebookPlay();
    if (gamebook) {
      if (gamebook.state.feedback === 'end') onVictory();
      return;
    }
    if (!getStudy().data.chapter.practice) {
      return saveNbMoves();
    }
    if (success() !== null) return;
    nbMoves(computeNbMoves());
    const res = success(makeSuccess(root, goal(), nbMoves()));
    if (res) onVictory();
    else if (res === false) onFailure();
  }

  function onVictory(): void {
    saveNbMoves();
    sound.success();
    if (autoNext()) setTimeout(goToNext, 1000);
  }

  function saveNbMoves(): void {
    const chapterId = getStudy().currentChapter().id,
    former = data.completion[chapterId] || 999;
    if (nbMoves() < former) {
      data.completion[chapterId] = nbMoves();
      xhr.practiceComplete(chapterId, nbMoves());
    }
  }

  function goToNext() {
    const next = getStudy().nextChapter();
    if (next) getStudy().setChapter(next.id);
  }

  function onFailure(): void {
    root.node.fail = true;
    sound.failure();
  }

  return {
    onLoad,
    onJump() {
      // reset failure state if no failed move found in mainline history
      if (success() === false && !root.nodeList.find(n => !!n.fail)) success(null);
      checkSuccess();
    },
    onCeval: checkSuccess,
    data,
    goal,
    success,
    nbMoves,
    reset() {
      root.tree.root.children = [];
      root.userJump('');
      root.practice!.reset();
      onLoad();
    },
    isWhite: root.bottomIsWhite,
    analysisUrl,
    autoNext,
    goToNext
  };
}
