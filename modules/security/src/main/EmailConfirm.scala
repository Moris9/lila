package lila.security

import akka.actor.ActorSystem
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current
import scala.concurrent.duration._

import lila.common.EmailAddress
import lila.user.{ User, UserRepo }

trait EmailConfirm {

  def effective: Boolean

  def send(user: User, email: EmailAddress, tryNb: Int = 1): Funit

  def confirm(token: String): Fu[Option[User]]
}

object EmailConfirmSkip extends EmailConfirm {

  def effective = false

  def send(user: User, email: EmailAddress, tryNb: Int = 1) = UserRepo setEmailConfirmed user.id

  def confirm(token: String): Fu[Option[User]] = fuccess(none)
}

final class EmailConfirmMailGun(
    apiUrl: String,
    apiKey: String,
    sender: String,
    replyTo: String,
    baseUrl: String,
    secret: String,
    system: ActorSystem
) extends EmailConfirm {

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress, tryNb: Int = 1): Funit = tokener make user flatMap { token =>
    lila.mon.email.confirmation()
    val url = s"$baseUrl/signup/confirm/$token"
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(sender),
      "to" -> Seq(email.value),
      "h:Reply-To" -> Seq(replyTo),
      "o:tag" -> Seq("registration"),
      "subject" -> Seq(s"Confirm your lichess.org account, ${user.username}"),
      "text" -> Seq(s"""
Final step!

Confirm your email address to complete your lichess account. It's easy — just click on the link below.

$url

(Clicking not working? Try pasting it into your browser!)

This is a service email related to your use of lichess.org. If you did not register with Lichess you can safely ignore this message.
"""),
      "html" -> Seq(s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>Confirm your lichess.org account, ${user.username}</title>
  </head>
  <body>
    <div itemscope itemtype="http://schema.org/EmailMessage">
      <strong>Final step!</strong>
      <p itemprop="description">Confirm your email address to activate your Lichess account. It's easy — just click the link below.</p>
      <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
        <meta itemprop="name" content="Activate account">
        <meta itemprop="url" content="$url">
        <p><a itemprop="target" href="$url">$url</a></p>
        <p>(Clicking not working? Try pasting it into your browser!)</p>
      </div>
      <div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
        <small>This is a service email related to your use of <a itemprop="url" href="https://lichess.org/"><span itemprop="name">lichess.org</span></a>. If you did not register with Lichess you can safely ignore this message.</small>
      </div>
    </div>
  </body>
</html>""")
    )).void addFailureEffect {
      case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
      case _ =>
    } recoverWith {
      case e if tryNb < maxTries => akka.pattern.after(15 seconds, system.scheduler) {
        send(user, email, tryNb + 1)
      }
      case e => fufail(e)
    }
  }

  def confirm(token: String): Fu[Option[User]] = tokener read token flatMap {
    case u @ Some(user) => UserRepo.mustConfirmEmail(user.id) flatMap {
      case true => UserRepo setEmailConfirmed user.id inject u
      case _ => fuccess(none)
    }
    case _ => fuccess(none)
  }

  private val tokener = new UserTokener(
    secret = secret,
    getCurrentValue = id => UserRepo email id map (_.??(_.value))
  )
}
