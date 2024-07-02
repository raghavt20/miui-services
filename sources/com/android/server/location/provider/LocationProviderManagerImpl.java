package com.android.server.location.provider;

import android.content.Context;
import android.location.LocationRequest;
import android.location.LocationResult;
import android.location.util.identity.CallerIdentity;
import android.os.Build;
import android.util.Log;
import com.android.internal.listeners.ListenerExecutor;
import com.android.internal.util.Preconditions;
import com.android.server.location.GnssMockLocationOptStub;
import com.android.server.location.LocationDumpLogStub;
import com.android.server.location.MiuiBlurLocationManagerStub;
import com.android.server.location.gnss.GnssCollectDataStub;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.location.gnss.GnssLocationProviderStub;
import com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub;
import com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecoveryStub;
import com.android.server.location.gnss.hal.GnssPowerOptimizeStub;
import com.android.server.location.provider.LocationProviderManager;
import com.miui.base.MiuiStubRegistry;
import java.lang.reflect.Method;
import java.util.function.Function;
import miui.util.ReflectionUtils;

/* loaded from: classes.dex */
public class LocationProviderManagerImpl implements LocationProviderManagerStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LocationProviderManagerImpl> {

        /* compiled from: LocationProviderManagerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LocationProviderManagerImpl INSTANCE = new LocationProviderManagerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public LocationProviderManagerImpl m1865provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public LocationProviderManagerImpl m1864provideNewInstance() {
            return new LocationProviderManagerImpl();
        }
    }

    public void onRegister(Context context, String requestInfo, String name, MockableLocationProvider provider, CallerIdentity identity, LocationRequest request) {
        LocationDumpLogStub.getInstance().addToBugreport(1, requestInfo);
        if ("gps".equals(name)) {
            if (provider.isMock()) {
                Log.d("LocationManagerService", name + "to sent mock intent");
                GnssLocationProviderStub.getInstance().notifyCallerName(identity.getPackageName() + " gps is mock");
                GnssSelfRecoveryStub.getInstance().diagnosticMockLocation();
            } else {
                GnssLocationProviderStub.getInstance().notifyCallerName(identity.getPackageName());
            }
        }
        if (MiuiBlurLocationManagerStub.get().isBlurLocationMode(identity.getUid(), identity.getPackageName())) {
            GnssLocationProviderStub.getInstance().notifyCallerName(identity.getPackageName(), "blurLocation_notify is on");
        }
        GnssEventTrackingStub.getInstance().recordRequest(name, identity.getListenerId().hashCode(), identity.getPackageName(), request.getIntervalMillis());
        GnssMockLocationOptStub.getInstance().handleNaviBatRegisteration(context, identity, name, provider);
    }

    public void onUnregister(String removeInfo, String name, MockableLocationProvider provider, CallerIdentity identity, boolean hasLocationPermissions, boolean foreground) {
        GnssPowerOptimizeStub.getInstance().removeLocationRequestId(identity.getListenerId().hashCode());
        if ("gps".equalsIgnoreCase(name) && provider.isMock()) {
            Log.d("LocationManagerService", name + "to sent remove mock intent");
            GnssLocationProviderStub.getInstance().notifyCallerName("remove mock intent");
        }
        if (MiuiBlurLocationManagerStub.get().isBlurLocationMode(identity.getUid(), identity.getPackageName())) {
            GnssLocationProviderStub.getInstance().notifyCallerName(identity.getPackageName(), "blurLocation_notify is off");
        }
        LocationDumpLogStub.getInstance().addToBugreport(1, removeInfo);
        GnssEventTrackingStub.getInstance().recordRemove(name, identity.getListenerId().hashCode(), foreground, hasLocationPermissions);
        GnssBackgroundUsageOptStub.getInstance().remove(identity.getUid(), identity.getPid(), name, identity.getListenerId());
        GnssMockLocationOptStub.getInstance().handleNaviBatUnregisteration(identity, name, provider);
    }

    public void onForegroundChanged(int uid, boolean foreground, String name, boolean hasLocationPermissions, CallerIdentity identity) {
        LocationDumpLogStub.getInstance().addToBugreport(1, "request from uid " + uid + " " + identity.getPackageName() + " is now " + (foreground ? "foreground" : "background"));
        GnssEventTrackingStub.getInstance().recordChangeToBackground(name, identity.getListenerId().hashCode(), identity.getPackageName(), foreground, hasLocationPermissions);
        if (!foreground) {
            GnssCollectDataStub.getInstance().saveUngrantedBackPermission(identity.getUid(), identity.getPid(), identity.getPackageName());
        }
    }

    public boolean onReportBlurLocation(LocationProviderManager manager, final LocationResult locationResult, final CallerIdentity identity, Object lock) {
        if (!"gps".equals(manager.getName())) {
            return false;
        }
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(lock));
        }
        final boolean[] hasBlurApp = {false};
        Function<LocationProviderManager.Registration, ListenerExecutor.ListenerOperation<LocationProviderManager.LocationTransport>> function = new Function() { // from class: com.android.server.location.provider.LocationProviderManagerImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return LocationProviderManagerImpl.lambda$onReportBlurLocation$0(identity, hasBlurApp, locationResult, (LocationProviderManager.Registration) obj);
            }
        };
        deliverToListeners(manager, function);
        return hasBlurApp[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ListenerExecutor.ListenerOperation lambda$onReportBlurLocation$0(CallerIdentity identity, boolean[] hasBlurApp, LocationResult locationResult, LocationProviderManager.Registration registration) {
        try {
            CallerIdentity regIdentity = registration.getIdentity();
            if (regIdentity.getPackageName().equals(identity.getPackageName()) && regIdentity.getUid() == identity.getUid()) {
                hasBlurApp[0] = true;
                return registration.acceptLocationChange(locationResult);
            }
            return null;
        } catch (Exception e) {
            Log.e("LocationManagerService", "reflect exception!", e);
            return null;
        }
    }

    public void mayNotifyGpsBlurListener(LocationProviderManager manager, final LocationResult locationResult, Object lock) {
        if (!"gps".equals(manager.getName())) {
            return;
        }
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(lock));
        }
        Function<LocationProviderManager.Registration, ListenerExecutor.ListenerOperation<LocationProviderManager.LocationTransport>> function = new Function() { // from class: com.android.server.location.provider.LocationProviderManagerImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return LocationProviderManagerImpl.lambda$mayNotifyGpsBlurListener$1(locationResult, (LocationProviderManager.Registration) obj);
            }
        };
        deliverToListeners(manager, function);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ListenerExecutor.ListenerOperation lambda$mayNotifyGpsBlurListener$1(LocationResult locationResult, LocationProviderManager.Registration registration) {
        try {
            if (MiuiBlurLocationManagerStub.get().isBlurLocationMode(registration.getIdentity())) {
                return registration.acceptLocationChange(locationResult);
            }
            return null;
        } catch (Exception e) {
            Log.e("LocationManagerService", "reflect exception!", e);
            return null;
        }
    }

    private static void deliverToListeners(LocationProviderManager manager, Function function) {
        try {
            Method method = ReflectionUtils.findMethodBestMatch(manager.getClass().getSuperclass(), "deliverToListeners", new Class[]{Function.class});
            method.invoke(manager, function);
        } catch (Exception e) {
            Log.e("LocationManagerService", "deliverToListeners exception!", e);
        }
    }

    public boolean onMockModeChanged(boolean flag, String name, MockableLocationProvider provider, MockLocationProvider testProvider) {
        return GnssMockLocationOptStub.getInstance().recordOrderSchedule(flag, name, provider, testProvider);
    }
}
