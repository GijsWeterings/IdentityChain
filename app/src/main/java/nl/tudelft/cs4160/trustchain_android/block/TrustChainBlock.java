package nl.tudelft.cs4160.trustchain_android.block;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

/**
 * Created by meijer on 20-9-17.
 */

public class TrustChainBlock {
    static final ByteString GENESIS_HASH = ByteString.copyFrom(new byte[] {0x00});
    static final int GENESIS_SEQ = 1;
    static final int UNKNOWN_SEQ = 0;
    static final ByteString EMPTY_SIG = ByteString.copyFrom(new byte[] {0x00});
    static final ByteString EMPTY_PK = ByteString.copyFrom(new byte[] {0x00});

    /**
     * Creates a TrustChain genesis block using protocol buffers.
     * @return block - A BlockProto.TrustChainBlock
     */
    public static BlockProto.TrustChainBlock createGenesisBlock() {
        BlockProto.TrustChainBlock block = BlockProto.TrustChainBlock.newBuilder()
                .setTransaction(ByteString.EMPTY)
                .setPublicKey(EMPTY_PK)
                .setSequenceNumber(GENESIS_SEQ)
                .setLinkPublicKey(EMPTY_PK)
                .setLinkSequenceNumber(UNKNOWN_SEQ)
                .setPreviousHash(GENESIS_HASH)
                .setSignature(EMPTY_SIG)
                .setInsertTime(Timestamp.getDefaultInstance())
                .build();
        return block;
    }


    /**
     * Creates a TrustChainBlock for the given input.
     * @param transaction - Details the message of the block
     * @param pubk - Public Key of this user
     * @param seq_num - Depicting the position in the chain
     * @param link_pubk - Public Key of the linked user
     * @param link_seq_num - Depicting the position in the linked chain
     * @param prev_hash - Hash of the previous block in the chain
     * @param sig - Signature of this block
     * @param time - time the block was created
     * @return
     */
    public static BlockProto.TrustChainBlock createBlock(byte[] transaction, byte[] pubk, int seq_num,
                                                  byte[] link_pubk, int link_seq_num, byte[] prev_hash,
                                                  byte[] sig, Timestamp time) {
        BlockProto.TrustChainBlock block = BlockProto.TrustChainBlock.newBuilder()
                .setTransaction(ByteString.copyFrom(transaction))
                .setPublicKey(ByteString.copyFrom(pubk))
                .setSequenceNumber(seq_num)
                .setLinkPublicKey(ByteString.copyFrom(link_pubk))
                .setLinkSequenceNumber(link_seq_num)
                .setPreviousHash(ByteString.copyFrom(prev_hash))
                .setSignature(ByteString.copyFrom(sig))
                .setInsertTime(time)
                .build();
        return block;
    }




}
