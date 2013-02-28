package lila.app
package implicits

import play.api.libs.json._

object RichJs {

  implicit def richJsObject(js: JsObject) = new {

    def str(key: String): Option[String] =
      js.value get key flatMap (_.asOpt[String])

    def int(key: String): Option[Int] =
      js.value get key flatMap (_.asOpt[Int])

    def long(key: String): Option[Long] =
      js.value get key flatMap (_.asOpt[Long])

    def obj(key: String): Option[JsObject] =
      js.value get key flatMap (_.asOpt[JsObject])
  }

  implicit def richJsValue(js: JsValue) = new {

    def str(key: String): Option[String] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      str ← value.asOpt[String]
    } yield str

    def int(key: String): Option[Int] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      int ← value.asOpt[Int]
    } yield int

    def long(key: String): Option[Long] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      int ← value.asOpt[Long]
    } yield int

    def obj(key: String): Option[JsObject] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      obj2 ← value.asOpt[JsObject]
    } yield obj2
  }
}
