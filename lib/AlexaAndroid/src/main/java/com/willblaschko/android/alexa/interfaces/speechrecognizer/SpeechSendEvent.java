package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.SendEvent;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Abstract class to extend {@link SendEvent} to automatically add the RequestBody with the correct type
 * and name, as well as the SpeechRecognizer {@link Event}
 *
 * @author will on 5/21/2016.
 */
public abstract class SpeechSendEvent extends SendEvent {

    @NotNull
    @Override
    protected String getEvent() {
        return Event.getSpeechRecognizerEvent();
    }

    @NotNull
    @Override
    protected Event.EventWrapper getEventWrapper(List<Event> context, Event.Initiator initiator) {
        return Event.getSpeechRecognizerEventWrapper(context, initiator);
    }

    @Override
    protected void addFormDataParts(MultipartBody.Builder builder){
        builder.addFormDataPart("audio", "speech.wav", getRequestBody());
    }

    @NotNull
    protected abstract RequestBody getRequestBody();
}
