package lila
package format

trait Format[A] {

  def <<(str: String): A

  def >>(obj: A): String
}
