package nl.tudelft.cs4160.identitychain.verification

import android.arch.lifecycle.*
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import nl.tudelft.cs4160.identitychain.database.TrustChainDBHelper
import nl.tudelft.cs4160.identitychain.database.TrustChainStorage
import nl.tudelft.cs4160.identitychain.main.MainViewModel
import nl.tudelft.cs4160.identitychain.message.ChainService
import nl.tudelft.cs4160.identitychain.message.MessageProto
import nl.tudelft.cs4160.identitychain.peers.KeyedPeer
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.recyclerview.v7.recyclerView
import kotlin.properties.Delegates

class VerificationFragment : DialogFragment() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var trustChainStorage: TrustChainStorage
    private lateinit var verificationBlockAdapter: VerificationBlockAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = ViewModelProviders.of(this.activity)
        mainViewModel = provider[MainViewModel::class.java]
        trustChainStorage = TrustChainDBHelper(this.activity)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainViewModel.peerSelection.observe(this, Observer<KeyedPeer> { keyPeer ->
            if (keyPeer != null) {
                val blocksForKey: List<MessageProto.TrustChainBlock> = blocksForKey(keyPeer)
                Log.i(TAG, "blocks for key size: ${blocksForKey.size}")
                verificationBlockAdapter.data = blocksForKey.mapNotNull(::tryParsePublicResult)
                verificationBlockAdapter.notifyDataSetChanged()
            }
        })

        return with(this.context) {
            frameLayout {
                lparams(matchParent, matchParent)
                backgroundColor = Color.WHITE
                textView("hello")
                recyclerView {
                    layoutManager = LinearLayoutManager(this@with)
                    verificationBlockAdapter = VerificationBlockAdapter(emptyList(), mainViewModel, {this@VerificationFragment.dismiss()})
                    adapter = verificationBlockAdapter
                }
            }
        }
    }

    private fun blocksForKey(keyPeer: KeyedPeer): List<MessageProto.TrustChainBlock> {
        val allBlocks = trustChainStorage.allBlocks

        val block = allBlocks[3]
        val sameKeys = block.linkPublicKey == block.publicKey

        val goodBlocks = allBlocks.filter { it.publicKey == ByteString.copyFrom(keyPeer.publicKey) }
        return goodBlocks
    }

        companion object {
        val TAG = "verification fragment"
    }

}

fun tryParsePublicResult(block: MessageProto.TrustChainBlock): Pair<Int,ChainService.PublicSetupResult>? {
    if (block.transaction.isEmpty) {
        return null
    }
    try {
        return block.sequenceNumber.to(ChainService.PublicSetupResult.parseFrom(block.transaction))
    } catch (otherData: InvalidProtocolBufferException) {
        return null
    }
}


class VerificationBlockAdapter(var data: List<Pair<Int,ChainService.PublicSetupResult>>, val verificationViewModel: MainViewModel,
                               val dismiss: () -> Unit)
    : RecyclerView.Adapter<VerificationBlockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerificationBlockViewHolder {
        var range: TextView by Delegates.notNull()
        var verify: Button by Delegates.notNull()
        val view = with(parent) {
            cardView {
                verticalLayout {
                    textView("type age")
                    range = textView()
                    textView("validity")
                    verify = button("verify!")
                }
            }
        }
        return VerificationBlockViewHolder(view, range, verify)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VerificationBlockViewHolder, position: Int) {
        val (seq, publicSetupResult) = data[position]
        holder.range.text = "${publicSetupResult.a} - ${publicSetupResult.b}"
        holder.verify.setOnClickListener {
            dismiss()
            verificationViewModel.proofSelected(data[holder.adapterPosition])
        }
    }

}

class VerificationBlockViewHolder(view: View, val range: TextView, val verify: Button) : RecyclerView.ViewHolder(view)