package com.iotina.iotina_blue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;



import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



   // private FirmwareUpdater mFirmwareUpdater;
   // private PeripheralList mPeripheralList;
    TextView t;

    private ArrayList<BluetoothDeviceData> mScannedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t = (TextView)findViewById(R.id.test);


        // Init variables

        t.setText("Boom Boom baby");

    }



    boolean checkBluetoothConnectivity() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth is not supported in your device.", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }
    }

    // region Scan
    private void startScan(final UUID[] servicesToScan) {
        Log.d("da", "startScan");

        // Stop current scanning (if needed)
        //stopScanning();
        t.setText("sdaf");


    }


    // region Helpers
    private class BluetoothDeviceData {
        BluetoothDevice device;
        public int rssi;
        byte[] scanRecord;
        private String advertisedName;           // Advertised name
        private String cachedNiceName;
        private String cachedName;

        // Decoded scan record (update R.array.scan_devicetypes if this list is modified)
        static final int kType_Unknown = 0;
        static final int kType_Uart = 1;
        static final int kType_Beacon = 2;
        static final int kType_UriBeacon = 3;

        public int type;
        int txPower;
        ArrayList<UUID> uuids;

        String getName() {
            if (cachedName == null) {
                cachedName = device.getName();
                if (cachedName == null) {
                    cachedName = advertisedName;      // Try to get a name (but it seems that if device.getName() is null, this is also null)
                }
            }

            return cachedName;
        }

        String getNiceName() {
            if (cachedNiceName == null) {
                cachedNiceName = getName();
                if (cachedNiceName == null) {
                    cachedNiceName = device.getAddress();
                }
            }

            return cachedNiceName;
        }
    }
    //endregion
}
