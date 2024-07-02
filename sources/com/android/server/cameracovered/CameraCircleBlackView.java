package com.android.server.cameracovered;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

/* loaded from: classes.dex */
public class CameraCircleBlackView extends ImageView {
    AttributeSet mAttrs;
    private Context mContext;
    private float mInternalRadio;
    private Paint mPaint;
    private int mViewCenterX;
    private int mViewCenterY;

    public CameraCircleBlackView(Context context) {
        this(context, null);
    }

    public CameraCircleBlackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraCircleBlackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mPaint = new Paint();
        this.mContext = context;
        this.mAttrs = attrs;
        init();
    }

    public void setRadius(float radius) {
        this.mInternalRadio = radius;
    }

    public void setRadiusColor(int color) {
        switch (color) {
            case 1:
                this.mPaint.setColor(-16711936);
                return;
            case 2:
                this.mPaint.setColor(-16777216);
                return;
            default:
                this.mPaint.setColor(-16777216);
                return;
        }
    }

    public void init() {
        Paint paint = new Paint(1);
        this.mPaint = paint;
        paint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaint.setColor(-16777216);
    }

    @Override // android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();
        this.mViewCenterX = viewWidth / 2;
        this.mViewCenterY = viewHeight / 2;
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawCircle(this.mViewCenterX, this.mViewCenterY, this.mInternalRadio, this.mPaint);
    }
}
