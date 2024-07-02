package com.android.server.location.gnss.gnssSelfRecovery;

import android.content.Context;
import android.os.Handler;

/* loaded from: classes.dex */
public abstract class GnssDiagnosticsBase implements Runnable {
    private static final boolean DEBUG = false;
    private static final int DIAGNOSTICS_TIME_OUT = 1000000;
    public static final String LAST_MOCK_APP_PKG_NAME = "mockAppPackageName";
    public static final int MSG_FINISHI_DIAGNOTIC = 9;
    public static final int MSG_MOCK_LOCATION = 101;
    public static final int MSG_WEAK_SIGNAL = 110;
    public static final int MSG_WEAK_SIGNAL_RECOVER = 1;
    public static final String TAG = "GnssDiagnosticsBase";
    private boolean isRunning;
    private Context mContext;
    private Handler mHandler;

    public abstract void finishDiagnostics();

    public abstract DiagnoticResult getDiagnosticsResult();

    public abstract void startDiagnostics();

    public GnssDiagnosticsBase(Context context, Handler handler) {
        this.mHandler = handler;
        this.mContext = context;
    }

    public synchronized void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override // java.lang.Runnable
    public void run() {
        startDiagnostics();
        this.isRunning = true;
    }

    public synchronized void finish() {
        if (this.isRunning) {
            finishDiagnostics();
            this.isRunning = false;
        }
    }
}
