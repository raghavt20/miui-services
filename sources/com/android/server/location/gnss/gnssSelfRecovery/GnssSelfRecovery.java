package com.android.server.location.gnss.gnssSelfRecovery;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.util.Log;
import com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecovery;
import com.android.server.location.gnss.hal.GnssNative;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.security.AccessControlImpl;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public class GnssSelfRecovery implements GnssSelfRecoveryStub {
    private static final String GNSS_SELF_RECOVERY_PROP = "persist.sys.gps.selfRecovery";
    private static final int GPS_GSR_MAIN_BIT = 1;
    private static final int GPS_GSR_MOCK_BIT = 2;
    private static final int GPS_GSR_WEAK_SIGN_BIT = 4;
    public static final int REASON_MOCK_LOCATION = 0;
    public static final int REASON_WEAK_SIGNAL = 1;
    private static final String TAG = "GnssSelfRecovery";
    private static String[] appList = {"com.baidu.BaiduMap", "com.autonavi.minimap", "com.tencent.map"};
    private IGnssRecovery gnssRecovery;
    private GnssWeakSignalDiagnostic gnssWeakSign;
    private Context mContext;
    private GnssNative mGnssNative;
    private Handler mHandler;
    private HandlerThread mHandlerThread = new HandlerThread("gnssSelfRecoveryThread");
    private final ArrayList<GnssDiagnosticsBase> diagnosticsItemArrayList = new ArrayList<>();
    private final File mFile = new File(new File(Environment.getDataDirectory(), "system"), "gnss_properties.xml");
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private boolean isMockFlag = false;
    private boolean isEnableFullTracking = false;
    private boolean mockStatus = false;
    private boolean weakSignStatus = false;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface GnssRecoveryReason {
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssSelfRecovery> {

        /* compiled from: GnssSelfRecovery$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssSelfRecovery INSTANCE = new GnssSelfRecovery();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssSelfRecovery m1823provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssSelfRecovery m1822provideNewInstance() {
            return new GnssSelfRecovery();
        }
    }

    public void setGsrConfig() {
        int config = SystemProperties.getInt(GNSS_SELF_RECOVERY_PROP, 0);
        if ((config & 1) != 0 && (config & 2) != 0) {
            this.mockStatus = true;
        } else {
            this.mockStatus = false;
        }
        if ((config & 1) != 0 && (config & 4) != 0) {
            this.weakSignStatus = true;
        } else {
            this.weakSignStatus = false;
        }
    }

    public void init(Context context) {
        this.mContext = context;
        if (this.isRunning.get()) {
            return;
        }
        Log.i(TAG, "init");
        try {
            this.mHandlerThread.start();
            this.mHandler = new GnssDiagnoseHandler(this.mHandlerThread.getLooper());
            setGsrConfig();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        this.isRunning.set(true);
    }

    public synchronized void deInit() {
        Log.i(TAG, "deInit");
        Handler handler = this.mHandler;
        if (handler != null) {
            try {
                handler.removeCallbacksAndMessages(null);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        this.diagnosticsItemArrayList.clear();
        this.isRunning.set(false);
    }

    public void diagnosticMockLocation() {
        if (this.mHandler == null || !this.mockStatus) {
            return;
        }
        Log.d(TAG, "start diagnostic mock location");
        if (this.isMockFlag) {
            GnssDiagnosticsBase mMockLocationDiagnostic = new MockLocationDiagnostic(this.mContext.getApplicationContext(), this.mHandler);
            mMockLocationDiagnostic.start();
        }
    }

    public boolean startDiagnostic(WorkSource mWorkSource, GnssNative gnssNative) {
        if (!isUsingNaviApp(mWorkSource) || this.mHandler == null || !this.weakSignStatus) {
            return false;
        }
        Log.i(TAG, "start");
        this.mGnssNative = gnssNative;
        if (this.gnssRecovery == null) {
            this.gnssRecovery = new GnssRecoveryImpl(this.mContext, gnssNative);
        }
        if (this.gnssWeakSign == null) {
            this.gnssWeakSign = new GnssWeakSignalDiagnostic(this.mContext.getApplicationContext(), this.mHandler);
        }
        this.gnssWeakSign.startDiagnostics();
        return true;
    }

    public void storeMockAppPkgName(Context mContext, String pkgName) {
        if (mContext == null || !this.mockStatus) {
            return;
        }
        Log.d(TAG, "storeMockAppPkgName");
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(this.mFile, 0);
        sharedPreferences.getString(GnssDiagnosticsBase.LAST_MOCK_APP_PKG_NAME, "");
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(GnssDiagnosticsBase.LAST_MOCK_APP_PKG_NAME, pkgName);
            editor.apply();
            this.isMockFlag = true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private boolean isUsingNaviApp(WorkSource mWorkSource) {
        Log.d(TAG, "workSource:" + mWorkSource);
        if (mWorkSource == null) {
            return false;
        }
        for (int i = 0; i < appList.length; i++) {
            for (int j = 0; j < mWorkSource.size(); j++) {
                if (appList[i].equals(mWorkSource.getPackageName(j))) {
                    Log.d(TAG, "is navic app :" + mWorkSource.getPackageName(j));
                    return true;
                }
            }
        }
        return false;
    }

    public void finishDiagnostic() {
        Log.i(TAG, "finishDiagnostic");
        GnssWeakSignalDiagnostic gnssWeakSignalDiagnostic = this.gnssWeakSign;
        if (gnssWeakSignalDiagnostic == null) {
            return;
        }
        try {
            gnssWeakSignalDiagnostic.finishDiagnostics();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void weakSignRecover() {
        Log.i(TAG, "recove");
        GnssWeakSignalDiagnostic gnssWeakSignalDiagnostic = this.gnssWeakSign;
        if (gnssWeakSignalDiagnostic == null) {
            return;
        }
        try {
            gnssWeakSignalDiagnostic.weakSignRecovers();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static String getRecoveryReasonAsString(int reason) {
        switch (reason) {
            case 0:
                return "mock location";
            case 1:
                return "weak GNSS signal";
            default:
                return "UNKNOWN";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class GnssDiagnoseHandler extends Handler {
        public GnssDiagnoseHandler(Looper looper) {
            super(looper);
        }

        public GnssDiagnoseHandler(Looper looper, Handler.Callback callback) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            Log.i(GnssSelfRecovery.TAG, "handle receive result");
            switch (msg.what) {
                case 9:
                    Log.v(GnssSelfRecovery.TAG, "receive diagnostic msg, type:FINISHI_DIAGNOTIC");
                    GnssSelfRecovery.this.gnssRecovery.setFullTracking(false);
                    return;
                case 101:
                    Log.v(GnssSelfRecovery.TAG, "receive diagnostic msg, type: MOCK_LOCATION");
                    IGnssRecovery gnssMockRecovery = new GnssRecoveryImpl(GnssSelfRecovery.this.mContext);
                    DiagnoticResult diagResult = (DiagnoticResult) msg.obj;
                    if (diagResult.getResult()) {
                        gnssMockRecovery.removeMockLocation();
                        GnssSelfRecovery.this.isMockFlag = false;
                        return;
                    }
                    return;
                case 110:
                    Log.v(GnssSelfRecovery.TAG, "receive diagnostic msg, type: WEAK_SIGNAL");
                    GnssSelfRecovery.this.gnssRecovery.setFullTracking(true);
                    GnssSelfRecovery.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecovery$GnssDiagnoseHandler$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            GnssSelfRecovery.GnssDiagnoseHandler.this.lambda$handleMessage$0();
                        }
                    }, AccessControlImpl.LOCK_TIME_OUT);
                    return;
                default:
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$handleMessage$0() {
            GnssSelfRecovery.this.finishDiagnostic();
        }
    }
}
