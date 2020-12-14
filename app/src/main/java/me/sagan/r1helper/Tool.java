package me.sagan.r1helper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.phicomm.speaker.player.light.LedLight;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

public class Tool {
    private static final String TAG = "Tool";
    private static int currentColor = 0;
    private static DateFormat dateFormat = DateFormat.getDateTimeInstance();

//    private static OutputStream lightFile;
//    static {
//        try {
//            lightFile = new FileOutputStream("/sys/class/leds/multi_leds0/led_color");
//        } catch(Exception e) {}
//    }

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

    public static void addLog(Context context, String content) {
        if( content != null ) {
            if( App.log.length() > 10000 ) {
                App.log = StringUtils.substring(App.log, 5000);
            }
            App.log += dateFormat.format(new Date()) + ": " + content + "\n";
        }
        if( context != null ) {
            Intent intent = new Intent();
            intent.setAction("me.sagan.r1helper.action.REFRESH");
            context.sendBroadcast(intent);
        }
    }

    public static void setLight(int color) {
        color = 0xFFFFFF & color;
        if( currentColor != color  ) {
//            try {
//                if( lightFile != null  ) {
//                    lightFile.write(String.format("%04x %06x", 32767L, 0xFFFFFF & color).getBytes());
//                }
//            } catch(Exception e) {}
            LedLight.setColor(color);
            currentColor = color;
        }
    }

    public static boolean empty(Object obj) {
        if( obj == null ) {
            return true;
        }
        if( obj instanceof String) {
            String str = (String) obj;
            return str.equals("");
        }
        return false;
    }
}
