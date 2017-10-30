package nl.tudelft.cs4160.trustchain_android.database;

import android.provider.BaseColumns;

public class TrustChainDBContract {
    /**
     * Private constructor to prevent instantiation of class
     */
    private TrustChainDBContract() {}

    /**
     * Inner class defining table contents.
     */
    public static class BlockEntry implements BaseColumns {
        public static final String TABLE_NAME = "block";
        public static final String COLUMN_NAME_TX = "tx";
        public static final String COLUMN_NAME_PUBLIC_KEY = "public_key";
        public static final String COLUMN_NAME_SEQUENCE_NUMBER = "sequence_number";
        public static final String COLUMN_NAME_LINK_PUBLIC_KEY = "link_public_key";
        public static final String COLUMN_NAME_LINK_SEQUENCE_NUMBER = "link_sequence_number";
        public static final String COLUMN_NAME_PREVIOUS_HASH = "previous_hash";
        public static final String COLUMN_NAME_SIGNATURE = "signature";
        public static final String COLUMN_NAME_INSERT_TIME = "insert_time";
        public static final String COLUMN_NAME_BLOCK_HASH = "block_hash";
    }
}
