package com.wix.reactnativenotifications.fcm;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.wix.reactnativenotifications.core.JsIOHelper;

import static com.wix.reactnativenotifications.Defs.LOGTAG;
import static com.wix.reactnativenotifications.Defs.TOKEN_RECEIVED_EVENT_NAME;

public class FcmTokenBridge {

    private final JsIOHelper mJsIOHelper;

    protected FcmTokenBridge(JsIOHelper jsIOHelper) {
        mJsIOHelper = jsIOHelper;
    }

    public FcmTokenBridge(Context context) {
        this(new JsIOHelper(context));
    }

    public void refreshToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(LOGTAG, "Failed to refresh FCM token", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    sendReceivedEvent(token);
                });
    }

    public void invalidateToken() {
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(LOGTAG, "Failed to invalidate FCM token", task.getException());
                        return;
                    }

                    Log.i(LOGTAG, "Firebase token invalidated");
                    refreshToken();  // Request a new token
                });
    }

    private void sendReceivedEvent(String token) {
        Log.i(LOGTAG, "Firebase has a new token: token=" + token);
        mJsIOHelper.sendEventToJS(TOKEN_RECEIVED_EVENT_NAME, token);
    }
}
