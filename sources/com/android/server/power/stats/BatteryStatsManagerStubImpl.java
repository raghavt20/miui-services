package com.android.server.power.stats;

import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.os.Clock;
import com.android.server.location.GnssCollectData;
import com.android.server.power.stats.BatteryStatsImpl;
import com.android.server.power.stats.BatteryStatsManagerStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.whetstone.PowerKeeperPolicy;
import com.miui.whetstone.server.WhetstoneActivityManagerService;
import database.SlaDbSchema.SlaDbSchema;
import java.util.ArrayList;
import miui.telephony.SubscriptionManager;

/* loaded from: classes.dex */
public class BatteryStatsManagerStubImpl implements BatteryStatsManagerStub {
    private static final long BYTES_PER_GB = 1073741824;
    private static final long BYTES_PER_KB = 1024;
    private static final long BYTES_PER_MB = 1048576;
    private static final boolean DEBUG = false;
    private static final String TAG = "BatteryStatsManagerStub";
    public BatteryStatsManagerStub.ActiveCallback mActiveCallback;
    private SparseArray<SoleCalculateApp> mCalculateAppMap = new SparseArray<>();
    private int mCurrentSoleUid = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BatteryStatsManagerStubImpl> {

        /* compiled from: BatteryStatsManagerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BatteryStatsManagerStubImpl INSTANCE = new BatteryStatsManagerStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BatteryStatsManagerStubImpl m2293provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BatteryStatsManagerStubImpl m2292provideNewInstance() {
            return new BatteryStatsManagerStubImpl();
        }
    }

    public void noteAudioOnLocked(int uid) {
        BatteryStatsManagerStub.ActiveCallback activeCallback = this.mActiveCallback;
        if (activeCallback != null) {
            activeCallback.noteAudioOnLocked(uid);
        }
    }

    public void noteAudioOffLocked(int uid) {
        BatteryStatsManagerStub.ActiveCallback activeCallback = this.mActiveCallback;
        if (activeCallback != null) {
            activeCallback.noteAudioOffLocked(uid);
        }
    }

    public void noteResetAudioLocked() {
        BatteryStatsManagerStub.ActiveCallback activeCallback = this.mActiveCallback;
        if (activeCallback != null) {
            activeCallback.noteResetAudioLocked();
        }
    }

    public void noteStartGpsLocked(int uid) {
        BatteryStatsManagerStub.ActiveCallback activeCallback = this.mActiveCallback;
        if (activeCallback != null) {
            activeCallback.noteStartGpsLocked(uid);
        }
    }

    public void noteStopGpsLocked(int uid) {
        BatteryStatsManagerStub.ActiveCallback activeCallback = this.mActiveCallback;
        if (activeCallback != null) {
            activeCallback.noteStopGpsLocked(uid);
        }
    }

    public void setActiveCallback(BatteryStatsManagerStub.ActiveCallback callback) {
        this.mActiveCallback = callback;
    }

    public void noteSyncStart(String str, int i, boolean z) {
        if (i < 10000 || i > 19999) {
            return;
        }
        PowerKeeperPolicy.getInstance().notifySyncEvent(z ? 1 : 0, 4, z ? "syncstart" : "syncend", str, new int[]{i});
    }

    public void notifyDetailsWakeUp(String type, int uid, String name) {
        Bundle b = new Bundle();
        b.putInt(SlaDbSchema.SlaTable.Uidlist.UID, uid);
        b.putString(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, type);
        b.putString("name", name);
        PowerKeeperPolicy.getInstance().notifyEvent(8, b);
    }

    public void notifyWakeUp(String reason) {
        Bundle b = new Bundle();
        b.putString(EdgeSuppressionManager.EdgeSuppressionHandler.MSG_DATA_REASON, reason);
        PowerKeeperPolicy.getInstance().notifyEvent(12, b);
    }

    public int getDefaultDataSlotId() {
        return SubscriptionManager.getDefault().getDefaultDataSlotId();
    }

    public void reportActiveEvent(Handler handler, int what, int uid) {
        Message m = handler.obtainMessage(what);
        m.arg1 = uid;
        handler.sendMessage(m);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1000:
                noteAudioOnLocked(msg.arg1);
                return;
            case 1001:
                noteAudioOffLocked(msg.arg1);
                return;
            case 1002:
                noteResetAudioLocked();
                return;
            case 1003:
                noteStartGpsLocked(msg.arg1);
                return;
            case 1004:
                noteStopGpsLocked(msg.arg1);
                return;
            default:
                return;
        }
    }

    public void writeUidStatsToParcel(BatteryStats batteryStats, Parcel out, long elapsedRealtimeUs) {
        SparseArray<BatteryStatsImpl.Uid> mUidStats = batteryStats.getUidStats();
        int size = mUidStats.size();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeInt(mUidStats.keyAt(i));
            BatteryStatsImpl.Uid uid = mUidStats.valueAt(i);
            uid.writeToParcelLocked(out, elapsedRealtimeUs);
        }
    }

    public void readSummaryFromParcel(Parcel in, BatteryStats batteryStats) {
        readSoleCalculateApp(in, (BatteryStatsImpl) batteryStats);
    }

    public void writeSummaryToParcel(Parcel out, long elapsedRealtimeUs) {
        writeSoleCalculateApp(out, elapsedRealtimeUs);
    }

    public void readFromParcelLocked(Parcel in, BatteryStats batteryStats) {
        readSoleCalculateApp(in, (BatteryStatsImpl) batteryStats);
    }

    public void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
        writeSoleCalculateApp(out, elapsedRealtimeUs);
    }

    public void resetAllStatsLocked(long uptimeMillis, long elapsedRealtimeMillis) {
        if (this.mCalculateAppMap.size() == 0) {
            return;
        }
        long elapsedRealtimeUs = 1000 * elapsedRealtimeMillis;
        SoleCalculateApp currentApp = null;
        for (int i = 0; i < this.mCalculateAppMap.size(); i++) {
            SoleCalculateApp app = this.mCalculateAppMap.valueAt(i);
            if (app.uid == this.mCurrentSoleUid) {
                currentApp = app;
            }
            app.reset(elapsedRealtimeUs);
        }
        synchronized (this.mCalculateAppMap) {
            this.mCalculateAppMap.clear();
            if (this.mCurrentSoleUid != -1 && currentApp != null && currentApp.oldBin > -1) {
                this.mCalculateAppMap.put(this.mCurrentSoleUid, currentApp);
                currentApp.screenBrightnessTimers[currentApp.oldBin].startRunningLocked(elapsedRealtimeMillis);
            }
        }
    }

    public void noteActivityResumedLocked(BatteryStats batteryStats, int uid, long elapsedRealtimeMs, long uptimeMs, String packageName) {
        if (!UserHandle.isApp(uid)) {
            return;
        }
        this.mCurrentSoleUid = uid;
        BatteryStatsImpl impl = (BatteryStatsImpl) batteryStats;
        synchronized (this.mCalculateAppMap) {
            if (!this.mCalculateAppMap.contains(uid)) {
                SoleCalculateApp app = new SoleCalculateApp(impl.mClock, impl.mOnBatteryTimeBase);
                app.uid = uid;
                app.packageName = packageName;
                if (impl.mScreenBrightnessBin > -1) {
                    app.screenBrightnessTimers[impl.mScreenBrightnessBin].startRunningLocked(elapsedRealtimeMs);
                    app.oldBin = impl.mScreenBrightnessBin;
                }
                this.mCalculateAppMap.put(uid, app);
            } else {
                SoleCalculateApp app2 = this.mCalculateAppMap.get(uid);
                if (app2 != null && impl.mScreenBrightnessBin > -1) {
                    app2.screenBrightnessTimers[impl.mScreenBrightnessBin].startRunningLocked(elapsedRealtimeMs);
                    app2.oldBin = impl.mScreenBrightnessBin;
                }
            }
        }
    }

    public void noteActivityPausedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mCalculateAppMap.contains(uid)) {
            this.mCurrentSoleUid = -1;
            SoleCalculateApp app = this.mCalculateAppMap.get(uid);
            if (app != null && app.oldBin > -1) {
                app.screenBrightnessTimers[app.oldBin].stopRunningLocked(elapsedRealtimeMs);
                app.oldBin = -1;
            }
        }
    }

    public void noteScreenBrightnessLocked(BatteryStats batteryStats, int bin, long elapsedRealtimeMs, long uptimeMs) {
        SoleCalculateApp app;
        int i = this.mCurrentSoleUid;
        if (i != -1 && (app = this.mCalculateAppMap.get(i)) != null && app.oldBin > -1) {
            app.screenBrightnessTimers[app.oldBin].stopRunningLocked(elapsedRealtimeMs);
            app.screenBrightnessTimers[bin].startRunningLocked(elapsedRealtimeMs);
            app.oldBin = bin;
        }
    }

    public byte[] getAppScreenInfo(BatteryStats batteryStats, long elapsedRealtimeMs) {
        BatteryStatsManagerStubImpl batteryStatsManagerStubImpl = this;
        byte[] bytes = null;
        Parcel out = Parcel.obtain();
        try {
            try {
                try {
                    if (batteryStatsManagerStubImpl.mCalculateAppMap.size() > 0) {
                        out.writeInt(batteryStatsManagerStubImpl.mCalculateAppMap.size());
                        int i = 100;
                        long[] binTimes = new long[100];
                        int i2 = 0;
                        while (i2 < batteryStatsManagerStubImpl.mCalculateAppMap.size()) {
                            SoleCalculateApp app = batteryStatsManagerStubImpl.mCalculateAppMap.valueAt(i2);
                            if (app != null) {
                                String pkgName = app.packageName;
                                long fgTimeMs = batteryStatsManagerStubImpl.getProcessForegroundTimeMs(app.uid, batteryStats, elapsedRealtimeMs * 1000);
                                int bin = 0;
                                while (bin < i) {
                                    binTimes[bin] = app.screenBrightnessTimers[bin].getTotalTimeLocked(elapsedRealtimeMs * 1000, 0) / 1000;
                                    bin++;
                                    i = 100;
                                }
                                out.writeString(pkgName);
                                out.writeLong(fgTimeMs);
                                out.writeLongArray(binTimes);
                            }
                            i2++;
                            i = 100;
                            batteryStatsManagerStubImpl = this;
                        }
                    } else {
                        out.writeInt(0);
                    }
                    bytes = out.marshall();
                } catch (Exception e) {
                    e = e;
                    e.printStackTrace();
                    out.recycle();
                    return bytes;
                }
            } catch (Throwable th) {
                th = th;
                out.recycle();
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
        } catch (Throwable th2) {
            th = th2;
            out.recycle();
            throw th;
        }
        out.recycle();
        return bytes;
    }

    public byte[] getTargetScreenInfo(String pkgName, BatteryStats batteryStats, long elapsedRealtimeMs) {
        byte[] bytes = null;
        Parcel out = Parcel.obtain();
        try {
            try {
                try {
                    if (this.mCalculateAppMap.size() > 0) {
                        out.writeInt(this.mCalculateAppMap.size());
                        long[] binTimes = new long[100];
                        SoleCalculateApp app = null;
                        boolean isExist = false;
                        int i = 0;
                        while (true) {
                            if (i >= this.mCalculateAppMap.size()) {
                                break;
                            }
                            app = this.mCalculateAppMap.valueAt(i);
                            if (app != null) {
                                try {
                                    if (TextUtils.equals(app.packageName, pkgName)) {
                                        isExist = true;
                                        break;
                                    }
                                } catch (Exception e) {
                                    e = e;
                                    e.printStackTrace();
                                    out.recycle();
                                    return bytes;
                                } catch (Throwable th) {
                                    th = th;
                                    out.recycle();
                                    throw th;
                                }
                            }
                            i++;
                        }
                        if (isExist) {
                            out.writeInt(1);
                            long fgTimeMs = getProcessForegroundTimeMs(app.uid, batteryStats, elapsedRealtimeMs * 1000);
                            int bin = 0;
                            for (int i2 = 100; bin < i2; i2 = 100) {
                                binTimes[bin] = app.screenBrightnessTimers[bin].getTotalTimeLocked(elapsedRealtimeMs * 1000, 0) / 1000;
                                bin++;
                            }
                            out.writeLong(fgTimeMs);
                            out.writeLongArray(binTimes);
                        } else {
                            out.writeInt(0);
                        }
                    } else {
                        out.writeInt(0);
                    }
                    bytes = out.marshall();
                } catch (Exception e2) {
                    e = e2;
                    e.printStackTrace();
                    out.recycle();
                    return bytes;
                }
            } catch (Throwable th2) {
                th = th2;
                out.recycle();
                throw th;
            }
        } catch (Exception e3) {
            e = e3;
        } catch (Throwable th3) {
            th = th3;
        }
        out.recycle();
        return bytes;
    }

    private long getProcessForegroundTimeMs(int curUid, BatteryStats batteryStats, long realtimeUs) {
        BatteryStats.Uid uid = (BatteryStats.Uid) batteryStats.getUidStats().get(curUid);
        if (uid == null) {
            return 0L;
        }
        long topStateDurationUs = uid.getProcessStateTime(0, realtimeUs, 0);
        long foregroundActivityDurationUs = 0;
        BatteryStats.Timer foregroundActivityTimer = uid.getForegroundActivityTimer();
        if (foregroundActivityTimer != null) {
            foregroundActivityDurationUs = foregroundActivityTimer.getTotalTimeLocked(realtimeUs, 0);
        }
        long totalForegroundDurationUs = Math.min(topStateDurationUs, foregroundActivityDurationUs);
        return totalForegroundDurationUs / 1000;
    }

    public void noteStartAudioInNeed(int uid, int pid, int session, int port, int type) {
        if (WhetstoneActivityManagerService.getSingletonService() != null) {
            WhetstoneActivityManagerService.getSingletonService().notifyStartAudioInNeed(uid, pid, session, port, type);
        }
    }

    public void noteStopAudioInNeed(int uid, int pid, int session, int port, int type) {
        if (WhetstoneActivityManagerService.getSingletonService() != null) {
            WhetstoneActivityManagerService.getSingletonService().notifyStopAudioInNeed(uid, pid, session, port, type);
        }
    }

    public void noteMuteAudioInNeed(int uid, int pid, int session, int status) {
        if (WhetstoneActivityManagerService.getSingletonService() != null) {
            WhetstoneActivityManagerService.getSingletonService().notifyMuteAudioInNeed(uid, pid, session, status);
        }
    }

    private void readSoleCalculateApp(Parcel in, BatteryStatsImpl batteryStats) {
        synchronized (this.mCalculateAppMap) {
            this.mCalculateAppMap.clear();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                int uid = in.readInt();
                String pkgName = in.readString();
                SoleCalculateApp app = new SoleCalculateApp(batteryStats.mClock, batteryStats.mOnBatteryTimeBase);
                for (int j = 0; j < 100; j++) {
                    if (in.readInt() != 0) {
                        app.screenBrightnessTimers[j].readSummaryFromParcelLocked(in);
                    }
                }
                app.uid = uid;
                app.packageName = pkgName;
                if (UserHandle.isApp(uid)) {
                    this.mCalculateAppMap.put(uid, app);
                }
            }
        }
    }

    private void writeSoleCalculateApp(Parcel out, long elapsedRealtimeUs) {
        int size = this.mCalculateAppMap.size();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            SoleCalculateApp app = this.mCalculateAppMap.valueAt(i);
            if (app != null) {
                out.writeInt(app.uid);
                out.writeString(app.packageName);
                for (int j = 0; j < 100; j++) {
                    if (app.screenBrightnessTimers[j] != null) {
                        out.writeInt(1);
                        app.screenBrightnessTimers[j].writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
                    } else {
                        out.writeInt(0);
                    }
                }
            }
        }
    }

    public SparseArray<SoleCalculateApp> getScreenCalculateMap() {
        return this.mCalculateAppMap;
    }

    /* loaded from: classes.dex */
    public static class SoleCalculateApp {
        public String packageName;
        public float refreshRate;
        public int uid;
        public int oldBin = -1;
        public BatteryStatsImpl.StopwatchTimer[] screenBrightnessTimers = new BatteryStatsImpl.StopwatchTimer[100];

        public SoleCalculateApp(Clock clock, BatteryStatsImpl.TimeBase timeBase) {
            for (int i = 0; i < 100; i++) {
                this.screenBrightnessTimers[i] = new BatteryStatsImpl.StopwatchTimer(clock, (BatteryStatsImpl.Uid) null, (-10000) - i, (ArrayList) null, timeBase);
            }
        }

        public void reset(long elapsedRealtimeUs) {
            for (int i = 0; i < 100; i++) {
                this.screenBrightnessTimers[i].reset(false, elapsedRealtimeUs);
            }
        }
    }

    public void noteVideoFps(int uid, int fps) {
        if (WhetstoneActivityManagerService.getSingletonService() != null) {
            WhetstoneActivityManagerService.getSingletonService().notifyVideoFps(uid, fps);
        }
    }

    public void notifyEvent(int resId, Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        long elapsedRealtimeMs = SystemClock.elapsedRealtime();
        bundle.putLong(GnssCollectData.CollectDbEntry.COLUMN_NAME_STARTTIME, elapsedRealtimeMs);
        if (WhetstoneActivityManagerService.getSingletonService() != null) {
            WhetstoneActivityManagerService.getSingletonService().notifyEvent(resId, bundle);
        }
        Log.d(TAG, "notifyEvent for resId=" + resId + " used " + (SystemClock.elapsedRealtime() - elapsedRealtimeMs) + "ms");
    }
}
