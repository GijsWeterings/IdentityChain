package grpc

import android.support.test.runner.AndroidJUnit4
import com.google.common.util.concurrent.ListenableFuture
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import junit.framework.Assert.fail
import nl.tudelft.cs4160.identitychain.message.ChainGrpc
import nl.tudelft.cs4160.identitychain.message.ChainService
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GrpcTest {

    @Test
    fun testClientAndServer() {
        //start server
        ServerBuilder.forPort(8080).addService(ChainServiceServer()).build().start()

        val mChannel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext(true).build();
        val stub = ChainGrpc.newFutureStub(mChannel)

        val empty = ChainService.Empty.newBuilder().build()
        val metaBlock: ListenableFuture<ChainService.MetaBlock> = stub.getMetaBlock(empty)

        println(metaBlock.get(2, TimeUnit.SECONDS))
        fail()
    }
    class ChainServiceServer : ChainGrpc.ChainImplBase() {
        override fun getMetaBlock(request: ChainService.Empty, responseObserver: StreamObserver<ChainService.MetaBlock>) {
            val metaBlock = ChainService.MetaBlock.newBuilder().setMetaData(42).build()
            responseObserver.onNext(metaBlock)
            responseObserver.onCompleted()
        }
    }
}