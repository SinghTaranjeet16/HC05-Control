package arduino.hc05;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class HomeScreen extends ActionBarActivity {
    private static final String TAG = "HC-05:";

    Button btnScan;
    ImageButton btnCN;
    ToggleButton btnOnOff1, btnOnOff2;
    TextView item1, item2;
    LinearLayout sl;


    private static final int REQUEST_ENABLE_BT = 1;
    private static final int GET_MAC_ADDRESS = 2;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //UUID for SPP service

    public static final String PREFS_NAME = "UserPreference";
    private static String RECENT_CONN_MAC = "";
    private static String Element1 = "";
    private static String Element2 = "";
    private static String address = "";  //MAC address of bluetooth module to connect

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final MediaPlayer mMediaPlayer = MediaPlayer.create(HomeScreen.this, R.raw.btnclick);

        SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        RECENT_CONN_MAC = pref.getString("RecentMAC", ""); //2nd param is DEFAULT value in case no preference is found
        Element1 = pref.getString("eName1", "Element1");
        Element2 = pref.getString("eName2", "Element2");

        Log.d(TAG, "In onCreate()");
        setContentView(R.layout.activity_home_screen);

        sl = (LinearLayout) findViewById(R.id.scan_led);
        btnScan = (Button) findViewById(R.id.btnscan);
        btnCN = (ImageButton) findViewById(R.id.Connection);
        btnOnOff1 = (ToggleButton) findViewById(R.id.OnOff1);
        btnOnOff2 = (ToggleButton) findViewById(R.id.OnOff2);
        item1 = (TextView) findViewById(R.id.item1);
        item2 = (TextView) findViewById(R.id.item2);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        item1.setText(Element1);
        item2.setText(Element2);

        btnOnOff1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMediaPlayer.start();
                if (btSocket != null) {
                    if (isChecked) {
                        Toast.makeText(getApplicationContext(), "Switch-1 ON", Toast.LENGTH_SHORT).show();
                        sendData("1");

                    } else {
                        Toast.makeText(getApplicationContext(), "Switch-1 OFF", Toast.LENGTH_SHORT).show();
                        sendData("0");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No connection detected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOnOff2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mMediaPlayer.start();
                if (btSocket != null) {

                    if (isChecked) {
                        Toast.makeText(getApplicationContext(), "Switch-2 ON", Toast.LENGTH_SHORT).show();
                        sendData("8");

                    } else {
                        Toast.makeText(getApplicationContext(), "Switch-2 OFF", Toast.LENGTH_SHORT).show();
                        sendData("9");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No connection detected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnScan.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent scanMode = new Intent(HomeScreen.this, Scanning.class);
                startActivityForResult(scanMode, GET_MAC_ADDRESS);
                Toast msg = Toast.makeText(getBaseContext(), "Scanning Intent Initialized", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

//        Setting up a click event for the element name tag that will allow users to edit the appliances name
        item1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomeScreen.this);
                alertDialog.setTitle("Name Change");
                alertDialog.setMessage("Enter new name");

                final EditText input = new EditText(HomeScreen.this);
                alertDialog.setView(input);
                alertDialog.setIcon(R.drawable.about);

                alertDialog.setPositiveButton("YES",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                item1.setText(input.getText().toString());
                            }
                        });

                alertDialog.setNegativeButton("NO",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.create();
                alertDialog.show();
            }
        });

//        Setting up a click event for the element name tag that will allow users to edit the appliances name
        item2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomeScreen.this);
                alertDialog.setTitle("Name Change");
                alertDialog.setMessage("Enter new name");

                final EditText input = new EditText(HomeScreen.this);
                alertDialog.setView(input);
                alertDialog.setIcon(R.drawable.about);

                alertDialog.setPositiveButton("YES",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                item2.setText(input.getText().toString());
                            }
                        });

                alertDialog.setNegativeButton("NO",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.create();
                alertDialog.show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        btnCN.setImageResource(R.drawable.grey);

        if (!address.isEmpty()) {
            Log.d(TAG, "...In onResume - Attempting client connect...");
            btnCN.setImageResource(R.drawable.yellow);

            // Set up a pointer to the remote node using it's address.
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // Two things are needed to make a connection:
            //   A MAC address, which we got above.
            //   A Service ID or UUID.  In this case we are using the UUID for SPP.
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            // Discovery is resource intensive.  Cancel scanning before sending data.
            btAdapter.cancelDiscovery();

            // Establish the connection.  This will block until it connects.
            Log.d(TAG, "...Connecting to Remote...");
            try {
                btSocket.connect();
                Log.d(TAG, "...Connection established and data link opened...");
                btnCN.setImageResource(R.drawable.green);
            } catch (IOException e) {
                errorExit("Fatal Error", "In onResume() and unable to create socket" + e.getMessage() + ".");
                btnCN.setImageResource(R.drawable.yellow);
                try {
                    Log.d(TAG, "Trying Fallback method");
                    btSocket = (BluetoothSocket) address.getClass()
                            .getMethod("createRfcommSocket", new Class[]{int.class}).invoke(address, 1);
                    btSocket.connect();
                    btnCN.setImageResource(R.drawable.green);
                } catch (Exception e2) {
                    errorExit("Fatal Error", "In onResume(). Fallback method failed..." + e2.getMessage() + ".");
                    btnCN.setImageResource(R.drawable.yellow);
                    try {
                        btSocket.close();
                        btnCN.setImageResource(R.drawable.red);
                    } catch (IOException e3) {
                        btnCN.setImageResource(R.drawable.yellow);
                        Log.d(TAG, "In OnResume(). Unable to close socket" + e3.getMessage());
                    }
                }
            }

            // Create a data stream so we can talk to server.
            Log.d(TAG, "...Creating Socket...");

            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!address.isEmpty()) {
            Log.d(TAG, "...In onPause()...");

            if (outStream != null && btSocket.isConnected()) {
                try {
                    outStream.flush();
                } catch (IOException e) {
                    errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
                }


                try {
                    btSocket.close();
                } catch (IOException e2) {
                    errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
                }
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED) {
                    Toast msg = Toast.makeText(getBaseContext(),
                            "Enable error: Bluetooth should be enabled to use this application"
                            , Toast.LENGTH_SHORT);
                    msg.show();
                } else if (resultCode == RESULT_OK) {
                }
                break;

            case GET_MAC_ADDRESS:
                if (data != null) {
                    address = data.getStringExtra("btMAC");
                }
                break;

            default:
                break;

        }
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Sending data: " + message + "...");

        try {
            outStream.write(msgBuffer);
            outStream.flush();
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address ";
            msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }

    private void errorExit(String title, String message) {
        Toast msg = Toast.makeText(getBaseContext(),
                title + " - " + message, Toast.LENGTH_LONG);
        msg.show();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_on) {
            Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(this, "Enable bluetooth to use the application", Toast.LENGTH_SHORT).show();
            if (btAdapter == null) {
                errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
            }
        } else if (id == R.id.action_off) {
            btAdapter.cancelDiscovery();
            btAdapter.disable();
            Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
        } else if (id == R.id.action_recentlyUsed) {
            Toast.makeText(this, "RECENT_CONN_MAC=" + RECENT_CONN_MAC, Toast.LENGTH_SHORT).show();
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "Saving address = RECENT_CONN_MAC");
                address = RECENT_CONN_MAC;

                Log.d(TAG, "restarting activity");
                finish();
                startActivity(getIntent());
            } else {
                Toast.makeText(this, "Enable Bluetooth to use this option", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.action_exit) {
            if (btAdapter.isEnabled()) {
                //Put up the Yes or No Dialog box to ask for disabling bluetooth
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setTitle("Bluetooth State Change")
                        .setMessage(Html.fromHtml("<font color='#FF0000'><b>Turn bluetooth off before exiting</b></font>"))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (btSocket != null) {
                                    try {
                                        btSocket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //Yes button clicked, do something
                                btAdapter.cancelDiscovery();
                                btAdapter.disable();
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            } else {
                if (btSocket != null) {
                    try {
                        btSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                finish();
            }

        } else if (id == R.id.action_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle("How to use")
                    .setMessage("Follows the steps\n1.In option menu, choose to turn bluetooth on(if already off.)" +
                            "\n2.Click on \"SCAN\" button to scan for devices or choose from." +
                            "\"Recently Connected\" device in option menu" +
                            "\n3. If the led turns green, your connection has been made else " +
                            "led turns red." +
                            "\n4.In case of failure, follow from step '2' again." +
                            "\n\n\u00A9 Taranjeet Singh")
                    .setCancelable(false)
                    .setIcon(R.drawable.about)
                    .setNeutralButton("OK", null)
                    .show();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem showHideOn = menu.findItem(R.id.action_on);
        MenuItem showHideOff = menu.findItem(R.id.action_off);
        if (!btAdapter.isEnabled()) {
            showHideOff.setVisible(false);
            showHideOn.setVisible(true);
        } else if (btAdapter.isEnabled()) {
            showHideOn.setVisible(false);
            showHideOff.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (address != "") {
            editor.putString("RecentMAC", address);
        }
        editor.putString("eName1", String.valueOf(item1.getText()));
        editor.putString("eName2", String.valueOf(item2.getText()));
        editor.apply();
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "OnDestroy(): Can't close socket: " + e.getMessage());
            }
        }
        address = "";
        btAdapter.cancelDiscovery();
        finish();
        super.onDestroy();
    }
}


