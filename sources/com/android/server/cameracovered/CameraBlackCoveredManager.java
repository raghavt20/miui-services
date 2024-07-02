package com.android.server.cameracovered;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.WindowManager;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.android.server.wm.WindowManagerInternal;
import com.miui.server.AccessController;
import com.miui.server.stability.DumpSysInfoUtil;
import java.util.ArrayList;
import java.util.List;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class CameraBlackCoveredManager {
    public static final String ACTION_FULLSCREEN_STATE_CHANGE = "com.miui.fullscreen_state_change";
    public static final String ACTION_LAUNCH_HOME_FROM_HOTKEY = "com.miui.launch_home_from_hotkey";
    private static final int COVERED_BLACK_TYPE = 1;
    private static final int COVERED_HBM_TYPE = 2;
    private static final int COVERED_MURA_TYPE = 3;
    private static final int CUP_MURA_COVER_ENTER = 6;
    private static final int CUP_MURA_COVER_EXIT = 7;
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_STATE_NAME = "state";
    private static final String FORCE_BLACK_V2 = "force_black_v2";
    private static final int FRONT_CAMERA_CLOSE = 1;
    private static final int FRONT_CAMERA_HIDE = 4;
    private static final int FRONT_CAMERA_OPEN = 0;
    private static final int FRONT_CAMERA_SHOW = 5;
    private static final int HBM_ENTER = 2;
    private static final int HBM_EXIT = 3;
    private static final String PERMISSION_INTERNAL_GENERAL_API = "miui.permission.USE_INTERNAL_GENERAL_API";
    public static final String STATE_CLOSE_WINDOW = "closeWindow";
    public static final String STATE_QUICK_SWITCH_ANIM_END = "quickSwitchAnimEnd";
    public static final String STATE_TO_ANOTHER_APP = "toAnotherApp";
    public static final String STATE_TO_HOME = "toHome";
    public static final String STATE_TO_RECENTS = "toRecents";
    public static final String TAG = "CameraBlackCoveredManager";
    private static final int VIRTUAL_CAMERA_BOUNDARY = 100;
    public static final ArrayList<String> sBlackList;
    public static final ArrayList<String> sBroadcastBlackList;
    public static final ArrayList<String> sForceShowList;
    private CameraCircleBlackView mBlackCoveredView;
    private CameraManager mCameraManager;
    private Context mContext;
    private Display mDisplay;
    private DisplayInfo mDisplayInfo;
    private DisplayManager mDisplayManager;
    private String mForegroundPackage;
    private CameraHBMCoveredView mHBMCoveredView;
    private Handler mHandler;
    private boolean mHideNotch;
    private ContentObserver mHideNotchObserver;
    private HomeGestureReceiver mHomeGestureReceiver;
    private boolean mIsAddedView;
    private int mLastDisplayRotation;
    private int mLastUnavailableCameraId;
    private CupMuraCoveredView mMuraCoveredView;
    private String mOccupiedPackage;
    private WindowManager mWindowManager;
    private WindowManagerInternal mWindowManagerService;
    private IntentFilter mFilter_state_change = new IntentFilter();
    private IntentFilter mFilter_launch_home = new IntentFilter();
    private List<Integer> mFrontCameraID = new ArrayList();
    private List<Integer> mBackCameraID = new ArrayList();
    private boolean mIsAddedBlackView = false;
    private boolean mIsAddedHBMView = false;
    private boolean mIsAddedMuraView = false;
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.cameracovered.CameraBlackCoveredManager.1
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            CameraBlackCoveredManager.this.mDisplay.getDisplayInfo(CameraBlackCoveredManager.this.mDisplayInfo);
            if (CameraBlackCoveredManager.this.mLastDisplayRotation != CameraBlackCoveredManager.this.mDisplayInfo.rotation) {
                Slog.d(CameraBlackCoveredManager.TAG, "onDisplayChanged");
                CameraBlackCoveredManager.this.stopCameraAnimation();
                CameraBlackCoveredManager.this.startCameraAnimation(false);
            }
        }
    };
    private CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.cameracovered.CameraBlackCoveredManager.2
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            int id = Integer.valueOf(cameraId).intValue();
            if (CameraBlackCoveredManager.this.mFrontCameraID.contains(Integer.valueOf(id)) && id == 1) {
                Slog.d(CameraBlackCoveredManager.TAG, "wz_debug onCameraAvailable" + id);
                CameraBlackCoveredManager.this.mDisplayManager.unregisterDisplayListener(CameraBlackCoveredManager.this.mDisplayListener);
                CameraBlackCoveredManager.this.stopCameraAnimation();
                Slog.d(CameraBlackCoveredManager.TAG, "onCameraAvailable " + id);
            }
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            if (CameraBlackCoveredManager.this.mFrontCameraID.size() == 0) {
                CameraBlackCoveredManager.this.initCameraId();
            }
            super.onCameraUnavailable(cameraId);
            int id = Integer.valueOf(cameraId).intValue();
            if (CameraBlackCoveredManager.this.mFrontCameraID.contains(Integer.valueOf(id))) {
                CameraBlackCoveredManager.this.mDisplayManager.registerDisplayListener(CameraBlackCoveredManager.this.mDisplayListener, CameraBlackCoveredManager.this.mHandler);
                CameraBlackCoveredManager.this.mDisplay.getDisplayInfo(CameraBlackCoveredManager.this.mDisplayInfo);
                CameraBlackCoveredManager.this.startCameraAnimation(false);
                Slog.d(CameraBlackCoveredManager.TAG, "onCameraUnavailable " + id);
            }
            CameraBlackCoveredManager.this.mLastUnavailableCameraId = id;
        }
    };
    private IForegroundInfoListener.Stub mForegroundInfoChangeListener = new IForegroundInfoListener.Stub() { // from class: com.android.server.cameracovered.CameraBlackCoveredManager.4
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            CameraBlackCoveredManager.this.mForegroundPackage = foregroundInfo.mForegroundPackageName;
            Log.d(CameraBlackCoveredManager.TAG, "mForegroundPackage = " + CameraBlackCoveredManager.this.mForegroundPackage + " mOccupiedPackage = " + CameraBlackCoveredManager.this.mOccupiedPackage);
            if (CameraBlackCoveredManager.this.mIsAddedBlackView && foregroundInfo.mForegroundPackageName.equals(CameraBlackCoveredManager.this.mOccupiedPackage)) {
                CameraBlackCoveredManager.this.mHandler.sendEmptyMessage(5);
            }
            CameraBlackCoveredManager.this.mLastUnavailableCameraId = -1;
        }
    };

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sBlackList = arrayList;
        arrayList.add("com.miui.home");
        arrayList.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        arrayList.add(AccessController.PACKAGE_SYSTEMUI);
        arrayList.add("com.miui.face");
        arrayList.add("light.sensor.service");
        ArrayList<String> arrayList2 = new ArrayList<>();
        sBroadcastBlackList = arrayList2;
        arrayList2.add("com.tencent.mm");
        arrayList2.add("com.tencent.mobileqq");
        arrayList2.add("com.skype.rover");
        ArrayList<String> arrayList3 = new ArrayList<>();
        sForceShowList = arrayList3;
        arrayList3.add(AccessController.PACKAGE_CAMERA);
    }

    public CameraBlackCoveredManager(Context context, WindowManagerInternal service) {
        this.mLastDisplayRotation = 0;
        this.mHideNotchObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.cameracovered.CameraBlackCoveredManager.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                CameraBlackCoveredManager cameraBlackCoveredManager = CameraBlackCoveredManager.this;
                cameraBlackCoveredManager.mHideNotch = Settings.Global.getInt(cameraBlackCoveredManager.mContext.getContentResolver(), CameraBlackCoveredManager.FORCE_BLACK_V2, 0) != 0;
            }
        };
        this.mContext = context;
        this.mWindowManagerService = service;
        this.mWindowManager = (WindowManager) context.getSystemService(DumpSysInfoUtil.WINDOW);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        this.mDisplayManager = displayManager;
        this.mDisplay = displayManager.getDisplay(0);
        DisplayInfo displayInfo = new DisplayInfo();
        this.mDisplayInfo = displayInfo;
        this.mDisplay.getDisplayInfo(displayInfo);
        this.mLastDisplayRotation = this.mDisplayInfo.rotation;
        HandlerThread t = new HandlerThread("camera_animation", -2);
        t.start();
        this.mHandler = new H(t.getLooper());
        this.mBlackCoveredView = new CameraCircleBlackView(context);
        this.mHBMCoveredView = new CameraHBMCoveredView(context);
        this.mBlackCoveredView.setForceDarkAllowed(false);
        this.mHBMCoveredView.setForceDarkAllowed(false);
        CupMuraCoveredView cupMuraCoveredView = new CupMuraCoveredView(context);
        this.mMuraCoveredView = cupMuraCoveredView;
        cupMuraCoveredView.setForceDarkAllowed(false);
        this.mIsAddedView = false;
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(FORCE_BLACK_V2), false, this.mHideNotchObserver, -1);
        this.mHideNotchObserver.onChange(false);
        String deviceNM = Build.DEVICE;
        if ((deviceNM != null && deviceNM.equals("odin")) || ((deviceNM != null && deviceNM.equals("mona")) || (deviceNM != null && deviceNM.equals("zijin")))) {
            sBlackList.add(AccessController.PACKAGE_CAMERA);
        }
    }

    public void systemReady() {
        CameraManager cameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mCameraManager = cameraManager;
        cameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mHandler);
        registerForegroundInfoChangeListener();
        initCameraId();
        this.mHomeGestureReceiver = new HomeGestureReceiver();
        this.mFilter_state_change.addAction(ACTION_FULLSCREEN_STATE_CHANGE);
        this.mContext.registerReceiver(this.mHomeGestureReceiver, this.mFilter_state_change);
        this.mFilter_launch_home.addAction(ACTION_LAUNCH_HOME_FROM_HOTKEY);
        this.mContext.registerReceiverAsUser(this.mHomeGestureReceiver, UserHandle.CURRENT, this.mFilter_launch_home, "miui.permission.USE_INTERNAL_GENERAL_API", null);
    }

    public void setCoverdPackageName(String packageName) {
        this.mOccupiedPackage = packageName;
    }

    public void hbmCoveredAnimation(int type) {
        if (type != 1 || this.mHideNotch) {
            if (type == 0) {
                this.mHandler.sendEmptyMessage(3);
                return;
            } else {
                Slog.e(TAG, "hbmCoveredAnimation for HBM the type " + type + " error!");
                return;
            }
        }
        Message msg = Message.obtain();
        msg.what = 2;
        this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        int i = this.mDisplayInfo.rotation;
        this.mLastDisplayRotation = i;
        msg.obj = Integer.valueOf(i);
        this.mHandler.sendMessage(msg);
    }

    public void cupMuraCoveredAnimation(int type) {
        if (type != 1 || this.mHideNotch) {
            if (type == 0) {
                this.mHandler.sendEmptyMessage(7);
                return;
            } else {
                Slog.d(TAG, "cupMuraCoveredAnimation " + type + " error!");
                return;
            }
        }
        Message msg = Message.obtain();
        msg.what = 6;
        this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        int i = this.mDisplayInfo.rotation;
        this.mLastDisplayRotation = i;
        msg.obj = Integer.valueOf(i);
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initCameraId() {
        try {
            String[] ids = this.mCameraManager.getCameraIdList();
            if (ids != null && ids.length > 0) {
                for (String sid : ids) {
                    int id = Integer.valueOf(sid).intValue();
                    if (id < 100) {
                        CameraCharacteristics cc = this.mCameraManager.getCameraCharacteristics(sid);
                        int facing = ((Integer) cc.get(CameraCharacteristics.LENS_FACING)).intValue();
                        if (facing == 0) {
                            this.mFrontCameraID.add(Integer.valueOf(id));
                            Slog.d(TAG, "wz_debug mFrontCameraID" + id);
                        } else if (facing == 1) {
                            this.mBackCameraID.add(Integer.valueOf(id));
                            Slog.d(TAG, "wz_debug mBackCameraID" + id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Slog.d(TAG, "Can't initCameraId");
        }
    }

    public void hideCoveredBlackView() {
        if (this.mIsAddedBlackView) {
            this.mHandler.sendEmptyMessage(4);
        }
    }

    public void startCameraAnimation(boolean forceShow) {
        if (forceShow) {
            this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        }
        if (canShowBlackCovered(forceShow)) {
            Message msg = Message.obtain();
            msg.what = 0;
            int i = this.mDisplayInfo.rotation;
            this.mLastDisplayRotation = i;
            msg.obj = Integer.valueOf(i);
            this.mHandler.sendMessage(msg);
        }
    }

    public void stopCameraAnimation() {
        this.mHandler.sendEmptyMessage(1);
    }

    public boolean canShowBlackCovered(boolean forceShow) {
        if (forceShow) {
            return !this.mHideNotch && sForceShowList.contains(this.mOccupiedPackage);
        }
        String cameraPackageFromProp = SystemProperties.get("vendor.debug.camera.pkgname", "");
        if (!this.mHideNotch) {
            ArrayList<String> arrayList = sBlackList;
            if (!arrayList.contains(this.mOccupiedPackage) && !arrayList.contains(cameraPackageFromProp)) {
                return true;
            }
        }
        return false;
    }

    public void registerForegroundInfoChangeListener() {
        ProcessManager.unregisterForegroundInfoListener(this.mForegroundInfoChangeListener);
        ProcessManager.registerForegroundInfoListener(this.mForegroundInfoChangeListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WindowManager.LayoutParams getWindowParam(int ori, int type) {
        int h = Integer.parseInt(SystemProperties.get("persist.sys.cameraHeight", "0"));
        int w = Integer.parseInt(SystemProperties.get("persist.sys.cameraWidth", "0"));
        if (h == 0 || w == 0) {
            h = this.mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_button_max_width);
            w = this.mContext.getResources().getDimensionPixelSize(R.dimen.action_bar_content_inset_with_nav);
        }
        switch (ori) {
            case 1:
            case 3:
                int tempW = w;
                w = h;
                h = tempW;
                break;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h, 2015, 5432, -3);
        lp.privateFlags |= 64;
        lp.privateFlags |= 16;
        lp.layoutInDisplayCutoutMode = 1;
        switch (ori) {
            case 0:
                lp.gravity = 51;
                break;
            case 1:
                lp.gravity = 83;
                break;
            case 2:
                lp.gravity = 85;
                break;
            case 3:
                lp.gravity = 53;
                break;
        }
        if (type == 1) {
            lp.setTitle("cameraBlackCovered");
        } else if (type == 2) {
            lp.setTitle("cameraHBMCovered");
        } else if (type == 3) {
            lp.setTitle("cupMuraCovered");
            lp.type = 2006;
        }
        return lp;
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int ori = ((Integer) msg.obj).intValue();
                    WindowManager.LayoutParams layoutParams = CameraBlackCoveredManager.this.getWindowParam(ori, 1);
                    Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_BlackView is added FRONT_CAMERA_OPEN");
                    if (CameraBlackCoveredManager.this.mBlackCoveredView != null && !CameraBlackCoveredManager.this.mIsAddedBlackView) {
                        float radius = Float.parseFloat(SystemProperties.get("persist.sys.cameraRadius", "0.0"));
                        if (radius == 0.0d) {
                            radius = CameraBlackCoveredManager.this.mContext.getResources().getDimension(R.dimen.action_bar_content_inset_material);
                        }
                        CameraBlackCoveredManager.this.mBlackCoveredView.setRadius(radius);
                        CameraBlackCoveredManager.this.mWindowManager.addView(CameraBlackCoveredManager.this.mBlackCoveredView, layoutParams);
                        Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_BlackView is added");
                        CameraBlackCoveredManager.this.mIsAddedBlackView = true;
                        CameraBlackCoveredManager.this.mBlackCoveredView.setVisibility(0);
                        return;
                    }
                    return;
                case 1:
                    if (CameraBlackCoveredManager.this.mBlackCoveredView != null && CameraBlackCoveredManager.this.mIsAddedBlackView) {
                        CameraBlackCoveredManager.this.mWindowManager.removeView(CameraBlackCoveredManager.this.mBlackCoveredView);
                        Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_BlackView is removed");
                        CameraBlackCoveredManager.this.mIsAddedBlackView = false;
                        return;
                    }
                    return;
                case 2:
                    int ori2 = ((Integer) msg.obj).intValue();
                    WindowManager.LayoutParams layoutParams2 = CameraBlackCoveredManager.this.getWindowParam(ori2, 2);
                    if (CameraBlackCoveredManager.this.mHBMCoveredView != null && !CameraBlackCoveredManager.this.mIsAddedHBMView) {
                        float radius2 = Float.parseFloat(SystemProperties.get("persist.sys.cameraRadius", "0.0"));
                        if (radius2 == 0.0d) {
                            radius2 = CameraBlackCoveredManager.this.mContext.getResources().getDimension(R.dimen.action_bar_content_inset_material);
                        }
                        CameraBlackCoveredManager.this.mHBMCoveredView.setRadius(radius2);
                        CameraBlackCoveredManager.this.mWindowManager.addView(CameraBlackCoveredManager.this.mHBMCoveredView, layoutParams2);
                        Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_HBMView is added");
                        CameraBlackCoveredManager.this.mIsAddedHBMView = true;
                        return;
                    }
                    return;
                case 3:
                    if (CameraBlackCoveredManager.this.mHBMCoveredView != null && CameraBlackCoveredManager.this.mIsAddedHBMView) {
                        CameraBlackCoveredManager.this.mWindowManager.removeView(CameraBlackCoveredManager.this.mHBMCoveredView);
                        Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_HBMView is removed");
                        CameraBlackCoveredManager.this.mIsAddedHBMView = false;
                        return;
                    }
                    return;
                case 4:
                    if (CameraBlackCoveredManager.this.mBlackCoveredView != null && CameraBlackCoveredManager.this.mIsAddedBlackView) {
                        CameraBlackCoveredManager.this.mBlackCoveredView.setVisibility(4);
                        Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_BlackView is hided.");
                        return;
                    }
                    return;
                case 5:
                    if (CameraBlackCoveredManager.this.mBlackCoveredView != null && CameraBlackCoveredManager.this.mIsAddedBlackView) {
                        CameraBlackCoveredManager.this.mBlackCoveredView.setVisibility(0);
                        Slog.d(CameraBlackCoveredManager.TAG, "FrontCamera_BlackView is showed.");
                        return;
                    }
                    return;
                case 6:
                    int ori3 = ((Integer) msg.obj).intValue();
                    WindowManager.LayoutParams layoutParams3 = CameraBlackCoveredManager.this.getWindowParam(ori3, 3);
                    if (CameraBlackCoveredManager.this.mMuraCoveredView != null && !CameraBlackCoveredManager.this.mIsAddedMuraView) {
                        float radius3 = Float.parseFloat(SystemProperties.get("persist.sys.cameraRadius", "0.0"));
                        if (radius3 == 0.0d) {
                            radius3 = CameraBlackCoveredManager.this.mContext.getResources().getDimension(R.dimen.action_bar_content_inset_material);
                        }
                        CameraBlackCoveredManager.this.mMuraCoveredView.setRadius(radius3);
                        layoutParams3.alpha = 0.001f;
                        CameraBlackCoveredManager.this.mWindowManager.addView(CameraBlackCoveredManager.this.mMuraCoveredView, layoutParams3);
                        Slog.d(CameraBlackCoveredManager.TAG, "CUP_MURA_COVER is added");
                        CameraBlackCoveredManager.this.mIsAddedMuraView = true;
                        return;
                    }
                    return;
                case 7:
                    if (CameraBlackCoveredManager.this.mMuraCoveredView != null && CameraBlackCoveredManager.this.mIsAddedMuraView) {
                        CameraBlackCoveredManager.this.mWindowManager.removeView(CameraBlackCoveredManager.this.mMuraCoveredView);
                        Slog.d(CameraBlackCoveredManager.TAG, "CUP_MURA_COVER is removed");
                        CameraBlackCoveredManager.this.mIsAddedMuraView = false;
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    private final class HomeGestureReceiver extends BroadcastReceiver {
        private HomeGestureReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            CameraBlackCoveredManager.this.handleHomeGestureReceiver(intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleHomeGestureReceiver(Intent intent) {
        char c;
        String str;
        Slog.d(TAG, "handleHomeGestureReceiver receive broadcast action is " + intent.getAction());
        if (this.mBlackCoveredView == null || !this.mIsAddedBlackView) {
            return;
        }
        String freeformPackageName = this.mWindowManagerService.getTopStackPackageName(5);
        if (sBroadcastBlackList.contains(this.mOccupiedPackage)) {
            Slog.d(TAG, this.mOccupiedPackage + " is in sBroadcastBlackList");
            return;
        }
        if (freeformPackageName != null && (str = this.mOccupiedPackage) != null && str.equals(freeformPackageName)) {
            Slog.d(TAG, "The top app " + freeformPackageName + " is freeform mode and using the camera service!");
            return;
        }
        String action = intent.getAction();
        switch (action.hashCode()) {
            case 903231409:
                if (action.equals(ACTION_FULLSCREEN_STATE_CHANGE)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1785331554:
                if (action.equals(ACTION_LAUNCH_HOME_FROM_HOTKEY)) {
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
                String state = intent.getExtras().getString("state");
                String packageName = intent.getExtras().getString(EXTRA_PACKAGE_NAME);
                Log.d(TAG, "handleHomeGestureReceiver action: com.miui.fullscreen_state_changeEXTRA_STATE_NAME = " + state);
                if (STATE_TO_RECENTS.equals(state) || STATE_TO_HOME.equals(state) || STATE_TO_ANOTHER_APP.equals(state) || STATE_CLOSE_WINDOW.equals(state)) {
                    this.mHandler.sendEmptyMessage(4);
                    return;
                }
                String str2 = this.mOccupiedPackage;
                if (str2 != null && str2.equals(packageName) && STATE_QUICK_SWITCH_ANIM_END.equals(state)) {
                    this.mHandler.sendEmptyMessage(5);
                    return;
                } else {
                    Log.d("TAG", "wz_debug handleHomeGestureReceiver state = " + state);
                    return;
                }
            case 1:
                Log.d(TAG, "handleHomeGestureReceiver action: com.miui.launch_home_from_hotkey");
                this.mHandler.sendEmptyMessage(4);
                return;
            default:
                Log.e(TAG, "handleHomeGestureReceiver action: " + intent.getAction());
                return;
        }
    }
}
