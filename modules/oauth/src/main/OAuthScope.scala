package lila.oauth

import lila.i18n.I18nKey
import lila.i18n.I18nKeys.{ oauthScope => trans }

sealed abstract class OAuthScope(val key: String, val name: I18nKey) {
  override def toString = s"Scope($key)"
}

object OAuthScope {

  object Preference {
    case object Read  extends OAuthScope("preference:read", trans.preferenceRead)
    case object Write extends OAuthScope("preference:write", trans.preferenceWrite)
  }

  object Email {
    case object Read extends OAuthScope("email:read", trans.emailRead)
  }

  object Challenge {
    case object Read  extends OAuthScope("challenge:read", trans.challengeRead)
    case object Write extends OAuthScope("challenge:write", trans.challengeWrite)
    case object Bulk  extends OAuthScope("challenge:bulk", trans.challengeBulk)
  }

  object Study {
    case object Read  extends OAuthScope("study:read", trans.studyRead)
    case object Write extends OAuthScope("study:write", trans.studyWrite)
  }

  object Tournament {
    case object Write extends OAuthScope("tournament:write", trans.tournamentWrite)
  }

  object Racer {
    case object Write extends OAuthScope("racer:write", trans.racerWrite)
  }

  object Puzzle {
    case object Read extends OAuthScope("puzzle:read", trans.puzzleRead)
  }

  object Team {
    case object Read  extends OAuthScope("team:read", trans.teamRead)
    case object Write extends OAuthScope("team:write", trans.teamWrite)
  }

  object Follow {
    case object Read  extends OAuthScope("follow:read", trans.followRead)
    case object Write extends OAuthScope("follow:write", trans.followWrite)
  }

  object Msg {
    case object Write extends OAuthScope("msg:write", trans.msgWrite)
  }

  object Board {
    case object Play extends OAuthScope("board:play", trans.boardPlay)
  }

  object Bot {
    case object Play extends OAuthScope("bot:play", trans.botPlay)
  }

  object Engine {
    case object Read  extends OAuthScope("engine:read", trans.engineRead)
    case object Write extends OAuthScope("engine:write", trans.engineWrite)
  }

  object Web {
    case object Login extends OAuthScope("web:login", trans.webLogin)
    case object Mod   extends OAuthScope("web:mod", trans.webMod)
  }

  case class Scoped(user: lila.user.User, scopes: List[OAuthScope])

  type Selector = OAuthScope.type => OAuthScope

  val all: List[OAuthScope] = List(
    Preference.Read,
    Preference.Write,
    Email.Read,
    Challenge.Read,
    Challenge.Write,
    Challenge.Bulk,
    Study.Read,
    Study.Write,
    Tournament.Write,
    Racer.Write,
    Puzzle.Read,
    Team.Read,
    Team.Write,
    Follow.Read,
    Follow.Write,
    Msg.Write,
    Board.Play,
    Bot.Play,
    Engine.Read,
    Engine.Write,
    Web.Login,
    Web.Mod
  )

  val allButWeb = all.filterNot(_.key startsWith "web:")

  val dangerList: Set[OAuthScope] = Set(
    Team.Write,
    Web.Login,
    Web.Mod,
    Msg.Write
  )

  val byKey: Map[String, OAuthScope] = all.map { s =>
    s.key -> s
  } toMap

  def keyList(scopes: Iterable[OAuthScope]) = scopes.map(_.key) mkString ", "

  def select(selectors: Iterable[OAuthScope.type => OAuthScope]) = selectors.map(_(OAuthScope)).toList

  import reactivemongo.api.bson._
  import lila.db.dsl._
  implicit private[oauth] val scopeHandler = tryHandler[OAuthScope](
    { case b: BSONString => OAuthScope.byKey.get(b.value) toTry s"No such scope: ${b.value}" },
    s => BSONString(s.key)
  )
}
