package com.android.server;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.storage.IStorageManager;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MountServiceIdlerImpl implements MountServiceIdlerStub {
    private static final long FINISH_INTERVAL_TIME = 7200000;
    private static final int MINIMUM_BATTERY_LEVEL = 10;
    private static final long MINIMUM_INTERVAL_TIME = 1800000;
    private static final String TAG = "MountServiceIdlerImpl";
    IStorageManager mSm;
    private long sNextTrimDuration = FINISH_INTERVAL_TIME;
    private BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() { // from class: com.android.server.MountServiceIdlerImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MountServiceIdlerImpl.this.mSm == null) {
                MountServiceIdlerImpl.this.mSm = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
            }
            if (MountServiceIdlerImpl.this.mSm == null) {
                Slog.w(MountServiceIdlerImpl.TAG, "Failed to find running mount service");
                return;
            }
            try {
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    Slog.d(MountServiceIdlerImpl.TAG, "Get the action of screen on");
                    MountServiceIdlerImpl.this.mSm.abortIdleMaintenance();
                }
            } catch (Exception e) {
                Slog.w(MountServiceIdlerImpl.TAG, "Failed to send stop defrag or trim command to vold", e);
            }
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MountServiceIdlerImpl> {

        /* compiled from: MountServiceIdlerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MountServiceIdlerImpl INSTANCE = new MountServiceIdlerImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MountServiceIdlerImpl m256provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MountServiceIdlerImpl m255provideNewInstance() {
            return new MountServiceIdlerImpl();
        }
    }

    MountServiceIdlerImpl() {
    }

    public boolean runIdleMaint(Context context, int jobId, ComponentName componentName) {
        BatteryManager bm = (BatteryManager) context.getSystemService("batterymanager");
        int batteryLevel = bm.getIntProperty(4);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        boolean isInteractive = pm.isInteractive();
        if (!isInteractive && batteryLevel >= 10) {
            this.sNextTrimDuration = FINISH_INTERVAL_TIME;
            return false;
        }
        this.sNextTrimDuration >>= 1;
        internalScheduleIdlePass(context, jobId, componentName);
        return true;
    }

    public boolean internalScheduleIdlePass(Context context, int jobId, ComponentName componentName) {
        JobScheduler tm = (JobScheduler) context.getSystemService("jobscheduler");
        if (this.sNextTrimDuration < 1800000) {
            this.sNextTrimDuration = 1800000L;
        }
        Slog.i(TAG, "sNextTrimDuration :  " + this.sNextTrimDuration);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);
        builder.setMinimumLatency(this.sNextTrimDuration);
        tm.schedule(builder.build());
        return true;
    }

    public void addScreenOnFilter(Context context) {
        if (Build.IS_MIUI) {
            IntentFilter screenOnFilter = new IntentFilter();
            screenOnFilter.addAction("android.intent.action.SCREEN_ON");
            context.registerReceiver(this.mScreenOnReceiver, screenOnFilter, 2);
        }
    }
}
