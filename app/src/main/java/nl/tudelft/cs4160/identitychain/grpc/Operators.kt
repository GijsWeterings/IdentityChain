package nl.tudelft.cs4160.identitychain.grpc

import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

fun <A> ListenableFuture<A>.guavaAsSingle(scheduler: Scheduler): Single<A> {
    return Single.fromFuture(this, Schedulers.io())
}

/**
 * This is a work around for the network on main thread exception.
 * Even though the we use the async version of grpc, it does some network call
 * that sometimes triggers the network on main thread exception as setup.
 *
 * This runs that code in the back ground aswell
 */
fun <T> startNetworkOnComputation(f: () -> Single<T>): Single<T> {
    return Single.defer {
        f()
    }.subscribeOn(Schedulers.io())
}