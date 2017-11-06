.. TrustChain Android documentation master file, created by
   sphinx-quickstart on Fri Sep  8 15:43:27 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

==============================================
Introduction
==============================================
.. toctree::
   :maxdepth: 2
   :caption: Contents:


TrustChain Android is a native Android app implementing the TU Delft style blockchain, called TrustChain. This app provides an accessible way to understand and to use TrustChain. The app is build as part of a Blockchain Engineering course of the TU Delft. It is meant as a basic building block to experiment with blockchain technology. This documentation should get you started in the workings of the app, however for thorough understanding, reading other documentation and looking at the source code is a necessity.

We have tried to make the code clear. However, this app was not build by Android experts so please don't hold any mistakes or weird structures for Android against us. Instead, please let us know what could be improved, or provide a fix yourself by submitting a pull request on `GitHub <https://github.com/wkmeijer/CS4160-trustchain-android>`_.

===================
What is TrustChain?
===================
* basics
* references to ipv8
* main differences from bitcoin like blockchains
* dispersy

.. _creating-block-label:

================
Creating a block
================
* go through all the steps
* half block
* validation
* crawl requests



====================
Connection to a peer
====================
In order to send blocks to other people, you will need to find peers. In this simple app, this has to be done manually. In IPv8, this is done with help of `Dispersy <https://dispersy.readthedocs.io/en/devel/system_overview.html#overlay>`_.

Connecting to a peer is very simple. Find out the IP-address of a peer who has opened the app, listed at the top in the main screen. Fill in the ip address and the port number (default hardcoded as 8080) and press connect. Now the app will go through the steps as explained in :ref:`creating-block-label`.

------------
ServerSocket
------------
The connection is made by using the `ServerSocket class <https://developer.android.com/reference/java/net/ServerSocket.html>`_. The implementation in TrustChain Android is a basic client-server model. All outgoing messages, like sending a crawl request or half block, is done by the client. The server handles the incoming messages. It checks whether it has received a half block or a crawl request and calls handles the response by calling either ``synchronizedReceivedHalfBlock`` or ``receivedCrawlRequest``.

If, from looking at the source code, it is not yet clear how the connection is made, please look into other `Android server/client tutorials <http://android-er.blogspot.nl/2014/02/android-sercerclient-example-server.html>`_ that can be found online.

----
WiFi
----
The simplest way for connecting via IP, which does not have to deal with possible NAT puncturing or port forwarding is connecting to a local IP on the same WiFi network. A guaranteed method involves setting up a WiFi hotspot on one of the devices and letting the other peer connect to this hotspot. WiFi networks, which make use of IPv6 are not guaranteed to work.

----------------------------------
Other possible ways for connecting
----------------------------------
We are looking into other ways of connecting to peers. Currently we are working on connection via Bluetooth.

-------------
Links to code
-------------
* `Client implementation (ClientTask.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/main/ClientTask.java>`_
* `Server implementation (Server.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/main/Server.java>`_


===================================
Sending a message (protocolbuffers)
===================================
* Explain google protcolbuffers in short (advantages)
* reference to google tutorial
* Explain in short how to make changes

==============================
Local chain storage (database)
==============================
Every half block correctly created gets saved locally on the device. Also all the incoming blocks of other peers, either as a response to a crawlrequest or in other ways, are saved locally when validated correctly. The blocks are saved using an SQLite database. Android has code in place to handle all the complicated parts, so using the database after setup consists mainly of writing queries. Please refer to the `Android tutorials <https://developer.android.com/training/basics/data-storage/databases.html>`_ for an explanation on how to use SQLite databases in Android.

The database is set up in the same way as `ipv8 python code <https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/database.py>`_. So the database from implementation of the python code can be imported. Notice that the columns correspond to the attributes of the Protocol Buffers object, so for inserting it simply needs to get the relevant data from the object. Note that it when receiving a raw message it always has to be passed to a Protocol Buffers object first, to ensure that data was received correctly.

-------------
Links to code
-------------
* `Creation of database, inserting blocks (TrustChainDBHelper.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/database/TrustChainDBHelper.java>`_
* `Retrieving blocks (TrustChainBlock.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/block/TrustChainBlock.java>`_
* `IPv8 (database.py) <https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/database.py>`_

=======
Contact
=======



============
Useful links
============
* `App source code on GitHub <https://github.com/wkmeijer/CS4160-trustchain-android>`_
* `TrustChain source code on IPv8 GitHub <https://github.com/qstokkink/py-ipv8/tree/master/ipv8/attestation/trustchain>`_
* `Dispersy ReadTheDocs <https://dispersy.readthedocs.io/en/devel/>`_
* `Tribler GitHub <https://github.com/Tribler/tribler>`_
* `Blockchain Lab TU Delft <http://www.blockchain-lab.org/>`_
* `Paper | TrustChain: A Sybil-resistant scalable blockchain <https://www.sciencedirect.com/science/article/pii/S0167739X17318988/>`_
