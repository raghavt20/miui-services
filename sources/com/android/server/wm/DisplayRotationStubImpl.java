package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.NetworkBoost.StatusManager;
import java.io.PrintWriter;
import java.util.Iterator;

/* loaded from: classes.dex */
public class DisplayRotationStubImpl implements DisplayRotationStub {
    public static final int BOOT_SENSOR_ROTATION_INVALID = -1;
    private static final String TAG = "WindowManager";
    private SurfaceControl mBootBlackCoverLayer;
    private Context mContext;
    private MiuiSettingsObserver mMiuiSettingsObserver;
    private WindowManagerService mService;
    private int mUserRotationModeOuter = 1;
    private int mUserRotationOuter = 0;
    private int mUserRotationModeInner = 0;
    private int mUserRotationInner = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayRotationStubImpl> {

        /* compiled from: DisplayRotationStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayRotationStubImpl INSTANCE = new DisplayRotationStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayRotationStubImpl m2471provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayRotationStubImpl m2470provideNewInstance() {
            return new DisplayRotationStubImpl();
        }
    }

    public void init(WindowManagerService wms, Context context) {
        this.mContext = context;
        this.mService = wms;
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(UiThread.getHandler());
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
    }

    public boolean forceEnableSeamless(WindowState w) {
        if (w != null && w.mAttrs != null && (w.mAttrs.extraFlags & 1073741824) != 0) {
            return true;
        }
        return false;
    }

    public void cancelAppAnimationIfNeeded(DisplayContent mDisplayContent) {
        if (mDisplayContent.isAnimating(4, 1)) {
            Slog.d(TAG, "prepareNormalRA apptransition still animating");
            if (mDisplayContent.mAppTransition.mLastOpeningAnimationTargets != null) {
                Iterator it = mDisplayContent.mAppTransition.mLastOpeningAnimationTargets.iterator();
                while (it.hasNext()) {
                    WindowContainer animatingContainer = (WindowContainer) it.next();
                    animatingContainer.cancelAnimation();
                    if (ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
                        Slog.d(TAG, "prepareNormalRA cancelAnimation " + animatingContainer);
                    }
                }
            }
            if (mDisplayContent.mAppTransition.mLastClosingAnimationTargets != null) {
                Iterator it2 = mDisplayContent.mAppTransition.mLastClosingAnimationTargets.iterator();
                while (it2.hasNext()) {
                    WindowContainer animatingContainer2 = (WindowContainer) it2.next();
                    animatingContainer2.cancelAnimation();
                    if (ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
                        Slog.d(TAG, "prepareNormalRA cancelAnimation " + animatingContainer2);
                    }
                }
            }
        }
    }

    public void updateRotation(DisplayContent dc, int lastRotation) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null) {
            miuiHoverIn.updateLastRotation(dc, lastRotation);
        }
    }

    public int getHoverModeRecommendRotation(DisplayContent displayContent) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null) {
            return miuiHoverIn.getHoverModeRecommendRotation(displayContent);
        }
        return -1;
    }

    public boolean shouldHoverModeEnableSensor(DisplayContent displayContent) {
        MiuiHoverModeInternal miuiHoverIn = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        if (miuiHoverIn != null) {
            return miuiHoverIn.shouldHoverModeEnableSensor(displayContent);
        }
        return false;
    }

    public void updateRotationMode() {
        int userRotationMode;
        if (MiuiOrientationStub.get().isEnabled() && MiuiOrientationStub.IS_FOLD) {
            ContentResolver resolver = this.mContext.getContentResolver();
            int userRotation = Settings.System.getIntForUser(resolver, "user_rotation", 0, -2);
            if (Settings.System.getIntForUser(resolver, "accelerometer_rotation", 0, -2) != 0) {
                userRotationMode = 0;
            } else {
                userRotationMode = 1;
            }
            boolean shouldUpdateRotationMode = false;
            int userRotationModeOuter = getUserRotationModeFromSettings(true);
            int userOrientationOuter = getUserRotationFromSettings(true);
            if (this.mUserRotationModeOuter != userRotationModeOuter || this.mUserRotationOuter != userOrientationOuter) {
                this.mUserRotationModeOuter = userRotationModeOuter;
                this.mUserRotationOuter = userOrientationOuter;
                if (this.mService.mPolicy.isDisplayFolded() && (this.mUserRotationModeOuter != userRotationMode || this.mUserRotationOuter != userRotation)) {
                    shouldUpdateRotationMode = true;
                }
            }
            int userRotationModeInner = getUserRotationModeFromSettings(false);
            int userOrientationInner = getUserRotationFromSettings(false);
            if (this.mUserRotationModeInner != userRotationModeInner || this.mUserRotationInner != userOrientationInner) {
                this.mUserRotationModeInner = userRotationModeInner;
                this.mUserRotationInner = userOrientationInner;
                if (!this.mService.mPolicy.isDisplayFolded() && (this.mUserRotationModeInner != userRotationMode || this.mUserRotationInner != userRotation)) {
                    shouldUpdateRotationMode = true;
                }
            }
            if (shouldUpdateRotationMode) {
                setUserRotation();
            }
        }
    }

    public void setUserRotation() {
        if (this.mService.mPolicy.isDisplayFolded()) {
            setUserRotation(this.mUserRotationModeOuter, this.mUserRotationOuter);
        } else {
            setUserRotation(this.mUserRotationModeInner, this.mUserRotationInner);
        }
    }

    private void setUserRotation(int userRotationMode, int userRotation) {
        synchronized (this.mService.mGlobalLock) {
            ContentResolver res = this.mContext.getContentResolver();
            int accelerometerRotation = userRotationMode == 1 ? 0 : 1;
            Settings.System.putIntForUser(res, "accelerometer_rotation", accelerometerRotation, -2);
            Settings.System.putIntForUser(res, "user_rotation", userRotation, -2);
        }
    }

    public void setUserRotationForFold(int currRotation, boolean enable, int rotation, boolean isFolded) {
        int rotation2 = rotation == -1 ? currRotation : rotation;
        int userRotationMode = enable ? 1 : 0;
        int accelerometerRotation = userRotationMode != 1 ? 1 : 0;
        setUserRotationForFold(userRotationMode, rotation2, isFolded);
        setUserRotationLockedForSettings(accelerometerRotation, rotation2, isFolded);
    }

    private void setUserRotationForFold(int userRotationMode, int userRotation, boolean isFolded) {
        if (MiuiOrientationStub.get().isEnabled() && MiuiOrientationStub.IS_FOLD) {
            if (isFolded) {
                this.mUserRotationModeOuter = userRotationMode;
                this.mUserRotationOuter = userRotation;
            } else {
                this.mUserRotationModeInner = userRotationMode;
                this.mUserRotationInner = userRotation;
            }
        }
    }

    public boolean isRotationLocked(boolean isFolded) {
        return (isFolded ? this.mUserRotationModeOuter : this.mUserRotationModeInner) == 1;
    }

    public int getUserRotationModeFromSettings(boolean isFolded) {
        ContentResolver resolver = this.mContext.getContentResolver();
        return isFolded ? Settings.System.getIntForUser(resolver, "accelerometer_rotation_outer", 0, -2) != 0 ? 0 : 1 : Settings.System.getIntForUser(resolver, "accelerometer_rotation_inner", 1, -2) != 0 ? 0 : 1;
    }

    public int getUserRotationFromSettings(boolean isFolded) {
        ContentResolver resolver = this.mContext.getContentResolver();
        return isFolded ? Settings.System.getIntForUser(resolver, "user_rotation_outer", 0, -2) : Settings.System.getIntForUser(resolver, "user_rotation_inner", 0, -2);
    }

    public void setUserRotationLockedForSettings(int userRotationMode, int userRotation, boolean isFolded) {
        ContentResolver res = this.mContext.getContentResolver();
        if (isFolded) {
            Settings.System.putIntForUser(res, "accelerometer_rotation_outer", userRotationMode, -2);
            Settings.System.putIntForUser(res, "user_rotation_outer", userRotation, -2);
        } else {
            Settings.System.putIntForUser(res, "accelerometer_rotation_inner", userRotationMode, -2);
            Settings.System.putIntForUser(res, "user_rotation_inner", userRotation, -2);
        }
    }

    public void dumpDoubleSwitch(String prefix, PrintWriter pw) {
        pw.print(prefix + "  mUserRotationModeOuter=" + WindowManagerPolicy.userRotationModeToString(this.mUserRotationModeOuter));
        pw.print(" mUserRotationOuter=" + Surface.rotationToString(this.mUserRotationOuter));
        pw.println(prefix + "  mUserRotationModeInner=" + WindowManagerPolicy.userRotationModeToString(this.mUserRotationModeInner));
        pw.print(" mUserRotationInner=" + Surface.rotationToString(this.mUserRotationInner));
    }

    public void writeRotationChangeToProperties(int rotation) {
        int currRotation = -1;
        switch (rotation) {
            case 0:
                currRotation = 0;
                break;
            case 1:
                currRotation = 90;
                break;
            case 2:
                currRotation = 180;
                break;
            case 3:
                currRotation = 270;
                break;
        }
        if (currRotation < 0) {
            return;
        }
        final int finalRotation = currRotation;
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() { // from class: com.android.server.wm.DisplayRotationStubImpl.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    SystemProperties.set("sys.tp.grip_enable", Integer.toString(finalRotation));
                } catch (Exception e) {
                    Slog.e(DisplayRotationStubImpl.TAG, "set SystemProperties error.", e);
                }
            }
        });
    }

    public boolean isProvisioned() {
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Secure.getInt(resolver, "device_provisioned", 0) != 0;
    }

    public void writeRotationChangeToPropertiesForDisplay(int rotation) {
        int currRotation = -1;
        switch (rotation) {
            case 0:
                currRotation = 0;
                break;
            case 1:
                currRotation = 90;
                break;
            case 2:
                currRotation = 180;
                break;
            case 3:
                currRotation = 270;
                break;
        }
        if (currRotation < 0) {
            return;
        }
        final int finalRotation = currRotation;
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() { // from class: com.android.server.wm.DisplayRotationStubImpl.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    SystemProperties.set("debug.tp.grip_enable", Integer.toString(finalRotation));
                } catch (Exception e) {
                    Slog.e(DisplayRotationStubImpl.TAG, "set SystemProperties error.", e);
                }
            }
        });
    }

    public int getBootSensorRotation() {
        int bootSensorRotation = SystemProperties.getInt("ro.boot.ori", 1);
        switch (bootSensorRotation) {
            case 1:
                return 1;
            case 2:
                return 0;
            case 3:
                return 3;
            case 4:
                return 2;
            default:
                return 1;
        }
    }

    public boolean needBootBlackCoverLayer() {
        return MiuiOrientationStub.IS_TABLET && SystemProperties.getInt("ro.boot.ori", -1) != -1;
    }

    public boolean hasBootBlackCoverLayer() {
        return this.mBootBlackCoverLayer != null;
    }

    public void addBootBlackCoverLayer() {
        if (!needBootBlackCoverLayer() || hasBootBlackCoverLayer()) {
            return;
        }
        SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mService.mTransactionFactory.get();
        DisplayContent displayContent = this.mService.getDefaultDisplayContentLocked();
        SurfaceControl build = displayContent.makeOverlay().setName("BootBlackCoverLayerSurface").setColorLayer().setCallsite("BootAnimation").build();
        this.mBootBlackCoverLayer = build;
        t.setLayer(build, 2010001);
        t.setAlpha(this.mBootBlackCoverLayer, 1.0f);
        t.show(this.mBootBlackCoverLayer);
        t.apply();
        Slog.d("DisplayRotationImpl", "addBootBlackCoverLayerSurface");
    }

    public void removeBootBlackCoverLayer() {
        if (this.mBootBlackCoverLayer != null) {
            SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mService.mTransactionFactory.get();
            if (this.mBootBlackCoverLayer.isValid()) {
                t.remove(this.mBootBlackCoverLayer);
            }
            this.mBootBlackCoverLayer = null;
            t.apply();
            Slog.d("DisplayRotationImpl", "removeBootBlackCoverLayerSurface");
        }
    }

    public void checkBootBlackCoverLayer() {
        if (!needBootBlackCoverLayer() || !hasBootBlackCoverLayer()) {
            return;
        }
        DisplayContent displayContent = this.mService.getDefaultDisplayContentLocked();
        if (displayContent.getRotation() == displayContent.getWindowConfiguration().getRotation()) {
            removeBootBlackCoverLayer();
        } else {
            this.mService.mH.sendNewMessageDelayed(StatusManager.CALLBACK_NETWORK_PRIORITY_CHANGED, displayContent, 2000L);
        }
    }

    public void immediatelyRemoveBootLayer() {
        if (!needBootBlackCoverLayer() || !hasBootBlackCoverLayer()) {
            return;
        }
        removeBootBlackCoverLayer();
        this.mService.mH.removeMessages(StatusManager.CALLBACK_NETWORK_PRIORITY_CHANGED, this.mService.getDefaultDisplayContentLocked());
    }

    /* loaded from: classes.dex */
    private class MiuiSettingsObserver extends ContentObserver {
        MiuiSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = DisplayRotationStubImpl.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation_outer"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("user_rotation_outer"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation_inner"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("user_rotation_inner"), false, this, -1);
            DisplayRotationStubImpl.this.updateRotationMode();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            DisplayRotationStubImpl.this.updateRotationMode();
        }
    }
}
