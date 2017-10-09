package lila

import lila.socket.WithSocket

package object study extends PackageObject with WithSocket {

  private[study] val logger = lila.log("study")

  private[study] type ChapterMap = Map[lila.study.Chapter.Id, lila.study.Chapter]

  private[study] type LightStudyCache = lila.memo.AsyncCache[lila.study.Study.Id, Option[lila.study.Study.LightStudy]]
}
