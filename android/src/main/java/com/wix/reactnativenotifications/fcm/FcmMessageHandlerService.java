package com.wix.reactnativenotifications.fcm;

import android.util.Log;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.Context;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.wix.reactnativenotifications.core.notifications.NotificationProps;
import com.wix.reactnativenotifications.core.notifications.RemoteNotification;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

public class FcmMessageHandlerService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d(LOGTAG, "New message from firebase");
        final NotificationProps notificationProps = NotificationProps.fromRemoteMessage(this, message);
        new RemoteNotification(this, notificationProps).onReceived();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        Log.e("screen on.................................", ""+isScreenOn);
        if(isScreenOn==false){
            WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MyLock");
            wl.acquire(10000);
        }
    }
}
