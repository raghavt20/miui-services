package com.android.server.input;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.server.input.TouchWakeUpFeatureManager;
import com.android.server.input.config.InputCommonConfig;
import java.io.PrintWriter;
import miui.os.DeviceFeature;
import miui.util.ITouchFeature;
import miui.util.MiuiMultiDisplayTypeInfo;

/* loaded from: classes.dex */
public class TouchWakeUpFeatureManager {
    private static final String TAG = "TouchWakeUpFeatureManager";
    private static final int WAKEUP_OFF = 4;
    private static final int WAKEUP_ON = 5;
    private final Context mContext;
    private final Handler mHandler;
    private final SettingsObserver mSettingsObserver;
    private final ITouchFeature mTouchFeature;

    private TouchWakeUpFeatureManager() {
        this.mContext = ActivityThread.currentActivityThread().getSystemContext();
        Handler handler = new Handler(MiuiInputThread.getHandler().getLooper());
        this.mHandler = handler;
        this.mTouchFeature = ITouchFeature.getInstance();
        this.mSettingsObserver = new SettingsObserver(handler);
    }

    /* loaded from: classes.dex */
    private static final class TouchWakeUpFeatureManagerHolder {
        private static final TouchWakeUpFeatureManager sInstance = new TouchWakeUpFeatureManager();

        private TouchWakeUpFeatureManagerHolder() {
        }
    }

    public static TouchWakeUpFeatureManager getInstance() {
        return TouchWakeUpFeatureManagerHolder.sInstance;
    }

    public void onSystemBooted() {
        this.mSettingsObserver.register();
        this.mSettingsObserver.refreshAllSettings();
    }

    public void onUserSwitch(int userId) {
        this.mSettingsObserver.refreshAllSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onGestureWakeupSettingsChanged(boolean newState) {
        Slog.w(TAG, "On gesture wakeup settings changed, newState = " + newState);
        if (this.mTouchFeature.hasDoubleTapWakeUpSupport()) {
            setTouchMode(0, newState);
            if (MiuiMultiDisplayTypeInfo.isFoldDevice() || MiuiMultiDisplayTypeInfo.isFlipDevice()) {
                setTouchMode(1, newState);
                return;
            }
            return;
        }
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setWakeUpMode(newState ? 5 : 4);
        inputCommonConfig.flushToNative();
        Slog.w(TAG, "Flush wake up state to native, newState = " + newState);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSubScreenGestureWakeupSettingsChanged(boolean newState) {
        Slog.w(TAG, "On sub screen gesture wakeup settings changed, newState = " + newState);
        setTouchMode(1, newState);
    }

    private void setTouchMode(int i, boolean z) {
        Slog.w(TAG, "Update state to ITouchFeature, id = " + i + ", mode = 14, newState = " + (z ? 1 : 0));
        this.mTouchFeature.setTouchMode(i, 14, z ? 1 : 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            ContentResolver contentResolver = TouchWakeUpFeatureManager.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(Settings.System.getUriFor("gesture_wakeup"), false, this, -1);
            if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
                contentResolver.registerContentObserver(Settings.System.getUriFor("subscreen_gesture_wakeup"), false, this, -1);
            }
        }

        public void refreshAllSettings() {
            TouchWakeUpFeatureManager.this.mHandler.post(new Runnable() { // from class: com.android.server.input.TouchWakeUpFeatureManager$SettingsObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TouchWakeUpFeatureManager.SettingsObserver.this.refreshAllSettingsInternal();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void refreshAllSettingsInternal() {
            onChange(false, Settings.System.getUriFor("gesture_wakeup"));
            if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
                onChange(false, Settings.System.getUriFor("subscreen_gesture_wakeup"));
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver cr = TouchWakeUpFeatureManager.this.mContext.getContentResolver();
            if (Settings.System.getUriFor("gesture_wakeup").equals(uri)) {
                TouchWakeUpFeatureManager.this.onGestureWakeupSettingsChanged(Settings.System.getIntForUser(cr, "gesture_wakeup", 0, -2) != 0);
            } else if (Settings.System.getUriFor("subscreen_gesture_wakeup").equals(uri)) {
                TouchWakeUpFeatureManager.this.onSubScreenGestureWakeupSettingsChanged(Settings.System.getIntForUser(cr, "subscreen_gesture_wakeup", 0, -2) != 0);
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", prefix);
        ipw.println(TAG);
        ipw.increaseIndent();
        ipw.println("HasDoubleTapWakeUpSupport = " + this.mTouchFeature.hasDoubleTapWakeUpSupport());
        ipw.decreaseIndent();
    }
}
