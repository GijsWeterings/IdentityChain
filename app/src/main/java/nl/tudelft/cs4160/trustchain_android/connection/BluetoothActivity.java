package nl.tudelft.cs4160.trustchain_android.connection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;

import static android.provider.Settings.NameValueTable.NAME;
import static nl.tudelft.cs4160.trustchain_android.message.MessageProto.Message.newBuilder;

public class BluetoothActivity extends AppCompatActivity {

    private final static String TAG = BluetoothActivity.class.getName();

    private BluetoothAdapter btAdapter;
    private UUID myUUID;

    private ListView listPairedDevices;

    private AcceptThread acceptThread;

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            BluetoothDevice device = (BluetoothDevice) adapterView.getItemAtPosition(i);
            Log.e(TAG, "pressed " + device.getName() + "\nUUID: " +device.getUuids()[0].getUuid());
            //start new tread
            new ConnectThread(device).start();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);


        listPairedDevices = (ListView) findViewById(R.id.bluetooth_list);
        init();
    }

    public void init() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return;
        }
        Log.i(TAG, "working bluetooth");
        if (!btAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth not enabled");
            return;
        }
        Log.i(TAG, "Bluetooth enabled");

        listPairedDevices.setAdapter(new DeviceAdapter(getApplicationContext(), btAdapter.getBondedDevices()));
        listPairedDevices.setOnItemClickListener(itemClickListener);

        setMyUUID();


        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private void setMyUUID() {
//        try {
//            Method getUUIDsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
//            ParcelUuid[] dUUIDs = (ParcelUuid[]) getUUIDsMethod.invoke(btAdapter, null);
//            myUUID =dUUIDs[dUUIDs.length-1].getUuid();
            myUUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
            Log.i(TAG, "Set UUID to: " + myUUID.toString());
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
    }


    protected void onDestroy() {
        super.onDestroy();

        //close the server
        acceptThread.cancel();
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, myUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
                Log.e(TAG, "almost");
                if (socket != null) {
                    Log.e(TAG, "start reading data");
                    // A connection was accepted. Perform work associated with
                    //TODO: read something
                    try {
                        Log.i(TAG, "created a server");
//                        while(true) {
                            /*try {
                                MessageProto.Message message= MessageProto.Message.parseFrom(socket.getInputStream());
                                if(message == null) {
                                    break;
                                }
                            } catch(InvalidProtocolBufferException e) {
                                Log.e(TAG, "invalid message received: " + e.getMessage());
                            }*/
                            byte[] buffer = new byte[1024];
                            socket.getInputStream().read(buffer);
                            if(buffer[0] == 1 && buffer[1] == 2 && buffer[2] == 3) {
                                Log.i(TAG, "Received message");
                            } else {
                                Log.e(TAG, "Received wrong message");
                            }
  //                      }


                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //TODO: send something
            Log.e(TAG, "Trying to send data...");
            try {

                OutputStream os = mmSocket.getOutputStream();
                MessageProto.TrustChainBlock.Builder builder = MessageProto.TrustChainBlock.newBuilder();
                builder.setLinkPublicKey(ByteString.copyFrom(new byte[] {0x01,0x02,0x03,0x04}))
                        .setPublicKey(ByteString.copyFrom(new byte[] {0x01,0x02,0x03,0x04}))
                        .setSequenceNumber(1).setLinkSequenceNumber(2);
                MessageProto.TrustChainBlock block = builder.build();
                MessageProto.Message msg = newBuilder().setHalfBlock(block).build();
                //MessageProto.TrustChainBlock msg = newBuilder().setHalfBlock(block).build();
                os.write(new byte[] { 0x01, 0x02, 0x03});
                //msg.writeTo(os);
                //os.write(msg.toByteArray());
                Log.e(TAG, "send message!");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

}
