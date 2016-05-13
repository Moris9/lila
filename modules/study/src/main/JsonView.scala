package lila.study

import chess.format.pgn.{ Glyph, Glyphs }
import chess.format.{ Uci, UciCharPair, Forsyth, FEN }
import chess.Pos
import org.joda.time.DateTime
import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.socket.Socket.Uid
import lila.socket.tree.Node.Shape
import lila.user.User

final class JsonView(lightUser: LightUser.Getter) {

  import JsonView._

  def apply(study: Study, chapters: List[Chapter.Metadata], me: Option[User]) =
    studyWrites.writes(study) ++ Json.obj(
      "chapters" -> chapters.map(chapterMetadataWrites.writes),
      "features" -> Json.obj(
        "computer" -> StudySettings.UserSelection.allows(study.settings.computer, study, me.map(_.id)),
        "explorer" -> StudySettings.UserSelection.allows(study.settings.explorer, study, me.map(_.id))
      )
    )

  private implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj("id" -> u.id, "name" -> u.name, "title" -> u.title).noNull
  }

  private[study] implicit val memberRoleWrites = Writes[StudyMember.Role] { r =>
    JsString(r.id)
  }
  private[study] implicit val memberWrites: Writes[StudyMember] = Writes[StudyMember] { m =>
    Json.obj(
      "user" -> lightUser(m.id),
      "role" -> m.role,
      "addedAt" -> m.addedAt)
  }

  private[study] implicit val membersWrites: Writes[StudyMembers] = Writes[StudyMembers] { m =>
    Json toJson m.members
  }

  private implicit val studyWrites = OWrites[Study] { s =>
    Json.obj(
      "id" -> s.id,
      "name" -> s.name,
      "members" -> s.members,
      "position" -> s.position,
      "ownerId" -> s.ownerId,
      "settings" -> s.settings,
      "createdAt" -> s.createdAt,
      "isNew" -> s.createdAt.isAfter(DateTime.now minusSeconds 4).option(true)
    ).noNull
  }
}

object JsonView {

  case class JsData(study: JsObject, analysis: JsObject, chat: JsValue)

  private implicit val uciWrites: Writes[Uci] = Writes[Uci] { u =>
    JsString(u.uci)
  }
  private implicit val uciCharPairWrites: Writes[UciCharPair] = Writes[UciCharPair] { u =>
    JsString(u.toString)
  }
  private implicit val posReader: Reads[Pos] = Reads[Pos] { v =>
    (v.asOpt[String] flatMap Pos.posAt).fold[JsResult[Pos]](JsError(Nil))(JsSuccess(_))
  }
  private[study] implicit val pathWrites: Writes[Path] = Writes[Path] { p =>
    JsString(p.toString)
  }
  private implicit val colorWriter: Writes[chess.Color] = Writes[chess.Color] { c =>
    JsString(c.name)
  }
  private implicit val fenWriter: Writes[FEN] = Writes[FEN] { f =>
    JsString(f.value)
  }
  private[study] implicit val uidWriter: Writes[Uid] = Writes[Uid] { uid =>
    JsString(uid.value)
  }
  private[study] implicit val visibilityWriter = Writes[StudySettings.Visibility] { v =>
    JsString(v.key)
  }
  private[study] implicit val userSelectionWriter = Writes[StudySettings.UserSelection] { v =>
    JsString(v.key)
  }
  private[study] implicit val settingsWriter: Writes[StudySettings] = Json.writes[StudySettings]

  private[study] implicit val shapeReader: Reads[Shape] = Reads[Shape] { js =>
    js.asOpt[JsObject].flatMap { o =>
      for {
        brush <- o str "brush"
        orig <- o.get[Pos]("orig")
      } yield o.get[Pos]("dest") match {
        case Some(dest) => Shape.Arrow(brush, orig, dest)
        case _          => Shape.Circle(brush, orig)
      }
    }.fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))
  }

  private implicit val fenWrites = Writes[chess.format.FEN] { f =>
    JsString(f.value)
  }

  private implicit val variantWrites = Writes[chess.variant.Variant] { v => JsString(v.key) }
  private implicit val chapterSetupWrites = Json.writes[Chapter.Setup]
  private[study] implicit val chapterMetadataWrites = OWrites[Chapter.Metadata] { c =>
    Json.obj(
      "id" -> c._id,
      "name" -> c.name,
      "setup" -> c.setup)
  }
  // private implicit val moveWrites: Writes[Uci.WithSan] = Json.writes[Uci.WithSan]

  private[study] implicit val positionRefWrites: Writes[Position.Ref] = Json.writes[Position.Ref]
}
