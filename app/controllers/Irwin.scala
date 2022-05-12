package controllers

import lila.app._

final class Irwin(env: Env) extends LilaController(env) {

  import lila.irwin.JSONHandlers.reportReader

  def dashboard =
    Secure(_.MarkEngine) { implicit ctx => _ =>
      env.irwin.irwinApi.dashboard map { d =>
        Ok(views.html.irwin.dashboard(d))
      }
    }

  def saveReport =
    ScopedBody(parse.json)(Nil) { req => me =>
      IfGranted(_.Admin, req, me) {
        req.body
          .validate[lila.irwin.IrwinReport]
          .fold(
            err => fuccess(BadRequest(err.toString)),
            report => env.irwin.irwinApi.reports.insert(report) inject Ok
          ) map (_ as TEXT)
      }
    }

  def eventStream =
    Scoped() { req => me =>
      IfGranted(_.Admin, req, me) {
        noProxyBuffer(Ok.chunked(env.irwin.irwinStream())).fuccess
      }
    }

  def kaladin =
    Secure(_.MarkEngine) { implicit ctx => _ =>
      env.irwin.kaladinApi.dashboard map { d =>
        Ok(views.html.kaladin.dashboard(d))
      }
    }
}
