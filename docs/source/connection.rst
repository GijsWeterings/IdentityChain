********************
Connection to a peer
********************
In order to send blocks to other people, you will need to find peers. In this simple app, this has to be done manually. In IPv8, this is done with help of `Dispersy <https://dispersy.readthedocs.io/en/devel/system_overview.html#overlay>`_.

Connecting to a peer is very simple. Find out the IP-address of a peer who has opened the app, listed at the top in the main screen. Fill in the ip address and the port number (default hardcoded as 8080) and press connect. Now the app will go through the steps as explained in :ref:`creating-block-label`.


ServerSocket
============
The connection is made by using the `ServerSocket class <https://developer.android.com/reference/java/net/ServerSocket.html>`_. The implementation in TrustChain Android is a basic client-server model. All outgoing messages, like sending a crawl request or half block, is done by the client. The server handles the incoming messages. It checks whether it has received a half block or a crawl request and calls handles the response by calling either ``synchronizedReceivedHalfBlock`` or ``receivedCrawlRequest``.

If, from looking at the source code, it is not yet clear how the connection is made, please look into other `Android server/client tutorials <http://android-er.blogspot.nl/2014/02/android-sercerclient-example-server.html>`_ that can be found online.


WiFi
====
The simplest way for connecting via IP, which does not have to deal with possible NAT puncturing or port forwarding is connecting to a local IP on the same WiFi network. A guaranteed method involves setting up a WiFi hotspot on one of the devices and letting the other peer connect to this hotspot. WiFi networks, which make use of IPv6 are not guaranteed to work.

.. figure:: ./images/connection_example.png 
	:width: 300px

Example of connecting to a peer using on a device using a WiFi hotspot.


Other possible ways for connecting
==================================
We are looking into other ways of connecting to peers. Currently we are working on connection via Bluetooth.


Links to code
=============
* `Client implementation (ClientTask.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/main/ClientTask.java>`_
* `Server implementation (Server.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/main/Server.java>`_
