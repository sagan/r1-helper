package me.sagan.r1helper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.os.health.SystemHealthManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.audioplayer.PlaybackAudioPlayer;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
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
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsStopCaptureItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;
import com.willblaschko.android.alexa.service.DownChannelService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ai.kitt.snowboy.SnowboyDetect;
import ee.ioc.phon.android.speechutils.AudioCue;
import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;
import ee.ioc.phon.android.speechutils.SpeechRecord;
import okio.BufferedSink;

public class AlexaService extends IntentService {
    private static final String TAG = "AlexaService";
    private static final String ACTION_ALEXA = "me.sagan.r1helper.action.ALEXA";
    SharedPreferences preferences;

    public static boolean running = false;
    public int isLogin = 0;
    public static boolean enteredIdle = false;
    public static boolean listening = false; // alexa is listening voice;
    public long listeningStartTime = 0;
    public static boolean playing = false;

    private static SnowboyDetect snowboyDetect;
    private static final int AUDIO_RATE = 16000;
    private RawAudioRecorder recorder;
    private AlexaManager alexaManager;
    private AlexaAudioPlayer audioPlayer;
    private PlaybackAudioPlayer playbackAudioPlayer;
    AudioCue audioCue;
    private List<AvsItem> avsQueue = new ArrayList<>();
    public static AlexaService instance;
    private long lastTrimCacheTime = 0;
    private int recordPausing = 0;

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(AvsItem item) {
        Log.d(TAG, "receive AvsItem from down channel " + item.getClass().getName());
        avsQueue.add(item);
        checkQueue();
    };

    public static void setLanguage(String lang) {
        if( instance != null ) {
            instance.sendMessage("Set language to " + lang);
            instance.alexaManager.sendEvent(Event.getSettingsUpdatedEvent(lang), new AsyncCallback<AvsResponse, Exception>() {
                @Override
                public void start() {
                    Log.i(TAG, "Set language start");
                }

                @Override
                public void success(AvsResponse result) {
                    if( instance != null ) {
                        instance.sendMessage("Set language Success");
                    }
                }

                @Override
                public void failure(Exception error) {
                    if( instance != null ) {
                        instance.sendMessage( "Set language failure " + error.getMessage());
                    }
                }

                @Override
                public void complete() {
                    Log.i(TAG, "Set language complete");
                }
            });
        }
    }
    public static void reset(boolean full) {
        if( instance != null ) {
            instance.alexaManager.cancelAudioRequest();
            instance.checkLogin();
            instance.avsQueue.clear();
            instance.audioPlayer.stop();
            instance.playbackAudioPlayer.stop();
            instance.alexaManager.finishedPlayback();
            instance.alexaManager.updateInitiator(null);
            if( full ) {
                instance.stopService(new Intent(instance, DownChannelService.class));
                instance.startService(new Intent(instance, DownChannelService.class));
            }
            instance.stopListening();
        }
    }
    public static void trigger() {
        if( instance != null ) {
            instance.startListening();
        }
    }

    public static void test2(int audioSource, int sampleRate, int audioFormat, int channel) {
        if( instance != null ) {
            instance.stopListening();
            instance.recorder.stop();
            instance.recorder.release();
            try {
                instance.sendMessage("Test audio record audioSource " + audioSource
                        + " sampleRate " + sampleRate
                        + " audioFormat " + audioFormat
                        + " channel " + channel
                );
                AudioRecord ar = new AudioRecord(audioSource, sampleRate, channel, audioFormat, 10000);
                ar.startRecording();
                SystemClock.sleep(50);
                ar.startRecording();
                ar.release();
                instance.sendMessage("Test audio record ok");
            } catch(Exception e) {
                instance.sendMessage("Test audio record error " + e.getMessage());
            }
            reset(false);
        }
    }

    public static void login() {
        if( instance != null ) {
            instance.alexaManager.logIn(new AuthorizationCallback() {
                @Override
                public void onCancel() {

                }
                @Override
                public void onSuccess() {
                    instance.sendMessage("login success");
                    instance.stopService(new Intent(instance, DownChannelService.class));
                    instance.startService(new Intent(instance, DownChannelService.class));
                }
                @Override
                public void onError(Exception error) {
                    instance.sendMessage("login error " + error.getMessage());
                }
            });
        }
    }
    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {
        @Override
        public void playerPrepared(AvsItem pendingItem) {
            playing = true;
            sendMessage("play response");
        }

        @Override
        public void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent) {
//            Log.d(TAG, "play_progress percent " + percent);
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            playing = false;
            sendMessage("play response complete");
            avsQueue.remove(completedItem);
            audioPlayer.stop();
            checkQueue();
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            sendMessage("play error " +  ( item != null  ? item.getToken() : "null") );
            avsQueue.remove(item);
            audioPlayer.stop();
            return false;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            sendMessage("play data_error " + e.getMessage());
            reset(false);
        }
    };

    private PlaybackAudioPlayer.Callback playbackAudioPlayerCallback = new PlaybackAudioPlayer.Callback() {
        @Override
        public void playerPrepared(AvsItem pendingItem) {
            playing = true;
            sendMessage("play content");
        }

        @Override
        public void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent) {
//            Log.d(TAG, "play_progress percent " + percent);
        }

        @Override
        public void itemComplete(AvsItem completedItem) {
            playing = false;
            sendMessage("play content complete");
            avsQueue.remove(completedItem);
            alexaManager.finishedPlayback();
            playbackAudioPlayer.stop();
            checkQueue();
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            Log.d(TAG, "playback_error " + ( item != null  ? item.getToken() : "null") );
            avsQueue.remove(item);
            playbackAudioPlayer.stop();
            alexaManager.finishedPlayback();
            return false;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            Log.d(TAG, "playback_data_error " + e.getMessage());
            reset(false);
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
            sendMessage( "Voice failure " + error.getMessage());
            if( listening ) {
                stopListening();
            }
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
    private synchronized void checkQueue() {

        //if we're out of things, hang up the phone and move on
        if (avsQueue.size() == 0) {
            sendMessage("queue empty");
            alexaManager.updateInitiator(null);
            playbackAudioPlayer.play();
            return;
        }

        AvsItem current = avsQueue.get(0);
        sendMessage("current queue item " + current.getClass().getName());

        alexaManager.updateInitiator(current);

        if( current instanceof AvsResponseException) {
            stopAlexaAudio();
            stopPlaybackAudio();
            avsQueue.clear();
        } else if ( current instanceof AvsStopCaptureItem) {
            Event.EventWrapper event = alexaManager.activeRecognizeEvent;
            AvsStopCaptureItem asi = (AvsStopCaptureItem) current;
            if( event != null && event.getEvent().getHeader().getDialogRequestId().equals(asi.dialogRequestId) ) {
                recorder.stop();
            }
            avsQueue.remove(current);
        } else if (current instanceof AvsPlayRemoteItem) {
            //play a URL
            stopAlexaAudio();
            alexaManager.updatePlaybackStateEvent(current, "PLAYING");
            if (!playbackAudioPlayer.isPlaying() || playbackAudioPlayer.getCurrentItem() != current ) {
                AvsItem currentItem = playbackAudioPlayer.getCurrentItem();
                if( currentItem != null ) {
                    avsQueue.remove(currentItem);
                }
                playbackAudioPlayer.playItem((AvsPlayRemoteItem) current);
            } else {
                playbackAudioPlayer.play();
            }
        } else if (current instanceof AvsPlayContentItem) {
            //play a URL
            stopAlexaAudio();
            alexaManager.updatePlaybackStateEvent(current, "PLAYING");
            if (!playbackAudioPlayer.isPlaying() || playbackAudioPlayer.getCurrentItem() != current) {
                AvsItem currentItem = playbackAudioPlayer.getCurrentItem();
                if( currentItem != null ) {
                    avsQueue.remove(currentItem);
                }
                playbackAudioPlayer.playItem((AvsPlayContentItem) current);
            } else {
                playbackAudioPlayer.play();
            }
        } else if (current instanceof AvsSpeakItem) {
            //play a sound file
            playbackAudioPlayer.pause();
            if (!audioPlayer.isPlaying() || audioPlayer.getCurrentItem() != current) {
                AvsItem currentItem = audioPlayer.getCurrentItem();
                if( currentItem != null ) {
                    avsQueue.remove(currentItem);
                }
                audioPlayer.playItem((AvsSpeakItem) current);
            } else {
                audioPlayer.play();
            }
        } else if (current instanceof AvsStopItem) {
            //stop our play
            stopAlexaAudio();
            stopPlaybackAudio();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceAllItem) {
            stopAlexaAudio();
            stopPlaybackAudio();
            avsQueue.remove(current);
        } else if (current instanceof AvsReplaceEnqueuedItem) {
            avsQueue.remove(current);
        } else if (current instanceof AvsExpectSpeechItem) {
            //listen for user input
            stopAlexaAudio();
            playbackAudioPlayer.pause();
            avsQueue.remove(current);
//            avsQueue.clear();
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
        sendMessage("Alexa start listening");
        listening = true;
        Tool.setLight(0xFFFFFF);
        audioCue.playStartSoundAndSleep();
        recorder.consumeRecordingAndTruncate();
        listeningStartTime = System.currentTimeMillis();
        stopAlexaAudio();
        playbackAudioPlayer.pause();
        alexaManager.sendAudioRequest(requestBody, requestCallback);
    }

    //tear down our recorder
    private void stopListening(){
        sendMessage("stop listening " + avsQueue.size());
        listening = false;
        Tool.setLight(0);
        enteredIdle = true;
        audioCue.playStopSound();
        restartRecorder();
    }

    private void restartRecorder() {
        if( recorder != null ) {
            sendMessage("restart recorder");
            recorder.stop();
            recorder.release();
        }
        recorder = new RawAudioRecorder(AUDIO_RATE);
        recorder.start();
    }
    private DataRequestBody requestBody = new DataRequestBody() {
        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            while (true) {
                long passedTime =  System.currentTimeMillis() - listeningStartTime;
                if( recorder.getState() == AudioRecorder.State.ERROR ) {
                    Log.d(TAG, "recorder error");
                    break;
                }
                if( recorder.getState() == AudioRecorder.State.STOPPED ) {
                    Log.d(TAG, "recorder stopped");
                    break;
                }
                if( passedTime > 30000 ) {
                    Log.d(TAG, "recorder timeout");
                    break;
                }
                if( recordPausing > 0 && recorder.isPausing() && passedTime > recordPausing ) {
                    Log.d(TAG, "recorder isPausing");
                    break;
                }
//                final float rmsdb = recorder.getRmsdb();
//                Log.d(TAG, " passed Time " + passedTime + " ; rmsdb " + rmsdb);
                // update UI rmsdb level
                if(sink != null && recorder != null) {
                    sink.write(recorder.consumeRecording());
                }
                SystemClock.sleep(25);
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
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        //get our AlexaManager instance for convenience
        recorder = new RawAudioRecorder(AUDIO_RATE);
        alexaManager = AlexaManager.getInstance(this);
        audioCue = new AudioCue(this);
        audioPlayer = AlexaAudioPlayer.getInstance(this);
        playbackAudioPlayer = PlaybackAudioPlayer.getInstance(this);
        audioPlayer.addCallback(alexaAudioPlayerCallback);
        playbackAudioPlayer.addCallback(playbackAudioPlayerCallback);
        audioPlayer.context = this;
        EventBus.getDefault().register(this);
        SnowboyUtils.copyAssets(this);
        config();
        instance = this;
    }

    public void config() {
        if( snowboyDetect != null ) {
            snowboyDetect.delete();
            snowboyDetect = null;
        }
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
        String modelName = preferences.getString("snowboy_model", getString(R.string.default_snowboy_model));
        String sensitivity = preferences.getString("sensitivity", getString(R.string.default_sensitivity));
        File model = new File(snowboyDirectory, modelName);
        File common = new File(snowboyDirectory, "common.res");
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model.getAbsolutePath());
        recordPausing = preferences.getInt("recordPausing",0);
        //sensitivity: [0,1], Increasing the value lead to better detection rate, but also higher false alarm rate.
        sendMessage("Set snowboy model to " + modelName + ", sensitivity to " + sensitivity);
        snowboyDetect.setSensitivity(sensitivity);
        snowboyDetect.applyFrontend(true);
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
        running = false;
        EventBus.getDefault().unregister(this);
        Log.d(TAG, "onDestroy");
        stopForeground(true);
        snowboyDetect.delete();
        instance = null;
        super.onDestroy();
        Intent intent = new Intent("me.sagan.r1helper.start");
        sendBroadcast(intent);
    }

    private void checkLogin() {
        alexaManager.checkLoggedIn(new  AsyncCallback<Boolean, Throwable>() {
            @Override
            public void start() {
            }
            @Override
            public void success(Boolean result) {
                isLogin = result ? 2 : 1;
                sendMessage("checkLogin result " + result);
            }
            @Override
            public void failure(Throwable error) {
                sendMessage("checkLogin failure " + error.getMessage());
            }
            @Override
            public void complete() {

            }
        });
    }

    private void go() {
        checkLogin();

        listening = false;
        playing = false;
        enteredIdle = true;
        snowboyDetect.reset();
        recorder.start();
        while( true ) {
            long t = System.currentTimeMillis();
            if( t - lastTrimCacheTime > 1800000 ) {
                AlexaAudioPlayer.trimCache(this);
                lastTrimCacheTime = t;
            }
            try {
                int result = 0;
                if( !listening ) {
                    if( enteredIdle ) {
                        enteredIdle = false;
                        sendMessage("detecting");
                    }
                    byte[] recordButes = recorder.consumeRecordingAndTruncate();
                    short[] shortArray = new short[recordButes.length / 2];
                    ByteBuffer.wrap(recordButes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
                    result = snowboyDetect.runDetection(shortArray, shortArray.length);
                }
                if (result > 0) {
                    sendMessage("hotword detected");
                    if( isLogin == 2 ) {
                        startListening();
                    } else {
                        sendMessage("Please login to use alexa");
                    }
                }
                SystemClock.sleep(50);
            } catch(Exception e) {
                SystemClock.sleep(5000);
                sendMessage("error" +  e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void sendMessage(String content) {
        Log.d(TAG, content);
        Tool.addLog(this, content);
    }

    void stopAlexaAudio() {
        AvsItem item = audioPlayer.getCurrentItem();
        if( item != null ) {
            avsQueue.remove(item);
        }
        audioPlayer.stop();
    }

    void stopPlaybackAudio() {
        AvsItem item = playbackAudioPlayer.getCurrentItem();
        if( item != null ) {
            avsQueue.remove(item);
        }
        playbackAudioPlayer.stop();
    }

}
