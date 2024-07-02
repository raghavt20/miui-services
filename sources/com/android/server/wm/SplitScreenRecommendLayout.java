package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.miui.server.stability.DumpSysInfoUtil;

/* loaded from: classes.dex */
public class SplitScreenRecommendLayout extends FrameLayout {
    private static final float RECOMMEND_VIEW_TOP_MARGIN = 37.0f;
    private static final float RECOMMEND_VIEW_TOP_MARGIN_OFFSET = 10.0f;
    private static final String TAG = "SplitScreenRecommendLayout";
    private Context mContext;
    private ImageView mPrimaryImageView;
    private ImageView mSecondaryImageView;
    private RelativeLayout mSplitScreenIconContainer;
    private WindowManager mWindowManager;

    public SplitScreenRecommendLayout(Context context) {
        super(context);
        init(context);
    }

    public SplitScreenRecommendLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SplitScreenRecommendLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public SplitScreenRecommendLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void init(Context context) {
        Slog.d(TAG, "init splitScreenRecommendLayout ");
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(DumpSysInfoUtil.WINDOW);
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration newConfig) {
        Slog.d(TAG, "onConfigurationChanged newConfig.orientation=" + newConfig.orientation);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        RelativeLayout relativeLayout = (RelativeLayout) getChildAt(0);
        this.mSplitScreenIconContainer = relativeLayout;
        this.mPrimaryImageView = (ImageView) relativeLayout.getChildAt(0);
        this.mSecondaryImageView = (ImageView) this.mSplitScreenIconContainer.getChildAt(1);
        Slog.d(TAG, "onFinishInflate");
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
        lp.setTitle("SplitScreen-RecommendView");
        return lp;
    }

    public RelativeLayout getSplitScreenIconContainer() {
        return this.mSplitScreenIconContainer;
    }

    public ImageView getPrimaryImageView() {
        return this.mPrimaryImageView;
    }

    public ImageView getSecondaryImageView() {
        return this.mSecondaryImageView;
    }

    public void setSplitScreenIcon(Drawable primaryDrawable, Drawable secondaryDrawble) {
        this.mPrimaryImageView.setImageDrawable(primaryDrawable);
        this.mSecondaryImageView.setImageDrawable(secondaryDrawble);
    }

    private float dipToPx(float dip) {
        return TypedValue.applyDimension(1, dip, getResources().getDisplayMetrics());
    }

    private void setRadius(View content, final float radius) {
        content.setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.server.wm.SplitScreenRecommendLayout.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        content.setClipToOutline(true);
    }
}
