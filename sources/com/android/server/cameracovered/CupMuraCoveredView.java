package com.android.server.cameracovered;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/* loaded from: classes.dex */
public class CupMuraCoveredView extends ImageView {
    static final String TAG = "CupMuraCoveredView";
    protected Context mContext;

    public CupMuraCoveredView(Context context) {
        this(context, null);
    }

    public CupMuraCoveredView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CupMuraCoveredView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    public void setRadius(float r) {
        Bitmap sbmp;
        int radius = (int) r;
        Context context = this.mContext;
        if (context == null) {
            Log.e(TAG, "setRadius error because mContext is null");
            return;
        }
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), 285737358);
        if (bmp == null) {
            Log.e(TAG, "the bitmap from decodeResource is null");
            return;
        }
        if (bmp.getWidth() != radius * 2 || bmp.getHeight() != radius * 2) {
            sbmp = Bitmap.createScaledBitmap(bmp, radius * 2, radius * 2, true);
        } else {
            sbmp = bmp;
        }
        setImageBitmap(sbmp);
        setScaleType(ImageView.ScaleType.CENTER);
    }
}
