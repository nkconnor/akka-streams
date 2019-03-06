package akka.stream.alpakka.sqs.newdsl

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.{Flow, Source}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message, ReceiveMessageRequest}

import scala.compat.java8.FutureConverters._
import scala.collection.JavaConverters._
import concurrent.duration._

/**
  * Author: Nicholas Connor
  * Date: 3/6/19
  * Package: akka.stream.alpakka.sqs.newdsl
  */
case class SqsMessage(queueUrl: String, message: Message)

object SqsGetFlow {
  def apply(paralellism: Int)(implicit sqsAsyncClient: SqsAsyncClient): Flow[MessageRequest.Get, Message, NotUsed] =
    Flow[MessageRequest.Get].mapAsync(paralellism) { get =>
      sqsAsyncClient.receiveMessage(
        ReceiveMessageRequest.builder()
          .queueUrl(get.queueUrl)
          .maxNumberOfMessages(get.limit)
          .build()
      ).thenApply[Iterator[Message]] { mr =>
        mr.messages().iterator().asScala
      }.toScala
    }.flatMapConcat(f => Source.fromIterator(() => f))
}

object SqsSource {
  def apply()(implicit sqsAsyncClient: SqsAsyncClient): Source[Message, Cancellable] = {
    Source.tick(0.seconds, 1.second, Unit)
      // will backpressure make it this far?
      .mapAsync(1){ _ =>
      sqsAsyncClient.receiveMessage(
        ReceiveMessageRequest.builder()
          .queueUrl("")
          .build()
      ).thenApply[Iterator[Message]] { mr =>
        mr.messages().iterator().asScala
      }.toScala
    }.flatMapConcat(f => Source.fromIterator(() => f))
  }
}