package lila
package forum

case class CategView(
    categ: Categ,
    lastPost: Option[Post]) {

  def slug = categ.slug
  def name = categ.name
  def desc = categ.desc
  def nbTopics = categ.nbTopics
  def nbPosts = categ.nbPosts
}
