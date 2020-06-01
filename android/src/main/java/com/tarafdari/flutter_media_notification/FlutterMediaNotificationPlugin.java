package com.tarafdari.flutter_media_notification;


import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.content.Intent;
import android.net.Uri;

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
        final String title = Utils.getDefault((String) call.argument("title"), "");
        final String author = Utils.getDefault((String) call.argument("author"), "");
        final boolean isPlaying = Utils.getDefault((Boolean) call.argument("isPlaying"), true);
        final String cover = call.argument("cover");
        showNotification(title, author, isPlaying, cover);
        result.success(null);
        break;
      case "hideNotification":
        hideNotification();
        result.success(null);
        break;
      case "updatePlaybackInfo":
        final int position = Utils.getDefault((int) call.argument("position"), 0);
        final int duration = Utils.getDefault((int) call.argument("duration"), 0);
        final double rate = Utils.getDefault((Double) call.argument("rate"), 1D);
        updatePlaybackInfo(position, duration, rate);
        result.success(null);
        break;
      default:
        result.notImplemented();
    }
  }

  static void callEvent(String event) {

    FlutterMediaNotificationPlugin.channel.invokeMethod(event, null, new Result() {
      @Override
      public void success(Object o) {
        // this will be called with o = "some string"
      }

      @Override
      public void error(String s, String s1, Object o) {}

      @Override
      public void notImplemented() {}
    });
  }

  private static void showNotification(String title, String author, boolean play, String cover) {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    serviceIntent.setAction(NotificationPanel.ACTION_SHOW_NOTIFICATION);
    serviceIntent.putExtra("appName", registrar.context().getApplicationInfo().name);
    serviceIntent.putExtra("appIcon", registrar.context().getApplicationInfo().icon);
    serviceIntent.putExtra("title", title);
    serviceIntent.putExtra("author", author);
    serviceIntent.putExtra("isPlaying", play);
    serviceIntent.putExtra("cover", cover);
    if (cover != null && !"".equals(cover.trim())) {
      serviceIntent.setData(Uri.parse(cover));
    }
    registrar.context().startService(serviceIntent);
  }

  static void hideNotification() {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    registrar.context().stopService(serviceIntent);
  }

  private static void updatePlaybackInfo(int position, int duration, double rate) {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    serviceIntent.setAction(NotificationPanel.ACTION_UPDATE_PLAYBACK_INFO);
    serviceIntent.putExtra("position", position);
    serviceIntent.putExtra("duration", duration);
    serviceIntent.putExtra("rate", rate);
    registrar.context().startService(serviceIntent);
  }

  static void togglePlaying() {
    Intent serviceIntent = new Intent(registrar.context(), NotificationPanel.class);
    serviceIntent.setAction(NotificationPanel.ACTION_TOGGLE_PLAYING);
    registrar.context().startService(serviceIntent);
  }
}