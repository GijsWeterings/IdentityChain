package nl.tudelft.cs4160.identitychain.verification

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.protobuf.ByteString
import io.realm.Realm
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.database.TrustChainStorage
import nl.tudelft.cs4160.identitychain.main.MainViewModel
import nl.tudelft.cs4160.identitychain.message.MessageProto
import nl.tudelft.cs4160.identitychain.peers.KeyedPeer
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout

class VerificationFragment : Fragment() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var trustChainStorage: TrustChainStorage
    private lateinit var verificationBlockAdapter: VerificationBlockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProviders.of(this.activity)[MainViewModel::class.java]
        trustChainStorage = TrustChainDBHelper(this.activity)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainViewModel.peerSelection.observe(this, Observer<KeyedPeer> { keyPeer ->
            if (keyPeer != null) {
                val blocksForKey: List<MessageProto.TrustChainBlock> = blocksForKey(keyPeer)
                Log.i(TAG, "blocks for key size: ${blocksForKey.size}")
                verificationBlockAdapter.data = blocksForKey
                verificationBlockAdapter.notifyDataSetChanged()
            }
        })

        return with(this.context) {
            recyclerView {
                layoutManager = LinearLayoutManager(this@with)
                verificationBlockAdapter = VerificationBlockAdapter(emptyList(), Realm.getDefaultInstance())
                adapter = verificationBlockAdapter
            }
        }
    }

    private fun blocksForKey(keyPeer: KeyedPeer) = trustChainStorage.allBlocks
            .filter { it.publicKey == ByteString.copyFrom(keyPeer.publicKey) }

    companion object {
        val TAG = "verification fragment"
    }
}

class VerificationFragmentViewModel : ViewModel() {

}


class VerificationBlockAdapter(var data: List<MessageProto.TrustChainBlock>, val nameLookUp: Realm) : RecyclerView.Adapter<VerificationBlockViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerificationBlockViewHolder {
        return VerificationBlockViewHolder(with(parent) {
            cardView {
                verticalLayout {
                    textView("type age")
                    textView(" a - b")
                    textView("validity")
                }
            }
        })
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VerificationBlockViewHolder?, position: Int) {

    }
}

class VerificationBlockViewHolder(view: View) : RecyclerView.ViewHolder(view)