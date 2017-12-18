package nl.tudelft.cs4160.identitychain.modals

import android.app.DialogFragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.network.PeerViewRecyclerAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.activity_peer_connect.view.*


class PeerConnectActivity: DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View? {
        super.onCreateView(inflater, parent, state)

        val view = inflater.inflate(R.layout.activity_peer_connect, parent, false)
        view.discoveryList.layoutManager = LinearLayoutManager(view.context)
        view.discoveryList.adapter = discoveryListAdapter
        return view
    }


    companion object {
        val TAG = "PeerConnectActivity"
    }

    lateinit var discoveryListAdapter: PeerViewRecyclerAdapter
}