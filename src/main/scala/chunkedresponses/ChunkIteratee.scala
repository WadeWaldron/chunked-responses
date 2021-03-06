package chunkedresponses

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import play.api.libs.iteratee.{Done, Step, Input, Iteratee}
import spray.http.HttpData
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}

class ChunkIteratee(chunkedResponder: ActorRef) extends Iteratee[HttpData, Unit] {
  import ChunkedResponder._
  private implicit val timeout = Timeout(30.seconds)

  def fold[B](folder: (Step[HttpData, Unit]) => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    def waitForAck(future: Future[Any]):Iteratee[HttpData, Unit] = Iteratee.flatten(future.map(_ => this))

    def step(input: Input[HttpData]):Iteratee[HttpData, Unit] = input match {
      case Input.El(e) => waitForAck(chunkedResponder ? Chunk(e))
      case Input.Empty => waitForAck(Future.successful(Unit))
      case Input.EOF =>
        chunkedResponder ! Shutdown
        Done(Unit, Input.EOF)
    }

    folder(Step.Cont(step))
  }
}