package lila.chess

class ColorTest extends LilaTest {

  "Color" should {
    "unary !" in {
      "white" in { !White must_== Black }
      "black" in { !Black must_== White }
    }
  }
}
