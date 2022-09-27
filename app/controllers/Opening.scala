package controllers

import play.api.libs.json.Json
import play.api.mvc._
import views.html

import lila.api.Context
import lila.app._
import lila.common.LilaOpeningFamily
import lila.opening.OpeningQuery

final class Opening(env: Env) extends LilaController(env) {

  def index =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.index flatMap {
        _ ?? { page =>
          Ok(html.opening.index(page)).fuccess
        }
      }
    }

  def query(q: String) =
    Secure(_.Beta) { implicit ctx => _ =>
      env.opening.api.lookup(q) flatMap {
        _ ?? { page =>
          page.query.family.??(f => env.puzzle.opening.find(f)) map { puzzle =>
            Ok(html.opening.show(page, puzzle))
          }
        }
      }
    }
}
