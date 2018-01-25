package nl.tudelft.cs4160.identitychain.attestation

import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import nl.tudelft.cs4160.identitychain.Peer
import nl.tudelft.cs4160.identitychain.R
import nl.tudelft.cs4160.identitychain.database.AttestationRequest
import nl.tudelft.cs4160.identitychain.main.MainViewModel
import nl.tudelft.cs4160.identitychain.peers.nameForContact
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.recyclerview.v7.recyclerView
import kotlin.properties.Delegates


class AttestationFragment : Fragment() {
    val realm = Realm.getDefaultInstance()
    lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val attestationAdapter = AttestationAdapter(realm.where(AttestationRequest::class.java).findAll(), true, realm, viewModel)
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

class AttestationAdapter(data: OrderedRealmCollection<AttestationRequest>, update: Boolean, val realm: Realm, val viewModel: MainViewModel)
    : RealmRecyclerViewAdapter<AttestationRequest, AttestationViewHolder>(data, update) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttestationViewHolder {
        var publicKeyTextView: TextView by Delegates.notNull()
        var rejectButton: ImageButton by Delegates.notNull()
        var verifyAttestationButton: ImageButton by Delegates.notNull()
        var nameTextView: TextView by Delegates.notNull()
        val view = with(parent.context) {
            cardView {
                lparams(width = matchParent) {
                    bottomMargin = dip(4)
                }

                verticalLayout {
                    textView("Attestation") {
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    linearLayout {
                        textView("From:").lparams {
                            rightMargin = dip(2)
                        }
                        nameTextView = textView()
                    }

                    textView("Age greater than 18")
                    publicKeyTextView = textView()
                }

                linearLayout {
                    gravity = Gravity.END
                    val imageSize = 60
                    verifyAttestationButton = confirmationButton(imageSize, R.drawable.ic_check_circle_24dp)
                    rejectButton = confirmationButton(imageSize, R.drawable.ic_cancel_24dp)
                }
            }
        }

        return AttestationViewHolder(view, publicKeyTextView, rejectButton, verifyAttestationButton, nameTextView)
    }

    private fun @AnkoViewDslMarker _LinearLayout.confirmationButton(imageSize: Int, resource: Int): ImageButton {
        return imageButton(resource) {
            backgroundColor = Color.TRANSPARENT
            scaleType = ImageView.ScaleType.FIT_CENTER
        }.lparams(dip(imageSize), dip(imageSize))
    }

    private val TAG: String = "Attestation Recycler"

    override fun onBindViewHolder(holder: AttestationViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            val keyAsText = Peer.bytesToHex(item.publicKey())
            holder.name.text = item.publicKey()?.let { nameForContact(realm, it) } ?: "Unknown peer"

            holder.publicKey.text = keyAsText.takeLast(20)

            holder.rejectButton.setOnClickListener {
                realm.executeTransaction {
                    //the position in the argument can become out to date if items in the middle of the list are removed
                    //then this view holder doesn't rebind and the position become bullshit
                    val upToDatePosition = holder.adapterPosition
                    Log.i(TAG, "deleteting at position $upToDatePosition")
                    Log.i(TAG, "data has length: ${data?.size}")

                    if (upToDatePosition != -1) {
                        data?.deleteFromRealm(upToDatePosition)
                    }
                }
            }

            holder.verifyAttestationButton.setOnClickListener {
                val attestationRequest = getItem(holder.adapterPosition)
                attestationRequest?.let(viewModel::startVerificationAndSigning)
            }
        }
    }

}

class AttestationViewHolder(view: View, val publicKey: TextView, val rejectButton: ImageButton,
                            val verifyAttestationButton: ImageButton, val name: TextView) : RecyclerView.ViewHolder(view)



