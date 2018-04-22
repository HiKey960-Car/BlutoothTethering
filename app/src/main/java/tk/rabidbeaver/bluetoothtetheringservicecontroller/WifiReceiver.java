package tk.rabidbeaver.bluetoothtetheringservicecontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean autooffwifi = prefs.getBoolean("autooffwifi", false);
        boolean autohotspot = prefs.getBoolean("autohotspot", false);
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

        if (autooffwifi && wifiState == WifiManager.WIFI_STATE_ENABLED){
            try {
                Thread.sleep(5000);
            } catch (Exception e){e.printStackTrace();}
            Log.d("WifiReceiver","Trying to deactivate wifi.");
            WifiManager wman = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            int counter = 0;
            while (!wman.setWifiEnabled(false) && counter < 15){
                Log.d("WifiReceiver","Failed, trying again to deactivate wifi.");
                try {
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
                counter++;
                if (!wman.isWifiEnabled()) break;
            }
        }
        if (autohotspot && !ApManager.isApOn(context)) ApManager.startTethering(context);
    }
}
