package nl.tudelft.cs4160.trustchain_android.main.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.item_device.view.*
import nl.tudelft.cs4160.trustchain_android.R

class DeviceAdapter(private val context: Context, devices: Set<BluetoothDevice>) : BaseAdapter() {
    private val devices: MutableList<BluetoothDevice> = devices.toMutableList()

    override fun getCount(): Int = devices.size

    override fun getItem(i: Int): BluetoothDevice = devices[i]

    override fun getItemId(i: Int): Long = i.toLong()

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val inflatedView = view ?: LayoutInflater.from(context).inflate(R.layout.item_device, viewGroup, false)
        val device = getItem(i)

        inflatedView.deviceName.text = device.name
        inflatedView.deviceMac.text = device.address

        return inflatedView
    }
}
