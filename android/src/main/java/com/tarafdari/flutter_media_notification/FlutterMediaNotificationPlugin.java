package com.tarafdari.flutter_media_notification;


import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

/** FlutterMediaNotificationPlugin */
public class FlutterMediaNotificationPlugin implements MethodCallHandler {
  private static Registrar registrar;
  private static MethodChannel channel;

  private FlutterMediaNotificationPlugin(Registrar r) {
    registrar = r;
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    FlutterMediaNotificationPlugin.channel = new MethodChannel(registrar.messenger(), "flutter_media_notification");
    FlutterMediaNotificationPlugin.channel.setMethodCallHandler(new FlutterMediaNotificationPlugin(registrar));
  }

  @Override
  public void onMethodCall(MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "showNotification":
        final boolean isPlaying = Utils.getDefault((Boolean) call.argument("isPlaying"), true);
        final String title = Utils.getDefault((String) call.argument("title"), "");
        final String author = Utils.getDefault((String) call.argument("author"), "");
        final String cover = call.argument("cover");
        final int position = Utils.getDefault((int) call.argument("position"), 0);
        final int duration = Utils.getDefault((int) call.argument("duration"), 0);
        final double rate = Utils.getDefault((Double) call.argument("rate"), 1.0);
        showNotification(isPlaying, title, author, cover, position, duration, rate);
        result.success(null);
        break;
      case "hideNotification":
        hideNotification();
        result.success(null);
        break;
      default:
        result.notImplemented();
    }
  }

  static void callEvent(String event) {
    FlutterMediaNotificationPlugin.channel.invokeMethod(event, null);
  }

  private static void showNotification(boolean isPlaying, String title, String author, String cover, int position, int duration, double rate) {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    serviceIntent.setAction(NotificationPanel.ACTION_SHOW_NOTIFICATION);
    PackageManager pm = registrar.context().getPackageManager();
    try {
      PackageInfo info = pm.getPackageInfo(registrar.context().getPackageName(), 0);
      serviceIntent.putExtra("appName", info.applicationInfo.loadLabel(pm).toString());
    } catch (PackageManager.NameNotFoundException e) {
      serviceIntent.putExtra("appName", registrar.context().getApplicationInfo().name);
    }
    serviceIntent.putExtra("appName", registrar.context().getApplicationInfo().name);
    serviceIntent.putExtra("appIcon", registrar.context().getApplicationInfo().icon);
    serviceIntent.putExtra("isPlaying", isPlaying);
    serviceIntent.putExtra("title", title);
    serviceIntent.putExtra("author", author);
    serviceIntent.putExtra("cover", cover);
    serviceIntent.putExtra("position", Math.min(position, duration));
    serviceIntent.putExtra("duration", duration);
    serviceIntent.putExtra("rate", rate);
    registrar.context().startService(serviceIntent);
  }

  static void hideNotification() {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    registrar.context().stopService(serviceIntent);
  }

  static void togglePlaying() {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    serviceIntent.setAction(NotificationPanel.ACTION_TOGGLE_PLAYING);
    registrar.context().startService(serviceIntent);
  }
}