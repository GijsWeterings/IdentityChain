package nl.tudelft.cs4160.identitychain.connection

import android.os.AsyncTask
import android.util.Log

import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException

import nl.tudelft.cs4160.identitychain.message.MessageProto


/**
 * Class is package private to prevent another activity from accessing it and breaking everything
 */
internal class ClientTask(var destinationIP: String, var destinationPort: Int, var message: MessageProto.Message, private val listener: CommunicationListener) : AsyncTask<Unit?, Unit?, Unit?>() {

    /**
     * Sends the block or crawlrequest as a message to the specified server (another phone)
     * and listens for a response from the server.
     */
    override fun doInBackground(vararg arg0: Unit?): Unit? {
        var loop = true
        while (loop) {
            var socket: Socket? = null
            try {
                Log.i(TAG, "Opening socket to " + destinationIP + ":" + Communication.DEFAULT_PORT)
                socket = Socket(destinationIP, Communication.DEFAULT_PORT)
                message.writeTo(socket.getOutputStream())
                socket.shutdownOutput()

                // check whether we're sending a half block or a message
                if (message.crawlRequest.publicKey.size() == 0) {
                    Log.i(TAG, "Sent half block to peer with ip $destinationIP:$destinationPort")
                    listener.updateLog("\n\nClient: Sent half block to peer with ip $destinationIP:$destinationPort")
                } else {
                    Log.i(TAG, "Sent crawl request to peer with ip $destinationIP:$destinationPort")
                    listener.updateLog("\n\nClient: Sent crawl request to peer with ip $destinationIP:$destinationPort")
                }

            } catch (e: UnknownHostException) {
                e.printStackTrace()
                listener.updateLog("\n  Client: Cannot resolve host")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (socket != null) {
                    try {
                        loop = false
                        socket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }

        }
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * After sending a message and receiving a response from the server, update the log.
     */
    override fun onPostExecute(result: Unit?) {
        listener.updateLog("\n  Send message ")
        super.onPostExecute(result)
    }

    companion object {

        private val TAG = ClientTask::class.java.name
    }

}
