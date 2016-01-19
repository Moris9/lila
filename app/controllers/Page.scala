package controllers

import play.api.mvc._, Results._

import lila.app._
import views._

object Page extends LilaController {

  private def page(bookmark: String) = Open { implicit ctx =>
    OptionOk(Prismic oneShotBookmark bookmark) {
      case (doc, resolver) => views.html.site.page(doc, resolver)
    }
  }

  def thanks = page("thanks")

  def tos = page("tos")

  def helpLichess = page("help")

  def streamHowTo = page("stream-howto")

  def contact = page("contact")

  def kingOfTheHill = page("king-of-the-hill")

  def racingKings = page("racing-kings")

  def crazyhouse = page("crazyhouse")

  def privacy = page("privacy")
}
