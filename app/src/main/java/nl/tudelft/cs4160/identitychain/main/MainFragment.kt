package nl.tudelft.cs4160.identitychain.main

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


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

    fun fetchDisplayName() {
        requestPermissions(arrayOf(Manifest.permission.WRITE_CONTACTS), 2)

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
            }
            requestPermissions(arrayOf(Manifest.permission.WRITE_CONTACTS), 2)
//            ActivityCompat.requestPermissions(activity,
//                    arrayOf(Manifest.permission.WRITE_CONTACTS), 2)

        }
        else {
            val c = context.contentResolver.query(ContactsContract.Profile.CONTENT_URI, null, null, null, null)
            c.moveToFirst()
            name.text = c.getString(c.getColumnIndex("display_name"))
            imageView.setImageURI(Uri.parse(c.getString(c.getColumnIndex("photo_uri"))))
            c.close()
        }
        if(name.text.equals("")) {
            name.text = "John Doe"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchDisplayName()

        viewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)
        imageView.setOnLongClickListener(debugMenuListener)

        attestationType.adapter = SpinnerAdapter()

        view.addClaimButton.setOnClickListener {
            val parseNumbers = parseNumbers()
            if (parseNumbers != null) {
                val (a, b, m) = parseNumbers
                val claimCreation = viewModel.createClaim(a, b, m)
                if (claimCreation == null) {
                    Toast.makeText(activity, "Please select a peer to connect with", Toast.LENGTH_SHORT).show()
                } else {
                    claimCreation.subscribe();
                }
            } else {
                Toast.makeText(activity, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun parseNumbers(): Triple<Int, Int, Int>? {
        val a = lowerBound.text.toString().toIntOrNull()
        val b = upperBound.text.toString().toIntOrNull()
        val m = value.text.toString().toIntOrNull()
        //kotlin needs a validation applicative
        return if (a != null && b != null && m != null) {
            Triple(a, b, m)
        } else {
            null
        }
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