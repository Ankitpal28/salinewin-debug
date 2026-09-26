package com.salinewin;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class RemoteActivity extends AppCompatActivity implements IpcClient.Listener {

    private IpcClient mClient;
    private TextView  mTvStatus, mTvState;
    private SeekBar   mSbHue, mSbSat, mSbBri;
    private Button    mBtnConnect;
    private boolean   mConnected = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_remote);

        mTvStatus   = findViewById(R.id.tvStatus);
        mTvState    = findViewById(R.id.tvState);
        mBtnConnect = findViewById(R.id.btnConnect);
        mSbHue      = findViewById(R.id.sbHueR);
        mSbSat      = findViewById(R.id.sbSatR);
        mSbBri      = findViewById(R.id.sbBriR);

        mClient = new IpcClient("localhost", 9999);
        mClient.setListener(this);

        mBtnConnect.setOnClickListener(v->{
            if(!mConnected) mClient.connect(); else mClient.disconnect();
        });

        SeekBar.OnSeekBarChangeListener sl = new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){
                if(!u||!mConnected)return;
                if(b==mSbHue) mClient.setHue(p);
                if(b==mSbSat) mClient.setSat(p/100f);
                if(b==mSbBri) mClient.setBri(p/100f);
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){if(mConnected)mClient.status();}
        };
        mSbHue.setOnSeekBarChangeListener(sl);
        mSbSat.setOnSeekBarChangeListener(sl);
        mSbBri.setOnSeekBarChangeListener(sl);

        findViewById(R.id.btnRemToggle).setOnClickListener(v->mClient.toggle());
        findViewById(R.id.btnRemHueInc).setOnClickListener(v->mClient.hueInc());
        findViewById(R.id.btnRemHueDec).setOnClickListener(v->mClient.hueDec());
        findViewById(R.id.btnRemReset) .setOnClickListener(v->mClient.reset());
        findViewById(R.id.btnRemStatus).setOnClickListener(v->mClient.status());
    }

    public void onConnected(){
        mConnected=true;
        mTvStatus.setText("● CONNECTED");
        mBtnConnect.setText(getString(R.string.btn_disconnect));
        mClient.status();
    }
    public void onDisconnected(){
        mConnected=false;
        mTvStatus.setText("○ DISCONNECTED");
        mBtnConnect.setText(getString(R.string.btn_connect));
    }
    public void onStateUpdate(IpcClient.OverlayState s){
        mTvState.setText(s.toString());
        mSbHue.setProgress((int)s.hue);
        mSbSat.setProgress((int)(s.sat*100));
        mSbBri.setProgress((int)(s.bri*100));
    }
    public void onReply(String r){mTvState.setText(r);}
    public void onError(String m){
        mTvStatus.setText("ERR: "+m);
        mConnected=false;
        Toast.makeText(this,"Run: adb forward tcp:9999 tcp:9999",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy(){super.onDestroy();if(mClient!=null)mClient.disconnect();}
}
