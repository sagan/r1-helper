package com.phicomm.speaker.player.light;

import android.util.Log;

public class LedLight {
    private static int mColor = 0;
    private static final String TAG = "LedLight";
    public static boolean loaded = false;

    public static void setColor(long paramLong, int paramInt) {
        if( loaded ) {
            if (mColor != paramInt) {
                set_color(paramLong, paramInt);
                mColor = paramInt;
            }
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
