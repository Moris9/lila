package lila

package object swiss extends PackageObject {

  private[swiss] val logger = lila.log("swiss")

  private[swiss] type Ranking = Map[SwissPlayer.Number, Int]
}
