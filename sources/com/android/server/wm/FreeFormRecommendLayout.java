package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.miui.server.stability.DumpSysInfoUtil;

/* loaded from: classes.dex */
public class FreeFormRecommendLayout extends FrameLayout {
    private static final float RECOMMEND_VIEW_TOP_MARGIN = 37.0f;
    private static final float RECOMMEND_VIEW_TOP_MARGIN_OFFSET = 10.0f;
    private static final String TAG = "FreeFormRecommendLayout";
    private Context mContext;
    private RelativeLayout mFreeFormIconContainer;
    private ImageView mFreeFormImageView;
    private WindowManager mWindowManager;

    public FreeFormRecommendLayout(Context context) {
        super(context);
        init(context);
    }

    public FreeFormRecommendLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FreeFormRecommendLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public FreeFormRecommendLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context context) {
        Log.d(TAG, "init FreeFormRecommendLayout ");
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(DumpSysInfoUtil.WINDOW);
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged newConfig.orientation=" + newConfig.orientation);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        RelativeLayout relativeLayout = (RelativeLayout) getChildAt(0);
        this.mFreeFormIconContainer = relativeLayout;
        this.mFreeFormImageView = (ImageView) relativeLayout.getChildAt(0);
        Log.d(TAG, "onFinishInflate");
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
    }

    public WindowManager.LayoutParams createLayoutParams(int topMargin) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-2, -2, 2038, 16778536, 1);
        lp.privateFlags |= 16;
        lp.privateFlags |= 64;
        lp.privateFlags |= 536870912;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.windowAnimations = -1;
        lp.rotationAnimation = -1;
        lp.gravity = 49;
        lp.y = ((int) dipToPx(RECOMMEND_VIEW_TOP_MARGIN_OFFSET)) + topMargin;
        lp.x = 0;
        lp.setTitle("FreeForm-RecommendView");
        return lp;
    }

    public RelativeLayout getFreeFormIconContainer() {
        return this.mFreeFormIconContainer;
    }

    public void setFreeFormIcon(Drawable freeFormDrawable) {
        this.mFreeFormImageView.setImageDrawable(freeFormDrawable);
    }

    private float dipToPx(float dip) {
        return TypedValue.applyDimension(1, dip, getResources().getDisplayMetrics());
    }
}
