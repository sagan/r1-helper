package me.sagan.r1helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MainReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if( !BackgroundService.running ) {
            BackgroundService.startActionMain(context,"","");
        }
        if( !AlexaService.running ) {
            AlexaService.startAlexa(context);
        }
    }
}
