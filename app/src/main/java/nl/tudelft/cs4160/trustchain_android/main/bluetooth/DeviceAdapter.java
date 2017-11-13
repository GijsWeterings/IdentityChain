package nl.tudelft.cs4160.trustchain_android.main.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nl.tudelft.cs4160.trustchain_android.R;

public class DeviceAdapter extends BaseAdapter {
    private List<BluetoothDevice> devices;
    private Context context;

    public DeviceAdapter(Context context, Set<BluetoothDevice> devices) {
        this.context = context;
        Iterator<BluetoothDevice> it = devices.iterator();
        this.devices = new ArrayList<>();
        while(it.hasNext()) {
            this.devices.add(it.next());
        }
    }
    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public BluetoothDevice getItem(int i) {
        return devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        BluetoothDevice device = getItem(i);
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_device, viewGroup, false);
        }

        TextView deviceName = (TextView)view.findViewById(R.id.device_name);
        TextView deviceMAC = (TextView)view.findViewById(R.id.device_mac);

        deviceName.setText(device.getName());
        deviceMAC.setText(device.getAddress());

        return view;
    }
}
