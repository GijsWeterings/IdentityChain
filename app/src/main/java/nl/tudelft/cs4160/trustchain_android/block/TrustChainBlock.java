package nl.tudelft.cs4160.trustchain_android.block;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.security.PublicKey;
import java.util.Arrays;

import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBContract;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

/**
 * Created by meijer on 20-9-17.
 */

public class TrustChainBlock {
    public static final ByteString GENESIS_HASH = ByteString.copyFrom(new byte[] {0x00});
    public static final int GENESIS_SEQ = 1;
    public static final int UNKNOWN_SEQ = 0;
    public static final ByteString EMPTY_SIG = ByteString.copyFrom(new byte[] {0x00});
    public static final ByteString EMPTY_PK = ByteString.copyFrom(new byte[] {0x00});

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

    // TODO: REMOVE
    public static BlockProto.TrustChainBlock createTestBlock() {
        BlockProto.TrustChainBlock block = BlockProto.TrustChainBlock.newBuilder()
                .setTransaction(ByteString.EMPTY)
                .setPublicKey(EMPTY_PK)
                .setSequenceNumber(9)
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
     * @param transaction - Details the message of the block, can be null if there is a linked block
     * @param db - database in which the previous blocks of this peer can be found
     * @param mypubk - the public key of this peer
     * @param linkedBlock - The halfblock that is linked to this to be created half block, can be null
     * @param linkpubk - The public key of the linked peer
     * @return a new half block
     */
    public static BlockProto.TrustChainBlock createBlock(byte[] transaction, SQLiteDatabase db,
                                                         byte[] mypubk, BlockProto.TrustChainBlock linkedBlock,
                                                         byte[] linkpubk) {
        BlockProto.TrustChainBlock latestBlock = getLatestBlock(db,mypubk);

        long millis = System.currentTimeMillis();
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();

        BlockProto.TrustChainBlock.Builder builder = BlockProto.TrustChainBlock.newBuilder();
        if(linkedBlock != null) {
            builder.setTransaction(linkedBlock.getTransaction())
                    .setLinkPublicKey(linkedBlock.getPublicKey())
                    .setLinkSequenceNumber(linkedBlock.getSequenceNumber());
        } else {
            builder.setTransaction(ByteString.copyFrom(transaction))
                    .setLinkPublicKey(ByteString.copyFrom(linkpubk))
                    .setLinkSequenceNumber(TrustChainBlock.UNKNOWN_SEQ);
        }

        // if a latest block was found set the correct fields, if not the block won't be validated
        // so it doesn't matter much that no exception is raised here.
        if(latestBlock != null) {
            builder.setSequenceNumber(latestBlock.getSequenceNumber() + 1)
                    .setPreviousHash(ByteString.copyFrom(hash(latestBlock)));
        }

        builder.setPublicKey(ByteString.copyFrom(mypubk));
        builder.setSignature(EMPTY_SIG);

        return builder.build();
    }

    /**
     * Checks if the given block is a genesis block
     * @param block - TrustChainBlock that we want to check
     * @return boolean - true if the block is a genesis block, false otherwise
     */
    public static boolean isGenesisBlock(BlockProto.TrustChainBlock block) {
        return (block.getSequenceNumber() == GENESIS_SEQ) || (block.getPreviousHash() == GENESIS_HASH);
    }

    /**
     * Returns a sha256 hash of the block.
     * TODO: implement this method
     * @param block
     * @return
     */
    public static byte[] hash(BlockProto.TrustChainBlock block) {
        return new byte[] {0x01};
    }


    /**
     * Signs this block with a given public key.
     * TODO: implement this method
     */
    public static void sign(BlockProto.TrustChainBlock block, byte[] myPubKey) {

    }

    /**
     * Gets the latest block associated with the given public key from the database
     * @param dbReadable - Database to search in
     * @param pubkey - Public key of which the latest block should be found
     * @return The latest block in the database or null if something went wrong
     */
    public static BlockProto.TrustChainBlock getLatestBlock(SQLiteDatabase dbReadable, byte[] pubkey) {
        BlockProto.TrustChainBlock res = null;
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " = ?";
        String[] whereArgs = new String[] {ByteString.copyFrom(pubkey).toStringUtf8(),
                Integer.toString(getMaxSeqNum(dbReadable,pubkey))};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                null                                            // How the rows should be ordered
        );
        if(cursor.getCount() == 1) {
            cursor.moveToFirst();
            int nanos = java.sql.Timestamp.valueOf(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME))).getNanos();

            res = BlockProto.TrustChainBlock.newBuilder().setTransaction(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))))
                    .setPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY))))
                    .setSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER)))
                    .setLinkPublicKey(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY))))
                    .setLinkSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER)))
                    .setPreviousHash(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH))))
                    .setSignature(ByteString.copyFromUtf8(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE))))
                    .setInsertTime(com.google.protobuf.Timestamp.newBuilder().setNanos(nanos))
                    .build();
        }
        cursor.close();
        return res;
    }

    /**
     * Get the maximum sequence number in the database associated with the given public key
     * @param dbReadable - database in which to search
     * @param pubkey - public key for which to search for blocks
     * @return the maximum sequence number found
     */
    public static int getMaxSeqNum(SQLiteDatabase dbReadable, byte[] pubkey) {
        int res = -1;
        String[] projection = new String[] {"max(" +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + ")"};
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ?";
        String[] whereArgs = new String[] {ByteString.copyFrom(pubkey).toStringUtf8()};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,
                projection,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        if(cursor.getCount() == 1) {
            cursor.moveToFirst();
            res = cursor.getInt(cursor.getColumnIndex(
                    "max(" + TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + ")"));
        }
        cursor.close();
        return res;
    }

}
