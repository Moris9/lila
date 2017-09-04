package lila.push

private sealed abstract class Stacking(val key: String, val message: String)

private object Stacking {

  case object GameFinish extends Stacking("gameFinish", "$[notif_count] games are over")
  case object GameMove extends Stacking("gameMove", "It's your turn in $[notif_count] games")
  case object GameTakeback extends Stacking("gameTakeback", "Takeback offers in $[notif_count] games")
  case object NewMessage extends Stacking("newMessage", "You have $[notif_count] new messages")
  case object ChallengeCreate extends Stacking("challengeCreate", "You have $[notif_count] new challenges")
  case object ChallengeAccept extends Stacking("challengeAccept", "$[notif_count] players accepted your challenges")
}
