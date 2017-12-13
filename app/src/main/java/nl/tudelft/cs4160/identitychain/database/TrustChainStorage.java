package nl.tudelft.cs4160.identitychain.database;

import java.util.List;

import nl.tudelft.cs4160.identitychain.message.MessageProto;

interface TrustChainStorage {
    long insertInDB(MessageProto.TrustChainBlock block);

    MessageProto.TrustChainBlock getBlock(byte[] pubkey, int seqNumber);

    MessageProto.TrustChainBlock getLinkedBlock(MessageProto.TrustChainBlock block);

    MessageProto.TrustChainBlock getBlockBefore(byte[] pubkey, int seqNumber);

    MessageProto.TrustChainBlock getBlockAfter(byte[] pubkey, int seqNumber);

    MessageProto.TrustChainBlock getLatestBlock(byte[] pubkey);

    int getMaxSeqNum(byte[] pubkey);

    List<MessageProto.TrustChainBlock> getAllBlocks();

    List<MessageProto.TrustChainBlock> crawl(byte[] pubKey, int seqNum, int limit) throws Exception;

    // uses the default limit of 100
    List<MessageProto.TrustChainBlock> crawl(byte[] pubKey, int seqNum);
}
