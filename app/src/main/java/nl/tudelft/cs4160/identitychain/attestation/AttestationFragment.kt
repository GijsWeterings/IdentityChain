package nl.tudelft.cs4160.identitychain.attestation

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.database.AttestationRequest
import org.jetbrains.anko.AnkoComponent
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import kotlin.properties.Delegates


class AttestationFragment : Fragment() {
    val realm = Realm.getDefaultInstance()


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val attestationAdapter = AttestationAdapter(realm.where(AttestationRequest::class.java).findAll(), true)
        return AttestationUI(attestationAdapter).createView(AnkoContext.Companion.create(context, this))
    }
}

class AttestationUI(
        val attestationAdapter: RecyclerView.Adapter<*>
) : AnkoComponent<AttestationFragment> {

    override fun createView(ui: AnkoContext<AttestationFragment>): View {
        return with(ui) {
            verticalLayout {
                recyclerView {
                    this.adapter = attestationAdapter
                }
            }
        }
    }
}

class AttestationAdapter(data: OrderedRealmCollection<AttestationRequest>, update: Boolean)
    : RealmRecyclerViewAdapter<AttestationRequest, AttestationViewHolder>(data, update) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttestationViewHolder {
        var publicKeyTextView: TextView by Delegates.notNull()
        val view = with(parent.context) {
            cardView {
                textView {
                    text = "Attestation"
                }

                textView {
                    text = "what up glibb gloobs"
                }

                publicKeyTextView = textView()
            }
        }

        return AttestationViewHolder(view, publicKeyTextView)
    }

    override fun onBindViewHolder(holder: AttestationViewHolder, position: Int) {
        val item = getItem(position)
        holder.publicKey.text = Peer.bytesToHex(item?.publicKey)
    }

}

class AttestationViewHolder(view: View, val publicKey: TextView) : RecyclerView.ViewHolder(view) {}

