package com.phicomm.speaker.player;

import android.content.Context;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.util.Log;

import me.sagan.r1helper.App;
import me.sagan.r1helper.Tool;

public class PlayerVisualizer {
    private static final float CHROMA_INCRE_STEP = 0.001F;

    public static final String MUSIC_LIGHT_CHROMA = "music_light_chroma";

    public static final String MUSIC_LIGHT_ENABLE = "music_light_enable";

    public static final String MUSIC_LIGHT_LUMA = "music_light_luma";

    public static final String MUSIC_LIGHT_MODE = "music_light_mode";

    public static final int SCALING_MODE_AS_PLAYED = 1;

    public static final int SCALING_MODE_NORMALIZED = 0;

    private static final String TAG = "PlayerVisualizer";

    private float amp1 = 0.0F;

    private float amp2 = 0.0F;

    private float amp3 = 0.0F;

    private Visualizer.OnDataCaptureListener dataCaptureListener = new Visualizer.OnDataCaptureListener() {
        public void onFftDataCapture(Visualizer param1Visualizer, byte[] param1ArrayOfbyte, int param1Int) {
            float f1 = 0.0F;
            param1Int = 2;
            while (param1Int < param1ArrayOfbyte.length) {
                int i = (int)Math.hypot(param1ArrayOfbyte[param1Int], param1ArrayOfbyte[param1Int + 1]);
                float f = f1;
                if (f1 < i)
                    f = i;
                param1Int += 2;
                f1 = f;
            }
            float f2 = PlayerVisualizer.this.move5Avg(f1) / 180.0F;
            //PlayerVisualizer.access$116(PlayerVisualizer.this, PlayerVisualizer.this.mChromaGain * 0.001F);
            //PlayerVisualizer.access$148(PlayerVisualizer.this, 1.0F);
            mHue += mChromaGain * 0.36;
            mHue %= 360;

            f1 = f2;
            if (mScalingMode == 0)
                f1 = f2 * mLumaGain / 100.0F;
//            Log.d(TAG, "set-color " + mHue + " " + f1);
            if( (App.mode == 0 || App.mode == 3) && f1 != 0 ) {
                float[] color = {mHue, 1.0F, f1};
                Tool.setLight(Color.HSVToColor(color));
            }
            App.playing = f1 != 0;
        }
        public void onWaveFormDataCapture(Visualizer param1Visualizer, byte[] param1ArrayOfbyte, int param1Int) {}
    };

    private int mChromaGain = 4; // [0.100]

    private Context mContext;

    private float mHue = 0.0F;

    private int mLumaGain = 100;

    private int mScalingMode = 0;

    private int mSessionId = 0;

    private Visualizer mVisualizer;

    private boolean mVisualizerEnable = false;

    public PlayerVisualizer(int paramInt, Context paramContext) {
        this.mContext = paramContext;
        this.mSessionId = paramInt;
    }

    private float move5Avg(float paramFloat) {
        this.amp1 = this.amp2;
        this.amp2 = this.amp3;
        this.amp3 = paramFloat;
        return (this.amp1 + this.amp2 + this.amp3) / 3.0F;
    }

    public void disable() {
        if (this.mVisualizerEnable) {
            if (this.mVisualizer != null) {
                this.mVisualizer.release();
                this.mVisualizer = null;
            }
            this.mVisualizerEnable = false;
            Tool.setLight(0);
            App.playing = false;
            Log.v(TAG, "disable");
        }
    }

    public void enable() {
        if (!this.mVisualizerEnable) {
            this.mVisualizer = new Visualizer(this.mSessionId);
            this.mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            this.mVisualizer.setScalingMode(this.mScalingMode);
            this.mVisualizer.setDataCaptureListener(this.dataCaptureListener, Visualizer.getMaxCaptureRate(), false, true);
            Log.v(TAG, "captureSize:" + this.mVisualizer.getCaptureSize() + " sampleRate:" + this.mVisualizer.getSamplingRate() + " maxCaptureRate:" + Visualizer.getMaxCaptureRate() + " scalingMode:" + this.mVisualizer.getScalingMode()  + " mScalingMode:" + this.mScalingMode + " mLumaGain:" + this.mLumaGain + " mChromaGain:" + this.mChromaGain);
            this.mVisualizer.setEnabled(true);
            this.mVisualizerEnable = true;
            Log.v(TAG, "enable");
        }
    }

    public int setChromaGain(int paramInt) {
        if (paramInt <= 0 || paramInt > 100) {
            Log.v(TAG, "invalid chroma gain:" + paramInt);
            return -1;
        }
        this.mChromaGain = paramInt;
        Log.v(TAG, "set chrome gain:" + paramInt);
        return 0;
    }

    public int setLumaGain(int paramInt) {
        if (paramInt < 0 || paramInt > 100) {
            Log.v(TAG, "invalid luma gain:" + paramInt);
            return -1;
        }
        this.mLumaGain = paramInt;
        Log.v(TAG, "set luma gain:" + paramInt);
        return 0;
    }

    public int setScalingMode(int paramInt) {
        if (paramInt != 0 && paramInt != 1) {
            Log.v(TAG, "invalid scaling mode:" + paramInt);
            return -1;
        }
        this.mScalingMode = paramInt;
        if (this.mVisualizer != null)
            this.mVisualizer.setScalingMode(paramInt);
        Log.v(TAG, "set scaling mode:" + paramInt);
        return 0;
    }
}
