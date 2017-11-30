package nl.tudelft.cs4160.trustchain_android.chainExplorer

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.google.protobuf.ByteString

import java.util.HashMap

import nl.tudelft.cs4160.trustchain_android.R
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock
import nl.tudelft.cs4160.trustchain_android.message.MessageProto

import nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex
import kotlinx.android.synthetic.main.item_trustchainblock.view.*

class ChainExplorerAdapter(internal var context: Context, internal var blocksList: List<MessageProto.TrustChainBlock>, myPubKey: ByteArray) : BaseAdapter() {
    internal var peerList = HashMap<ByteString, String>()

    init {
        // put my public key in the peerList
        peerList.put(ByteString.copyFrom(myPubKey), "me")
        peerList.put(TrustChainBlock.EMPTY_PK, "unknown")
    }

    override fun getCount(): Int = blocksList.size

    override fun getItem(position: Int): MessageProto.TrustChainBlock = blocksList[position]


    override fun getItemId(position: Int): Long = position.toLong()


    /**
     * Puts the data from a TrustChainBlock object into the item textview.
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflatedView = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_trustchainblock,
                parent, false)

        val block = getItem(position)
        // Check if we already know the peer, otherwise add it to the peerList
        val pubKeyByteStr = block.publicKey
        val linkPubKeyByteStr = block.linkPublicKey

        val peerAlias = findInPeersOrAdd(pubKeyByteStr)
        val linkPeerAlias = findInPeersOrAdd(linkPubKeyByteStr)

        // Check if the sequence numbers are 0, which would mean that they are unknown
        val seqNumStr = displayStringForSequenceNumber(block.sequenceNumber)
        val linkSeqNumStr = displayStringForSequenceNumber(block.linkSequenceNumber)


        // For the collapsed view, set the public keys to the aliases we gave them.
        inflatedView.peer.text = peerAlias
        inflatedView.seqNum.text = seqNumStr
        inflatedView.linkPeer.text = linkPeerAlias
        inflatedView.linkSeqNum.text = linkSeqNumStr
        inflatedView.transaction.text = block.transaction.toStringUtf8()


        inflatedView.pubKey.text = bytesToHex(pubKeyByteStr.toByteArray())
        inflatedView.linkPubKey.text = bytesToHex(linkPubKeyByteStr.toByteArray())
        inflatedView.prevHash.text = bytesToHex(block.previousHash.toByteArray())
        inflatedView.signature.text = bytesToHex(block.signature.toByteArray())
        inflatedView.expandedTransaction.text = block.transaction.toStringUtf8()

        if (peerAlias == "me" || linkPeerAlias == "me") {
            inflatedView.findViewById<View>(R.id.own_chain_indicator).setBackgroundColor(Color.GREEN)
        } else {
            inflatedView.findViewById<View>(R.id.own_chain_indicator).setBackgroundColor(Color.TRANSPARENT)
        }
        return inflatedView
    }

    // Check if we already know the peer, otherwise add it to the peerList
    internal fun findInPeersOrAdd(keyByteString: ByteString): String = peerList.getOrPut(keyByteString, { "peer" + (peerList.size - 1) })


    companion object {

        // Check if the sequence numbers are 0, which would mean that they are unknown
        internal fun displayStringForSequenceNumber(sequenceNumber: Int): String {
            return if (sequenceNumber == 0) {
                "unknown"
            } else {
                sequenceNumber.toString()
            }

        }
    }

}
