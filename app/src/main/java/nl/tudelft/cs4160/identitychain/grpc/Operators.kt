package nl.tudelft.cs4160.identitychain.grpc

import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

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

/**
 * This is a work around for the network on main thread exception.
 * Even though the we use the async version of grpc, it does some network call
 * that sometimes triggers the network on main thread exception as setup.
 *
 * This runs that code in the back ground aswell
 */
fun <T> startNetworkOnComputation(f: () -> Single<T>): Single<T> {
    return Single.defer{
        f()
    }.subscribeOn(Schedulers.io())
}