package controllers

import org.joda.time.DateTime
import play.api.mvc.RequestHeader

import lila.common.Form.trueish
import lila.common.HTTPRequest
import lila.user.UserContext

trait RequestGetter:

  protected def get(name: String)(implicit ctx: UserContext): Option[String] = get(name, ctx.req)

  protected def get(name: String, req: RequestHeader): Option[String] =
    HTTPRequest.queryStringGet(req, name)

  protected def getInt(name: String)(implicit ctx: UserContext) =
    get(name) flatMap (_.toIntOption)

  protected def getInt(name: String, req: RequestHeader): Option[Int] =
    req.queryString get name flatMap (_.headOption) flatMap (_.toIntOption)

  protected def getLong(name: String)(implicit ctx: UserContext) =
    get(name) flatMap (_.toLongOption)

  protected def getLong(name: String, req: RequestHeader) =
    get(name, req) flatMap (_.toLongOption)

  protected def getTimestamp(name: String, req: RequestHeader) =
    getLong(name, req) map { new DateTime(_) }

  protected def getBool(name: String)(implicit ctx: UserContext) =
    (getInt(name) exists trueish) || (get(name) exists trueish)

  protected def getBool(name: String, req: RequestHeader) =
    (getInt(name, req) exists trueish) || (get(name, req) exists trueish)

  protected def getBoolOpt(name: String)(implicit ctx: UserContext) =
    (getInt(name) map trueish) orElse (get(name) map trueish)

  protected def getBoolOpt(name: String, req: RequestHeader) =
    (getInt(name, req) map trueish) orElse (get(name, req) map trueish)
