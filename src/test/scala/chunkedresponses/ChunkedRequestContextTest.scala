package chunkedresponses

import akka.actor.{Actor, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FreeSpec}
import org.mockito.Mockito._
import play.api.libs.iteratee.{Input, Enumerator}
import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.routing.RequestContext

import scala.collection.mutable.ListBuffer

class ChunkedRequestContextTest extends FreeSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  implicit val system = ActorSystem("ChunkedRequestContextTest")
  import system.dispatcher

  val testProbe = TestProbe()
  val testResponder = TestActorRef(new Actor {
    val responses = new ListBuffer[HttpMessagePart]()

    override def receive: Receive = {
      case response:HttpResponsePart =>
        responses.append(response)
      case confirmedResponse:Confirmed =>
        responses.append(confirmedResponse.messagePart)
        sender() ! confirmedResponse.sentAck
    }
  })

  val mockRequestContext:RequestContext = mock(classOf[RequestContext])
  val contentType = ContentTypes.`text/plain`

  override protected def beforeEach(): Unit = {
    testResponder.underlyingActor.responses.clear()
    reset(mockRequestContext)
    when(mockRequestContext.responder).thenReturn(testResponder)
  }

  "completeChunked" - {
    "should send an empty response for an empty iterator." in {
      mockRequestContext.completeChunked(contentType, Enumerator.empty)

      testProbe.awaitAssert {
        assert(testResponder.underlyingActor.responses === List(HttpResponse(headers = List(RawHeader("Content-Type", contentType.value)))))
      }
    }
    "should send the appropriate message chunks for a populated iterator." in {
      val chunks = (1 to 5).map(i => HttpData(s"data$i"))

      mockRequestContext.completeChunked(contentType, Enumerator(chunks:_*))

      val expectedChunks = List(
        List(ChunkedResponseStart(HttpResponse(entity = HttpEntity(contentType, chunks.head)))),
        chunks.tail.map(data => MessageChunk(data)),
        List(ChunkedMessageEnd())
      ).flatten

      testProbe.awaitAssert {
        assert(testResponder.underlyingActor.responses === expectedChunks)
      }
    }
    "should ignore empty inputs" in {
      val emptyInputEnumerator: Enumerator[HttpData] =
        Enumerator.enumInput[HttpData](Input.Empty)

      val chunkEnumerator =
        Enumerator(HttpData("Data1")) >>>
          emptyInputEnumerator >>>
          Enumerator(HttpData("Data2")) >>>
          Enumerator.eof

      mockRequestContext.completeChunked(contentType, chunkEnumerator)

      val expectedChunks = List(
        ChunkedResponseStart(HttpResponse(entity = HttpEntity(contentType, HttpData("Data1")))),
        MessageChunk(HttpData("Data2")),
        ChunkedMessageEnd()
      )

      testProbe.awaitAssert {
        assert(testResponder.underlyingActor.responses === expectedChunks)
      }
    }
  }

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}
