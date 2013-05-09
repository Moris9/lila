package lila.db
package api

import Types._

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

object $save {

  def apply[ID: Writes, A <: Identified[ID]: TubeInColl](doc: A): Funit =
    (implicitly[Tube[A]] toMongo doc).fold(e ⇒ fufail(e.toString),
      js ⇒ $update($select(doc.id), js, upsert = true)
    )
  def apply[A <: Identified[String]: TubeInColl](doc: A): Funit = apply[String, A](doc)
}
