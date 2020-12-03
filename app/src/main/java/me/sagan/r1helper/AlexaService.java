package me.sagan.r1helper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.KeyEvent;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import ai.kitt.snowboy.SnowboyDetect;
import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;
import okio.BufferedSink;

public class AlexaService extends IntentService {
    private static final String TAG = "AlexaService";
    private static final String ACTION_ALEXA = "me.sagan.r1helper.action.ALEXA";
    public static boolean running = false;

    private static boolean shouldDetect;
    private static SnowboyDetect snowboyDetect;

    private static final int AUDIO_RATE = 16000;
    private RawAudioRecorder recorder;
    private AlexaManager alexaManager;
    private AlexaAudioPlayer audioPlayer;
    private List<AvsItem> avsQueue = new ArrayList<>();

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {
        @Override
        public void playerPrepared(AvsItem pendingItem) {
            Log.d(TAG, "play_prepared");
        }

        @Override
        public void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent) {
            Log.d(TAG, "play_progress percent " + percent);
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            Log.d(TAG, "play_item_complete");
            avsQueue.remove(completedItem);
            checkQueue();
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            Log.d(TAG, "play_error " + what + "," + extra);
            return false;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            Log.d(TAG, "play_data_error " + e.getMessage());
        }
    };


    //async callback for commands sent to Alexa Voice
    private AsyncCallback<AvsResponse, Exception> requestCallback = new AsyncCallback<AvsResponse, Exception>() {
        @Override
        public void start() {
            Log.i(TAG, "Voice start");
        }

        @Override
        public void success(AvsResponse result) {
            Log.i(TAG, "Voice Success");
            handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            Log.i(TAG, "Voice failure " + error.getMessage());
        }

        @Override
        public void complete() {
            Log.i(TAG, "Voice complete");
        }
    };

    /**
     * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
     * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
     */
    private void handleResponse(AvsResponse response){
        if(response != null){
            //if we have a clear queue item in the list, we need to clear the current queue before proceeding
            //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
            //from doing that
            for(int i = response.size() - 1; i >= 0; i--){
                if(response.get(i) instanceof AvsReplaceAllItem || response.get(i) instanceof AvsReplaceEnqueuedItem){
                    //clear our queue
                    avsQueue.clear();
                    //remove item
                    response.remove(i);
                }
            }
            avsQueue.addAll(response);
        }
        checkQueue();
    }


    /**
     * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
     * next item in our list.
     *
     * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
     * the new items are added to the list, it should have no function here.
     */
    private void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
            return;
        }

        AvsItem current = avsQueue.get(0);

        Log.d(TAG, "current queue item " + current.getClass().getName());

        if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayRemoteItem) current);
            }
        } else if (current instanceof AvsPlayContentItem) {
            //play a URL
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsPlayContentItem) current);
            }
        } else if (current instanceof AvsSpeakItem) {
            //play a sound file
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playItem((AvsSpeakItem) current);
            }
        } else if (current instanceof AvsStopItem) {
            //stop our play
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceAllItem) {
            audioPlayer.stop();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            avsQueue.remove(current);
        } else if (current instanceof AvsExpectSpeechItem) {
            //listen for user input
            audioPlayer.stop();
            startListening();
        } else if (current instanceof AvsSetVolumeItem) {
            setVolume(((AvsSetVolumeItem) current).getVolume());
            avsQueue.remove(current);
        } else if(current instanceof AvsAdjustVolumeItem){
            adjustVolume(((AvsAdjustVolumeItem) current).getAdjustment());
            avsQueue.remove(current);
        } else if(current instanceof AvsSetMuteItem){
            setMute(((AvsSetMuteItem) current).isMute());
            avsQueue.remove(current);
        }else if(current instanceof AvsMediaPlayCommandItem){
            //fake a hardware "play" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY);
        }else if(current instanceof AvsMediaPauseCommandItem){
            //fake a hardware "pause" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE);
        }else if(current instanceof AvsMediaNextCommandItem){
            //fake a hardware "next" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT);
        }else if(current instanceof AvsMediaPreviousCommandItem){
            //fake a hardware "previous" press
            sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }

    }

    public void startListening() {
        if(recorder == null){
            recorder = new RawAudioRecorder(AUDIO_RATE);
        }
        recorder.start();
        Log.d(TAG, "Alexa start listening");
        alexaManager.sendAudioRequest(requestBody, requestCallback);
    }

    //tear down our recorder
    private void stopListening(){
        if(recorder != null) {
            Log.d(TAG, "stop listening");
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    private DataRequestBody requestBody = new DataRequestBody() {
        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            //while our recorder is not null and it is still recording, keep writing to POST data
            while (recorder != null && recorder.getState() != AudioRecorder.State.ERROR && !recorder.isPausing()) {
                if(recorder != null) {
                    final float rmsdb = recorder.getRmsdb();
//                    if(recorderView != null) {
//                        recorderView.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                recorderView.setRmsdbLevel(rmsdb);
//                            }
//                        });
//                    }
                    if(sink != null && recorder != null) {
                        sink.write(recorder.consumeRecording());
                    }
                }

                //sleep and do it all over again
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopListening();
        }
    };

    //adjust our device volume
    private void adjustVolume(long adjust){
        setVolume(adjust, true);
    }

    //set our device volume
    private void setVolume(long volume){
        setVolume(volume, false);
    }

    //set our device volume, handles both adjust and set volume to avoid repeating code
    private void setVolume(final long volume, final boolean adjust){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(adjust){
            vol += volume * max / 100;
        }else{
            vol = volume * max / 100;
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, AudioManager.FLAG_VIBRATE);
        //confirm volume change
        alexaManager.sendVolumeChangedEvent(volume, vol == 0, requestCallback);
    }

    //set device to mute
    private void setMute(final boolean isMute){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);
        //confirm device mute
        alexaManager.sendMutedEvent(isMute, requestCallback);
    }

    private static void sendMediaButton(Context context, int keyCode) {
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        context.sendOrderedBroadcast(intent, null);

        keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        context.sendOrderedBroadcast(intent, null);
    }

    public AlexaService() {
        super("AlexaService");
    }

    static {
        System.loadLibrary("snowboy-detect-android");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(this, "r1assistant");
        audioPlayer = AlexaAudioPlayer.getInstance(this);
        audioPlayer.addCallback(alexaAudioPlayerCallback);
    }

    public static void startAlexa(Context context) {
        Intent intent = new Intent(context, AlexaService.class);
        intent.setAction(ACTION_ALEXA);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        running = true;
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "action " + action);
            if (ACTION_ALEXA.equals(action)) {
                go();
            }
        }
    }

    public void onDestroy() {
        stopForeground(true);
        Log.d(TAG, "onDestroy");
        running = false;
        Intent intent = new Intent("me.sagan.r1helper.start");
//        sendBroadcast(intent);
        snowboyDetect.delete();
        super.onDestroy();
    }

    private void go() {
        SnowboyUtils.copyAssets(this);
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
        File model = new File(snowboyDirectory, "alexa.umdl");
        File common = new File(snowboyDirectory, "common.res");

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model.getAbsolutePath());
        snowboyDetect.setSensitivity("0.1"); // 0.48,0.43
        snowboyDetect.applyFrontend(true);
        Log.d(TAG, "detecting hotword ---");
        int bufferSize =(int) (16000 * 2 * 0.1); // 0.1 second
        shouldDetect = true;
        while( true ) {
            if( !shouldDetect ) {
                Tool.sleep(1000);
                continue;
            }
            try {
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                );
                byte[] audioBuffer = new byte[bufferSize];
                audioRecord.startRecording();
                Log.d(TAG, "start listening to hotword");
                Tool.sendMessage(this, "listening hotword");
                int result = 0;
                snowboyDetect.reset();
                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    result = snowboyDetect.runDetection(shortArray, shortArray.length);
//                    Log.d(TAG, "detect result " + result);
                    if (result > 0) {
                        break;
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                if (result > 0) {
                    Log.d(TAG,"hotword detected " + result);
                    Tool.sendMessage(this, "hotword detected");
                    shouldDetect = false;
                    startListening();
                }
            } catch(Exception e) {
                Tool.sleep(5000);
                Log.d(TAG, "error" +  e.getMessage());
                Tool.sendMessage(this, "Error " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
