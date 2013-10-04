package lila.ai
package stockfish

import java.util.regex.Matcher.quoteReplacement

import lila.analyse.Info

object AnalyseParser {

  // info depth 4 seldepth 5 score cp -3309 nodes 1665 nps 43815 time 38 multipv 1 pv f2e3 d4c5 c1d1 c5g5 d1d2 g5g2 d2c1 e8e3
  def apply(lines: List[String], move: String): Option[Int ⇒ Info] =
    findBestMove(lines) flatMap { best ⇒
      Info(move, best, findCp(lines), findMate(lines), findLine(lines))
    }

  private val bestMoveRegex = """^bestmove\s(\w+).*$""".r
  private def findBestMove(lines: List[String]) =
    lines.headOption map { line ⇒
      bestMoveRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1))
    }

  private val cpRegex = """^info.*\scp\s(\-?\d+).*$""".r
  private def findCp(lines: List[String]) =
    lines.tail.headOption map { line ⇒
      cpRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1))
    } flatMap parseIntOption

  private val mateRegex = """^info.*\smate\s(\-?\d+).*$""".r
  private def findMate(lines: List[String]) =
    lines.tail.headOption map { line ⇒
      mateRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1))
    } flatMap parseIntOption

  private val lineRegex = """\spv\s(\w+)$""".r
  private def findLine(lines: List[String]) =
    lines.tail.headOption map { line ⇒
      lineRegex.replaceAllIn(line, m ⇒ quoteReplacement(m group 1))
    } 
}
