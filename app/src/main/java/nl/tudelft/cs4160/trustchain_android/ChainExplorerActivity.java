package nl.tudelft.cs4160.trustchain_android;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

public class ChainExplorerActivity extends AppCompatActivity {
    TrustChainDBHelper dbHelper;
    SQLiteDatabase db;

    TextView databaseContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chain_explorer);

        initVariables();
        init();
    }

    private void initVariables() {
        databaseContentText = (TextView) findViewById(R.id.database_content);
    }

    private void init() {
        dbHelper = new TrustChainDBHelper(this);
        db = dbHelper.getReadableDatabase();
        displayLocalChain(dbHelper.getAllBlocks());
    }

    public void displayLocalChain(List<MessageProto.TrustChainBlock> blockList) {
        String blocksString = "";
        Iterator<MessageProto.TrustChainBlock> iterator = blockList.iterator();
        while(iterator.hasNext()) {
            blocksString += iterator.next().toString() + "\n\n";
        }
        databaseContentText.setText(blocksString);
    }



}
