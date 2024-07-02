package com.android.server.wm;

import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IWallpaperManagerCallback;
import android.app.IWindowSecureChangeListener;
import android.app.WallpaperColors;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IFlipWindowStateListener;
import android.view.IWindowAnimationFinishedCallback;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.window.ScreenCapture;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.LocalServices;
import com.android.server.display.DisplayControl;
import com.android.server.display.mode.DisplayModeDirectorStub;
import com.android.server.wallpaper.WallpaperManagerInternal;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceImpl;
import com.miui.app.smartpower.SmartPowerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.miui.whetstone.PowerKeeperPolicy;
import com.xiaomi.screenprojection.IMiuiScreenProjectionStub;
import java.io.Closeable;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import miui.app.StorageRestrictedPathManager;
import miui.io.IOUtils;
import miui.security.SecurityManagerInternal;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class WindowManagerServiceImpl implements WindowManagerServiceStub {
    private static final boolean BACKGROUND_BLUR_STATUS_DEFAULT;
    private static final boolean BACKGROUND_BLUR_SUPPORTED;
    private static final String BUILD_DENSITY_PROP = "ro.sf.lcd_density";
    private static final String CARWITH_PACKAGE_NAME = "com.miui.carlink";
    private static final String CARWITH_TRUSTED_DAILY_SIG = "c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8";
    private static final String CARWITH_TRUSTED_NON_DAILY_SIG = "c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025";
    private static final String CIRCULATE = "home_worldCirculate_and_smallWindow_crop";
    private static final String CIRCULATEACTIVITY = "com.miui.circulate.world.AppCirculateActivity";
    private static final String CLIENT_DIMMER_NAME = "ClientDimmerForAppPair";
    private static String CUR_DEVICE = null;
    public static String FIND_DEVICE = null;
    private static String[] FORCE_ORI_DEVICES_LIST = null;
    private static String[] FORCE_ORI_LIST = null;
    public static String GOOGLE = null;
    public static String GOOGLE_FLOATING = null;
    private static final int INVALID_APPEARANCE = -1;
    private static final String LAUNCHER = "com.miui.home/.launcher.Launcher";
    private static final String MIUI_RESOLUTION = "persist.sys.miui_resolution";
    public static String MM = null;
    public static String MM_FLOATING = null;
    public static final int MSG_SERVICE_INIT_AFTER_BOOT = 107;
    public static final int MSG_UPDATE_BLUR_WALLPAPER = 106;
    public static String QQ = null;
    public static String QQ_FLOATING = null;
    public static final int SCREEN_SHARE_PROTECTION_OPEN = 1;
    public static final int SCREEN_SHARE_PROTECTION_WITH_BLUR = 1;
    public static String SECURITY = null;
    public static String SECURITY_FLOATING = null;
    private static final String SHA_256 = "SHA-256";
    private static final boolean SUPPORT_UNION_POWER_CORE;
    private static final boolean SUPPPORT_CLOUD_DIM;
    private static final String TAG = "WindowManagerService";
    private static final int VIRTUAL_CAMERA_BOUNDARY = 100;
    private static ArrayList<String> mFloatWindowMirrorWhiteList;
    private static List<String> mProjectionBlackList;
    boolean isFpClientOn;
    AppOpsManager mAppOps;
    ActivityTaskManagerService mAtmService;
    private List<String> mBlackList;
    private Bitmap mBlurWallpaperBmp;
    private BroadcastReceiver mBootCompletedReceiver;
    private CameraManager mCameraManager;
    Context mContext;
    DisplayContent mDisplayContent;
    DisplayManagerInternal mDisplayManagerInternal;
    private WindowManagerGlobalLock mGlobalLock;
    private boolean mIsResolutionChanged;
    MiuiContrastOverlayStub mMiuiContrastOverlay;
    private int mMiuiDisplayDensity;
    private int mMiuiDisplayHeight;
    private int mMiuiDisplayWidth;
    private int mNavigationColor;
    private boolean mPendingSwitchResolution;
    private int mPhysicalDensity;
    private int mPhysicalHeight;
    private int mPhysicalWidth;
    private boolean mRunningRecentsAnimation;
    private SecurityManagerInternal mSecurityInternal;
    private SmartPowerServiceInternal mSmartPowerServiceInternal;
    private boolean mSupportActiveModeSwitch;
    private boolean mSupportSwitchResolutionFeature;
    IWindowAnimationFinishedCallback mUiModeAnimFinishedCallback;
    private IWallpaperManagerCallback mWallpaperCallback;
    WindowManagerService mWmService;
    private MiuiHoverModeInternal miuiHoverModeInternal;
    private HashMap<String, Integer> mDimUserTimeoutMap = new HashMap<>();
    private HashMap<String, Boolean> mDimNeedAssistMap = new HashMap<>();
    private PowerKeeperPolicy mPowerKeeperPolicy = null;
    private SparseArray<Float> mTaskIdScreenBrightnessOverrides = new SparseArray<>();
    boolean IS_CTS_MODE = false;
    boolean HAS_SCREEN_SHOT = false;
    private boolean mSplitMode = false;
    private Set mOpeningCameraID = new HashSet();
    private final Uri mDarkModeEnable = Settings.Secure.getUriFor("ui_night_mode");
    private final Uri mDarkModeContrastEnable = Settings.System.getUriFor("dark_mode_contrast_enable");
    private final Uri mContrastAlphaUri = Settings.System.getUriFor("contrast_alpha");
    public boolean mLastWindowHasSecure = false;
    private Rect leftStatusBounds = new Rect();
    private Rect rightStatusBounds = new Rect();
    private int leftAppearance = -1;
    private int rightAppearance = -1;
    private SurfaceControl mMiuiPaperContrastOverlay = null;
    private Boolean mLastIsFullScreen = null;
    private final ArrayMap<IBinder, IFlipWindowStateListener> mNavigationBarColorListenerMap = new ArrayMap<>();
    private final Object mFlipListenerLock = new Object();
    private ArrayList<String> mMiuiSecurityImeWhiteList = new ArrayList() { // from class: com.android.server.wm.WindowManagerServiceImpl.1
        {
            add("WifiSettingsActivity");
            add("WifiConfigActivity");
            add("WifiProvisionSettingsActivity");
        }
    };
    private HashSet<Integer> mTransitionReadyList = new HashSet<>();
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.wm.WindowManagerServiceImpl.4
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            WindowManagerServiceImpl.this.mOpeningCameraID.remove(Integer.valueOf(id));
            if (WindowManagerServiceImpl.this.mOpeningCameraID.size() == 0) {
                DisplayContent displayContent = WindowManagerServiceImpl.this.mDisplayContent;
            }
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            WindowManagerServiceImpl.this.mOpeningCameraID.add(Integer.valueOf(id));
        }
    };
    final ArrayList<IWindowSecureChangeListener> mSecureChangeListeners = new ArrayList<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowManagerServiceImpl> {

        /* compiled from: WindowManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowManagerServiceImpl INSTANCE = new WindowManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public WindowManagerServiceImpl m2804provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public WindowManagerServiceImpl m2803provideNewInstance() {
            return new WindowManagerServiceImpl();
        }
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        mFloatWindowMirrorWhiteList = arrayList;
        arrayList.add("com.alibaba.android.rimet");
        mFloatWindowMirrorWhiteList.add("com.tencent.mm");
        FORCE_ORI_LIST = new String[]{"com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity", "com.tencent.mm/com.tencent.mm.plugin.multitalk.ui.MultiTalkMainUI"};
        FORCE_ORI_DEVICES_LIST = new String[]{"lithium", "chiron", "polaris"};
        CUR_DEVICE = Build.DEVICE;
        mProjectionBlackList = new ArrayList();
        MM = "com.tencent.mm";
        QQ = "com.tencent.mobileqq";
        GOOGLE = "com.google.android.dialer";
        SECURITY = ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME;
        MM_FLOATING = "com.tencent.mm/.FloatingWindow";
        QQ_FLOATING = "com.tencent.mobileqq/.FloatingWindow";
        GOOGLE_FLOATING = "com.google.android.dialer/.FloatingWindow";
        SECURITY_FLOATING = "com.miui.securitycenter/.FloatingWindow";
        FIND_DEVICE = "com.xiaomi.finddevice";
        SUPPPORT_CLOUD_DIM = FeatureParser.getBoolean("support_cloud_dim", false);
        SUPPORT_UNION_POWER_CORE = SystemProperties.getBoolean("persist.sys.unionpower.enable", false);
        BACKGROUND_BLUR_SUPPORTED = SystemProperties.getBoolean("persist.sys.background_blur_supported", false);
        BACKGROUND_BLUR_STATUS_DEFAULT = SystemProperties.getBoolean("persist.sys.background_blur_status_default", false);
    }

    /* loaded from: classes.dex */
    private final class WallpaperDeathMonitor implements IBinder.DeathRecipient {
        final int mDisplayId;
        final IBinder mIBinder;

        WallpaperDeathMonitor(IBinder binder, int displayId) {
            this.mDisplayId = displayId;
            this.mIBinder = binder;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            WindowManagerServiceImpl.this.mWmService.removeWindowToken(this.mIBinder, this.mDisplayId);
            this.mIBinder.unlinkToDeath(this, 0);
        }
    }

    public static WindowManagerServiceImpl getInstance() {
        return (WindowManagerServiceImpl) WindowManagerServiceStub.get();
    }

    private void registerBootCompletedReceiver() {
        this.mWallpaperCallback = new IWallpaperManagerCallback.Stub() { // from class: com.android.server.wm.WindowManagerServiceImpl.2
            public void onWallpaperChanged() throws RemoteException {
                WindowManagerServiceImpl.this.mWmService.mH.removeMessages(106);
                WindowManagerServiceImpl.this.mWmService.mH.sendEmptyMessage(106);
            }

            public void onWallpaperColorsChanged(WallpaperColors colors, int which, int userId) throws RemoteException {
                WindowManagerServiceImpl.this.mWmService.mH.removeMessages(106);
                WindowManagerServiceImpl.this.mWmService.mH.sendEmptyMessage(106);
            }
        };
        this.mBootCompletedReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.WindowManagerServiceImpl.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                WindowManagerServiceImpl.this.mWmService.mH.sendEmptyMessage(107);
            }
        };
        IntentFilter bootCompletedFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBootCompletedReceiver, bootCompletedFilter);
    }

    public void initAfterBoot() {
        if (this.mBlurWallpaperBmp == null) {
            updateBlurWallpaperBmp();
        }
    }

    public Bitmap getBlurWallpaperBmp() {
        Bitmap bitmap = this.mBlurWallpaperBmp;
        if (bitmap == null) {
            this.mWmService.mH.removeMessages(106);
            this.mWmService.mH.sendEmptyMessage(106);
            Slog.d(TAG, "mBlurWallpaperBmp is null, wait to update");
            return null;
        }
        if (bitmap != null) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return null;
    }

    public void updateBlurWallpaperBmp() {
        WallpaperManagerInternal wpMgr = (WallpaperManagerInternal) LocalServices.getService(WallpaperManagerInternal.class);
        if (wpMgr == null) {
            Slog.w(TAG, "WallpaperManagerInternal is null");
            return;
        }
        ParcelFileDescriptor fd = null;
        try {
            try {
                fd = wpMgr.getBlurWallpaper(this.mWallpaperCallback);
                if (fd == null) {
                    Slog.w(TAG, "getWallpaper, fd is null");
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    this.mBlurWallpaperBmp = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
                    if (BoundsCompatControllerStub.newInstance().isDebugEnable()) {
                        Slog.i(TAG, "decodeFileDescriptor, mBlurWallpaperBmp = " + this.mBlurWallpaperBmp);
                    }
                    updateAppearance();
                }
            } catch (Exception e) {
                Slog.w(TAG, "getWallpaper wrong", e);
            }
        } finally {
            IOUtils.closeQuietly((Closeable) null);
        }
    }

    public boolean executeShellCommand(PrintWriter pw, String[] args, int opti, String opt) {
        String[] str;
        if (opti < args.length) {
            str = new String[args.length - opti];
        } else {
            str = new String[0];
        }
        System.arraycopy(args, opti, str, 0, args.length - opti);
        pw.println("WindowManagerServiceImpl:executeShellCommand opti = " + opti + " opt = " + opt);
        try {
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return ActivityTaskManagerServiceStub.get().executeShellCommand(opt, str, pw);
    }

    public void init(WindowManagerService wms, Context context) {
        this.mContext = context;
        this.mWmService = wms;
        this.mAppOps = wms.mAppOps;
        this.mAtmService = this.mWmService.mAtmService;
        this.mGlobalLock = this.mWmService.mGlobalLock;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mSupportSwitchResolutionFeature = isSupportSwitchResoluton();
        checkDDICSupportAndInitPhysicalSize();
        registerBootCompletedReceiver();
        AppOpsManager.OnOpChangedListener listener = new WindowOpChangedListener(this.mWmService, context);
        this.mAppOps.startWatchingMode(10041, (String) null, listener);
        this.mPowerKeeperPolicy = PowerKeeperPolicy.getInstance();
        this.mBlackList = Arrays.asList(this.mContext.getResources().getStringArray(285409450));
        this.mSmartPowerServiceInternal = (SmartPowerServiceInternal) LocalServices.getService(SmartPowerServiceInternal.class);
    }

    public void adjustWindowParams(WindowManager.LayoutParams attrs, String packageName, int uid) {
        if (attrs == null) {
            return;
        }
        if (uid != 1000 && ((attrs.flags & 524288) != 0 || (attrs.flags & 4194304) != 0)) {
            ApplicationInfo appInfo = null;
            try {
                appInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(uid));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            int appUid = appInfo == null ? uid : appInfo.uid;
            int mode = this.mAppOps.noteOpNoThrow(10020, appUid, packageName, (String) null, "WindowManagerServiceImpl#adjustWindowParams");
            if (this.mSecurityInternal == null) {
                this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
            }
            SecurityManagerInternal securityManagerInternal = this.mSecurityInternal;
            if (securityManagerInternal != null) {
                securityManagerInternal.recordAppBehaviorAsync(36, packageName, 1L, String.valueOf(attrs.getTitle()));
            }
            if (mode != 0) {
                attrs.flags &= -524289;
                attrs.flags &= -4194305;
                Slog.i(TAG, "MIUILOG- Show when locked PermissionDenied pkg : " + packageName + " uid : " + appUid);
            }
        }
        adjustFindDeviceAttrs(uid, attrs, packageName);
        adjustGoogleAssistantAttrs(attrs, packageName);
        if ((attrs.flags & 4096) != 0 && attrs.type <= 99) {
            attrs.flags &= -4097;
        }
    }

    private void adjustGoogleAssistantAttrs(WindowManager.LayoutParams attrs, String packageName) {
        if (!TextUtils.equals("com.google.android.googlequicksearchbox", packageName)) {
            return;
        }
        attrs.extraFlags |= 32768;
    }

    /* loaded from: classes.dex */
    private static class WindowOpChangedListener implements AppOpsManager.OnOpChangedListener {
        private static final int SCREEN_SHARE_PROTECTION_OPEN = 1;
        private final Context mContext;
        private final WindowManagerService mWmService;

        public WindowOpChangedListener(WindowManagerService mWmService, Context context) {
            this.mWmService = mWmService;
            this.mContext = context;
        }

        @Override // android.app.AppOpsManager.OnOpChangedListener
        public void onOpChanged(String op, final String packageName) {
            boolean isScreenShareProtection = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_share_protection_on", 0, -2) == 1;
            if (isScreenShareProtection && AppOpsManager.opToPublicName(10041).equals(op)) {
                try {
                    ApplicationInfo appInfo = this.mWmService.mContext.getPackageManager().getApplicationInfo(packageName, 0);
                    final int newMode = this.mWmService.mAppOps.noteOpNoThrow(10041, appInfo.uid, packageName);
                    synchronized (this.mWmService.mGlobalLock) {
                        this.mWmService.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$WindowOpChangedListener$$ExternalSyntheticLambda0
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                WindowManagerServiceImpl.WindowOpChangedListener.lambda$onOpChanged$0(packageName, newMode, (WindowState) obj);
                            }
                        }, false);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onOpChanged$0(String packageName, int newMode, WindowState w) {
            if (TextUtils.equals(packageName, w.getOwningPackage()) && w.mWinAnimator != null && w.mWinAnimator.mSurfaceController != null && w.mWinAnimator.mSurfaceController.mSurfaceControl != null) {
                SurfaceControl sc = w.mWinAnimator.mSurfaceController.mSurfaceControl;
                if (newMode == 1) {
                    sc.setScreenProjection(1);
                } else {
                    sc.setScreenProjection(0);
                }
                Slog.i(WindowManagerServiceImpl.TAG, "MIUILOG- Adjust ShareProjectFlag for:" + packageName + " newMode: " + newMode);
            }
        }
    }

    private void adjustFindDeviceAttrs(int uid, WindowManager.LayoutParams attrs, String packageName) {
        addShowOnFindDeviceKeyguardAttrsIfNecessary(attrs, packageName);
        removeFindDeviceKeyguardFlagsIfNecessary(uid, attrs, packageName);
        killFreeformForFindDeviceKeyguardIfNecessary(uid, attrs, packageName);
    }

    private void killFreeformForFindDeviceKeyguardIfNecessary(int uid, WindowManager.LayoutParams attrs, String packageName) {
        if (packageName != null && attrs != null && FIND_DEVICE.equals(packageName) && isFindDeviceFlagUsePermitted(uid, packageName) && (attrs.extraFlags & 2048) != 0) {
            this.mWmService.mAtmService.mMiuiFreeFormManagerService.freeformKillAll();
        }
    }

    private void addShowOnFindDeviceKeyguardAttrsIfNecessary(WindowManager.LayoutParams attrs, String packageName) {
        if (!TextUtils.equals("com.google.android.dialer", packageName) || Settings.Global.getInt(this.mContext.getContentResolver(), "com.xiaomi.system.devicelock.locked", 0) == 0) {
            return;
        }
        attrs.format = -1;
        attrs.layoutInDisplayCutoutMode = 0;
        attrs.extraFlags |= 4096;
    }

    private void removeFindDeviceKeyguardFlagsIfNecessary(int uid, WindowManager.LayoutParams attrs, String packageName) {
        if ((attrs.extraFlags & 2048) == 0 && (attrs.extraFlags & 4096) == 0) {
            return;
        }
        if (isFindDeviceFlagUsePermitted(uid, packageName) && ((attrs.extraFlags & 2048) == 0 || "com.xiaomi.finddevice".equals(packageName))) {
            return;
        }
        attrs.extraFlags &= -2049;
        attrs.extraFlags &= -4097;
    }

    private boolean isFindDeviceFlagUsePermitted(int uid, String packageName) {
        IPackageManager pm;
        if (TextUtils.isEmpty(packageName) || (pm = AppGlobals.getPackageManager()) == null) {
            return false;
        }
        if (pm.checkSignatures("android", packageName, UserHandle.getUserId(uid)) == 0) {
            return true;
        }
        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0L, UserHandle.getUserId(uid));
        if (ai != null) {
            if ((ai.flags & 1) != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == 255) {
            data.enforceInterface("android.view.IWindowManager");
            return switchResolution(data, reply, flags);
        }
        return false;
    }

    boolean switchResolution(Parcel data, Parcel reply, int flags) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Only system uid can switch resolution");
        }
        int displayId = data.readInt();
        int width = data.readInt();
        int height = data.readInt();
        if (displayId != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            switchResolutionInternal(width, height);
            Binder.restoreCallingIdentity(ident);
            reply.writeNoException();
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private void switchResolutionInternal(int width, int height) {
        synchronized (this.mGlobalLock) {
            if (this.mPendingSwitchResolution) {
                return;
            }
            int density = calcDensityAndUpdateForceDensityIfNeed(width);
            Slog.i(TAG, "start switching resolution, width:" + width + ",height:" + height + ",density:" + density);
            this.mMiuiDisplayWidth = width;
            this.mMiuiDisplayHeight = height;
            this.mMiuiDisplayDensity = density;
            this.mPendingSwitchResolution = true;
            setRoundedCornerOverlaysCanScreenShot(true);
            SystemProperties.set("persist.sys.miui_resolution", this.mMiuiDisplayWidth + "," + this.mMiuiDisplayHeight + "," + this.mMiuiDisplayDensity);
            ActivityTaskManagerService.H h = this.mWmService.mAtmService.mH;
            final ActivityManagerInternal activityManagerInternal = this.mWmService.mAmInternal;
            Objects.requireNonNull(activityManagerInternal);
            h.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    activityManagerInternal.notifyResolutionChanged();
                }
            });
            this.mSmartPowerServiceInternal.onDisplaySwitchResolutionLocked(width, height, density);
            if (this.mDisplayContent == null) {
                this.mDisplayContent = this.mWmService.mRoot.getDisplayContent(0);
            }
            updateScreenResolutionLocked(this.mDisplayContent);
            if (this.mSupportActiveModeSwitch) {
                DisplayModeDirectorStub.getInstance().updateDisplaySize(0, this.mMiuiDisplayWidth, this.mMiuiDisplayHeight);
            }
        }
    }

    private int calcDensityAndUpdateForceDensityIfNeed(int desireWidth) {
        int i;
        int density = getForcedDensity();
        if (!isDensityForced()) {
            return Math.round(((this.mPhysicalDensity * desireWidth) * 1.0f) / this.mPhysicalWidth);
        }
        if (this.mWmService.mSystemBooted && (i = this.mMiuiDisplayWidth) != 0) {
            int density2 = Math.round(((desireWidth * density) * 1.0f) / i);
            this.mDisplayContent.mBaseDisplayDensity = density2;
            Settings.Secure.putStringForUser(this.mWmService.mContext.getContentResolver(), "display_density_forced", density2 + "", UserHandle.getCallingUserId());
            return density2;
        }
        return density;
    }

    private boolean isDensityForced() {
        return getForcedDensity() != 0;
    }

    private void updateScreenResolutionLocked(DisplayContent dc) {
        dc.mIsSizeForced = (dc.mInitialDisplayWidth == this.mMiuiDisplayWidth && dc.mInitialDisplayHeight == this.mMiuiDisplayHeight) ? false : true;
        dc.mIsDensityForced = dc.mInitialDisplayDensity != this.mMiuiDisplayDensity;
        dc.updateBaseDisplayMetrics(this.mMiuiDisplayWidth, this.mMiuiDisplayHeight, this.mMiuiDisplayDensity, dc.mBaseDisplayPhysicalXDpi, dc.mBaseDisplayPhysicalYDpi);
        dc.reconfigureDisplayLocked();
    }

    public void finishSwitchResolution() {
        synchronized (this.mGlobalLock) {
            setRoundedCornerOverlaysCanScreenShot(false);
            if (this.mPendingSwitchResolution) {
                Slog.i(TAG, "finished switching resolution");
                this.mPendingSwitchResolution = false;
            }
        }
    }

    private void setRoundedCornerOverlaysCanScreenShot(final boolean canScreenshot) {
        this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowManagerServiceImpl.this.lambda$setRoundedCornerOverlaysCanScreenShot$0(canScreenshot, (WindowState) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setRoundedCornerOverlaysCanScreenShot$0(boolean canScreenshot, WindowState w) {
        if (!w.mToken.mRoundedCornerOverlay || !w.isVisible() || !w.mWinAnimator.hasSurface()) {
            return;
        }
        SurfaceControl clientSurfaceControl = w.getClientViewRootSurface();
        setSurfaceCanScreenShot(clientSurfaceControl, canScreenshot);
    }

    private void setSurfaceCanScreenShot(SurfaceControl clientSurfaceControl, boolean canScreenshot) {
        if (clientSurfaceControl == null) {
            return;
        }
        clientSurfaceControl.setSkipScreenshot(!canScreenshot);
        this.mWmService.requestTraversal();
    }

    public int getForcedDensity() {
        String densityString = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", UserHandle.getCallingUserId());
        if (!TextUtils.isEmpty(densityString)) {
            return Integer.valueOf(densityString).intValue();
        }
        return 0;
    }

    public void initializeMiuiResolutionLocked() {
        if (!this.mSupportSwitchResolutionFeature) {
            return;
        }
        this.mDisplayContent = this.mWmService.mRoot.getDisplayContent(0);
        int[] screenResolution = getUserSetResolution();
        int screenWidth = screenResolution != null ? screenResolution[0] : this.mPhysicalWidth;
        int screenHeight = screenResolution != null ? screenResolution[1] : this.mPhysicalHeight;
        switchResolutionInternal(screenWidth, screenHeight);
        this.mPendingSwitchResolution = false;
    }

    private boolean isSupportSwitchResoluton() {
        boolean supportSwitchResolution = false;
        int[] mScreenResolutionsSupported = FeatureParser.getIntArray("screen_resolution_supported");
        if (mScreenResolutionsSupported != null && mScreenResolutionsSupported.length > 1) {
            supportSwitchResolution = true;
        }
        Slog.i(TAG, "isSupportSwitchResoluton:" + supportSwitchResolution);
        return supportSwitchResolution;
    }

    private void checkDDICSupportAndInitPhysicalSize() {
        DisplayInfo defaultDisplayInfo = this.mDisplayManagerInternal.getDisplayInfo(0);
        Display.Mode[] modes = defaultDisplayInfo.supportedModes;
        for (Display.Mode mode : modes) {
            if (this.mPhysicalHeight != 0 && mode.getPhysicalHeight() != this.mPhysicalHeight) {
                this.mSupportActiveModeSwitch = true;
            }
            if (mode.getPhysicalHeight() > this.mPhysicalHeight) {
                this.mPhysicalWidth = mode.getPhysicalWidth();
                this.mPhysicalHeight = mode.getPhysicalHeight();
            }
        }
        this.mPhysicalDensity = SystemProperties.getInt(BUILD_DENSITY_PROP, 560);
        Slog.i(TAG, "init resolution mSupportActiveModeSwitch:" + this.mSupportActiveModeSwitch + " mPhysicalWidth:" + this.mPhysicalWidth + " mPhysicalHeight:" + this.mPhysicalHeight + " mPhysicalDensity:" + this.mPhysicalDensity);
    }

    public int[] getUserSetResolution() {
        int[] screenSizeInfo = new int[3];
        String miuiResolution = SystemProperties.get("persist.sys.miui_resolution", (String) null);
        if (TextUtils.isEmpty(miuiResolution)) {
            return null;
        }
        try {
            screenSizeInfo[0] = Integer.parseInt(miuiResolution.split(",")[0]);
            screenSizeInfo[1] = Integer.parseInt(miuiResolution.split(",")[1]);
            screenSizeInfo[2] = Integer.parseInt(miuiResolution.split(",")[2]);
            return screenSizeInfo;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "getResolutionFromProperty exception:" + e.toString());
            return null;
        }
    }

    public int getDefaultDensity() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent == null) {
            return -1;
        }
        if (this.mSupportSwitchResolutionFeature) {
            int i = this.mPhysicalWidth;
            if (i * this.mMiuiDisplayWidth != 0) {
                return Math.round(((this.mPhysicalDensity * r2) * 1.0f) / i);
            }
        }
        return displayContent.mInitialDisplayDensity;
    }

    public boolean isPendingSwitchResolution() {
        return this.mPendingSwitchResolution;
    }

    public boolean isSupportSetActiveModeSwitchResolution() {
        return this.mSupportActiveModeSwitch;
    }

    public ScreenCapture.ScreenshotHardwareBuffer getResolutionSwitchShotBuffer(boolean isDisplayRotation, Rect cropBounds) {
        if (!this.mPendingSwitchResolution) {
            return null;
        }
        long[] physicalDisplayIds = DisplayControl.getPhysicalDisplayIds();
        IBinder displayToken = DisplayControl.getPhysicalDisplayToken(physicalDisplayIds[0]);
        ScreenCapture.DisplayCaptureArgs displayCaptureArgs = new ScreenCapture.DisplayCaptureArgs.Builder(displayToken).setSourceCrop(cropBounds).setCaptureSecureLayers(true).setAllowProtected(true).setHintForSeamlessTransition(isDisplayRotation).build();
        ScreenCapture.ScreenshotHardwareBuffer screenshotBuffer = ScreenCapture.captureDisplay(displayCaptureArgs);
        return screenshotBuffer;
    }

    public void clearForcedDisplaySize(DisplayContent displayContent) {
        int[] screenResolution = getUserSetResolution();
        if (screenResolution != null) {
            int width = screenResolution[0];
            int height = screenResolution[1];
            displayContent.setForcedSize(width, height);
            return;
        }
        displayContent.setForcedSize(displayContent.mInitialDisplayWidth, displayContent.mInitialDisplayHeight);
    }

    private DisplayContent getDefaultDisplayContent() {
        if (this.mDisplayContent == null) {
            this.mDisplayContent = this.mWmService.mRoot.getDisplayContent(0);
        }
        return this.mDisplayContent;
    }

    public boolean isTopLayoutFullScreen() {
        if (getDefaultDisplayContent() != null) {
            DisplayPolicy displayPolicy = this.mDisplayContent.getDisplayPolicy();
            return displayPolicy.isTopLayoutFullscreen();
        }
        return false;
    }

    public static List<String> getProjectionBlackList() {
        if (mProjectionBlackList.size() == 0) {
            mProjectionBlackList.add("StatusBar");
            mProjectionBlackList.add("Splash Screen com.android.incallui");
            mProjectionBlackList.add("com.android.incallui/com.android.incallui.InCallActivity");
            mProjectionBlackList.add("FloatAssistantView");
            mProjectionBlackList.add("MiuiFreeformBorderView");
            mProjectionBlackList.add("SnapshotStartingWindow for");
            mProjectionBlackList.add("ScreenshotThumbnail");
            mProjectionBlackList.add("com.milink.ui.activity.ScreeningConsoleWindow");
            mProjectionBlackList.add("FloatNotificationPanel");
            mProjectionBlackList.add("com.tencent.mobileqq/com.tencent.av.ui.AVActivity");
            mProjectionBlackList.add("com.tencent.mobileqq/com.tencent.av.ui.AVLoadingDialogActivity");
            mProjectionBlackList.add("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity");
            mProjectionBlackList.add("com.tencent.mobileqq/.FloatingWindow");
            mProjectionBlackList.add("Splash Screen com.tencent.mm");
            mProjectionBlackList.add("com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity");
            mProjectionBlackList.add("com.tencent.mm/.FloatingWindow");
            mProjectionBlackList.add("com.whatsapp/com.whatsapp.voipcalling.VoipActivityV2");
            mProjectionBlackList.add("com.google.android.dialer/com.android.incallui.InCallActivity");
            mProjectionBlackList.add("com.google.android.dialer/.FloatingWindow");
            mProjectionBlackList.add("com.miui.yellowpage/com.miui.yellowpage.activity.MarkNumberActivity");
            mProjectionBlackList.add("com.miui.securitycenter/.FloatingWindow");
            mProjectionBlackList.add("com.milink.service.ui.PrivateWindow");
            mProjectionBlackList.add("com.milink.ui.activity.NFCLoadingActivity");
            mProjectionBlackList.add("Freeform-OverLayView");
            mProjectionBlackList.add("Freeform-HotSpotView");
            mProjectionBlackList.add("Freeform-TipView");
        }
        return mProjectionBlackList;
    }

    public static void setProjectionBlackList(List<String> blacklist) {
        mProjectionBlackList = blacklist;
    }

    public static boolean getLastFrame(String name) {
        if (name.contains("Splash Screen com.android.incallui") || name.contains("com.android.incallui/com.android.incallui.InCallActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVLoadingDialogActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity") || name.contains("Splash Screen com.tencent.mm") || name.contains("com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity") || name.contains("com.google.android.dialer/com.android.incallui.InCallActivity") || name.contains("com.whatsapp/com.whatsapp.voipcalling.VoipActivityV2")) {
            return true;
        }
        return false;
    }

    public static void setAlertWindowTitle(WindowManager.LayoutParams attrs) {
        if (!WindowManager.LayoutParams.isSystemAlertWindowType(attrs.type)) {
            return;
        }
        if (QQ.equals(attrs.packageName)) {
            attrs.setTitle(QQ_FLOATING);
        }
        if (MM.equals(attrs.packageName)) {
            attrs.setTitle(MM_FLOATING);
        }
        if (GOOGLE.equals(attrs.packageName)) {
            attrs.setTitle(GOOGLE_FLOATING);
        }
        if (SECURITY.equals(attrs.packageName)) {
            attrs.setTitle(SECURITY_FLOATING);
        }
    }

    public boolean isCtsModeEnabled() {
        return this.IS_CTS_MODE;
    }

    public boolean isSplitMode() {
        return this.mSplitMode;
    }

    public void setSplittable(boolean splittable) {
        this.mSplitMode = splittable;
    }

    public int enableWmsDebugConfig(WindowManagerShellCommand shellcmd, PrintWriter pw) {
        String cmd = shellcmd.getNextArgRequired();
        if (!"enable-text".equals(cmd) && !"disable-text".equals(cmd)) {
            pw.println("Error: wrong args , must be enable-text or disable-text");
            return -1;
        }
        boolean enable = "enable-text".equals(cmd);
        while (true) {
            String config = shellcmd.getNextArg();
            if (config != null) {
                enableWmsDebugConfig(config, enable);
            } else {
                return 0;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void enableWmsDebugConfig(String config, boolean enable) {
        char c;
        Slog.d(TAG, "enableWMSDebugConfig, config=:" + config + ", enable=:" + enable);
        switch (config.hashCode()) {
            case -2091566946:
                if (config.equals("DEBUG_VISIBILITY")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1682081203:
                if (config.equals("DEBUG_WALLPAPER_LIGHT")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1610158464:
                if (config.equals("SHOW_LIGHT_TRANSACTIONS")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case -1411240286:
                if (config.equals("DEBUG_WINDOW_TRACE")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -1326138211:
                if (config.equals("DEBUG_ANIM")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1326045248:
                if (config.equals("DEBUG_DRAG")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -1149250858:
                if (config.equals("DEBUG_WALLPAPER")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -663252277:
                if (config.equals("DEBUG_TASK_POSITIONING")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -154379534:
                if (config.equals("DEBUG_SCREENSHOT")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 64921139:
                if (config.equals("DEBUG")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 654509718:
                if (config.equals("DEBUG_DISPLAY")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 663952660:
                if (config.equals("SHOW_VERBOSE_TRANSACTIONS")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 833576886:
                if (config.equals("DEBUG_ROOT_TASK")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 937928650:
                if (config.equals("DEBUG_CONFIGURATION")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1201042717:
                if (config.equals("DEBUG_TASK_MOVEMENT")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1269506447:
                if (config.equals("DEBUG_LAYOUT_REPEATS")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 1278188518:
                if (config.equals("DEBUG_STARTING_WINDOW_VERBOSE")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1298095333:
                if (config.equals("SHOW_STACK_CRAWLS")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 1477990771:
                if (config.equals("DEBUG_WINDOW_CROP")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 1489852622:
                if (config.equals("DEBUG_LAYERS")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1489862326:
                if (config.equals("DEBUG_LAYOUT")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1846783646:
                if (config.equals("DEBUG_INPUT")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1853284313:
                if (config.equals("DEBUG_POWER")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case 2026833314:
                if (config.equals("DEBUG_INPUT_METHOD")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 2105417809:
                if (config.equals("DEBUG_UNKNOWN_APP_VISIBILITY")) {
                    c = 24;
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
                WindowManagerDebugConfig.DEBUG = enable;
                return;
            case 1:
                WindowManagerDebugConfig.DEBUG_ANIM = enable;
                return;
            case 2:
                WindowManagerDebugConfig.DEBUG_LAYOUT = enable;
                return;
            case 3:
                WindowManagerDebugConfig.DEBUG_LAYERS = enable;
                return;
            case 4:
                WindowManagerDebugConfig.DEBUG_INPUT = enable;
                return;
            case 5:
                WindowManagerDebugConfig.DEBUG_INPUT_METHOD = enable;
                return;
            case 6:
                WindowManagerDebugConfig.DEBUG_VISIBILITY = enable;
                return;
            case 7:
                WindowManagerDebugConfig.DEBUG_CONFIGURATION = enable;
                return;
            case '\b':
                WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE = enable;
                return;
            case '\t':
                WindowManagerDebugConfig.DEBUG_WALLPAPER = enable;
                return;
            case '\n':
                WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT = enable;
                return;
            case 11:
                WindowManagerDebugConfig.DEBUG_DRAG = enable;
                return;
            case '\f':
                WindowManagerDebugConfig.DEBUG_SCREENSHOT = enable;
                return;
            case '\r':
                WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS = enable;
                return;
            case 14:
                WindowManagerDebugConfig.DEBUG_WINDOW_TRACE = enable;
                return;
            case 15:
                WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT = enable;
                return;
            case 16:
                WindowManagerDebugConfig.DEBUG_TASK_POSITIONING = enable;
                return;
            case 17:
                WindowManagerDebugConfig.DEBUG_ROOT_TASK = enable;
                return;
            case 18:
                WindowManagerDebugConfig.DEBUG_DISPLAY = enable;
                return;
            case 19:
                WindowManagerDebugConfig.DEBUG_POWER = enable;
                return;
            case 20:
                WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS = enable;
                return;
            case 21:
                WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS = enable;
                return;
            case 22:
                WindowManagerDebugConfig.SHOW_STACK_CRAWLS = enable;
                return;
            case 23:
                WindowManagerDebugConfig.DEBUG_WINDOW_CROP = enable;
                return;
            case 24:
                WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY = enable;
                return;
            default:
                return;
        }
    }

    public void setCloudDimControllerList(String DimCloudConfig) {
        String[] DimConfigArray = DimCloudConfig.split(StorageRestrictedPathManager.SPLIT_MULTI_PATH);
        this.mDimUserTimeoutMap.clear();
        this.mDimNeedAssistMap.clear();
        for (String str : DimConfigArray) {
            String[] DimConfigTotal = str.split(",");
            this.mDimUserTimeoutMap.put(DimConfigTotal[0], Integer.valueOf(Integer.parseInt(DimConfigTotal[1])));
            this.mDimNeedAssistMap.put(DimConfigTotal[0], Boolean.valueOf(Boolean.parseBoolean(DimConfigTotal[2])));
        }
    }

    public int getUserActivityTime(String name) {
        return this.mDimUserTimeoutMap.get(name).intValue();
    }

    public boolean isAdjustScreenOff(CharSequence tag) {
        if (!SUPPPORT_CLOUD_DIM) {
            return false;
        }
        String targetWindowName = tag.toString();
        return !this.mDimUserTimeoutMap.isEmpty() && this.mDimUserTimeoutMap.containsKey(targetWindowName);
    }

    public boolean isAssistResson(CharSequence tag) {
        String targetWindowName = tag.toString();
        if (!this.mDimNeedAssistMap.isEmpty() && this.mDimNeedAssistMap.containsKey(targetWindowName) && this.mDimNeedAssistMap.get(targetWindowName).booleanValue()) {
            return true;
        }
        return false;
    }

    public boolean isCameraOpen() {
        return this.mOpeningCameraID.size() > 0;
    }

    public void linkWallpaperWindowTokenDeathMonitor(IBinder binder, int displayId) {
        try {
            binder.linkToDeath(new WallpaperDeathMonitor(binder, displayId), 0);
        } catch (RemoteException e) {
        }
    }

    public void addSecureChangedListener(IWindowSecureChangeListener listener) {
        synchronized (this.mGlobalLock) {
            if (listener != null) {
                if (!this.mSecureChangeListeners.contains(listener)) {
                    this.mSecureChangeListeners.add(listener);
                }
            }
        }
    }

    public void removeSecureChangedListener(IWindowSecureChangeListener listener) {
        synchronized (this.mGlobalLock) {
            if (listener != null) {
                if (this.mSecureChangeListeners.contains(listener)) {
                    this.mSecureChangeListeners.remove(listener);
                }
            }
        }
    }

    public void onSecureChangedListener(WindowState win, boolean ignoreActivityState) {
        if (win != null && win.mAttrs != null) {
            boolean isResumeOrStart = true;
            boolean hasSecure = (win.mAttrs.flags & 8192) != 0;
            if (win.mActivityRecord == null || (!win.mActivityRecord.isState(ActivityRecord.State.RESUMED) && !win.mActivityRecord.isState(ActivityRecord.State.STARTED))) {
                isResumeOrStart = false;
            }
            if (hasSecure != this.mLastWindowHasSecure) {
                if (ignoreActivityState || isResumeOrStart) {
                    if ((topRunningActivityChange(win) || win.getWindowType() == 2017 || win.getWindowType() == 2040) && win.getWindowType() != 3) {
                        this.mLastWindowHasSecure = hasSecure;
                        Slog.d(TAG, "onSecureChanged, current win = " + win + ", hasSecure = " + hasSecure);
                        synchronized (this.mGlobalLock) {
                            if (this.mSecureChangeListeners.isEmpty()) {
                                return;
                            }
                            Iterator<IWindowSecureChangeListener> it = this.mSecureChangeListeners.iterator();
                            while (it.hasNext()) {
                                IWindowSecureChangeListener listener = it.next();
                                try {
                                    listener.onSecureChangeCallback(win.getWindowTag().toString(), hasSecure);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean topRunningActivityChange(WindowState win) {
        ActivityRecord topRunningActivity = getDefaultDisplayContent().topRunningActivity();
        if (topRunningActivity == null) {
            return false;
        }
        int windowingMode = topRunningActivity.getWindowingMode();
        String flattenToString = topRunningActivity.mActivityComponent.flattenToString();
        return (win.getWindowTag().toString().equals(flattenToString) || isKeyGuardShowing()) && windowingMode != 5;
    }

    public void registerSettingsObserver(Context context, WindowManagerService.SettingsObserver settingsObserver) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(this.mDarkModeEnable, false, settingsObserver, -1);
        resolver.registerContentObserver(this.mDarkModeContrastEnable, false, settingsObserver, -1);
    }

    public boolean onSettingsObserverChange(boolean selfChange, Uri uri) {
        if (this.mDarkModeEnable.equals(uri) || this.mDarkModeContrastEnable.equals(uri) || this.mContrastAlphaUri.equals(uri)) {
            Slog.d(TAG, "updateContrast : " + uri);
            return true;
        }
        return false;
    }

    public void updateContrastAlpha(final boolean darkmode) {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl.5
            @Override // java.lang.Runnable
            public void run() {
                float alpha = Settings.System.getFloat(WindowManagerServiceImpl.this.mContext.getContentResolver(), "contrast_alpha", MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X);
                if (alpha >= 0.5f) {
                    alpha = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                }
                boolean isContrastEnabled = WindowManagerServiceImpl.this.isDarkModeContrastEnable();
                Slog.i(WindowManagerServiceImpl.TAG, "updateContrastOverlay, darkmode: " + darkmode + " isContrastEnabled: " + isContrastEnabled + " alpha: " + alpha);
                synchronized (WindowManagerServiceImpl.this.mWmService.mWindowMap) {
                    WindowManagerServiceImpl.this.mWmService.openSurfaceTransaction();
                    try {
                        if (darkmode && isContrastEnabled) {
                            if (WindowManagerServiceImpl.this.mMiuiContrastOverlay == null) {
                                WindowManagerServiceImpl.this.mMiuiContrastOverlay = MiuiContrastOverlayStub.getInstance();
                                DisplayContent displayContent = WindowManagerServiceImpl.this.mWmService.getDefaultDisplayContentLocked();
                                WindowManagerServiceImpl.this.mMiuiContrastOverlay.init(displayContent, displayContent.mRealDisplayMetrics, WindowManagerServiceImpl.this.mContext);
                            }
                            WindowManagerServiceImpl.this.mMiuiContrastOverlay.showContrastOverlay(alpha);
                        } else {
                            if (WindowManagerServiceImpl.this.mMiuiContrastOverlay != null) {
                                Slog.i(WindowManagerServiceImpl.TAG, " hideContrastOverlay ");
                                WindowManagerServiceImpl.this.mMiuiContrastOverlay.hideContrastOverlay();
                            }
                            WindowManagerServiceImpl.this.mMiuiContrastOverlay = null;
                        }
                    } finally {
                        WindowManagerServiceImpl.this.mWmService.closeSurfaceTransaction("MiuiContrastOverlay");
                    }
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDarkModeContrastEnable() {
        boolean z = false;
        if ((this.mContext.getResources().getConfiguration().uiMode & 48) == 32 && MiuiSettings.System.getBoolean(this.mContext.getContentResolver(), "dark_mode_contrast_enable", false)) {
            z = true;
        }
        boolean isContrastEnable = z;
        return isContrastEnable;
    }

    public void registerUiModeAnimFinishedCallback(IWindowAnimationFinishedCallback callback) {
        this.mUiModeAnimFinishedCallback = callback;
    }

    public void onAnimationFinished() {
        try {
            IWindowAnimationFinishedCallback iWindowAnimationFinishedCallback = this.mUiModeAnimFinishedCallback;
            if (iWindowAnimationFinishedCallback != null) {
                iWindowAnimationFinishedCallback.onWindowAnimFinished();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Call mUiModeAnimFinishedCallback.onWindowAnimFinished error " + e);
        }
    }

    public void positionContrastSurface(int defaultDw, int defaultDh) {
    }

    public void updateScreenShareProjectFlag() {
        this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerServiceImpl.this.lambda$updateScreenShareProjectFlag$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateScreenShareProjectFlag$2() {
        synchronized (this.mWmService.mGlobalLock) {
            this.mWmService.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WindowManagerServiceImpl.this.lambda$updateScreenShareProjectFlag$1((WindowState) obj);
                }
            }, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateScreenShareProjectFlag$1(WindowState w) {
        SurfaceControl surfaceControl;
        if (w != null && w.mWinAnimator != null && w.mWinAnimator.mSurfaceController != null && (surfaceControl = w.mWinAnimator.mSurfaceController.mSurfaceControl) != null) {
            int flags = getScreenShareProjectAndPrivateCastFlag(w.mWinAnimator);
            surfaceControl.setScreenProjection(flags);
        }
    }

    public int getScreenShareProjectAndPrivateCastFlag(WindowStateAnimator winAnimator) {
        ArrayList<String> list;
        int applyFlag = 0;
        IMiuiScreenProjectionStub imp = IMiuiScreenProjectionStub.getInstance();
        boolean isScreenShareProtection = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_share_protection_on", 0, -2) == 1;
        int screenProjectionOnOrOff = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), imp.getMiuiInScreeningSettingsKey(), 0, -2);
        int screenProjectionPrivacy = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), imp.getMiuiPrivacyOnSettingsKey(), 0, -2);
        if (winAnimator != null && winAnimator.mWin != null && winAnimator.mWin.getAttrs() != null && winAnimator.mWin.getAttrs().getTitle() != null) {
            String name = winAnimator.mWin.getAttrs().getTitle().toString();
            if (screenProjectionOnOrOff > 0 && screenProjectionPrivacy > 0 && (list = imp.getProjectionBlackList()) != null) {
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    String blackTitle = it.next();
                    if (name.contains(blackTitle)) {
                        applyFlag |= imp.getExtraScreenProjectFlag();
                    } else if (winAnimator.mWin.mIsImWindow && winAnimator.mWin.getName() != null && winAnimator.mWin.getName().contains("PopupWindow") && winAnimator.mWin.mViewVisibility == 0) {
                        applyFlag |= imp.getExtraScreenProjectFlag();
                    }
                }
            }
            int mode = 0;
            String packageName = null;
            if (winAnimator.mWin.mActivityRecord != null && winAnimator.mWin.mActivityRecord.info != null) {
                int uid = winAnimator.mWin.mActivityRecord.info.applicationInfo.uid;
                packageName = winAnimator.mWin.mActivityRecord.info.applicationInfo.packageName;
                mode = this.mAppOps.checkOpNoThrow(10041, uid, packageName);
            }
            if (isScreenShareProtection) {
                if (mode == 1 && packageName != null) {
                    applyFlag |= 1;
                }
                ArrayList<String> screenShareProjectBlacklist = imp.getScreenShareProjectBlackList();
                if (screenShareProjectBlacklist == null) {
                    return applyFlag;
                }
                Iterator<String> it2 = screenShareProjectBlacklist.iterator();
                while (it2.hasNext()) {
                    String shareName = it2.next();
                    if (name.contains(shareName)) {
                        applyFlag |= 2;
                    }
                }
            }
        }
        return applyFlag;
    }

    public void notifyTouchFromNative(boolean isTouched) {
    }

    public void notifyTaskFocusChange(ActivityRecord touchedActivity) {
        ComponentName componentName;
        if (this.mPowerKeeperPolicy != null && touchedActivity != null && touchedActivity.intent != null && (componentName = touchedActivity.intent.getComponent()) != null) {
            this.mPowerKeeperPolicy.notifyTaskFocusChange(componentName.getPackageName(), componentName.getClassName());
        }
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (this.mPowerKeeperPolicy == null || motionEvent == null) {
            return;
        }
        if (motionEvent.getAction() == 0) {
            this.mPowerKeeperPolicy.notifyTouchStatus(true);
        } else if (motionEvent.getAction() == 1) {
            this.mPowerKeeperPolicy.notifyTouchStatus(false);
        }
    }

    public boolean isNotifyTouchEnable() {
        return SUPPORT_UNION_POWER_CORE;
    }

    public void notifyFloatWindowScene(String packageName, int windowType, boolean status) {
    }

    public boolean isKeyGuardShowing() {
        return this.mAtmService.mKeyguardController.isKeyguardShowing(0);
    }

    public void notifySystemBrightnessChange() {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerServiceImpl.this.lambda$notifySystemBrightnessChange$4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifySystemBrightnessChange$4() {
        ArrayList<Task> visibleTasks;
        TaskDisplayArea defaultTaskDisplayArea = this.mWmService.mRoot.getDefaultTaskDisplayArea();
        synchronized (this.mGlobalLock) {
            visibleTasks = defaultTaskDisplayArea.getVisibleTasks();
        }
        visibleTasks.forEach(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowManagerServiceImpl.this.lambda$notifySystemBrightnessChange$3((Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$notifySystemBrightnessChange$3(Task task) {
        notifySystemBrightnessChange(task.getTopVisibleAppMainWindow());
    }

    private void notifySystemBrightnessChange(WindowState w) {
        if (w == null || w.mAttrs.screenBrightness == -1.0f || this.mTaskIdScreenBrightnessOverrides.contains(w.getTask().mTaskId)) {
            return;
        }
        this.mTaskIdScreenBrightnessOverrides.put(w.getTask().mTaskId, Float.valueOf(w.mAttrs.screenBrightness));
        this.mWmService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(Float.NaN);
    }

    public boolean shouldApplyOverrideBrightness(WindowState w) {
        Task task = w.getTask();
        if (task == null) {
            return true;
        }
        int index = this.mTaskIdScreenBrightnessOverrides.indexOfKey(task.mTaskId);
        if (index < 0) {
            return true;
        }
        if (this.mTaskIdScreenBrightnessOverrides.get(task.mTaskId).floatValue() != w.mAttrs.screenBrightness && !this.mBlackList.contains(w.getOwningPackage())) {
            Slog.i(TAG, "shouldApplyOverrideBrightness: mTaskIdScreenBrightnessOverrides=" + this.mTaskIdScreenBrightnessOverrides + " taskId : " + task.mTaskId);
            this.mTaskIdScreenBrightnessOverrides.delete(task.mTaskId);
            return true;
        }
        return false;
    }

    public void clearOverrideBrightnessRecord(int taskId) {
        int index = this.mTaskIdScreenBrightnessOverrides.indexOfKey(taskId);
        if (index >= 0) {
            Slog.i(TAG, "onTaskRemoved: mTaskIdScreenBrightnessOverrides : " + this.mTaskIdScreenBrightnessOverrides + " taskId : " + taskId);
            this.mTaskIdScreenBrightnessOverrides.delete(taskId);
        }
    }

    public void updateSurfaceParentIfNeed(WindowState ws) {
        WindowManagerService windowManagerService;
        if (ws == null || (windowManagerService = this.mWmService) == null || windowManagerService.mAtmService == null) {
            return;
        }
        Task topRootTask1 = this.mWmService.mAtmService.getTopDisplayFocusedRootTask();
        Task topRootTask2 = this.mWmService.mRoot.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
        Task pairRootTask = null;
        if (ActivityTaskManagerServiceImpl.getInstance().isInSystemSplitScreen(topRootTask1)) {
            pairRootTask = topRootTask1;
        } else if (ActivityTaskManagerServiceImpl.getInstance().isInSystemSplitScreen(topRootTask2)) {
            pairRootTask = topRootTask2;
        }
        if (pairRootTask != null && CLIENT_DIMMER_NAME.equals(ws.getWindowTag()) && ws.getSurfaceControl() != null && ws.getSurfaceControl().isValid() && pairRootTask.getSurfaceControl() != null && pairRootTask.getSurfaceControl().isValid()) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.setRelativeLayer(ws.getSurfaceControl(), pairRootTask.mSurfaceControl, Integer.MAX_VALUE);
            t.apply();
        }
    }

    public void setRunningRecentsAnimation(boolean running) {
        this.mRunningRecentsAnimation = running;
    }

    public void showPinnedTaskIfNeeded(Task task) {
        if (!this.mRunningRecentsAnimation && task.isRootTask() && task.isVisible()) {
            task.getPendingTransaction().show(task.mSurfaceControl);
            Slog.d(TAG, "show pinned task surface: " + task);
        }
    }

    public float getCompatScale(String packageName, int uid) {
        return this.mWmService.mAtmService.mCompatModePackages.getCompatScale(packageName, uid);
    }

    public void onWindowRequestSize(int ownerUid, int width, int height, CharSequence tag) {
        if (UserHandle.getAppId(ownerUid) >= 10000 && width > 0 && height > 0) {
            checkIfUnusualWindowEvent(ownerUid, String.valueOf(tag), width, height);
        }
    }

    public void getBlurWallpaperAppearance(Rect leftBounds, Rect rightBounds, int[] outAppearance) {
        int i;
        int i2;
        if (outAppearance == null || outAppearance.length < 2 || this.mBlurWallpaperBmp == null || leftBounds == null || rightBounds == null) {
            return;
        }
        if (leftBounds.isEmpty()) {
            outAppearance[0] = -1;
        } else if (leftBounds.equals(this.leftStatusBounds) && (i = this.leftAppearance) != -1) {
            outAppearance[0] = i;
        } else {
            int createAppearance = createAppearance(leftBounds);
            this.leftAppearance = createAppearance;
            outAppearance[0] = createAppearance;
            this.leftStatusBounds.set(leftBounds);
            Slog.d(TAG, "getBlurWallpaperAppearance left bounds = " + leftBounds + "appearance = " + outAppearance[0]);
        }
        if (rightBounds.isEmpty()) {
            outAppearance[1] = -1;
            return;
        }
        if (rightBounds.equals(this.rightStatusBounds) && (i2 = this.rightAppearance) != -1) {
            outAppearance[1] = i2;
            return;
        }
        int createAppearance2 = createAppearance(rightBounds);
        this.rightAppearance = createAppearance2;
        outAppearance[1] = createAppearance2;
        this.rightStatusBounds.set(rightBounds);
        Slog.d(TAG, "getBlurWallpaperAppearance right bounds = " + rightBounds + "appearance = " + outAppearance[1]);
    }

    private int createAppearance(Rect bounds) {
        Rect bitmapBounds = new Rect(0, 0, this.mBlurWallpaperBmp.getWidth(), this.mBlurWallpaperBmp.getHeight());
        Rect intersectBounds = new Rect(bounds);
        if (intersectBounds.intersect(bitmapBounds) && !intersectBounds.isEmpty()) {
            try {
                Bitmap intersectBitmap = Bitmap.createBitmap(this.mBlurWallpaperBmp, intersectBounds.left, intersectBounds.top, intersectBounds.width(), intersectBounds.height());
                int avgColor = getAverageColorBitmap(intersectBitmap);
                intersectBitmap.recycle();
                return ColorUtils.calculateLuminance(avgColor) >= 0.5d ? 8 : 0;
            } catch (Exception e) {
                Slog.e(TAG, "create appearance fail " + intersectBounds + " e = " + e);
                return -1;
            }
        }
        return -1;
    }

    private void updateAppearance() {
        WindowState statusBar;
        DisplayContent displayContent = this.mWmService.mRoot.getDisplayContent(0);
        if (displayContent == null || this.mBlurWallpaperBmp == null || (statusBar = displayContent.getDisplayPolicy().getStatusBar()) == null) {
            return;
        }
        Rect statusBarFrame = statusBar.getFrame();
        int width = this.mBlurWallpaperBmp.getWidth();
        int height = Math.min(statusBarFrame.height(), this.mBlurWallpaperBmp.getHeight());
        if (this.leftStatusBounds.isEmpty()) {
            this.leftStatusBounds = new Rect(0, 0, width / 4, height);
        }
        if (this.rightStatusBounds.isEmpty()) {
            this.rightStatusBounds = new Rect((width * 3) / 4, 0, width, height);
        }
        if (!this.leftStatusBounds.isEmpty()) {
            this.leftAppearance = createAppearance(this.leftStatusBounds);
        }
        if (!this.rightStatusBounds.isEmpty()) {
            this.rightAppearance = createAppearance(this.rightStatusBounds);
        }
        Slog.d(TAG, "updateAppearance left" + this.leftStatusBounds + " right" + this.rightStatusBounds + " appearance = " + this.leftAppearance + ", " + this.rightAppearance);
    }

    private int getAverageColorBitmap(Bitmap bitmap) {
        int redColors = 0;
        int greenColors = 0;
        int blueColors = 0;
        int alphaColor = 0;
        int pixelCount = 0;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int c = bitmap.getPixel(x, y);
                pixelCount++;
                redColors += Color.red(c);
                greenColors += Color.green(c);
                blueColors += Color.blue(c);
                alphaColor += Color.alpha(c);
            }
        }
        int y2 = redColors / pixelCount;
        int green = greenColors / pixelCount;
        int blue = blueColors / pixelCount;
        int alpha = alphaColor / pixelCount;
        return Color.argb(alpha, y2, green, blue);
    }

    private void checkIfUnusualWindowEvent(int uid, String tag, int width, int height) {
        int i;
        try {
            String packageName = AppGlobals.getPackageManager().getNameForUid(uid);
            if (TextUtils.isEmpty(packageName) || "com.miui.home".equals(packageName) || AccessController.PACKAGE_SYSTEMUI.equals(packageName) || "android".equals(packageName)) {
                return;
            }
            if (this.mSecurityInternal == null) {
                this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
            }
            SecurityManagerInternal securityManagerInternal = this.mSecurityInternal;
            if (securityManagerInternal != null) {
                long threshold = securityManagerInternal.getThreshold(37);
                if (width <= threshold || height <= threshold) {
                    this.mSecurityInternal.recordAppBehaviorAsync(37, packageName, 1L, tag);
                    return;
                }
                int i2 = this.mPhysicalWidth;
                if (i2 <= 0 || (i = this.mPhysicalHeight) <= 0) {
                    return;
                }
                if (width >= i2 + threshold || height >= i + threshold) {
                    this.mSecurityInternal.recordAppBehaviorAsync(38, packageName, 1L, tag);
                }
            }
        } catch (Exception e) {
        }
    }

    public boolean notAllowCaptureDisplay(RootWindowContainer windowState, int displayId) {
        if (Binder.getCallingUid() == 1000 || ActivityRecordStub.get().isCompatibilityMode()) {
            return false;
        }
        DisplayContent displayContent = windowState.getDisplayContent(displayId);
        if (displayContent == null) {
            Slog.e(TAG, " Screenshot on invalid display " + displayId + "  " + Binder.getCallingUid());
            return false;
        }
        return displayContent.forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda6
            public final boolean apply(Object obj) {
                boolean lambda$notAllowCaptureDisplay$5;
                lambda$notAllowCaptureDisplay$5 = WindowManagerServiceImpl.this.lambda$notAllowCaptureDisplay$5((WindowState) obj);
                return lambda$notAllowCaptureDisplay$5;
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$notAllowCaptureDisplay$5(WindowState w) {
        if (!w.isVisible() || !w.mWinAnimator.hasSurface() || !w.isOnScreen() || (w.mAttrs.flags & 8192) == 0 || (w.getRootTask() != null && this.mWmService.mAtmService.mMiuiFreeFormManagerService.isHideStackFromFullScreen(w.getRootTask().mTaskId))) {
            return false;
        }
        Slog.e(TAG, " Secure window Can't CaptureDisplay   return ! " + w);
        return true;
    }

    public void onSystemReady() {
        MiuiHoverModeInternal miuiHoverModeInternal = (MiuiHoverModeInternal) LocalServices.getService(MiuiHoverModeInternal.class);
        this.miuiHoverModeInternal = miuiHoverModeInternal;
        if (miuiHoverModeInternal != null) {
            miuiHoverModeInternal.onSystemReady(this.mWmService);
        }
        setDefaultBlurConfigIfNeeded(this.mContext);
    }

    private void setDefaultBlurConfigIfNeeded(Context context) {
        if (BACKGROUND_BLUR_SUPPORTED && Settings.Secure.getInt(context.getContentResolver(), "background_blur_enable", -1) == -1 && BACKGROUND_BLUR_STATUS_DEFAULT) {
            Slog.d(TAG, "set default background blur config to true");
            Settings.Secure.putInt(context.getContentResolver(), "background_blur_enable", 1);
        }
    }

    public void onHoverModeRecentAnimStart() {
        MiuiHoverModeInternal miuiHoverModeInternal = this.miuiHoverModeInternal;
        if (miuiHoverModeInternal != null) {
            miuiHoverModeInternal.onHoverModeRecentAnimStart();
        }
    }

    public void onFinishTransition() {
        MiuiHoverModeInternal miuiHoverModeInternal = this.miuiHoverModeInternal;
        if (miuiHoverModeInternal != null) {
            miuiHoverModeInternal.onFinishTransition();
        }
    }

    public void onStopFreezingDisplayLocked() {
        MiuiHoverModeInternal miuiHoverModeInternal = this.miuiHoverModeInternal;
        if (miuiHoverModeInternal != null) {
            miuiHoverModeInternal.onStopFreezingDisplayLocked();
        }
    }

    public void calcHoverAnimatingColor(float[] startColor) {
        MiuiHoverModeInternal miuiHoverModeInternal = this.miuiHoverModeInternal;
        if (miuiHoverModeInternal != null) {
            miuiHoverModeInternal.calcHoverAnimatingColor(startColor);
        }
    }

    public void onScreenRotationAnimationEnd() {
        MiuiHoverModeInternal miuiHoverModeInternal = this.miuiHoverModeInternal;
        if (miuiHoverModeInternal != null) {
            miuiHoverModeInternal.onScreenRotationAnimationEnd();
        }
    }

    public boolean mayChangeToMiuiSecurityInputMethod() {
        InputTarget imeInputTarget;
        WindowState windowState;
        ActivityRecord activityRecord;
        DisplayContent defaultDisplay = this.mWmService.mRoot.getDefaultDisplay();
        if (defaultDisplay != null && (imeInputTarget = defaultDisplay.getImeInputTarget()) != null && (windowState = imeInputTarget.getWindowState()) != null && (activityRecord = windowState.getActivityRecord()) != null) {
            Iterator<String> it = this.mMiuiSecurityImeWhiteList.iterator();
            while (it.hasNext()) {
                String windowName = it.next();
                if (activityRecord.toString().contains(windowName)) {
                    Slog.i(TAG, "windowName: " + windowName + " activityRecord: " + activityRecord);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mayChangeToMiuiSecurityInputMethod(IBinder token) {
        WindowState windowState;
        ActivityRecord activityRecord;
        WindowManagerInternal service = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        if (service == null) {
            return false;
        }
        synchronized (this.mWmService.mGlobalLock) {
            windowState = (WindowState) this.mWmService.mWindowMap.get(token);
        }
        if (windowState != null && (activityRecord = windowState.getActivityRecord()) != null) {
            Iterator<String> it = this.mMiuiSecurityImeWhiteList.iterator();
            while (it.hasNext()) {
                String windowName = it.next();
                if (activityRecord.toString().contains(windowName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addMiuiPaperContrastOverlay(ArrayList<SurfaceControl> excludeLayersList) {
        SurfaceControl miuiPaperContrastOverlay;
        if (Binder.getCallingUid() != 1000 && (miuiPaperContrastOverlay = MiuiPaperContrastOverlayStub.get().getMiuiPaperSurfaceControl()) != null) {
            this.mMiuiPaperContrastOverlay = miuiPaperContrastOverlay;
            excludeLayersList.add(miuiPaperContrastOverlay);
        }
    }

    public void removeMiuiPaperContrastOverlay(SurfaceControl[] excludeLayers) {
        if (this.mMiuiPaperContrastOverlay == null) {
            return;
        }
        for (int i = 0; i < excludeLayers.length; i++) {
            if (excludeLayers[i] != null && this.mMiuiPaperContrastOverlay.equals(excludeLayers[i])) {
                excludeLayers[i] = null;
                this.mMiuiPaperContrastOverlay = null;
            }
        }
    }

    public void setTransitionReadyState(int syncId, boolean ready) {
        if (ready) {
            if (!this.mTransitionReadyList.contains(Integer.valueOf(syncId))) {
                this.mTransitionReadyList.add(Integer.valueOf(syncId));
                return;
            }
            return;
        }
        this.mTransitionReadyList.remove(Integer.valueOf(syncId));
    }

    public boolean getTransitionReadyState() {
        return !this.mTransitionReadyList.isEmpty();
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0068, code lost:
    
        if (com.android.server.wm.WindowManagerServiceImpl.CARWITH_TRUSTED_NON_DAILY_SIG.equals(r6.toString()) != false) goto L20;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean doesAddCarWithNavigationBarRequireToken(java.lang.String r12) {
        /*
            r11 = this;
            java.lang.String r0 = "WindowManagerService"
            r1 = 0
            java.lang.String r2 = "com.miui.carlink"
            boolean r2 = r2.equals(r12)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            if (r2 == 0) goto L90
            android.content.Context r2 = r11.mContext     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            android.content.pm.PackageManager r2 = r2.getPackageManager()     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            r3 = 64
            android.content.pm.PackageInfo r2 = r2.getPackageInfo(r12, r3)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            android.content.pm.Signature[] r2 = r2.signatures     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            r2 = r2[r1]     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            byte[] r3 = r2.toByteArray()     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            r4 = 0
            java.lang.String r5 = "SHA-256"
            java.security.MessageDigest r5 = java.security.MessageDigest.getInstance(r5)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            r4 = r5
            byte[] r5 = r4.digest(r3)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            int r7 = r5.length     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            int r7 = r7 * 2
            r6.<init>(r7)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            r7 = 0
        L34:
            int r8 = r5.length     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            r9 = 1
            if (r7 >= r8) goto L52
            r8 = r5[r7]     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            r8 = r8 & 255(0xff, float:3.57E-43)
            java.lang.String r8 = java.lang.Integer.toHexString(r8)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            int r10 = r8.length()     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            if (r10 != r9) goto L4b
            r9 = 48
            r6.append(r9)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
        L4b:
            r6.append(r8)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            int r7 = r7 + 1
            goto L34
        L52:
            java.lang.String r7 = "c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8"
            java.lang.String r8 = r6.toString()     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            boolean r7 = r7.equals(r8)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            if (r7 != 0) goto L6c
            java.lang.String r7 = "c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025"
            java.lang.String r8 = r6.toString()     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            boolean r0 = r7.equals(r8)     // Catch: java.security.NoSuchAlgorithmException -> L6d android.content.pm.PackageManager.NameNotFoundException -> Lad
            if (r0 == 0) goto L6b
            goto L6c
        L6b:
            goto L8e
        L6c:
            return r9
        L6d:
            r5 = move-exception
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            r6.<init>()     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.String r7 = "packageName"
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.StringBuilder r6 = r6.append(r12)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.String r7 = "NoSuchAlgorithmException: "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.StringBuilder r6 = r6.append(r5)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.String r6 = r6.toString()     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            android.util.Slog.e(r0, r6)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
        L8e:
            goto Lc4
        L90:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            r2.<init>()     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.String r3 = "packageName:"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.StringBuilder r2 = r2.append(r12)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.String r3 = " is not carlink"
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            java.lang.String r2 = r2.toString()     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            android.util.Slog.e(r0, r2)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> Lad
            return r1
        Lad:
            r2 = move-exception
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "NameNotFoundException"
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r2)
            java.lang.String r3 = r3.toString()
            android.util.Slog.e(r0, r3)
        Lc4:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.WindowManagerServiceImpl.doesAddCarWithNavigationBarRequireToken(java.lang.String):boolean");
    }

    public boolean shouldApplyOverrideBrightnessForPip(WindowState w) {
        Transition transition;
        if (w.mAttrs.screenBrightness < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X || w.mActivityRecord == null || !w.mActivityRecord.isVisibleRequested() || !w.inTransition()) {
            return false;
        }
        if ((!w.mActivityRecord.mWaitForEnteringPinnedMode && !w.inPinnedWindowingMode()) || (transition = w.mTransitionController.getCollectingTransition()) == null || (transition.mType != 10 && transition.mType != 6)) {
            return false;
        }
        Slog.i(TAG, "Override brightness when " + w.mActivityRecord + " is entering PIP.");
        return true;
    }

    public void saveNavigationBarColor(int color) {
        if (color != this.mNavigationColor) {
            this.mNavigationColor = color;
            Slog.i(TAG, "before notify flip navigation bar color change, color = " + color);
            synchronized (this.mGlobalLock) {
                for (int i = this.mNavigationBarColorListenerMap.size() - 1; i >= 0; i--) {
                    IFlipWindowStateListener listener = this.mNavigationBarColorListenerMap.valueAt(i);
                    try {
                        listener.onNavigationColorChange(color);
                    } catch (RemoteException e) {
                        if (e instanceof DeadObjectException) {
                            this.mNavigationBarColorListenerMap.removeAt(i);
                            Slog.d(TAG, "onNavigationColorChange binder died, remove it, key = " + this.mNavigationBarColorListenerMap.keyAt(i));
                        } else {
                            Slog.d(TAG, "onNavigationColorChange fail", e);
                        }
                    }
                }
            }
        }
    }

    public void checkFlipFullScreenChange(ActivityRecord record) {
        boolean activityEnableFullScreen = enableFullScreen(record);
        boolean nowIsFullScreen = activityEnableFullScreen && record.getWindowConfiguration().getBounds().left <= 0;
        Boolean bool = this.mLastIsFullScreen;
        if (bool != null && bool.booleanValue() == nowIsFullScreen) {
            Slog.d(TAG, "checkFlipFullScreenChange is same, nowIsFullScreen = " + nowIsFullScreen);
            return;
        }
        this.mLastIsFullScreen = Boolean.valueOf(nowIsFullScreen);
        Slog.i(TAG, "before notify flip small screen change, is full = " + nowIsFullScreen);
        synchronized (this.mFlipListenerLock) {
            for (int i = this.mNavigationBarColorListenerMap.size() - 1; i >= 0; i--) {
                IFlipWindowStateListener listener = this.mNavigationBarColorListenerMap.valueAt(i);
                try {
                    listener.onIsFullScreenChange(nowIsFullScreen);
                } catch (RemoteException e) {
                    if (e instanceof DeadObjectException) {
                        Slog.d(TAG, "onNavigationColorChange binder died, remove it, key = " + this.mNavigationBarColorListenerMap.keyAt(i));
                        this.mNavigationBarColorListenerMap.removeAt(i);
                    } else {
                        Slog.d(TAG, "onNavigationColorChange fail", e);
                    }
                }
            }
        }
    }

    private boolean enableFullScreen(ActivityRecord record) {
        IPackageManager manager = record.mAtmService.getPackageManager();
        int activityMode = getFlipCompatModeByActivity(manager, record.mActivityComponent);
        if (activityMode != -1) {
            return activityMode == 0;
        }
        int appMode = getFlipCompatModeByApp(manager, record.packageName);
        return appMode == 0;
    }

    private int getFlipCompatModeByApp(IPackageManager manager, String packageName) {
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = manager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
        } catch (Exception e) {
            Slog.e(TAG, "getApplicationInfo fail", e);
        }
        return getFullScreenValue(applicationInfo);
    }

    private int getFlipCompatModeByActivity(IPackageManager manager, ComponentName componentName) {
        ActivityInfo activityInfo = null;
        try {
            activityInfo = manager.getActivityInfo(componentName, 128L, UserHandle.myUserId());
        } catch (Exception e) {
            Slog.e(TAG, "getActivityInfo fail", e);
        }
        return getFullScreenValue(activityInfo);
    }

    private int getFullScreenValue(PackageItemInfo info) {
        if (info != null && info.metaData != null && info.metaData.containsKey("miui.supportFlipFullScreen")) {
            return info.metaData.getInt("miui.supportFlipFullScreen");
        }
        return -1;
    }

    public int getNavigationBarColor() {
        return this.mNavigationColor;
    }

    public void registerFlipWindowStateListener(IFlipWindowStateListener listener) {
        synchronized (this.mFlipListenerLock) {
            this.mNavigationBarColorListenerMap.put(listener.asBinder(), listener);
            Slog.i(TAG, "registerNavigationBarColorListener binder = " + listener.asBinder() + ", size = " + this.mNavigationBarColorListenerMap.size());
        }
    }

    public void removeFlipWindowStateListener(IFlipWindowStateListener listener) {
        synchronized (this.mFlipListenerLock) {
            this.mNavigationBarColorListenerMap.remove(listener.asBinder());
            Slog.i(TAG, "removeNavigationBarColorListener binder = " + listener.asBinder() + ", size = " + this.mNavigationBarColorListenerMap.size());
        }
    }

    public void bindFloatwindowAndTargetTask(final WindowManager.LayoutParams attrs, WindowToken token, RootWindowContainer root) {
        if (attrs.type == 2038 && !CIRCULATE.equals(attrs.getTitle().toString()) && mFloatWindowMirrorWhiteList.contains(attrs.packageName)) {
            Task topFocusedRootTask = this.mAtmService.getTopDisplayFocusedRootTask();
            ActivityRecord topActivity = topFocusedRootTask.getTopResumedActivity();
            if (topActivity != null && (topActivity.getName().contains(LAUNCHER) || topActivity.getName().contains(CIRCULATEACTIVITY))) {
                TaskDisplayArea defaultDisplayArea = root.getDefaultTaskDisplayArea();
                final ArrayList<Task> targetTaskList = new ArrayList<>();
                defaultDisplayArea.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda4
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WindowManagerServiceImpl.lambda$bindFloatwindowAndTargetTask$6(attrs, targetTaskList, (Task) obj);
                    }
                });
                for (int i = 0; i < targetTaskList.size(); i++) {
                    targetTaskList.get(i).floatWindow = token;
                }
                return;
            }
            topFocusedRootTask.floatWindow = token;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$bindFloatwindowAndTargetTask$6(WindowManager.LayoutParams attrs, ArrayList targetTaskList, Task task) {
        String packageName = task.getPackageName();
        if (attrs.packageName.equals(packageName)) {
            targetTaskList.add(task);
        }
    }

    public void trackScreenData(int wakefulness, String details) {
        OneTrackRotationHelper.getInstance().trackScreenData(wakefulness, details);
    }

    public boolean isAbortTransitionInScreenOff(int type, ActivityTaskManagerService atms, Transition transition) {
        DisplayContent dc;
        if (type != 4) {
            return false;
        }
        boolean isVoiceassist = false;
        if (transition != null) {
            ArraySet<WindowContainer> participants = transition.mParticipants;
            int i = 0;
            while (true) {
                if (i >= participants.size()) {
                    break;
                }
                WindowContainer wc = participants.valueAt(i);
                if (wc.asTask() == null || wc.asTask().affinity == null || !wc.asTask().affinity.contains("com.miui.voiceassist.floatactivity")) {
                    i++;
                } else {
                    isVoiceassist = true;
                    break;
                }
            }
        }
        if (atms == null || atms.mRootWindowContainer == null || (dc = atms.mRootWindowContainer.getDisplayContent(0)) == null || dc.getDisplayInfo() == null || dc.getDisplayInfo().state != 1 || !isVoiceassist) {
            return false;
        }
        transition.abort();
        return true;
    }
}
