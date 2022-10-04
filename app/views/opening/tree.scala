package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ Opening, OpeningConfig }

object tree {

  import bits._

  def apply(root: Opening.Tree, config: OpeningConfig)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(none),
      title = trans.opening.txt()
    ) {
      main(cls := "page box box-pad opening opening--tree")(
        index.searchAndConfig(config, "", "tree"),
        div(cls := "box__top")(
          h1("Chess openings name tree"),
          a(href := routes.Opening.index())("Browse openings")
        ),
        search.resultsList(Nil),
        div(cls := "opening__tree")(
          renderChildren(root, 1)
        )
      )
    }

  private def renderChildren(node: Opening.Tree, level: Int): Frag =
    node.children map { case (op, node) =>
      val fold = level < 4 && node.children.nonEmpty
      val content = frag(
        (if (fold) summary else div)(op match {
          case (name, None)     => name
          case (name, Some(op)) => a(href := routes.Opening.query(op.key))(name)
        }),
        renderChildren(node, level + 1)
      )
      if (fold) details(content) else content
    }
}
