package tk.rabidbeaver.bluetoothtetheringservicecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class TetheringConfigActivity extends AppCompatActivity {
    Class<?> classBluetoothPan = null;
    Constructor<?> BTPanCtor = null;
    Object BTSrvInstance = null;
    private static final int WRITE_SETTINGS_REQUEST_CONST = 37119;

    private boolean setContainsString(Set<String> set, String string){
        Object[] setarr = set.toArray();
        for (int i=0; i<set.size(); i++){
            if (((String)setarr[i]).contentEquals(string)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_tethering);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        Set<String> selectedDevices = prefs.getStringSet("devices", new HashSet<String>());

        boolean autoconnect = prefs.getBoolean("autoconnect", false);
        boolean autotether = prefs.getBoolean("autotether", false);
        boolean autooffwifi = prefs.getBoolean("autooffwifi", false);
        boolean autohotspot = prefs.getBoolean("autohotspot", false);
        boolean autowifitether = prefs.getBoolean("autowifitether", false);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        PairedDev[] pda = new PairedDev[pairedDevices.size()];
        boolean[] selected = new boolean[pairedDevices.size()];

        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            Log.d("TetheringConfigActivity",device.getName()+", "+device.getAddress());
            if (selectedDevices.size() > 0 && setContainsString(selectedDevices, device.getAddress())) selected[i] = true;
            pda[i] = new PairedDev(device);
            i++;
        }

        final MultiSpinner spinner = (MultiSpinner) findViewById(R.id.devspin);
        final ArrayAdapter spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pda);
        spinner.setAdapter(spinnerArrayAdapter, false, null);

        spinner.setSelected(selected);

        Switch nswitch = (Switch) findViewById(R.id.clisw);
        nswitch.setChecked(autoconnect);
        nswitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
                editor.putBoolean("autoconnect", isChecked);
                Set<String> selectedItems = new HashSet<>();
                for (int i=0; i<spinnerArrayAdapter.getCount(); i++){
                    if (spinner.getSelected()[i]) selectedItems.add(((PairedDev)spinnerArrayAdapter.getItem(i)).getDev());
                }
                editor.putStringSet("devices", selectedItems);
                editor.apply();
            }
        });

        Switch wifiswitch = (Switch) findViewById(R.id.offwifi);
        wifiswitch.setChecked(autooffwifi);
        wifiswitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
                editor.putBoolean("autooffwifi", isChecked);
                editor.apply();
            }
        });

        Switch hotspotswitch = (Switch) findViewById(R.id.onhotspot);
        hotspotswitch.setChecked(autohotspot);
        hotspotswitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
                editor.putBoolean("autohotspot", isChecked);
                editor.apply();
                if (isChecked){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!Settings.System.canWrite(getApplicationContext())) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }
            }
        });

        nswitch = (Switch) findViewById(R.id.srvsw);
        nswitch.setChecked(autotether);
        nswitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
                editor.putBoolean("autotether", isChecked);
                editor.apply();
            }
        });

        nswitch = (Switch) findViewById(R.id.srvwifisw);
        nswitch.setChecked(autowifitether);
        nswitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
                editor.putBoolean("autowifitether", isChecked);
                editor.apply();
            }
        });

        findViewById(R.id.srvbtn).setOnClickListener(new Switch.OnClickListener(){
            public void onClick(View v){
                try {
                    BluetoothManager bm = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
                    BluetoothAdapter mBluetoothAdapter = bm.getAdapter();
                    mBluetoothAdapter.enable();
                    Thread.sleep(100);
                    enableTethering();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Button sett = (Button) findViewById(R.id.btset);
        sett.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intentOpenBluetoothSettings = new Intent();
                intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intentOpenBluetoothSettings);
            }
        });

        Button button = (Button) findViewById(R.id.panbtn);
        button.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                String sClassName = "android.bluetooth.BluetoothPan";

                class BTPanClientListener implements BluetoothProfile.ServiceListener {
                    @Override
                    public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
                        Log.e("TetheringConfigActivity", "BTPan proxy connected");
                        PairedDev selectedItem = null;
                        for (int i=0; i<spinnerArrayAdapter.getCount(); i++){
                            if (spinner.getSelected()[i]) selectedItem = (PairedDev) spinnerArrayAdapter.getItem(i);
                        }
                        String dev = selectedItem.getDev();
                        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(dev); //e.g. this line gets the hardware address for the bluetooth device with MAC AA:BB:CC:DD:EE:FF. You can use any BluetoothDevice
                        try {
                            Method connectMethod = proxy.getClass().getDeclaredMethod("connect", BluetoothDevice.class);
                            if(!((Boolean) connectMethod.invoke(proxy, device))){
                                Log.e("TetheringConfigActivity", "Unable to start connection");
                            }
                        } catch (Exception e) {
                            Log.e("TetheringConfigActivity", "Unable to reflect android.bluetooth.BluetoothPan", e);
                        }
                    }

                    @Override
                    public void onServiceDisconnected(final int profile) {}
                }

                try {
                    Class<?> classBluetoothPan = Class.forName(sClassName);

                    Constructor<?> ctor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
                    ctor.setAccessible(true);
                    ctor.newInstance(getApplicationContext(), new BTPanClientListener());
                } catch (Exception e) {
                    Log.e("TetheringConfigActivity", "Unable to reflect android.bluetooth.BluetoothPan", e);
                }
            }
        });
    }

    private class PairedDev {
        String dev;
        String name;
        PairedDev(BluetoothDevice btd){
            name = btd.getName();
            dev = btd.getAddress();
        }
        public String toString(){return name;}
        String getDev(){return dev;}
    }

    private void enableTethering() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)){
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            try {
                classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
                BTPanCtor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
                BTPanCtor.setAccessible(true);
                BTSrvInstance = BTPanCtor.newInstance(getApplicationContext(), new BTPanServiceEnabler());
                Thread.sleep(250);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
