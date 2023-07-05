package lila.practice

import lila.study.{ Chapter, Study }

case class UserPractice(
    structure: PracticeStructure,
    progress: PracticeProgress
):

  def progressOn(studyId: StudyId) =
    val chapterIds = structure.study(studyId).so(_.chapterIds)
    Completion(
      done = progress countDone chapterIds,
      total = chapterIds.size
    )

  lazy val nbDoneChapters = structure.chapterIds count progress.chapters.contains

  lazy val progressPercent = nbDoneChapters * 100 / structure.nbUnhiddenChapters.atLeast(1)

case class UserStudy(
    practice: UserPractice,
    practiceStudy: PracticeStudy,
    chapters: List[Chapter.Metadata],
    study: Study.WithChapter,
    section: PracticeSection
):

  def url = s"/practice/${section.id}/${practiceStudy.slug}/${study.study.id}"

case class Completion(done: Int, total: Int):

  def percent = if total == 0 then 0 else done * 100 / total

  def complete = done >= total
