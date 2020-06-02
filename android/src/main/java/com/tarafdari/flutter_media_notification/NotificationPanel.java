package com.tarafdari.flutter_media_notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class NotificationPanel extends Service {
    public static int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "flutter_media_notification";

    public Timer timer;

    private MediaInfo info;

    private NotificationCompat.Builder builder;

    private RemoteViews remoteViews;

    private AsyncTask<String, Void, Bitmap> coverDownloadTask;

    static final String ACTION_SHOW_NOTIFICATION = "show_notification";

    static final String ACTION_TOGGLE_PLAYING = "update_toggle_playing";

    @Override
    public void onCreate() {
        super.onCreate();
        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        remoteViews = new RemoteViews(getPackageName(), R.layout.media_notification_layout);
        info = MediaInfo.defaults();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case ACTION_SHOW_NOTIFICATION:
                showNotification(intent);
                break;
            case ACTION_TOGGLE_PLAYING:
                togglePlaying(intent);
                break;
        }

        // 设置数据
        setContentViewData(remoteViews, info);

        startForeground(NOTIFICATION_ID, builder.build());

        if (info.isPlaying) {
            startTimer();
        } else {
            clearTimer();
        }

        if (!info.isPlaying) {
            stopForeground(false);
        }

        return START_NOT_STICKY;
    }

    void showNotification(Intent intent) {
        this.updateMediaInfoFromIntent(intent);

        builder
                .setContentTitle(info.appName)
                .setSmallIcon(info.appIcon)
                .setCustomContentView(remoteViews);

        setActions(remoteViews);

        // 点击通知栏
        Intent selectIntent = new Intent(this, NotificationReturnSlot.class).setAction("select");
        PendingIntent selectPendingIntent = PendingIntent.getBroadcast(this, 0, selectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(selectPendingIntent);
    }

    void togglePlaying(Intent intent) {
        info.isPlaying = !info.isPlaying;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("flutter_media_notification");
            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void setContentViewData(RemoteViews remoteViews, MediaInfo info) {
        if (info.cover != null) {
            remoteViews.setImageViewBitmap(R.id.mfn_cover, info.cover);
        } else {
            remoteViews.setImageViewResource(R.id.mfn_cover, R.drawable.default_cover);
        }
        remoteViews.setTextViewText(R.id.mfn_title, info.title);
        remoteViews.setTextViewText(R.id.mfn_author, info.author);
        remoteViews.setTextViewText(R.id.mfn_position, getFormatTime(info.position));
        remoteViews.setTextViewText(R.id.mfn_duration, getFormatTime(info.duration));
        remoteViews.setViewVisibility(R.id.mfn_play_btn, info.isPlaying ? View.INVISIBLE : View.VISIBLE);
        remoteViews.setViewVisibility(R.id.mfn_pause_btn, info.isPlaying ? View.VISIBLE : View.INVISIBLE);
    }

    private void setActions(RemoteViews views) {
        // 播放 & 暂停
        Intent playIntent = new Intent(this, NotificationReturnSlot.class).setAction("play");
        PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.mfn_play_btn, pendingPlayIntent);

        Intent pauseIntent = new Intent(this, NotificationReturnSlot.class).setAction("pause");
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.mfn_pause_btn, pendingPauseIntent);

        Intent closeIntent = new Intent(this, NotificationReturnSlot.class).setAction("close");
        PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0, closeIntent, PendingIntent.FLAG_ONE_SHOT);
        views.setOnClickPendingIntent(R.id.mnf_close_btn, pendingCloseIntent);
    }

    private String getFormatTime(int time) {
        int remain = time / 60;
        int second = time % 60;
        return String.format(Locale.ENGLISH, "%02d", remain) + ":" + String.format(Locale.ENGLISH, "%02d", second);
    }

    private void updateMediaInfoFromIntent(Intent intent) {
        if (intent.hasExtra("appName")) {
            info.appName = intent.getStringExtra("appName");
        }
        if (intent.hasExtra("appIcon")) {
            info.appIcon = intent.getIntExtra("appIcon", 0);
        }
        if (intent.hasExtra("title")) {
            info.title = intent.getStringExtra("title");
        }
        if (intent.hasExtra("author")) {
            info.author = intent.getStringExtra("author");
        }
        if (intent.hasExtra("cover")) {
            String coverUrlString = intent.getStringExtra("cover");
            if(coverUrlString != null) {
                if (coverDownloadTask != null) {
                    coverDownloadTask.cancel(false);
                }
                coverDownloadTask = new BitmapTask(this).execute(intent.getStringExtra("cover"));
            }
        }
        if (intent.hasExtra("position")) {
            info.position = intent.getIntExtra("position", 0);
        }
        if (intent.hasExtra("duration")) {
            info.duration = intent.getIntExtra("duration", 0);
        }
        if (intent.hasExtra("isPlaying")) {
            info.isPlaying = intent.getBooleanExtra("isPlaying", true);
        }
        if (intent.hasExtra("rate")) {
            info.rate = intent.getDoubleExtra("rate", 1.0);
        }
    }

    synchronized private void startTimer() {
        clearTimer();
        timer = new Timer("update_progress_timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (info.position >= info.duration) {
                    clearTimer();
                    return;
                }
                info.position += info.rate;
                setContentViewData(remoteViews, info);

                startForeground(NOTIFICATION_ID, builder.build());

                if(!info.isPlaying) {
                    stopForeground(false);
                }
            }
        }, 0, 1000);
    }

    synchronized private void clearTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class BitmapTask extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<NotificationPanel> contextReference;

        BitmapTask(NotificationPanel context) {
            contextReference = new WeakReference<>(context);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                InputStream is = url.openConnection().getInputStream();
                return BitmapFactory.decodeStream(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap cover) {
            NotificationPanel context = contextReference.get();
            context.info.cover = cover;
            context.coverDownloadTask = null;
        }
    }
}

