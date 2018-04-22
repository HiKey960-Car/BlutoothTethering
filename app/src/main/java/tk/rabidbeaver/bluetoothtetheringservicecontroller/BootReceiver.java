package tk.rabidbeaver.bluetoothtetheringservicecontroller;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Constructor;

import static android.content.Context.MODE_PRIVATE;

public class BootReceiver extends BroadcastReceiver {
    Class<?> classBluetoothPan = null;
    Constructor<?> BTPanCtor = null;
    Object BTSrvInstance = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("Settings", MODE_PRIVATE);
        boolean autotether = prefs.getBoolean("autotether", false);
        boolean autohotspot = prefs.getBoolean("autohotspot", false);

        Log.d("BootReceiver", "autotether: "+Boolean.toString(autotether)+", autohotspot: "+Boolean.toString(autohotspot));

        if (autotether){
            Log.d("BootReceiver","Trying to activate tethering.");

            try {
                Thread.sleep(250);
                classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
                BTPanCtor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
                BTPanCtor.setAccessible(true);

                BTSrvInstance = BTPanCtor.newInstance(context, new BTPanServiceEnabler());
                Thread.sleep(250);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if (autohotspot && !ApManager.isApOn(context)) ApManager.startTethering(context);
    }
}
