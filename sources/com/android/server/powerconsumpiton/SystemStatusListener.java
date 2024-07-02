package com.android.server.powerconsumpiton;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Handler;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.wm.WindowManagerService;
import com.miui.server.stability.DumpSysInfoUtil;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class SystemStatusListener {
    private static final long DECREASE_BRIGHTNESS_WAIT_TIME = 180000;
    private static final String GAME_MODE_URI = "gb_boosting";
    private static final String GTB_BRIGHTNESS_ADJUST = "gb_brightness";
    private static final boolean SUPPORT_GAME_DIM = FeatureParser.getBoolean("support_game_dim", false);
    private static final boolean SUPPORT_VIDEO_IDLE_DIM = FeatureParser.getBoolean("support_video_idle_dim", false);
    private static final String VIDEO_IDLE_STATUS = "video_idle_status";
    private static final String VIDEO_IDLE_TIMEOUT = "video_idle_timeout";
    private static final int VIRTUAL_CAMERA_BOUNDARY = 100;
    private CameraManager mCameraManager;
    private Context mContext;
    private Handler mHandler;
    private PowerConsumptionServiceController mPowerConsumptionServiceController;
    private FrameworkPowerSaveModeObserver mPowerSaveModeObserver;
    private ContentResolver mResolver;
    private SettingsObserver mSettingsObserver;
    private boolean mVideoIdleStatus;
    private final Set<Integer> mOpeningCameraID = new HashSet();
    private boolean mDimEnable = false;
    private long mBrightWaitTime = DECREASE_BRIGHTNESS_WAIT_TIME;
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.powerconsumpiton.SystemStatusListener.1
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            SystemStatusListener.this.mOpeningCameraID.remove(Integer.valueOf(id));
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            SystemStatusListener.this.mOpeningCameraID.add(Integer.valueOf(id));
        }
    };
    private WindowManagerService mWindowManagerService = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
    private PowerConsumptionPointerEventListener mPowerConsumptionPointerEventListener = new PowerConsumptionPointerEventListener();

    public SystemStatusListener(Handler handler, Context context) {
        this.mHandler = handler;
        this.mContext = context;
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
        this.mPowerSaveModeObserver = new FrameworkPowerSaveModeObserver(context, this.mHandler.getLooper());
        initSettingsObserver();
    }

    private void initSettingsObserver() {
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mHandler);
        this.mResolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("gb_boosting"), true, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(GTB_BRIGHTNESS_ADJUST), false, this.mSettingsObserver, -1);
        if (SUPPORT_VIDEO_IDLE_DIM) {
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(VIDEO_IDLE_STATUS), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(VIDEO_IDLE_TIMEOUT), false, this.mSettingsObserver, -1);
        }
        this.mWindowManagerService.registerPointerEventListener(this.mPowerConsumptionPointerEventListener, 0);
    }

    public void setPowerConsumptionServiceController(PowerConsumptionServiceController powerConsumptionServiceController) {
        this.mPowerConsumptionServiceController = powerConsumptionServiceController;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            SystemStatusListener.this.handleSettingsChangedLocked(uri);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleSettingsChangedLocked(Uri uri) {
        char c;
        String lastPathSegment = uri.getLastPathSegment();
        switch (lastPathSegment.hashCode()) {
            case -1568446215:
                if (lastPathSegment.equals(VIDEO_IDLE_STATUS)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -793979590:
                if (lastPathSegment.equals(VIDEO_IDLE_TIMEOUT)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -377350077:
                if (lastPathSegment.equals("gb_boosting")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1345941045:
                if (lastPathSegment.equals(GTB_BRIGHTNESS_ADJUST)) {
                    c = 1;
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
                updateGameDimState();
                return;
            case 2:
            case 3:
                updateVideoDimState();
                return;
            default:
                return;
        }
    }

    private void updateGameDimState() {
        boolean gameBrightnessEnable = Settings.Secure.getIntForUser(this.mResolver, GTB_BRIGHTNESS_ADJUST, 1, -2) == 1;
        boolean gameModeEnable = Settings.Secure.getIntForUser(this.mResolver, "gb_boosting", 0, -2) == 1;
        this.mBrightWaitTime = DECREASE_BRIGHTNESS_WAIT_TIME;
        boolean z = gameBrightnessEnable && gameModeEnable && SUPPORT_GAME_DIM;
        this.mDimEnable = z;
        this.mPowerConsumptionServiceController.updateDimState(z, DECREASE_BRIGHTNESS_WAIT_TIME);
    }

    private void updateVideoDimState() {
        boolean z = false;
        this.mVideoIdleStatus = Settings.Secure.getIntForUser(this.mResolver, VIDEO_IDLE_STATUS, 0, -2) == 1;
        long longForUser = Settings.Secure.getLongForUser(this.mResolver, VIDEO_IDLE_TIMEOUT, DECREASE_BRIGHTNESS_WAIT_TIME, -2);
        this.mBrightWaitTime = longForUser;
        if (this.mVideoIdleStatus && SUPPORT_VIDEO_IDLE_DIM) {
            z = true;
        }
        this.mDimEnable = z;
        this.mPowerConsumptionServiceController.updateDimState(z, longForUser);
    }

    public boolean isCameraOpen() {
        return this.mOpeningCameraID.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isPowerSaveMode() {
        return this.mPowerSaveModeObserver.isPowerSaveMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PowerConsumptionPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        private PowerConsumptionPointerEventListener() {
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 0:
                    SystemStatusListener.this.mPowerConsumptionServiceController.setScreenOffTimeOutDelay();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("  isCameraOpen=" + isCameraOpen());
        pw.println("  mDimEnable=" + this.mDimEnable);
        pw.println("  mBrightWaitTime=" + this.mBrightWaitTime);
    }
}
