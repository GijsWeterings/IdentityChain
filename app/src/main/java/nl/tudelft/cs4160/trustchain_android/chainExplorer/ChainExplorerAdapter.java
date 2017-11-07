package nl.tudelft.cs4160.trustchain_android.chainExplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock.pubKeyToString;

/**
 * Created by meijer on 7-11-17.
 */

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
        TextView peer = (TextView) convertView.findViewById(R.id.peer);
        TextView linkPeer = (TextView) convertView.findViewById(R.id.link_peer);

        peer.setText(pubKeyToString(block.getPublicKey().toByteArray()));
        linkPeer.setText(pubKeyToString(block.getLinkPublicKey().toByteArray()));

        return convertView;
    }
}
