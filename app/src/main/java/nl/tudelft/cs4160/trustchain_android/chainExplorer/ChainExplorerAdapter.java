package nl.tudelft.cs4160.trustchain_android.chainExplorer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.protobuf.ByteString;

import java.util.HashMap;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;
import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.pubKeyToString;

public class ChainExplorerAdapter extends BaseAdapter{
    static final String TAG = "ChainExplorerAdapter";

    Context context;
    List<MessageProto.TrustChainBlock> blocksList;
    HashMap<ByteString,String> peerList = new HashMap<>();

    public ChainExplorerAdapter(Context context, List<MessageProto.TrustChainBlock> blocksList, byte[] myPubKey) {
        this.context = context;
        this.blocksList = blocksList;
        // put my public key in the peerList
        peerList.put(ByteString.copyFrom(myPubKey),"me");
        peerList.put(TrustChainBlock.EMPTY_PK,"unknown");
    }

    @Override
    public int getCount() {
        return blocksList.size();
    }

    @Override
    public Object getItem(int position) {
        return blocksList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Puts the data from a TrustChainBlock object into the item textview.
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MessageProto.TrustChainBlock block = (MessageProto.TrustChainBlock) getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_trustchainblock,
                    parent, false);
        }

        // Check if we already know the peer, otherwise add it to the peerList
        ByteString pubKeyByteStr = block.getPublicKey();
        ByteString linkPubKeyByteStr = block.getLinkPublicKey();
        String peerAlias;
        String linkPeerAlias;

        if(peerList.containsKey(pubKeyByteStr)) {
            peerAlias = peerList.get(pubKeyByteStr);
        } else {
            peerAlias = "peer" + (peerList.size()-1);
            peerList.put(pubKeyByteStr,peerAlias);
        }

        if(peerList.containsKey(linkPubKeyByteStr)) {
            linkPeerAlias = peerList.get(linkPubKeyByteStr);
        } else {
            linkPeerAlias = "peer" + (peerList.size()-1);
            peerList.put(linkPubKeyByteStr,linkPeerAlias);
        }

        // Check if the sequence numbers are 0, which would mean that they are unknown
        String seqNumStr;
        String linkSeqNumStr;
        if(block.getSequenceNumber() == 0) {
            seqNumStr = "unknown";
        } else {
            seqNumStr = String.valueOf(block.getSequenceNumber());
        }

        if(block.getLinkSequenceNumber() == 0) {
            linkSeqNumStr = "unknown";
        } else {
            linkSeqNumStr = String.valueOf(block.getLinkSequenceNumber());
        }

        // collapsed view
        TextView peer = (TextView) convertView.findViewById(R.id.peer);
        TextView seqNum = (TextView) convertView.findViewById(R.id.sequence_number);
        TextView linkPeer = (TextView) convertView.findViewById(R.id.link_peer);
        TextView linkSeqNum = (TextView) convertView.findViewById(R.id.link_sequence_number);
        TextView transaction = (TextView) convertView.findViewById(R.id.transaction);

        // For the collapsed view, set the public keys to the aliases we gave them.
        peer.setText(peerAlias);
        seqNum.setText(seqNumStr);
        linkPeer.setText(linkPeerAlias);
        linkSeqNum.setText(linkSeqNumStr);
        transaction.setText(block.getTransaction().toStringUtf8());

        // expanded view
        TextView pubKey = (TextView) convertView.findViewById(R.id.pub_key);
        TextView linkPubKey = (TextView) convertView.findViewById(R.id.link_pub_key);
        TextView prevHash = (TextView) convertView.findViewById(R.id.prev_hash);
        TextView signature = (TextView) convertView.findViewById(R.id.signature);
        TextView expTransaction = (TextView) convertView.findViewById(R.id.expanded_transaction);

        pubKey.setText(bytesToHex(pubKeyByteStr.toByteArray()));
        linkPubKey.setText(bytesToHex(linkPubKeyByteStr.toByteArray()));
        prevHash.setText(bytesToHex(block.getPreviousHash().toByteArray()));
        signature.setText(bytesToHex(block.getSignature().toByteArray()));
        expTransaction.setText(block.getTransaction().toStringUtf8());

        if(peerAlias.equals("me") || linkPeerAlias.equals("me")) {
            convertView.findViewById(R.id.own_chain_indicator).setBackgroundColor(Color.GREEN);
        } else {
            convertView.findViewById(R.id.own_chain_indicator).setBackgroundColor(Color.TRANSPARENT);
        }
        return convertView;
    }
}
