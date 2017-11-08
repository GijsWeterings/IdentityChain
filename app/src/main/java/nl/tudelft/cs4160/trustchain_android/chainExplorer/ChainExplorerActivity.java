package nl.tudelft.cs4160.trustchain_android.chainExplorer;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

import static android.view.Gravity.CENTER;


public class ChainExplorerActivity extends AppCompatActivity {
    TrustChainDBHelper dbHelper;
    SQLiteDatabase db;
    ChainExplorerAdapter adapter;
    ListView blocksList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chain_explorer);
        blocksList = (ListView) findViewById(R.id.blocks_list);

        // Create a progress bar to display while the list loads
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, CENTER));
        progressBar.setIndeterminate(true);
        blocksList.setEmptyView(progressBar);

        // Must add the progress bar to the root of the layout
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(progressBar);

        init();
    }

    private void init() {
        dbHelper = new TrustChainDBHelper(this);
        try {
            adapter = new ChainExplorerAdapter(this, dbHelper.getAllBlocks());
            blocksList.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        blocksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Do something when a list item is clicked

            }
        });
    }

}
