package lila.ai

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.routing._
import akka.util.Timeout

import actorApi._
import lila.analyse.{ Evaluation, Info }

private[ai] final class Queue(config: Config) extends Actor {

  import Puller._

  import akka.actor.SupervisorStrategy._

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception => Restart
  }

  (1 to config.nbInstances).toList map { id =>
    context.actorOf(
      Props(classOf[Puller], config, id),
      name = s"pull-$id")
  }

  private val extraStockfishTime = 1 second

  private val tasks = new scala.collection.mutable.PriorityQueue[Task]

  def receive = {

    case Enqueue(task) => tasks += task

    case GimmeWork =>
      if (tasks.isEmpty) sender ! NoWork
      else sender ! tasks.dequeue

    case req: PlayReq =>
      val timeout = makeTimeout((config moveTime req.level).millis + extraStockfishTime)
      tasks += Task(req, sender, timeout)

    case req: AnalReq =>
      if (req.isStart) sender ! Evaluation.start.some
      else {
        val timeout = makeTimeout(config.analyseMoveTime + extraStockfishTime)
        tasks += Task(req, sender, timeout)
      }

    case FullAnalReq(moves, fen) =>
      val mrSender = sender
      implicit val timeout = makeTimeout(config.analyseTimeout)
      val futures = (0 to moves.size) map moves.take map { serie =>
        self ? AnalReq(serie, fen) mapTo manifest[Option[Evaluation]]
      }
      Future.fold(futures)(Vector[Option[Evaluation]]())(_ :+ _) addFailureEffect {
        case e => mrSender ! Status.Failure(e)
      } foreach { results =>
        mrSender ! Evaluation.toInfos(results.toList.map(_ | Evaluation.empty), moves)
      }
  }
}
