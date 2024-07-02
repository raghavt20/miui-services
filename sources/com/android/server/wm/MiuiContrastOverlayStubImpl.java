package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControlFeatureImpl;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class MiuiContrastOverlayStubImpl implements MiuiContrastOverlayStub {
    private static final int TYPE_LAYER_MULTIPLIER = 10000;
    private static SurfaceControlFeatureImpl surfaceControlFeature = new SurfaceControlFeatureImpl();
    private Display mDisplay;
    private SurfaceControl mSurfaceControl;
    private SurfaceControl.Transaction mTransaction;
    private String TAG = "MiuiContrastOverlayStubImpl";
    private int mDw = 0;
    private int mDh = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiContrastOverlayStubImpl> {

        /* compiled from: MiuiContrastOverlayStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiContrastOverlayStubImpl INSTANCE = new MiuiContrastOverlayStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiContrastOverlayStubImpl m2512provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiContrastOverlayStubImpl m2511provideNewInstance() {
            return new MiuiContrastOverlayStubImpl();
        }
    }

    public void init(DisplayContent dc, DisplayMetrics dm, Context mcontext) {
        this.mDisplay = dc.getDisplay();
        DisplayInfo defaultInfo = dc.getDisplayInfo();
        this.mDw = defaultInfo.logicalWidth;
        this.mDh = defaultInfo.logicalHeight;
        SurfaceControl control = null;
        float f = dm.densityDpi / 160.0f;
        try {
            control = dc.makeOverlay().setName("MiuiContrastOverlay").setOpaque(true).setColorLayer().build();
            this.mTransaction = SurfaceControl.getGlobalTransaction();
            surfaceControlFeature.setLayerStack(control, this.mDisplay.getLayerStack());
            this.mTransaction.setLayer(control, 999999);
            this.mTransaction.setPosition(control, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            this.mTransaction.show(control);
        } catch (Surface.OutOfResourcesException e) {
            Log.d(this.TAG, "createSurface e " + e);
        }
        this.mSurfaceControl = control;
    }

    public void positionSurface(int dw, int dh) {
        if (dw != this.mDw || dh != this.mDh) {
            this.mDw = dw;
            this.mDh = dh;
            this.mTransaction.setWindowCrop(this.mSurfaceControl, new Rect(0, 0, this.mDw, this.mDh));
        }
    }

    public void showContrastOverlay(float alpha) {
        surfaceControlFeature.setColor(this.mSurfaceControl, new float[]{MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X});
        setAlpha(alpha);
        this.mTransaction.setWindowCrop(this.mSurfaceControl, new Rect(0, 0, this.mDw, this.mDh));
        this.mTransaction.show(this.mSurfaceControl);
    }

    public void setAlpha(float alpha) {
        this.mTransaction.setAlpha(this.mSurfaceControl, alpha);
    }

    public void hideContrastOverlay() {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            this.mTransaction.hide(surfaceControl);
            this.mSurfaceControl.release();
            this.mSurfaceControl = null;
        }
    }
}
