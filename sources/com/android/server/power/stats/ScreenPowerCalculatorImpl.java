package com.android.server.power.stats;

import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.Parcel;
import android.os.SystemClock;
import android.os.UidBatteryConsumer;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
import com.android.internal.os.PowerProfile;
import com.android.server.power.stats.BatteryStatsImpl;
import com.android.server.power.stats.BatteryStatsManagerStubImpl;
import com.miui.base.MiuiStubRegistry;
import java.util.Objects;

/* loaded from: classes.dex */
public class ScreenPowerCalculatorImpl extends PowerCalculator implements ScreenPowerCalculatorStub {
    private static final boolean DEBUG = false;
    public static final long MIN_ACTIVE_TIME_FOR_SMEARING = 600000;
    private static final String POWER_DY_SCREEN_FULL = "dy.screen.full";
    private static final String POWER_DY_SCREEN_HBM = "dy.screen.hbm";
    private static final String POWER_DY_SCREEN_LOW = "dy.screen.low";
    private static final String TAG = "ScreenPowerCalculatorStub";
    private SparseArray<UidTimeStats> mAppDurationMap = new SparseArray<>();
    private UsageBasedPowerEstimator[] mAppScreenFullPowerEstimators;
    private UsageBasedPowerEstimator[] mAppScreenHBMPowerEstimators;
    private UsageBasedPowerEstimator[] mAppScreenLowPowerEstimators;
    private boolean mHasDyPowerController;
    private UsageBasedPowerEstimator[] mScreenFullPowerEstimators;
    private UsageBasedPowerEstimator[] mScreenOnPowerEstimators;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ScreenPowerCalculatorImpl> {

        /* compiled from: ScreenPowerCalculatorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ScreenPowerCalculatorImpl INSTANCE = new ScreenPowerCalculatorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ScreenPowerCalculatorImpl m2295provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ScreenPowerCalculatorImpl m2294provideNewInstance() {
            return new ScreenPowerCalculatorImpl();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UidTimeStats {
        public long fgTimeMs;
        public BatteryStatsImpl.StopwatchTimer[] screenBrightnessTimers;

        private UidTimeStats() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PowerAndDuration {
        public long durationMs;
        public double powerMah;

        private PowerAndDuration() {
        }
    }

    public void init(PowerProfile powerProfile) {
        int numDisplays = powerProfile.getNumDisplays();
        this.mScreenOnPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        this.mScreenFullPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        this.mAppScreenLowPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        this.mAppScreenFullPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        this.mAppScreenHBMPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        double dyLowMa = powerProfile.getAveragePower(POWER_DY_SCREEN_LOW);
        double dyFullMa = powerProfile.getAveragePower(POWER_DY_SCREEN_FULL);
        double dyHbmMa = powerProfile.getAveragePower(POWER_DY_SCREEN_HBM);
        for (int display = 0; display < numDisplays; display++) {
            this.mScreenOnPowerEstimators[display] = new UsageBasedPowerEstimator(powerProfile.getAveragePowerForOrdinal("screen.on.display", display));
            this.mScreenFullPowerEstimators[display] = new UsageBasedPowerEstimator(powerProfile.getAveragePowerForOrdinal("screen.full.display", display));
            this.mAppScreenLowPowerEstimators[display] = new UsageBasedPowerEstimator(dyLowMa);
            this.mAppScreenFullPowerEstimators[display] = new UsageBasedPowerEstimator(dyFullMa);
            this.mAppScreenHBMPowerEstimators[display] = new UsageBasedPowerEstimator(dyHbmMa);
        }
        this.mHasDyPowerController = (dyFullMa == 0.0d || dyHbmMa == 0.0d) ? false : true;
    }

    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 0;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long j, long j2, BatteryUsageStatsQuery batteryUsageStatsQuery) {
        SparseArray<BatteryStatsManagerStubImpl.SoleCalculateApp> screenCalculateMap;
        long j3 = j;
        UidTimeStatsIA uidTimeStatsIA = null;
        PowerAndDuration powerAndDuration = new PowerAndDuration();
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        int size = uidBatteryConsumerBuilders.size() - 1;
        while (size >= 0) {
            BatteryStats.Uid batteryStatsUid = uidBatteryConsumerBuilders.valueAt(size).getBatteryStatsUid();
            long processForegroundTimeMs = getProcessForegroundTimeMs(batteryStatsUid, j3);
            BatteryStatsManagerStubImpl batteryStatsManagerStubImpl = (BatteryStatsManagerStubImpl) BatteryStatsManagerStub.getInstance();
            if (batteryStatsManagerStubImpl != null && (screenCalculateMap = batteryStatsManagerStubImpl.getScreenCalculateMap()) != null && screenCalculateMap.contains(batteryStatsUid.getUid())) {
                UidTimeStats uidTimeStats = new UidTimeStats();
                uidTimeStats.fgTimeMs = processForegroundTimeMs;
                uidTimeStats.screenBrightnessTimers = ((BatteryStatsManagerStubImpl.SoleCalculateApp) Objects.requireNonNull(screenCalculateMap.get(batteryStatsUid.getUid()))).screenBrightnessTimers;
                this.mAppDurationMap.put(batteryStatsUid.getUid(), uidTimeStats);
            }
            size--;
            uidTimeStatsIA = null;
        }
        long screenOnEnergyConsumptionUC = batteryStats.getScreenOnEnergyConsumptionUC();
        int powerModel = getPowerModel(screenOnEnergyConsumptionUC, batteryUsageStatsQuery);
        calculateTotalDurationAndPower(powerAndDuration, powerModel, batteryStats, j, 0, screenOnEnergyConsumptionUC);
        double d = 0.0d;
        long j4 = 0;
        switch (powerModel) {
            case 2:
                PowerAndDuration powerAndDuration2 = new PowerAndDuration();
                int size2 = uidBatteryConsumerBuilders.size() - 1;
                while (size2 >= 0) {
                    UidBatteryConsumer.Builder valueAt = uidBatteryConsumerBuilders.valueAt(size2);
                    calculateAppUsingMeasuredEnergy(powerAndDuration2, valueAt.getBatteryStatsUid(), j3);
                    valueAt.setUsageDurationMillis(0, powerAndDuration2.durationMs).setConsumedPower(0, powerAndDuration2.powerMah, powerModel);
                    if (!valueAt.isVirtualUid()) {
                        d += powerAndDuration2.powerMah;
                        j4 += powerAndDuration2.durationMs;
                    }
                    size2--;
                    j3 = j;
                }
                break;
            default:
                smearScreenBatteryDrain(uidBatteryConsumerBuilders, powerAndDuration, j);
                d = powerAndDuration.powerMah;
                j4 = powerAndDuration.durationMs;
                break;
        }
        builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(0, Math.max(powerAndDuration.powerMah, d), powerModel).setUsageDurationMillis(0, powerAndDuration.durationMs);
        builder.getAggregateBatteryConsumerBuilder(1).setConsumedPower(0, d, powerModel).setUsageDurationMillis(0, j4);
    }

    private void calculateTotalDurationAndPower(PowerAndDuration totalPowerAndDuration, int powerModel, BatteryStats batteryStats, long rawRealtimeUs, int statsType, long consumptionUC) {
        totalPowerAndDuration.durationMs = calculateDuration(batteryStats, rawRealtimeUs, statsType);
        switch (powerModel) {
            case 2:
                totalPowerAndDuration.powerMah = uCtoMah(consumptionUC);
                return;
            default:
                if (this.mHasDyPowerController) {
                    totalPowerAndDuration.powerMah = calculateOthersPowerFromBrightness(batteryStats, rawRealtimeUs) + calculateSoleAppPowerFromBrightness(batteryStats, rawRealtimeUs);
                    return;
                } else {
                    totalPowerAndDuration.powerMah = calculateTotalPowerFromBrightness(batteryStats, rawRealtimeUs);
                    return;
                }
        }
    }

    private void calculateAppUsingMeasuredEnergy(PowerAndDuration appPowerAndDuration, BatteryStats.Uid u, long rawRealtimeUs) {
        appPowerAndDuration.durationMs = getProcessForegroundTimeMs(u, rawRealtimeUs);
        long chargeUC = u.getScreenOnEnergyConsumptionUC();
        if (chargeUC < 0) {
            Slog.wtf(TAG, "Screen energy not supported, so calculateApp shouldn't de called");
            appPowerAndDuration.powerMah = 0.0d;
        } else {
            appPowerAndDuration.powerMah = uCtoMah(chargeUC);
        }
    }

    private long calculateDuration(BatteryStats batteryStats, long rawRealtimeUs, int statsType) {
        return batteryStats.getScreenOnTime(rawRealtimeUs, statsType) / 1000;
    }

    private double calculateTotalPowerFromBrightness(BatteryStats batteryStats, long rawRealtimeUs) {
        int numDisplays = this.mScreenOnPowerEstimators.length;
        double power = 0.0d;
        for (int display = 0; display < numDisplays; display++) {
            long j = 1000;
            long displayTime = batteryStats.getDisplayScreenOnTime(display, rawRealtimeUs) / 1000;
            power += this.mScreenOnPowerEstimators[display].calculatePower(displayTime);
            int bin = 0;
            while (bin < 100) {
                long brightnessTime = batteryStats.getDisplayScreenBrightnessTime(display, bin, rawRealtimeUs) / j;
                double binPowerMah = (this.mScreenFullPowerEstimators[display].calculatePower(brightnessTime) * (bin + 0.5f)) / 100.0d;
                power += binPowerMah;
                bin++;
                j = 1000;
            }
        }
        return power;
    }

    private double calculateOthersPowerFromBrightness(BatteryStats batteryStats, long rawRealtimeUs) {
        int numDisplays = this.mScreenOnPowerEstimators.length;
        double power = 0.0d;
        for (int display = 0; display < numDisplays; display++) {
            int i = 0;
            long j = 1000;
            long displayTime = batteryStats.getScreenOnTime(rawRealtimeUs, 0) / 1000;
            power += this.mScreenOnPowerEstimators[display].calculatePower(displayTime);
            long[] calculateAppTimeUs = getSoleCalculateAppsBinTime(rawRealtimeUs);
            int bin = 0;
            while (bin < 100) {
                long brightnessTime = (batteryStats.getScreenBrightnessTimer(bin).getTotalTimeLocked(rawRealtimeUs, i) - calculateAppTimeUs[bin]) / j;
                double binPowerMah = (this.mScreenFullPowerEstimators[display].calculatePower(brightnessTime) * (bin + 0.5f)) / 100.0d;
                power += binPowerMah;
                bin++;
                displayTime = displayTime;
                i = 0;
                j = 1000;
            }
        }
        return power;
    }

    private double calculateSoleAppPowerFromBrightness(BatteryStats batteryStats, long rawRealtimeUs) {
        int numDisplays = this.mScreenOnPowerEstimators.length;
        double power = 0.0d;
        for (int display = 0; display < numDisplays; display++) {
            for (int i = 0; i < this.mAppDurationMap.size(); i++) {
                UidTimeStats uidTimeStats = this.mAppDurationMap.valueAt(i);
                if (uidTimeStats != null && uidTimeStats.screenBrightnessTimers != null) {
                    power = calculateAppsBinPower(display, uidTimeStats.screenBrightnessTimers, rawRealtimeUs);
                }
            }
        }
        return power;
    }

    private double calculateAppsBinPower(int display, BatteryStatsImpl.StopwatchTimer[] binTime, long rawRealtimeUs) {
        double binPowerMah;
        double calculatePower;
        double power = 0.0d;
        for (int bin = 0; bin < 100; bin++) {
            long brightnessTime = binTime[bin].getTotalTimeLocked(rawRealtimeUs, 0) / 1000;
            if (bin <= 50) {
                binPowerMah = ((this.mAppScreenFullPowerEstimators[display].calculatePower(brightnessTime) - this.mAppScreenLowPowerEstimators[display].calculatePower(brightnessTime)) * bin) / 50.0d;
                calculatePower = this.mAppScreenLowPowerEstimators[display].calculatePower(brightnessTime);
            } else {
                binPowerMah = ((this.mAppScreenHBMPowerEstimators[display].calculatePower(brightnessTime) - this.mAppScreenFullPowerEstimators[display].calculatePower(brightnessTime)) * (bin - 50)) / 49.0d;
                calculatePower = this.mAppScreenFullPowerEstimators[display].calculatePower(brightnessTime);
            }
            power += binPowerMah + calculatePower;
        }
        return power;
    }

    public long[] getSoleCalculateAppsBinTime(long rawRealtimeUs) {
        long[] totalBinTimes = new long[100];
        for (int bin = 0; bin < 100; bin++) {
            for (int i = 0; i < this.mAppDurationMap.size(); i++) {
                UidTimeStats timeStats = this.mAppDurationMap.valueAt(i);
                if (timeStats.screenBrightnessTimers != null) {
                    totalBinTimes[bin] = totalBinTimes[bin] + timeStats.screenBrightnessTimers[bin].getTotalTimeLocked(rawRealtimeUs, 0);
                }
            }
        }
        return totalBinTimes;
    }

    private void smearScreenBatteryDrain(SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders, PowerAndDuration totalPowerAndDuration, long rawRealtimeUs) {
        long totalActivityTimeMs = 0;
        SparseLongArray activityTimeArray = new SparseLongArray();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            BatteryStats.Uid uid = app.getBatteryStatsUid();
            long timeMs = getProcessForegroundTimeMs(uid, rawRealtimeUs);
            activityTimeArray.put(uid.getUid(), timeMs);
            if (!app.isVirtualUid()) {
                totalActivityTimeMs += timeMs;
            }
        }
        if (totalActivityTimeMs >= 600000) {
            double totalScreenPowerMah = totalPowerAndDuration.powerMah;
            int i2 = uidBatteryConsumerBuilders.size() - 1;
            while (i2 >= 0) {
                UidBatteryConsumer.Builder app2 = uidBatteryConsumerBuilders.valueAt(i2);
                long durationMs = activityTimeArray.get(app2.getUid(), 0L);
                SparseLongArray activityTimeArray2 = activityTimeArray;
                double powerMah = (durationMs * totalScreenPowerMah) / totalActivityTimeMs;
                app2.setUsageDurationMillis(0, durationMs).setConsumedPower(0, powerMah, 1);
                i2--;
                activityTimeArray = activityTimeArray2;
                totalScreenPowerMah = totalScreenPowerMah;
            }
        }
    }

    public long getProcessForegroundTimeMs(BatteryStats.Uid uid, long rawRealTimeUs) {
        int[] foregroundTypes = {0};
        long timeUs = 0;
        for (int type : foregroundTypes) {
            long localTime = uid.getProcessStateTime(type, rawRealTimeUs, 0);
            timeUs += localTime;
        }
        return Math.min(timeUs, getForegroundActivityTotalTimeUs(uid, rawRealTimeUs)) / 1000;
    }

    public long getForegroundActivityTotalTimeUs(BatteryStats.Uid uid, long rawRealtimeUs) {
        BatteryStats.Timer timer = uid.getForegroundActivityTimer();
        if (timer == null) {
            return 0L;
        }
        return timer.getTotalTimeLocked(rawRealtimeUs, 0);
    }

    public byte[] getDyAppScreenPower(BatteryStats batteryStats) {
        byte[] bytes;
        BatteryStatsManagerStubImpl impl;
        byte[] bytes2 = null;
        BatteryStatsManagerStubImpl impl2 = (BatteryStatsManagerStubImpl) BatteryStatsManagerStub.getInstance();
        if (impl2 != null) {
            double powerMah = 0.0d;
            long fgTimeMs = 0;
            long[] brightnessBinTimes = new long[100];
            int numDisplays = this.mScreenOnPowerEstimators.length;
            long rawRealtimeUs = SystemClock.elapsedRealtime() * 1000;
            SparseArray<BatteryStatsManagerStubImpl.SoleCalculateApp> uidMap = impl2.getScreenCalculateMap();
            int i = 0;
            while (true) {
                if (i >= uidMap.size()) {
                    bytes = bytes2;
                    break;
                }
                int uid = uidMap.keyAt(i);
                BatteryStatsManagerStubImpl.SoleCalculateApp uidStats = uidMap.get(uid);
                if (uidStats == null) {
                    bytes = bytes2;
                    impl = impl2;
                } else if (uidStats.screenBrightnessTimers == null) {
                    bytes = bytes2;
                    impl = impl2;
                } else {
                    bytes = bytes2;
                    if (TextUtils.equals(uidStats.packageName, "com.ss.android.ugc.aweme")) {
                        BatteryStatsImpl.Uid bsiUid = (BatteryStatsImpl.Uid) batteryStats.getUidStats().get(uidStats.uid);
                        long timeMs = getProcessForegroundTimeMs(bsiUid, rawRealtimeUs);
                        long fgTimeMs2 = timeMs;
                        int display = 0;
                        while (display < numDisplays) {
                            powerMah = powerMah + this.mScreenOnPowerEstimators[display].calculatePower(timeMs) + calculateAppsBinPower(display, uidStats.screenBrightnessTimers, rawRealtimeUs);
                            display++;
                            fgTimeMs2 = fgTimeMs2;
                        }
                        long fgTimeMs3 = fgTimeMs2;
                        for (int j = 0; j < 100; j++) {
                            brightnessBinTimes[j] = uidStats.screenBrightnessTimers[j].getTotalTimeLocked(rawRealtimeUs, 0);
                        }
                        fgTimeMs = fgTimeMs3;
                    } else {
                        impl = impl2;
                    }
                }
                i++;
                bytes2 = bytes;
                impl2 = impl;
            }
            Parcel out = Parcel.obtain();
            try {
                out.writeDouble(powerMah);
                out.writeLong(fgTimeMs);
                out.writeLongArray(brightnessBinTimes);
                byte[] bytes3 = out.marshall();
                return bytes3;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                out.recycle();
            }
        } else {
            bytes = null;
        }
        return bytes;
    }
}
