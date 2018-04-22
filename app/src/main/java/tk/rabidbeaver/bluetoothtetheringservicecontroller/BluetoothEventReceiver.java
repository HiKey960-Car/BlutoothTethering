package tk.rabidbeaver.bluetoothtetheringservicecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Set;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static android.content.Context.MODE_PRIVATE;


public class BluetoothEventReceiver extends BroadcastReceiver {
    Class<?> classBluetoothPan = null;
    Constructor<?> BTPanCtor = null;
    Object BTSrvInstance = null;
    BluetoothDevice device;

    private boolean setContainsString(Set<String> set, String string){
        Object[] setarr = set.toArray();
        for (int i=0; i<set.size(); i++){
            if (((String)setarr[i]).contentEquals(string)) return true;
        }
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("Settings", MODE_PRIVATE);
        Set<String> selectedDevices = prefs.getStringSet("devices", null);
        boolean autoconnect = prefs.getBoolean("autoconnect", false);
        boolean autotether = prefs.getBoolean("autotether", false);
        //boolean autooffwifi = prefs.getBoolean("autooffwifi", false);
        boolean autowifitether = prefs.getBoolean("autowifitether", false);

        if (intent.getAction().contentEquals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")){
            Log.e("BluetoothEventReceiver", "HEADSET CONNECTION_STATE_CHANGED");
            if (autowifitether && intent.hasExtra("android.bluetooth.profile.extra.STATE")){
                int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                if (state == BluetoothProfile.STATE_CONNECTED){
                    boolean wifiEnabled = ((WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
                    boolean apOn = ApManager.isApOn(context);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("wifiIsOn", wifiEnabled);
                    editor.putBoolean("wifiApIsOn", apOn);
                    editor.apply();

                    if (!apOn) ApManager.startTethering(context);
                } else if (state == BluetoothProfile.STATE_DISCONNECTED){
                    boolean wifiEnabled = prefs.getBoolean("wifiIsOn", false);
                    boolean apOn = prefs.getBoolean("wifiIsApOn", false);
                    if (!apOn && ApManager.isApOn(context)) ApManager.stopTethering(context);
                    if (wifiEnabled) ((WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
                }
            }
        } else {

            int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
            if (autotether && state == BluetoothAdapter.STATE_ON) {
                Log.d("BluetoothEventReceiver", "Bluetooth has turned ON");

                try {
                    Thread.sleep(250);
                    classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
                    BTPanCtor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
                    BTPanCtor.setAccessible(true);

                    BTSrvInstance = BTPanCtor.newInstance(context, new BTPanServiceEnabler());
                    Thread.sleep(250);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int connstate = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", -1);
            if (connstate == BluetoothAdapter.STATE_CONNECTED) {
                Log.d("BluetoothEventReceiver", "Bluetooth has CONNECTED");

                //if (autooffwifi) ((WifiManager)context.getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(false);

                device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device != null) {
                    Log.d("BluetoothEventReceiver", "Bluetooth has connected to " + device.getName());
                    if (autoconnect && selectedDevices != null && selectedDevices.size() > 0 && setContainsString(selectedDevices, device.getAddress())) {
                        // Currently this will try to connect to this device, regardless of whether or not
                        // it is already connected to *something*. I'm not sure if this matters or not.

                        String sClassName = "android.bluetooth.BluetoothPan";

                        class BTPanClientListener implements BluetoothProfile.ServiceListener {
                            @Override
                            public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
                                Log.e("BluetoothEventReceiver", "BTPan proxy connected");
                                try {
                                    Method connectMethod = proxy.getClass().getDeclaredMethod("connect", BluetoothDevice.class);
                                    if (!((Boolean) connectMethod.invoke(proxy, device))) {
                                        Log.e("BluetoothEventReceiver", "Unable to start connection");
                                        //TODO: if we get here, it means that the authorized device has
                                        // established a bluetooth connection with THIS device, but we
                                        // were unsuccessful at establishing a PAN connection with it.
                                        // Probably means that the PAN service is not running on THAT
                                        // device. We should keep trying until THAT device is no longer
                                        // connected. Probably use BluetoothPan.getConnectionState(BluetoothDevice)
                                        // to validate that it is still connected.

                                    }
                                } catch (Exception e) {
                                    Log.e("BluetoothEventReceiver", "Unable to reflect android.bluetooth.BluetoothPan", e);
                                }
                            }

                            @Override
                            public void onServiceDisconnected(final int profile) {
                            }
                        }

                        try {
                            Class<?> classBluetoothPan = Class.forName(sClassName);
                            Constructor<?> ctor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
                            ctor.setAccessible(true);
                            ctor.newInstance(context, new BTPanClientListener());
                        } catch (Exception e) {
                            Log.e("BluetoothEventReceiver", "Unable to reflect android.bluetooth.BluetoothPan", e);
                        }
                    }
                }
            }
        }

        /*// This is debug code, used to just dump all the values from the received intent.
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Log.e("BluetoothEventReceiver","Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Log.e("BluetoothEventReceiver","[" + key + "=" + bundle.get(key)+"]");
            }
            Log.e("BluetoothEventReceiver","Dumping Intent end");
        }*/
    }
}