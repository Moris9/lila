package controllers

import lila.app._
import views._

object Search extends LilaController with BaseGame {

  // private def indexer = env.search.indexer
  // private def forms = env.search.forms

  def form(page: Int) = TODO
  // OpenBody { implicit ctx ⇒
  //   reasonable(page) {
  //     implicit def req = ctx.body
  //     forms.search.bindFromRequest.fold(
  //       failure ⇒ Ok(html.search.form(makeListMenu, failure)),
  //       data ⇒ Ok(html.search.form(
  //         makeListMenu,
  //         forms.search fill data,
  //         data.query.nonEmpty option env.search.paginator(data.query, page)
  //       ))
  //     )
  //   }
  // }
}
