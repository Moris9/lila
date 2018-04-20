package lila.oauth

import org.joda.time.DateTime

import lila.user.User

case class AccessToken(
    id: AccessToken.Id,
    clientId: String,
    userId: User.ID,
    expiresAt: DateTime,
    createdAt: Option[DateTime] = None, // for personal access tokens
    description: Option[String] = None, // for personal access tokens
    usedAt: Option[DateTime] = None,
    scopes: List[OAuthScope]
) {
  def isBrandNew = createdAt.exists(DateTime.now.minusSeconds(5).isBefore)
}

object AccessToken {

  val idSize = 16

  case class Id(value: String) extends AnyVal

  def makeId = Id(ornicar.scalalib.Random secureString idSize)

  case class ForAuth(userId: User.ID, expiresAt: DateTime, usedAt: DateTime, scopes: List[OAuthScope]) {
    def isExpired = expiresAt isBefore DateTime.now
    def usedRecently = usedAt.isAfter(DateTime.now minusMinutes 5)
  }

  object BSONFields {
    val id = "access_token_id"
    val clientId = "client_id"
    val userId = "user_id"
    val createdAt = "create_date"
    val expiresAt = "expire_date"
    val description = "description"
    val usedAt = "used_at"
    val scopes = "scopes"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler
  import OAuthScope.scopeHandler

  private[oauth] val forAuthProjection = $doc(
    BSONFields.userId -> true,
    BSONFields.expiresAt -> true,
    BSONFields.usedAt -> true,
    BSONFields.scopes -> true
  )

  private[oauth] implicit val accessTokenIdHandler = stringAnyValHandler[Id](_.value, Id.apply)

  implicit val ForAuthBSONReader = new BSONDocumentReader[ForAuth] {
    def read(doc: BSONDocument) = ForAuth(
      userId = doc.getAs[User.ID](BSONFields.userId) err "ForAuth userId missing",
      expiresAt = doc.getAs[DateTime](BSONFields.expiresAt) err "ForAuth expiresAt missing",
      usedAt = doc.getAs[DateTime](BSONFields.usedAt) err "ForAuth usedAt missing",
      scopes = doc.getAs[List[OAuthScope]](BSONFields.scopes) err "ForAuth scopes missing"
    )
  }

  implicit val AccessTokenBSONHandler = new BSON[AccessToken] {

    import BSONFields._

    def reads(r: BSON.Reader): AccessToken = AccessToken(
      id = r.get[Id](id),
      clientId = r str clientId,
      userId = r str userId,
      expiresAt = r.get[DateTime](expiresAt),
      createdAt = r.getO[DateTime](createdAt),
      description = r strO description,
      usedAt = r.getO[DateTime](usedAt),
      scopes = r.get[List[OAuthScope]](scopes)
    )

    def writes(w: BSON.Writer, o: AccessToken) = $doc(
      id -> o.id,
      clientId -> o.clientId,
      userId -> o.userId,
      expiresAt -> o.expiresAt,
      createdAt -> o.createdAt,
      description -> o.description,
      usedAt -> o.usedAt,
      scopes -> o.scopes
    )
  }
}
