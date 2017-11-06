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
* possible ways for connecting
* WiFi
* java socketserver


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
* `ipv8 (database.py) <https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/database.py>`_
=======
Contact
=======



============
Useful links
============
* `App source code on GitHub <https://github.com/wkmeijer/CS4160-trustchain-android>`_
* `TrustChain source code on ipv8 GitHub <https://github.com/qstokkink/py-ipv8/tree/master/ipv8/attestation/trustchain>`_
* `Dispersy ReadTheDocs <https://dispersy.readthedocs.io/en/devel/>`_
* `Tribler GitHub <https://github.com/Tribler/tribler>`_
* `Blockchain Lab TU Delft <http://www.blockchain-lab.org/>`_
* `Paper | TrustChain: A Sybil-resistant scalable blockchain <https://www.sciencedirect.com/science/article/pii/S0167739X17318988/>`_
