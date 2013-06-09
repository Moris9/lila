package lila.app
package templating

import chess.{ Mode, Variant, Speed }
import lila.setup._
import lila.user.Context

trait SetupHelper extends scalaz.Booleans { self: I18nHelper ⇒

  def translatedModeChoices(implicit ctx: Context) = List(
    Mode.Casual.id.toString -> trans.casual.str(),
    Mode.Rated.id.toString -> trans.rated.str()
  )

  def translatedVariantChoices(implicit ctx: Context) = List(
    Variant.Standard.id.toString -> trans.standard.str(),
    Variant.Chess960.id.toString -> Variant.Chess960.name.capitalize
  )

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+ (Variant.FromPosition.id.toString -> "FEN")

  def translatedSpeedChoices(implicit ctx: Context) = Speed.all map { s ⇒
    s.id.toString -> (s.toString + " - " + s.name.capitalize)
  }

  def eloDiffChoices(elo: Int)(implicit ctx: Context) = FilterConfig.eloDiffs map { diff ⇒
    diff -> "%d - %d (±%d)".format(elo - diff, elo + diff, diff)
  }
}
