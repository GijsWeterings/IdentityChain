package nl.tudelft.cs4160.identitychain.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.peer_connect_fragment.view.*
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.network.PeerItem
import nl.tudelft.cs4160.identitychain.network.PeerViewRecyclerAdapter


class PeerConnectFragment : Fragment() {
    lateinit var viewModel: MainViewModel
    val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View? {
        super.onCreateView(inflater, parent, state)

        val view = inflater.inflate(R.layout.peer_connect_fragment, parent, false)
        view.discoveryList.layoutManager = LinearLayoutManager(view.context)
        val peerViewRecyclerAdapter = PeerViewRecyclerAdapter()
        view.discoveryList.adapter = peerViewRecyclerAdapter

        viewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)

        viewModel.allPeers.observe(this, Observer<PeerItem> {
            peerViewRecyclerAdapter.addItem(it!!)
        })

        val disposable = peerViewRecyclerAdapter.selection().subscribe({ viewModel.select(it.withPort(8080)) })
        disposables.add(disposable)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.dispose()
    }

    companion object {
        val TAG = "PeerConnectFragment"
    }
}