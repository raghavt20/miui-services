package com.android.server.power.statistic;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Slog;

/* loaded from: classes.dex */
public class DimEventStatistic {
    private static final boolean DEBUG;
    private static final String ENTER_DIM_DURATION = "enter_dim_duration";
    private static final String ENTER_DIM_TIMES = "enter_dim_times";
    private static final String EVENT_NAME = "dim_statistic";
    private static final String TAG = "DimEventStatistic";
    private final String mAppId;
    private final Context mContext;
    private int mEnterDimTimes;
    private long mLastEnterDimTime;
    private final OneTrackerHelper mOneTrackerHelper;
    private long mSumDimDuration;

    static {
        DEBUG = SystemProperties.getInt("debug.miui.dp.statistic.dbg", 0) != 0;
    }

    public DimEventStatistic(String appId, Context context, OneTrackerHelper oneTrackerHelper) {
        this.mAppId = appId;
        this.mContext = context;
        this.mOneTrackerHelper = oneTrackerHelper;
    }

    public void notifyDimStateChanged(DimEvent dimEvent) {
        if (DEBUG) {
            Slog.d(TAG, "notifyDimStateChanged: isEnterDim: " + dimEvent.getIsEnterDim() + ", enter dim time: " + dimEvent.getDimTime());
        }
        if (dimEvent.getIsEnterDim()) {
            this.mLastEnterDimTime = dimEvent.getDimTime();
            this.mEnterDimTimes++;
        } else {
            recordDimDuration(dimEvent.getDimTime());
        }
    }

    public void reportDimStateByIntent() {
        Intent intent = this.mOneTrackerHelper.getTrackEventIntent(this.mAppId, EVENT_NAME, this.mContext.getPackageName());
        int i = this.mEnterDimTimes;
        if (i != 0) {
            intent.putExtra(ENTER_DIM_TIMES, i);
            intent.putExtra(ENTER_DIM_DURATION, this.mSumDimDuration);
        }
        try {
            this.mOneTrackerHelper.reportTrackEventByIntent(intent);
        } catch (Exception e) {
            Slog.e(TAG, "reportDimStateByIntent fail! " + e);
        }
        if (DEBUG) {
            Slog.d(TAG, "reportDimStateByIntent: data: " + intent.getExtras());
        }
        resetDimStateInfo();
    }

    private void resetDimStateInfo() {
        this.mEnterDimTimes = 0;
        this.mSumDimDuration = 0L;
    }

    private void recordDimDuration(long now) {
        long duration = now - this.mLastEnterDimTime;
        this.mSumDimDuration += duration;
    }

    /* loaded from: classes.dex */
    public static class DimEvent {
        private long mDimTime;
        private boolean mIsEnterDim;

        public DimEvent() {
        }

        public DimEvent(long dimTime, boolean isEnterDim) {
            this.mDimTime = dimTime;
            this.mIsEnterDim = isEnterDim;
        }

        public long getDimTime() {
            return this.mDimTime;
        }

        public void setDimTime(long dimTime) {
            this.mDimTime = dimTime;
        }

        public boolean getIsEnterDim() {
            return this.mIsEnterDim;
        }

        public void setIsEnterDim(boolean isEnterDim) {
            this.mIsEnterDim = isEnterDim;
        }
    }
}
