package com.android.server.location.gnss.gnssSelfRecovery;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.File;
import java.util.List;

/* loaded from: classes.dex */
public class MockLocationDiagnostic extends GnssDiagnosticsBase {
    public static final String DIAG_ITEM_NAME_MOCK = "mockDiagsResult";
    private static final String TAG = "MockLocationDiagnostic";
    private boolean isMockLocation;
    private boolean isRuning;
    private Context mContext;
    private Handler mHandler;
    private DiagnoticResult mockDiagResult;
    private SharedPreferences sharedPreferences;

    public MockLocationDiagnostic(Context context, Handler handler) {
        super(context, handler);
        this.mContext = context;
        this.mHandler = handler;
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase, java.lang.Runnable
    public void run() {
        diagnoseMockLocationInternel();
        Handler handler = this.mHandler;
        if (handler != null) {
            Message message = handler.obtainMessage();
            message.what = 101;
            message.obj = getDiagnosticsResult();
            this.mHandler.sendMessage(message);
        }
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase
    public void startDiagnostics() {
        if (!this.isRuning) {
            this.isRuning = true;
            Thread thread = new Thread("mockDiagsThread");
            thread.start();
        }
    }

    public void diagnoseMockLocationInternel() {
        Log.i(TAG, "start Diagnose mock location");
        this.mockDiagResult = new DiagnoticResult(DIAG_ITEM_NAME_MOCK, false, 0.0d);
        String lastMockPkg = getMockLocationPkgName(this.mContext);
        if (!checkIfMockAppAlive(lastMockPkg)) {
            this.mockDiagResult.setResult(true);
        } else {
            this.mockDiagResult.setResult(false);
        }
        Log.i(TAG, "start Diagnose mock location result: " + this.mockDiagResult.toString());
    }

    private String getMockLocationPkgName(Context mContext) {
        if (mContext == null) {
            return "";
        }
        try {
            File mFile = new File(new File(Environment.getDataDirectory(), "system"), "gnss_properties.xml");
            SharedPreferences sharedPreferences = mContext.getSharedPreferences(mFile, 0);
            this.sharedPreferences = sharedPreferences;
            String pkgName = sharedPreferences.getString(GnssDiagnosticsBase.LAST_MOCK_APP_PKG_NAME, "");
            return pkgName;
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
            return "";
        }
    }

    private boolean checkIfMockAppAlive(String pkgName) {
        ActivityManager mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        List<ActivityManager.RunningAppProcessInfo> processInfos = mActivityManager.getRunningAppProcesses();
        int i = 0;
        while (true) {
            if (i >= processInfos.size()) {
                return false;
            }
            ActivityManager.RunningAppProcessInfo processInfo = processInfos.get(i);
            String[] pkgList = processInfo.pkgList;
            for (String packageName : pkgList) {
                if (pkgName.equals(packageName)) {
                    Log.d(TAG, "Service name");
                    return true;
                }
            }
            i++;
        }
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase
    public void finishDiagnostics() {
    }

    @Override // com.android.server.location.gnss.gnssSelfRecovery.GnssDiagnosticsBase
    public DiagnoticResult getDiagnosticsResult() {
        return this.mockDiagResult;
    }
}
