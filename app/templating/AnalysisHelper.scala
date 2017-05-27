package lila.app
package templating

import lila.analyse.Advice.Judgment

import lila.api.Context
import lila.i18n.I18nKeys

trait AnalysisHelper { self: I18nHelper with SecurityHelper =>

  def judgmentName(judgment: Judgment)(implicit ctx: Context) = judgment match {
    case Judgment.Blunder => I18nKeys.blunders()
    case Judgment.Mistake => I18nKeys.mistakes()
    case Judgment.Inaccuracy => I18nKeys.inaccuracies()
    case judgment => judgment.toString
  }
}
