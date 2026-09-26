package com.salinewin;

import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        TextView tvHue = findViewById(R.id.tvHue);
        TextView tvSat = findViewById(R.id.tvSat);
        TextView tvBri = findViewById(R.id.tvBri);
        SeekBar  sbHue = findViewById(R.id.sbHue);
        SeekBar  sbSat = findViewById(R.id.sbSat);
        SeekBar  sbBri = findViewById(R.id.sbBri);

        sbHue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){
                tvHue.setText("Hue: "+p+"°");
                if(u) send(OverlayService.ACTION_SET_HUE, p);
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        sbSat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){
                tvSat.setText("Saturation: "+p+"%");
                if(u) send(OverlayService.ACTION_SET_SAT, p/100f);
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        sbBri.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){
                tvBri.setText("Brightness: "+p+"%");
                if(u) send(OverlayService.ACTION_SET_BRI, p/100f);
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });

        findViewById(R.id.btnStart) .setOnClickListener(v->{
            if(!canDraw()){reqPerm();return;}
            startService(new Intent(this,OverlayService.class));
        });
        findViewById(R.id.btnStop)  .setOnClickListener(v->cmd(OverlayService.ACTION_STOP));
        findViewById(R.id.btnToggle).setOnClickListener(v->cmd(OverlayService.ACTION_TOGGLE));
        findViewById(R.id.btnHueInc).setOnClickListener(v->cmd(OverlayService.ACTION_HUE_INC));
        findViewById(R.id.btnHueDec).setOnClickListener(v->cmd(OverlayService.ACTION_HUE_DEC));
        findViewById(R.id.btnReset) .setOnClickListener(v->cmd(OverlayService.ACTION_RESET));
        findViewById(R.id.btnRemote).setOnClickListener(v->
            startActivity(new Intent(this,RemoteActivity.class)));
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(!canDraw()) reqPerm();
    }

    private boolean canDraw(){
        return Build.VERSION.SDK_INT<23||Settings.canDrawOverlays(this);
    }
    private void reqPerm(){
        startActivityForResult(
            new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                       Uri.parse("package:"+getPackageName())), REQ_OVERLAY);
    }
    private void cmd(String action){
        startService(new Intent(this,OverlayService.class).setAction(action));
    }
    private void send(String action, float value){
        startService(new Intent(this,OverlayService.class)
            .setAction(action)
            .putExtra(OverlayService.EXTRA_VALUE, value));
    }
}
