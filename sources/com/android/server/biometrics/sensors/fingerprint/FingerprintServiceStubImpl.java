package com.android.server.biometrics.sensors.fingerprint;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.fingerprint.Fingerprint;
import android.os.Binder;
import android.os.IHwBinder;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.util.ArrayList;

@MiuiStubHead(manifestName = "com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStub$$")
/* loaded from: classes.dex */
public class FingerprintServiceStubImpl extends FingerprintServiceStub {
    private static final String TAG = "FingerprintServiceStubImpl";
    private IHwBinder mExtDaemon;
    Fingerprint mFingerIdentifer;
    ArrayList<Byte> mFingerToken;
    public long mOpId = -1;
    public String mOpPackage = "";
    protected int mLockoutMode = 0;
    private int lockFlag = 0;
    private final String NAME_EXT_DAEMON = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private final String EXT_DESCRIPTOR = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private final int CODE_EXT_CMD = 1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FingerprintServiceStubImpl> {

        /* compiled from: FingerprintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FingerprintServiceStubImpl INSTANCE = new FingerprintServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public FingerprintServiceStubImpl m877provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public FingerprintServiceStubImpl m876provideNewInstance() {
            return new FingerprintServiceStubImpl();
        }
    }

    public boolean interruptsPrecedingClients(BaseClientMonitor client) {
        if (Utils.isKeyguard(client.getContext(), client.getOwnerString())) {
            return true;
        }
        return false;
    }

    public boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        return powerManager.isInteractive();
    }

    public void saveAuthenticateConfig(long opId, String opPackageName) {
        this.mOpId = opId;
        this.mOpPackage = opPackageName;
    }

    public long getOpId() {
        long opId = this.mOpId;
        return opId;
    }

    public String getOpPackageName() {
        String opPackage = this.mOpPackage;
        return opPackage;
    }

    public void saveAuthenResultLocal(Fingerprint identifier, ArrayList<Byte> token) {
        this.mFingerIdentifer = identifier;
        this.mFingerToken = token;
    }

    public Fingerprint getIdentifier() {
        return this.mFingerIdentifer;
    }

    public ArrayList<Byte> getToken() {
        return this.mFingerToken;
    }

    public void clearSavedAuthenResult() {
        if (PowerFingerprintServiceStub.getInstance().getIsPowerfp()) {
            this.mFingerIdentifer = null;
            this.mFingerToken = null;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public boolean isFingerDataSharer(int userId) {
        char c;
        UserInfo uinfo = null;
        Application app = ActivityThread.currentApplication();
        UserManager manager = (UserManager) app.getSystemService(UserManager.class);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                uinfo = manager.getUserInfo(userId);
            } catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }
            if (uinfo != null) {
                if (uinfo.name == null) {
                    Log.d(TAG, "calling from anonymous user, id = " + userId);
                } else {
                    String str = uinfo.name;
                    switch (str.hashCode()) {
                        case -1695516786:
                            if (str.equals("XSpace")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case -946401722:
                            if (str.equals("child_model")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 2135952422:
                            if (str.equals("security space")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                        case 1:
                        case 2:
                            Log.d(TAG, "calling from " + uinfo.name + ", id = " + userId);
                            return true;
                    }
                }
                if (uinfo.isManagedProfile()) {
                    Log.d(TAG, "calling from managed-profile, id =  " + userId + ", owned by " + uinfo.profileGroupId);
                    return true;
                }
                Log.d(TAG, uinfo.toFullString());
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public int getcurrentUserId(int groupId) {
        return getMiuiGroupId(groupId);
    }

    public int getMiuiGroupId(int userId) {
        if (userId == 0 || !isFingerDataSharer(userId)) {
            return userId;
        }
        return 0;
    }

    public void setCurrentLockoutMode(int lockoutMode) {
        this.mLockoutMode = lockoutMode;
        FodFingerprintServiceStub.getInstance().setCurrentLockoutMode(lockoutMode);
    }

    public int getCurrentLockoutMode() {
        return this.mLockoutMode;
    }

    public boolean isFingerprintClient(BaseClientMonitor client) {
        return client != null && client.statsModality() == 1;
    }

    public boolean isFingerDownAcquireCode(int acquiredInfo, int vendorCode) {
        if (acquiredInfo == 6 && vendorCode == 22) {
            return true;
        }
        return false;
    }

    public boolean isFingerUpAcquireCode(int acquiredInfo, int vendorCode) {
        if (acquiredInfo == 6 && vendorCode == 23) {
            return true;
        }
        return false;
    }

    public boolean isSecurityCenterClient(String clientName) {
        return clientName.equals(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
    }

    public boolean shouldHandleFailedAttempt(int acquiredInfo, int vendorCode) {
        if (acquiredInfo == 6 && vendorCode != 19 && vendorCode != 20 && vendorCode != 21 && vendorCode != 22 && vendorCode != 23 && vendorCode != 28 && vendorCode != 50 && vendorCode != 52) {
            return true;
        }
        return false;
    }

    public void resetLockFlag() {
        this.lockFlag = 0;
    }

    public void handleAcquiredInfo(int acquiredInfo, int vendorCode, AuthenticationClient client) {
        int errorCode;
        if (this.lockFlag == 0 && shouldHandleFailedAttempt(acquiredInfo, vendorCode)) {
            this.lockFlag = 1;
        }
        if (this.lockFlag == 1 && acquiredInfo == 5) {
            this.lockFlag = 0;
            int lockoutMode = client.handleFailedAttempt(client.getTargetUserId());
            if (isSecurityCenterClient(client.getOwnerString())) {
                client.onError(19, 0);
            }
            if (lockoutMode != 0) {
                if (lockoutMode == 1) {
                    errorCode = 7;
                } else {
                    errorCode = 9;
                }
                client.onError(errorCode, 0);
            }
        }
    }

    public boolean isFpHardwareDetected() {
        String fpVendor = SystemProperties.get("persist.vendor.sys.fp.vendor", "");
        if (!fpVendor.trim().equals("none") && !fpVendor.trim().equals("")) {
            return true;
        }
        Log.e(TAG, "Fingerprint isHardwareDetected is failed.");
        return false;
    }
}
