.. _creating-block-label:

****************
Creating a block
****************
In order to complete a transaction with a peer, we need to create a block. A block in TrustChain is a little different than in bitcoin-style blockchains. In bitcoin-style blockchains, a block is a collection of transactions that happened in the network. A block is created by a node and is propagated through the network. All connected nodes validate the block and the transactions. In TrustChain a block is formed by two peers who wish to agree on a transaction. Therefore a TrustChainBlock only has one transaction.

Both parties need to agree on a transaction, so there has to be some interaction between peers. The way this is done in TrustChain is to first create an incomplete block, called a half block. This half block is then send to the second peer, which creates a full block from the half block and sends it back to the first peer. This process is explained in more detail below.

Structure of blocks
===================
A block has the following attributes:

* ``public_key`` - The public key of the peer that created this block
* ``sequence_number`` - Represents the position this block has in the chain of the creating peer
* ``link_public_key`` - The public key of the other party
* ``link_sequence_number`` - The position the connected block has in the chain of the other party
* ``previous_hash`` - A hash of the previous block in the chain
* ``signature`` - The signature of the hash of this block
* ``transaction`` - The data that both parties need to agree on, this can be anything, from text to documents to monetary transactions

Note that ``link_sequence_number`` will be unknown for the first half block created, because peer A won't be sure when peer B inserts the linked block in his chain. This will stay unknown, as updating a block already in the chain is not desirable, since it might invalidate later blocks. When an interaction is completed peer A will have the block of peer B in its database as well, so it can always find out the position of the linked block in peer B's chain.

Create block
============
There are two situation that require creating a block. Initiating the creation of a transaction with another peer and completing a block send to you by another peer. This is both done by calling the ``signBlock`` method in `Communication.java <https://github.com/wkmeijer/CS4160-trustchain-android/blob/develop/app/src/main/java/nl/tudelft/cs4160/trustchain_android/connection/Communication.java>`_. This method calls the ``createBlock`` method in `TrustChainBlock.java <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/block/TrustChainBlock.java>`_, signs the block, and validates the correctness of the block, before it gets added to the chain and send.

Initiating a transaction
------------------------
When you want to initiate a transaction, you need to provide the bytes of the transaction, your public key, and the public key of the other party, and a link to the database containing your chain to ``createBlock``. The latest block in your chain will be retrieved from the database, to be able to set ``sequence_number`` and ``prev_hash``. The other attributes will be set according to the input, ``signature`` will remain empty for now.

Received a half block
---------------------
A half block was received and it contains a transaction that we agree with. In this Android implementation we always want to complete the block, regardless of the transaction, so we don't need to check the transaction. The attributes are again set according to the input, with as difference that we now retrieve ``transaction`` and ``link_sequence_number`` from the linked block. ``signature`` will again remain empty.

Sign block
==========
The next step is signing the block. This is as simple as creating a sha256 hash of the block and giving this digest to the build-in signing function of the crypto library.

Validate block
==============
Block validation is the most important step here, as this ensures the validity of the blockchain. The validation function is located in `TrustChainBlock.java <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/block/TrustChainBlock.java>`_. There are 6 different validation results:

* ``VALID``
* ``PARTIAL`` - There are gaps between this block and the previous and next
* ``PARTIAL_NEXT`` - There is a gap between this block and the next
* ``PARTIAL_PREVIOUS`` - There is a gap between this block and the previous
* ``NO_INFO`` - We know nothing about this block, it is from an unknown peer, so we can say nothing about its validity
* ``INVALID`` - It violates some rules

The validation function starts of with a valid result and will update the validity result according to whether the rules hold for the block. Validation consists of six steps:

* Step 1: Retrieving all the relevant blocks from the database if they exist (previous, next, linked, this block)
* Step 2: Determine the maximum validity level according to the blocks retrieved in the previous step
* Step 3: Check whether the block is created correctly, e.g. whether it has a sequence number that comes after the sequence number of the genesis block
* Step 4: Check if we already know this block, if so it should be the same as we have in our database
* Step 5: Check if we know the linked block and check if their relation is correct
* Step 6: Check the validity of the previous and next block

For a more detailed explanation of the validation function, please take a look in the code and try to understand what happens there.


Links to code
=============
* `Block structure in ProtocolBuffers (Message.proto) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/Message.proto>`_
* `All block related methods (TrustChainBlock.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/block/TrustChainBlock.java>`_
* `Sign block method (Communication.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/connection/Communication.java>`_
* `Validation result (ValidationResult.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/block/ValidationResult.java>`_

Also see the `readme on the ipv8 github <https://github.com/qstokkink/py-ipv8/blob/master/doc/trustchain.md>`_

