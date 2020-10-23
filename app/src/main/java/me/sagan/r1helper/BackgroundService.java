package me.sagan.r1helper;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.Log;

import com.phicomm.speaker.player.PlayerVisualizer;
import com.phicomm.speaker.player.light.LedLight;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

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

    private PlayerVisualizer mPlayerVisualizer;

    public BackgroundService() {
        super("BackgroundService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service create");
        this.mPlayerVisualizer = new PlayerVisualizer(0, this);
        this.mPlayerVisualizer.enable();
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
        while( true ) {
            try {
                if( App.enableLed ) {
                    if( !App.playing ) {
                        Calendar now = Calendar.getInstance();
                        int seconds = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
                        float brightness = 0F;
                        if( seconds >= 25200 && seconds <= 68400 ) { // 7:00 - 19:00
                            brightness = 0.7F;
                        } else if ( seconds <= 10800 ) { // 3:00
                            brightness = 0.3F;
                        } else if( seconds < 25200 ) {
                            brightness = 0.3F + (seconds-10800) * 0.4F / (25200-10800);
                        } else {
                            brightness = 0.7F -  (seconds - 68400) * 0.4F / (86400 - 68400);
                        }
                        float[] color = {seconds * 360 / 86400F, 1.0F, brightness};
                        LedLight.setColor(32767L, 0xFFFFFF & Color.HSVToColor(color));
                    }
                } else {
                    LedLight.set_color(32767L, 0);
                }
                Thread.sleep(2000);
            } catch(Exception e) {

            }
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        running = false;
        Intent intent = new Intent("me.sagan.r1helper.start");
        sendBroadcast(intent);
        super.onDestroy();
    }
}
