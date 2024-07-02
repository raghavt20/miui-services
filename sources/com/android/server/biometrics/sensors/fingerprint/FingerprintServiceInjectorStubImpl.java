package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.hardware.biometrics.BiometricStateListener;
import android.hardware.display.DisplayManager;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.MiuiBgThread;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintAuthTimeData;
import com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintFailReasonData;
import com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintHalAuthData;
import com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintLocalStatisticsData;
import com.android.server.biometrics.sensors.fingerprint.bigdata.FingerprintUnlockRateData;
import com.miui.base.MiuiStubRegistry;
import com.miui.misight.MiEvent;
import com.miui.misight.MiSight;
import java.util.List;

/* loaded from: classes.dex */
public class FingerprintServiceInjectorStubImpl implements FingerprintServiceInjectorStub {
    private static final int FINGERPRINT_ACQUIRED_AUTH_BIGDATA = 201;
    private static final int FINGERPRINT_ACQUIRED_ENROLL_BIGDATA = 202;
    private static final int FINGERPRINT_ACQUIRED_FINGER_DOWN = 22;
    private static final int FINGERPRINT_ACQUIRED_INFORE6 = 6;
    private static final int FINGERPRINT_ACQUIRED_INFORE7 = 7;
    private static final int FINGERPRINT_ACQUIRED_INIT_BIGDATA = 200;
    private static final boolean FP_LOCAL_STATISTICS_ENABLED = SystemProperties.getBoolean("persist.vendor.sys.fp.onetrack.enable", true);
    private static final int MISIGHT_FINGERPRINT_TEMPLATELOST_EVENT = 1;
    private static final int MISIGHT_FINGERPRINT_TEMPLATELOST_ID = 914001003;
    private static final int SCREEN_STATUS_DOZE = 2;
    private static final int SCREEN_STATUS_OFF = 0;
    private static final int SCREEN_STATUS_ON = 1;
    private static final String TAG = "FingerprintServiceInjectorStubImpl";
    private FingerprintLocalStatisticsData localStatisticsInstance;
    private Context mContext;
    private DisplayManager mDisplayManager;
    private FingerprintManager mFingerprintManager;
    private int mScreenStatus = -1;
    private int mBiometricState = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FingerprintServiceInjectorStubImpl> {

        /* compiled from: FingerprintServiceInjectorStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FingerprintServiceInjectorStubImpl INSTANCE = new FingerprintServiceInjectorStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public FingerprintServiceInjectorStubImpl m875provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public FingerprintServiceInjectorStubImpl m874provideNewInstance() {
            return new FingerprintServiceInjectorStubImpl();
        }
    }

    private void updataScreenStatus() {
        int state = this.mDisplayManager.getDisplay(0).getState();
        if (state == 2) {
            this.mScreenStatus = 1;
            Slog.d(TAG, "screen on when finger down");
        } else if (state == 1) {
            this.mScreenStatus = 0;
            Slog.d(TAG, "screen off when finger down");
        } else if (state == 3 || state == 4) {
            this.mScreenStatus = 2;
            Slog.d(TAG, "screen doze when finger down");
        }
        Slog.w(TAG, "updataScreenStatus, state: " + state + ", mScreenStatus: " + this.mScreenStatus);
    }

    public void recordAuthResult(final String packName, final int authen) {
        Slog.w(TAG, "recordAuthResult, packName: " + packName + ", authen" + authen);
        if (!FP_LOCAL_STATISTICS_ENABLED) {
            return;
        }
        FingerprintLocalStatisticsData.getInstance().initLocalStatistics(this.mContext);
        FingerprintLocalStatisticsData.getInstance().startLocalStatisticsOneTrackUpload();
        int result = FingerprintAuthTimeData.getInstance().calculateAuthTime(authen, this.mScreenStatus);
        if (result == 0) {
            FingerprintLocalStatisticsData.getInstance().updataLocalStatistics("auth_time_info");
        }
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintServiceInjectorStubImpl.this.lambda$recordAuthResult$0(packName, authen);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$recordAuthResult$0(String packName, int authen) {
        FingerprintUnlockRateData.getInstance().calculateUnlockCnt(packName, authen, this.mScreenStatus);
        FingerprintLocalStatisticsData.getInstance().updataLocalStatistics("unlock_rate_info");
    }

    public void recordAcquiredInfo(final int acquiredInfo, final int vendorCode) {
        Slog.w(TAG, "recordAcquiredInfo, acquiredInfo:" + acquiredInfo + ", vendorCode:" + vendorCode);
        if (!FP_LOCAL_STATISTICS_ENABLED) {
            return;
        }
        FingerprintLocalStatisticsData.getInstance().initLocalStatistics(this.mContext);
        if (vendorCode == 22) {
            updataScreenStatus();
        }
        FingerprintAuthTimeData.getInstance().handleAcquiredInfo(acquiredInfo, vendorCode);
        FingerprintUnlockRateData.getInstance().handleAcquiredInfo(acquiredInfo, vendorCode);
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintServiceInjectorStubImpl.this.lambda$recordAcquiredInfo$1(acquiredInfo, vendorCode);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$recordAcquiredInfo$1(int acquiredInfo, int vendorCode) {
        int i = this.mBiometricState;
        if (i == 2 || i == 3 || i == 4) {
            boolean result = FingerprintFailReasonData.getInstance().calculateFailReasonCnt(acquiredInfo, vendorCode, this.mScreenStatus);
            if (result) {
                FingerprintLocalStatisticsData.getInstance().updataLocalStatistics("fail_reason_info");
            }
        }
        if (acquiredInfo == 6 || acquiredInfo == 7) {
            switch (vendorCode) {
                case FINGERPRINT_ACQUIRED_INIT_BIGDATA /* 200 */:
                    Slog.d(TAG, "calculateHalInitInfo");
                    return;
                case FINGERPRINT_ACQUIRED_AUTH_BIGDATA /* 201 */:
                    Slog.d(TAG, "calculateHalAuthInfo");
                    boolean result2 = FingerprintHalAuthData.getInstance().calculateHalAuthInfo();
                    if (result2) {
                        FingerprintLocalStatisticsData.getInstance().updataLocalStatistics("unlock_hal_info");
                        return;
                    }
                    return;
                case FINGERPRINT_ACQUIRED_ENROLL_BIGDATA /* 202 */:
                    Slog.d(TAG, "calculateHalEnrollInfo");
                    return;
                default:
                    return;
            }
        }
    }

    public void initAuthStatistics() {
        Slog.w(TAG, "initAuthStatistics");
        if (!FP_LOCAL_STATISTICS_ENABLED) {
            return;
        }
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintServiceInjectorStubImpl.this.lambda$initAuthStatistics$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initAuthStatistics$2() {
        FingerprintLocalStatisticsData.getInstance().initLocalStatistics(this.mContext);
        FingerprintUnlockRateData.getInstance().setContext(this.mContext);
    }

    public void initRecordFeature(Context context) {
        if (!FP_LOCAL_STATISTICS_ENABLED) {
            return;
        }
        Slog.w(TAG, "initRecordFeature");
        this.mContext = context;
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        }
        FingerprintManager fingerprintManager = (FingerprintManager) this.mContext.getSystemService(FingerprintManager.class);
        this.mFingerprintManager = fingerprintManager;
        fingerprintManager.addAuthenticatorsRegisteredCallback(new IFingerprintAuthenticatorsRegisteredCallback.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl.1
            public void onAllAuthenticatorsRegistered(List<FingerprintSensorPropertiesInternal> sensors) {
                FingerprintServiceInjectorStubImpl.this.mFingerprintManager.registerBiometricStateListener(new BiometricStateListener() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl.1.1
                    public void onStateChanged(int newState) {
                        Slog.d(FingerprintServiceInjectorStubImpl.TAG, "onStateChanged : " + newState);
                        FingerprintServiceInjectorStubImpl.this.mBiometricState = newState;
                    }
                });
            }
        });
    }

    public void recordFpTypeAndEnrolledCount(final int unlockType, final int count) {
        Slog.w(TAG, "recordFpTypeAndEnrolledCount，unlockType:  " + unlockType + ", count: " + count);
        if (!FP_LOCAL_STATISTICS_ENABLED) {
            return;
        }
        FingerprintLocalStatisticsData.getInstance().initLocalStatistics(this.mContext);
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintLocalStatisticsData.getInstance().recordFpTypeAndEnrolledCount(unlockType, count);
            }
        });
    }

    public void recordActivityVisible() {
    }

    private void miSightEventReport(int event, int param) {
        switch (event) {
            case 1:
                MiEvent miEvent = new MiEvent(MISIGHT_FINGERPRINT_TEMPLATELOST_ID);
                miEvent.addInt("FingerprintTemplateLost", param);
                MiSight.sendEvent(miEvent);
                return;
            default:
                Slog.w(TAG, "unknow mi sight event");
                return;
        }
    }

    public void handleUnknowTemplateCleanup(BaseClientMonitor client, int unknownTemplateCount) {
        if (client.statsModality() == 1 && unknownTemplateCount > 0) {
            miSightEventReport(1, unknownTemplateCount);
        }
    }
}
