package lila.chess

sealed trait Color {

  def -(role: Role) = Piece(this, role)

  val unary_! : Color

  val unmovedPawnY: Int
  val passablePawnY: Int
  val promotablePawnY: Int

  val letter: Char

  def pawn   = this - Pawn
  def bishop = this - Bishop
  def knight = this - Knight
  def rook   = this - Rook
  def queen  = this - Queen
  def king   = this - King
}

case object White extends Color {

  lazy val unary_! = Black

  val unmovedPawnY = 2
  val passablePawnY = 5
  val promotablePawnY = 7

  val letter = 'w'
}

case object Black extends Color {

  lazy val unary_! = White

  val unmovedPawnY = 7
  val passablePawnY = 4
  val promotablePawnY = 2

  val letter = 'b'
}

object Color {

  def apply(b: Boolean): Color = if (b) White else Black

  def apply(n: String): Option[Color] =
    if (n == "white") Some(White)
    else if (n == "black") Some(Black)
    else None

  def apply(c: Char): Option[Color] =
    if (c == 'w') Some(White)
    else if (c == 'b') Some(Black)
    else None

  val all = List(White, Black)
}
