package nl.tudelft.cs4160.identitychain.network

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import nl.tudelft.cs4160.identitychain.R
import kotlinx.android.synthetic.main.peer_view.view.*
import nl.tudelft.cs4160.identitychain.message.ChainService

class PeerViewRecyclerAdapter : RecyclerView.Adapter<RecyclerViewHolder>() {
    val peers: MutableList<PeerItem> = ArrayList()
    private val clickedItems: PublishSubject<PeerItem> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.peer_view, parent, false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int = peers.size

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.textview.text = peers[position].name
        holder.itemView.setOnClickListener { clickedItems.onNext(peers[position]) }
    }

    fun addItem(item: PeerItem) {
        peers.add(item)
        val size = peers.size

        this.notifyItemInserted(size - 1)
    }

    fun selection() = clickedItems.toFlowable(BackpressureStrategy.BUFFER)
}

class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textview = view.peerName
}

data class PeerItem(val name: String, val host: String, val port: Int) {

    fun withPort(port: Int) = PeerItem(name, host, port)

    fun asPeerMessage() = ChainService.Peer.newBuilder().setHostname(host).setPort(port).build()
}