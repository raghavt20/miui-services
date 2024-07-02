package com.android.server.power.statistic;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class DisplayPortEventStatistic {
    private static final String CONNECTED_APP_DURATION = "connected_app_duration";
    private static final String CONNECTED_DEVICE_INFO = "connected_device_info";
    private static final String CONNECTED_DURATION = "connected_duration";
    private static final String CONNECTED_TIMES = "connected_times";
    private static final boolean DEBUG;
    private static final String DISPLAY_PORT_EVENT_NAME = "display_port_statistic";
    private static final String TAG = "DisplayPortEventStatist";
    private static final int UPDATE_REASON_DISPLAY_CONNECTED = 3;
    private static final int UPDATE_REASON_FOREGROUND_APP = 2;
    private static final int UPDATE_REASON_INTERACTIVE = 1;
    private static final int UPDATE_REASON_RESET = 0;
    private final String mAppId;
    private int mConnectedTimes;
    private final Context mContext;
    private String mForegroundAppName;
    private boolean mIsDisplayPortConnected;
    private long mLastConnectedTime;
    private long mLastEventTime;
    private int mLastUpdateReason;
    private final OneTrackerHelper mOneTrackerHelper;
    private long mSumOfConnectedDuration;
    private boolean mInteractive = true;
    private final List<DisplayPortEvent> mConnectedDeviceInfoList = new ArrayList();
    private final Map<String, Long> mForegroundAppUsageDurationMap = new HashMap();
    private final DisplayPortEvent mLastDisplayPortEvent = new DisplayPortEvent();

    static {
        DEBUG = SystemProperties.getInt("debug.miui.dp.statistic.dbg", 0) != 0;
    }

    public DisplayPortEventStatistic(String appId, Context context, OneTrackerHelper oneTrackerHelper) {
        this.mAppId = appId;
        this.mContext = context;
        this.mOneTrackerHelper = oneTrackerHelper;
    }

    /* loaded from: classes.dex */
    public static class DisplayPortEvent {
        private int mFrameRate;
        private boolean mIsConnected;
        private long mPhysicalDisplayId;
        private String mProductName;
        private String mResolution;

        DisplayPortEvent() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public DisplayPortEvent(long physicalDisplayId, boolean isConnected, String productName, int frameRate, String resolution) {
            this.mPhysicalDisplayId = physicalDisplayId;
            this.mIsConnected = isConnected;
            this.mProductName = productName;
            this.mFrameRate = frameRate;
            this.mResolution = resolution;
        }

        public long getPhysicalDisplayId() {
            return this.mPhysicalDisplayId;
        }

        public void setPhysicalDisplayId(long physicalDisplayId) {
            this.mPhysicalDisplayId = physicalDisplayId;
        }

        public boolean getIsConnected() {
            return this.mIsConnected;
        }

        public void setIsConnected(boolean isConnected) {
            this.mIsConnected = isConnected;
        }

        public String getProductName() {
            return this.mProductName;
        }

        public void setProductName(String productName) {
            this.mProductName = productName;
        }

        public int getFrameRate() {
            return this.mFrameRate;
        }

        public void setFrameRate(int frameRate) {
            this.mFrameRate = frameRate;
        }

        public String getResolution() {
            return this.mResolution;
        }

        public void setResolution(String resolution) {
            this.mResolution = resolution;
        }

        public void copyFrom(DisplayPortEvent other) {
            this.mPhysicalDisplayId = other.getPhysicalDisplayId();
            this.mIsConnected = other.getIsConnected();
            this.mProductName = other.getProductName();
            this.mFrameRate = other.getFrameRate();
            this.mResolution = other.getResolution();
        }

        public String toString() {
            return "[productName=" + this.mProductName + ", frameRate=" + this.mFrameRate + ", resolution=" + this.mResolution + "]";
        }
    }

    public void reportDisplayPortEventByIntent() {
        long now = SystemClock.uptimeMillis();
        if (this.mIsDisplayPortConnected) {
            recordConnectedDuration(now);
            if (this.mInteractive) {
                recordAppUsageTimeDuringConnected(now);
            }
        }
        Intent intent = this.mOneTrackerHelper.getTrackEventIntent(this.mAppId, DISPLAY_PORT_EVENT_NAME, this.mContext.getPackageName());
        int i = this.mConnectedTimes;
        if (i != 0) {
            intent.putExtra(CONNECTED_TIMES, i).putExtra(CONNECTED_DURATION, this.mSumOfConnectedDuration).putExtra(CONNECTED_DEVICE_INFO, this.mConnectedDeviceInfoList.toString()).putExtra(CONNECTED_APP_DURATION, this.mForegroundAppUsageDurationMap.toString());
        }
        Slog.d(TAG, "reportDisplayPortEventByIntent: data: " + intent.getExtras());
        try {
            this.mOneTrackerHelper.reportTrackEventByIntent(intent);
        } catch (Exception e) {
            Slog.e(TAG, "reportDisplayPortEventByIntent fail! " + e);
        }
        resetDisplayPortEventInfo(now);
    }

    private void resetDisplayPortEventInfo(long now) {
        this.mConnectedTimes = 0;
        this.mSumOfConnectedDuration = 0L;
        updateLastAppUsageEventInfo(now, 0);
        updateConnectedEventInfo(now, this.mLastDisplayPortEvent);
        this.mConnectedDeviceInfoList.clear();
        this.mForegroundAppUsageDurationMap.clear();
    }

    private void updateLastAppUsageEventInfo(long eventTime, int updateReason) {
        if (this.mIsDisplayPortConnected && this.mInteractive) {
            this.mLastEventTime = eventTime;
            this.mLastUpdateReason = updateReason;
            if (DEBUG) {
                Slog.d(TAG, "updateLastAppUsageEventInfo: mLastEventTime: " + this.mLastEventTime + ", mLastUpdateReason: " + this.mLastUpdateReason);
            }
        }
    }

    private void updateConnectedEventInfo(long eventTime, DisplayPortEvent displayPortEvent) {
        if (this.mIsDisplayPortConnected) {
            this.mLastConnectedTime = eventTime;
            this.mConnectedTimes++;
            this.mLastDisplayPortEvent.copyFrom(displayPortEvent);
        }
    }

    private void recordConnectedDuration(long now) {
        long duration = now - this.mLastConnectedTime;
        if (DEBUG) {
            Slog.d(TAG, "recordConnectedDuration: duration: " + duration);
        }
        this.mSumOfConnectedDuration += duration;
        for (DisplayPortEvent displayPortEvent : this.mConnectedDeviceInfoList) {
            if (this.mLastDisplayPortEvent.getPhysicalDisplayId() == displayPortEvent.getPhysicalDisplayId()) {
                return;
            }
        }
        this.mConnectedDeviceInfoList.add(this.mLastDisplayPortEvent);
    }

    private void recordAppUsageTimeDuringConnected(long now) {
        long duration = this.mForegroundAppUsageDurationMap.getOrDefault(this.mForegroundAppName, 0L).longValue();
        long newDuration = (now - this.mLastEventTime) + duration;
        if (DEBUG) {
            Slog.d(TAG, "recordAppUsageTimeDuringConnected: mForegroundAppName: " + this.mForegroundAppName + ", newDuration: " + newDuration);
        }
        this.mForegroundAppUsageDurationMap.put(this.mForegroundAppName, Long.valueOf(newDuration));
    }

    public void handleConnectedStateChanged(DisplayPortEvent displayPortEvent) {
        boolean isConnected = displayPortEvent.getIsConnected();
        long now = SystemClock.uptimeMillis();
        if (this.mIsDisplayPortConnected != isConnected) {
            this.mIsDisplayPortConnected = isConnected;
            if (DEBUG) {
                Slog.d(TAG, "handleConnectedStateChanged: connected: " + displayPortEvent.getIsConnected() + ", physic display id: " + displayPortEvent.getPhysicalDisplayId() + ", product name: " + displayPortEvent.getProductName() + ", resolution: " + displayPortEvent.getResolution() + ", frame rate: " + displayPortEvent.getFrameRate());
            }
            if (this.mIsDisplayPortConnected) {
                updateLastAppUsageEventInfo(now, 3);
                updateConnectedEventInfo(now, displayPortEvent);
            } else {
                if (this.mInteractive) {
                    recordAppUsageTimeDuringConnected(now);
                }
                recordConnectedDuration(now);
            }
        }
    }

    public void handleForegroundAppChanged(String foregroundAppName) {
        if (!TextUtils.equals(this.mForegroundAppName, foregroundAppName)) {
            long now = SystemClock.uptimeMillis();
            if (this.mInteractive && this.mIsDisplayPortConnected) {
                recordAppUsageTimeDuringConnected(now);
            }
            updateLastAppUsageEventInfo(now, 2);
            this.mForegroundAppName = foregroundAppName;
            if (DEBUG) {
                Slog.d(TAG, "handleForegroundAppChanged: mForegroundAppName: " + this.mForegroundAppName);
            }
        }
    }

    public void handleInteractiveChanged(boolean interactive) {
        if (this.mInteractive != interactive) {
            this.mInteractive = interactive;
            if (DEBUG) {
                Slog.d(TAG, "handleInteractiveChanged: mInteractive: " + this.mInteractive);
            }
            long now = SystemClock.uptimeMillis();
            updateLastAppUsageEventInfo(now, 1);
            if (this.mIsDisplayPortConnected && !this.mInteractive) {
                recordAppUsageTimeDuringConnected(now);
            }
        }
    }
}
