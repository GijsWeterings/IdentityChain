package nl.tudelft.cs4160.trustchain_android.chainExplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.Peer.bytesToHex;

public class ChainExplorerAdapter extends BaseAdapter{
    Context context;
    List<MessageProto.TrustChainBlock> blocksList;

    public ChainExplorerAdapter(Context context, List<MessageProto.TrustChainBlock> blocksList) {
        this.context = context;
        this.blocksList = blocksList;
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
        // collapsed view
        TextView peer = (TextView) convertView.findViewById(R.id.peer);
        TextView seqNum = (TextView) convertView.findViewById(R.id.sequence_number);
        TextView linkPeer = (TextView) convertView.findViewById(R.id.link_peer);
        TextView linkSeqNum = (TextView) convertView.findViewById(R.id.link_sequence_number);
        TextView transaction = (TextView) convertView.findViewById(R.id.transaction);

        peer.setText(bytesToHex(block.getPublicKey().toByteArray()));
        seqNum.setText(String.valueOf(block.getSequenceNumber()));
        linkPeer.setText(bytesToHex(block.getLinkPublicKey().toByteArray()));
        linkSeqNum.setText(String.valueOf(block.getLinkSequenceNumber()));
        transaction.setText(block.getTransaction().toStringUtf8());

        // expanded view
        TextView pubKey = (TextView) convertView.findViewById(R.id.pub_key);
        TextView linkPubKey = (TextView) convertView.findViewById(R.id.link_pub_key);
        TextView prevHash = (TextView) convertView.findViewById(R.id.prev_hash);
        TextView signature = (TextView) convertView.findViewById(R.id.signature);
        TextView expTransaction = (TextView) convertView.findViewById(R.id.expanded_transaction);

        pubKey.setText(bytesToHex(block.getPublicKey().toByteArray()));
        linkPubKey.setText(bytesToHex(block.getLinkPublicKey().toByteArray()));
        prevHash.setText(bytesToHex(block.getPreviousHash().toByteArray()));
        signature.setText(bytesToHex(block.getSignature().toByteArray()));
        expTransaction.setText(block.getTransaction().toStringUtf8());

        return convertView;
    }
}
