package com.devftt.turtleneckalert;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class RealService extends Service {
    private Thread mainThread;
    public static Context mContext;
    public static Intent serviceIntent = null;
    public static boolean isStopTimer = false;
    public static final String NOTICE_ON_OFF_KEY = "NOTICE_BOOL";
    public static final String NOTICE_TIME_S_KEY = "NOTICE_TIME_S_INT";
    public static final String NOTICE_TIME_M_KEY = "NOTICE_TIME_M_INT";
    public static final String NOTICE_TIME_H_KEY = "NOTICE_TIME_H_INT";

    public RealService() {
        mContext = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceIntent = intent;

        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean run = true;
                while (run) {
                    try {
                        TimerManager(Integer.parseInt(PreferenceManager.getString(mContext, NOTICE_TIME_H_KEY)), Integer.parseInt(PreferenceManager.getString(mContext, NOTICE_TIME_M_KEY)), Integer.parseInt(PreferenceManager.getString(mContext, NOTICE_TIME_S_KEY)));
                    }catch (InterruptedException ex) {
                        run = false;
                    }
                }
            }
        });
        mainThread.start();

        return START_NOT_STICKY;
    }

    public void TimerManager(int h, int m, int s) throws InterruptedException {
        for (int h_ = 0; h_ < h; h_++) {

            if (isStopTimer) {
                isStopTimer = false;
                return;
            }

            for (int temp = 0; temp < 60; temp++) {

                if (isStopTimer) {
                    isStopTimer = false;
                    return;
                }

                for (int temp2 = 0; temp2 < 60; temp2++) {

                    if (isStopTimer) {
                        isStopTimer = false;
                        return;
                    }

                    Thread.sleep(1000);
                }
            }
        }


        for (int m_ = 0; m_ < m; m_++) {

            if (isStopTimer) {
                isStopTimer = false;
                return;
            }

            for (int temp = 0; temp < 60; temp++) {

                if (isStopTimer) {
                    isStopTimer = false;
                    return;
                }

                Thread.sleep(1000);
            }
        }

        for (int s_ = 0; s_ < s; s_++) {
            if (isStopTimer) {
                isStopTimer = false;
                return;
            }

            Thread.sleep(1000);
        }

        if (PreferenceManager.getBoolean(mContext, NOTICE_ON_OFF_KEY)) {
            sendNotification("까먹으신 거 아니시죠?");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        setAlarmTimer();
        Thread.currentThread().interrupt();

        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void showToast(final Application application, final String msg) {
        Handler h = new Handler(application.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(application, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void setAlarmTimer() {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, 1);
        Intent intent = new Intent(this, AlarmRecever.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0,intent,0);

        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
    }

    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = "fcm_default_channel";//getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher_tn)//drawable.splash)
                        .setContentTitle("습관 제거")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,"Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
