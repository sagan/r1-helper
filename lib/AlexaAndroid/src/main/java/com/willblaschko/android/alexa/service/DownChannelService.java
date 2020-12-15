package com.willblaschko.android.alexa.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.TokenManager;
import com.willblaschko.android.alexa.callbacks.ImplAsyncCallback;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.response.ResponseParser;
import com.willblaschko.android.alexa.system.AndroidSystemHandler;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MultipartReader;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * @author will on 4/27/2016.
 */
public class DownChannelService extends Service {

    private static final String TAG = "DownChannelService";

    private AlexaManager alexaManager;
    private Call currentCall;
    private AndroidSystemHandler handler;
    private Handler runnableHandler;
    private Runnable pingRunnable;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Launched");
        alexaManager = AlexaManager.getInstance(this);
        handler = AndroidSystemHandler.getInstance(this);

        runnableHandler = new Handler(Looper.getMainLooper());
        pingRunnable = new Runnable() {
            @Override
            public void run() {
                TokenManager.getAccessToken(alexaManager.getAuthorizationManager().getAmazonAuthorizationManager(), DownChannelService.this, new TokenManager.TokenCallback() {
                    @Override
                    public void onSuccess(String token) {

                        Log.i(TAG, "Sending heartbeat");
                        final Request request = new Request.Builder()
                                .url(alexaManager.getPingUrl())
                                .get()
                                .addHeader("Authorization", "Bearer " + token)
                                .build();

                        ClientUtil.getTLS12OkHttpClient()
                                .newCall(request)
                                .enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {

                                    }

                                    @Override
                                    public void onResponse(Call call, Response response) throws IOException {
                                        runnableHandler.postDelayed(pingRunnable, 4 * 60 * 1000);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Throwable e) {

                    }
                });
            }
        };
        
        openDownChannel();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if(currentCall != null){
            currentCall.cancel();
        }
        runnableHandler.removeCallbacks(pingRunnable);
    }


    private void openDownChannel(){
        Log.d(TAG, "openDownChannel");
        TokenManager.getAccessToken(alexaManager.getAuthorizationManager().getAmazonAuthorizationManager(), DownChannelService.this, new TokenManager.TokenCallback() {
            @Override
            public void onSuccess(String token) {

                OkHttpClient downChannelClient = ClientUtil.getTLS12OkHttpClient();

                final Request request = new Request.Builder()
                        .url(alexaManager.getDirectivesUrl())
                        .get()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                currentCall = downChannelClient.newCall(request);
                currentCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "downChannel onFailure " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.d(TAG, "downchannel response start " + response.header("content-type"));
                        alexaManager.sendEvent(Event.getSynchronizeStateEvent(), new ImplAsyncCallback<AvsResponse, Exception>() {
                            @Override
                            public void success(AvsResponse result) {
                                handler.handleItems(result);
                                runnableHandler.post(pingRunnable);
                            }
                        });

                        // HTTP/2 half-open long connection
                        // Content-Type: multipart/related; boundary=------abcde123; type=application/json
                        try {
                            ResponseBody body = response.body();
//                            Log.d(TAG, ":--" + body.contentType().toString());
                            MultipartReader multipartReader = new MultipartReader(body.source(), "------abcde123");
                            while(true) {
                                Log.d(TAG, "reading downchannel stream");
                                MultipartReader.Part part = multipartReader.nextPart();
                                if( part == null ) {
                                    break;
                                }
                                BufferedSource bufferedSource = part.body();
                                String line = bufferedSource.readUtf8Line();
                                while (!bufferedSource.exhausted()) {
                                    line += bufferedSource.readUtf8();
                                }
                                Log.d(TAG, "--- "  + line);
                                try {
                                    Directive directive = ResponseParser.getDirective(line);
                                    handler.handleDirective(directive);
                                    //surface to our UI if it's up
                                    try {
                                        AvsItem item = ResponseParser.parseDirective(directive);
                                        EventBus.getDefault().post(item);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Bad line");
                                }
                            }
                        } catch(Exception e) {
                            Log.d(TAG, "down channel error " + e.getMessage());
                        }
                        Log.d(TAG, "down channel end");
                        SystemClock.sleep(5000);
                        openDownChannel();
                    }
                });

            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace();
            }
        });
    }

}
