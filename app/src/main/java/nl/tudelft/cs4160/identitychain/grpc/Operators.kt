package nl.tudelft.cs4160.identitychain.grpc

import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Scheduler
import io.reactivex.Single

fun <A> ListenableFuture<A>.guavaAsSingle(scheduler: Scheduler): Single<A> {
    val worker = scheduler.createWorker()
    return Single.create { emitter ->
        this.addListener({
            try {
                //the future should be done here
                emitter.onSuccess(this.get())
            } catch (t: Throwable) {
                emitter.onError(t)
            }

        }, {
            worker.schedule({
                try {
                    it.run()
                } finally {
                    worker.dispose()
                }
            })
        })
    }
}