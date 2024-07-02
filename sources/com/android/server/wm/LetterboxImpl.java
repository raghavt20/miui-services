package com.android.server.wm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.devicestate.DeviceStateManagerGlobal;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.Letterbox;
import com.miui.base.MiuiStubRegistry;
import java.util.function.Supplier;
import miui.util.MiuiMultiDisplayTypeInfo;

/* loaded from: classes.dex */
public class LetterboxImpl extends LetterboxStub {
    private static final String BITMAP_SURFACE_NAME = "bitmapBackground";
    private static final String FLIP_SMALL_SURFACE_NAME = "flipSmallBackground";
    private static final String TAG = "LetterboxImpl";
    private ActivityRecord mActivityRecord;
    protected LetterboxSurfaceEx mBackgroundSurfaceEx;
    private boolean mIsDarkMode;
    private Letterbox mLetterbox;
    private boolean mNeedRedraw = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LetterboxImpl> {

        /* compiled from: LetterboxImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LetterboxImpl INSTANCE = new LetterboxImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LetterboxImpl m2490provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LetterboxImpl m2489provideNewInstance() {
            return new LetterboxImpl();
        }
    }

    public void setLetterbox(Letterbox letterbox) {
        String str;
        this.mLetterbox = letterbox;
        boolean isFlipSmallScreen = MiuiMultiDisplayTypeInfo.isFlipDevice() && DeviceStateManagerGlobal.getInstance().getCurrentState() == 0;
        if (isFlipSmallScreen) {
            str = FLIP_SMALL_SURFACE_NAME;
        } else {
            str = BITMAP_SURFACE_NAME;
        }
        this.mBackgroundSurfaceEx = new LetterboxSurfaceEx(str, this);
    }

    public boolean attachInput(WindowState win) {
        this.mActivityRecord = win.mActivityRecord;
        if (!useMiuiBackgroundWindowSurface()) {
            return false;
        }
        this.mBackgroundSurfaceEx.attachInput(win);
        return true;
    }

    public void layout(Rect outer, Rect inner, Point surfaceOrigin) {
        this.mBackgroundSurfaceEx.layout(outer.left, outer.top, outer.right, outer.bottom, surfaceOrigin);
    }

    public void remove() {
        this.mBackgroundSurfaceEx.remove();
    }

    public boolean useMiuiBackgroundWindowSurface() {
        return this.mActivityRecord != null && ((MiuiAppSizeCompatModeStub.get().isEnabled() && this.mActivityRecord.inMiuiSizeCompatMode()) || MiuiEmbeddingWindowServiceStub.get().useMiuiBackgroundLetterbox(this.mActivityRecord));
    }

    public boolean darkModeChanged() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && activityRecord.isEmbedded()) {
            Context context = this.mActivityRecord.mAtmService.mContext;
            boolean isDarkMode = (context.getResources().getConfiguration().uiMode & 48) == 32;
            if (this.mIsDarkMode != isDarkMode) {
                this.mIsDarkMode = isDarkMode;
                this.mNeedRedraw = true;
                return true;
            }
        }
        return false;
    }

    public boolean needsApplySurfaceChanges() {
        return this.mBackgroundSurfaceEx.needsApplySurfaceChanges();
    }

    public void applySurfaceChanges(SurfaceControl.Transaction t, Letterbox.LetterboxSurface[] mSurfaces, Letterbox.LetterboxSurface mBehind) {
        this.mBackgroundSurfaceEx.applySurfaceChanges(t);
        for (Letterbox.LetterboxSurface surface : mSurfaces) {
            surface.remove();
        }
        mBehind.remove();
    }

    public void onMovedToDisplay(int displayId) {
        if (this.mBackgroundSurfaceEx.getInputInterceptor() != null) {
            this.mBackgroundSurfaceEx.getInputInterceptor().mWindowHandle.displayId = displayId;
        }
    }

    public SurfaceControl createLetterboxSurfaceSurface(SurfaceControl.Transaction t, String mType, Supplier<SurfaceControl.Builder> mSurfaceControlFactory) {
        if (isFlipSmallSurface(mType)) {
            return this.mBackgroundSurfaceEx.createFlipSmallSurface(t, mSurfaceControlFactory);
        }
        return this.mBackgroundSurfaceEx.createSurface(t, mType, mSurfaceControlFactory);
    }

    public boolean isBitmapSurface(String mType) {
        return BITMAP_SURFACE_NAME.equals(mType);
    }

    public boolean isFlipSmallSurface(String type) {
        return FLIP_SMALL_SURFACE_NAME.equals(type);
    }

    public boolean applyLetterboxSurfaceChanges(SurfaceControl.Transaction t, Rect mSurfaceFrameRelative, Rect mLayoutFrameRelative, String mType) {
        return this.mBackgroundSurfaceEx.applyLetterboxSurfaceChanges(t, mSurfaceFrameRelative, mLayoutFrameRelative, mType);
    }

    public void redraw(boolean forceRedraw) {
        if (this.mActivityRecord != null && MiuiEmbeddingWindowServiceStub.get().isActivityInFixedOrientation(this.mActivityRecord, true, true) && forceRedraw) {
            this.mBackgroundSurfaceEx.redrawBitmapSurface();
            return;
        }
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && activityRecord.isEmbedded() && (this.mNeedRedraw || forceRedraw)) {
            this.mBackgroundSurfaceEx.redrawBitmapSurface();
            if (this.mNeedRedraw) {
                this.mNeedRedraw = false;
                return;
            }
            return;
        }
        ActivityRecord activityRecord2 = this.mActivityRecord;
        if (activityRecord2 != null && activityRecord2.inMiuiSizeCompatMode() && forceRedraw) {
            this.mBackgroundSurfaceEx.redrawBitmapSurface();
        }
        if (navigationBarColorChanged()) {
            try {
                SurfaceControl.Transaction sct = new SurfaceControl.Transaction();
                try {
                    updateColor(this.mBackgroundSurfaceEx.getSurface(), sct);
                    sct.apply();
                    sct.close();
                } finally {
                }
            } catch (Exception e) {
            }
            ActivityRecord activityRecord3 = this.mActivityRecord;
            if (activityRecord3 != null) {
                activityRecord3.mActivityRecordStub.updateNavigationBarColorFinish();
            }
        }
    }

    public boolean needShowLetterbox() {
        if (this.mActivityRecord == null || !(MiuiEmbeddingWindowServiceStub.get().isActivityInFixedOrientation(this.mActivityRecord, true, true) || (MiuiEmbeddingWindowServiceStub.get().isEmbeddingEnabledForPackage(this.mActivityRecord.packageName) && this.mActivityRecord.isActivityEmbedded(true)))) {
            return super.needShowLetterbox();
        }
        return this.mActivityRecord.isVisible();
    }

    public boolean needRemoveMiuiLetterbox() {
        LetterboxSurfaceEx letterboxSurfaceEx;
        if (this.mActivityRecord != null && (letterboxSurfaceEx = this.mBackgroundSurfaceEx) != null && letterboxSurfaceEx.needRemoveMiuiLetterbox() && !this.mActivityRecord.isClientVisible() && this.mActivityRecord.isState(ActivityRecord.State.STOPPED, ActivityRecord.State.DESTROYING, ActivityRecord.State.DESTROYED)) {
            return true;
        }
        return false;
    }

    public int getNavigationBarColor() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord == null) {
            return 0;
        }
        return activityRecord.mActivityRecordStub.getNavigationBarColor();
    }

    /* loaded from: classes.dex */
    class LetterboxSurfaceEx extends Letterbox.LetterboxSurface {
        private Bitmap mBackgroundBitmap;
        private Surface mBackgroundSurface;
        Paint mPaint;

        /* JADX WARN: Illegal instructions before constructor call */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        LetterboxSurfaceEx(java.lang.String r3, com.android.server.wm.LetterboxImpl r4) {
            /*
                r1 = this;
                com.android.server.wm.LetterboxImpl.this = r2
                com.android.server.wm.Letterbox r0 = com.android.server.wm.LetterboxImpl.m2487$$Nest$fgetmLetterbox(r4)
                java.util.Objects.requireNonNull(r0)
                r1.<init>(r0, r3)
                android.graphics.Paint r0 = new android.graphics.Paint
                r0.<init>()
                r1.mPaint = r0
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.LetterboxImpl.LetterboxSurfaceEx.<init>(com.android.server.wm.LetterboxImpl, java.lang.String, com.android.server.wm.LetterboxImpl):void");
        }

        private boolean getWallpaperBitmap() {
            Bitmap backgroundBitmap;
            if (LetterboxImpl.this.mActivityRecord == null || (backgroundBitmap = LetterboxImpl.this.mActivityRecord.mWmService.getBlurWallpaperBmp()) == null) {
                return false;
            }
            this.mBackgroundBitmap = backgroundBitmap;
            return true;
        }

        private void drawBackgroundBitmapLocked(Bitmap bgBitmap, Rect rect) {
            Surface surface;
            TaskFragment taskFragment;
            TaskFragment adjacentTaskFragment;
            int splitLineLightColor;
            if (bgBitmap != null && (surface = this.mBackgroundSurface) != null) {
                Canvas canvas = null;
                try {
                    canvas = surface.lockCanvas(null);
                } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
                    Slog.w(LetterboxImpl.TAG, "Failed to lock canvas", e);
                }
                if (canvas == null) {
                    return;
                }
                int left = 0;
                int right = bgBitmap.getWidth() + 0;
                int top = 0;
                int bottom = bgBitmap.getHeight();
                if (bgBitmap.getWidth() >= rect.width() && bgBitmap.getHeight() >= rect.height()) {
                    int inWidth = bgBitmap.getWidth();
                    int outWidth = rect.width();
                    int inHeight = bgBitmap.getHeight();
                    int outHeight = rect.height();
                    int outWidth2 = Math.min(inWidth, outWidth);
                    left = (int) ((inWidth - outWidth2) * 0.5f);
                    right = left + outWidth2;
                    top = (int) ((inHeight - r9) * 0.5f);
                    bottom = top + Math.min(inHeight, outHeight);
                }
                canvas.drawBitmap(bgBitmap, new Rect(left, top, right, bottom), rect, (Paint) null);
                if (LetterboxImpl.this.mActivityRecord.isEmbedded() && (taskFragment = LetterboxImpl.this.mActivityRecord.getTaskFragment()) != null && (adjacentTaskFragment = taskFragment.getAdjacentTaskFragment()) != null) {
                    Context context = LetterboxImpl.this.mActivityRecord.mAtmService.mContext;
                    boolean isDarkMode = (context.getResources().getConfiguration().uiMode & 48) == 32;
                    Paint paint = this.mPaint;
                    if (isDarkMode) {
                        splitLineLightColor = MiuiEmbeddingWindowServiceStub.get().getSplitLineDarkColor(LetterboxImpl.this.mActivityRecord.packageName);
                    } else {
                        splitLineLightColor = MiuiEmbeddingWindowServiceStub.get().getSplitLineLightColor(LetterboxImpl.this.mActivityRecord.packageName);
                    }
                    paint.setColor(splitLineLightColor);
                    Rect splitLineRect = new Rect();
                    getSplitLineRect(taskFragment, adjacentTaskFragment, rect, splitLineRect);
                    if (!splitLineRect.isEmpty()) {
                        canvas.drawRect(splitLineRect, this.mPaint);
                    }
                }
                this.mBackgroundSurface.unlockCanvasAndPost(canvas);
            }
        }

        private void getSplitLineRect(TaskFragment tf1, TaskFragment tf2, Rect bitmapRect, Rect rect) {
            if (tf1 != null && tf2 != null) {
                Rect tf1Rect = tf1.getBounds();
                Rect tf2Rect = tf2.getBounds();
                if (tf1Rect.right < tf2Rect.left) {
                    rect.set(tf1Rect.right, bitmapRect.top, tf2Rect.left, bitmapRect.bottom);
                } else {
                    rect.set(tf2Rect.right, bitmapRect.top, tf1Rect.left, bitmapRect.bottom);
                }
            }
        }

        private void getBitmapSurfaceParams(Rect rect) {
            if (LetterboxImpl.this.mActivityRecord == null) {
                return;
            }
            DisplayContent dc = LetterboxImpl.this.mActivityRecord.getDisplayContent();
            DisplayInfo displayInfo = dc.isFixedRotationLaunchingApp(LetterboxImpl.this.mActivityRecord) ? LetterboxImpl.this.mActivityRecord.getFixedRotationTransformDisplayInfo() : dc.getDisplayInfo();
            if (displayInfo == null) {
                Slog.w(LetterboxImpl.TAG, "skip getBitmapSurfaceParams, displayInfo == null ,mActivityRecord = " + LetterboxImpl.this.mActivityRecord + " ,isFixedRotationLaunchingApp = " + dc.isFixedRotationLaunchingApp(LetterboxImpl.this.mActivityRecord));
            } else {
                rect.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
            }
        }

        public SurfaceControl createFlipSmallSurface(SurfaceControl.Transaction sct, Supplier<SurfaceControl.Builder> supplier) {
            SurfaceControl surfaceControl = supplier.get().setName("Letterbox - NBC").setFlags(4).setColorLayer().setCallsite("LetterboxSurface.createSurface").build();
            LetterboxImpl.this.updateColor(surfaceControl, sct);
            sct.show(surfaceControl);
            return surfaceControl;
        }

        public SurfaceControl createSurface(SurfaceControl.Transaction t, String mType, Supplier<SurfaceControl.Builder> mSurfaceControlFactory) {
            Rect rect = new Rect(0, 0, 0, 0);
            getBitmapSurfaceParams(rect);
            int longSide = Math.max(rect.width(), rect.height());
            Rect bitmapRect = (LetterboxImpl.this.mActivityRecord.isEmbedded() || LetterboxImpl.this.mActivityRecord.isLetterboxedForFixedOrientationAndAspectRatio()) ? rect : new Rect(0, 0, longSide, longSide);
            SurfaceControl mSurface = mSurfaceControlFactory.get().setName("Letterbox - " + LetterboxImpl.this.mActivityRecord + " - " + mType).setFlags(4).setBufferSize(bitmapRect.width(), bitmapRect.height()).setCallsite("LetterboxSurface.createSurface").build();
            if (getWallpaperBitmap() && this.mBackgroundBitmap != null) {
                this.mBackgroundSurface = new Surface(mSurface);
                Slog.d(LetterboxImpl.TAG, LetterboxImpl.this.mActivityRecord + " create " + LetterboxImpl.BITMAP_SURFACE_NAME + " " + bitmapRect);
                drawBackgroundBitmapLocked(this.mBackgroundBitmap, bitmapRect);
                if (!LetterboxImpl.this.mActivityRecord.isEmbedded() && !LetterboxImpl.this.mActivityRecord.isLetterboxedForFixedOrientationAndAspectRatio()) {
                    t.show(mSurface);
                }
            }
            return mSurface;
        }

        public void remove() {
            super.remove();
            if (this.mBackgroundSurface != null) {
                Slog.d(LetterboxImpl.TAG, "remove letterbox surface bitmapBackground for " + LetterboxImpl.this.mActivityRecord);
                this.mBackgroundSurface.release();
                this.mShowed = false;
                this.mBackgroundSurface = null;
            }
        }

        public boolean applyLetterboxSurfaceChanges(SurfaceControl.Transaction t, Rect mSurfaceFrameRelative, Rect mLayoutFrameRelative, String mType) {
            if (!mSurfaceFrameRelative.equals(mLayoutFrameRelative) && LetterboxImpl.BITMAP_SURFACE_NAME.equals(mType) && !mSurfaceFrameRelative.isEmpty() && getWallpaperBitmap() && this.mBackgroundBitmap != null && applyLetterboxSurfaceChangesForActivityEmbedding(mSurfaceFrameRelative, mLayoutFrameRelative)) {
                return true;
            }
            return false;
        }

        private boolean applyLetterboxSurfaceChangesForActivityEmbedding(Rect mSurfaceFrameRelative, Rect mLayoutFrameRelative) {
            if (LetterboxImpl.this.mActivityRecord.isEmbedded()) {
                return (mSurfaceFrameRelative.height() == mLayoutFrameRelative.height() || mSurfaceFrameRelative.width() == mLayoutFrameRelative.width()) ? false : true;
            }
            return true;
        }

        public void redrawBitmapSurface() {
            Rect rect = new Rect(0, 0, 0, 0);
            getBitmapSurfaceParams(rect);
            if (this.mBackgroundSurface == null) {
                if (getSurface() == null) {
                    return;
                } else {
                    this.mBackgroundSurface = new Surface(getSurface());
                }
            }
            int longSide = Math.max(rect.width(), rect.height());
            if (this.mBackgroundBitmap != null) {
                Rect bitmapRect = (LetterboxImpl.this.mActivityRecord.isEmbedded() || LetterboxImpl.this.mActivityRecord.isLetterboxedForFixedOrientationAndAspectRatio()) ? rect : new Rect(0, 0, longSide, longSide);
                Slog.d(LetterboxImpl.TAG, LetterboxImpl.this.mActivityRecord + " redraw " + LetterboxImpl.BITMAP_SURFACE_NAME + " " + bitmapRect);
                drawBackgroundBitmapLocked(this.mBackgroundBitmap, bitmapRect);
            }
        }

        public boolean needRemoveMiuiLetterbox() {
            return this.mBackgroundSurface != null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateColor(SurfaceControl sc, SurfaceControl.Transaction sct) {
        Color color;
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord == null) {
            color = Color.valueOf(0);
        } else {
            color = Color.valueOf(activityRecord.mActivityRecordStub.getNavigationBarColor());
        }
        sct.setColor(sc, new float[]{color.red(), color.green(), color.blue()});
    }

    public boolean isFlipSmallSurface() {
        return isFlipSmallSurface(this.mBackgroundSurfaceEx.getType());
    }

    public boolean navigationBarColorChanged() {
        return this.mActivityRecord.mActivityRecordStub.navigationBarColorChanged();
    }
}
