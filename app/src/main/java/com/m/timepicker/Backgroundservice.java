package com.m.timepicker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.ContentHandler;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class Backgroundservice extends Service implements MediaPlayer.OnPreparedListener {
    int delay1, state, index = 0;
    private final String CHANNEL_ID = "Primary Notifications";
    private final int Notification_id = 001;
    public boolean isr = false;
    static int lol;
    List<ScheduleInform> scheduleinfolist;
    PowerManager.WakeLock wakelock;
    Intent intent;
    private boolean isRunning;
    private Context context;
    private Thread backgroundThread;
    private CountDownTimer countDownTimer;
    MediaPlayer mediaPlayer;



    public void Timethread(int d, Context c, int st) {
        isr = false;
        final int delay, stid;
        final Context context;
        context = c;
        delay = d;
        lol = d;
        stid = st;
       // Log.e("this", "before thread" + delay);
        final AudioManager ad = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (scheduleinfolist.get(index).getVol() == 2) {

            ad.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

        }
        else {
            ad.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        countDownTimer = new CountDownTimer(delay, 1000) {
            @Override
            public void onTick(long l) {
                lol = (int) l;
                savelstvalue(lol);
                isr = true;
               // Log.e("lol", l + "<->" + lol+ " delay= "+delay);
            }

            @Override
            public void onFinish() {

                countDownTimer.cancel();
                isr = false;
                //  Log.e("this", "after thread");

                ad.setRingerMode(AudioManager.RINGER_MODE_NORMAL);//norm(ad,chkk,intent);
                playMusic(context);
                notifi(context);
                stopSelf(stid); //service cancelling
            }
        }.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // Log.e("wakelock","wakelock acquire1");
        this.context = this;
        this.isRunning = false;
        loadData(context);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getCanonicalName());
        wakelock.acquire();

    }

    @Override
    public void onDestroy() {
        this.isRunning = false;
        if (isr == true) {
            countDownTimer.cancel();
        }
        if(wakelock.isHeld()){
            wakelock.release();
           // Log.e("wakelock", "wakelock release 2");
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.e("asdf","onstart");
        this.intent = intent;
        if (intent != null) {
            index = intent.getIntExtra("value", 0);
            delay1 = scheduleinfolist.get(index).getDelay();
            state = scheduleinfolist.get(index).getVol();
            savelstvalue(delay1);
            delay1 = getlastvalue();
        } else {
            delay1 = getlastvalue();
        }


        if (!isRunning) {
            //Toast.makeText(context, "Kaj korsay reh", Toast.LENGTH_SHORT).show();
            isRunning = true;
//            Timethread timethread = new Timethread(delay1, context, startId);
//            timethread.start();
            Timethread(delay1, context, startId);
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        if (isr == true) {
            countDownTimer.cancel();
        }
       // Toast.makeText(context, "ontaskcommand working", Toast.LENGTH_SHORT).show();

    }

    public void loadData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Infos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("lists", null);
        Type type = new TypeToken<ArrayList<ScheduleInform>>() {
        }.getType();
        scheduleinfolist = gson.fromJson(json, type);
        if (scheduleinfolist == null) {
            scheduleinfolist = new ArrayList<>();
        }

        // Log.e("context","context "+s);
    }

    public void savelstvalue(int d) {//service restart oilay abar ou vaue takia restart oibo
        SharedPreferences sharedPreferences = this.getSharedPreferences("Lastvalue", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("last", d);
        editor.putInt("index", index);
        editor.commit();

    }

    public int getlastvalue() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("Lastvalue", Context.MODE_PRIVATE);
        int t = sharedPreferences.getInt("last", 0);
        index = sharedPreferences.getInt("index", 0);
        //Log.e("t=",""+t);
        return t;
    }




    private void playMusic(Context context) {
        mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.successsound);
       // mediaPlayer.start();
        mediaPlayer.setOnPreparedListener(this);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.stop();
            }
        });

    }
    private void notifi(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationChannel(context, index);
        else {
            //CREATE INTENT
            Intent intent = new Intent(context.getApplicationContext(), ShowEvents.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //END OF INTENT

            //CREATE A PENDING INTENT

            PendingIntent pendingIntent = PendingIntent.getActivities(context.getApplicationContext(), 0, new Intent[]{intent}, PendingIntent.FLAG_ONE_SHOT);
            //END OF PENDING INTENT
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext(), CHANNEL_ID);
            builder.setSmallIcon(R.drawable.smallicon);
            String s = "Your phone turned into normal mode";

            builder.setContentTitle("Silencio");
            builder.setContentText(s);
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            builder.setAutoCancel(true);
            builder.setContentIntent(pendingIntent);


            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context.getApplicationContext());
            notificationManagerCompat.notify(Notification_id, builder.build());
        }
    }

    private void notificationChannel(Context context, int index) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = "Silencio";
            String s = "";
            s = "Your phone turned into normal mode";

            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            // create notification channel object
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationChannel.setDescription(s);

            //create notification manager object which hold notification channel

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel); // create notification channel through manager object
        }

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }
}
