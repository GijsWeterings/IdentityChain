package nl.tudelft.cs4160.identitychain.network

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
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
    private val peers: MutableList<SelectablePeer> = ArrayList()
    private var selected: SelectablePeer? = null
    private val clickedItems: PublishSubject<PeerItem> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.peer_view, parent, false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int = peers.size

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val selectedPeer = peers[position]
        holder.textview.text = selectedPeer.peer.name
        val view = holder.cardView
        view.setCardBackgroundColor(colorForSelection(selectedPeer, view))

        holder.itemView.setOnClickListener {
            selected?.selected = false
            selectedPeer.selected = !selectedPeer.selected
            holder.itemView.isActivated = selectedPeer.selected
            clickedItems.onNext(selectedPeer.peer)

            view.setCardBackgroundColor(colorForSelection(selectedPeer, view))
        }
    }

    private fun colorForSelection(selectedPeer: SelectablePeer, view: CardView): Int {
        if (selectedPeer.selected) {
            return ContextCompat.getColor(view.context, R.color.colorPrimary)
        } else {
            return Color.TRANSPARENT
        }
    }

    fun addItem(item: PeerItem) {
        peers.add(SelectablePeer(false, item))
        val size = peers.size

        this.notifyItemInserted(size - 1)
    }

    fun selection() = clickedItems.toFlowable(BackpressureStrategy.BUFFER)
}

class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textview = view.peerName
    val cardView = view.peerCardView
}

private data class SelectablePeer(var selected: Boolean, val peer: PeerItem)

data class PeerItem(val name: String, val host: String, val port: Int) {

    fun withPort(port: Int) = PeerItem(name, host, port)

    fun asPeerMessage() = ChainService.Peer.newBuilder().setHostname(host).setPort(port).build()
}