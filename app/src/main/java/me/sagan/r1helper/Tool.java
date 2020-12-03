package me.sagan.r1helper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Tool {
    private static final String TAG = "Tool";

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

    public static void sleep(int miliseconds) {
        // 傻逼java连sleep都抛异常
        try {
            Thread.sleep(miliseconds);
        } catch (Exception e1 ) { }
    }

    public static void sendMessage(Context context, String content) {
        Intent intent = new Intent();
        intent.setAction("me.sagan.r1helper.action.MESSAGE");
        intent.putExtra( "content",content);
        context.sendBroadcast(intent);
    }
}
