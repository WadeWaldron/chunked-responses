import akka.actor.{ActorContext, ActorRefFactory, Props}
import play.api.libs.iteratee.Enumerator
import spray.http.{HttpData, ContentType}
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext

package object chunkedresponses {
  implicit class ChunkedRequestContext(requestContext: RequestContext) {
    def completeChunked(contentType: ContentType, enumerator: Enumerator[HttpData])
                       (implicit executionContext: ExecutionContext, actorRefFactory: ActorRefFactory) {
      val chunkedResponder = actorRefFactory.actorOf(Props(new ChunkedResponder(contentType, requestContext.responder)))
      val iteratee = new ChunkIteratee(chunkedResponder)
      enumerator.run(iteratee)
    }
  }
}
