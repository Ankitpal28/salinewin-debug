package com.salinewin;

import android.app.*;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.*;
import android.os.*;
import android.view.*;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    public static final String ACTION_TOGGLE  = "com.salinewin.TOGGLE";
    public static final String ACTION_HUE_INC = "com.salinewin.HUE_INC";
    public static final String ACTION_HUE_DEC = "com.salinewin.HUE_DEC";
    public static final String ACTION_RESET   = "com.salinewin.RESET";
    public static final String ACTION_STOP    = "com.salinewin.STOP";
    public static final String ACTION_SET_HUE = "com.salinewin.SET_HUE";
    public static final String ACTION_SET_SAT = "com.salinewin.SET_SAT";
    public static final String ACTION_SET_BRI = "com.salinewin.SET_BRI";
    public static final String EXTRA_VALUE    = "value";

    private static final String CHAN_ID  = "salinewin";
    private static final int    NOTIF_ID = 1;

    private WindowManager mWM;
    private OverlayView   mView;
    private Handler       mHandler;
    private final Runnable mRedraw = this::redraw;

    private float mTouchDX, mTouchDY;
    private static final float SWIPE_MIN = 80f;

    @Override
    public void onCreate() {
        super.onCreate();
        mWM      = (WindowManager) getSystemService(WINDOW_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        createChannel();
        startForeground(NOTIF_ID, buildNotif());
        addView();
        beep(880f,60); beep(1320f,60);
        mHandler.postDelayed(mRedraw, 16);
    }

    private void addView() {
        mView = new OverlayView(this);
        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;

        mView.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchDX = e.getRawX(); mTouchDY = e.getRawY();
                    mView.crossX = e.getRawX(); mView.crossY = e.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mView.crossX = e.getRawX(); mView.crossY = e.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    handleGesture(e.getRawX()-mTouchDX, e.getRawY()-mTouchDY);
                    break;
            }
            return true;
        });
        mWM.addView(mView, p);
    }

    private void handleGesture(float dx, float dy) {
        if (Math.abs(dx)<SWIPE_MIN && Math.abs(dy)<SWIPE_MIN) {
            mView.filterOn = !mView.filterOn;
            beep(mView.filterOn?1000f:600f, 50);
            toast(mView.filterOn ? "ON" : "OFF");
        } else if (Math.abs(dx)>Math.abs(dy)) {
            mView.hueShift = (mView.hueShift + (dx>0?15f:-15f) + 360f) % 360f;
            toast("Hue " + (int)mView.hueShift + "°"); beep(750f,30);
        } else if (dy<0) {
            mView.brightness = Math.min(2f, mView.brightness+0.1f);
            toast("Bri " + (int)(mView.brightness*100) + "%");
        } else {
            mView.hueShift=0; mView.saturation=1; mView.brightness=1; mView.filterOn=true;
            toast("Reset"); beep(880f,60);
        }
        updateNotif();
    }

    private void redraw() {
        if (mView!=null) mView.invalidate();
        mHandler.postDelayed(mRedraw, 16);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent==null||intent.getAction()==null) return START_STICKY;
        switch (intent.getAction()) {
            case ACTION_TOGGLE:  mView.filterOn=!mView.filterOn; break;
            case ACTION_HUE_INC: mView.hueShift=(mView.hueShift+15f)%360f; break;
            case ACTION_HUE_DEC: mView.hueShift=(mView.hueShift-15f+360f)%360f; break;
            case ACTION_RESET:
                mView.hueShift=0;mView.saturation=1;mView.brightness=1;mView.filterOn=true; break;
            case ACTION_SET_HUE: mView.hueShift=intent.getFloatExtra(EXTRA_VALUE,0f); break;
            case ACTION_SET_SAT: mView.saturation=intent.getFloatExtra(EXTRA_VALUE,1f); break;
            case ACTION_SET_BRI: mView.brightness=intent.getFloatExtra(EXTRA_VALUE,1f); break;
            case ACTION_STOP:    stopSelf(); return START_NOT_STICKY;
        }
        updateNotif();
        return START_STICKY;
    }

    private void beep(float freq, int ms) {
        new Thread(()->{
            int n=44100*ms/1000; short[] buf=new short[n];
            for(int i=0;i<n;i++){
                double t=(double)i/44100,env=1.0-(double)i/n;
                buf[i]=(short)(28000*env*Math.sin(2*Math.PI*freq*t));
            }
            AudioTrack t=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setSampleRate(44100)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(n*2)
                .setTransferMode(AudioTrack.MODE_STATIC).build();
            t.write(buf,0,n); t.play();
            try{Thread.sleep(ms+20);}catch(InterruptedException ignored){}
            t.release();
        }).start();
    }

    private void toast(String msg) {
        mHandler.post(()->Toast.makeText(this,msg,Toast.LENGTH_SHORT).show());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT>=26) {
            NotificationChannel ch=new NotificationChannel(
                CHAN_ID,getString(R.string.channel_name),NotificationManager.IMPORTANCE_LOW);
            ch.setSound(null,null);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification buildNotif() {
        Intent stopI=new Intent(this,OverlayService.class).setAction(ACTION_STOP);
        Intent togI =new Intent(this,OverlayService.class).setAction(ACTION_TOGGLE);
        PendingIntent piStop=PendingIntent.getService(this,0,stopI,PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piTog =PendingIntent.getService(this,1,togI, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this,CHAN_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("salinewin")
            .setContentText(mView!=null?(mView.filterOn?"ON":"OFF")+" hue="+(int)(mView!=null?mView.hueShift:0):"starting")
            .addAction(android.R.drawable.ic_media_play,"Toggle",piTog)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"Stop",piStop)
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build();
    }

    private void updateNotif() {
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF_ID,buildNotif());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRedraw);
        if (mView!=null) mWM.removeView(mView);
    }

    @Override public IBinder onBind(Intent i){return null;}
}
