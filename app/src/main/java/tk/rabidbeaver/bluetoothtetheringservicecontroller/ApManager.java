package tk.rabidbeaver.bluetoothtetheringservicecontroller;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;

import com.android.dx.Code;
import com.android.dx.DexMaker;
import com.android.dx.Local;
import com.android.dx.MethodId;
import com.android.dx.TypeId;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class ApManager {

    //check whether wifi hotspot on or off
    static boolean isApOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        }
        catch (Throwable ignored) {}
        return false;
    }

    static void startTethering(Context context){

        CallbackMaker cm = new CallbackMaker(context);
        Class<?> mSystemCallbackClazz = cm.getCallBackClass();
        Object mSystemCallback;

        ConnectivityManager manager = context.getApplicationContext().getSystemService(ConnectivityManager.class);
        Class callbackClass;
        try {
            Constructor constructor = mSystemCallbackClazz.getDeclaredConstructor(int.class);
            mSystemCallback = constructor.newInstance(0);
            callbackClass = Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");
            manager.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, callbackClass, Handler.class)
                    .invoke(manager, 0, false, mSystemCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void stopTethering(Context context) {
        ConnectivityManager manager = context.getApplicationContext().getSystemService(ConnectivityManager.class);
        try {
            manager.getClass().getDeclaredMethod("stopTethering", int.class).invoke(manager, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final class CallbackMaker {
        Context mContext;
        Class <?> myTetheringCallbackClazz;
        DexMaker dexMaker;

        CallbackMaker(Context c){
            mContext = c;

            Class callbackClass;
            try {
                callbackClass = Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }

            TypeId<?> systemCallbackTypeId = TypeId.get(callbackClass);

            dexMaker = new DexMaker();

            // Generate a TetheringCallback class.
            TypeId<?> tetheringCallback = TypeId.get("LTetheringCallback;");

            dexMaker.declare(tetheringCallback, "TetheringCallback.generated", Modifier.PUBLIC, systemCallbackTypeId);

            generateConstructor(tetheringCallback,systemCallbackTypeId);

            // Create the dex file and load it.
            File outputDir = mContext.getCodeCacheDir();

            try {
                ClassLoader loader = dexMaker.generateAndLoad(CallbackMaker.class.getClassLoader(),outputDir);
                myTetheringCallbackClazz = loader.loadClass("TetheringCallback");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void generateConstructor(TypeId<?> declaringType, TypeId<?> superType){
            final MethodId<?, ?> superConstructor = superType.getConstructor();

            MethodId<?, ?> constructor = declaringType.getConstructor(TypeId.INT);
            Code constructorCode = dexMaker.declare(constructor, Modifier.PUBLIC);
            final Local thisRef = constructorCode.getThis(declaringType);
            constructorCode.invokeDirect(superConstructor, null, thisRef);
            constructorCode.returnVoid();
        }

        Class<?> getCallBackClass(){
            return myTetheringCallbackClazz;
        }
    }
}