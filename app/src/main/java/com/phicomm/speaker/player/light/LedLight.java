package com.phicomm.speaker.player.light;

import android.util.Log;

public class LedLight {
    private static final String TAG = "LedLight";
    public static boolean loaded = false;

    public static void setColor(int paramInt) {
        setColor(32767L, paramInt);
    }

    public static void setColor(long paramLong, int paramInt) {
        if( loaded ) {
            set_color(paramLong, paramInt);
        }
    }

    // need selinux permissive to work
    public static native void set_color(long paramLong, int paramInt);

    static {
        try {
            System.loadLibrary("ledLight-jni");
            loaded = true;
            Log.w(TAG, "R1 native library successfully loaded");
        } catch(UnsatisfiedLinkError e) {
            Log.w(TAG,"R1 ledLight-jni native library not found");
        }
    }
}
