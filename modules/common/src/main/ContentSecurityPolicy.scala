package lila.common

case class ContentSecurityPolicy(
    defaultSrc: List[String],
    connectSrc: List[String],
    styleSrc: List[String],
    fontSrc: List[String],
    childSrc: List[String],
    imgSrc: List[String],
    scriptSrc: List[String]
) {

  private def withScriptSrc(source: String) = copy(scriptSrc = source :: scriptSrc)

  def withNonce(nonce: Nonce) = withScriptSrc(nonce.scriptSrc)

  def withStripe = copy(
    connectSrc = "https://*.stripe.com" :: connectSrc,
    scriptSrc = "https://*.stripe.com" :: scriptSrc,
    childSrc = "https://*.stripe.com" :: childSrc
  )

  override def toString: String =
    List(
      "default-src " -> defaultSrc,
      "connect-src " -> connectSrc,
      "style-src " -> styleSrc,
      "font-src " -> fontSrc,
      "child-src " -> childSrc,
      "img-src " -> imgSrc,
      "script-src " -> scriptSrc
    ) filter {
        case (_, sources) =>
          sources.nonEmpty
      } map {
        case (directive, sources) =>
          sources.mkString(directive, " ", ";")
      } mkString (" ")
}
