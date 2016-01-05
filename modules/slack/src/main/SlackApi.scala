package lila.slack

import lila.common.LightUser
import lila.user.User

final class SlackApi(
    client: SlackClient,
    implicit val lightUser: String => Option[LightUser]) {

  def donation(event: lila.hub.actorApi.DonationEvent): Funit = {
    val user = event.userId flatMap lightUser
    val username = user.fold("Anonymous")(_.titleName)
    def amount(cents: Int) = s"$$${lila.common.Maths.truncateAt(cents / 100d, 2)}"
    client(SlackMessage(
      username = "donation",
      icon = "heart_eyes",
      text = s"$username donated ${amount(event.gross)} (${amount(event.net)})! Monthly progress: ${event.progress}%",
      channel = "general"
    )) >> event.message.?? { msg =>
      client(SlackMessage(
        username = username,
        icon = "kissing_heart",
        text = msg,
        channel = "general"))
    }
  }

  def userMod(user: User, mod: User): Funit = client(SlackMessage(
    username = mod.username,
    icon = "oncoming_police_car",
    text = s"Let's have a look at <http://lichess.org/@/${user.username}>",
    channel = "deputy"))
}
