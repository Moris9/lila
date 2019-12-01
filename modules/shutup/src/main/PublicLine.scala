package lila.shutup

import org.joda.time.DateTime
import scala.util.{ Try, Success, Failure }

import lila.hub.actorApi.shutup.{ PublicSource => Source }

case class PublicLine(
    text: String,
    from: Option[Source],
    date: Option[DateTime]
)

object PublicLine {

  def make(text: String, from: Source): PublicLine =
    PublicLine(text, from.some, DateTime.now.some)

  import reactivemongo.api.bson._
  import lila.db.dsl._
  private implicit val SourceHandler = lila.db.BSON.tryHandler[Source](
    {
      case BSONString(v) => v split ':' match {
        case Array("t", id) => Success(Source.Tournament(id))
        case Array("s", id) => Success(Source.Simul(id))
        case Array("w", gameId) => Success(Source.Watcher(gameId))
        case Array("u", id) => Success(Source.Study(id))
        case a => lila.db.BSON.handlerBadValue(s"Invalid PublicLine source $v")
      }
    },
    x => BSONString(x match {
      case Source.Tournament(id) => s"t:$id"
      case Source.Simul(id) => s"s:$id"
      case Source.Study(id) => s"u:$id"
      case Source.Watcher(gameId) => s"w:$gameId"
    })
  )

  private val objectHandler = Macros.handler[PublicLine]

  implicit val PublicLineBSONHandler = lila.db.BSON.tryHandler[PublicLine](
    {
      case doc: BSONDocument => objectHandler readTry doc
      case BSONString(text) => Success(PublicLine(text, none, none))
      case a => lila.db.BSON.handlerBadValue(s"Invalid PublicLine $a")
    },
    x => if (x.from.isDefined) objectHandler.writeTry(x).get else BSONString(x.text)
  )
}
