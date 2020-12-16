package com.willblaschko.android.alexa.interfaces.speechrecognizer;

import com.willblaschko.android.alexa.interfaces.AvsItem;

/**
 * Created by will on 4/9/2017.
 */

public class AvsStopCaptureItem extends AvsItem {
    public String dialogRequestId;
    public AvsStopCaptureItem(String token, String dialogRequestId) {
        super(token);
        this.dialogRequestId = dialogRequestId;
    }
}
