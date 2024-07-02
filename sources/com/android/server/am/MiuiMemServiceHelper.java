package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.Slog;

/* loaded from: classes.dex */
final class MiuiMemServiceHelper extends BroadcastReceiver {
    private static final long CHARGING_RECLAIM_DELAYED = 60000;
    private static final boolean DEBUG = MiuiMemoryService.DEBUG;
    private static final long SCREENOFF_RECLAIM_DELAYED = 1200000;
    private static final String TAG = "MiuiMemoryService";
    private IntentFilter mFilter;
    private boolean mIsCharging;
    private boolean mIsScreenOff;
    private MiuiMemoryService mMemService;
    private PowerManager mPowerManager;
    private boolean mSetIsSatisfied;
    private WorkHandler mWorkHandler;
    final HandlerThread mWorkThread = new HandlerThread("MiuiMemoryService_Helper");

    /* loaded from: classes.dex */
    private class WorkHandler extends Handler {
        public static final int CHARGING_RECLAIM = 1;
        public static final int SCREENOFF_RECLAIM = 2;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                case 2:
                    Slog.v("MiuiMemoryService", "start screen off reclaim... " + msg.what);
                    MiuiMemServiceHelper.this.mMemService.runProcsCompaction(2);
                    return;
                default:
                    return;
            }
        }
    }

    public MiuiMemServiceHelper(Context context, MiuiMemoryService service) {
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMemService = service;
        if (this.mPowerManager != null) {
            registerBroadcast(context);
        } else {
            Slog.e("MiuiMemoryService", "Register broadcast failed!");
        }
    }

    public void startWork() {
        this.mWorkThread.start();
        this.mWorkHandler = new WorkHandler(this.mWorkThread.getLooper());
        Process.setThreadGroupAndCpuset(this.mWorkThread.getThreadId(), 2);
    }

    private void registerBroadcast(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        this.mFilter = intentFilter;
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mFilter.addAction("android.intent.action.DREAMING_STARTED");
        this.mFilter.addAction("android.intent.action.DREAMING_STOPPED");
        this.mFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        this.mFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        context.registerReceiver(this, this.mFilter);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Failed to find 'out' block for switch in B:6:0x004c. Please report as an issue. */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0069  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x0088  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x00a7  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x00b1  */
    /* JADX WARN: Removed duplicated region for block: B:31:? A[RETURN, SYNTHETIC] */
    @Override // android.content.BroadcastReceiver
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void onReceive(android.content.Context r8, android.content.Intent r9) {
        /*
            r7 = this;
            java.lang.String r0 = r9.getAction()
            int r1 = r0.hashCode()
            r2 = 2
            r3 = 0
            r4 = 1
            switch(r1) {
                case -2128145023: goto L41;
                case -1886648615: goto L37;
                case -1454123155: goto L2d;
                case 244891622: goto L23;
                case 257757490: goto L19;
                case 1019184907: goto Lf;
                default: goto Le;
            }
        Le:
            goto L4b
        Lf:
            java.lang.String r1 = "android.intent.action.ACTION_POWER_CONNECTED"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto Le
            r1 = 4
            goto L4c
        L19:
            java.lang.String r1 = "android.intent.action.DREAMING_STOPPED"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto Le
            r1 = r3
            goto L4c
        L23:
            java.lang.String r1 = "android.intent.action.DREAMING_STARTED"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto Le
            r1 = r2
            goto L4c
        L2d:
            java.lang.String r1 = "android.intent.action.SCREEN_ON"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto Le
            r1 = r4
            goto L4c
        L37:
            java.lang.String r1 = "android.intent.action.ACTION_POWER_DISCONNECTED"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto Le
            r1 = 5
            goto L4c
        L41:
            java.lang.String r1 = "android.intent.action.SCREEN_OFF"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto Le
            r1 = 3
            goto L4c
        L4b:
            r1 = -1
        L4c:
            switch(r1) {
                case 0: goto L59;
                case 1: goto L62;
                case 2: goto L56;
                case 3: goto L56;
                case 4: goto L53;
                case 5: goto L50;
                default: goto L4f;
            }
        L4f:
            return
        L50:
            r7.mIsCharging = r3
            goto L65
        L53:
            r7.mIsCharging = r4
            goto L65
        L56:
            r7.mIsScreenOff = r4
            goto L65
        L59:
            android.os.PowerManager r1 = r7.mPowerManager
            boolean r1 = r1.isInteractive()
            if (r1 != 0) goto L62
            return
        L62:
            r7.mIsScreenOff = r3
        L65:
            boolean r1 = com.android.server.am.MiuiMemServiceHelper.DEBUG
            if (r1 == 0) goto L84
            boolean r1 = r7.mIsCharging
            java.lang.Boolean r1 = java.lang.Boolean.valueOf(r1)
            boolean r5 = r7.mIsScreenOff
            java.lang.Boolean r5 = java.lang.Boolean.valueOf(r5)
            java.lang.Object[] r1 = new java.lang.Object[]{r0, r1, r5}
            java.lang.String r5 = "Received:%s, cur:%s,%s"
            java.lang.String r1 = java.lang.String.format(r5, r1)
            java.lang.String r5 = "MiuiMemoryService"
            android.util.Slog.v(r5, r1)
        L84:
            boolean r1 = r7.mIsScreenOff
            if (r1 == 0) goto L99
            com.android.server.am.MiuiMemServiceHelper$WorkHandler r1 = r7.mWorkHandler
            boolean r1 = r1.hasMessages(r2)
            if (r1 != 0) goto L99
            com.android.server.am.MiuiMemServiceHelper$WorkHandler r1 = r7.mWorkHandler
            r5 = 1200000(0x124f80, double:5.92879E-318)
            r1.sendEmptyMessageDelayed(r2, r5)
            goto La3
        L99:
            com.android.server.am.MiuiMemServiceHelper$WorkHandler r1 = r7.mWorkHandler
            r1.removeMessages(r2)
            com.android.server.am.MiuiMemoryService r1 = r7.mMemService
            r1.interruptProcsCompaction()
        La3:
            boolean r1 = r7.mIsCharging
            if (r1 == 0) goto Lac
            boolean r1 = r7.mIsScreenOff
            if (r1 == 0) goto Lac
            r3 = r4
        Lac:
            r1 = r3
            boolean r2 = r7.mSetIsSatisfied
            if (r1 == r2) goto Lc8
            com.android.server.am.MiuiMemServiceHelper$WorkHandler r2 = r7.mWorkHandler
            r2.removeMessages(r4)
            if (r1 == 0) goto Lc1
            com.android.server.am.MiuiMemServiceHelper$WorkHandler r2 = r7.mWorkHandler
            r5 = 60000(0xea60, double:2.9644E-319)
            r2.sendEmptyMessageDelayed(r4, r5)
            goto Lc6
        Lc1:
            com.android.server.am.MiuiMemoryService r2 = r7.mMemService
            r2.interruptProcsCompaction()
        Lc6:
            r7.mSetIsSatisfied = r1
        Lc8:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.MiuiMemServiceHelper.onReceive(android.content.Context, android.content.Intent):void");
    }
}
