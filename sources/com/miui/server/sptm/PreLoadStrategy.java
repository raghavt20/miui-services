package com.miui.server.sptm;

import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import com.xiaomi.abtest.d.i;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.process.LifecycleConfig;
import miui.process.ProcessManagerNative;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PreLoadStrategy implements SpeedTestModeServiceImpl.Strategy {
    private static final String PUBG_PACKAGE_NAME = "com.tencent.tmgp.pubgmhd";
    private static final String SGAME_PACKAGE_NAME = "com.tencent.tmgp.sgame";
    private static final String STOP_TIME_PACKAGES_PREFIX = "persist.sys.miui_sptm.";
    private boolean mIsInSpeedTestMode;
    private boolean mIsLowMemoryDevice;
    private int mLastPreloadStartedAppCount;
    private int mNextPreloadTargetIndex;
    private int mNormalAppCount;
    private int mNormalAppCountNew;
    private ProcessManagerInternal mPMS;
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();
    private final String TAG = SpeedTestModeServiceImpl.TAG;
    private final boolean DEBUG = SpeedTestModeServiceImpl.DEBUG;
    private PreloadConfigs mPreloadConfigs = new PreloadConfigs();
    private HashMap<String, AppStartRecord> mAppStartedRecords = new HashMap<>();
    private int mPreloadType = this.mSpeedTestModeService.getPreloadCloudType();
    private int mNormalAppCountOld = SpeedTestModeServiceImpl.PRELOAD_APPS.size() - SpeedTestModeServiceImpl.PRELOAD_GAME_APPS.size();

    /* JADX INFO: Access modifiers changed from: package-private */
    public PreLoadStrategy() {
        int size = SpeedTestModeServiceImpl.PRELOAD_APPS_NEW.size();
        this.mNormalAppCountNew = size;
        this.mNormalAppCount = this.mPreloadType <= 3 ? this.mNormalAppCountOld : size;
        this.mLastPreloadStartedAppCount = 0;
        this.mNextPreloadTargetIndex = 0;
        this.mIsInSpeedTestMode = false;
        this.mIsLowMemoryDevice = SpeedTestModeServiceImpl.TOTAL_MEMORY < 8192;
    }

    public void onPreloadAppStarted(String packageName) {
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onNewEvent(int eventType) {
        ProcessManagerInternal processManagerInternal;
        if (eventType == 5 && (processManagerInternal = this.mPMS) != null) {
            processManagerInternal.setSpeedTestState(false);
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onAppStarted(AppStartRecord r) {
        if (SpeedTestModeServiceImpl.SPEED_TEST_APP_LIST.contains(r.packageName) && this.mPreloadType != 0 && !this.mAppStartedRecords.containsKey(r.packageName)) {
            this.mAppStartedRecords.put(r.packageName, r);
            preloadNextApps();
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onSpeedTestModeChanged(boolean isEnable) {
        this.mIsInSpeedTestMode = isEnable;
        if (this.mPMS == null) {
            this.mPMS = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        }
        int i = this.mPreloadType;
        if (i == 0) {
            return;
        }
        if (isEnable) {
            preloadNextApps();
            ProcessManagerInternal processManagerInternal = this.mPMS;
            if (processManagerInternal != null) {
                processManagerInternal.setSpeedTestState(isEnable);
                return;
            }
            return;
        }
        if ((i > 3 && this.mPMS != null && this.mAppStartedRecords.size() < SpeedTestModeServiceImpl.STARTED_APPCOUNT) || (this.mPreloadType <= 3 && this.mPMS != null && this.mAppStartedRecords.size() < 14)) {
            this.mPMS.setSpeedTestState(isEnable);
        }
        reset();
    }

    private void preloadNextApps() {
        if (!this.mIsInSpeedTestMode) {
            return;
        }
        int appStartedCount = this.mAppStartedRecords.size();
        int preloadThreshold = SpeedTestModeServiceImpl.PRELOAD_THRESHOLD;
        if (!this.mIsLowMemoryDevice && this.mPreloadType > 2 && this.mNextPreloadTargetIndex <= this.mNormalAppCountNew) {
            preloadThreshold = 2;
        }
        int nextPreloadAppStartedThreshold = this.mLastPreloadStartedAppCount;
        if (nextPreloadAppStartedThreshold != 0) {
            nextPreloadAppStartedThreshold += preloadThreshold;
        }
        if (this.DEBUG) {
            Slog.d(this.TAG, String.format("preloadNextApps: cur_apps: %s, threshold: %s, pre_apps: %s", Integer.valueOf(appStartedCount), Integer.valueOf(nextPreloadAppStartedThreshold), Integer.valueOf(this.mNormalAppCountNew)));
        }
        if (appStartedCount < nextPreloadAppStartedThreshold) {
            return;
        }
        this.mLastPreloadStartedAppCount = appStartedCount;
        int i = this.mPreloadType;
        if (i > 3 && !this.mIsLowMemoryDevice) {
            if (this.mNextPreloadTargetIndex < SpeedTestModeServiceImpl.PRELOAD_APPS_NEW.size()) {
                List<String> list = SpeedTestModeServiceImpl.PRELOAD_APPS_NEW;
                int i2 = this.mNextPreloadTargetIndex;
                this.mNextPreloadTargetIndex = i2 + 1;
                preloadPackage(list.get(i2));
                return;
            }
            return;
        }
        if (i > 2 && !this.mIsLowMemoryDevice) {
            if (this.mNextPreloadTargetIndex < SpeedTestModeServiceImpl.PRELOAD_APPS.size()) {
                List<String> list2 = SpeedTestModeServiceImpl.PRELOAD_APPS;
                int i3 = this.mNextPreloadTargetIndex;
                this.mNextPreloadTargetIndex = i3 + 1;
                preloadPackage(list2.get(i3));
                return;
            }
            return;
        }
        if (i > 0 && this.mNextPreloadTargetIndex < SpeedTestModeServiceImpl.PRELOAD_GAME_APPS.size()) {
            List<String> list3 = SpeedTestModeServiceImpl.PRELOAD_GAME_APPS;
            int i4 = this.mNextPreloadTargetIndex;
            this.mNextPreloadTargetIndex = i4 + 1;
            preloadPackage(list3.get(i4));
        }
    }

    private void preloadPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        try {
            LifecycleConfig lifecycleConfig = this.mPreloadConfigs.find(packageName);
            if (lifecycleConfig != null) {
                int res = ProcessManagerNative.getDefault().startPreloadApp(packageName, true, false, lifecycleConfig);
                if (this.DEBUG) {
                    Slog.d(this.TAG, String.format("preloadNextApps: preload: %s, res=%s", packageName, Integer.valueOf(res)));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        this.mAppStartedRecords.clear();
        this.mLastPreloadStartedAppCount = 0;
        this.mNextPreloadTargetIndex = 0;
    }

    /* loaded from: classes.dex */
    public static class AppStartRecord {
        String packageName = null;
        boolean isColdStart = false;

        public String toString() {
            return "AppStartRecord{packageName='" + this.packageName + "', isColdStart=" + this.isColdStart + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PreloadConfigs {
        private HashMap<String, LifecycleConfig> mConfigs = new HashMap<>();

        public PreloadConfigs() {
            resetPreloadConfigs();
            if (PreLoadStrategy.this.DEBUG) {
                for (Map.Entry<String, LifecycleConfig> entry : this.mConfigs.entrySet()) {
                    Slog.d(PreLoadStrategy.this.TAG, "preload configs:" + entry.getKey() + " " + entry.getValue().getStopTimeout());
                }
            }
        }

        private void resetPreloadConfigs() {
            this.mConfigs.clear();
            if (PreLoadStrategy.this.mPreloadType > 3) {
                if (!PreLoadStrategy.this.mIsLowMemoryDevice) {
                    for (int i = 0; i < PreLoadStrategy.this.mNormalAppCount; i++) {
                        SpeedTestModeServiceImpl unused = PreLoadStrategy.this.mSpeedTestModeService;
                        setPreloadAppConfig(SpeedTestModeServiceImpl.PRELOAD_APPS_NEW.get(i), 3000);
                    }
                }
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, 20000);
                setPreloadAppConfig(PreLoadStrategy.PUBG_PACKAGE_NAME, 16000);
            } else if (PreLoadStrategy.this.mPreloadType == 3) {
                if (!PreLoadStrategy.this.mIsLowMemoryDevice) {
                    for (int i2 = 0; i2 < PreLoadStrategy.this.mNormalAppCount; i2++) {
                        SpeedTestModeServiceImpl unused2 = PreLoadStrategy.this.mSpeedTestModeService;
                        setPreloadAppConfig(SpeedTestModeServiceImpl.PRELOAD_APPS.get(i2), 3000);
                    }
                }
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, 20000);
                setPreloadAppConfig(PreLoadStrategy.PUBG_PACKAGE_NAME, 16000);
            } else if (PreLoadStrategy.this.mPreloadType == 2) {
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, i.b);
                setPreloadAppConfig(PreLoadStrategy.PUBG_PACKAGE_NAME, i.b);
            } else if (PreLoadStrategy.this.mPreloadType == 1) {
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, 16000);
            }
            loadProperty();
        }

        private void setPreloadAppConfig(String preloadPkg, int timeOut) {
            LifecycleConfig config = LifecycleConfig.create(1);
            config.setStopTimeout(timeOut);
            SpeedTestModeServiceImpl unused = PreLoadStrategy.this.mSpeedTestModeService;
            config.setSchedAffinity(0, SpeedTestModeServiceImpl.SPTM_LOW_MEMORY_DEVICE_PRELOAD_CORE);
            this.mConfigs.put(preloadPkg, config);
        }

        private void loadProperty() {
            for (Map.Entry<String, LifecycleConfig> entry : this.mConfigs.entrySet()) {
                long stopTimeout = SystemProperties.getLong(PreLoadStrategy.STOP_TIME_PACKAGES_PREFIX + entry.getKey(), -1L);
                if (stopTimeout > 0) {
                    entry.getValue().setStopTimeout(stopTimeout);
                }
            }
        }

        public LifecycleConfig find(String packageName) {
            resetPreloadConfigs();
            LifecycleConfig lifecycleConfig = this.mConfigs.get(packageName);
            return lifecycleConfig;
        }
    }
}
