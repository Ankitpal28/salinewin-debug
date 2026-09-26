package com.salinewin;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public class OverlayView extends View {
    public volatile float   hueShift   = 0f;
    public volatile float   saturation = 1f;
    public volatile float   brightness = 1f;
    public volatile boolean filterOn   = true;
    public volatile float   crossX     = -1f;
    public volatile float   crossY     = -1f;

    private static final int GAP=4, LEN=10, THICK=3, CR=18, SR=8, SM=32;

    private final Paint mFilter  = new Paint();
    private final Paint mArm, mCircle, mDotOn, mDotOff;

    public OverlayView(Context ctx) {
        super(ctx);
        setWillNotDraw(false);

        mArm = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArm.setColor(Color.GREEN);
        mArm.setStrokeWidth(THICK);
        mArm.setStyle(Paint.Style.STROKE);
        mArm.setStrokeCap(Paint.Cap.ROUND);

        mCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircle.setColor(Color.argb(200,0,220,0));
        mCircle.setStrokeWidth(1.5f);
        mCircle.setStyle(Paint.Style.STROKE);

        mDotOn = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotOn.setColor(Color.GREEN);
        mDotOn.setStyle(Paint.Style.FILL);

        mDotOff = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotOff.setColor(Color.RED);
        mDotOff.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // colour filter tint
        if (filterOn && (hueShift!=0f||saturation!=1f||brightness!=1f)) {
            float[] mat = HueMat.build(hueShift, saturation, brightness);
            mFilter.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(mat)));
            mFilter.setColor(Color.WHITE);
            mFilter.setAlpha(55);
            canvas.drawRect(0,0,getWidth(),getHeight(),mFilter);
        }

        // crosshair
        float cx = crossX>0 ? crossX : getWidth()/2f;
        float cy = crossY>0 ? crossY : getHeight()/2f;
        canvas.drawCircle(cx, cy, CR, mCircle);
        canvas.drawLine(cx,   cy-GAP,   cx,         cy-GAP-LEN, mArm);
        canvas.drawLine(cx,   cy+GAP,   cx,         cy+GAP+LEN, mArm);
        canvas.drawLine(cx-GAP,cy,      cx-GAP-LEN, cy,         mArm);
        canvas.drawLine(cx+GAP,cy,      cx+GAP+LEN, cy,         mArm);

        // status dot
        canvas.drawCircle(getWidth()-SM, getHeight()-SM, SR,
                          filterOn ? mDotOn : mDotOff);
    }
}
