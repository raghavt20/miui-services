package com.android.server.location.gnss.gnssSelfRecovery;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/* loaded from: classes.dex */
public class GnssWeakSignalDiagnostic extends GnssDiagnosticsBase {
    public static final String DIAG_ITEM_NAME_WEAK_SIGNAL = "weakSignalDiagsResult";
    private boolean isRunning;
    private Context mContext;
    private Handler mHandler;

    public GnssWeakSignalDiagnostic(Context context, Handler handler) {
        super(context, handler);
        this.mContext = context;
        this.mHandler = handler;
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase, java.lang.Runnable
    public void run() {
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.d(GnssDiagnosticsBase.TAG, "mHandler is null");
            return;
        }
        Message message = handler.obtainMessage();
        message.what = 110;
        this.mHandler.sendMessage(message);
    }

    public void weakSignRecovers() {
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.d(GnssDiagnosticsBase.TAG, "mHandler is null");
            return;
        }
        Message message = handler.obtainMessage();
        message.what = 1;
        this.mHandler.sendMessage(message);
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase
    public void startDiagnostics() {
        Thread mThread = new Thread(this);
        mThread.start();
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase
    public void finishDiagnostics() {
        Handler handler = this.mHandler;
        if (handler == null) {
            Log.d(GnssDiagnosticsBase.TAG, "mHandler is null");
            return;
        }
        Message message = handler.obtainMessage();
        message.what = 9;
        this.mHandler.sendMessage(message);
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase
    public DiagnoticResult getDiagnosticsResult() {
        return new DiagnoticResult();
    }
}
