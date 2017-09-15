package nl.tudelft.cs4160.trustchain_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.security.Security;

public class MainActivity extends AppCompatActivity {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);}


    private Button buttonConnect;
    private EditText connectTo;
    private Button buttonKeyOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initVariables();
        init();
    }

    private void initVariables() {
        buttonConnect = (Button) findViewById(R.id.connect);
        buttonKeyOptions = (Button) findViewById(R.id.key_options);
        connectTo = (EditText) findViewById(R.id.connect_to);
    }

    private void init() {
        final MainActivity thisActivity = this;
        buttonKeyOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisActivity, KeyActivity.class);
                startActivity(intent);
            }
        });

    }


}
