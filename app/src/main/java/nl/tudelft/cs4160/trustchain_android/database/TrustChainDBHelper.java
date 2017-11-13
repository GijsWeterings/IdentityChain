package nl.tudelft.cs4160.trustchain_android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

public class TrustChainDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TrustChain.db";

    private final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + TrustChainDBContract.BlockEntry.TABLE_NAME + " (" +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_TX + " TEXT NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " TEXT NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " INTEGER NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY + " TEXT NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER + " INTEGER NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH + " TEXT NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE + " TEXT NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_BLOCK_HASH + " TEXT NOT NULL," +
            "PRIMARY KEY (" + TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + "," +
            TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + ")" +
            ");" +

            "CREATE TABLE option(key TEXT PRIMARY KEY, value BLOB);" +
            "INSERT INTO option(key, value) VALUES('database_version','" + DATABASE_VERSION + "');";

    public TrustChainDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    /**
     * When the database is upgraded, create a new database next to the old one.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    /**
     * When the database is downgraded, create a new database next to the old one.
     */
    public void onDownGrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Insert a block into the database
     * @param block - The protoblock that needs to be added to the database
     * @return A long depicting the primary key value of the newly inserted row of the database.
     *          returns -1 as an error indicator.
     */
    public long insertInDB(MessageProto.TrustChainBlock block) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX, block.getTransaction().toStringUtf8());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY, Base64.encodeToString(block.getPublicKey().toByteArray(), Base64.DEFAULT));
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER, block.getSequenceNumber());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY, Base64.encodeToString(block.getLinkPublicKey().toByteArray(), Base64.DEFAULT));
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER, block.getLinkSequenceNumber());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH, Base64.encodeToString(block.getPreviousHash().toByteArray(), Base64.DEFAULT));
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE, Base64.encodeToString(block.getSignature().toByteArray(), Base64.DEFAULT));
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_BLOCK_HASH, Base64.encodeToString(TrustChainBlock.hash(block), Base64.DEFAULT));

        return db.insert(TrustChainDBContract.BlockEntry.TABLE_NAME, null, values);
    }

    /**
     * Retrieves the block associated with the given public key and sequence number from the database
     * @param pubkey - Public key of which the latest block should be found
     * @param seqNumber - Int value of the sequence number of the block to be retrieved
     * @return The latest block in the database or null if something went wrong
     */
    public MessageProto.TrustChainBlock getBlock(byte[] pubkey, int seqNumber) {
        SQLiteDatabase dbReadable = getReadableDatabase();
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " = ?";
        String[] whereArgs = new String[] { Base64.encodeToString(pubkey, Base64.DEFAULT),
                Integer.toString(seqNumber)};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                null                                            // How the rows should be ordered
        );

        List<MessageProto.TrustChainBlock> res = buildBlocksList(cursor);
        cursor.close();

        if(res.size() == 1) {
            return res.get(0);
        }
        return null;
    }

    /**
     * Retrieves the block linked with the given block
     * @param block - The block for which to get the linked block
     * @return The linked block
     */
    public MessageProto.TrustChainBlock getLinkedBlock(MessageProto.TrustChainBlock block) {
        SQLiteDatabase dbReadable = getReadableDatabase();
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " = ? OR " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER + " = ?";
        String[] whereArgs = new String[] { Base64.encodeToString(block.getLinkPublicKey().toByteArray(), Base64.DEFAULT),
                Integer.toString(block.getLinkSequenceNumber()),
                Base64.encodeToString(block.getPublicKey().toByteArray(), Base64.DEFAULT),
                Integer.toString(block.getSequenceNumber())};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                null                                            // How the rows should be ordered
        );

        List<MessageProto.TrustChainBlock> res = buildBlocksList(cursor);
        cursor.close();

        if(res.size() == 1) {
            return res.get(0);
        }

        return null;
    }

    /**
     * Returns the block with the highest sequence number smaller than the given sequence number and
     * the same public key: the previous block in the chain. Sequence number is allowed to be another
     * value than seqNumber - 1.
     * @param pubkey - Public key of the block of which to find the previous block in the chain
     * @param seqNumber - Sequence number of block of which to find the previous block in the chain
     * @return The previous TrustChainBlock in the chain
     */
    public MessageProto.TrustChainBlock getBlockBefore(byte[] pubkey, int seqNumber){
        SQLiteDatabase dbReadable = getReadableDatabase();
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " < ?";
        String[] whereArgs = new String[] {Base64.encodeToString(pubkey, Base64.DEFAULT),
                Integer.toString(seqNumber)};
        String orderBy = TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " DESC";

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                orderBy                                            // How the rows should be ordered
        );

        List<MessageProto.TrustChainBlock> res = buildBlocksList(cursor);
        cursor.close();

        if(res.size() >= 1) {
            return res.get(0);
        }
        return null;
    }

    /**
     * Returns the block with the lowest sequence number greater than the given sequence number and
     * the same public key: The next block in the chain. Sequence number is allowed to be another
     * value than seqNumber + 1.
     * @param pubkey - Public key of the block of which to find the previous block in the chain
     * @param seqNumber - Sequence number of block of which to find the previous block in the chain
     * @return The next TrustChainBlock in the chain
     */
    public MessageProto.TrustChainBlock getBlockAfter(byte[] pubkey, int seqNumber){
        SQLiteDatabase dbReadable = getReadableDatabase();
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ? AND " +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " > ?";
        String[] whereArgs = new String[] { Base64.encodeToString(pubkey, Base64.DEFAULT),
                Integer.toString(seqNumber)};
        String orderBy = TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " ASC";

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return, in this case all columns
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                orderBy                                         // How the rows should be ordered
        );

        List<MessageProto.TrustChainBlock> res = buildBlocksList(cursor);
        cursor.close();

        if(res.size() >= 1) {
            return res.get(0);
        }
        return null;
    }

    /**
     * Returns the latest block in the database associated with the given public key.
     * @param pubkey - public key for which to search for blocks
     * @return
     */
    public MessageProto.TrustChainBlock getLatestBlock(byte[] pubkey) {
        return getBlock(pubkey,getMaxSeqNum(pubkey));
    }

    /**
     * Get the maximum sequence number in the database associated with the given public key
     * @param pubkey - public key for which to search for blocks
     * @return the maximum sequence number found
     */
    public int getMaxSeqNum(byte[] pubkey) {
        SQLiteDatabase dbReadable = getReadableDatabase();
        int res = -1;
        String[] projection = new String[] {"max(" +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + ")"};
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ?";
        String[] whereArgs = new String[] {Base64.encodeToString(pubkey, Base64.DEFAULT)};

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

    /**
     * Retrieves all the blocks inserted in the database.
     * @return a List of all blocks
     */
    public List<MessageProto.TrustChainBlock> getAllBlocks() {
        SQLiteDatabase db = getReadableDatabase();

        String sortOrder =
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " ASC";

        Cursor cursor = db.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return
                null,                                           // Filter for which rows to return
                null,                                           // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                sortOrder                                       // How the rows should be ordered
        );

        List<MessageProto.TrustChainBlock> res = buildBlocksList(cursor);
        cursor.close();
        return res;
    }

    /**
     * Searches the database for the blocks from the given sequence number to some limit and returns
     * a list of these blocks.
     * When no limit is given the default limit of 100 is used.
     * Limit may not be higher than 100 to prevent sending huge amounts of blocks, potentially
     * slowing down the network.
     * @param pubKey - public key of the chain to from which blocks need to be fetched
     * @param seqNum - sequence number of block, the blocks inserted after this block should be returned
     * @param limit - the limit of the amount of blocks to return
     * @return
     */
    public List<MessageProto.TrustChainBlock> crawl(byte[] pubKey, int seqNum, int limit) throws Exception {
        if(limit > 100) {
            throw new Exception("Limit is too high, don't fetch too much.");
        }
        SQLiteDatabase dbReadable = getReadableDatabase();

        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME + " >= ?" +
                " AND (" + TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ?" +
                " OR " + TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY + " = ?)";
        String[] whereArgs = new String[] {Long.toString(getMaxInsertTime(pubKey,seqNum)),
                Base64.encodeToString(pubKey, Base64.DEFAULT),
                Base64.encodeToString(pubKey, Base64.DEFAULT)};
        String sortOrder =
                TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME + " ASC";
        String rowsLimit = Integer.toString(limit);

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                null,                                           // The columns to return
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                sortOrder,                                      // How the rows should be ordered
                rowsLimit                                       // Sets the maximum rows to be returned
        );

        List<MessageProto.TrustChainBlock> res = buildBlocksList(cursor);
        cursor.close();

        return res;
    }

    // uses the default limit of 100
    public List<MessageProto.TrustChainBlock> crawl(byte[] pubKey, int seqNum) {
        List<MessageProto.TrustChainBlock> blockList = new ArrayList<>();
        try {
            blockList = crawl(pubKey,seqNum,100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blockList;
    }

    /**
     * Get the maximum insert time of a block in the database given a public key and a maximum
     * sequence number.
     * @param pubKey - the public key for which to find associated blocks
     * @param seqNum - the maximum sequence number of the blocks
     * @return long - a maximum insert time
     */
    public long getMaxInsertTime(byte[] pubKey, int seqNum) {
        SQLiteDatabase dbReadable = getReadableDatabase();

        long res = 0;
        String[] projection = new String[] {"max(" +
                TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME + ")"};
        String whereClause = TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY + " = ?" +
                " AND " + TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " <= ?";
        String[] whereArgs = new String[] { Base64.encodeToString(pubKey, Base64.DEFAULT),
                Integer.toString(seqNum)};

        Cursor cursor = dbReadable.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                projection,                                           // The columns to return
                whereClause,                                    // Filter for which rows to return
                whereArgs,                                      // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                null                                       // How the rows should be ordered
        );
        if(cursor.getCount() == 1) {
            cursor.moveToFirst();
            res = cursor.getInt(cursor.getColumnIndex(
                    "max(" + TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME + ")"));
        }
        cursor.close();
        return res;
    }

    /**
     * Builds an List of Blocks from a Cursor retrieved by a database request.
     * @param cursor - The cursor holding the database results
     * @return List of TrustChainBlocks
     */
    private List<MessageProto.TrustChainBlock> buildBlocksList(Cursor cursor) {
        List<MessageProto.TrustChainBlock> res = new ArrayList<>();
        MessageProto.TrustChainBlock.Builder builder = MessageProto.TrustChainBlock.newBuilder();

        while(cursor.moveToNext()) {
            builder.setTransaction(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))))
                    .setPublicKey(ByteString.copyFrom( Base64.decode(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY)), Base64.DEFAULT)))
                    .setSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER)))
                    .setLinkPublicKey(ByteString.copyFrom( Base64.decode(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY)), Base64.DEFAULT)))
                    .setLinkSequenceNumber(cursor.getInt(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER)))
                    .setPreviousHash(ByteString.copyFrom(Base64.decode(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH)), Base64.DEFAULT)))
                    .setSignature(ByteString.copyFrom(Base64.decode(cursor.getString(
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE)), Base64.DEFAULT)));

            res.add(builder.build());
        }

        return res;
    }

}
