package nl.tudelft.cs4160.identitychain.network

import android.content.Context
import android.net.nsd.NsdServiceInfo
import java.net.ServerSocket
import android.net.nsd.NsdManager
import android.util.Log
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposables


class ServiceFactory(val context: Context) {
    val registrationListener: NsdManager.RegistrationListener by lazy { initializeRegistrationListener() }
    val serviceInfo = NsdServiceInfo()
    val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager


    fun initializeDiscoveryServer() {
        val port = 8081
        val serverSocket = ServerSocket(port)

        registerService(port)
    }

    fun startPeerDiscovery(): Observable<PeerItem> {
        return Observable.create {

            val resolveListener = initializeResolveListener(it)

            val discoveryListener = initializeDiscoveryListener(resolveListener)
            nsdManager.discoverServices(
                    serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

            it.setDisposable(Disposables.fromAction {nsdManager.stopServiceDiscovery(discoveryListener)})
        }
    }

    fun registerService(port: Int) {
        // Warning: Multiple Services with the same name will get their name changed automatically,
        // This is checked and corrected in the RegistrationListener::onServiceRegistered method.
        serviceInfo.serviceName = serviceName
        serviceInfo.serviceType = serviceType
        serviceInfo.port = port

        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun initializeRegistrationListener(): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {

            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                // Update serviceName to deal with conflicts.
                serviceInfo.serviceName = NsdServiceInfo.serviceName
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service Registration failed with errorCode $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        }
    }

    fun initializeDiscoveryListener(listener: NsdManager.ResolveListener): NsdManager.DiscoveryListener {

        // Instantiate a new DiscoveryListener
        return object : NsdManager.DiscoveryListener {

            //  Called as soon as service discovery begins.
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service)
                if (service.serviceType != serviceInfo.serviceType) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType + serviceInfo.serviceType)
                } else if (service.serviceName == serviceInfo.serviceName) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + serviceInfo.serviceName)
                } else if (service.serviceName.contains(serviceName)) {
                    nsdManager.resolveService(service, listener)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: " + serviceType)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode)
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode)
                nsdManager.stopServiceDiscovery(this)
            }
        }
    }

    fun initializeResolveListener(emitter: ObservableEmitter<PeerItem>): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + info)

                if (info.serviceName == serviceInfo.serviceName) {
                    Log.d(TAG, "Same IP.")
                    return
                }

                emitter.onNext(PeerItem(info.serviceName))
            }
        }
    }

    companion object {
        val serviceType = "_http._tcp."
        val serviceName = "IdentityChain"
        val TAG = "ServiceFactory"
    }


}
