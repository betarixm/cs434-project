package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kr.ac.postech.paranode.core.{Block, KeyRange, WorkerMetadata}

import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import scala.concurrent.Future
import worker._
import worker.WorkerGrpc.WorkerStub
import common.{
  Node,
  KeyRange => RpcKeyRange,
  WorkerMetadata => RpcWorkerMetadata
}

object WorkerClient {
  def apply(host: String, port: Int): WorkerClient = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build
    val stub = WorkerGrpc.stub(channel)
    new WorkerClient(channel, stub)
  }
}

class WorkerClient private (
    private val channel: ManagedChannel,
    private val stub: WorkerStub
) {
  Logger.getLogger(classOf[WorkerClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def sample(numberOfKeys: Int): Future[SampleReply] = {
    val request = SampleRequest(numberOfKeys)
    val response = stub.sample(request)

    response
  }

  def sort(): Future[SortReply] = {
    val request = SortRequest()
    val response = stub.sort(request)

    response
  }

  def partition(
      workers: List[(WorkerMetadata, KeyRange)]
  ): Future[PartitionReply] = {
    val request = PartitionRequest(workers.map({ case (worker, keyRange) =>
      RpcWorkerMetadata(
        Some(Node(worker.host, worker.port)),
        Some(
          RpcKeyRange(
            ByteString.copyFrom(keyRange.from.underlying),
            ByteString.copyFrom(keyRange.to.underlying)
          )
        )
      )
    }))

    stub.partition(request)
  }

  def exchange(
      workers: List[(WorkerMetadata, KeyRange)]
  ): Future[ExchangeReply] = {
    val request = ExchangeRequest(workers.map({ case (worker, keyRange) =>
      RpcWorkerMetadata(
        Some(Node(worker.host, worker.port)),
        Some(
          RpcKeyRange(
            ByteString.copyFrom(keyRange.from.underlying),
            ByteString.copyFrom(keyRange.to.underlying)
          )
        )
      )
    }))

    stub.exchange(request)
  }

  def saveBlock(
      block: Block
  ): Future[SaveBlockReply] = {
    val request = SaveBlockRequest(
      ByteString.copyFrom(block.toChars.map(_.toByte).toArray)
    )

    stub.saveBlock(request)
  }

  def merge(): Future[MergeReply] = {
    val request = MergeRequest()
    stub.merge(request)
  }
}
