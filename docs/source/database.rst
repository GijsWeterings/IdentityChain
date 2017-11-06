******************************
Local chain storage (database)
******************************
Every half block correctly created gets saved locally on the device. Also all the incoming blocks of other peers, either as a response to a crawlrequest or in other ways, are saved locally when validated correctly. The blocks are saved using an SQLite database. Android has code in place to handle all the complicated parts, so using the database after setup consists mainly of writing queries. Please refer to the `Android tutorials <https://developer.android.com/training/basics/data-storage/databases.html>`_ for an explanation on how to use SQLite databases in Android.

The database is set up in the same way as `ipv8 python code <https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/database.py>`_. So the database from implementation of the python code can be imported. Notice that the columns correspond to the attributes of the Protocol Buffers object, so for inserting it simply needs to get the relevant data from the object. Note that it when receiving a raw message it always has to be passed to a Protocol Buffers object first, to ensure that data was received correctly.


Links to code
=============
* `Creation of database, inserting blocks (TrustChainDBHelper.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/database/TrustChainDBHelper.java>`_
* `Retrieving blocks (TrustChainBlock.java) <https://github.com/wkmeijer/CS4160-trustchain-android/blob/master/app/src/main/java/nl/tudelft/cs4160/trustchain_android/block/TrustChainBlock.java>`_
* `IPv8 (database.py) <https://github.com/qstokkink/py-ipv8/blob/master/ipv8/attestation/trustchain/database.py>`_
