package nl.tudelft.cs4160.identitychain.attestation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.database.AttestationRequest
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.recyclerview.v7.recyclerView
import kotlin.properties.Delegates


class AttestationFragment : Fragment() {
    val realm = Realm.getDefaultInstance()
    
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val attestationAdapter = AttestationAdapter(realm.where(AttestationRequest::class.java).findAll(), true, realm)
        return AttestationUI(attestationAdapter).createView(AnkoContext.Companion.create(context, this))
    }
}

class AttestationUI(
        val attestationAdapter: RecyclerView.Adapter<*>
) : AnkoComponent<AttestationFragment> {

    override fun createView(ui: AnkoContext<AttestationFragment>): View {
        return with(ui) {
            verticalLayout {
                gravity = Gravity.CENTER_HORIZONTAL
                recyclerView {
                    layoutManager = LinearLayoutManager(ui.ctx)
                    this.adapter = attestationAdapter
                }.lparams(width = matchParent, height = matchParent) {
                    gravity = Gravity.CENTER_HORIZONTAL
                }

            }
        }
    }
}

class AttestationAdapter(data: OrderedRealmCollection<AttestationRequest>, update: Boolean, val realm: Realm)
    : RealmRecyclerViewAdapter<AttestationRequest, AttestationViewHolder>(data, update) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttestationViewHolder {
        var publicKeyTextView: TextView by Delegates.notNull()
        var rejectButton: Button by Delegates.notNull()
        val view = with(parent.context) {
            cardView {
                lparams(width = matchParent) {
                    bottomMargin = dip(4)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                verticalLayout {
                    textView {
                        text = "Attestation"
                    }

                    textView {
                        text = "what up glibb globs"
                    }

                    publicKeyTextView = textView()
                    linearLayout {
                        gravity = Gravity.CENTER_HORIZONTAL
                        button("ok").lparams {
                            horizontalMargin = dip(24)
                        }
                        rejectButton = button("not ok")
                    }
                }
            }
        }

        return AttestationViewHolder(view, publicKeyTextView, rejectButton)
    }

    private val TAG: String = "Attestation Recycler"

    override fun onBindViewHolder(holder: AttestationViewHolder, position: Int) {
        val item = getItem(position)
        holder.publicKey.text = Peer.bytesToHex(item?.publicKey)
        holder.rejectButton.setOnClickListener {
            realm.executeTransaction {
                //the position in the argument can become out to date if items in the middle of the list are removed
                //then this view holder doesn't rebind and the position become bullshit
                val upToDatePosition = holder.adapterPosition
                Log.i(TAG, "deleteting at position $upToDatePosition")
                Log.i(TAG, "data has length: ${data?.size}")

                data?.deleteFromRealm(upToDatePosition)
            }
        }
    }

}

class AttestationViewHolder(view: View, val publicKey: TextView, val rejectButton: Button) : RecyclerView.ViewHolder(view)

