package me.sagan.r1helper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.OutputStream;

public class Tool {
    private static final String TAG = "Tool";
    private static int currentColor = 0;
    private static OutputStream lightFile;

    static {
        try {
            lightFile = new FileOutputStream("/sys/class/leds/multi_leds0/led_color");
        } catch(Exception e) {}
    }

    public static  String escapeJsonSpecial(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        // TODO: escape other non-printing characters using uXXXX notation
        return escaped;
    }

    public static void sendMessage(Context context, String content) {
        Intent intent = new Intent();
        intent.setAction("me.sagan.r1helper.action.MESSAGE");
        intent.putExtra( "content",content);
        context.sendBroadcast(intent);
    }

    public static void setLight(int color) {
        try {
            // LedLight.setColor(32767L, color);
            if( lightFile != null && currentColor != color  ) {
                lightFile.write(String.format("%04x %06x", 32767L, 0xFFFFFF & color).getBytes());
                currentColor = color;
            }
        } catch(Exception e) {}
    }
}
