package nl.tudelft.cs4160.trustchain_android.chainExplorer


import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_chain_explorer.*
import kotlinx.android.synthetic.main.item_trustchainblock.view.*
import nl.tudelft.cs4160.trustchain_android.R
import nl.tudelft.cs4160.trustchain_android.Util.Key
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper

class ChainExplorerActivity : AppCompatActivity() {
    lateinit internal var dbHelper: TrustChainDBHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chain_explorer)

        // Create a progress bar to display while the list loads
        val progressBar = ProgressBar(this)
        progressBar.layoutParams = LinearLayout.LayoutParams(GridLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, CENTER.toFloat())
        progressBar.isIndeterminate = true
        blocksList.emptyView = progressBar

        // Must add the progress bar to the root of the layout
        val content = findViewById<ViewGroup>(android.R.id.content)
        content.addView(progressBar)

        init()
    }

    private fun init() {
        dbHelper = TrustChainDBHelper(this)
        val kp = Key.loadKeys(applicationContext)
        try {
            blocksList.adapter = ChainExplorerAdapter(this, dbHelper.allBlocks, kp!!.public.encoded)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        blocksList.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // Expand the item when it is clicked
            if (view.expandedItem.visibility == View.GONE) {
                view.expandedItem.visibility = View.VISIBLE
                Log.v(TAG, "Item height: " + view.expandedItem.height)
                view.expandArrow.setImageDrawable(getDrawable(R.drawable.ic_expand_less_black_24dp))
            } else {
                view.expandedItem.visibility = View.GONE
                view.expandArrow.setImageDrawable(getDrawable(R.drawable.ic_expand_more_black_24dp))
            }
        }
    }

    companion object {

        internal val TAG = "ChainExplorerActivity"
    }

}
