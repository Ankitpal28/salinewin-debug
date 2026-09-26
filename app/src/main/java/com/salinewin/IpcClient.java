package com.salinewin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class IpcClient {
    private static final String TAG = "salinewin.IPC";

    public static final String CMD_TOGGLE  = "TOGGLE";
    public static final String CMD_HUE_INC = "HUE_INC";
    public static final String CMD_HUE_DEC = "HUE_DEC";
    public static final String CMD_RESET   = "RESET";
    public static final String CMD_STATUS  = "STATUS";
    public static final String CMD_QUIT    = "QUIT";

    public static class OverlayState {
        public boolean enabled;
        public float   hue, sat, bri;
        static OverlayState parse(String j) {
            OverlayState s=new OverlayState();
            try {
                s.enabled=j.contains("\"enabled\":true");
                s.hue=pf(j,"hue"); s.sat=pf(j,"sat"); s.bri=pf(j,"bri");
            } catch(Exception e){ s.sat=s.bri=1f; }
            return s;
        }
        private static float pf(String j,String k){
            int i=j.indexOf("\""+k+"\":");
            if(i<0)return 1f;
            int s=i+k.length()+3,e=s;
            while(e<j.length()&&(Character.isDigit(j.charAt(e))||j.charAt(e)=='.'||j.charAt(e)=='-'))e++;
            return Float.parseFloat(j.substring(s,e));
        }
        public String toString(){
            return String.format("%s  hue=%.0f°  sat=%.2f  bri=%.2f",
                enabled?"ON":"OFF",hue,sat,bri);
        }
    }

    public interface Listener {
        void onConnected();
        void onDisconnected();
        void onStateUpdate(OverlayState s);
        void onReply(String raw);
        void onError(String msg);
    }

    private final String  mHost;
    private final int     mPort;
    private Listener      mListener;
    private Socket        mSock;
    private PrintWriter   mOut;
    private BufferedReader mIn;
    private final ExecutorService mExec = Executors.newCachedThreadPool();
    private final LinkedBlockingQueue<String> mQ = new LinkedBlockingQueue<>();
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private volatile boolean mRunning = false;

    public IpcClient(String host, int port){mHost=host;mPort=port;}
    public void setListener(Listener l){mListener=l;}

    public void connect() {
        mExec.execute(()->{
            try {
                mSock=new Socket(mHost,mPort);
                mSock.setSoTimeout(5000);
                mOut=new PrintWriter(mSock.getOutputStream(),true);
                mIn =new BufferedReader(new InputStreamReader(mSock.getInputStream()));
                mRunning=true;
                post(()->{ if(mListener!=null) mListener.onConnected(); });
                mExec.execute(this::writePump);
                readPump();
            } catch(IOException e){
                Log.e(TAG,e.getMessage());
                post(()->{ if(mListener!=null) mListener.onError(e.getMessage()); });
            }
        });
    }

    public void send(String cmd){mQ.offer(cmd);}
    public void toggle() {send(CMD_TOGGLE);}
    public void hueInc(){send(CMD_HUE_INC);}
    public void hueDec(){send(CMD_HUE_DEC);}
    public void reset() {send(CMD_RESET);}
    public void status(){send(CMD_STATUS);}
    public void setHue(float v){send("SET_HUE:"+v);}
    public void setSat(float v){send("SET_SAT:"+v);}
    public void setBri(float v){send("SET_BRI:"+v);}

    public void disconnect(){
        mRunning=false; mQ.offer(CMD_QUIT);
        try{if(mSock!=null)mSock.close();}catch(IOException ignored){}
    }

    private void writePump(){
        while(mRunning){
            try{
                String c=mQ.take();
                mOut.println(c);
                if(CMD_QUIT.equals(c))break;
            }catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
    }

    private void readPump(){
        try{
            String line;
            while(mRunning&&(line=mIn.readLine())!=null){
                final String r=line;
                post(()->{
                    if(mListener==null)return;
                    mListener.onReply(r);
                    if(r.startsWith("{")) mListener.onStateUpdate(OverlayState.parse(r));
                });
            }
        }catch(IOException ignored){
        }finally{
            mRunning=false;
            post(()->{ if(mListener!=null) mListener.onDisconnected(); });
        }
    }

    private void post(Runnable r){mMain.post(r);}
}
