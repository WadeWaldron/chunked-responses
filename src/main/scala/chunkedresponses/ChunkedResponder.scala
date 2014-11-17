package chunkedresponses

import akka.actor.{Actor, ActorRef}
import spray.http.HttpHeaders.RawHeader
import spray.http._

object ChunkedResponder {
  case class Chunk(data: HttpData)
  case object Shutdown
  case object Ack
}

class ChunkedResponder(contentType: ContentType, responder: ActorRef) extends Actor {
  import ChunkedResponder._
  def receive:Receive = {
    case chunk: Chunk =>
      responder.forward(ChunkedResponseStart(HttpResponse(entity = HttpEntity(contentType, chunk.data))).withAck(Ack))
      context.become(chunking)
    case Shutdown =>
      responder.forward(HttpResponse(headers = List(RawHeader("Content-Type", contentType.value))).withAck(Ack))
      context.stop(self)
  }

  def chunking:Receive = {
    case chunk: Chunk =>
      responder.forward(MessageChunk(chunk.data).withAck(Ack))
    case Shutdown =>
      responder.forward(ChunkedMessageEnd().withAck(Ack))
      context.stop(self)
  }
}