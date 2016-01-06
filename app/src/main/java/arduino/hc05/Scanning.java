package arduino.hc05;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015-04-05.
 */

public class Scanning extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private ListView lv;
    private static final int GET_MAC_ADDRESS = 2;
    private static boolean DISCOVERY_FINISHED = false;
    private BluetoothAdapter BA = null;
    private BluetoothDevice device = null;
    private ArrayAdapter adapter1 = null;
    private ArrayList<String> scannedDevices = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);
        lv = (ListView) findViewById(R.id.listView);

        lv.setOnItemClickListener(this);
        BA = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        if (BA.isEnabled()) {
            BA.startDiscovery();
            Toast.makeText(this, "Scanning started...", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle("Bluetooth turned off")
                    .setMessage("Turn the bluetooth on before using the scanning feature")
                    .setCancelable(false)
                    .setIcon(R.drawable.about)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            Toast.makeText(getApplicationContext(), "Activity Closed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        }
        // Register for broadcasts when devices are found
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        super.onResume();
    }

    private BroadcastReceiver mReceiver = mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (!scannedDevices.contains(device)) {
                    scannedDevices.add("Device Name: " + device.getName() + "\nMAC Address: " + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Bluetooth adapter has stopped discovering now",
                        Toast.LENGTH_SHORT).show();
                DISCOVERY_FINISHED = true;
            }
            if (scannedDevices.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No discoverable device in vicinity",
                        Toast.LENGTH_SHORT).show();
            } else {
                adapter1 = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1,
                        scannedDevices);
                lv.setAdapter(adapter1);
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BA.cancelDiscovery();
        String temp = String.valueOf(lv.getItemAtPosition(position));
        String[] temp2 = temp.split("\n");
        String[] temp3 = temp2[1].split(" ");
        String MAC_TO_CONNECT = temp3[2].trim();


        Intent i = new Intent();
        i.putExtra("btMAC", MAC_TO_CONNECT);
        setResult(GET_MAC_ADDRESS, i);
        finish();

        Toast.makeText(this, "You clicked device-" + MAC_TO_CONNECT, Toast.LENGTH_SHORT).show();
//        BluetoothDevice remoteDevice = BA.getRemoteDevice(MAC_TO_CONNECT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scanning, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            if (BA.isEnabled()) {
                adapter1.clear();
                BA.startDiscovery();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setTitle("Bluetooth turned off")
                        .setMessage("Turn the bluetooth on before using the scanning feature")
                        .setCancelable(false)
                        .setIcon(R.drawable.about)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                                Toast.makeText(getApplicationContext(), "Activity Closed", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            }
        } else if (id == R.id.action_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setTitle("How to use")
                    .setMessage("1.Choose the device from list to connect\n" +
                            "2.If you can not see your device, try refreshing in option menu \n")
                    .setCancelable(false)
                    .setIcon(R.drawable.about)
                    .setNeutralButton("OK", null)
                    .show();

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        BA.cancelDiscovery();
        if (BA.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth still ON...", Toast.LENGTH_SHORT).show();
        }
        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}