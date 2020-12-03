package me.sagan.r1helper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";
    public final static int KEY_MAIN = 275;
    private MessageReceiver receiver = null;
    boolean receiverRegistered = false;
    private boolean shortPress = false;
    private TextView mainText;
    Button testButton,clearButton;
    ScrollView main;
    DateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dateFormat = DateFormat.getDateTimeInstance();
        main = (ScrollView) findViewById(R.id.main);
        mainText = (TextView) findViewById(R.id.main_text);
        testButton = (Button) findViewById(R.id.test);
        clearButton = (Button) findViewById(R.id.clear);
        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                appendMessage("Test----\n\n\n");
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mainText.setText("");
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            unregisterReceiver(receiver);
            receiver = null;
            receiverRegistered = false;
            Log.d("AlexaService", "onPause unregister receiver");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            if(receiver == null ) {
                receiver = new MessageReceiver();
            }
            Log.d("AlexaService", "onResume register receiver");
            registerReceiver(receiver, new IntentFilter("me.sagan.r1helper.action.MESSAGE"));
            receiverRegistered = true;
        }

        if( !BackgroundService.running ) {
            BackgroundService.startActionMain(this,"","");
        }
        if( !AlexaService.running ) {
            AlexaService.startAlexa(this);
        }
    }
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KEY_MAIN) {
            shortPress = false;
            Log.d(TAG, "long press");
            return true;
        }
        //Just return false because the super call does always the same (returning false)
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "key down " + keyCode);
        if (keyCode == KEY_MAIN) {
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                event.startTracking();
                if(event.getRepeatCount() == 0){
                    shortPress = true;
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KEY_MAIN) {
            if(shortPress){
                App.mode = (App.mode + 1 ) % 3;
                Log.d(TAG, "short press, switch app mode to " + App.mode);
            } else {
                //Don't handle longpress here, because the user will have to get his finger back up first
            }
            shortPress = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("me.sagan.r1helper.action.MESSAGE")) {
                String content = intent.getStringExtra("content");
                appendMessage(content);
            }
        }
    }

    void appendMessage(String content) {
        mainText.append("\n" + dateFormat.format(new Date()) + ": " + content + "\n");
        main.post(new Runnable() {
            @Override
            public void run() {
                main.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
