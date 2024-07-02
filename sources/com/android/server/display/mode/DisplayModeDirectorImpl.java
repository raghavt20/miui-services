package com.android.server.display.mode;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.display.mode.DisplayModeDirector;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

/* loaded from: classes.dex */
public class DisplayModeDirectorImpl implements DisplayModeDirectorStub {
    private static final String EXTERNAL_DISPLAY_CONNECTED = "external_display_connected";
    private static final int HISTORY_COUNT_FOR_HIGH_RAM_DEVICE = 30;
    private static final int HISTORY_COUNT_FOR_LOW_RAM_DEVICE = 10;
    public static final String MIUI_OPTIMIZATION = "miui_optimization";
    public static final String MIUI_OPTIMIZATION_PROP = "persist.sys.miui_optimization";
    public static final String MIUI_REFRESH_RATE = "miui_refresh_rate";
    public static final String MIUI_THERMAL_LIMIT_REFRESH_RATE = "thermal_limit_refresh_rate";
    public static final String MIUI_USER_REFRESH_RATE = "user_refresh_rate";
    private static final String TAG = "DisplayModeDirectorImpl";
    private DisplayModeDirector.BrightnessObserver mBrightnessObserver;
    private Context mContext;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private DisplayModeDirector mDisplayModeDirector;
    private DisplayModeDirectorEntry[] mDisplayModeDirectorHistory;
    private boolean mExternalDisplayConnected;
    private final Uri mExternalDisplayRefreshRateSetting;
    private int mHistoryCount;
    private int mHistoryIndex;
    private Object mLock;
    private final Uri mMiuiOptimizationSetting;
    private boolean mMiuiRefreshRateEnable;
    private final Uri mMiuiRefreshRateSetting;
    private float mSceneMaxRefreshRate;
    private DisplayModeDirector.SettingsObserver mSettingsObserver;
    private final Uri mThermalRefreshRateSetting;
    private VotesStorage mVotesStorage;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayModeDirectorImpl> {

        /* compiled from: DisplayModeDirectorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayModeDirectorImpl INSTANCE = new DisplayModeDirectorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayModeDirectorImpl m1333provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayModeDirectorImpl m1332provideNewInstance() {
            return new DisplayModeDirectorImpl();
        }
    }

    public DisplayModeDirectorImpl() {
        this.mHistoryCount = ActivityManager.isLowRamDeviceStatic() ? 10 : 30;
        this.mHistoryIndex = 0;
        this.mMiuiRefreshRateSetting = Settings.Secure.getUriFor(MIUI_REFRESH_RATE);
        this.mThermalRefreshRateSetting = Settings.System.getUriFor(MIUI_THERMAL_LIMIT_REFRESH_RATE);
        this.mMiuiOptimizationSetting = Settings.Secure.getUriFor(MIUI_OPTIMIZATION);
        this.mExternalDisplayRefreshRateSetting = Settings.System.getUriFor(EXTERNAL_DISPLAY_CONNECTED);
    }

    public void init(DisplayModeDirector modeDirector, Object lock, DisplayModeDirector.BrightnessObserver brightnessObserver, DisplayModeDirector.SettingsObserver settingsObserver, Context context, VotesStorage votesStorage) {
        this.mLock = lock;
        this.mDisplayModeDirector = modeDirector;
        this.mDisplayModeDirectorHistory = new DisplayModeDirectorEntry[this.mHistoryCount];
        this.mBrightnessObserver = brightnessObserver;
        this.mSettingsObserver = settingsObserver;
        this.mContext = context;
        this.mVotesStorage = votesStorage;
    }

    public void onDesiredDisplayModeSpecsChanged(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs, SparseArray<Vote> votes) {
        boolean noVotes = votes == null || votes.size() == 0;
        if (!noVotes) {
            addToHistory(displayId, desiredDisplayModeSpecs, votes);
        }
        Slog.i(TAG, "onDesiredDisplayModeSpecsChanged:" + desiredDisplayModeSpecs + " noVotes=" + noVotes);
    }

    public void dumpLocked(PrintWriter pw) {
        pw.println("  mSceneMaxRefreshRate: " + this.mSceneMaxRefreshRate);
        pw.println("History of DisplayMoDirector");
        int i = 0;
        while (true) {
            int i2 = this.mHistoryCount;
            if (i < i2) {
                int index = (this.mHistoryIndex + i) % i2;
                DisplayModeDirectorEntry displayModeDirectorEntry = this.mDisplayModeDirectorHistory[index];
                if (displayModeDirectorEntry != null) {
                    pw.println(displayModeDirectorEntry.toString());
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void notifyDisplayModeSpecsChanged() {
        DisplayModeDirector displayModeDirector = this.mDisplayModeDirector;
        if (displayModeDirector != null) {
            displayModeDirector.notifyDesiredDisplayModeSpecsChanged();
        }
    }

    private DisplayModeDirectorEntry addToHistory(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs, SparseArray<Vote> votes) {
        DisplayModeDirectorEntry displayModeDirectorEntry;
        synchronized (this.mLock) {
            DisplayModeDirectorEntry[] displayModeDirectorEntryArr = this.mDisplayModeDirectorHistory;
            int i = this.mHistoryIndex;
            DisplayModeDirectorEntry displayModeDirectorEntry2 = displayModeDirectorEntryArr[i];
            if (displayModeDirectorEntry2 != null) {
                displayModeDirectorEntry2.timesTamp = System.currentTimeMillis();
                this.mDisplayModeDirectorHistory[this.mHistoryIndex].displayId = displayId;
                this.mDisplayModeDirectorHistory[this.mHistoryIndex].desiredDisplayModeSpecs = desiredDisplayModeSpecs;
                this.mDisplayModeDirectorHistory[this.mHistoryIndex].votes = votes;
            } else {
                displayModeDirectorEntryArr[i] = new DisplayModeDirectorEntry(displayId, desiredDisplayModeSpecs, votes);
            }
            DisplayModeDirectorEntry[] displayModeDirectorEntryArr2 = this.mDisplayModeDirectorHistory;
            int i2 = this.mHistoryIndex;
            displayModeDirectorEntry = displayModeDirectorEntryArr2[i2];
            this.mHistoryIndex = (i2 + 1) % this.mHistoryCount;
        }
        return displayModeDirectorEntry;
    }

    private void updateMiuiRefreshRateState() {
        this.mMiuiRefreshRateEnable = SystemProperties.getBoolean(MIUI_OPTIMIZATION_PROP, !"1".equals(SystemProperties.get("ro.miui.cts")));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DisplayModeDirectorEntry {
        private DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs;
        private int displayId;
        private long timesTamp = System.currentTimeMillis();
        private SparseArray<Vote> votes;

        public DisplayModeDirectorEntry(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs desiredDisplayModeSpecs, SparseArray<Vote> votes) {
            this.displayId = displayId;
            this.desiredDisplayModeSpecs = desiredDisplayModeSpecs;
            this.votes = votes;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(DisplayModeDirectorImpl.this.mDateFormat.format(Long.valueOf(this.timesTamp)) + "  Display " + this.displayId + ":\n");
            stringBuilder.append("  mDesiredDisplayModeSpecs:" + this.desiredDisplayModeSpecs + "\n");
            stringBuilder.append("  mVotes:\n");
            for (int p = 17; p >= 0; p--) {
                Vote vote = this.votes.get(p);
                if (vote != null) {
                    stringBuilder.append("      " + Vote.priorityToString(p) + " -> " + vote + "\n");
                }
            }
            return stringBuilder.toString();
        }
    }

    public void updateDisplaySize(int displayId, int width, int height) {
        synchronized (this.mLock) {
            Vote sizeVote = Vote.forSize(width, height);
            this.mVotesStorage.updateVote(displayId, 17, sizeVote);
        }
    }

    public boolean updateMiuiRefreshRateSettingLocked(float minRefreshRate) {
        float thermalRefreshRate;
        Vote miuiRefreshVote = getMiuiRefreshRateVote();
        if (this.mMiuiRefreshRateEnable) {
            Vote thermalVote = getThermalLimitRefreshRateVote();
            this.mVotesStorage.updateGlobalVote(16, thermalVote);
            this.mVotesStorage.updateGlobalVote(15, miuiRefreshVote);
            clearAospSettingVote();
            float miuiRefresh = Float.POSITIVE_INFINITY;
            if (thermalVote == null) {
                thermalRefreshRate = Float.POSITIVE_INFINITY;
            } else {
                thermalRefreshRate = thermalVote.refreshRateRanges.render.max;
            }
            if (miuiRefreshVote != null) {
                miuiRefresh = miuiRefreshVote.refreshRateRanges.render.max;
            }
            float miuiMaxRefreshRate = Math.min(thermalRefreshRate, miuiRefresh);
            this.mBrightnessObserver.onRefreshRateSettingChangedLocked(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, miuiMaxRefreshRate);
            return miuiRefreshVote != null;
        }
        clearMiuiSettingVote();
        return false;
    }

    private Vote getMiuiRefreshRateVote() {
        if (this.mExternalDisplayConnected) {
            Vote miuiVote = Vote.forRenderFrameRates(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 60.0f);
            return miuiVote;
        }
        if (this.mSceneMaxRefreshRate > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            float userSetRefreshRate = Settings.Secure.getFloat(this.mContext.getContentResolver(), "user_refresh_rate", -1.0f);
            float min = userSetRefreshRate > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? Math.min(this.mSceneMaxRefreshRate, userSetRefreshRate) : this.mSceneMaxRefreshRate;
            this.mSceneMaxRefreshRate = min;
            Vote miuiVote2 = Vote.forRenderFrameRates(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, min);
            return miuiVote2;
        }
        ContentResolver cr = this.mContext.getContentResolver();
        float miuiRefreshRate = Settings.Secure.getFloat(cr, MIUI_REFRESH_RATE, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
        Vote miuiVote3 = miuiRefreshRate == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? null : Vote.forRenderFrameRates(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, miuiRefreshRate);
        return miuiVote3;
    }

    private Vote getThermalLimitRefreshRateVote() {
        int thermalRefreshRate = Settings.System.getInt(this.mContext.getContentResolver(), MIUI_THERMAL_LIMIT_REFRESH_RATE, 0);
        if (thermalRefreshRate == 0) {
            return null;
        }
        return Vote.forRenderFrameRates(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, thermalRefreshRate);
    }

    private void clearMiuiSettingVote() {
        this.mVotesStorage.updateGlobalVote(16, (Vote) null);
        this.mVotesStorage.updateGlobalVote(15, (Vote) null);
    }

    private void clearAospSettingVote() {
        this.mVotesStorage.updateGlobalVote(7, (Vote) null);
        this.mVotesStorage.updateGlobalVote(0, (Vote) null);
        this.mVotesStorage.updateGlobalVote(3, (Vote) null);
    }

    private void updateExternalDisplayConnectedLocked() {
        this.mExternalDisplayConnected = Settings.System.getInt(this.mContext.getContentResolver(), EXTERNAL_DISPLAY_CONNECTED, 0) != 0;
    }

    public boolean updateMiuiRefreshRateEnhance(Uri uri) {
        if (this.mMiuiOptimizationSetting.equals(uri)) {
            updateMiuiRefreshRateState();
            return true;
        }
        if (!this.mExternalDisplayRefreshRateSetting.equals(uri)) {
            return this.mThermalRefreshRateSetting.equals(uri) || this.mMiuiRefreshRateSetting.equals(uri);
        }
        updateExternalDisplayConnectedLocked();
        return true;
    }

    public void setSceneMaxRefreshRate(int displayId, float maxFrameRate) {
        synchronized (this.mLock) {
            this.mSceneMaxRefreshRate = maxFrameRate;
            this.mSettingsObserver.updateMiuiRefreshRateSettingLocked();
        }
    }

    public void registerMiuiContentObserver(ContentResolver cr, ContentObserver observer) {
        cr.registerContentObserver(this.mMiuiRefreshRateSetting, false, observer, 0);
        cr.registerContentObserver(this.mMiuiOptimizationSetting, false, observer, 0);
        if (Settings.System.getInt(cr, MIUI_THERMAL_LIMIT_REFRESH_RATE, 0) > 0) {
            Settings.System.putInt(cr, MIUI_THERMAL_LIMIT_REFRESH_RATE, 0);
        }
        cr.registerContentObserver(this.mThermalRefreshRateSetting, false, observer, 0);
        cr.registerContentObserver(this.mExternalDisplayRefreshRateSetting, false, observer, 0);
        synchronized (this.mLock) {
            updateExternalDisplayConnectedLocked();
            updateMiuiRefreshRateState();
        }
    }

    public SparseArray<Vote> getVotes(int displayId) {
        return this.mVotesStorage.getVotes(displayId);
    }
}
