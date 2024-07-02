package com.android.server.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import miui.app.IFreeformCallback;
import miui.app.MiuiFreeFormManager;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class GameAudioEnhancer {
    private static final String ADUIO_GAME_SOUND_EFFECT_SWITCH = "audio_game_sound_effect_switch";
    private static final String ADUIO_GAME_SOUND_NEW_EFFECT_SWITCH = "audio_game_sound_mode_switch";
    private static final String AUDIO_GAME_PACKAGE_NAME = "audio_game_package_name";
    public static final String GAME_MODE_ENABLE = "game_mode_enable";
    public static final String GAME_MODE_PKGS = "game_mode_packages";
    private static final int MSG_SET_PROCESS_LISTENER = 1;
    private static final int MSG_START_GAME_EFFECT = 2;
    private static final int MSG_STOP_GAME_EFFECT = 3;
    private static final String PM_SERVICE_NAME = "ProcessManager";
    private static final int QUERY_PM_SERVICE_DELAY_MS = 1000;
    private static final int QUERY_PM_SERVICE_MAX_TIMES = 20;
    private static final int SEND_PARAMETER_DELAY_MS = 500;
    private static final String TAG = "GameAudioEnhancer";
    private AudioManager mAudioManager;
    private final ContentResolver mContentResolver;
    private Context mContext;
    private WorkHandler mHandler;
    private int mQueryPMServiceTime;
    private final String[] LOCAL_ENABLED_PACKAGES = {"com.tencent.tmgp.cf", "com.tencent.mf.uam", "com.tencent.toaa", "com.tencent.tmgp.cod", "com.miHoYo.ys.mi", "com.tencent.tmgp.sgame"};
    private Set<String> mEnabledPackages = new ArraySet();
    private String mCurrentEnablePkg = "";
    private String mCurForegroundPkg = "";
    private String mFreeFormWindowPkg = "";
    private volatile boolean mParamSet = false;
    private int mGameModeSwitchStatus = 1;
    private int mFreeWinVersion = MiuiFreeFormManager.getMiuiFreeformVersion();
    private final Object mLock = new Object();
    private final IFreeformCallback mFreeformCallBack = new IFreeformCallback.Stub() { // from class: com.android.server.audio.GameAudioEnhancer.1
        public void dispatchFreeFormStackModeChanged(int action, MiuiFreeFormManager.MiuiFreeFormStackInfo stackInfo) {
            Log.d(GameAudioEnhancer.TAG, "Enter dispatchFreeFormStackModeChanged action: " + action);
            new ArrayList();
            GameAudioEnhancer.this.mFreeFormWindowPkg = "";
            if (GameAudioEnhancer.this.mGameModeSwitchStatus == 0 && !GameAudioEnhancer.this.mParamSet) {
                return;
            }
            int i = 0;
            switch (action) {
                case 0:
                case 1:
                case 21:
                    if (GameAudioEnhancer.this.mFreeWinVersion == 3) {
                        if (GameAudioEnhancer.this.mContext.getDisplay() != null) {
                            i = GameAudioEnhancer.this.mContext.getDisplay().getDisplayId();
                        }
                        List<MiuiFreeFormManager.MiuiFreeFormStackInfo> freeFormStackInfoList = MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay(i);
                        for (MiuiFreeFormManager.MiuiFreeFormStackInfo miuiFreeFormStackInfo : freeFormStackInfoList) {
                            GameAudioEnhancer.this.mFreeFormWindowPkg = miuiFreeFormStackInfo.packageName;
                            GameAudioEnhancer.this.setParamSend(miuiFreeFormStackInfo.packageName);
                        }
                        return;
                    }
                    if (GameAudioEnhancer.this.mFreeWinVersion == 2) {
                        String freeFormPkg = MiuiFreeFormManager.getMiuiFreeformStackPackageName(GameAudioEnhancer.this.mContext);
                        GameAudioEnhancer.this.mFreeFormWindowPkg = freeFormPkg;
                        GameAudioEnhancer.this.setParamSend(freeFormPkg);
                        return;
                    }
                    return;
                case 2:
                case 4:
                    if (GameAudioEnhancer.this.mFreeWinVersion == 3) {
                        if (GameAudioEnhancer.this.mContext.getDisplay() != null) {
                            i = GameAudioEnhancer.this.mContext.getDisplay().getDisplayId();
                        }
                        List<MiuiFreeFormManager.MiuiFreeFormStackInfo> freeFormStackInfoList2 = MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay(i);
                        for (MiuiFreeFormManager.MiuiFreeFormStackInfo miuiFreeFormStackInfo2 : freeFormStackInfoList2) {
                            if (GameAudioEnhancer.this.isPackageEnabled(miuiFreeFormStackInfo2.packageName)) {
                                GameAudioEnhancer.this.mFreeFormWindowPkg = miuiFreeFormStackInfo2.packageName;
                            }
                        }
                        return;
                    }
                    if (GameAudioEnhancer.this.mFreeWinVersion == 2) {
                        String freeFormPkg2 = MiuiFreeFormManager.getMiuiFreeformStackPackageName(GameAudioEnhancer.this.mContext);
                        if (GameAudioEnhancer.this.isPackageEnabled(freeFormPkg2)) {
                            GameAudioEnhancer.this.mFreeFormWindowPkg = freeFormPkg2;
                            return;
                        }
                        return;
                    }
                    return;
                case 3:
                case 5:
                    GameAudioEnhancer gameAudioEnhancer = GameAudioEnhancer.this;
                    gameAudioEnhancer.setParamSend(gameAudioEnhancer.mCurForegroundPkg);
                    return;
                default:
                    return;
            }
        }
    };
    private final IForegroundInfoListener.Stub mForegroundInfoListener = new IForegroundInfoListener.Stub() { // from class: com.android.server.audio.GameAudioEnhancer.2
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) throws RemoteException {
            Log.i(GameAudioEnhancer.TAG, "foreground change to " + foregroundInfo.mForegroundPackageName + ", last foreground is " + foregroundInfo.mLastForegroundPackageName);
            GameAudioEnhancer.this.mCurForegroundPkg = foregroundInfo.mForegroundPackageName;
            String LastForegroundPkg = foregroundInfo.mLastForegroundPackageName;
            if (GameAudioEnhancer.this.mParamSet && GameAudioEnhancer.this.isPackageEnabled(LastForegroundPkg)) {
                GameAudioEnhancer.this.mParamSet = false;
                GameAudioEnhancer.this.mHandler.sendMessageDelayed(GameAudioEnhancer.this.mHandler.obtainMessage(3), 500L);
            }
            GameAudioEnhancer gameAudioEnhancer = GameAudioEnhancer.this;
            gameAudioEnhancer.setParamSend(gameAudioEnhancer.mCurForegroundPkg);
        }
    };
    private SettingsObserver mSettingsObserver = new SettingsObserver();

    public GameAudioEnhancer(Context context, Looper looper) {
        this.mQueryPMServiceTime = 0;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mHandler = new WorkHandler(looper);
        this.mQueryPMServiceTime = 0;
        initSettingStatus();
        initEnabledPackages();
        initProcessListenerAsync();
        registerForegroundObserver();
    }

    private void registerForegroundObserver() {
        MiuiFreeFormManager.registerFreeformCallback(this.mFreeformCallBack);
        ProcessManager.registerForegroundInfoListener(this.mForegroundInfoListener);
    }

    private void unregisterForegroundObserver() {
        MiuiFreeFormManager.unregisterFreeformCallback(this.mFreeformCallBack);
        ProcessManager.unregisterForegroundInfoListener(this.mForegroundInfoListener);
    }

    /* loaded from: classes.dex */
    private class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler());
            GameAudioEnhancer.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(GameAudioEnhancer.GAME_MODE_ENABLE), false, this);
            GameAudioEnhancer.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(GameAudioEnhancer.GAME_MODE_PKGS), false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            super.onChange(selfChange);
            Uri audioEnhancerUri = Settings.Global.getUriFor(GameAudioEnhancer.GAME_MODE_ENABLE);
            if (uri == audioEnhancerUri) {
                GameAudioEnhancer.this.updateGameModeSettingstatus();
                return;
            }
            synchronized (GameAudioEnhancer.this.mLock) {
                GameAudioEnhancer.this.updateWhiteList();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGameModeSettingstatus() {
        try {
            int gameModeEnable = Settings.Global.getInt(this.mContentResolver, GAME_MODE_ENABLE, 0);
            setGameModeEnabled(gameModeEnable);
        } catch (Exception e) {
            Log.e(TAG, "updateGameModeSettingstatus: Exception " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWhiteList() {
        try {
            String gameModeEnablePkgs = Settings.Global.getString(this.mContentResolver, GAME_MODE_PKGS);
            setWhiteList(gameModeEnablePkgs);
        } catch (Exception e) {
            Log.e(TAG, "updateGameModeSettingstatus: Exception " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (ServiceManager.getService("ProcessManager") != null) {
                        ProcessManager.registerForegroundInfoListener(GameAudioEnhancer.this.mForegroundInfoListener);
                        return;
                    }
                    GameAudioEnhancer gameAudioEnhancer = GameAudioEnhancer.this;
                    int i = gameAudioEnhancer.mQueryPMServiceTime;
                    gameAudioEnhancer.mQueryPMServiceTime = i + 1;
                    if (i < 20) {
                        Log.w(GameAudioEnhancer.TAG, "process manager service not published, wait 1 second");
                        GameAudioEnhancer.this.initProcessListenerAsync();
                        return;
                    } else {
                        Log.e(GameAudioEnhancer.TAG, "failed to get ProcessManager service");
                        return;
                    }
                case 2:
                    GameAudioEnhancer.this.setGameMode(true);
                    return;
                case 3:
                    GameAudioEnhancer.this.setGameMode(false);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPackageEnabled(String pkgName) {
        synchronized (this.mLock) {
            return this.mEnabledPackages.contains(pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setParamSend(String pkgName) {
        if (this.mGameModeSwitchStatus == 1) {
            beOpened(pkgName);
        } else {
            beClosed(pkgName);
            unregisterForegroundObserver();
        }
    }

    private void beOpened(String pkgName) {
        if (!this.mParamSet && isPackageEnabled(pkgName)) {
            this.mParamSet = true;
            this.mCurrentEnablePkg = pkgName;
            WorkHandler workHandler = this.mHandler;
            workHandler.sendMessageDelayed(workHandler.obtainMessage(2), 500L);
            return;
        }
        if (this.mParamSet && !isPackageEnabled(pkgName)) {
            this.mParamSet = false;
            WorkHandler workHandler2 = this.mHandler;
            workHandler2.sendMessageDelayed(workHandler2.obtainMessage(3), 500L);
        }
    }

    private void beClosed(String pkgName) {
        if (this.mParamSet && isPackageEnabled(pkgName)) {
            this.mParamSet = false;
            WorkHandler workHandler = this.mHandler;
            workHandler.sendMessageDelayed(workHandler.obtainMessage(3), 500L);
        }
    }

    private void setWhiteList(String pkg) {
        this.mEnabledPackages.clear();
        initEnabledPackagesFromCloudSettings();
    }

    private void setGameModeEnabled(int enable) {
        this.mGameModeSwitchStatus = enable;
        if (this.mFreeFormWindowPkg.isEmpty()) {
            return;
        }
        setParamSend(this.mFreeFormWindowPkg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setGameMode(boolean on) {
        if (SystemProperties.getBoolean("ro.vendor.audio.game.mode", false)) {
            AudioSystem.setParameters("sendparam=AudioGameEnhance;audio_game_sound_mode_switch=" + (on ? "on;" : "off;") + AUDIO_GAME_PACKAGE_NAME + "=" + this.mCurrentEnablePkg);
            Log.i(TAG, "audio_game_sound_mode_switch=" + (on ? "on;" : "off;") + AUDIO_GAME_PACKAGE_NAME + "=" + this.mCurrentEnablePkg);
        } else {
            AudioSystem.setParameters("sendparam=AudioGameEnhance;audio_game_sound_effect_switch=" + (on ? "on;" : "off;") + AUDIO_GAME_PACKAGE_NAME + "=" + this.mCurrentEnablePkg);
            Log.i(TAG, "audio_game_sound_effect_switch=" + (on ? "on;" : "off;") + AUDIO_GAME_PACKAGE_NAME + "=" + this.mCurrentEnablePkg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initProcessListenerAsync() {
        WorkHandler workHandler = this.mHandler;
        workHandler.sendMessageDelayed(workHandler.obtainMessage(1), 1000L);
    }

    private void initSettingStatus() {
        try {
            Settings.Global.getInt(this.mContentResolver, GAME_MODE_ENABLE);
        } catch (Exception e) {
            Settings.Global.putInt(this.mContentResolver, GAME_MODE_ENABLE, 1);
        }
    }

    private void initEnabledPackages() {
        if (!initEnabledPackagesFromCloudSettings()) {
            initLocalPackages();
        }
    }

    private boolean initEnabledPackagesFromCloudSettings() {
        String pkgs = Settings.Global.getString(this.mContentResolver, GAME_MODE_PKGS);
        if (pkgs == null || pkgs.isEmpty()) {
            return false;
        }
        this.mEnabledPackages.clear();
        String strPkgs = pkgs.substring(1, pkgs.length() - 1);
        String[] jsonStrs = strPkgs.split(",");
        for (String jsonStr : jsonStrs) {
            this.mEnabledPackages.add(jsonStr.substring(1, jsonStr.length() - 1));
        }
        return true;
    }

    private void initLocalPackages() {
        Collections.addAll(this.mEnabledPackages, this.LOCAL_ENABLED_PACKAGES);
    }
}
