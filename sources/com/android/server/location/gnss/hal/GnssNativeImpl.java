package com.android.server.location.gnss.hal;

import android.os.SystemClock;
import android.util.Log;
import com.android.server.location.LocationDumpLogStub;
import com.android.server.location.gnss.GnssCollectDataStub;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.location.gnss.GnssLocationProviderStub;
import com.miui.base.MiuiStubRegistry;

/* loaded from: classes.dex */
public class GnssNativeImpl implements GnssNativeStub {
    private boolean mNmeaStatus = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssNativeImpl> {

        /* compiled from: GnssNativeImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssNativeImpl INSTANCE = new GnssNativeImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssNativeImpl m1825provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssNativeImpl m1824provideNewInstance() {
            return new GnssNativeImpl();
        }
    }

    public boolean start() {
        if (GnssPowerOptimizeStub.getInstance().blockEngineStart()) {
            GnssPowerOptimizeStub.getInstance().setEngineStatus(1);
            GnssEventTrackingStub.getInstance().recordEngineUsage(1, SystemClock.elapsedRealtime());
            return true;
        }
        return false;
    }

    public boolean nativeStart() {
        LocationDumpLogStub.getInstance().setRecordLoseLocation(true);
        if (GnssPowerOptimizeStub.getInstance().getEngineStatus() == 2) {
            return true;
        }
        GnssPowerOptimizeStub.getInstance().setEngineStatus(2);
        GnssEventTrackingStub.getInstance().recordEngineUsage(2, SystemClock.elapsedRealtime());
        return false;
    }

    public boolean nativeStop() {
        if (GnssPowerOptimizeStub.getInstance().engineStoppedByGpo()) {
            GnssPowerOptimizeStub.getInstance().setEngineStatus(3);
            GnssEventTrackingStub.getInstance().recordEngineUsage(3, SystemClock.elapsedRealtime());
            return false;
        }
        GnssPowerOptimizeStub.getInstance().clearLocationRequest();
        GnssPowerOptimizeStub.getInstance().setEngineStatus(4);
        GnssEventTrackingStub.getInstance().recordEngineUsage(4, SystemClock.elapsedRealtime());
        return false;
    }

    public void reportStatus(int gnssStatus) {
        if (gnssStatus == 3) {
            Log.d("GnssManager", "gnss engine report: on");
            GnssCollectDataStub.getInstance().savePoint(1, (String) null);
            LocationDumpLogStub.getInstance().addToBugreport(2, "gnss engine report: on");
            if (GnssLocationProviderStub.getInstance().getSendingSwitch()) {
                GnssLocationProviderStub.getInstance().notifyState(1);
            }
            GnssScoringModelStub.getInstance().startScoringModel(true);
            return;
        }
        if (gnssStatus == 4) {
            Log.d("GnssManager", "gnss engine report: off");
            GnssCollectDataStub.getInstance().savePoint(3, (String) null);
            LocationDumpLogStub.getInstance().addToBugreport(2, "gnss engine report: off");
            if (GnssLocationProviderStub.getInstance().getSendingSwitch()) {
                GnssLocationProviderStub.getInstance().notifyState(2);
            }
            GnssScoringModelStub.getInstance().startScoringModel(false);
        }
    }

    public boolean reportNmea(String nmea) {
        GnssLocationProviderStub.getInstance().precisionProcessByType(nmea);
        LocationDumpLogStub.getInstance().addToBugreport(4, nmea);
        GnssCollectDataStub.getInstance().savePoint(6, nmea);
        if (nmea.startsWith("$P") || !this.mNmeaStatus) {
            return false;
        }
        return true;
    }

    public void setNmeaStatus(boolean nmeaStatus) {
        this.mNmeaStatus = nmeaStatus;
    }
}
