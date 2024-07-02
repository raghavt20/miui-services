package com.android.server.location.provider;

import android.location.GnssStatus;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import com.android.server.location.gnss.GnssLocationProvider;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.location.provider.LocationProviderManager;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import miui.util.ObjectReference;
import miui.util.ReflectionUtils;

/* loaded from: classes.dex */
public class AmapCustomImpl implements AmapCustomStub {
    private static boolean DEBUG = Build.IS_DEBUGGABLE;
    private static final String TAG = "AmapCustomImpl";
    private ArrayList<Float> mCn0s = new ArrayList<>();
    private long mUpdateSvStatusTime = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AmapCustomImpl> {

        /* compiled from: AmapCustomImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AmapCustomImpl INSTANCE = new AmapCustomImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AmapCustomImpl m1863provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AmapCustomImpl m1862provideNewInstance() {
            return new AmapCustomImpl();
        }
    }

    public void setLastSvStatus(GnssStatus gnssStatus) {
        synchronized (this.mCn0s) {
            this.mCn0s.clear();
            for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
                this.mCn0s.add(Float.valueOf(gnssStatus.getCn0DbHz(i)));
            }
            this.mUpdateSvStatusTime = SystemClock.elapsedRealtime();
        }
    }

    public boolean onSpecialExtraCommand(LocationProviderManager manager, int uid, String command, Bundle extras) {
        if (command == null || extras == null) {
            Log.e(TAG, "Exception: command/bundle is null");
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "provider " + manager.getName() + " , " + command);
        }
        boolean isHandled = false;
        if (AmapExtraCommand.isSupported(command, extras)) {
            isHandled = true;
            if ("gps".equals(manager.getName()) && AmapExtraCommand.GPS_TIMEOUT_CMD.equals(command)) {
                handleAmapGpsTimeoutCmd(manager, uid, command, extras);
            }
        }
        return isHandled;
    }

    private void handleAmapGpsTimeoutCmd(LocationProviderManager locationProviderManager, int i, String str, Bundle bundle) {
        fetchRegistrationState(locationProviderManager, bundle);
        fetchPowerPolicyState(bundle);
        ObjectReference tryGetObjectField = ReflectionUtils.tryGetObjectField(locationProviderManager, "mProvider", MockableLocationProvider.class);
        MockableLocationProvider mockableLocationProvider = tryGetObjectField != null ? (MockableLocationProvider) tryGetObjectField.get() : null;
        if (mockableLocationProvider != null) {
            bundle.putInt(AmapExtraCommand.GNSS_REAL_KEY, !mockableLocationProvider.isMock() ? 1 : 0);
            fetchGnssState(mockableLocationProvider.getProvider(), bundle);
        }
        bundle.putString(AmapExtraCommand.VERSION_KEY, AmapExtraCommand.VERSION_NAME);
        if (DEBUG) {
            Log.d(TAG, "response: " + bundle.toString());
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0043, code lost:
    
        r16 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x004b, code lost:
    
        if (r13.isActive() == false) goto L21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x004d, code lost:
    
        r15 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0052, code lost:
    
        r5 = r15;
        r6 = r13.getPermissionLevel();
        r15 = r13.getLastDeliveredLocation();
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x005c, code lost:
    
        if (r15 == null) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0083, code lost:
    
        if (((java.lang.Boolean) miui.util.ReflectionUtils.tryGetObjectField(r13, "mIsUsingHighPower", java.lang.Boolean.class).get()).booleanValue() == false) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x0085, code lost:
    
        r16 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0087, code lost:
    
        r9 = r16;
        r10 = r13.isForeground() ? 1 : 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x0066, code lost:
    
        r7 = java.lang.System.currentTimeMillis() - r15.getTime();
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0069, code lost:
    
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x0050, code lost:
    
        r15 = 0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void fetchRegistrationState(com.android.server.location.provider.LocationProviderManager r23, android.os.Bundle r24) {
        /*
            r22 = this;
            r1 = r24
            java.lang.String r0 = "mRegistrations"
            java.lang.Class<android.util.ArrayMap> r2 = android.util.ArrayMap.class
            r3 = r23
            miui.util.ObjectReference r2 = miui.util.ReflectionUtils.tryGetObjectField(r3, r0, r2)
            if (r2 == 0) goto L15
            java.lang.Object r0 = r2.get()
            android.util.ArrayMap r0 = (android.util.ArrayMap) r0
            goto L16
        L15:
            r0 = 0
        L16:
            r4 = r0
            if (r4 != 0) goto L21
            java.lang.String r0 = "AmapCustomImpl"
            java.lang.String r5 = "Exception: mRegistrations is null"
            android.util.Log.e(r0, r5)
            return
        L21:
            r5 = -1
            r6 = -1
            r7 = -1
            r9 = -1
            r10 = -1
            monitor-enter(r4)
            int r0 = r4.size()     // Catch: java.lang.Throwable -> Lb9
            java.lang.String r11 = "listenerHashcode"
            int r11 = r1.getInt(r11)     // Catch: java.lang.Throwable -> Lb9
            r12 = 0
        L33:
            if (r12 >= r0) goto L98
            java.lang.Object r13 = r4.valueAt(r12)     // Catch: java.lang.Throwable -> Lb9
            com.android.server.location.provider.LocationProviderManager$Registration r13 = (com.android.server.location.provider.LocationProviderManager.Registration) r13     // Catch: java.lang.Throwable -> Lb9
            r14 = r22
            int r15 = r14.listenerHashCode(r13)     // Catch: java.lang.Throwable -> L96
            if (r15 != r11) goto L8f
            boolean r15 = r13.isActive()     // Catch: java.lang.Throwable -> L96
            r16 = 0
            r17 = 1
            if (r15 == 0) goto L50
            r15 = r17
            goto L52
        L50:
            r15 = r16
        L52:
            r5 = r15
            int r15 = r13.getPermissionLevel()     // Catch: java.lang.Throwable -> L96
            r6 = r15
            android.location.Location r15 = r13.getLastDeliveredLocation()     // Catch: java.lang.Throwable -> L96
            if (r15 == 0) goto L6d
            long r18 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> L69
            long r20 = r15.getTime()     // Catch: java.lang.Throwable -> L69
            long r7 = r18 - r20
            goto L6d
        L69:
            r0 = move-exception
            r19 = r2
            goto Lbe
        L6d:
            r18 = r0
            java.lang.String r0 = "mIsUsingHighPower"
            r19 = r2
            java.lang.Class<java.lang.Boolean> r2 = java.lang.Boolean.class
            miui.util.ObjectReference r0 = miui.util.ReflectionUtils.tryGetObjectField(r13, r0, r2)     // Catch: java.lang.Throwable -> Lc0
            java.lang.Object r2 = r0.get()     // Catch: java.lang.Throwable -> Lc0
            java.lang.Boolean r2 = (java.lang.Boolean) r2     // Catch: java.lang.Throwable -> Lc0
            boolean r2 = r2.booleanValue()     // Catch: java.lang.Throwable -> Lc0
            if (r2 == 0) goto L87
            r16 = r17
        L87:
            r9 = r16
            boolean r2 = r13.isForeground()     // Catch: java.lang.Throwable -> Lc0
            r10 = r2
            goto L9e
        L8f:
            r18 = r0
            r19 = r2
            int r12 = r12 + 1
            goto L33
        L96:
            r0 = move-exception
            goto Lbc
        L98:
            r14 = r22
            r18 = r0
            r19 = r2
        L9e:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lc0
            java.lang.String r0 = "app_active"
            r1.putInt(r0, r5)
            java.lang.String r0 = "app_permission"
            r1.putInt(r0, r6)
            java.lang.String r0 = "app_last_report_second"
            r1.putLong(r0, r7)
            java.lang.String r0 = "app_power_mode"
            r1.putInt(r0, r9)
            java.lang.String r0 = "app_forground"
            r1.putInt(r0, r10)
            return
        Lb9:
            r0 = move-exception
            r14 = r22
        Lbc:
            r19 = r2
        Lbe:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lc0
            throw r0
        Lc0:
            r0 = move-exception
            goto Lbe
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.provider.AmapCustomImpl.fetchRegistrationState(com.android.server.location.provider.LocationProviderManager, android.os.Bundle):void");
    }

    private int listenerHashCode(LocationProviderManager.Registration registration) {
        String listenerId = registration.getIdentity().getListenerId();
        int length = listenerId.length();
        int index = listenerId.lastIndexOf(64, length - 1);
        if (index == -1 || length - index < 1) {
            return -1;
        }
        try {
            int listenerHashCode = Integer.parseInt(listenerId.substring(index + 1));
            return listenerHashCode;
        } catch (Exception e) {
            Log.e(TAG, "caught exception from Integer.parseInt " + e.getMessage());
            return -1;
        }
    }

    private void fetchPowerPolicyState(Bundle extras) {
        extras.putInt(AmapExtraCommand.APP_CTRL_KEY, -1);
        extras.putString(AmapExtraCommand.APP_CTRL_LOG_KEY, "The device is not supported");
    }

    private void fetchGnssState(AbstractLocationProvider abstractLocationProvider, Bundle bundle) {
        AbstractLocationProvider abstractLocationProvider2;
        if (abstractLocationProvider != null && (abstractLocationProvider instanceof DelegateLocationProvider)) {
            ObjectReference tryGetObjectField = ReflectionUtils.tryGetObjectField(abstractLocationProvider, "mDelegate", AbstractLocationProvider.class);
            abstractLocationProvider2 = tryGetObjectField != null ? (AbstractLocationProvider) tryGetObjectField.get() : null;
        } else {
            abstractLocationProvider2 = abstractLocationProvider;
        }
        if (abstractLocationProvider2 != null && (abstractLocationProvider2 instanceof GnssLocationProvider)) {
            ObjectReference tryGetObjectField2 = ReflectionUtils.tryGetObjectField((GnssLocationProvider) abstractLocationProvider2, "mLastFixTime", Long.class);
            boolean booleanValue = ((Boolean) ReflectionUtils.tryGetObjectField((GnssLocationProvider) abstractLocationProvider2, "mStarted", Boolean.class).get()).booleanValue();
            bundle.putInt(AmapExtraCommand.GNSS_STATUS_KEY, booleanValue ? 1 : 0);
            bundle.putLong(AmapExtraCommand.GNSS_LAST_RPT_TIME_KEY, SystemClock.elapsedRealtime() - ((Long) tryGetObjectField2.get()).longValue());
            synchronized (this.mCn0s) {
                int i = -1;
                int i2 = -1;
                int i3 = -1;
                try {
                    try {
                        long elapsedRealtime = SystemClock.elapsedRealtime() - this.mUpdateSvStatusTime;
                        if (booleanValue && elapsedRealtime <= 5000) {
                            i = 0;
                            i2 = 0;
                            i3 = this.mCn0s.size();
                            int i4 = 0;
                            while (i4 < i3) {
                                float floatValue = this.mCn0s.get(i4).floatValue();
                                AbstractLocationProvider abstractLocationProvider3 = abstractLocationProvider2;
                                ObjectReference objectReference = tryGetObjectField2;
                                if (floatValue > 0.0d) {
                                    i++;
                                }
                                if (floatValue > 20.0d) {
                                    i2++;
                                }
                                i4++;
                                abstractLocationProvider2 = abstractLocationProvider3;
                                tryGetObjectField2 = objectReference;
                            }
                        }
                        bundle.putInt(AmapExtraCommand.SAT_ALL_CNT_KEY, i3);
                        bundle.putInt(AmapExtraCommand.SAT_SNR_OVER0_CNT_KEY, i);
                        bundle.putInt(AmapExtraCommand.SAT_SNR_OVER20_CNT_KEY, i2);
                        return;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        Log.e(TAG, "Exception: bad argument for provider");
    }
}
