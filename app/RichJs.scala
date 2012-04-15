package lila

import play.api.libs.json._

object RichJs {

  implicit def richJsObject(js: JsObject) = new {

    def str(key: String): Option[String] =
      js.value get key flatMap (_.asOpt[String])

    def obj(key: String): Option[JsObject] =
      js.value get key flatMap (_.asOpt[JsObject])
  }

  implicit def richJsValue(js: JsValue) = new {

    def str(key: String): Option[String] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      str ← value.asOpt[String]
    } yield str

    def obj(key: String): Option[JsObject] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      obj2 ← value.asOpt[JsObject]
    } yield obj2
  }
}
