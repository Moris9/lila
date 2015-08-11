package lila.common

import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement

object String {

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  final class Delocalizer(netDomain: String) {

    private val regex = ("""\w{2}\.""" + quoteReplacement(netDomain)).r

    def apply(url: String) = regex.replaceAllIn(url, netDomain)
  }

  def hex2bytes(hex: String): Array[Byte] = hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

  def bytes2hex(bytes: Array[Byte]): String = bytes.map("%02x".format(_)).mkString
}
