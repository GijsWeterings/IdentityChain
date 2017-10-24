package nl.tudelft.cs4160.trustchain_android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
            "PRIMARY KEY (" + TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY + "," +
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
     * @param db - The database that holds the TrustChain.
     * @return A long depicting the primary key value of the newly inserted row of the database.
     *          returns -1 as an error indicator.
     */
    public static long insertInDB(MessageProto.TrustChainBlock block, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX, block.getTransaction().toStringUtf8());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY, block.getPublicKey().toStringUtf8());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER, block.getSequenceNumber());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY, block.getLinkPublicKey().toStringUtf8());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER, block.getLinkSequenceNumber());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH, block.getPreviousHash().toStringUtf8());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE, block.getSignature().toStringUtf8());
        values.put(TrustChainDBContract.BlockEntry.COLUMN_NAME_BLOCK_HASH, new String(TrustChainBlock.hash(block)));

        return db.insert(TrustChainDBContract.BlockEntry.TABLE_NAME, null, values);
    }

    /**
     * Retrieves all the blocks inserted in the database.
     * @return a List of all blocks
     */
    public List<MessageProto.TrustChainBlock> getAllBlocks() {
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                TrustChainDBContract.BlockEntry.COLUMN_NAME_TX,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE,
                TrustChainDBContract.BlockEntry.COLUMN_NAME_INSERT_TIME
        };

        String sortOrder =
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER + " ASC";

        Cursor cursor = db.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                projection,                                     // The columns to return
                null,                                           // Filter for which rows to return
                null,                                           // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                sortOrder                                       // How the rows should be ordered
        );

        List<MessageProto.TrustChainBlock> res = new ArrayList<>();
        MessageProto.TrustChainBlock.Builder builder = MessageProto.TrustChainBlock.newBuilder();

        while(cursor.moveToNext()) {

            builder.setTransaction(ByteString.copyFromUtf8(cursor.getString(
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
                            cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE))));

            res.add(builder.build());
        }

        cursor.close();
        return res;
    }

}
