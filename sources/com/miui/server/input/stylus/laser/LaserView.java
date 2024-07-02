package com.miui.server.input.stylus.laser;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class LaserView extends View {
    public static final int MODE_LASER = 0;
    public static final int MODE_PEN = 1;
    private final Paint mInPaint;
    private boolean mKeepPath;
    private final float mLaserCenterX;
    private final float mLaserCenterY;
    private final float mLaserInRadius;
    private final float mLaserOutBlurRadius;
    private final float mLaserOutFillRadius;
    private final float mLaserPenHeight;
    private final Drawable mLaserPenImage;
    private final float mLaserPenWidth;
    private int mMode;
    private final Paint mOutBlurPaint;
    private final Paint mOutFillPaint;
    private final List<Path> mPathList;
    private final Paint mPathPaint;
    private boolean mPresentationVisible;
    private boolean mVisible;
    private float mX;
    private float mY;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface Mode {
    }

    public LaserView(Context context) {
        this(context, null);
    }

    public LaserView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LaserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mMode = 0;
        this.mVisible = true;
        this.mPresentationVisible = false;
        Paint paint = new Paint();
        this.mInPaint = paint;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#CCFF001F"));
        Paint paint2 = new Paint();
        this.mOutBlurPaint = paint2;
        paint2.setStrokeWidth(context.getResources().getDimension(285671521));
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setColor(Color.parseColor("#FF001F"));
        paint2.setMaskFilter(new BlurMaskFilter(context.getResources().getDimension(285671518), BlurMaskFilter.Blur.NORMAL));
        Paint paint3 = new Paint();
        this.mOutFillPaint = paint3;
        paint3.setStrokeWidth(context.getResources().getDimension(285671521));
        paint3.setStyle(Paint.Style.STROKE);
        paint3.setColor(Color.parseColor("#FF001F"));
        this.mPathList = new LinkedList();
        Paint paint4 = new Paint();
        this.mPathPaint = paint4;
        paint4.setAntiAlias(true);
        paint4.setDither(true);
        paint4.setColor(-65536);
        paint4.setStyle(Paint.Style.STROKE);
        paint4.setStrokeWidth(context.getResources().getDimension(285671525));
        paint4.setStrokeJoin(Paint.Join.ROUND);
        paint4.setStrokeCap(Paint.Cap.ROUND);
        this.mLaserInRadius = context.getResources().getDimension(285671517);
        this.mLaserOutFillRadius = context.getResources().getDimension(285671520);
        this.mLaserOutBlurRadius = context.getResources().getDimension(285671519);
        this.mLaserPenWidth = context.getResources().getDimension(285671526);
        this.mLaserPenHeight = context.getResources().getDimension(285671524);
        this.mLaserCenterX = context.getResources().getFloat(285671522);
        this.mLaserCenterY = context.getResources().getFloat(285671523);
        this.mLaserPenImage = context.getDrawable(285737551);
    }

    @Override // android.view.View
    protected void onDraw(final Canvas canvas) {
        if (!this.mVisible) {
            return;
        }
        this.mPathList.forEach(new Consumer() { // from class: com.miui.server.input.stylus.laser.LaserView$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                LaserView.this.lambda$onDraw$0(canvas, (Path) obj);
            }
        });
        if (!this.mPresentationVisible) {
            return;
        }
        int i = this.mMode;
        if (i == 0) {
            canvas.drawCircle(this.mX, this.mY, this.mLaserOutBlurRadius, this.mOutBlurPaint);
            canvas.drawCircle(this.mX, this.mY, this.mLaserOutFillRadius, this.mOutFillPaint);
            canvas.drawCircle(this.mX, this.mY, this.mLaserInRadius, this.mInPaint);
        } else if (i == 1) {
            this.mLaserPenImage.draw(canvas);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onDraw$0(Canvas canvas, Path path) {
        canvas.drawPath(path, this.mPathPaint);
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
        invalidate();
    }

    public void setPresentationVisible(boolean presentationVisible) {
        this.mPresentationVisible = presentationVisible;
        invalidate();
    }

    public void move(float deltaX, float deltaY) {
        setPosition(this.mX + deltaX, this.mY + deltaY);
    }

    public void setPosition(PointF position) {
        setPosition(position.x, position.y);
    }

    public void setPosition(float x, float y) {
        if (!this.mPathList.isEmpty() && this.mKeepPath) {
            Path path = this.mPathList.get(0);
            if (path.isEmpty()) {
                path.moveTo(x, y);
            } else {
                float f = this.mX;
                float f2 = this.mY;
                path.quadTo(f, f2, (x + f) / 2.0f, (y + f2) / 2.0f);
            }
        }
        this.mX = x;
        this.mY = y;
        float f3 = this.mLaserPenWidth;
        int left = (int) (x - (this.mLaserCenterX * f3));
        float f4 = this.mLaserPenHeight;
        int top = (int) (y - (this.mLaserCenterY * f4));
        int right = (int) (left + f3);
        int bottom = (int) (top + f4);
        this.mLaserPenImage.setBounds(left, top, right, bottom);
        invalidate();
    }

    public void getPosition(float[] outPosition) {
        outPosition[0] = this.mX;
        outPosition[1] = this.mY;
    }

    public void setMode(int mode) {
        if (this.mMode == mode) {
            return;
        }
        this.mMode = mode;
        invalidate();
    }

    public int getMode() {
        return this.mMode;
    }

    public boolean isKeepPath() {
        return this.mKeepPath;
    }

    public void setKeepPath(boolean keepPath) {
        if (this.mKeepPath == keepPath) {
            return;
        }
        this.mKeepPath = keepPath;
        if (keepPath) {
            Path path = new Path();
            this.mPathList.add(0, path);
        }
    }

    public void clearPath() {
        this.mPathList.clear();
        invalidate();
    }

    public boolean needExist() {
        if (!this.mVisible) {
            return false;
        }
        if (!this.mPathList.isEmpty()) {
            return true;
        }
        return this.mPresentationVisible;
    }
}
