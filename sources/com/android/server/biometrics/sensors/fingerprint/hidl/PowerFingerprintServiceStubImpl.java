package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.biometrics.BiometricStateListener;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStub;
import com.android.server.biometrics.sensors.fingerprint.PowerFingerprintServiceStub;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21;
import com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl;
import com.miui.base.MiuiStubRegistry;
import java.util.List;
import miui.os.Build;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class PowerFingerprintServiceStubImpl implements PowerFingerprintServiceStub {
    private static final long INTERCEPT_POWERKEY_THRESHOLD = 200;
    private static final String TAG = "PowerFingerprintServiceStubImpl";
    private final String DOUBLE_TAP_SIDE_FP;
    private final int FINGERPRINT_CMD_LOCKOUT_MODE;
    private final boolean IS_FOLD;
    private boolean IS_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private boolean IS_SUPPORT_FINGERPRINT_TAP = FeatureParser.getBoolean("is_support_fingerprint_tap", false);
    protected final int POWERFP_DISABLE_NAVIGATION;
    protected final int POWERFP_ENABLE_LOCK_KEY;
    protected final int POWERFP_ENABLE_NAVIGATION;
    private final String PRODUCT_NAME;
    private String RO_BOOT_HWC;
    private boolean dealOnChange;
    private PowerFingerprintServiceStub.ChangeListener listener;
    private int mBiometricState;
    private String mDoubleTapSideFp;
    private long mFingerprintAuthFailTime;
    private int mFingerprintAuthState;
    private long mFingerprintAuthSuccessTime;
    private FingerprintManager mFingerprintManager;
    private boolean mIsInterceptPowerkeyAuthOrEnroll;
    private boolean mIsScreenOnWhenFingerdown;
    protected int mSideFpUnlockType;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PowerFingerprintServiceStubImpl> {

        /* compiled from: PowerFingerprintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PowerFingerprintServiceStubImpl INSTANCE = new PowerFingerprintServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public PowerFingerprintServiceStubImpl m900provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public PowerFingerprintServiceStubImpl m899provideNewInstance() {
            return new PowerFingerprintServiceStubImpl();
        }
    }

    public PowerFingerprintServiceStubImpl() {
        this.IS_FOLD = SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2;
        this.PRODUCT_NAME = SystemProperties.get("ro.product.device", "unknow");
        this.RO_BOOT_HWC = SystemProperties.get("ro.boot.hwc", "");
        this.mSideFpUnlockType = -1;
        this.dealOnChange = false;
        this.DOUBLE_TAP_SIDE_FP = "fingerprint_double_tap";
        this.mIsScreenOnWhenFingerdown = false;
        this.mFingerprintAuthState = 0;
        this.mFingerprintAuthSuccessTime = 0L;
        this.mFingerprintAuthFailTime = 0L;
        this.mBiometricState = 0;
        this.mIsInterceptPowerkeyAuthOrEnroll = false;
        this.POWERFP_DISABLE_NAVIGATION = 0;
        this.POWERFP_ENABLE_LOCK_KEY = 1;
        this.POWERFP_ENABLE_NAVIGATION = 2;
        this.FINGERPRINT_CMD_LOCKOUT_MODE = 12;
    }

    public boolean getIsPowerfp() {
        return this.IS_POWERFP;
    }

    public void setFingerprintAuthState(int state) {
        switch (state) {
            case 1:
                this.mFingerprintAuthSuccessTime = SystemClock.uptimeMillis();
                Slog.d(TAG, "Fingerprint Auth success time:" + this.mFingerprintAuthSuccessTime);
                return;
            case 2:
                this.mFingerprintAuthFailTime = SystemClock.uptimeMillis();
                Slog.d(TAG, "Fingerprint Auth failed time:" + this.mFingerprintAuthFailTime);
                return;
            default:
                return;
        }
    }

    public boolean shouldConsumeSinglePress(long eventTime, int biometricState) {
        if (!this.IS_POWERFP) {
            return false;
        }
        switch (biometricState) {
            case 0:
                long intervalFpPower = eventTime - this.mFingerprintAuthSuccessTime;
                Slog.d(TAG, "intervalFpPower gap:" + intervalFpPower);
                if (intervalFpPower >= INTERCEPT_POWERKEY_THRESHOLD && !this.mIsInterceptPowerkeyAuthOrEnroll) {
                    return false;
                }
                Slog.d(TAG, "fingerprint intercept power key press");
                return true;
            case 1:
                Slog.d(TAG, "intercept power press due to STATE_ENROLLING");
                return true;
            case 2:
                if (isPad()) {
                    long intervalFpPower2 = eventTime - this.mFingerprintAuthFailTime;
                    Slog.d(TAG, "intervalFpPower gap:" + intervalFpPower2);
                    if (!this.mIsScreenOnWhenFingerdown && intervalFpPower2 < INTERCEPT_POWERKEY_THRESHOLD) {
                        Slog.d(TAG, "fingerprint intercept power key press");
                        return true;
                    }
                }
                return false;
            case 3:
                Slog.d(TAG, "intercept power press due to STATE_BP_AUTH");
                return true;
            case 4:
                Slog.d(TAG, "intercept power press due to STATE_AUTH_OTHER");
                return true;
            default:
                return false;
        }
    }

    private void setChangeListener(PowerFingerprintServiceStub.ChangeListener listener) {
        Slog.d(TAG, "setChangeListener");
        this.listener = listener;
    }

    public void setDealOnChange(boolean value) {
        Slog.d(TAG, "setDealOnChange:" + (value ? "true" : "false"));
        this.dealOnChange = value;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getDealOnChange() {
        return this.dealOnChange;
    }

    private boolean isPad() {
        return Build.IS_TABLET;
    }

    public void notifyPowerPressed() {
        Slog.d(TAG, "notifyPowerPressed");
        switch (this.mBiometricState) {
            case 1:
            case 3:
            case 4:
                this.mIsInterceptPowerkeyAuthOrEnroll = true;
                break;
            case 2:
            default:
                this.mIsInterceptPowerkeyAuthOrEnroll = false;
                break;
        }
        PowerFingerprintServiceStub.ChangeListener changeListener = this.listener;
        if (changeListener != null) {
            changeListener.onChange(this.dealOnChange);
        }
    }

    public boolean getIsSupportFpTap() {
        return this.IS_SUPPORT_FINGERPRINT_TAP;
    }

    public void registerDoubleTapSideFpOptionObserver(final Context context, Handler handler, final BaseClientMonitor client) {
        String doubleTapSideFpOption = getDoubleTapSideFpOption(context);
        this.mDoubleTapSideFp = doubleTapSideFpOption;
        if (TextUtils.isEmpty(doubleTapSideFpOption) || "none".equalsIgnoreCase(this.mDoubleTapSideFp)) {
            notifyLockOutState(context, client, 0, 0);
        } else {
            notifyLockOutState(context, client, 0, 2);
        }
        ContentObserver observer = new ContentObserver(handler) { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                PowerFingerprintServiceStubImpl powerFingerprintServiceStubImpl = PowerFingerprintServiceStubImpl.this;
                powerFingerprintServiceStubImpl.mDoubleTapSideFp = powerFingerprintServiceStubImpl.getDoubleTapSideFpOption(context);
                if (!"none".equals(PowerFingerprintServiceStubImpl.this.mDoubleTapSideFp)) {
                    PowerFingerprintServiceStubImpl.this.notifyLockOutState(context, client, 0, 2);
                } else {
                    PowerFingerprintServiceStubImpl.this.notifyLockOutState(context, client, 0, 0);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("fingerprint_double_tap"), false, observer, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDoubleTapSideFpOption(Context context) {
        if (!getIsPowerfp() || !this.IS_SUPPORT_FINGERPRINT_TAP) {
            return null;
        }
        String dobuleTap = Settings.System.getStringForUser(context.getContentResolver(), "fingerprint_double_tap", -2);
        return dobuleTap;
    }

    public void notifyLockOutState(Context context, BaseClientMonitor client, int lockoutMode, int param) {
        if (!getIsPowerfp()) {
            return;
        }
        if (client != null && FingerprintServiceStub.getInstance().isFingerprintClient(client) && !client.isAlreadyDone() && lockoutMode == 0 && client.getStatsAction() != 0) {
            Slog.w(TAG, "mCurrentClient.statsAction() " + client.statsModality() + "mCurrentClient.isAlreadyDone() " + client.isAlreadyDone() + "mCurrentClient.statsAction() " + client.getStatsAction() + "lockoutMode " + lockoutMode);
            return;
        }
        if (param == 2 && !this.IS_SUPPORT_FINGERPRINT_TAP) {
            FingerprintServiceStub.getInstance();
            FingerprintServiceStub.startExtCmd(12, 0);
        } else if (param == 2 && (TextUtils.isEmpty(getDoubleTapSideFpOption(context)) || "none".equalsIgnoreCase(getDoubleTapSideFpOption(context)))) {
            FingerprintServiceStub.getInstance();
            FingerprintServiceStub.startExtCmd(12, 0);
        } else {
            FingerprintServiceStub.getInstance();
            FingerprintServiceStub.startExtCmd(12, param);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int isDefaultPressUnlock() {
        return ("INDIA".equalsIgnoreCase(this.RO_BOOT_HWC) || "IN".equalsIgnoreCase(this.RO_BOOT_HWC) || (this.IS_FOLD && !"cetus".equals(this.PRODUCT_NAME))) ? 1 : 0;
    }

    public int getFingerprintUnlockType(Context context) {
        if (!getIsPowerfp()) {
            return -1;
        }
        int i = this.mSideFpUnlockType;
        if (i != -1) {
            return i;
        }
        int i2 = 0;
        if (Settings.Secure.getIntForUser(context.getContentResolver(), MiuiSettings.Secure.FINGERPRINT_UNLOCK_TYPE, isDefaultPressUnlock(), 0) == 1) {
            i2 = 1;
        }
        int unlockType = i2;
        return unlockType;
    }

    private void registerSideFpUnlockTypeChangedObserver(Handler handler, final Context context) {
        ContentObserver observer = new ContentObserver(handler) { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                PowerFingerprintServiceStubImpl.this.mSideFpUnlockType = Settings.Secure.getIntForUser(context.getContentResolver(), MiuiSettings.Secure.FINGERPRINT_UNLOCK_TYPE, PowerFingerprintServiceStubImpl.this.isDefaultPressUnlock(), 0);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.FINGERPRINT_UNLOCK_TYPE), false, observer);
    }

    public boolean shouldDropFailAuthenResult(Context context, BaseClientMonitor client, boolean authenticated) {
        if (!getIsPowerfp() || !isPad()) {
            return false;
        }
        if (Utils.isKeyguard(context, client.getOwnerString()) && this.mIsScreenOnWhenFingerdown && !authenticated && !getDealOnChange() && !FingerprintServiceStub.getInstance().isScreenOn(context)) {
            Slog.d(TAG, "shouldDropFailAuthenResult ture");
            return true;
        }
        Slog.d(TAG, "shouldDropFailAuthenResult false mIsScreenOnWhenFingerdown:" + this.mIsScreenOnWhenFingerdown + " authenticated:" + authenticated + " getDealOnChange:" + getDealOnChange() + " isScreenOn:" + FingerprintServiceStub.getInstance().isScreenOn(context));
        return false;
    }

    public boolean shouldSavaAuthenResult(Context context, BaseClientMonitor client) {
        return getIsPowerfp() && getFingerprintUnlockType(context) == 1 && getDealOnChange() && !FingerprintServiceStub.getInstance().isScreenOn(context) && (client instanceof AuthenticationClient);
    }

    private boolean shouldRestartClient(Context context, BaseClientMonitor client) {
        return getFingerprintUnlockType(context) == 1 && !this.mIsScreenOnWhenFingerdown && (client instanceof AuthenticationClient);
    }

    public void handleAcquiredInfo(int acquiredInfo, int vendorCode, Context context, BiometricScheduler scheduler, int sensorId, Fingerprint21.HalResultController halResultController) {
        if (!getIsPowerfp()) {
            return;
        }
        FingerprintAuthenticationClient currentClient = scheduler.getCurrentClient();
        if ((currentClient instanceof AuthenticationClient) && FingerprintServiceStub.getInstance().isFingerDownAcquireCode(acquiredInfo, vendorCode)) {
            this.mIsScreenOnWhenFingerdown = FingerprintServiceStub.getInstance().isScreenOn(context);
            setDealOnChange(true);
            return;
        }
        if (FingerprintServiceStub.getInstance().isFingerUpAcquireCode(acquiredInfo, vendorCode)) {
            if (currentClient != null && Utils.isKeyguard(context, currentClient.getOwnerString())) {
                if (shouldRestartClient(context, currentClient)) {
                    try {
                        long opId = FingerprintServiceStub.getInstance().getOpId();
                        FingerprintAuthenticationClient authClient = currentClient;
                        int result = ((IBiometricsFingerprint) authClient.getFreshDaemon()).authenticate(opId, authClient.getTargetUserId());
                        if (result != 0) {
                            Slog.w(TAG, "startAuthentication failed, result=" + result);
                            try {
                                halResultController.onError(sensorId, 1, 0);
                            } catch (RemoteException e) {
                                e = e;
                                Slog.e(TAG, "startAuthentication failed", e);
                                FingerprintServiceStub.getInstance().clearSavedAuthenResult();
                            }
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                    }
                }
            }
            FingerprintServiceStub.getInstance().clearSavedAuthenResult();
        }
    }

    public void init(Context context, Handler handler, BiometricScheduler scheduler) {
        if (!getIsPowerfp()) {
            return;
        }
        registerSideFpUnlockTypeChangedObserver(handler, context);
        registerDoubleTapSideFpOptionObserver(context, handler, scheduler.getCurrentClient());
        notifyLockOutState(context, scheduler.getCurrentClient(), 0, 2);
        setChangeListener(new AnonymousClass3(context, handler, scheduler));
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(FingerprintManager.class);
        this.mFingerprintManager = fingerprintManager;
        fingerprintManager.addAuthenticatorsRegisteredCallback(new IFingerprintAuthenticatorsRegisteredCallback.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl.4
            public void onAllAuthenticatorsRegistered(List<FingerprintSensorPropertiesInternal> sensors) {
                PowerFingerprintServiceStubImpl.this.mFingerprintManager.registerBiometricStateListener(new BiometricStateListener() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl.4.1
                    public void onStateChanged(int newState) {
                        Slog.d(PowerFingerprintServiceStubImpl.TAG, "onStateChanged : " + newState);
                        PowerFingerprintServiceStubImpl.this.mBiometricState = newState;
                    }
                });
            }
        });
    }

    /* renamed from: com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl$3, reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass3 implements PowerFingerprintServiceStub.ChangeListener {
        final /* synthetic */ Context val$context;
        final /* synthetic */ Handler val$handler;
        final /* synthetic */ BiometricScheduler val$scheduler;

        AnonymousClass3(Context context, Handler handler, BiometricScheduler biometricScheduler) {
            this.val$context = context;
            this.val$handler = handler;
            this.val$scheduler = biometricScheduler;
        }

        public void onChange(boolean value) {
            if (PowerFingerprintServiceStubImpl.this.getDealOnChange() && PowerFingerprintServiceStubImpl.this.getFingerprintUnlockType(this.val$context) == 1) {
                Slog.i(PowerFingerprintServiceStubImpl.TAG, "onChange enter");
                Handler handler = this.val$handler;
                final BiometricScheduler biometricScheduler = this.val$scheduler;
                handler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl$3$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        PowerFingerprintServiceStubImpl.AnonymousClass3.lambda$onChange$0(biometricScheduler);
                    }
                });
            }
            PowerFingerprintServiceStubImpl.this.setDealOnChange(false);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onChange$0(BiometricScheduler scheduler) {
            if (FingerprintServiceStub.getInstance().getIdentifier() != null) {
                Fingerprint fp = FingerprintServiceStub.getInstance().getIdentifier();
                boolean authenticated = fp.getBiometricId() != 0;
                if (!authenticated && FingerprintServiceStub.getInstance().getSupportInterfaceVersion() == 2) {
                    FingerprintServiceStub.getInstance();
                    FingerprintServiceStub.startExtCmd(17, 0);
                }
                AuthenticationConsumer currentClient = scheduler.getCurrentClient();
                if (!(currentClient instanceof AuthenticationConsumer)) {
                    Slog.e(PowerFingerprintServiceStubImpl.TAG, "onAuthenticated for non-authentication consumer: " + Utils.getClientName(currentClient));
                    return;
                }
                AuthenticationConsumer authenticationConsumer = currentClient;
                authenticationConsumer.onAuthenticated(fp, authenticated, FingerprintServiceStub.getInstance().getToken());
                FingerprintServiceStub.getInstance().clearSavedAuthenResult();
            }
        }
    }
}
