/*
 * Copyright (c) Jan, 2016.
 * Piyush Shrivastava
 * This code is copyrighted and cannot be redistributed, modified, or sold as it is without the
 *  authority of owner.
 */

package com.bytezap.wobble.utils;

import android.app.Activity;
import android.app.VoiceInteractor;

public class VoiceNotifier {

    public static void notifySuccess(Activity activity, String message) {
        if (CommonUtils.isMOrLater()) {
            VoiceInteractor interactor = activity.getVoiceInteractor();
            if (interactor != null)  {
                final VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(message);
                interactor.submitRequest(new VoiceInteractor.CompleteVoiceRequest(prompt, null));
            }
        }
    }

    public static void notifyFailure(Activity activity, String message) {
        if (CommonUtils.isMOrLater()) {
            VoiceInteractor interactor = activity.getVoiceInteractor();
            if (interactor != null)  {
                final VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(message);
                interactor.submitRequest(new VoiceInteractor.AbortVoiceRequest(prompt, null));
            }
        }
    }
}
