package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.fingerprint.FingerprintFidoOut;
import android.hardware.fingerprint.FingerprintManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.miui.server.stability.DumpSysInfoUtil;
import java.math.BigDecimal;
import miui.util.ITouchFeature;

/* loaded from: classes.dex */
public class FodFingerprintServiceStubImpl implements FodFingerprintServiceStub {
    protected static final boolean DEBUG = true;
    protected static final String TAG = "[FingerprintService]FodFingerprintServiceStubImpl";
    private IBinder mFodService;
    private final Object mFodLock = new Object();
    private final String FOD_SERVICE_NAME = "android.app.fod.ICallback";
    private final String INTERFACE_DESCRIPTOR = "android.app.fod.ICallback";
    private final int CODE_PROCESS_CMD = 1;
    protected int mRootUserId = 0;
    private final String HEART_RATE_PACKAGE = "com.mi.health";
    private final String KEYGUARD_PACKAGE = AccessController.PACKAGE_SYSTEMUI;
    private final String SETTING_PACKAGE = "com.android.settings";
    private final String DEFAULT_PACKNAME = "";
    private final int TOUCH_AUTHEN = 1;
    private final int TOUCH_CANCEL = 0;
    private final int TOUCH_MODE = 10;
    private int mTouchStatus = -1;
    private int mSfValue = -1;
    private int mSfPackageType = -1;
    private final int DEFAULT_DISPLAY = 0;
    private final int CMD_NOTIFY_TO_SURFACEFLINGER = 31111;
    private final int SF_FINGERPRINT_NONE = 0;
    private final int SF_ENROLL_START = 1;
    private final int SF_ENROLL_STOP = 2;
    private final int SF_AUTH_START = 3;
    private final int SF_AUTH_STOP = 4;
    private final int SF_HEART_RATE_START = 5;
    private final int SF_HEART_RATE_STOP = 6;
    private final int SF_KEYGUARD_DETECT_START = 7;
    private final int SF_KEYGUARD_DETECT_STOP = 8;
    private final int CMD_NOTIFY_MONITOR_STATE_TO_FOD_ENGINE = 4;
    private final int CMD_NOTIFY_LOCK_OUT_TO_FOD_ENGINE = 5;
    private final int CMD_NOTIFY_FOD_LOWBRIGHTNESS_ALLOW_STATE = 7;
    private final int SETTING_VALUE_ON = 1;
    private final int SETTING_VALUE_OFF = 0;
    private int mSettingShowTapsState = 0;
    private int mSettingPointerLocationState = 0;
    protected boolean IS_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    private final String FOD_LOCATION_PROPERTY = "ro.hardware.fp.fod.location";
    private final boolean IS_FOD_SENSOR_LOCATION_LOW = SystemProperties.get("ro.hardware.fp.fod.location", "").equals("low");
    private final String FOD_LOCATION_XY_PROPERTY = "persist.vendor.sys.fp.fod.location.X_Y";
    private final String FOD_LOCATION_XY = SystemProperties.get("persist.vendor.sys.fp.fod.location.X_Y", "");
    private final String FOD_LOCATION_WH_PROPERTY = "persist.vendor.sys.fp.fod.size.width_height";
    private final String FOD_LOCATION_WH = SystemProperties.get("persist.vendor.sys.fp.fod.size.width_height", "");
    protected boolean IS_FOD_LHBM = SystemProperties.getBoolean("ro.vendor.localhbm.enable", false);
    private boolean IS_FODENGINE_ENABLED = SystemProperties.get("ro.hardware.fp.fod.touch.ctl.version", "").equals("2.0");
    private final String MIUI_DEFAULT_RESOLUTION_PROPERTY = "persist.sys.miui_default_resolution";
    private final String MIUI_DEFAULT_RESOLUTION = SystemProperties.get("persist.sys.miui_default_resolution");
    private int SCREEN_WIDTH_PHYSICAL = -1;
    private int SCREEN_HEIGHT_PHYSICAL = -1;
    private float GXZW_X_PRCENT = -1.0f;
    private float GXZW_Y_PRCENT = -1.0f;
    private float GXZW_WIDTH_PRCENT = -1.0f;
    private float GXZW_HEIGHT_PRCENT = -1.0f;
    private int SCREEN_WIDTH_PX = -1;
    private int SCREEN_HEIGHT_PX = -1;
    private int GXZW_ICON_X = 453;
    private int GXZW_ICON_Y = 1640;
    private int GXZW_ICON_WIDTH = 173;
    private int GXZW_ICON_HEIGHT = 173;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FodFingerprintServiceStubImpl> {

        /* compiled from: FodFingerprintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FodFingerprintServiceStubImpl INSTANCE = new FodFingerprintServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public FodFingerprintServiceStubImpl m879provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public FodFingerprintServiceStubImpl m878provideNewInstance() {
            return new FodFingerprintServiceStubImpl();
        }
    }

    private int cmdSendToKeyguard(int cmd, String packName) {
        switch (cmd) {
            case 7:
                if ("com.android.settings".equals(packName)) {
                    return cmd;
                }
                return 1;
            case 8:
                if (!"com.android.settings".equals(packName)) {
                    return 2;
                }
                return cmd;
            case 9:
            case 10:
            case 11:
            default:
                return cmd;
            case 12:
                return 1;
            case 13:
                return 2;
        }
    }

    public int fodCallBack(Context context, int cmd, int param, BaseClientMonitor client) {
        if (client != null) {
            return fodCallBack(context, cmd, param, client.getOwnerString(), client);
        }
        return fodCallBack(context, cmd, param, "", client);
    }

    /* JADX WARN: Code restructure failed: missing block: B:115:0x00c7, code lost:
    
        r2 = setTouchMode(0, 10, r0);
     */
    /* JADX WARN: Removed duplicated region for block: B:104:0x0105  */
    /* JADX WARN: Removed duplicated region for block: B:113:0x0125  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public synchronized int fodCallBack(android.content.Context r19, int r20, int r21, java.lang.String r22, com.android.server.biometrics.sensors.BaseClientMonitor r23) {
        /*
            Method dump skipped, instructions count: 617
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.biometrics.sensors.fingerprint.FodFingerprintServiceStubImpl.fodCallBack(android.content.Context, int, int, java.lang.String, com.android.server.biometrics.sensors.BaseClientMonitor):int");
    }

    public boolean getIsFod() {
        return this.IS_FOD;
    }

    public boolean getIsFodLocationLow() {
        return this.IS_FOD_SENSOR_LOCATION_LOW;
    }

    public boolean getFodCoordinate(int[] udfpsProps) {
        if (udfpsProps.length < 3) {
            Slog.i(TAG, "wrong udfpsProps length!");
            return false;
        }
        if (this.FOD_LOCATION_XY.isEmpty() || !this.FOD_LOCATION_XY.contains(",")) {
            return false;
        }
        udfpsProps[0] = Integer.parseInt(this.FOD_LOCATION_XY.split(",")[0]);
        udfpsProps[1] = Integer.parseInt(this.FOD_LOCATION_XY.split(",")[1]);
        if (this.FOD_LOCATION_WH.isEmpty() || !this.FOD_LOCATION_WH.contains(",")) {
            return false;
        }
        int width = Integer.parseInt(this.FOD_LOCATION_WH.split(",")[0]);
        int height = Integer.parseInt(this.FOD_LOCATION_WH.split(",")[1]);
        if (width == height) {
            udfpsProps[2] = width / 2;
        }
        return true;
    }

    public void setCurrentLockoutMode(int lockoutMode) {
        if (this.IS_FODENGINE_ENABLED) {
            FingerprintServiceStub.getInstance();
            FingerprintServiceStub.startExtCmd(5, lockoutMode);
        }
    }

    public boolean setTouchMode(int touchId, int mode, int value) {
        try {
            ITouchFeature touchFeature = ITouchFeature.getInstance();
            if (touchFeature != null) {
                return touchFeature.setTouchMode(touchId, mode, value);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getCmd4Touch(int cmd, int param) {
        switch (cmd) {
            case 1:
            case 7:
            case 12:
                return 1;
            case 2:
            case 4:
            case 5:
            case 6:
            case 8:
            case 11:
            case 13:
                return 0;
            case 3:
                if (param == 0) {
                    return -1;
                }
                return 0;
            case 9:
                if (param != 0) {
                    return -1;
                }
                return 0;
            case 400001:
                return 1;
            case 400004:
            case 400006:
                return 0;
            default:
                return -1;
        }
    }

    private boolean isFodMonitorState(int sf_status) {
        switch (sf_status) {
            case 1:
            case 3:
            case 5:
                return true;
            case 2:
            case 4:
            default:
                return false;
        }
    }

    private int getCmd4SurfaceFlinger(int cmd, int param, BaseClientMonitor client) {
        int res = -1;
        switch (cmd) {
            case 1:
                return 3;
            case 2:
            case 6:
                return 4;
            case 3:
                if (param == 0) {
                    return 3;
                }
                return 4;
            case 4:
                if (client == null) {
                    return -1;
                }
                if (client.getStatsAction() == 1) {
                    res = 2;
                }
                if (client.getStatsAction() == 2) {
                    return 4;
                }
                return res;
            case 5:
                return 0;
            case 7:
                return 1;
            case 8:
                return 2;
            case 9:
                if (param == 0) {
                    return 2;
                }
                return 1;
            case 12:
                return 7;
            case 13:
                return 8;
            case 400001:
                return 5;
            case 400004:
            case 400006:
                return 6;
            default:
                return -1;
        }
    }

    private int notifySurfaceFlinger(Context context, int msg, String packName, int value, int cmd) {
        int resBack = -1;
        int packageType = 0;
        if (!this.IS_FOD) {
            return -1;
        }
        if (isKeyguard(packName) && cmd == 1) {
            packageType = 1;
        }
        if (this.mSfValue == value && this.mSfPackageType == packageType) {
            Slog.i(TAG, "duplicate notifySurfaceFlinger msg: " + msg + ", value: " + value + ", packageType: " + packageType);
            return -1;
        }
        IBinder flinger = ServiceManager.getService(DumpSysInfoUtil.SURFACEFLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(value);
            data.writeInt(packageType);
            try {
                try {
                    flinger.transact(msg, data, reply, 0);
                    reply.readException();
                    resBack = reply.readInt();
                } catch (RemoteException ex) {
                    Slog.e(TAG, "Failed to notifySurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
        Slog.i(TAG, "notifySurfaceFlinger msg: " + msg + ", value: " + value + ", packageType: " + packageType);
        this.mSfValue = value;
        this.mSfPackageType = packageType;
        return resBack;
    }

    public IBinder getFodServ() throws RemoteException {
        IBinder iBinder;
        synchronized (this.mFodLock) {
            if (this.mFodService == null) {
                IBinder service = ServiceManager.getService("android.app.fod.ICallback");
                this.mFodService = service;
                if (service != null) {
                    service.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.biometrics.sensors.fingerprint.FodFingerprintServiceStubImpl$$ExternalSyntheticLambda0
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            FodFingerprintServiceStubImpl.this.lambda$getFodServ$0();
                        }
                    }, 0);
                }
            }
            iBinder = this.mFodService;
        }
        return iBinder;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$getFodServ$0() {
        synchronized (this.mFodLock) {
            Slog.e(TAG, "fodCallBack Service Died.");
            this.mFodService = null;
        }
    }

    public String getCmdStr(int cmd) {
        switch (cmd) {
            case 1:
                return "CMD_APP_AUTHEN";
            case 2:
                return "CMD_APP_CANCEL_AUTHEN";
            case 3:
                return "CMD_VENDOR_AUTHENTICATED";
            case 4:
                return "CMD_VENDOR_ERROR";
            case 5:
                return "CMD_FW_LOCK_CANCEL";
            case 6:
                return "CMD_FW_TOP_APP_CANCEL";
            case 7:
                return "CMD_APP_ENROLL";
            case 8:
                return "CMD_APP_CANCEL_ENROLL";
            case 9:
                return "CMD_VENDOR_ENROLL_RES";
            case 10:
            default:
                return "unknown";
            case 11:
                return "CMD_VENDOR_REMOVED";
            case 12:
                return "CMD_KEYGUARD_DETECT";
            case 13:
                return "CMD_KEYGUARD_CANCEL_DETECT";
        }
    }

    /* loaded from: classes.dex */
    public static abstract class AuthenticationFidoCallback extends FingerprintManager.AuthenticationCallback {
        public void onAuthenticationFidoSucceeded(FingerprintManager.AuthenticationResult result, FingerprintFidoOut fidoOut) {
        }
    }

    protected boolean isKeyguard(String clientPackage) {
        return AccessController.PACKAGE_SYSTEMUI.equals(clientPackage);
    }

    public void handleAcquiredInfo(int acquiredInfo, int vendorCode, AcquisitionClient client) {
        try {
            if (FingerprintServiceStub.getInstance().isFingerprintClient(client) && getIsFod()) {
                if (FingerprintServiceStub.getInstance().isFingerDownAcquireCode(acquiredInfo, vendorCode)) {
                    client.getListener().onUdfpsPointerDown(client.getSensorId());
                } else if (FingerprintServiceStub.getInstance().isFingerUpAcquireCode(acquiredInfo, vendorCode)) {
                    client.getListener().onUdfpsPointerUp(client.getSensorId());
                }
            }
        } catch (RemoteException e) {
            client.mCallback.onClientFinished(client, false);
        }
    }

    public int[] getSensorLocation(Context context) {
        calculateFodSensorLocation(context);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        Display display = displayManager.getDisplay(0);
        display.getSupportedModes();
        int[] mFodLocation = {this.GXZW_ICON_X, this.GXZW_ICON_Y, this.GXZW_ICON_WIDTH, this.GXZW_ICON_HEIGHT};
        return mFodLocation;
    }

    public void calculateFodSensorLocation(Context context) {
        if (this.SCREEN_WIDTH_PHYSICAL == -1) {
            phySicalScreenPx(context);
        }
        screenWhPx(context);
        String xyString = SystemProperties.get("persist.vendor.sys.fp.fod.location.X_Y", "");
        String whString = SystemProperties.get("persist.vendor.sys.fp.fod.size.width_height", "");
        if (xyString.isEmpty() || whString.isEmpty()) {
            resetDefaultValue();
            return;
        }
        try {
            this.GXZW_ICON_X = Integer.parseInt(xyString.split(",")[0]);
            this.GXZW_ICON_Y = Integer.parseInt(xyString.split(",")[1]);
            this.GXZW_ICON_WIDTH = Integer.parseInt(whString.split(",")[0]);
            this.GXZW_ICON_HEIGHT = Integer.parseInt(whString.split(",")[1]);
            this.GXZW_X_PRCENT = getPrcent(this.GXZW_ICON_X, this.SCREEN_WIDTH_PHYSICAL);
            this.GXZW_Y_PRCENT = getPrcent(this.GXZW_ICON_Y, this.SCREEN_HEIGHT_PHYSICAL);
            this.GXZW_WIDTH_PRCENT = getPrcent(this.GXZW_ICON_WIDTH, this.SCREEN_WIDTH_PHYSICAL);
            float prcent = getPrcent(this.GXZW_ICON_HEIGHT, this.SCREEN_HEIGHT_PHYSICAL);
            this.GXZW_HEIGHT_PRCENT = prcent;
            int i = this.SCREEN_WIDTH_PX;
            this.GXZW_ICON_X = (int) (i * this.GXZW_X_PRCENT);
            int i2 = this.SCREEN_HEIGHT_PX;
            this.GXZW_ICON_Y = (int) (i2 * this.GXZW_Y_PRCENT);
            this.GXZW_ICON_WIDTH = (int) (i * this.GXZW_WIDTH_PRCENT);
            this.GXZW_ICON_HEIGHT = (int) (i2 * prcent);
        } catch (Exception e) {
            e.printStackTrace();
            resetDefaultValue();
        }
    }

    public float getPrcent(int xory, int widthorheight) {
        if (widthorheight == 0 || xory == 0) {
            return 1.0f;
        }
        return new BigDecimal(xory).divide(new BigDecimal(widthorheight), 10, 5).floatValue();
    }

    public void phySicalScreenPx(Context context) {
        if (this.MIUI_DEFAULT_RESOLUTION.isEmpty()) {
            DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
            Display display = displayManager.getDisplay(0);
            this.SCREEN_WIDTH_PHYSICAL = display.getMode().getPhysicalWidth();
            this.SCREEN_HEIGHT_PHYSICAL = display.getMode().getPhysicalHeight();
            return;
        }
        this.SCREEN_WIDTH_PHYSICAL = Integer.parseInt(this.MIUI_DEFAULT_RESOLUTION.split("x")[0]);
        this.SCREEN_HEIGHT_PHYSICAL = Integer.parseInt(this.MIUI_DEFAULT_RESOLUTION.split("x")[1]);
    }

    private void screenWhPx(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        DisplayMetrics dm = new DisplayMetrics();
        displayManager.getDisplay(0).getRealMetrics(dm);
        boolean orientation = context.getResources().getConfiguration().orientation == 1;
        this.SCREEN_WIDTH_PX = orientation ? dm.widthPixels : dm.heightPixels;
        this.SCREEN_HEIGHT_PX = orientation ? dm.heightPixels : dm.widthPixels;
    }

    private void resetDefaultValue() {
        this.GXZW_ICON_X = 453;
        this.GXZW_ICON_Y = 1640;
        this.GXZW_ICON_WIDTH = 173;
        this.GXZW_ICON_HEIGHT = 173;
    }
}
