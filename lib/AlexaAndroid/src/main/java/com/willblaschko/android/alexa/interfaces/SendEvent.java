package com.willblaschko.android.alexa.interfaces;

import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.data.Event;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * An abstract class that supplies a DataOutputStream which is used to send a POST request to the AVS server
 * with a voice data intent, it handles the response with completePost() (called by extending classes)
 */
public abstract class SendEvent {

    private final static String TAG = "SendEvent";

    //the output stream that extending classes will use to pass data to the AVS server
    protected ByteArrayOutputStream mOutputStream = new ByteArrayOutputStream();
    protected AsyncCallback<Void, Exception> mCallback;

    private Call currentCall;

    //OkHttpClient for transfer of data
    Request.Builder mRequestBuilder = new Request.Builder();
    MultipartBody.Builder mBodyBuilder;

    /**
     * Set up all the headers that we need in our OkHttp POST/GET, this prepares the connection for
     * the event or the raw audio that we'll need to pass to the AVS server
     * @param url the URL we're posting to, this is either the default {@link com.willblaschko.android.alexa.data.Directive} or {@link com.willblaschko.android.alexa.data.Event} URL
     * @param accessToken the access token of the user who has given consent to the app
     */
    protected Event.EventWrapper prepareConnection(String url, String accessToken, List<Event> context, Event.Initiator initiator) {

        //set the request URL
        mRequestBuilder.url(url);

        //set our authentication access token header
        mRequestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        Event.EventWrapper event = getEventWrapper(context, initiator);

        mBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", "metadata", RequestBody.create(MediaType.parse("application/json; charset=UTF-8"),
                        event != null ? event.toJson() : getEvent()));

        //reset our output stream
        mOutputStream = new ByteArrayOutputStream();

        return event;
    }

    /**
     * When finished adding voice data to the output, we close it using completePost() and it is sent off to the AVS server
     * and the response is parsed and returned
     * @return AvsResponse with all the data returned from the server
     * @throws IOException if the OkHttp request can't execute
     * @throws AvsException if we can't parse the response body into an {@link AvsResponse} item
     * @throws RuntimeException
     */
    protected Call completePost() throws IOException, AvsException, RuntimeException {
        addFormDataParts(mBodyBuilder);
        mRequestBuilder.post(mBodyBuilder.build());
        return parseResponse();
    }

    protected Call completeGet() throws IOException, AvsException, RuntimeException {
        mRequestBuilder.get();
        return parseResponse();
    }

    protected void cancelCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    private Call parseResponse() throws IOException, AvsException, RuntimeException {

        Request request = mRequestBuilder.build();


        currentCall = ClientUtil.getTLS12OkHttpClient().newCall(request);

        return currentCall;
    }



    /**
     * When override, our extending classes can add their own data to the POST
     * @param builder with audio data
     */
    protected void addFormDataParts(MultipartBody.Builder builder){

    };

    /**
     * Get our JSON {@link com.willblaschko.android.alexa.data.Event} for this call
     * @return the JSON representation of the {@link com.willblaschko.android.alexa.data.Event}
     */
    @NotNull
    protected abstract String getEvent();

    protected abstract Event.EventWrapper getEventWrapper(List<Event> context, Event.Initiator initiator);

}