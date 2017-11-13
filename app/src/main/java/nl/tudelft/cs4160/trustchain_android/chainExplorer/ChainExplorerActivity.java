package nl.tudelft.cs4160.trustchain_android.chainExplorer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.security.KeyPair;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

import static android.view.Gravity.CENTER;


public class ChainExplorerActivity extends AppCompatActivity {
    TrustChainDBHelper dbHelper;
    ChainExplorerAdapter adapter;
    ListView blocksList;

    static final String TAG = "ChainExplorerActivity";


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
        KeyPair kp = Key.loadKeys(getApplicationContext());
        try {
            adapter = new ChainExplorerAdapter(this, dbHelper.getAllBlocks(), kp.getPublic().getEncoded());
            blocksList.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        blocksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LinearLayout expandedItem = (LinearLayout) view.findViewById(R.id.expanded_item);
                ImageView expandArrow = (ImageView) view.findViewById(R.id.expand_arrow);

                // Expand the item when it is clicked
                if(expandedItem.getVisibility() == View.GONE) {
                    expandedItem.setVisibility(View.VISIBLE);
                    Log.v(TAG, "Item height: " + expandedItem.getHeight());
                    expandArrow.setImageDrawable(getDrawable(R.drawable.ic_expand_less_black_24dp));
                } else {
                    expandedItem.setVisibility(View.GONE);
                    expandArrow.setImageDrawable(getDrawable(R.drawable.ic_expand_more_black_24dp));
                }
            }
        });
    }

}
