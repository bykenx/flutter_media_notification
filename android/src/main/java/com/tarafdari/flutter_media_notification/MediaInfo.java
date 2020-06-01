package com.tarafdari.flutter_media_notification;

import android.graphics.Bitmap;

class MediaInfo {

    String appName;
    int appIcon;
    String title;
    String author;
    Bitmap cover;
    int position;
    int duration;
    boolean isPlaying;
    double rate;

    private MediaInfo(String appName, int appIcon, String title, String author, Bitmap cover, int position, int duration, boolean isPlaying, double rate) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.title = title;
        this.author = author;
        this.cover = cover;
        this.position = position;
        this.duration = duration;
        this.isPlaying = isPlaying;
        this.rate = rate;
    }

    static MediaInfo defaults() {
        return new MediaInfo("", 0, "", "", null, 0, 0, true, 1.0);
    }
}
