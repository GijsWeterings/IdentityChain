***********************************
Message structure (Protocolbuffers)
***********************************
Creating a network of TrustChain peers which only run a Java version of TrustChain is not very useful. Therefore the TrustChain blocks and messages should be compatible with many platforms, so cross-platform connection is possible. For the storage of the chain this is achieved by using SQLite, which has implementation for many platforms. For sending messages (blocks and crawlrequests) this compatibility can be achieved by using `Google's Protocolbuffers <https://developers.google.com/protocol-buffers/>`_, which is a cross-platform data serialization mechanism. Note that this Android implementation was not implemented for compatibility with other TrustChain implementation and thus will not be compatible out of the box.

Making changes
==============
Protocolbuffers is used to create the structure of both a ``TrustChainBlock`` and ``CrawlRequest``, both can be found in `Message.proto <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/Message.proto>`_ With Protocolbuffers the corresponding Java classes can then be compiled. Making changes and recompiling the Java classes is quite easy, just follow the `tutorial of ProtocolBuffers <https://developers.google.com/protocol-buffers/docs/javatutorial>`_ and you should be fine. When making changes, don't forget to also update the database structure.

Links to code
=============
 * `Structure of message (Message.proto) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/Message.proto>`_ 

