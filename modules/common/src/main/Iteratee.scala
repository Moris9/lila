package lila.common

import scala.concurrent.duration._
import akka.actor.ActorSystem
import play.api.libs.iteratee._

object Iteratee {

  def delay[A](duration: FiniteDuration)(implicit system: ActorSystem): Enumeratee[A, A] =
    Enumeratee.mapM[A].apply[A] { as =>
      lila.common.Future.delay[A](duration)(fuccess(as))
    }

  // Avoid running `andThen` on empty elements, just for perf
  def prepend[A](elements: TraversableOnce[A], enumerator: Enumerator[A]): Enumerator[A] =
    if (elements.isEmpty) enumerator
    else Enumerator.enumerate(elements) andThen enumerator
}
