package nl.tudelft.cs4160.trustchain_android.chainExplorer

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.google.protobuf.ByteString
import nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex
import nl.tudelft.cs4160.trustchain_android.R
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock
import nl.tudelft.cs4160.trustchain_android.message.MessageProto
import java.util.*

class ChainExplorerAdapter(internal var context: Context, internal var blocksList: List<MessageProto.TrustChainBlock>, myPubKey: ByteArray) : BaseAdapter() {
    internal var peerList = HashMap<ByteString, String>()

    init {
        // put my public key in the peerList
        peerList.put(ByteString.copyFrom(myPubKey), "me")
        peerList.put(TrustChainBlock.EMPTY_PK, "unknown")
    }

    override fun getCount() = blocksList.size

    override fun getItem(position: Int) = blocksList[position]

    override fun getItemId(position: Int) = position.toLong()


    /**
     * Puts the data from a TrustChainBlock object into the item textview.
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val block = getItem(position)
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_trustchainblock,
                    parent, false)
        }

        // Check if we already know the peer, otherwise add it to the peerList
        val pubKeyByteStr = block.publicKey
        val linkPubKeyByteStr = block.linkPublicKey
        val peerAlias: String
        val linkPeerAlias: String

        if (peerList.containsKey(pubKeyByteStr)) {
            peerAlias = peerList[pubKeyByteStr]!!
        } else {
            peerAlias = "peer" + (peerList.size - 1)
            peerList.put(pubKeyByteStr, peerAlias)
        }

        if (peerList.containsKey(linkPubKeyByteStr)) {
            linkPeerAlias = peerList[linkPubKeyByteStr]!!
        } else {
            linkPeerAlias = "peer" + (peerList.size - 1)
            peerList.put(linkPubKeyByteStr, linkPeerAlias)
        }

        // Check if the sequence numbers are 0, which would mean that they are unknown
        val seqNumStr: String
        val linkSeqNumStr: String
        if (block.sequenceNumber == 0) {
            seqNumStr = "unknown"
        } else {
            seqNumStr = block.sequenceNumber.toString()
        }

        if (block.linkSequenceNumber == 0) {
            linkSeqNumStr = "unknown"
        } else {
            linkSeqNumStr = block.linkSequenceNumber.toString()
        }

        // collapsed view
        val peer = convertView!!.findViewById<TextView>(R.id.peer)
        val seqNum = convertView.findViewById<TextView>(R.id.sequence_number)
        val linkPeer = convertView.findViewById<TextView>(R.id.link_peer)
        val linkSeqNum = convertView.findViewById<TextView>(R.id.link_sequence_number)
        val transaction = convertView.findViewById<TextView>(R.id.transaction)

        // For the collapsed view, set the public keys to the aliases we gave them.
        peer.text = peerAlias
        seqNum.text = seqNumStr
        linkPeer.text = linkPeerAlias
        linkSeqNum.text = linkSeqNumStr
        transaction.text = block.transaction.toStringUtf8()

        // expanded view
        val pubKey = convertView.findViewById<TextView>(R.id.pub_key)
        val linkPubKey = convertView.findViewById<TextView>(R.id.link_pub_key)
        val prevHash = convertView.findViewById<TextView>(R.id.prev_hash)
        val signature = convertView.findViewById<TextView>(R.id.signature)
        val expTransaction: TextView = convertView.findViewById<TextView>(R.id.expanded_transaction)

        pubKey.text = bytesToHex(pubKeyByteStr.toByteArray())
        linkPubKey.text = bytesToHex(linkPubKeyByteStr.toByteArray())
        prevHash.text = bytesToHex(block.previousHash.toByteArray())
        signature.text = bytesToHex(block.signature.toByteArray())
        expTransaction.text = block.transaction.toStringUtf8()

        if (peerAlias == "me" || linkPeerAlias == "me") {
            convertView.findViewById<View>(R.id.own_chain_indicator).setBackgroundColor(Color.GREEN)
        } else {
            convertView.findViewById<View>(R.id.own_chain_indicator).setBackgroundColor(Color.TRANSPARENT)
        }
        return convertView
    }

    companion object {
        internal val TAG = "ChainExplorerAdapter"
    }
}

//fun TrustChainBlock.sequenceSring() =
