package com.tarafdari.flutter_media_notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class NotificationReturnSlot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case "prev":
                FlutterMediaNotificationPlugin.callEvent("prev");
                break;
            case "next":
                FlutterMediaNotificationPlugin.callEvent("next");
                break;
            case "play":
                FlutterMediaNotificationPlugin.callEvent("play");
                FlutterMediaNotificationPlugin.togglePlaying();
                break;
            case "pause":
                FlutterMediaNotificationPlugin.callEvent("pause");
                FlutterMediaNotificationPlugin.togglePlaying();
                break;
            case "close":
                FlutterMediaNotificationPlugin.hideNotification();
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                break;
            case "select":
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                String packageName = context.getPackageName();
                PackageManager pm = context.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                context.startActivity(launchIntent);

                FlutterMediaNotificationPlugin.callEvent("select");
                break;
        }
    }
}

