package nl.tudelft.cs4160.trustchain_android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.protobuf.ByteString;

import org.w3c.dom.Text;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.block.BlockProto;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBContract;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

public class trustChainExplorerActivity extends AppCompatActivity {
    TrustChainDBHelper dbHelper;
    SQLiteDatabase db;

    TextView databaseContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trust_chain_explorer);

        initVariables();
        init();
    }

    private void initVariables() {
        databaseContentText = (TextView) findViewById(R.id.database_content);
    }

    private void init() {
        dbHelper = new TrustChainDBHelper(this);
        db = dbHelper.getReadableDatabase();
    }

    public List<BlockProto.TrustChainBlock> getAllBlocks() {
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
                TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER+ "ASC";

        Cursor cursor = db.query(
                TrustChainDBContract.BlockEntry.TABLE_NAME,     // Table name for the query
                projection,                                     // The columns to return
                null,                                           // Filter for which rows to return
                null,                                           // Filter arguments
                null,                                           // Declares how to group rows
                null,                                           // Declares which row groups to include
                sortOrder                                       // How the rows should be ordered
        );

        List res = new ArrayList<>();
        BlockProto.TrustChainBlock.Builder builder = BlockProto.TrustChainBlock.newBuilder();

        while(cursor.moveToNext()) {
            builder.setTransaction(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))));
            builder.setPublicKey(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PUBLIC_KEY))));
            builder.setSequenceNumber(cursor.getInt(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SEQUENCE_NUMBER)));
            builder.setLinkPublicKey(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_PUBLIC_KEY))));
            builder.setLinkSequenceNumber(cursor.getInt(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_LINK_SEQUENCE_NUMBER)));
            builder.setPreviousHash(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_PREVIOUS_HASH))));
            builder.setSignature(ByteString.copyFromUtf8(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_SIGNATURE))));
            int nanos = Timestamp.valueOf(cursor.getString(
                    cursor.getColumnIndex(TrustChainDBContract.BlockEntry.COLUMN_NAME_TX))).getNanos();
            builder.setInsertTime(com.google.protobuf.Timestamp.newBuilder().setNanos(nanos));

            res.add(builder.build());
        }

        cursor.close();
        return res;
    }



}
