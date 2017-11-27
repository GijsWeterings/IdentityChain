package nl.tudelft.cs4160.trustchain_android.main.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyPair;

import nl.tudelft.cs4160.trustchain_android.Peer;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.Util.Key;
import nl.tudelft.cs4160.trustchain_android.connection.Communication;
import nl.tudelft.cs4160.trustchain_android.connection.CommunicationListener;
import nl.tudelft.cs4160.trustchain_android.connection.bluetooth.BluetoothCommunication;
import nl.tudelft.cs4160.trustchain_android.database.TrustChainDBHelper;

public class BluetoothActivity extends AppCompatActivity implements CommunicationListener {

    private final static String TAG = BluetoothActivity.class.getName();

    private BluetoothAdapter btAdapter;

    private final static int REQUEST_BLUETOOTH = 2;

    private ListView listPairedDevices;


    private TrustChainDBHelper dbHelper;
    private Communication communication;
    private KeyPair kp;

    private TextView textViewLog;


    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            BluetoothDevice device = (BluetoothDevice) adapterView.getItemAtPosition(i);
            Log.e(TAG, "pressed " + device.getName() + "\nUUID: " +device.getUuids()[0].getUuid());

            Peer peer = new Peer(device);
            communication.connectToPeer(peer);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        listPairedDevices = findViewById(R.id.bluetooth_list);
        init();
    }

    public void init() {
        textViewLog = findViewById(R.id.bluetooth_log);
        textViewLog.setMovementMethod(new ScrollingMovementMethod());

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            showToast("Device does not support bluetooth");
            finish();
            return;
        }
        Log.i(TAG, "working bluetooth");
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_BLUETOOTH);

            Log.e(TAG, "Bluetooth not enabled");
            return;
        }
        Log.i(TAG, "Bluetooth enabled");

        listPairedDevices.setAdapter(new DeviceAdapter(getApplicationContext(), btAdapter.getBondedDevices()));
        listPairedDevices.setOnItemClickListener(itemClickListener);

        dbHelper = new TrustChainDBHelper(getApplicationContext());
        kp = Key.loadKeys(getApplicationContext());

        //start listening for messages via bluetooth
        communication = new BluetoothCommunication(dbHelper, kp, this, btAdapter);
        communication.start();

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_BLUETOOTH) {
            if(resultCode == Activity.RESULT_OK) {
                init();
            } else {
                showToast("Please enable bluetooth");
                finish();
            }
        }
    }

    /**
     * Show a toast
     * @param msg The message.
     */
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
                toast.show();

            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();

        //close the communication
        if (communication != null) {
            communication.stop();
        }
    }

    private void addToLog(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewLog.append(msg + "\n");
            }
        });
    }


    @Override
    public void updateLog(String msg) {
        addToLog(msg);
    }
}
