package me.sagan.r1helper;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.Log;

import com.phicomm.speaker.player.PlayerVisualizer;
import com.phicomm.speaker.player.light.LedLight;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import androidhttpweb.TinyWebServer;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class BackgroundService extends IntentService {
    private static final String TAG = "BackgroundService";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_MAIN = "me.sagan.r1helper.action.STARTUP";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "me.sagan.r1helper.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "me.sagan.r1helper.extra.PARAM2";

    public static boolean running = false;

    private  static int btCnt = 0;

    private PlayerVisualizer mPlayerVisualizer;

    private BluetoothAdapter adapter;

//    private BroadcastReceiver bluetoothChangeReceiver = new BluetoothChangeReceiver();
//    private class BluetoothChangeReceiver extends BroadcastReceiver {
//        private BluetoothChangeReceiver() {}
//        public void onReceive(Context param1Context, Intent param1Intent) {
//        }
//    }

    public BackgroundService() {
        super("BackgroundService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service create");
        this.mPlayerVisualizer = new PlayerVisualizer(0, this);
        this.mPlayerVisualizer.enable();
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            TinyWebServer.startServer("0.0.0.0",9000, getDir("web", Context.MODE_PRIVATE).toString() );
        } catch( Exception e ) {
            Log.d(TAG, "error start web server " + e.toString());
        }
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionMain(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(ACTION_MAIN);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        running = true;
        startFrontActivity();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_MAIN.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionStartup(param1, param2);
            }
        }
    }

    /**
     * do the stuff
     */
    private void handleActionStartup(String param1, String param2) {
        if( !App.permissiive ) {
            try {
                String [] setPermissiveCmd={"su","-c","setenforce", "0"};
                Runtime.getRuntime().exec(setPermissiveCmd);
                App.permissiive = true;
            } catch (Exception e ) {
                Log.w(TAG, "Failed to setenforce 0 (no root?)");
            }
        }
//        IntentFilter intentFilter = new IntentFilter("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
//        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
//        intentFilter.addAction("android.bluetooth.adapter.action.SCAN_MODE_CHANGED");
//        registerReceiver(this.bluetoothChangeReceiver, intentFilter);

        if( adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE) {
            setBluetoothScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
            if( App.mode == 2 ) {
                App.mode = 0;
            }
            if( btCnt != 0 ) {
                btCnt = 0;
            }
        }
        int tick = 0;
        while( true ) {
            try {
                if( App.mode == 2 ) {
                    if( btCnt == 0) {
                        btCnt = 120;
                        setBluetoothScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                    } else if( btCnt == 1 ) {
                        btCnt = 0;
                        App.mode = 0;
                        setBluetoothScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                    } else {
                        btCnt--;
                    }
                    LedLight.setColor(32767L, tick % 2 == 0 ? 0x0000FF : 0xffffff);
                } else {
                    if( btCnt > 0 ) {
                        setBluetoothScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                        btCnt = 0;
                    }
                    if( App.mode == 0 ) {
                        if(tick % 2 == 0 && !App.playing ) {
                            Calendar now = Calendar.getInstance();
                            int seconds = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
                            float brightness = 0F;
                            if( seconds >= 25200 && seconds <= 75600 ) { // 7:00 - 21:00
                                brightness = 0.7F;
                            } else if ( seconds <= 10800 ) { // 3:00
                                brightness = 0.3F;
                            } else if( seconds < 25200 ) {
                                brightness = 0.3F + (seconds-10800) * 0.4F / (25200-10800);
                            } else {
                                brightness = 0.7F -  (seconds - 75600) * 0.4F / (86400 - 75600);
                            }
                            float[] color = {seconds * 360 / 86400F, 1.0F, brightness};
                            LedLight.setColor(32767L, 0xFFFFFF & Color.HSVToColor(color));
                        }
                    }  else if( App.mode == 1) {
                        if( tick % 2 == 0 ) {
                            LedLight.setColor(32767L, 0);
                        }
                    }
                }
                Thread.sleep(500);
                tick++;
                if( tick % 600 == 0 ) {
                    startFrontActivity();
                    tick = 0;
                }
            } catch(Exception e) {
            }
        }
    }

    public void startFrontActivity() {
        Intent it = new Intent(this, MainActivity.class);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(it);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        running = false;
        Intent intent = new Intent("me.sagan.r1helper.start");
        sendBroadcast(intent);
        TinyWebServer.stopServer();
        super.onDestroy();
    }

    // 20: disabled; 23 discoverable; 21 connectable;
    private boolean setBluetoothScanMode(int scanMode){
        Log.d(TAG, "set bluetooth scan mode " + scanMode);
        Method method = null;

        if(!adapter.isEnabled()){
            adapter.enable();
        }

        try {
            method = adapter.getClass().getMethod("setScanMode", int.class);
        } catch (SecurityException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }

        try {
            method.invoke(adapter, scanMode);
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
        return true;
    }
}
