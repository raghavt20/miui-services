package com.android.server.location;

import android.R;
import android.os.SystemProperties;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.Properties;

/* loaded from: classes.dex */
public class LocationDumpLogImpl implements LocationDumpLogStub {
    private static final String DUMP_TAG_GLP = "=MI GLP= ";
    private static final String DUMP_TAG_GLP_EN = "=MI GLP EN=";
    private static final String DUMP_TAG_LMS = "=MI LMS= ";
    private static final String DUMP_TAG_NMEA = "=MI NMEA=";
    private static final GnssLocalLog mdumpLms = new GnssLocalLog(1000);
    private static final GnssLocalLog mdumpGlp = new GnssLocalLog(1000);
    private static final GnssLocalLog mdumpNmea = new GnssLocalLog(20000);
    private int defaultNetworkProviderName = R.string.display_manager_built_in_display_name;
    private int defaultFusedProviderName = R.string.db_default_journal_mode;
    private int defaultGeocoderProviderName = R.string.db_default_sync_mode;
    private int defaultGeofenceProviderName = R.string.db_wal_sync_mode;
    private boolean mRecordLoseCount = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LocationDumpLogImpl> {

        /* compiled from: LocationDumpLogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LocationDumpLogImpl INSTANCE = new LocationDumpLogImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LocationDumpLogImpl m1771provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LocationDumpLogImpl m1770provideNewInstance() {
            return new LocationDumpLogImpl();
        }
    }

    public void addToBugreport(int type, String log) {
        switchTypeToDump(type).log(switchTypeToLogTag(type) + log);
    }

    public void setLength(int type, int length) {
        switchTypeToDump(type).setLength(length);
    }

    public String getConfig(Properties properties, String config, String defaultConfig) {
        return properties.getProperty(config, defaultConfig);
    }

    public boolean getRecordLoseLocation() {
        return this.mRecordLoseCount;
    }

    public void setRecordLoseLocation(boolean newValue) {
        this.mRecordLoseCount = newValue;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int getDefaultProviderName(String type) {
        char c;
        if (isXOptMode() && !isCnVersion()) {
            this.defaultNetworkProviderName = R.string.display_manager_hdmi_display_name;
            this.defaultFusedProviderName = R.string.display_manager_hdmi_display_name;
        }
        switch (type.hashCode()) {
            case 97798435:
                if (type.equals("fused")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1837067124:
                if (type.equals("geocoder")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1839549312:
                if (type.equals("geofence")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1843485230:
                if (type.equals("network")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return this.defaultNetworkProviderName;
            case 1:
                return this.defaultFusedProviderName;
            case 2:
                return this.defaultGeocoderProviderName;
            default:
                return this.defaultGeofenceProviderName;
        }
    }

    public void dump(int type, PrintWriter pw) {
        switchTypeToDump(type).dump(pw);
    }

    private boolean isXOptMode() {
        return !SystemProperties.getBoolean(DisplayModeDirectorImpl.MIUI_OPTIMIZATION_PROP, true);
    }

    private boolean isCnVersion() {
        return "CN".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region"));
    }

    private GnssLocalLog switchTypeToDump(int type) {
        switch (type) {
            case 1:
                return mdumpLms;
            case 2:
            case 3:
                return mdumpGlp;
            default:
                return mdumpNmea;
        }
    }

    private String switchTypeToLogTag(int type) {
        switch (type) {
            case 1:
                return DUMP_TAG_LMS;
            case 2:
                return DUMP_TAG_GLP;
            case 3:
                return DUMP_TAG_GLP_EN;
            default:
                return DUMP_TAG_NMEA;
        }
    }

    public void clearData() {
        mdumpGlp.clearData();
        mdumpNmea.clearData();
    }
}
