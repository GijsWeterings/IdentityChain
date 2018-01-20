package nl.tudelft.cs4160.identitychain.main

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.attestation_creation.*
import kotlinx.android.synthetic.main.attestation_creation.view.*
import nl.tudelft.cs4160.identitychain.R
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.textView

class MainFragment : Fragment() {
    private lateinit var viewModel: MainViewModel


    internal var debugMenuListener: View.OnLongClickListener = View.OnLongClickListener {
        val intent = Intent(this.activity, DebugActivity::class.java)
        startActivity(intent)
        true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.attestation_creation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)
        imageView.setOnLongClickListener(debugMenuListener)

        attestationType.adapter = SpinnerAdapter()

        view.addClaimButton.setOnClickListener {
            val claimCreation = viewModel.createClaim()
            if (claimCreation == null) {
                Toast.makeText(activity, "Please select a peer to connect with", Toast.LENGTH_SHORT).show()
            } else {
                claimCreation.subscribe();
            }
        }
    }


    companion object {
        private val TAG = MainFragment::class.java.toString()
    }

    class SpinnerAdapter : BaseAdapter() {
        val entries = listOf("age")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return with(parent.context) {
                linearLayout {
                    textView(entries[position])
                }
            }
        }

        override fun getItem(position: Int): Any = entries[position]


        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = entries.size

    }

}