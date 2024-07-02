package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.miui.base.MiuiStubRegistry;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import miui.android.animation.controller.AnimState;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class TalkbackWatermark implements TalkbackWatermarkStub {
    private static final boolean DEBUG = false;
    private static final String TAG = "TalkbackWatermark";
    private BLASTBufferQueue mBlastBufferQueue;
    private BroadcastReceiver mBroadcast;
    private int mDetPx;
    private int mPaddingPx;
    private String mString1;
    private String mString2;
    private Surface mSurface;
    private int mTextSizePx;
    private float mTitleSizePx;
    private WindowManagerService mWms;
    private final float mTitleSizeDp = 25.45f;
    private final float mTextSizeDp = 20.0f;
    private final float mDetDp = 20.37f;
    private final float mPaddingDp = 12.36f;
    private final float mShadowRadius = 1.0f;
    private final float mShadowDx = 2.0f;
    private final float mShadowDy = 2.0f;
    private final float mYProportionTop = 0.4f;
    private final float mXProportion = 0.5f;
    private final int TALKBACK_WATERMARK_SHOW_TIME = 10000;
    private SurfaceControl mSurfaceControl = null;
    private SurfaceControl.Transaction mTransaction = null;
    private int mLastDW = 0;
    private int mLastDH = 0;
    private boolean mDrawNeeded = false;
    private boolean mHasDrawn = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TalkbackWatermark> {

        /* compiled from: TalkbackWatermark$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TalkbackWatermark INSTANCE = new TalkbackWatermark();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public TalkbackWatermark m2785provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public TalkbackWatermark m2784provideNewInstance() {
            return new TalkbackWatermark();
        }
    }

    TalkbackWatermark() {
    }

    public void init(WindowManagerService wms) {
        final Uri talkbackWatermarkEnableUri = Settings.Secure.getUriFor("talkback_watermark_enable");
        wms.mContext.getContentResolver().registerContentObserver(talkbackWatermarkEnableUri, false, new ContentObserver(new Handler(Looper.getMainLooper())) { // from class: com.android.server.wm.TalkbackWatermark.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (talkbackWatermarkEnableUri.equals(uri)) {
                    TalkbackWatermark.this.updateWaterMark();
                }
            }
        }, -1);
        this.mWms = wms;
        setupBroadcast();
    }

    private synchronized void setupBroadcast() {
        if (this.mBroadcast == null) {
            this.mBroadcast = new BroadcastReceiver() { // from class: com.android.server.wm.TalkbackWatermark.2
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    char c;
                    String action = intent.getAction();
                    switch (action.hashCode()) {
                        case -19011148:
                            if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                                c = 0;
                                break;
                            }
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            TalkbackWatermark.this.refresh();
                            return;
                        default:
                            return;
                    }
                }
            };
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWaterMark() {
        this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.TalkbackWatermark$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                TalkbackWatermark.this.lambda$updateWaterMark$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateWaterMark$0() {
        boolean enabled = Settings.Secure.getIntForUser(this.mWms.mContext.getContentResolver(), "talkback_watermark_enable", 1, -2) != 0;
        this.mWms.openSurfaceTransaction();
        try {
            if (enabled) {
                doCreateSurface(this.mWms);
                showInternal();
                new Timer().schedule(new TimerTask() { // from class: com.android.server.wm.TalkbackWatermark.3
                    @Override // java.util.TimerTask, java.lang.Runnable
                    public void run() {
                        TalkbackWatermark.this.dismissInternal();
                    }
                }, 10000L);
            } else {
                dismissInternal();
            }
        } finally {
            this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
        }
    }

    private synchronized void doCreateSurface(WindowManagerService wms) {
        DisplayContent dc = wms.getDefaultDisplayContentLocked();
        float constNum = dc.mRealDisplayMetrics.densityDpi / 160.0f;
        this.mTextSizePx = (int) (20.0f * constNum);
        this.mDetPx = (int) (20.37f * constNum);
        this.mPaddingPx = (int) (12.36f * constNum);
        this.mTitleSizePx = (int) (25.45f * constNum);
        try {
            SurfaceControl ctrl = dc.makeOverlay().setName("TalkbackWatermarkSurface").setBLASTLayer().setBufferSize(1, 1).setFormat(-3).setCallsite("TalkbackWatermarkSurface").build();
            SurfaceControl.Transaction transaction = (SurfaceControl.Transaction) wms.mTransactionFactory.get();
            this.mTransaction = transaction;
            transaction.setLayer(ctrl, AnimState.VIEW_SIZE);
            this.mTransaction.setPosition(ctrl, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
            this.mTransaction.show(ctrl);
            InputMonitor.setTrustedOverlayInputInfo(ctrl, this.mTransaction, dc.getDisplayId(), "TalkbackWatermarkSurface");
            this.mTransaction.apply();
            this.mSurfaceControl = ctrl;
            BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue("TalkbackWatermarkSurface", this.mSurfaceControl, 1, 1, 1);
            this.mBlastBufferQueue = bLASTBufferQueue;
            this.mSurface = bLASTBufferQueue.createSurface();
        } catch (Surface.OutOfResourcesException e) {
            Slog.w(TAG, "createrSurface e" + e);
        }
    }

    public synchronized void positionSurface(int dw, int dh) {
        if (this.mSurfaceControl == null) {
            return;
        }
        if (this.mLastDW != dw || this.mLastDH != dh) {
            this.mLastDW = dw;
            this.mLastDH = dh;
            SurfaceControl.Transaction transaction = SurfaceControl.getGlobalTransaction();
            if (transaction != null) {
                transaction.setBufferSize(this.mSurfaceControl, dw, dh);
            }
            this.mDrawNeeded = true;
        }
    }

    private void drawIfNeeded() {
        if (this.mDrawNeeded) {
            int dw = this.mLastDW;
            int dh = this.mLastDH;
            this.mBlastBufferQueue.update(this.mSurfaceControl, dw, dh, 1);
            Rect dirty = new Rect(0, 0, dw, dh);
            Canvas c = null;
            try {
                c = this.mSurface.lockCanvas(dirty);
            } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
                Slog.w(TAG, "Failed to lock canvas", e);
            }
            if (c == null || c.getWidth() != dw || c.getHeight() != dh) {
                return;
            }
            this.mDrawNeeded = false;
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            int x = (int) (dw * 0.5f);
            int y = ((int) (dh * 0.4f)) + 60;
            Paint paint = new Paint(1);
            paint.setTextSize(this.mTitleSizePx);
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, 0));
            paint.setColor(-5000269);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setShadowLayer(1.0f, 2.0f, 2.0f, -16777216);
            c.drawText(this.mString1, x, y, paint);
            paint.setTextSize(this.mTextSizePx);
            TextPaint textPaint = new TextPaint(paint);
            int dir = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
            if (dir != 1) {
                StaticLayout staticLayout = new StaticLayout(this.mString2, textPaint, c.getWidth() - this.mPaddingPx, Layout.Alignment.ALIGN_NORMAL, 1.0f, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, false);
                c.save();
                c.translate(x, this.mDetPx + y);
                staticLayout.draw(c);
                c.restore();
            } else {
                int line = 1;
                int i = 0;
                while (i < this.mString2.length()) {
                    String str = this.mString2;
                    int i2 = i;
                    int len = textPaint.breakText(str, i2, str.length(), true, c.getWidth() - this.mPaddingPx, null);
                    c.drawTextRun(this.mString2.substring(i, i + len).toCharArray(), 0, len, 0, len, x, (this.mDetPx * line) + y, true, (Paint) textPaint);
                    i = i2 + len;
                    line++;
                    paint = paint;
                }
            }
            this.mSurface.unlockCanvasAndPost(c);
            this.mHasDrawn = true;
        }
    }

    public synchronized void show() {
        updateWaterMark();
        this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.TalkbackWatermark$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TalkbackWatermark.this.lambda$show$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$show$1() {
        this.mWms.mContext.registerReceiver(this.mBroadcast, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
    }

    public synchronized void setVisible(boolean visible) {
        if (visible) {
            showInternal();
        } else {
            hideInternal();
        }
    }

    public synchronized void dismiss() {
        this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.TalkbackWatermark$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TalkbackWatermark.this.lambda$dismiss$2();
            }
        });
        if (this.mSurfaceControl == null) {
            return;
        }
        Slog.d(TAG, "talkback-test dismiss");
        this.mWms.mH.post(new TalkbackWatermark$$ExternalSyntheticLambda2(this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$dismiss$2() {
        try {
            this.mWms.mContext.unregisterReceiver(this.mBroadcast);
        } catch (IllegalArgumentException e) {
            Slog.d(TAG, "mBroadcast is not registered", e);
        }
    }

    public synchronized void updateTalkbackMode(boolean enable, ComponentName mComponentName) {
        if (mComponentName.flattenToShortString().contains("TalkBackService") && mComponentName.getPackageName().equals("com.google.android.marvin.talkback")) {
            if (enable) {
                show();
            } else {
                dismiss();
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:26:0x0046, code lost:
    
        if (r6.mLastDH == 0) goto L15;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private synchronized void showInternal() {
        /*
            r6 = this;
            monitor-enter(r6)
            android.view.SurfaceControl r0 = r6.mSurfaceControl     // Catch: java.lang.Throwable -> L6d
            if (r0 != 0) goto L7
            monitor-exit(r6)
            return
        L7:
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6d
            r0.openSurfaceTransaction()     // Catch: java.lang.Throwable -> L6d
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6d
            android.content.Context r0 = r0.mContext     // Catch: java.lang.Throwable -> L6d
            android.content.res.Resources r0 = r0.getResources()     // Catch: java.lang.Throwable -> L6d
            r1 = 286196664(0x110f03b8, float:1.1281857E-28)
            java.lang.String r0 = r0.getString(r1)     // Catch: java.lang.Throwable -> L6d
            r6.mString1 = r0     // Catch: java.lang.Throwable -> L6d
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6d
            android.content.Context r0 = r0.mContext     // Catch: java.lang.Throwable -> L6d
            android.content.res.Resources r0 = r0.getResources()     // Catch: java.lang.Throwable -> L6d
            r1 = 286196665(0x110f03b9, float:1.1281858E-28)
            java.lang.String r0 = r0.getString(r1)     // Catch: java.lang.Throwable -> L6d
            r6.mString2 = r0     // Catch: java.lang.Throwable -> L6d
            android.view.SurfaceControl$Transaction r0 = android.view.SurfaceControl.getGlobalTransaction()     // Catch: java.lang.Throwable -> L63
            com.android.server.wm.WindowManagerService r1 = r6.mWms     // Catch: java.lang.Throwable -> L63
            com.android.server.wm.DisplayContent r1 = r1.getDefaultDisplayContentLocked()     // Catch: java.lang.Throwable -> L63
            android.view.DisplayInfo r2 = r1.getDisplayInfo()     // Catch: java.lang.Throwable -> L63
            int r3 = r2.logicalWidth     // Catch: java.lang.Throwable -> L63
            int r4 = r2.logicalHeight     // Catch: java.lang.Throwable -> L63
            int r5 = r6.mLastDW     // Catch: java.lang.Throwable -> L63
            if (r5 == 0) goto L4b
            int r5 = r6.mLastDH     // Catch: java.lang.Throwable -> L49
            if (r5 != 0) goto L4e
            goto L4b
        L49:
            r0 = move-exception
            goto L64
        L4b:
            r6.positionSurface(r3, r4)     // Catch: java.lang.Throwable -> L63
        L4e:
            r6.drawIfNeeded()     // Catch: java.lang.Throwable -> L63
            if (r0 == 0) goto L58
            android.view.SurfaceControl r5 = r6.mSurfaceControl     // Catch: java.lang.Throwable -> L49
            r0.show(r5)     // Catch: java.lang.Throwable -> L49
        L58:
            com.android.server.wm.WindowManagerService r0 = r6.mWms     // Catch: java.lang.Throwable -> L6d
            java.lang.String r1 = "updateTalkbackWatermark"
            r0.closeSurfaceTransaction(r1)     // Catch: java.lang.Throwable -> L6d
            monitor-exit(r6)
            return
        L63:
            r0 = move-exception
        L64:
            com.android.server.wm.WindowManagerService r1 = r6.mWms     // Catch: java.lang.Throwable -> L6d
            java.lang.String r2 = "updateTalkbackWatermark"
            r1.closeSurfaceTransaction(r2)     // Catch: java.lang.Throwable -> L6d
            throw r0     // Catch: java.lang.Throwable -> L6d
        L6d:
            r0 = move-exception
            monitor-exit(r6)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.TalkbackWatermark.showInternal():void");
    }

    private synchronized void hideInternal() {
        if (this.mSurfaceControl == null) {
            return;
        }
        this.mWms.openSurfaceTransaction();
        try {
            SurfaceControl.Transaction transaction = SurfaceControl.getGlobalTransaction();
            if (transaction != null) {
                try {
                    transaction.hide(this.mSurfaceControl);
                } catch (Throwable th) {
                    th = th;
                    this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
                    throw th;
                }
            }
            this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void dismissInternal() {
        if (this.mSurfaceControl == null) {
            return;
        }
        Slog.d(TAG, "talkback-test dismissInternal");
        hideInternal();
        this.mSurface.destroy();
        this.mBlastBufferQueue.destroy();
        this.mWms.openSurfaceTransaction();
        try {
            this.mSurfaceControl.reparent(null);
            this.mSurfaceControl.release();
            this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
            this.mBlastBufferQueue = null;
            this.mSurfaceControl = null;
            this.mSurface = null;
            this.mHasDrawn = false;
            this.mLastDH = 0;
            this.mLastDW = 0;
        } catch (Throwable th) {
            this.mWms.closeSurfaceTransaction("updateTalkbackWatermark");
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void refresh() {
        Slog.d(TAG, "talkback-test refresh");
        this.mWms.mH.post(new TalkbackWatermark$$ExternalSyntheticLambda2(this));
        this.mDrawNeeded = true;
        updateWaterMark();
    }
}
