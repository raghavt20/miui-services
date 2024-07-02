package com.android.server.display;

import android.app.UiModeManager;
import android.cameracovered.MiuiCameraCoveredManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.IntProperty;
import android.util.Slog;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.display.MiuiRampAnimator;
import com.android.server.display.RhythmicEyeCareManager;
import com.android.server.display.expertmode.ExpertData;
import com.android.server.input.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.policy.DisplayTurnoverManager;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowProcessController;
import com.xiaomi.NetworkBoost.NetworkSDK.telephony.NetworkBoostSimCardHelper;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import miui.hardware.display.IDisplayFeatureManager;
import miui.hardware.display.aidl.IDisplayFeatureCallback;
import miui.hardware.display.hidl.IDisplayFeatureCallback;
import miui.os.DeviceFeature;
import miui.util.FeatureParser;
import miui.util.MiuiMultiDisplayTypeInfo;
import vendor.xiaomi.hardware.misys.V1_0.IResultValue;

/* loaded from: classes.dex */
public class DisplayFeatureManagerService extends SystemService {
    private static final String AIDL_SERVICENAME_DEFAULT = "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default";
    private static final int CLASSIC_READING_MODE = 0;
    private static final int FLAG_SET_SCREEN_EFFECT = 0;
    private static final int FLAG_SET_VIEDEO_INFOMATION = 1;
    private static final String HIDL_SERVICENAME_DEFAULT = "vendor.xiaomi.hardware.displayfeature@1.0::IDisplayFeature";
    private static final int MSG_SDR_TO_HDR = 22;
    private static final int MSG_SEND_HBM_STATE = 30;
    private static final int MSG_SEND_MURA_STATE = 40;
    private static final int MSG_SET_COLOR_MODE = 13;
    private static final int MSG_SET_CURRENT_GRAY_VALUE = 4;
    private static final int MSG_SET_DC_PARSE_STATE = 14;
    private static final int MSG_SET_GRAY_VALUE = 3;
    private static final int MSG_SET_PAPER_COLOR_TYPE = 16;
    private static final int MSG_SET_SECONDARY_FRAME_RATE = 15;
    private static final int MSG_SWITCH_DARK_MODE = 10;
    private static final int MSG_UPDATE_COLOR_SCHEME = 2;
    private static final int MSG_UPDATE_DFPS_MODE = 11;
    private static final int MSG_UPDATE_EXPERT_MODE = 12;
    private static final int MSG_UPDATE_HDR_STATE = 20;
    private static final int MSG_UPDATE_PCC_LEVEL = 7;
    private static final int MSG_UPDATE_READING_MODE = 1;
    private static final int MSG_UPDATE_SMART_DFPS_MODE = 17;
    private static final int MSG_UPDATE_TRUETONE = 19;
    private static final int MSG_UPDATE_UNLIMITED_COLOR_LEVEL = 8;
    private static final int MSG_UPDATE_USERCHANGE = 21;
    private static final int MSG_UPDATE_WCG_STATE = 6;
    private static final int NEW_CLASSIC_READING_MODE = 3;
    private static final int PAPER_READING_MODE = 1;
    private static final String PERSISTENT_PROPERTY_DISPLAY_COLOR = "persist.sys.sf.native_mode";
    private static final int RHYTHMIC_READING_MODE = 2;
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE = 31100;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DC_PARSE_STATE = 31036;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DFPS = 31035;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_FPS_VIDEO_INFO = 31116;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_PCC = 31101;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SDR2HDR = 34000;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SECONDARY_FRAME_RATE = 31121;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SET_MODE = 31023;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SMART_DFPS = 31037;
    private static final String TAG = "DisplayFeatureManagerService";
    private static final int TEMP_PAPER_MODE_LEVEL = -1;
    private static final String THREAD_TAG = "DisplayFeatureThread";
    private boolean mAutoAdjustEnable;
    private IBinder.DeathRecipient mBinderDeathHandler;
    private boolean mBootCompleted;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks;
    private int mColorSchemeCTLevel;
    private int mColorSchemeModeType;
    private Context mContext;
    private float mCurrentGrayScale;
    private boolean mDeskTopModeEnabled;
    private DisplayFeatureManagerServiceImpl mDisplayFeatureServiceImpl;
    private DisplayManagerInternal mDisplayManagerInternal;
    private int mDisplayState;
    private boolean mDolbyState;
    private boolean mForceDisableEyeCare;
    private boolean mGameHdrEnabled;
    private float mGrayScale;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private IHwBinder.DeathRecipient mHwBinderDeathHandler;
    private final boolean mIsFoldOrFlip;
    public DisplayFeatureManagerInternal mLocalService;
    private final Object mLock;
    private int mMaxDozeBrightnessInt;
    private int mMinDozeBrightnessInt;
    private int mMinimumBrightnessInt;
    private int mPaperColorType;
    private MiuiRampAnimator mPaperModeAnimator;
    private int mPaperModeMinRate;
    private PowerManager mPowerManager;
    private int mReadingModeCTLevel;
    private boolean mReadingModeEnabled;
    private int mReadingModeType;
    private ContentResolver mResolver;
    private final RhythmicEyeCareManager.RhythmicEyeCareListener mRhythmicEyeCareListener;
    private RhythmicEyeCareManager mRhythmicEyeCareManager;
    private SettingsObserver mSettingsObserver;
    private int mTrueToneModeEnabled;
    private DisplayFeatureManagerWrapper mWrapper;
    private static final boolean IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT = FeatureParser.getBoolean("is_compatible_paper_and_screen_effect", false);
    private static final float PAPER_MODE_MIN_LEVEL = FeatureParser.getFloat("paper_mode_min_level", 1.0f).floatValue();
    private static final boolean SUPPORT_UNLIMITED_COLOR_MODE = MiuiSettings.ScreenEffect.SUPPORT_UNLIMITED_COLOR_MODE;
    private static final boolean SUPPORT_DISPLAY_EXPERT_MODE = ExpertData.SUPPORT_DISPLAY_EXPERT_MODE;
    private static final int SCREEN_DEFAULT_FPS = FeatureParser.getInteger("defaultFps", 0);
    private static final boolean FPS_SWITCH_DEFAULT = SystemProperties.getBoolean("ro.vendor.fps.switch.default", false);
    public static String BRIGHTNESS_THROTTLER_STATUS = "brightness_throttler_status";
    private static final boolean SUPPORT_SET_FEATURE = !FeatureParser.getBoolean("support_screen_effect", false);
    private static final int CONFIG_SERVICENAME_RESOURCEID = Resources.getSystem().getIdentifier("config_displayFeatureHidlServiceName", "string", "android");
    private static final boolean SUPPORT_MULTIPLE_AOD_BRIGHTNESS = SystemProperties.getBoolean("ro.vendor.aod.brightness.cust", false);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public enum DozeBrightnessMode {
        DOZE_TO_NORMAL,
        DOZE_BRIGHTNESS_HBM,
        DOZE_BRIGHTNESS_LBM
    }

    public DisplayFeatureManagerService(Context context) {
        super(context);
        this.mDisplayState = 0;
        this.mGrayScale = Float.NaN;
        this.mCurrentGrayScale = Float.NaN;
        this.mClientDeathCallbacks = new HashMap<>();
        Object obj = new Object();
        this.mLock = obj;
        this.mIsFoldOrFlip = MiuiMultiDisplayTypeInfo.isFlipDevice() || MiuiMultiDisplayTypeInfo.isFoldDeviceInside();
        RhythmicEyeCareManager.RhythmicEyeCareListener rhythmicEyeCareListener = new RhythmicEyeCareManager.RhythmicEyeCareListener() { // from class: com.android.server.display.DisplayFeatureManagerService.1
            @Override // com.android.server.display.RhythmicEyeCareManager.RhythmicEyeCareListener
            public void onRhythmicEyeCareChange(int category, int time) {
                DisplayFeatureManagerService.this.setRhythmicColor(category, time);
            }
        };
        this.mRhythmicEyeCareListener = rhythmicEyeCareListener;
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        HandlerThread handlerThread = new HandlerThread(THREAD_TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new DisplayFeatureHandler(this.mHandlerThread.getLooper());
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mMinimumBrightnessInt = powerManager.getMinimumScreenBrightnessSetting();
        this.mDisplayManagerInternal = (DisplayManagerInternal) getLocalService(DisplayManagerInternal.class);
        RhythmicEyeCareManager rhythmicEyeCareManager = new RhythmicEyeCareManager(context, this.mHandlerThread.getLooper());
        this.mRhythmicEyeCareManager = rhythmicEyeCareManager;
        rhythmicEyeCareManager.setRhythmicEyeCareListener(rhythmicEyeCareListener);
        this.mDisplayFeatureServiceImpl = (DisplayFeatureManagerServiceImpl) DisplayFeatureManagerServiceStub.getInstance();
        initServiceDeathRecipient();
        synchronized (obj) {
            initWrapperLocked();
        }
    }

    public void onStart() {
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        this.mDisplayFeatureServiceImpl.init(localService);
        publishBinderService("displayfeature", new BinderService());
        publishLocalService(DisplayFeatureManagerInternal.class, this.mLocalService);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            if (DeviceFeature.SUPPORT_PAPERMODE_ANIMATION) {
                this.mPaperModeMinRate = this.mContext.getResources().getInteger(285933599);
            }
            this.mSettingsObserver = new SettingsObserver();
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_enabled"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_level"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_optimize_mode"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_color_level"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_texture_color_type"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_texture_level"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_auto_adjust"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_mode_type"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_game_mode"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("miui_dkt_mode"), false, this.mSettingsObserver, -1);
            this.mContext.registerReceiver(new UserSwitchReceiver(), new IntentFilter("android.intent.action.USER_SWITCHED"));
            if (MiuiSettings.ScreenEffect.SUPPORT_TRUETONE_MODE) {
                this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_true_tone"), false, this.mSettingsObserver, -1);
            }
            if (DeviceFeature.SUPPORT_PAPERMODE_ANIMATION) {
                IntProperty<DisplayFeatureManagerService> paperMode = new IntProperty<DisplayFeatureManagerService>("papermode") { // from class: com.android.server.display.DisplayFeatureManagerService.2
                    @Override // android.util.IntProperty
                    public void setValue(DisplayFeatureManagerService object, int value) {
                        if (DisplayFeatureManagerService.this.mDisplayState != 1 && (value > 0 || DisplayFeatureManagerService.this.mPaperModeAnimator.isAnimating())) {
                            object.setScreenEffect(3, value);
                        } else if (DisplayFeatureManagerService.IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT && DisplayFeatureManagerService.this.mDisplayState != 1 && !DisplayFeatureManagerService.this.mReadingModeEnabled) {
                            object.setScreenEffect(3, 0);
                        }
                    }

                    @Override // android.util.Property
                    public Integer get(DisplayFeatureManagerService object) {
                        return 0;
                    }
                };
                MiuiRampAnimator miuiRampAnimator = new MiuiRampAnimator(this, paperMode);
                this.mPaperModeAnimator = miuiRampAnimator;
                miuiRampAnimator.setListener(new PaperModeAnimatListener());
            }
            if (DeviceFeature.SUPPORT_DISPLAYFEATURE_CALLBACK) {
                if (DeviceFeature.SUPPORT_DISPLAYFEATURE_HIDL) {
                    registerCallback(new IDisplayFeatureCallback.Stub() { // from class: com.android.server.display.DisplayFeatureManagerService.3
                        public void displayfeatureInfoChanged(int caseId, Object... params) throws RemoteException {
                            DisplayFeatureManagerService.this.handleDisplayFeatureInfoChanged(caseId, params);
                        }
                    });
                } else {
                    registerCallback(new IDisplayFeatureCallback.Stub() { // from class: com.android.server.display.DisplayFeatureManagerService.4
                        public void displayfeatureInfoChanged(int caseId, Object... params) throws RemoteException {
                            DisplayFeatureManagerService.this.handleDisplayFeatureInfoChanged(caseId, params);
                        }
                    });
                }
            }
            this.mHandler.obtainMessage(21).sendToTarget();
            loadDozeBrightnessThreshold();
            return;
        }
        if (phase == 1000) {
            this.mBootCompleted = true;
            if (!FPS_SWITCH_DEFAULT) {
                notifySFDfpsMode(getScreenDpiMode(), 17);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDisplayFeatureInfoChanged(int caseId, Object... params) {
        if (params.length > 0) {
            if (caseId == 10000) {
                this.mHandler.obtainMessage(6, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 10035) {
                this.mHandler.obtainMessage(11, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 10037) {
                this.mHandler.obtainMessage(17, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 0) {
                this.mHandler.obtainMessage(3, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 30000) {
                this.mHandler.obtainMessage(13, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 30001) {
                this.mHandler.obtainMessage(22, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 40000) {
                this.mHandler.obtainMessage(14, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 50000) {
                this.mHandler.obtainMessage(20, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 10036) {
                this.mHandler.obtainMessage(15, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 60000) {
                this.mHandler.obtainMessage(30, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 70000) {
                this.mHandler.obtainMessage(40, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
            if (caseId == 95000) {
                this.mHandler.removeMessages(4);
                this.mHandler.obtainMessage(4, ((Integer) params[0]).intValue(), 0).sendToTarget();
            }
        }
        if (caseId == 20000 && params.length >= 4) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = params[1];
            args.arg2 = params[2];
            args.arg3 = params[3];
            this.mHandler.obtainMessage(7, ((Integer) params[0]).intValue(), 0, args).sendToTarget();
        }
    }

    private int getScreenDpiMode() {
        return SystemProperties.getInt("persist.vendor.dfps.level", SCREEN_DEFAULT_FPS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenEffectAll(boolean userChange) {
        setScreenEffectColor(userChange);
        if (MiuiSettings.ScreenEffect.SUPPORT_TRUETONE_MODE) {
            this.mHandler.obtainMessage(19).sendToTarget();
        }
        this.mHandler.obtainMessage(1, true).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenEffectColor(boolean userChange) {
        boolean z = SUPPORT_UNLIMITED_COLOR_MODE;
        if (z) {
            this.mHandler.obtainMessage(8, Boolean.valueOf(userChange)).sendToTarget();
        }
        if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT || z) {
            this.mHandler.obtainMessage(2, Boolean.valueOf(userChange)).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenEffect(int mode, int value) {
        setScreenEffectInternal(mode, value, 255);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenEffectInternal(int mode, int value, int cookie) {
        int[] displays = getEffectedDisplayIndex();
        for (int i = 0; i < displays.length; i++) {
            setDisplayFeature(i, mode, value, cookie);
        }
    }

    private void setDisplayFeature(int displayId, int mode, int value, int cookie) {
        synchronized (this.mLock) {
            Slog.d(TAG, "setScreenEffect displayId=" + displayId + " mode=" + mode + " value=" + value + " cookie=" + cookie + " from pid=" + Binder.getCallingPid());
            if (this.mWrapper == null) {
                initWrapperLocked();
            }
            DisplayFeatureManagerWrapper displayFeatureManagerWrapper = this.mWrapper;
            if (displayFeatureManagerWrapper != null) {
                displayFeatureManagerWrapper.setFeature(displayId, mode, value, cookie);
            }
        }
    }

    private int[] getEffectedDisplayIndex() {
        int[] result = {0};
        if (this.mIsFoldOrFlip) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeInterfaceToken("android.view.android.hardware.display.IDisplayManager");
                    IBinder b = ServiceManager.getService("display");
                    b.transact(DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY, data, reply, 0);
                    int count = reply.readInt();
                    result = new int[count];
                    reply.readIntArray(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return result;
    }

    private int getEffectedDisplayIndex(long physicalDisplayId) {
        int result = 0;
        if (this.mIsFoldOrFlip) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeInterfaceToken("android.view.android.hardware.display.IDisplayManager");
                    data.writeLong(physicalDisplayId);
                    IBinder b = ServiceManager.getService("display");
                    b.transact(DisplayTurnoverManager.CODE_TURN_OFF_SUB_DISPLAY, data, reply, 0);
                    result = reply.readInt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                reply.recycle();
                data.recycle();
            }
        }
        return result;
    }

    public void loadDozeBrightnessThreshold() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                data.writeInterfaceToken("android.view.android.hardware.display.IDisplayManager");
                IBinder b = ServiceManager.getService("display");
                b.transact(16777207, data, reply, 0);
                int length = reply.readInt();
                if (length == 2) {
                    float[] result = new float[length];
                    reply.readFloatArray(result);
                    this.mMaxDozeBrightnessInt = BrightnessSynchronizer.brightnessFloatToInt(result[0]);
                    this.mMinDozeBrightnessInt = BrightnessSynchronizer.brightnessFloatToInt(result[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void setDozeBrightness(long physicalDisplayId, int brightness) {
        int value = brightness;
        if (!SUPPORT_MULTIPLE_AOD_BRIGHTNESS) {
            DozeBrightnessMode modeId = DozeBrightnessMode.DOZE_TO_NORMAL;
            if (brightness >= this.mMaxDozeBrightnessInt) {
                modeId = DozeBrightnessMode.DOZE_BRIGHTNESS_HBM;
            } else if (brightness > 0) {
                modeId = DozeBrightnessMode.DOZE_BRIGHTNESS_LBM;
            }
            value = modeId.ordinal();
            Slog.d(TAG, "setDozeBrightness, brightness: " + brightness + ", value: " + value);
        }
        int index = getEffectedDisplayIndex(physicalDisplayId);
        setDisplayFeature(index, 25, value, 255);
    }

    private void initWrapperLocked() {
        try {
            if (!DeviceFeature.SUPPORT_DISPLAYFEATURE_HIDL) {
                Slog.d(TAG, "initProxyLocked aidlServiceName: vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default");
                IBinder b = ServiceManager.getService(AIDL_SERVICENAME_DEFAULT);
                if (b != null) {
                    b.linkToDeath(this.mBinderDeathHandler, 0);
                    this.mWrapper = new DisplayFeatureManagerWrapper(b);
                    return;
                }
                return;
            }
            int i = CONFIG_SERVICENAME_RESOURCEID;
            String hidlServiceName = i == 0 ? HIDL_SERVICENAME_DEFAULT : Resources.getSystem().getString(i);
            Slog.d(TAG, "initProxyLocked CONFIG_SERVICENAME_RESOURCEID: " + i + " hidlServiceName: " + hidlServiceName);
            IHwBinder hb = HwBinder.getService(hidlServiceName, NetworkBoostSimCardHelper.DEFAULT_NULL_IMSI);
            if (hb != null) {
                hb.linkToDeath(this.mHwBinderDeathHandler, 10001L);
                this.mWrapper = new DisplayFeatureManagerWrapper(hb);
            }
        } catch (RemoteException | NoSuchElementException e) {
            Slog.e(TAG, "initProxyLocked failed.", e);
        }
    }

    public void registerCallback(Object callback) {
        synchronized (this.mLock) {
            if (this.mWrapper == null) {
                initWrapperLocked();
            }
            DisplayFeatureManagerWrapper displayFeatureManagerWrapper = this.mWrapper;
            if (displayFeatureManagerWrapper != null) {
                displayFeatureManagerWrapper.registerCallback(0, callback);
            }
        }
    }

    private void initServiceDeathRecipient() {
        if (DeviceFeature.SUPPORT_DISPLAYFEATURE_HIDL) {
            this.mHwBinderDeathHandler = new IHwBinder.DeathRecipient() { // from class: com.android.server.display.DisplayFeatureManagerService.5
                public void serviceDied(long cookie) {
                    synchronized (DisplayFeatureManagerService.this.mLock) {
                        Slog.v(DisplayFeatureManagerService.TAG, "hwbinder service binderDied!");
                        DisplayFeatureManagerService.this.mWrapper = null;
                    }
                }
            };
        } else {
            this.mBinderDeathHandler = new IBinder.DeathRecipient() { // from class: com.android.server.display.DisplayFeatureManagerService.6
                @Override // android.os.IBinder.DeathRecipient
                public void binderDied() {
                    synchronized (DisplayFeatureManagerService.this.mLock) {
                        Slog.v(DisplayFeatureManagerService.TAG, "binder service binderDied!");
                        DisplayFeatureManagerService.this.mWrapper = null;
                    }
                }
            };
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySFWcgState(boolean enable) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeBoolean(enable);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notifySurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySFDfpsMode(int mode, int msg) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(mode);
            try {
                try {
                    if (msg == 17) {
                        flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SMART_DFPS, data, null, 0);
                    } else {
                        flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DFPS, data, null, 0);
                    }
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dfps mode to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySFPccLevel(int level, float red, float green, float blue) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(level);
            data.writeFloat(red);
            data.writeFloat(green);
            data.writeFloat(blue);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_PCC, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notifySurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySFColorMode(int mode) {
        Settings.System.putIntForUser(this.mResolver, "display_color_mode", mode, -2);
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(mode);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SET_MODE, data, null, 0);
                    SystemProperties.set(PERSISTENT_PROPERTY_DISPLAY_COLOR, Integer.toString(mode));
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dfps mode to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setSDR2HDR(int mode) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(mode);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SDR2HDR, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to set sdr2hdr to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySFDCParseState(int state) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(state);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DC_PARSE_STATE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dc parse state to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifySFSecondaryFrameRate(int rateState) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(rateState);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SECONDARY_FRAME_RATE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dc parse state to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    private void notifySFVideoInformation(boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio) {
        IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        if (surfaceFlinger != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeBoolean(bulletChatStatus);
            data.writeFloat(frameRate);
            data.writeInt(width);
            data.writeInt(height);
            data.writeFloat(compressionRatio);
            Slog.d(TAG, "notifySFVideoInformation bulletChatStatus:" + bulletChatStatus + ", frameRate:" + frameRate + ", resolution:" + width + "x" + height + ", compressionRatio:" + compressionRatio);
            try {
                try {
                    surfaceFlinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_FPS_VIDEO_INFO, data, reply, 0);
                } catch (RemoteException | SecurityException | UnsupportedOperationException e) {
                    Slog.e(TAG, "notifySFVideoInformation RemoteException:" + e);
                }
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadSettings() {
        updateReadingModeEnable();
        updateReadingModeType();
        updatePaperColorType();
        resetLocalPaperLevelIfNeed();
        updateAutoAdjustEnable();
        updateColorSchemeModeType();
        updateColorSchemeCTLevel();
        if (MiuiSettings.ScreenEffect.SUPPORT_TRUETONE_MODE) {
            updateTrueToneModeEnable();
        }
        updateDeskTopMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTrueToneModeEnable() {
        this.mTrueToneModeEnabled = Settings.System.getIntForUser(this.mResolver, "screen_true_tone", 0, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePaperColorType() {
        this.mPaperColorType = Settings.System.getIntForUser(this.mResolver, "screen_texture_color_type", 0, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateColorSchemeCTLevel() {
        this.mColorSchemeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_color_level", 2, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateColorSchemeModeType() {
        this.mColorSchemeModeType = Settings.System.getIntForUser(this.mResolver, "screen_optimize_mode", MiuiSettings.ScreenEffect.DEFAULT_SCREEN_OPTIMIZE_MODE, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAutoAdjustEnable() {
        if (!isSupportSmartEyeCare()) {
            return;
        }
        this.mAutoAdjustEnable = Settings.System.getIntForUser(this.mResolver, "screen_auto_adjust", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateReadingModeEnable() {
        this.mReadingModeEnabled = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_enabled", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateReadingModeType() {
        this.mReadingModeType = Settings.System.getIntForUser(this.mResolver, "screen_mode_type", 0, -2);
        updateReadingModeCTLevel();
    }

    private void updateClassicReadingModeCTLevel() {
        this.mReadingModeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_level", MiuiSettings.ScreenEffect.DEFAULT_PAPER_MODE_LEVEL, -2);
    }

    private void updatePaperReadingModeCTLevel() {
        this.mReadingModeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_paper_texture_level", (int) MiuiSettings.ScreenEffect.DEFAULT_TEXTURE_MODE_LEVEL, -2);
    }

    private void updateRhythmicModeCTLevel() {
        this.mReadingModeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_rhythmic_mode_level", MiuiSettings.ScreenEffect.DEFAULT_RHYTHMIC_EYECARE_LEVEL, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateReadingModeCTLevel() {
        switch (this.mReadingModeType) {
            case 0:
                updateClassicReadingModeCTLevel();
                return;
            case 1:
            case 3:
                updatePaperReadingModeCTLevel();
                return;
            case 2:
                updateRhythmicModeCTLevel();
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateVideoInformationIfNeeded(int pid, boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio, IBinder token) {
        if (!isForegroundApp(pid)) {
            return;
        }
        setDeathCallbackLocked(token, 1, true);
        notifySFVideoInformation(bulletChatStatus, frameRate, width, height, compressionRatio);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeskTopMode() {
        boolean z = Settings.System.getIntForUser(this.mContext.getContentResolver(), "miui_dkt_mode", 0, -2) != 0;
        this.mDeskTopModeEnabled = z;
        this.mRhythmicEyeCareManager.updateDeskTopMode(z);
    }

    private boolean isForegroundApp(int pid) {
        ActivityTaskManagerInternal mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        WindowProcessController wpc = mAtmInternal != null ? mAtmInternal.getTopApp() : null;
        int topAppPid = wpc != null ? wpc.getPid() : 0;
        if (pid != topAppPid) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDeathCallbackLocked(IBinder token, int flag, boolean register) {
        if (register) {
            registerDeathCallbackLocked(token, flag);
        } else {
            unregisterDeathCallbackLocked(token);
        }
    }

    protected void registerDeathCallbackLocked(IBinder token, int flag) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.d(TAG, "Client token " + token + " has already registered.");
            return;
        }
        synchronized (this.mLock) {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token, flag));
        }
    }

    protected void unregisterDeathCallbackLocked(IBinder token) {
        if (token != null) {
            synchronized (this.mLock) {
                ClientDeathCallback deathCallback = this.mClientDeathCallbacks.remove(token);
                if (deathCallback != null) {
                    token.unlinkToDeath(deathCallback, 0);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private int mFlag;
        private IBinder mToken;

        public ClientDeathCallback(IBinder token, int flag) {
            this.mToken = token;
            this.mFlag = flag;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(DisplayFeatureManagerService.TAG, "binderDied: flag: " + this.mFlag);
            synchronized (DisplayFeatureManagerService.this.mClientDeathCallbacks) {
                DisplayFeatureManagerService.this.mClientDeathCallbacks.remove(this.mToken);
            }
            this.mToken.unlinkToDeath(this, 0);
            DisplayFeatureManagerService.this.doDieLocked(this.mFlag);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doDieLocked(int flag) {
        if (flag == 1) {
            notifySFVideoInformation(false, 0.0f, 0, 0, 0.0f);
        }
    }

    private void resetLocalPaperLevelIfNeed() {
        if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT && this.mReadingModeCTLevel < PAPER_MODE_MIN_LEVEL) {
            int tempValue = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_level", -1, -2);
            if (tempValue != -1) {
                int i = MiuiSettings.ScreenEffect.DEFAULT_PAPER_MODE_LEVEL;
                this.mReadingModeCTLevel = i;
                Settings.System.putIntForUser(this.mResolver, "screen_paper_mode_level", i, -2);
            }
        }
    }

    /* loaded from: classes.dex */
    private class SettingsObserver extends ContentObserver {
        public SettingsObserver() {
            super(DisplayFeatureManagerService.this.mHandler);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -1671532134:
                    if (lastPathSegment.equals("screen_paper_texture_level")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -1618391570:
                    if (lastPathSegment.equals("screen_paper_mode_level")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1457819203:
                    if (lastPathSegment.equals("screen_game_mode")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -1111615120:
                    if (lastPathSegment.equals("screen_true_tone")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case -185483197:
                    if (lastPathSegment.equals("screen_mode_type")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 632817389:
                    if (lastPathSegment.equals("screen_texture_color_type")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 671593557:
                    if (lastPathSegment.equals("screen_color_level")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1878999244:
                    if (lastPathSegment.equals("screen_auto_adjust")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1962624818:
                    if (lastPathSegment.equals("screen_optimize_mode")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 1967212548:
                    if (lastPathSegment.equals("miui_dkt_mode")) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case 2119453483:
                    if (lastPathSegment.equals("screen_paper_mode_enabled")) {
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
                    DisplayFeatureManagerService.this.updateReadingModeEnable();
                    DisplayFeatureManagerService.this.handleReadingModeChange(false);
                    return;
                case 1:
                case 2:
                    if (DisplayFeatureManagerService.this.mReadingModeEnabled) {
                        if (DisplayFeatureManagerService.this.mReadingModeType == 1 || DisplayFeatureManagerService.this.mReadingModeType == 3 || DisplayFeatureManagerService.this.mReadingModeType == 0) {
                            DisplayFeatureManagerService.this.updateReadingModeCTLevel();
                            DisplayFeatureManagerService.this.updatePaperMode(true, true);
                            return;
                        }
                        return;
                    }
                    return;
                case 3:
                    DisplayFeatureManagerService.this.updateColorSchemeModeType();
                    DisplayFeatureManagerService.this.handleScreenSchemeChange(false);
                    return;
                case 4:
                    DisplayFeatureManagerService.this.updateColorSchemeCTLevel();
                    if (DisplayFeatureManagerService.SUPPORT_UNLIMITED_COLOR_MODE) {
                        DisplayFeatureManagerService.this.handleUnlimitedColorLevelChange(false);
                        return;
                    } else {
                        DisplayFeatureManagerService.this.handleScreenSchemeChange(false);
                        return;
                    }
                case 5:
                    DisplayFeatureManagerService.this.handleGameModeChange();
                    return;
                case 6:
                    DisplayFeatureManagerService.this.updateAutoAdjustEnable();
                    if (DisplayFeatureManagerService.this.isSupportSmartEyeCare() && DisplayFeatureManagerService.this.mReadingModeEnabled) {
                        DisplayFeatureManagerService.this.handleAutoAdjustChange();
                        return;
                    }
                    return;
                case 7:
                    DisplayFeatureManagerService.this.updatePaperColorType();
                    if (DisplayFeatureManagerService.this.mReadingModeEnabled) {
                        if (DisplayFeatureManagerService.this.mReadingModeType == 1 || DisplayFeatureManagerService.this.mReadingModeType == 3) {
                            DisplayFeatureManagerService displayFeatureManagerService = DisplayFeatureManagerService.this;
                            displayFeatureManagerService.setPaperColors(displayFeatureManagerService.mReadingModeType);
                            return;
                        }
                        return;
                    }
                    return;
                case '\b':
                    DisplayFeatureManagerService.this.updateReadingModeType();
                    if (DisplayFeatureManagerService.this.mReadingModeEnabled) {
                        if (!DisplayFeatureManagerService.this.mAutoAdjustEnable) {
                            DisplayFeatureManagerService.this.updatePaperMode(true, false);
                        }
                        if (DisplayFeatureManagerService.this.mReadingModeType == 2) {
                            DisplayFeatureManagerService.this.setPaperColors(0);
                            DisplayFeatureManagerService.this.mRhythmicEyeCareManager.setModeEnable(true);
                            return;
                        } else {
                            DisplayFeatureManagerService.this.mRhythmicEyeCareManager.setModeEnable(false);
                            DisplayFeatureManagerService displayFeatureManagerService2 = DisplayFeatureManagerService.this;
                            displayFeatureManagerService2.setPaperColors(displayFeatureManagerService2.mReadingModeType);
                            return;
                        }
                    }
                    return;
                case '\t':
                    DisplayFeatureManagerService.this.updateTrueToneModeEnable();
                    DisplayFeatureManagerService.this.handleTrueToneModeChange();
                    return;
                case '\n':
                    DisplayFeatureManagerService.this.updateDeskTopMode();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleReadingModeChange(boolean immediate) {
        if (this.mReadingModeEnabled) {
            int i = this.mReadingModeType;
            if (i == 2) {
                setPaperColors(0);
                this.mRhythmicEyeCareManager.setModeEnable(true);
            } else {
                setPaperColors(i);
            }
            if (this.mAutoAdjustEnable) {
                handleAutoAdjustChange();
                return;
            } else {
                updatePaperMode(true, immediate);
                return;
            }
        }
        if (this.mReadingModeType == 2) {
            this.mRhythmicEyeCareManager.setModeEnable(false);
        }
        setPaperColors(0);
        updatePaperMode(false, this.mAutoAdjustEnable);
    }

    /* loaded from: classes.dex */
    private class DisplayFeatureHandler extends Handler {
        public DisplayFeatureHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 1:
                    DisplayFeatureManagerService.this.handleReadingModeChange(((Boolean) message.obj).booleanValue());
                    return;
                case 2:
                    DisplayFeatureManagerService.this.handleScreenSchemeChange(((Boolean) message.obj).booleanValue());
                    return;
                case 3:
                    DisplayFeatureManagerService.this.mGrayScale = (message.arg1 * 1.0f) / 255.0f;
                    DisplayFeatureManagerService.this.notifyGrayScaleChanged();
                    return;
                case 4:
                    DisplayFeatureManagerService.this.mCurrentGrayScale = message.arg1 * 1.0f;
                    DisplayFeatureManagerService.this.notifyCurrentGrayScaleChanged();
                    return;
                case 5:
                case 9:
                case 18:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case IResultValue.MISYS_ESPIPE /* 29 */:
                case IResultValue.MISYS_EMLINK /* 31 */:
                case 32:
                case UsbKeyboardUtil.COMMAND_TOUCH_PAD_ENABLE /* 33 */:
                case 34:
                case UsbKeyboardUtil.COMMAND_BACK_LIGHT_ENABLE /* 35 */:
                case UsbKeyboardUtil.COMMAND_TOUCH_PAD_SENSITIVITY /* 36 */:
                case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                case 38:
                case 39:
                default:
                    return;
                case 6:
                    DisplayFeatureManagerService.this.notifySFWcgState(message.arg1 == 1);
                    return;
                case 7:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    DisplayFeatureManagerService.this.notifySFPccLevel(message.arg1, ((Float) someArgs.arg1).floatValue(), ((Float) someArgs.arg2).floatValue(), ((Float) someArgs.arg3).floatValue());
                    someArgs.recycle();
                    return;
                case 8:
                    DisplayFeatureManagerService.this.handleUnlimitedColorLevelChange(((Boolean) message.obj).booleanValue());
                    return;
                case 10:
                    DisplayFeatureManagerService displayFeatureManagerService = DisplayFeatureManagerService.this;
                    Context context = displayFeatureManagerService.mContext;
                    DisplayFeatureManagerService displayFeatureManagerService2 = DisplayFeatureManagerService.this;
                    displayFeatureManagerService.setDarkModeEnable(context, displayFeatureManagerService2.isDarkModeEnable(displayFeatureManagerService2.mContext));
                    return;
                case 11:
                case 17:
                    DisplayFeatureManagerService.this.notifySFDfpsMode(message.arg1, message.what);
                    return;
                case 12:
                    DisplayFeatureManagerService.this.setExpertScreenMode();
                    return;
                case 13:
                    DisplayFeatureManagerService.this.notifySFColorMode(message.arg1);
                    return;
                case 14:
                    DisplayFeatureManagerService.this.notifySFDCParseState(message.arg1);
                    return;
                case 15:
                    DisplayFeatureManagerService.this.notifySFSecondaryFrameRate(message.arg1);
                    return;
                case 16:
                    DisplayFeatureManagerService displayFeatureManagerService3 = DisplayFeatureManagerService.this;
                    displayFeatureManagerService3.setPaperColors(displayFeatureManagerService3.mReadingModeEnabled ? DisplayFeatureManagerService.this.mReadingModeType : 0);
                    return;
                case 19:
                    DisplayFeatureManagerService.this.handleTrueToneModeChange();
                    return;
                case 20:
                    DisplayFeatureManagerService.this.mDolbyState = message.arg1 == 2;
                    Settings.System.putIntForUser(DisplayFeatureManagerService.this.mContext.getContentResolver(), DisplayFeatureManagerService.BRIGHTNESS_THROTTLER_STATUS, DisplayFeatureManagerService.this.mDolbyState ? 1 : 0, -2);
                    DisplayFeatureManagerService.this.notifyHdrStateChanged();
                    return;
                case 21:
                    Settings.System.putInt(DisplayFeatureManagerService.this.mResolver, "screen_game_mode", 0);
                    DisplayFeatureManagerService.this.loadSettings();
                    DisplayFeatureManagerService.this.setScreenEffectAll(true);
                    return;
                case 22:
                    DisplayFeatureManagerService.this.setSDR2HDR(message.arg1);
                    return;
                case 30:
                    DisplayFeatureManagerService.this.sendHbmState(message.arg1);
                    return;
                case 40:
                    DisplayFeatureManagerService.this.sendMuraState(message.arg1);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendHbmState(int type) {
        MiuiCameraCoveredManager.hbmCoveredAnimation(type);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMuraState(int type) {
        MiuiCameraCoveredManager.cupMuraCoveredAnimation(type);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePaperMode(boolean enabled, boolean immediate) {
        if (this.mForceDisableEyeCare) {
            return;
        }
        setScreenEyeCare(enabled, immediate);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAutoAdjustChange() {
        if (this.mAutoAdjustEnable) {
            setScreenEffect(3, 256);
        } else {
            updateReadingModeCTLevel();
            updatePaperMode(true, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSupportSmartEyeCare() {
        return FeatureParser.getBoolean("support_smart_eyecare", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPaperColors(int type) {
        boolean isPaperColorType = true;
        if (type != 1 && type != 3) {
            isPaperColorType = false;
        }
        setScreenEffect(31, isPaperColorType ? this.mPaperColorType : 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setRhythmicColor(int category, int time) {
        boolean modeEnable = this.mReadingModeEnabled && this.mReadingModeType == 2 && !this.mForceDisableEyeCare;
        setRhythmicScreenEffect(54, modeEnable ? 1 : 0, category, time);
    }

    public void setRhythmicScreenEffect(int screenModeType, int screenModeValue, int category, int time) {
        int cookie = (category << 16) | time;
        setScreenEffectInternal(screenModeType, screenModeValue, cookie);
    }

    private void setScreenEyeCare(boolean enabled, boolean immediate) {
        if (DeviceFeature.SUPPORT_PAPERMODE_ANIMATION && this.mPaperModeAnimator != null && (immediate || this.mDisplayState != 1)) {
            int rate = immediate ? 0 : Math.max((this.mReadingModeCTLevel * 2) / 3, this.mPaperModeMinRate);
            int targetLevel = enabled ? this.mReadingModeCTLevel : 0;
            if (this.mPaperModeAnimator.animateTo(targetLevel, rate)) {
                return;
            }
        }
        if (enabled) {
            setScreenEffect(3, this.mReadingModeCTLevel);
        } else if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT) {
            setScreenEffect(3, 0);
        } else {
            handleScreenSchemeChange(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenSchemeChange(boolean userChange) {
        if (!IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT && ((this.mReadingModeEnabled || this.mGameHdrEnabled) && !this.mForceDisableEyeCare)) {
            return;
        }
        int value = this.mColorSchemeCTLevel;
        int mode = 0;
        int i = this.mColorSchemeModeType;
        if (i == 2) {
            mode = 1;
        } else if (i == 3) {
            mode = 2;
        } else if (i == 4) {
            if (!this.mBootCompleted || userChange) {
                setExpertScreenMode();
                return;
            }
            return;
        }
        setScreenEffect(mode, value);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGameModeChange() {
        int gameMode = Settings.System.getIntForUser(this.mResolver, "screen_game_mode", 0, -2);
        int gameHdrLevel = Settings.System.getIntForUser(this.mResolver, "game_hdr_level", 0, -2);
        boolean gameHdrEnabled = (gameMode & 2) != 0;
        boolean forceDisableEyecare = (gameMode & 1) != 0;
        if (this.mForceDisableEyeCare != forceDisableEyecare) {
            this.mForceDisableEyeCare = forceDisableEyecare;
        }
        if (this.mGameHdrEnabled != gameHdrEnabled) {
            this.mGameHdrEnabled = gameHdrEnabled;
            if (!gameHdrEnabled) {
                setScreenEffect(19, 0);
                handleScreenSchemeChange(false);
            } else if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT || ((forceDisableEyecare && this.mReadingModeEnabled) || !this.mReadingModeEnabled)) {
                if (forceDisableEyecare && this.mReadingModeEnabled) {
                    if (this.mReadingModeType == 2) {
                        this.mRhythmicEyeCareManager.setModeEnable(false);
                    } else {
                        setPaperColors(0);
                    }
                    setScreenEyeCare(false, true);
                }
                setScreenEffect(19, gameHdrLevel);
            }
        }
        if ((IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT || !gameHdrEnabled) && this.mReadingModeEnabled) {
            if (this.mAutoAdjustEnable) {
                if (!forceDisableEyecare) {
                    handleAutoAdjustChange();
                    return;
                } else {
                    setScreenEyeCare(false, true);
                    return;
                }
            }
            int i = this.mReadingModeType;
            if (i == 2) {
                this.mRhythmicEyeCareManager.setModeEnable(!forceDisableEyecare);
            } else {
                if (forceDisableEyecare) {
                    i = 0;
                }
                setPaperColors(i);
            }
            setScreenEyeCare(forceDisableEyecare ? false : true, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnlimitedColorLevelChange(boolean userChange) {
        if (this.mColorSchemeModeType != 4 || !this.mBootCompleted || userChange) {
            setScreenEffect(23, this.mColorSchemeCTLevel);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTrueToneModeChange() {
        setScreenEffect(32, this.mTrueToneModeEnabled);
    }

    /* loaded from: classes.dex */
    private class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!DisplayFeatureManagerService.this.mBootCompleted) {
                return;
            }
            DisplayFeatureManagerService.this.mHandler.obtainMessage(21).sendToTarget();
            DisplayFeatureManagerService.this.mHandler.removeMessages(10);
            DisplayFeatureManagerService.this.mHandler.obtainMessage(10).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDarkModeEnable(Context ctx, boolean enable) {
        UiModeManager manager = (UiModeManager) ctx.getSystemService(UiModeManager.class);
        if (manager == null) {
            return;
        }
        manager.setNightMode(enable ? 2 : 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDarkModeEnable(Context ctx) {
        return 2 == Settings.Secure.getIntForUser(ctx.getContentResolver(), "ui_night_mode", 1, 0);
    }

    /* loaded from: classes.dex */
    class LocalService extends DisplayFeatureManagerInternal {
        LocalService() {
        }

        @Override // com.android.server.display.DisplayFeatureManagerInternal
        public void updateScreenEffect(int state) {
            int oldState = DisplayFeatureManagerService.this.mDisplayState;
            DisplayFeatureManagerService.this.mDisplayState = state;
            if (!DeviceFeature.PERSIST_SCREEN_EFFECT) {
                if (oldState == 1 && state != oldState) {
                    DisplayFeatureManagerService.this.setScreenEffectColor(false);
                }
                DisplayFeatureManagerService.this.mRhythmicEyeCareManager.notifyScreenStateChanged(state);
            }
        }

        @Override // com.android.server.display.DisplayFeatureManagerInternal
        public void updateDozeBrightness(long physicalDisplayId, int brightness) {
            DisplayFeatureManagerService.this.setDozeBrightness(physicalDisplayId, brightness);
        }

        @Override // com.android.server.display.DisplayFeatureManagerInternal
        public void updateBCBCState(int state) {
            DisplayFeatureManagerService.this.setScreenEffect(18, state);
        }

        @Override // com.android.server.display.DisplayFeatureManagerInternal
        public void setVideoInformation(int pid, boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio, IBinder token) {
            DisplayFeatureManagerService.this.updateVideoInformationIfNeeded(pid, bulletChatStatus, frameRate, width, height, compressionRatio, token);
        }

        @Override // com.android.server.display.DisplayFeatureManagerInternal
        public void updateRhythmicAppCategoryList(List<String> imageAppList, List<String> readAppList) {
            DisplayFeatureManagerService.this.mRhythmicEyeCareManager.updateRhythmicAppCategoryList(imageAppList, readAppList);
        }

        @Override // com.android.server.display.DisplayFeatureManagerInternal
        public void updateScreenGrayscaleState(int state) {
            DisplayFeatureManagerService.this.setScreenEffect(56, state);
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends IDisplayFeatureManager.Stub {
        private BinderService() {
        }

        public void setScreenEffect(int mode, int value, int cookie, IBinder token) {
            if (token != null) {
                DisplayFeatureManagerService.this.setDeathCallbackLocked(token, 0, true);
            }
            DisplayFeatureManagerService.this.setScreenEffectInternal(mode, value, cookie);
        }
    }

    /* loaded from: classes.dex */
    class PaperModeAnimatListener implements MiuiRampAnimator.Listener {
        PaperModeAnimatListener() {
        }

        @Override // com.android.server.display.MiuiRampAnimator.Listener
        public void onAnimationEnd() {
            if (DisplayFeatureManagerService.this.mDisplayState != 1 && !DisplayFeatureManagerService.IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT) {
                DisplayFeatureManagerService.this.handleScreenSchemeChange(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyGrayScaleChanged() {
        Bundle bundle = new Bundle();
        bundle.putFloat("gray_scale", this.mGrayScale);
        this.mDisplayManagerInternal.notifyDisplayManager(0, 1, bundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyCurrentGrayScaleChanged() {
        Bundle bundle = new Bundle();
        bundle.putFloat("current_gray_scale", this.mCurrentGrayScale);
        this.mDisplayManagerInternal.notifyDisplayManager(0, 5, bundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyHdrStateChanged() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("dolby_version_state", this.mDolbyState);
        this.mDisplayManagerInternal.notifyDisplayManager(0, 3, bundle);
    }

    private ExpertData getExpertData(Context context) {
        ExpertData data = ExpertData.getFromDatabase(context);
        if (data == null) {
            return ExpertData.getDefaultValue();
        }
        return data;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setExpertScreenMode() {
        if (!SUPPORT_DISPLAY_EXPERT_MODE) {
            Slog.w(TAG, "device don't support DISPLAY_EXPERT_MODE");
        }
        ExpertData data = getExpertData(this.mContext);
        if (data == null) {
            return;
        }
        for (int cookie = 0; cookie < 9; cookie++) {
            setScreenEffectInternal(26, data.getByCookie(cookie), cookie);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DisplayFeatureManagerWrapper {
        private static final int AIDL_TRANSACTION_registerCallback = 2;
        private static final int AIDL_TRANSACTION_setFeature = 7;
        private static final int HIDL_TRANSACTION_interfaceDescriptor = 256136003;
        private static final int HIDL_TRANSACTION_registerCallback = 2;
        private static final int HIDL_TRANSACTION_setFeature = 1;
        private static final String HWBINDER_BASE_INTERFACE_DESCRIPTOR = "android.hidl.base@1.0::IBase";
        private static final String HWBINDER_INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.displayfeature@1.0::IDisplayFeature";
        private static final String INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature";
        private String mDescriptor;
        private IHwBinder mHwService;
        private IBinder mService;

        DisplayFeatureManagerWrapper(Object service) {
            if (service instanceof IBinder) {
                IBinder iBinder = (IBinder) service;
                this.mService = iBinder;
                try {
                    this.mDescriptor = iBinder.getInterfaceDescriptor();
                } catch (RemoteException e) {
                }
                if (TextUtils.isEmpty(this.mDescriptor)) {
                    this.mDescriptor = INTERFACE_DESCRIPTOR;
                    return;
                }
                return;
            }
            if (service instanceof IHwBinder) {
                this.mHwService = (IHwBinder) service;
                try {
                    this.mDescriptor = interfaceDescriptor();
                } catch (RemoteException e2) {
                }
                if (TextUtils.isEmpty(this.mDescriptor)) {
                    this.mDescriptor = HWBINDER_INTERFACE_DESCRIPTOR;
                }
            }
        }

        void setFeature(int displayId, int mode, int value, int cookie) {
            if (DeviceFeature.SUPPORT_DISPLAYFEATURE_HIDL) {
                callHwBinderTransact(1, 0, Integer.valueOf(displayId), Integer.valueOf(mode), Integer.valueOf(value), Integer.valueOf(cookie));
            } else {
                callBinderTransact(7, 0, Integer.valueOf(displayId), Integer.valueOf(mode), Integer.valueOf(value), Integer.valueOf(cookie));
            }
        }

        void registerCallback(int displayId, Object callback) {
            if (DeviceFeature.SUPPORT_DISPLAYFEATURE_HIDL) {
                callHwBinderTransact(2, 0, Integer.valueOf(displayId), callback);
            } else {
                callBinderTransact(2, 0, Integer.valueOf(displayId), callback);
            }
        }

        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken("android.hidl.base@1.0::IBase");
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mHwService.transact(HIDL_TRANSACTION_interfaceDescriptor, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        private void callBinderTransact(int transactId, int flag, Object... params) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                try {
                    data.writeInterfaceToken(this.mDescriptor);
                    for (Object param : params) {
                        if (param instanceof Integer) {
                            data.writeInt(((Integer) param).intValue());
                        } else if (param instanceof IInterface) {
                            data.writeStrongBinder(((IInterface) param).asBinder());
                        }
                    }
                    IBinder iBinder = this.mService;
                    if (iBinder != null) {
                        iBinder.transact(transactId, data, reply, flag);
                    }
                } catch (RemoteException e) {
                    Slog.e(DisplayFeatureManagerService.TAG, "callBinderTransact transact fail. " + e);
                }
            } finally {
                reply.recycle();
                data.recycle();
            }
        }

        private void callHwBinderTransact(int _hidl_code, int flag, Object... params) {
            HwParcel hidl_reply = new HwParcel();
            try {
                try {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken(this.mDescriptor);
                    for (Object param : params) {
                        if (param instanceof Integer) {
                            hidl_request.writeInt32(((Integer) param).intValue());
                        } else if (param instanceof IHwInterface) {
                            hidl_request.writeStrongBinder(((IHwInterface) param).asBinder());
                        }
                    }
                    this.mHwService.transact(_hidl_code, hidl_request, hidl_reply, flag);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                } catch (RemoteException e) {
                    Slog.e(DisplayFeatureManagerService.TAG, "callHwBinderTransact transact fail. " + e);
                }
            } finally {
                hidl_reply.release();
            }
        }
    }
}
