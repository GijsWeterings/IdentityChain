package nl.tudelft.cs4160.trustchain_android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TrustChainDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TrustChain.db";

    private final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS" + TrustChainDBContract.BlockEntry.TABLE_NAME + " (" +
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
}
