package lila.coach

import reactivemongo.bson._
import reactivemongo.bson.Macros

import chess.Color
import lila.db.BSON._
import lila.db.Implicits._
import lila.game.BSONHandlers.StatusBSONHandler
import lila.rating.PerfType

private[coach] object BSONHandlers {

  private implicit val ColorBSONHandler = new BSONHandler[BSONBoolean, Color] {
    def read(b: BSONBoolean) = Color(b.value)
    def write(c: Color) = BSONBoolean(c.white)
  }
  private implicit val PerfTypeBSONHandler = new BSONHandler[BSONInteger, PerfType] {
    def read(b: BSONInteger) = PerfType.byId get b.value err s"Invalid perf type id ${b.value}"
    def write(p: PerfType) = BSONInteger(p.id)
  }
  private implicit val EcopeningBSONHandler = new BSONHandler[BSONString, Ecopening] {
    def read(b: BSONString) = EcopeningDB.allByEco get b.value err s"Invalid ECO ${b.value}"
    def write(e: Ecopening) = BSONString(e.eco)
  }
  private implicit val RelativeStrengthBSONHandler = new BSONHandler[BSONInteger, RelativeStrength] {
    def read(b: BSONInteger) = RelativeStrength.all.find(_.id == b.value) err s"Invalid relative strength ${b.value}"
    def write(e: RelativeStrength) = BSONInteger(e.id)
  }
  private implicit val ResultBSONHandler = new BSONHandler[BSONInteger, Result] {
    def read(b: BSONInteger) = Result.all.find(_.id == b.value) err s"Invalid result ${b.value}"
    def write(e: Result) = BSONInteger(e.id)
  }
  private implicit val PhaseBSONHandler = new BSONHandler[BSONInteger, Phase] {
    def read(b: BSONInteger) = Phase.all.find(_.id == b.value) err s"Invalid phase ${b.value}"
    def write(e: Phase) = BSONInteger(e.id)
  }
  private implicit def NumbersHandler = new BSONHandler[BSONDocument, Numbers] {
    def read(b: BSONDocument) = Numbers(
      size = b.getAs[Int]("s") err s"Missing numbers size",
      mean = b.getAs[Double]("m") err s"Missing numbers mean",
      median = b.getAs[Double]("n") err s"Missing numbers median",
      deviation = b.getAs[Double]("d") err s"Missing numbers deviation")
    def write(b: Numbers) = BSONDocument(
      "s" -> b.size,
      "m" -> b.mean,
      "n" -> b.median,
      "d" -> b.deviation)
  }
  private implicit val OpponentBSONHandler = Macros.handler[Opponent]
  private implicit val RatioBSONHandler = Macros.handler[Ratio]

  private implicit def ByPhaseHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]) = new BSONHandler[BSONDocument, ByPhase[T]] {
    private val r = reader.asInstanceOf[BSONReader[BSONValue, T]]
    def read(b: BSONDocument) = ByPhase(
      opening = b.getAs[T]("o") err s"No phase:opening value!",
      middle = b.getAs[T]("m"),
      end = b.getAs[T]("e"),
      all = b.getAs[T]("a") err s"No phase:all value!")
    def write(b: ByPhase[T]) = BSONDocument(
      "o" -> b.opening,
      "m" -> b.middle,
      "e" -> b.end,
      "a" -> b.all)
  }
  private implicit def ByPieceRoleHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]) = new BSONHandler[BSONDocument, ByPieceRole[T]] {
    private val r = reader.asInstanceOf[BSONReader[BSONValue, T]]
    def read(b: BSONDocument) = ByPieceRole(
      pawn = b.getAs[T]("p"),
      knight = b.getAs[T]("n"),
      bishop = b.getAs[T]("b"),
      rook = b.getAs[T]("r"),
      queen = b.getAs[T]("q"),
      king = b.getAs[T]("k"))
    def write(b: ByPieceRole[T]) = BSONDocument(
      "p" -> b.pawn,
      "n" -> b.knight,
      "b" -> b.bishop,
      "r" -> b.rook,
      "q" -> b.queen,
      "k" -> b.king)
  }
  private implicit def ByPositionQualityHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]) = new BSONHandler[BSONDocument, ByPositionQuality[T]] {
    private val r = reader.asInstanceOf[BSONReader[BSONValue, T]]
    def read(b: BSONDocument) = ByPositionQuality(
      losing = b.getAs[T]("l"),
      bad = b.getAs[T]("b"),
      equal = b.getAs[T]("e"),
      good = b.getAs[T]("g"),
      winning = b.getAs[T]("w"))
    def write(b: ByPositionQuality[T]) = BSONDocument(
      "l" -> b.losing,
      "b" -> b.bad,
      "e" -> b.equal,
      "g" -> b.good,
      "w" -> b.winning)
  }
  private implicit def ByMovetimeHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]) = new BSONHandler[BSONArray, ByMovetime[T]] {
    private val h = bsonArrayToVectorHandler[T]
    def read(b: BSONArray) = ByMovetime(h read b)
    def write(b: ByMovetime[T]) = h write b.values
  }
  private implicit def GroupedHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]) = new BSONHandler[BSONDocument, Grouped[T]] {
    def read(b: BSONDocument) = Grouped(
      byPhase = b.getAs[ByPhase[T]]("p") err s"Missing group byPhase",
      byMovetime = b.getAs[ByMovetime[T]]("m") err s"Missing group byMovetime",
      byPieceRole = b.getAs[ByPieceRole[T]]("r") err s"Missing group byPieceRole",
      byPositionQuality = b.getAs[ByPositionQuality[T]]("q") err s"Missing group byPositionQuality")
    def write(b: Grouped[T]) = BSONDocument(
      "p" -> b.byPhase,
      "m" -> b.byMovetime,
      "r" -> b.byPieceRole,
      "q" -> b.byPositionQuality)
  }
  implicit val EntryBSONHandler = Macros.handler[Entry]
}
