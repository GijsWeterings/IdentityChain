package nl.tudelft.cs4160.trustchain_android.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.block.BlockProto;
import nl.tudelft.cs4160.trustchain_android.block.TrustChainBlock;

public class MainActivity extends AppCompatActivity {
    BlockProto.TrustChainBlock message;

    TextView externalIPText;
    TextView localIPText;
    TextView statusText;
    Button connectionButton;
    EditText editTextDestinationIP;
    EditText editTextDestinationPort;

    MainActivity thisActivity;

    /**
     * Listener for the connection button.
     * On click a message is sent to the connected device.
     */
    View.OnClickListener connectionButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            ClientTask task = new ClientTask(
                    editTextDestinationIP.getText().toString(),
                    Integer.parseInt(editTextDestinationPort.getText().toString()),
                    message,
                    thisActivity);
            task.execute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;
        localIPText = (TextView) findViewById(R.id.my_local_ip);
        externalIPText = (TextView) findViewById(R.id.my_external_ip);
        statusText = (TextView) findViewById(R.id.status);
        statusText.setMovementMethod(new ScrollingMovementMethod());
        editTextDestinationIP = (EditText) findViewById(R.id.destination_IP);
        editTextDestinationPort = (EditText) findViewById(R.id.destination_port);
        connectionButton = (Button) findViewById(R.id.connection_button);

        updateIP();
        updateLocalIPField(getLocalIPAddress());
        constructTempBlock();

        connectionButton.setOnClickListener(connectionButtonListener);

        Server socketServer = new Server(thisActivity);
        socketServer.start();
    }

    /**
     * Construct a default block for testing purposes.
     */
    public void constructTempBlock() {
        message = TrustChainBlock.createGenesisBlock();
    }

    /**
     * Updates the external IP address textfield to the given IP address.
     */
    public void updateExternalIPField(String ipAddress) {
        externalIPText.setText(ipAddress);
        System.out.println("IP ADDRESS: " + ipAddress);
    }

    /**
     * Updates the internal IP address textfield to the given IP address.
     */
    public void updateLocalIPField(String ipAddress) {
        localIPText.setText(ipAddress);
        System.out.println("IP ADDRESS: " + ipAddress);
    }

    /**
     * Finds the external IP address of this device by making an API call to https://www.ipify.org/.
     * The networking runs on a separate thread.
     * @return a string representation of the device's external IP address
     */
    public void updateIP() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (java.util.Scanner s = new java.util.Scanner(new java.net.URL("https://api.ipify.org").openStream(), "UTF-8").useDelimiter("\\A")) {
                    final String ip = s.next();
                    // new thread to handle UI updates
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateExternalIPField(ip);
                        }
                    });
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Finds the local IP address of this device, loops trough network interfaces in order to find it.
     * The address that is not a loopback address is the IP of the device.
     * @return a string representation of the device's IP address
     */
    public String getLocalIPAddress() {
        try {
            List<NetworkInterface> netInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface netInt : netInterfaces) {
                List<InetAddress> addresses = Collections.list(netInt.getInetAddresses());
                for (InetAddress addr : addresses) {
                    if(addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
