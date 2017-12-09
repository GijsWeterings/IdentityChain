package nl.tudelft.cs4160.identitychain.network

import android.content.Context
import android.net.nsd.NsdServiceInfo
import java.net.ServerSocket
import android.net.nsd.NsdManager
import android.content.ContentValues.TAG
import android.util.Log


class ServiceFactory(val context: Context) {
    val registrationListener: NsdManager.RegistrationListener by lazy { initializeRegistrationListener() }
    val resolveListener: NsdManager.ResolveListener by lazy { initializeResolveListener() }
    val serviceInfo = NsdServiceInfo()
    val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    lateinit var resolvedService: NsdServiceInfo


    fun registerService(port: Int) {
        // Create the NsdServiceInfo object, and populate it.

        // The name is subject to change based on conflicts
        // with other services advertised on the same network.
        serviceInfo.serviceName = "IdentityChain"
        serviceInfo.serviceType = "_http._tcp"
        serviceInfo.port = port

        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)

    }

    fun initializeServerSocket() {
        val port = 8081
        val serverSocket = ServerSocket(port)

        registerService(port)
        nsdManager.discoverServices(
                "_http._tcp", NsdManager.PROTOCOL_DNS_SD, initializeDiscoveryListener());

    }


    fun initializeRegistrationListener(): NsdManager.RegistrationListener {
        return object : NsdManager.RegistrationListener {

            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                serviceInfo.serviceName = NsdServiceInfo.serviceName
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Registration failed!  Put debugging code here to determine why.
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

    fun initializeDiscoveryListener(): NsdManager.DiscoveryListener {

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
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)
                } else if (service.serviceName == serviceInfo.serviceName) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + serviceInfo.serviceName)
                } else if (service.serviceName.contains("IdentityChain")) {
                    nsdManager.resolveService(service, resolveListener)
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

    fun initializeResolveListener(): NsdManager.ResolveListener {
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
                resolvedService = info
                val port = resolvedService.getPort()
                val host = resolvedService.getHost()
            }
        }
    }


}
