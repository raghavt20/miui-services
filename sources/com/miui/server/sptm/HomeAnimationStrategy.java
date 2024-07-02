package com.miui.server.sptm;

import android.content.Context;
import android.provider.Settings;
import android.util.Slog;
import com.miui.server.sptm.PreLoadStrategy;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.util.HashSet;

/* loaded from: classes.dex */
class HomeAnimationStrategy implements SpeedTestModeServiceImpl.Strategy {
    private static final double HOME_ANIMATION_RATIO_DELTA = 0.25d;
    private static final String KEY_ANIMATION_RATIO = "transition_animation_duration_ratio";
    private static final double MAX_HOME_ANIMATION_RATIO = 1.0d;
    private static final double MIN_HOME_ANIMATION_RATIO = 0.5d;
    private Context mContext;
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();
    private double mCurHomeAnimationRatio = MAX_HOME_ANIMATION_RATIO;
    private volatile int mContinuesSPTCount = 0;
    private int mAppStartedCountInSPTMode = 0;
    private boolean mIsInSpeedTestMode = false;
    private HashSet<String> mStartedAppInLastRound = new HashSet<>();

    public HomeAnimationStrategy(Context context) {
        this.mContext = context;
        setAnimationRatio(Double.valueOf(MAX_HOME_ANIMATION_RATIO));
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onNewEvent(int eventType) {
        if (eventType == 5 && getContinuesSPTCount() != 0) {
            setAnimationRatio(Double.valueOf(MAX_HOME_ANIMATION_RATIO));
            setContinuesSPTCount(0);
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onAppStarted(PreLoadStrategy.AppStartRecord r) {
        if (this.mIsInSpeedTestMode) {
            this.mAppStartedCountInSPTMode = this.mAppStartedCountInSPTMode + 1;
            double limitHomeAnimationRatio = limitHomeAnimationRatio(MAX_HOME_ANIMATION_RATIO - ((r0 + getContinuesSPTCount()) * HOME_ANIMATION_RATIO_DELTA));
            this.mCurHomeAnimationRatio = limitHomeAnimationRatio;
            setAnimationRatio(Double.valueOf(limitHomeAnimationRatio));
        }
        if (r.isColdStart && SpeedTestModeServiceImpl.SPEED_TEST_APP_LIST.contains(r.packageName)) {
            this.mStartedAppInLastRound.add(r.packageName);
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onSpeedTestModeChanged(boolean isEnable) {
        this.mAppStartedCountInSPTMode = 0;
        this.mIsInSpeedTestMode = isEnable;
        if (!isEnable) {
            int speedTestType = this.mSpeedTestModeService.getPreloadCloudType();
            int startedAppCount = this.mStartedAppInLastRound.size();
            if ((speedTestType > 3 && startedAppCount >= SpeedTestModeServiceImpl.STARTED_APPCOUNT) || (speedTestType <= 3 && startedAppCount >= 14)) {
                setContinuesSPTCount(getContinuesSPTCount() + 1);
            } else {
                setContinuesSPTCount(0);
            }
            if (SpeedTestModeServiceImpl.DEBUG) {
                Slog.d(SpeedTestModeServiceImpl.TAG, String.format("In last round, started: %s, curRound: %s", Integer.valueOf(this.mStartedAppInLastRound.size()), Integer.valueOf(getContinuesSPTCount())));
            }
            this.mStartedAppInLastRound.clear();
            double limitHomeAnimationRatio = limitHomeAnimationRatio(MAX_HOME_ANIMATION_RATIO - (getContinuesSPTCount() * HOME_ANIMATION_RATIO_DELTA));
            this.mCurHomeAnimationRatio = limitHomeAnimationRatio;
            setAnimationRatio(Double.valueOf(limitHomeAnimationRatio));
        }
    }

    public float getWindowAnimatorDurationOverride() {
        if (getContinuesSPTCount() != 0 || this.mIsInSpeedTestMode) {
            return 0.3f;
        }
        return 1.0f;
    }

    private static double limitHomeAnimationRatio(double radio) {
        return Math.min(MAX_HOME_ANIMATION_RATIO, Math.max(MIN_HOME_ANIMATION_RATIO, radio));
    }

    private void setAnimationRatio(Double value) {
        if (this.mSpeedTestModeService.getAnimationCloudEnable()) {
            Settings.Global.putString(this.mContext.getContentResolver(), KEY_ANIMATION_RATIO, String.valueOf(value));
        } else {
            Settings.Global.putString(this.mContext.getContentResolver(), KEY_ANIMATION_RATIO, String.valueOf(MAX_HOME_ANIMATION_RATIO));
        }
    }

    private synchronized int getContinuesSPTCount() {
        return this.mContinuesSPTCount;
    }

    private synchronized void setContinuesSPTCount(int count) {
        this.mContinuesSPTCount = count;
    }
}
