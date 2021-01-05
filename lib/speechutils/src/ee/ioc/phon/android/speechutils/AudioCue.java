package ee.ioc.phon.android.speechutils;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;

// TODO: add a method that calls back when audio is finished
public class AudioCue {

    private static final int DELAY_AFTER_START_BEEP = 200;

    private final Context mContext;
    private final int mStartSound;
    private final int mStopSound;
    private final int mErrorSound;

    public AudioCue(Context context) {
        mContext = context;
        mStartSound = R.raw.explore_begin;
        mStopSound = R.raw.explore_end;
        mErrorSound = R.raw.error;
    }

    public AudioCue(Context context, int startSound, int stopSound, int errorSound) {
        mContext = context;
        mStartSound = startSound;
        mStopSound = stopSound;
        mErrorSound = errorSound;
    }

    public void playStartSoundAndSleep() {
        if (playSound(mStartSound)) {
            SystemClock.sleep(DELAY_AFTER_START_BEEP);
        }
    }


    public void playStopSound() {
        playSound(mStopSound);
    }


    public void playErrorSound() {
        playSound(mErrorSound);
    }


    private boolean playSound(int sound) {
        final MediaPlayer mp = MediaPlayer.create(mContext, sound);
        // create can return null, e.g. on Android Wear
        if (mp == null) {
            sendMessage("audio cue fail create player " + sound);
            return false;
        }
        // no effect?, MediaPlayer.create return a mp with prepare() called, which will ignore setAudioStreamType
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                sendMessage("audio cue play complete " + sound);
                mp.release();
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                sendMessage("audio cue play error " + sound + ", " + what + " " + extra);
                return false;
            }
        });
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                sendMessage("audio cue start play " + sound);
                mp.start();
            }
        });
        return true;
    }

    private void sendMessage(String content) {
        Intent intent = new Intent();
        intent.setAction("me.sagan.r1helper.action.MESSAGE");
        intent.putExtra( "content",content);
        mContext.sendBroadcast(intent);
    }
}