package com.miui.server.input.stylus.blocker;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.stylus.MiuiStylusUtils;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class MiuiEventBlockerManager {
    private static final String IS_IN_GAME_MODE_KEY = "gb_boosting";
    private static final String STYLUS_BLOCKER_CONFIG = "STYLUS_BLOCKER";
    private static final String SUPPORT_STYLUS_PALM_REJECT = "persist.stylus.palm.reject";
    private static final String TAG = MiuiEventBlockerManager.class.getSimpleName();
    private static volatile MiuiEventBlockerManager sInstance;
    private Context mContext;
    private String mCurrentFocusedPackageName;
    private Handler mHandler;
    private boolean mHasEditText;
    private boolean mInGameMode;
    private boolean mNativeEnableState;
    private boolean mNativeStrongMode = true;
    private SettingsObserver mSettingsObserver;
    private StylusBlockerConfig mStylusBlockerConfig;
    private boolean mStylusConnected;

    private MiuiEventBlockerManager() {
    }

    public static MiuiEventBlockerManager getInstance() {
        if (sInstance == null) {
            synchronized (MiuiEventBlockerManager.class) {
                if (sInstance == null) {
                    sInstance = new MiuiEventBlockerManager();
                }
            }
        }
        return sInstance;
    }

    public void onSystemBooted() {
        if (!MiuiStylusUtils.isSupportStylusPalmReject()) {
            return;
        }
        this.mContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mHandler = new Handler(MiuiInputThread.getHandler().getLooper());
        this.mStylusBlockerConfig = new StylusBlockerConfig();
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.stylus.blocker.MiuiEventBlockerManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiEventBlockerManager.this.lambda$onSystemBooted$0();
            }
        });
        SettingsObserver settingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver = settingsObserver;
        settingsObserver.observer();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onSystemBooted$0() {
        this.mStylusBlockerConfig = MiuiEventBlockerUtils.getDefaultStylusBlockerConfig();
    }

    public void onUserSwitch(int userId) {
        if (isNotInit()) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.stylus.blocker.MiuiEventBlockerManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiEventBlockerManager.this.updateSettingsConfig();
            }
        });
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observer() {
            ContentResolver contentResolver = MiuiEventBlockerManager.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("gb_boosting"), false, this, -1);
            Handler handler = MiuiEventBlockerManager.this.mHandler;
            final MiuiEventBlockerManager miuiEventBlockerManager = MiuiEventBlockerManager.this;
            handler.post(new Runnable() { // from class: com.miui.server.input.stylus.blocker.MiuiEventBlockerManager$SettingsObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiEventBlockerManager.this.updateSettingsConfig();
                }
            });
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (MiuiSettings.SettingsCloudData.getCloudDataNotifyUri().equals(uri)) {
                MiuiEventBlockerManager.this.updateCloudConfig();
            } else if (Settings.Secure.getUriFor("gb_boosting").equals(uri)) {
                MiuiEventBlockerManager.this.updateGameModeSettings();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSettingsConfig() {
        updateCloudConfig();
        updateGameModeSettings();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCloudConfig() {
        MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), STYLUS_BLOCKER_CONFIG, (String) null, (String) null, false);
        if (cloudData == null || cloudData.json() == null) {
            Slog.w(TAG, "Not has cloud config, use default");
        } else {
            StylusBlockerConfig cloudConfig = MiuiEventBlockerUtils.parseJsonToStylusBlockerConfig(cloudData.json());
            if (cloudConfig.getVersion() < this.mStylusBlockerConfig.getVersion()) {
                Slog.w(TAG, "Do not use cloud config, because " + cloudConfig.getVersion() + " < " + this.mStylusBlockerConfig.getVersion());
                return;
            } else {
                this.mStylusBlockerConfig = cloudConfig;
                Slog.w(TAG, "Use cloud config.");
            }
        }
        updateEnableStateToNative();
        updateThresholdToNative(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGameModeSettings() {
        this.mInGameMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gb_boosting", 0, -2) == 1;
        Slog.w(TAG, "Update game mode settings, current is in game mode " + this.mInGameMode);
        updateEnableStateToNative();
    }

    public void onStylusConnectionStateChanged(boolean connect) {
        if (isNotInit()) {
            return;
        }
        this.mStylusConnected = connect;
        Slog.w(TAG, "Stylus connect state changed, connect = " + connect);
        updateEnableStateToNative();
    }

    public void onFocusedWindowChanged(WindowManagerPolicy.WindowState newFocus) {
        String newFocusedPackageName;
        if (isNotInit() || newFocus == null || (newFocusedPackageName = newFocus.getOwningPackage()) == null || newFocusedPackageName.equals(this.mCurrentFocusedPackageName)) {
            return;
        }
        this.mCurrentFocusedPackageName = newFocusedPackageName;
        updateEnableStateToNative();
    }

    private boolean getCurrentStylusBlockerState() {
        if (this.mStylusBlockerConfig.isEnable() && !this.mInGameMode && this.mStylusConnected) {
            return !this.mStylusBlockerConfig.getBlockSet().contains(this.mCurrentFocusedPackageName);
        }
        return false;
    }

    private void updateEnableStateToNative() {
        boolean newState = getCurrentStylusBlockerState();
        if (this.mNativeEnableState == newState) {
            return;
        }
        this.mNativeEnableState = newState;
        InputCommonConfig.getInstance().setStylusBlockerEnable(this.mNativeEnableState);
        InputCommonConfig.getInstance().flushToNative();
        Slog.w(TAG, "Flush enable state to native, enable = " + this.mNativeEnableState);
    }

    public void setHasEditTextOnScreen(boolean hasEditText) {
        if (isNotInit()) {
            return;
        }
        this.mHasEditText = hasEditText;
        updateThresholdToNative(false);
    }

    private void updateThresholdToNative(boolean force) {
        if (!force && this.mHasEditText == this.mNativeStrongMode) {
            return;
        }
        boolean z = this.mHasEditText;
        this.mNativeStrongMode = z;
        int delayTime = z ? this.mStylusBlockerConfig.getStrongModeDelayTime() : this.mStylusBlockerConfig.getWeakModeDelayTime();
        float moveThreshold = this.mNativeStrongMode ? this.mStylusBlockerConfig.getStrongModeMoveThreshold() : this.mStylusBlockerConfig.getWeakModeMoveThreshold();
        Slog.w(TAG, "Flush delay time and move threshold to native, delayTime = " + delayTime + ", modeThreshold = " + moveThreshold);
        InputCommonConfig.getInstance().setStylusBlockerDelayTime(delayTime);
        InputCommonConfig.getInstance().setStylusBlockerMoveThreshold(moveThreshold);
        InputCommonConfig.getInstance().flushToNative();
    }

    private boolean isNotInit() {
        return this.mContext == null;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.print(prefix);
        if (isNotInit()) {
            pw.println("not support");
            return;
        }
        pw.print("init = ");
        pw.println(this.mContext != null);
        pw.print(prefix);
        pw.print("mCurrentFocusedWindow = ");
        String str = this.mCurrentFocusedPackageName;
        if (str == null) {
            str = "null";
        }
        pw.println(str);
        pw.print(prefix);
        pw.print("mStylusConnected = ");
        pw.println(this.mStylusConnected);
        pw.print(prefix);
        pw.print("mHasEditText = ");
        pw.println(this.mHasEditText);
        pw.print(prefix);
        pw.print("mInGameMode = ");
        pw.println(this.mInGameMode);
        pw.print(prefix);
        pw.print("mNativeEnableState = ");
        pw.println(this.mNativeEnableState);
        pw.print(prefix);
        pw.print("mNativeStrongMode = ");
        pw.println(this.mNativeStrongMode);
        pw.print(prefix);
        pw.println("mStylusBlockerConfig");
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mVersion = ");
        pw.println(this.mStylusBlockerConfig.getVersion());
        pw.print(prefix2);
        pw.print("mEnable = ");
        pw.println(this.mStylusBlockerConfig.isEnable());
        pw.print(prefix2);
        pw.print("mWeakModeDelayTime = ");
        pw.println(this.mStylusBlockerConfig.getWeakModeDelayTime());
        pw.print(prefix2);
        pw.print("mWeakModeMoveThreshold = ");
        pw.println(this.mStylusBlockerConfig.getWeakModeMoveThreshold());
        pw.print(prefix2);
        pw.print("mStrongModeDelayTime = ");
        pw.println(this.mStylusBlockerConfig.getStrongModeDelayTime());
        pw.print(prefix2);
        pw.print("mStrongModeMoveThreshold = ");
        pw.println(this.mStylusBlockerConfig.getStrongModeMoveThreshold());
        pw.print(prefix2);
        pw.print("mBlockedList.size = ");
        pw.println(this.mStylusBlockerConfig.getBlockSet().size());
    }
}
