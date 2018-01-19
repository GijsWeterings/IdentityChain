package nl.tudelft.cs4160.identitychain.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.peer_connect_fragment.view.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.network.PeerViewRecyclerAdapter
import nl.tudelft.cs4160.identitychain.peers.KeyedPeer


class PeerConnectFragment : Fragment() {
    lateinit var viewModel: MainViewModel
    val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View? {
        super.onCreateView(inflater, parent, state)

        val view = inflater.inflate(R.layout.peer_connect_fragment, parent, false)
        view.discoveryList.layoutManager = LinearLayoutManager(view.context)
        val peerViewRecyclerAdapter = PeerViewRecyclerAdapter(createNameDialog())
        view.discoveryList.adapter = peerViewRecyclerAdapter

        viewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)

        viewModel.keyedPeers.observe(this, Observer<KeyedPeer> {
            peerViewRecyclerAdapter.addItem(it!!)
        })

        val disposable = peerViewRecyclerAdapter.selection().subscribe({ viewModel.select(it) })
        disposables.add(disposable)

        return view
    }

    fun createNameDialog(): Single<String> {
        return Single.create<String> {
            val input = EditText(this.activity)
            input.inputType = InputType.TYPE_CLASS_TEXT


            AlertDialog.Builder(this.activity)
                    .setTitle("Enter contact id")
                    .setMessage("hi")
//                    .setView(input)
//                    .setPositiveButton("Name Key")  { _, _ ->
//                        it.onSuccess(input.text.toString())
//                    }
//                    .setNegativeButton("Cancel") {dialog, _ ->
//                        dialog.cancel()
//                        it.onError(RuntimeException("Cancel"))}
                    .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.dispose()
    }

    companion object {
        val TAG = "PeerConnectFragment"
    }
}