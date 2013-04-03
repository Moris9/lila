package lila.teamSearch

import lila.team.{ Team ⇒ TeamModel }
import lila.search.ElasticSearch
import play.api.libs.json._

private[teamSearch] object Team {

  object fields {
    val name = "na"
    val description = "de"
    val location = "lo"
    val nbMembers = "nbm"
  }
  import fields._
  import ElasticSearch.Mapping._

  def jsonMapping = Json.obj(
    "properties" -> Json.toJson(List(
      boost(name, "string", 3),
      boost(description, "string"),
      boost(location, "string"),
      field(nbMembers, "short")
    ).toMap),
    "analyzer" -> "snowball"
  )

  def apply(team: TeamModel): JsObject = Json.obj(
    name -> team.name,
    description -> team.description,
    location -> ~team.location,
    nbMembers -> team.nbMembers
  )
}
