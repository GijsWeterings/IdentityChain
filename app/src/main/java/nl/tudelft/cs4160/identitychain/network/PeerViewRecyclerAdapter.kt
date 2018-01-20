package nl.tudelft.cs4160.identitychain.network

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.peer_view.view.*
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.main.PeerConnectViewModel
import nl.tudelft.cs4160.identitychain.peers.KeyedPeer

class PeerViewRecyclerAdapter(val nameDialog: Single<String>, val viewModel: PeerConnectViewModel) : RecyclerView.Adapter<RecyclerViewHolder>() {
    private val peers: MutableList<SelectablePeer> = ArrayList()
    private var previouslySelected: RecyclerViewHolder? = null
    private val clickedItems: PublishSubject<KeyedPeer> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.peer_view, parent, false)
        return RecyclerViewHolder(view)
    }

    override fun getItemCount(): Int = peers.size

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val selectedPeer = peers[position]
        val publicKey = selectedPeer.peer.publicKey
        val contactName = viewModel.nameForPublicKey(publicKey)

        holder.name.text = contactName ?: selectedPeer.peer.name
        holder.publicKey.text = Peer.bytesToHex(publicKey).take(20)
        val view = holder.cardView
        view.setCardBackgroundColor(colorForSelection(selectedPeer, view))

        holder.itemView.setOnClickListener {
            previouslySelected?.toggleColorForSelection(peers)
            //if we click the same item twice it should stay disabled
            if (previouslySelected != holder) {
                holder.toggleColorForSelection(peers)
            }
            previouslySelected = holder
            clickedItems.onNext(selectedPeer.peer)
        }

        //contact hasn't been named yet
        if (contactName == null) {
            holder.itemView.setOnLongClickListener {
                nameDialog.subscribe({
                    viewModel.saveName(it, publicKey)
                    holder.name.text = it
                }, {
                    //on cancel do nothing
                })
                true
            }
        }
    }


    private fun colorForSelection(selectedPeer: SelectablePeer, view: CardView): Int {
        return if (selectedPeer.selected) {
            ContextCompat.getColor(view.context, R.color.colorPrimary)
        } else {
            Color.TRANSPARENT
        }
    }

    fun addItem(item: KeyedPeer) {
        peers.add(SelectablePeer(false, item))
        val size = peers.size

        this.notifyItemInserted(size - 1)
    }

    fun selection() = clickedItems.toFlowable(BackpressureStrategy.BUFFER)
}

class RecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val name = view.peerName
    val publicKey = view.publicKey
    val cardView = view.peerCardView

    internal fun toggleColorForSelection(peers: List<SelectablePeer>) {
        val selectedPeer = peers[this.adapterPosition]
        selectedPeer.selected = !selectedPeer.selected

        return cardView.setCardBackgroundColor(colorForPeer(selectedPeer))
    }

    private fun colorForPeer(selectedPeer: SelectablePeer): Int {
        if (selectedPeer.selected) {
            return ContextCompat.getColor(cardView.context, R.color.colorPrimary)
        } else {
            return Color.TRANSPARENT
        }
    }
}

internal data class SelectablePeer(var selected: Boolean, val peer: KeyedPeer)

