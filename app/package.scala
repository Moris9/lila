package lila

import scalaz.Zero
import play.api.mvc.{ Result, PlainResult, Results }
import scala.concurrent.Future

package object app extends PackageObject with WithPlay with socket.WithSocket {

  implicit val LilaResultZero = new Zero[Result] {
    val zero = Results.NotFound
  }
  implicit val LilaPlainResultZero = new Zero[PlainResult] {
    val zero = Results.NotFound
  }

  implicit final class LilaPimpedResult(result: Result) {
    def fuccess = Future successful result
  }

}
