package lila.oauth

import reactivemongo.api.bson.*
import com.roundeights.hasher.Algo
import ornicar.scalalib.SecureRandom

import lila.common.Bearer
import lila.user.User

case class AccessToken(
    id: AccessToken.Id,
    plain: Bearer,
    userId: UserId,
    createdAt: Option[DateTime],
    description: Option[String], // for personal access tokens
    usedAt: Option[DateTime] = None,
    scopes: List[OAuthScope],
    clientOrigin: Option[String],
    expires: Option[DateTime]
):
  def isBrandNew = createdAt.exists(DateTime.now.minusSeconds(5).isBefore)

  def isDangerous = scopes.exists(OAuthScope.dangerList.contains)

object AccessToken:

  opaque type Id = String
  object Id extends OpaqueString[Id]:
    def from(bearer: Bearer) = Id(Algo.sha256(bearer.value).hex)

  case class ForAuth(userId: UserId, scopes: List[OAuthScope], clientOrigin: Option[String])

  object BSONFields:
    val id           = "_id"
    val plain        = "plain"
    val userId       = "userId"
    val createdAt    = "created"
    val description  = "description"
    val usedAt       = "used"
    val scopes       = "scopes"
    val clientOrigin = "clientOrigin"
    val expires      = "expires"

  import lila.db.BSON
  import lila.db.dsl.{ *, given }
  import OAuthScope.given

  private[oauth] val forAuthProjection = $doc(
    BSONFields.userId       -> true,
    BSONFields.scopes       -> true,
    BSONFields.clientOrigin -> true
  )

  given BSONDocumentReader[ForAuth] = new BSONDocumentReader[ForAuth]:
    def readDocument(doc: BSONDocument) =
      for {
        userId <- doc.getAsTry[UserId](BSONFields.userId)
        scopes <- doc.getAsTry[List[OAuthScope]](BSONFields.scopes)
        origin = doc.getAsOpt[String](BSONFields.clientOrigin)
      } yield ForAuth(userId, scopes, origin)

  given BSONDocumentHandler[AccessToken] = new BSON[AccessToken]:

    import BSONFields.*

    def reads(r: BSON.Reader): AccessToken =
      AccessToken(
        id = r.get[Id](id),
        plain = r.get[Bearer](plain),
        userId = r.get[UserId](userId),
        createdAt = r.getO[DateTime](createdAt),
        description = r strO description,
        usedAt = r.getO[DateTime](usedAt),
        scopes = r.get[List[OAuthScope]](scopes),
        clientOrigin = r strO clientOrigin,
        expires = r.getO[DateTime](expires)
      )

    def writes(w: BSON.Writer, o: AccessToken) =
      $doc(
        id           -> o.id,
        plain        -> o.plain,
        userId       -> o.userId,
        createdAt    -> o.createdAt,
        description  -> o.description,
        usedAt       -> o.usedAt,
        scopes       -> o.scopes,
        clientOrigin -> o.clientOrigin,
        expires      -> o.expires
      )
