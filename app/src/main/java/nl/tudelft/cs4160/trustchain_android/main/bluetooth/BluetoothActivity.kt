package nl.tudelft.cs4160.trustchain_android.main.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.AdapterView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_bluetooth.*
import nl.tudelft.cs4160.trustchain_android.Peer
import nl.tudelft.cs4160.trustchain_android.R
import nl.tudelft.cs4160.trustchain_android.Util.Key
import nl.tudelft.cs4160.trustchain_android.connection.Communication
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener
import nl.tudelft.cs4160.trustchain_android.connection.bluetooth.BluetoothCommunication
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper
import java.security.KeyPair

class BluetoothActivity : AppCompatActivity(), CommunicationListener {

    private var btAdapter: BluetoothAdapter? = null
    private var dbHelper: TrustChainDBHelper? = null
    private var communication: Communication? = null
    private var kp: KeyPair? = null


    private val itemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
        val device = adapterView.getItemAtPosition(i) as BluetoothDevice
        Log.e(TAG, "pressed " + device.name + "\nUUID: " + device.uuids[0].uuid)

        val peer = Peer(device)
        communication!!.connectToPeer(peer)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        init()
    }

    fun init() {
        bluetoothLog.movementMethod = ScrollingMovementMethod()

        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            showToast("Device does not support bluetooth")
            finish()
            return
        }
        Log.i(TAG, "working bluetooth")
        if (!btAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_BLUETOOTH)

            Log.e(TAG, "Bluetooth not enabled")
            return
        }
        Log.i(TAG, "Bluetooth enabled")

        pairedDevicesList.adapter = DeviceAdapter(applicationContext, btAdapter!!.bondedDevices)
        pairedDevicesList.onItemClickListener = itemClickListener

        dbHelper = TrustChainDBHelper(applicationContext)
        kp = Key.loadKeys(applicationContext)

        //start listening for messages via bluetooth
        communication = BluetoothCommunication(dbHelper, kp, this, btAdapter)
        communication!!.start()

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                init()
            } else {
                showToast("Please enable bluetooth")
                finish()
            }
        }
    }

    /**
     * Show a toast
     * @param msg The message.
     */
    private fun showToast(msg: String) {
        runOnUiThread {
            val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //close the communication
        communication?.stop()
    }

    override fun updateLog(msg: String) {
        runOnUiThread { bluetoothLog!!.append(msg + "\n") }
    }

    companion object {

        private val TAG = BluetoothActivity::class.java.name

        private val REQUEST_BLUETOOTH = 2
    }
}
