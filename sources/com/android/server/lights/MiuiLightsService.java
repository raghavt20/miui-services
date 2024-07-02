package com.android.server.lights;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.hardware.light.HwLight;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.lights.LightsService;
import com.android.server.lights.MiuiLightsService;
import com.android.server.lights.VisualizerHolder;
import com.android.server.notification.NotificationManagerService;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import miui.app.MiuiLightsManagerInternal;
import miui.lights.ILightsManager;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.util.FeatureParser;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class MiuiLightsService extends LightsService {
    private static final long LED_END_WORKTIME_DEF = 82800000;
    private static final long LED_START_WORKTIME_DEF = 25200000;
    public static final int LIGHT_ID_COLORFUL = 8;
    public static final int LIGHT_ID_LED = 11;
    public static final int LIGHT_ID_MUSIC = 9;
    public static final int LIGHT_ID_PRIVACY = 10;
    private static final int LIGHT_ON_MS = 500;
    private static final long ONE_DAY = 86400000;
    private static final long ONE_HOUR = 3600000;
    private static final long ONE_MINUTE = 60000;
    private static final int STOP_FLASH_MSG = 1;
    private static MiuiLightsService sInstance;
    private long light_end_time;
    private long light_start_time;
    private final LocalService localService;
    private AudioManager mAudioManager;
    private final AudioManagerPlaybackListener mAudioManagerPlaybackCb;
    private BatteryManagerInternal mBatteryManagerInternal;
    private final LightImpl mColorfulLight;
    private Context mContext;
    private VisualizerHolder.OnDataCaptureListener mDataCaptureListener;
    private Handler mHandler;
    private boolean mIsLedTurnOn;
    private boolean mIsWorkTime;
    private List<String> mLedEvents;
    private final LightImpl mLedLight;
    private LightContentObserver mLightContentObserver;
    private Handler mLightHandler;
    private final LightStyleLoader mLightStyleLoader;
    private final Object mLock;
    private final LightImpl mMusicLight;
    private PackageManagerInternal mPackageManagerInt;
    private int mPlayingPid;
    private final LinkedList<LightState> mPreviousLights;
    private final int mPreviousLightsLimit;
    private final LightImpl mPrivacyLight;
    private ContentResolver mResolver;
    private final IBinder mService;
    private boolean mSupportButtonLight;
    private boolean mSupportColorGameLed;
    private boolean mSupportColorfulLed;
    private boolean mSupportLedLight;
    private boolean mSupportLedSchedule;
    private boolean mSupportTapFingerprint;
    private LightsThread mThread;
    private final WorkSource mTmpWorkSource;
    private final PowerManager.WakeLock mWakeLock;

    /* loaded from: classes.dex */
    public interface DataCaptureListener {
        void onFrequencyCapture(Context context, int i, float[] fArr);

        void onSetLightCallback(Context context, int i, int i2, int i3, int i4, int i5, int i6);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public MiuiLightsService(Context context) {
        super(context);
        this.mPreviousLightsLimit = 100;
        this.mLock = new Object();
        this.mTmpWorkSource = new WorkSource();
        this.light_end_time = ONE_DAY;
        this.light_start_time = 0L;
        this.mIsWorkTime = true;
        this.mHandler = new Handler() { // from class: com.android.server.lights.MiuiLightsService.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        LightImpl light = (LightImpl) msg.obj;
                        light.turnOff();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mService = new AnonymousClass2();
        this.mPlayingPid = -1;
        this.mAudioManagerPlaybackCb = new AudioManagerPlaybackListener();
        this.mContext = context;
        populateAvailableLightsforMiui();
        this.mResolver = this.mContext.getContentResolver();
        this.mLightStyleLoader = new LightStyleLoader(context);
        this.mColorfulLight = new LightImpl(this.mContext, 8);
        this.mMusicLight = new LightImpl(this.mContext, 9);
        this.mPrivacyLight = new LightImpl(this.mContext, 10);
        this.mLedLight = new LightImpl(this.mContext, 11);
        PowerManager.WakeLock newWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "*lights*");
        this.mWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(true);
        HandlerThread handlerThread = new HandlerThread("MiuiLightsHandlerThread");
        handlerThread.start();
        this.mLightHandler = new Handler(handlerThread.getLooper());
        this.mPreviousLights = new LinkedList<>();
        LocalService localService = new LocalService();
        this.localService = localService;
        LocalServices.addService(MiuiLightsManagerInternal.class, localService);
    }

    private void populateAvailableLightsforMiui() {
        for (int i = this.mLightsById.size() - 1; i >= 0; i--) {
            int type = this.mLightsById.keyAt(i);
            if (type >= 0 && this.mLightsByType != null && type < this.mLightsByType.length) {
                this.mLightsByType[type] = new LightImpl(this.mContext, ((LightsService.LightImpl) this.mLightsById.valueAt(i)).mHwLight);
            }
        }
    }

    public void onStart() {
        super.onStart();
        sInstance = this;
    }

    public void onBootPhase(int phase) {
        if (phase == 1000) {
            loadSupportLights();
            updateLightEnableAndType();
            LightContentObserver lightContentObserver = new LightContentObserver();
            this.mLightContentObserver = lightContentObserver;
            lightContentObserver.observe();
            this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
            this.mContext.registerReceiver(new UserSwitchReceiver(), new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mLightHandler);
        }
    }

    private void loadSupportLights() {
        this.mSupportLedLight = FeatureParser.getBoolean("support_led_light", false);
        this.mSupportButtonLight = FeatureParser.getBoolean("support_button_light", false);
        this.mSupportTapFingerprint = FeatureParser.getBoolean("support_tap_fingerprint_sensor_to_home", false);
        this.mSupportColorfulLed = FeatureParser.getBoolean("support_led_colorful", false);
        this.mSupportColorGameLed = FeatureParser.getBoolean("support_led_colorful_game", false);
        this.mSupportLedSchedule = FeatureParser.getBoolean("support_led_schedule", false);
    }

    private void updateLightEnableAndType() {
        if (this.mSupportButtonLight) {
            Settings.Secure.putIntForUser(this.mResolver, "screen_buttons_state", 0, -2);
            ((LightImpl) this.mLightsByType[2]).updateLight();
        }
        if (this.mSupportLedLight) {
            updateLightDefaultState("default_notification_led_on", "notification_light_turn_on", true);
            updateLightDefaultState("default_battery_led_on", "battery_light_turn_on", true);
            ((LightImpl) this.mLightsByType[4]).updateLight();
            ((LightImpl) this.mLightsByType[3]).updateLight();
        }
        if (this.mSupportLedSchedule) {
            updateLightDefaultState("default_schedule_led_on", "light_turn_on_Time", false);
            this.light_end_time = Settings.Secure.getLongForUser(this.mResolver, "light_turn_on_endTime", LED_END_WORKTIME_DEF, -2);
            this.light_start_time = Settings.Secure.getLongForUser(this.mResolver, "light_turn_on_startTime", LED_START_WORKTIME_DEF, -2);
            if (isTurnOnTimeLight()) {
                updateWorkState();
            }
            this.mContext.registerReceiver(new TimeTickReceiver(), new IntentFilter("android.intent.action.TIME_TICK"), null, this.mLightHandler);
        }
        if (this.mSupportColorfulLed) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
            this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            registerAudioPlaybackCallback();
        }
    }

    private void updateLightDefaultState(String str, String str2, Boolean bool) {
        boolean z = FeatureParser.getBoolean(str, bool.booleanValue());
        if (Settings.Secure.getIntForUser(this.mResolver, str2, z ? 1 : 0, -2) == z) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), str2, z ? 1 : 0);
        }
    }

    /* loaded from: classes.dex */
    public class LightImpl extends LightsService.LightImpl {
        private List<LightState> lightStates;
        private int mBrightnessMode;
        private int mColor;
        private boolean mDisabled;
        private int mId;
        private boolean mIsShutDown;
        private int mLastColor;
        public int mLastLightStyle;
        private int mMode;
        private int mOffMS;
        private int mOnMS;
        private int mUid;
        private String pkg_name;

        public /* bridge */ /* synthetic */ void pulse() {
            super.pulse();
        }

        public /* bridge */ /* synthetic */ void pulse(int i, int i2) {
            super.pulse(i, i2);
        }

        public /* bridge */ /* synthetic */ void setBrightness(float f) {
            super.setBrightness(f);
        }

        public /* bridge */ /* synthetic */ void setBrightness(float f, int i) {
            super.setBrightness(f, i);
        }

        public /* bridge */ /* synthetic */ void setColor(int i) {
            super.setColor(i);
        }

        public /* bridge */ /* synthetic */ void setFlashing(int i, int i2, int i3, int i4) {
            super.setFlashing(i, i2, i3, i4);
        }

        public /* bridge */ /* synthetic */ void setVrMode(boolean z) {
            super.setVrMode(z);
        }

        public /* bridge */ /* synthetic */ void turnOff() {
            super.turnOff();
        }

        private LightImpl(Context context, HwLight hwLight) {
            super(MiuiLightsService.this, context, hwLight);
            this.mLastLightStyle = -1;
            this.mIsShutDown = false;
            int i = hwLight.id;
            this.mId = i;
            this.mDisabled = i == 2 || i == 3;
        }

        private LightImpl(Context context, int mFakeId) {
            super(MiuiLightsService.this, context, ((LightsService.LightImpl) MiuiLightsService.this.mLightsById.get(4)).mHwLight);
            this.mLastLightStyle = -1;
            this.mIsShutDown = false;
            this.mId = mFakeId;
            this.mDisabled = mFakeId == 2 || mFakeId == 3;
        }

        void setFlashing(String colorSettingKey, String freqSettingKey) {
            int defaultColor = MiuiLightsService.this.mContext.getResources().getColor(285605890);
            int color = Settings.System.getIntForUser(MiuiLightsService.this.mResolver, colorSettingKey, defaultColor, -2);
            setFlashing(color, 1, 500, 0);
            MiuiLightsService.this.mHandler.removeMessages(1);
            MiuiLightsService.this.mHandler.sendMessageDelayed(Message.obtain(MiuiLightsService.this.mHandler, 1, this), 500L);
        }

        void updateLight() {
            int i = this.mId;
            boolean z = true;
            if (2 == i) {
                if (!MiuiLightsService.this.isDisableButtonLight() && MiuiLightsService.this.isTurnOnButtonLight()) {
                    z = false;
                }
                this.mDisabled = z;
            } else if (3 == i) {
                this.mDisabled = !MiuiLightsService.this.isTurnOnBatteryLight();
            } else if (4 == i) {
                this.mDisabled = !MiuiLightsService.this.isTurnOnNotificationLight();
            } else if (9 == i) {
                this.mDisabled = !MiuiLightsService.this.isTurnOnMusicLight();
            } else if (8 == i) {
                synchronized (MiuiLightsService.this.mLock) {
                    setColorfulLightLocked(this.pkg_name, this.mUid, this.mLastLightStyle, this.lightStates);
                }
                return;
            }
            synchronized (MiuiLightsService.this.mLock) {
                setLightLocked(this.mColor, this.mMode, this.mOnMS, this.mOffMS, this.mBrightnessMode);
            }
        }

        void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (this.mId == 0) {
                realSetLightLocked(color, mode, onMS, offMS, brightnessMode);
                return;
            }
            if (!MiuiLightsService.this.isLightEnable() || this.mDisabled) {
                updateState(color, mode, onMS, offMS, brightnessMode);
                realSetLightLocked(0, 0, 0, 0, 0);
                return;
            }
            int i = this.mId;
            if (i == 3 || i == 4 || i == 9) {
                if (MiuiLightsService.this.mColorfulLight.mLastLightStyle != 1) {
                    if (this.mId != 9 && MiuiLightsService.this.isMusicLightPlaying()) {
                        return;
                    }
                    if (MiuiLightsService.this.isSceneUncomfort(this.mId)) {
                        updateState(color, mode, onMS, offMS, brightnessMode);
                        realSetLightLocked(0, 0, 0, 0, 0);
                        return;
                    } else if (this.mId == 3 && MiuiLightsService.this.mBatteryManagerInternal != null) {
                        if (MiuiLightsService.this.mBatteryManagerInternal.getBatteryLevel() == 100) {
                            color = 0;
                        }
                        if (!MiuiLightsService.this.mBatteryManagerInternal.isPowered(15) && color != 0) {
                            color = 0;
                        }
                    }
                } else {
                    return;
                }
            }
            int i2 = this.mId;
            if (i2 == 4) {
                if (color != 0 && this.mColor == 0) {
                    MiuiLightsService.this.turnoffBatteryLight();
                }
            } else if (i2 == 3 && (((LightImpl) MiuiLightsService.this.mLightsByType[4]).mColor != 0 || MiuiLightsService.this.mColorfulLight.mLastLightStyle != -1)) {
                updateState(color, mode, onMS, offMS, brightnessMode);
                return;
            }
            int i3 = color;
            updateState(i3, mode, onMS, offMS, brightnessMode);
            realSetLightLocked(i3, mode, onMS, offMS, brightnessMode);
            if (this.mId == 4 && this.mColor == 0) {
                MiuiLightsService.this.recoveryBatteryLight();
            }
        }

        void setColorfulLightLocked(String pkg_name, int mUid, int styleType, List<LightState> lightStates) {
            if (this.mId != 8 || lightStates == null) {
                Slog.e("LightsService", "Illegal Argument mLastLightStyle:" + styleType + " lightStates:" + lightStates);
                return;
            }
            if (!MiuiLightsService.this.isLightEnable()) {
                updateState(pkg_name, mUid, styleType, lightStates);
                MiuiLightsService.this.doCancelColorfulLightLocked();
                return;
            }
            if (MiuiLightsService.this.isSceneUncomfort(this.mId)) {
                Slog.i("LightsService", "Scene is uncomfort , lightstyle phone skip");
                return;
            }
            if (this.mLastLightStyle == 1 || lightStates.isEmpty()) {
                if (styleType == -1) {
                    updateState(pkg_name, mUid, styleType, lightStates);
                    MiuiLightsService.this.doCancelColorfulLightLocked();
                    MiuiLightsService.this.recoveryBatteryLight();
                    return;
                }
                return;
            }
            MiuiLightsService.this.reportLedEventLocked(styleType, true, 0, 0);
            MiuiLightsService.this.doCancelColorfulLightLocked();
            MiuiLightsService.this.mThread = new LightsThread(lightStates, styleType, mUid);
            MiuiLightsService.this.mThread.start();
            updateState(pkg_name, mUid, styleType, lightStates);
            MiuiLightsService.this.addToLightCollectionLocked(new LightState(pkg_name, styleType));
        }

        public void setBrightness(int brightness, boolean isShutDown) {
            this.mIsShutDown = isShutDown;
            super.setBrightness(brightness);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void realSetLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (this.mIsShutDown) {
                color = 0;
            }
            int i = this.mId;
            if (i == 8 || i == 9) {
                super.setLightLocked(color, mode, onMS, offMS, brightnessMode);
                return;
            }
            if ((i == 3 || i == 4) && this.mLastColor != color) {
                Slog.v("LightsService", "realSetLightLocked #" + this.mId + ": color=#" + Integer.toHexString(color) + ": onMS=" + onMS + " offMS=" + offMS + " mode=" + mode);
                MiuiLightsService.this.addToLightCollectionLocked(new LightState(this.mId, color, mode, onMS, offMS, brightnessMode));
                if (MiuiLightsService.this.mLedEvents == null) {
                    MiuiLightsService.this.mLedEvents = new ArrayList();
                }
                if (color == 0) {
                    if (MiuiLightsService.this.mIsLedTurnOn) {
                        MiuiLightsService.this.mIsLedTurnOn = false;
                        MiuiLightsService.this.reportLedEventLocked(this.mId, false, onMS, offMS);
                    }
                } else if (!MiuiLightsService.this.mIsLedTurnOn) {
                    MiuiLightsService.this.mIsLedTurnOn = true;
                    MiuiLightsService.this.reportLedEventLocked(this.mId, true, onMS, offMS);
                }
            }
            super.setLightLocked(color, mode, onMS, offMS, brightnessMode);
            this.mLastColor = color;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setColorCommonLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            super.setLightLocked(color, mode, onMS, offMS, brightnessMode);
        }

        private void updateState(int color, int mode, int onMS, int offMS, int brightnessMode) {
            this.mColor = color;
            this.mMode = mode;
            this.mOnMS = onMS;
            this.mOffMS = offMS;
            this.mBrightnessMode = brightnessMode;
        }

        private void updateState(String pkg_name, int mUid, int styleType, List<LightState> lightStates) {
            this.pkg_name = pkg_name;
            this.mUid = mUid;
            this.mLastLightStyle = styleType;
            this.lightStates = lightStates;
        }

        public String toString() {
            return "LightImpl{mDisabled=" + this.mDisabled + ", mColor=" + this.mColor + ", mMode=" + this.mMode + ", mOnMS=" + this.mOnMS + ", mOffMS=" + this.mOffMS + ", mBrightnessMode=" + this.mBrightnessMode + ", mId=" + this.mId + ", mLastColor=" + this.mLastColor + ", pkg_name='" + this.pkg_name + "', mUid=" + this.mUid + ", mLastLightStyle=" + this.mLastLightStyle + ", mIsShutDown=" + this.mIsShutDown + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportLedEventLocked(int mId, boolean isTurnOn, int onMS, int offMs) {
        JSONObject info = new JSONObject();
        try {
            info.put(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, String.valueOf(mId));
            info.put("isTurnOn", String.valueOf(isTurnOn ? 1 : 0));
            info.put("onMs", String.valueOf(onMS));
            info.put("offMs", String.valueOf(offMs));
            info.put("time", String.valueOf(System.currentTimeMillis()));
            if (this.mLedEvents == null) {
                this.mLedEvents = new ArrayList();
            }
            this.mLedEvents.add(info.toString());
            if (this.mLedEvents.size() >= 30) {
                MQSEventManagerDelegate.getInstance().reportEvents("led", this.mLedEvents, false);
                this.mLedEvents.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MiuiLightsService getInstance() {
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.lights.MiuiLightsService$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends ILightsManager.Stub {
        AnonymousClass2() {
        }

        public void setColorfulLight(String callingPackage, int styleType, int userId) throws RemoteException {
            MiuiLightsService.this.checkCallerVerify(callingPackage);
            MiuiLightsService.this.localService.setColorfulLight(callingPackage, styleType, userId);
        }

        public void setColorCommon(final int color, final String callingPackage, final int styleType, int userId) throws RemoteException {
            MiuiLightsService.this.mLightHandler.post(new Runnable() { // from class: com.android.server.lights.MiuiLightsService$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiLightsService.AnonymousClass2.this.lambda$setColorCommon$0(callingPackage, styleType, color);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$setColorCommon$0(String callingPackage, int styleType, int color) {
            Slog.d("LightsService", "setColorCommon callingPkg: " + callingPackage + " styleType: " + styleType + " color:" + color);
            synchronized (MiuiLightsService.this.mLock) {
                if (styleType == 7) {
                    MiuiLightsService.this.mPrivacyLight.setColorCommonLocked(color, 0, 0, 0, 0);
                }
            }
        }

        public void setColorLed(final int color, final String callingPackage, final int styleType, int userId, final int category) throws RemoteException {
            MiuiLightsService.this.mLightHandler.post(new Runnable() { // from class: com.android.server.lights.MiuiLightsService$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiLightsService.AnonymousClass2.this.lambda$setColorLed$1(callingPackage, styleType, color, category);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$setColorLed$1(String callingPackage, int styleType, int color, int category) {
            Slog.d("LightsService", "setColorLed callingPkg: " + callingPackage + " styleType: " + styleType + " color:" + color);
            synchronized (MiuiLightsService.this.mLock) {
                if (styleType == 8) {
                    if (category == 1 || category == 2 || category == 3 || category == 4) {
                        MiuiLightsService.this.mLedLight.setColorCommonLocked(color, 0, 0, 0, 0);
                    } else {
                        MiuiLightsService.this.mLedLight.setColorCommonLocked(color, 1, 500, 0, 0);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public final class LocalService extends MiuiLightsManagerInternal {
        public LocalService() {
        }

        @Override // miui.app.MiuiLightsManagerInternal
        public void setColorfulLight(final String callingPackage, final int styleType, int userId) {
            MiuiLightsService.this.mLightHandler.post(new Runnable() { // from class: com.android.server.lights.MiuiLightsService$LocalService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiLightsService.LocalService.this.lambda$setColorfulLight$0(callingPackage, styleType);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$setColorfulLight$0(String callingPackage, int styleType) {
            Slog.d("LightsService", "setColorfulLight callingPkg: " + callingPackage + " styleType: " + styleType);
            List<LightState> lightStyle = MiuiLightsService.this.mLightStyleLoader.getLightStyle(styleType);
            synchronized (MiuiLightsService.this.mLock) {
                MiuiLightsService.this.mColorfulLight.setColorfulLightLocked(callingPackage, Binder.getCallingUid(), styleType, lightStyle);
            }
        }

        @Override // miui.app.MiuiLightsManagerInternal
        public IBinder getBinderService() {
            return MiuiLightsService.this.mService;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LightsThread extends Thread {
        private final List<LightState> lightStateList;
        private boolean mForceStop;
        private final int styleType;
        private final int LOOP_LIMIT = 35;
        private int loop_index = 0;

        public LightsThread(List<LightState> lightStateList, int styleType, int mUid) {
            this.lightStateList = lightStateList;
            this.styleType = styleType;
            MiuiLightsService.this.mTmpWorkSource.set(mUid);
            MiuiLightsService.this.mWakeLock.setWorkSource(MiuiLightsService.this.mTmpWorkSource);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Process.setThreadPriority(-8);
            MiuiLightsService.this.mWakeLock.acquire();
            try {
                MiuiLightsService.this.turnoffBatteryLight();
                boolean finished = playLight(this.styleType);
                if (finished) {
                    MiuiLightsService.this.recoveryBatteryLight();
                    MiuiLightsService.this.reportLedEventLocked(this.styleType, false, 0, 0);
                }
            } finally {
                MiuiLightsService.this.mWakeLock.release();
            }
        }

        public boolean playLight(int styleType) {
            boolean z;
            int i;
            synchronized (this) {
                int size = this.lightStateList.size();
                int index = 0;
                while (true) {
                    z = this.mForceStop;
                    if (!z) {
                        if (index < size) {
                            int index2 = index + 1;
                            LightState lightState = this.lightStateList.get(index);
                            MiuiLightsService.this.mColorfulLight.realSetLightLocked(lightState.colorARGB, lightState.flashMode, lightState.onMS, lightState.offMS, lightState.brightnessMode);
                            delayLocked(lightState.onMS + lightState.offMS);
                            index = index2;
                        } else if (styleType == 1 && (i = this.loop_index) < 35) {
                            index = 0;
                            this.loop_index = i + 1;
                        } else {
                            cancel();
                            MiuiLightsService.this.mColorfulLight.realSetLightLocked(0, 0, 0, 0, 0);
                            MiuiLightsService.this.mColorfulLight.mLastLightStyle = -1;
                        }
                    }
                }
            }
            return z;
        }

        private long delayLocked(long duration) {
            long durationRemaining = duration;
            if (duration <= 0) {
                return 0L;
            }
            long bedtime = SystemClock.uptimeMillis() + duration;
            do {
                try {
                    wait(durationRemaining);
                } catch (InterruptedException e) {
                }
                if (this.mForceStop) {
                    break;
                }
                durationRemaining = bedtime - SystemClock.uptimeMillis();
            } while (durationRemaining > 0);
            return duration - durationRemaining;
        }

        public void cancel() {
            synchronized (this) {
                MiuiLightsService.this.mThread.mForceStop = true;
                MiuiLightsService.this.mThread.notifyAll();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doCancelColorfulLightLocked() {
        LightsThread lightsThread = this.mThread;
        if (lightsThread != null) {
            lightsThread.cancel();
            this.mThread = null;
            this.mColorfulLight.mLastLightStyle = -1;
            this.mColorfulLight.realSetLightLocked(0, 0, 0, 0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class AudioManagerPlaybackListener extends AudioManager.AudioPlaybackCallback {
        private AudioManagerPlaybackListener() {
        }

        /* JADX WARN: Code restructure failed: missing block: B:23:0x00d8, code lost:
        
            if (com.android.server.lights.LightsService.DEBUG == false) goto L39;
         */
        /* JADX WARN: Code restructure failed: missing block: B:24:0x00da, code lost:
        
            android.util.Slog.d("LightsService", "Stop Playing pid:" + r8.this$0.mPlayingPid);
         */
        /* JADX WARN: Code restructure failed: missing block: B:25:0x00f8, code lost:
        
            r3 = r8.this$0.mLock;
         */
        /* JADX WARN: Code restructure failed: missing block: B:26:0x00fe, code lost:
        
            monitor-enter(r3);
         */
        /* JADX WARN: Code restructure failed: missing block: B:28:0x00ff, code lost:
        
            r0 = r8.this$0;
            r0.addToLightCollectionLocked(new com.android.server.lights.LightState(r0.mPackageManagerInt.getNameForUid(r2.getClientUid()), 3));
         */
        /* JADX WARN: Code restructure failed: missing block: B:29:0x0115, code lost:
        
            monitor-exit(r3);
         */
        /* JADX WARN: Code restructure failed: missing block: B:30:0x0116, code lost:
        
            r8.this$0.releaseVisualizer();
         */
        /* JADX WARN: Code restructure failed: missing block: B:32:0x0128, code lost:
        
            if (r8.this$0.mPlayingPid == (-1)) goto L80;
         */
        /* JADX WARN: Code restructure failed: missing block: B:33:0x012a, code lost:
        
            r0 = r9.iterator();
         */
        /* JADX WARN: Code restructure failed: missing block: B:35:0x0132, code lost:
        
            if (r0.hasNext() == false) goto L77;
         */
        /* JADX WARN: Code restructure failed: missing block: B:37:0x0144, code lost:
        
            if (r0.next().getClientPid() != r8.this$0.mPlayingPid) goto L79;
         */
        /* JADX WARN: Code restructure failed: missing block: B:39:0x0146, code lost:
        
            return;
         */
        /* JADX WARN: Code restructure failed: missing block: B:44:0x014a, code lost:
        
            if (com.android.server.lights.LightsService.DEBUG == false) goto L60;
         */
        /* JADX WARN: Code restructure failed: missing block: B:45:0x014c, code lost:
        
            android.util.Slog.d("LightsService", "Not found player: " + r8.this$0.mPlayingPid + "in callback!");
         */
        /* JADX WARN: Code restructure failed: missing block: B:46:0x0170, code lost:
        
            r8.this$0.releaseVisualizer();
         */
        /* JADX WARN: Code restructure failed: missing block: B:47:0x0175, code lost:
        
            return;
         */
        /* JADX WARN: Code restructure failed: missing block: B:48:?, code lost:
        
            return;
         */
        @Override // android.media.AudioManager.AudioPlaybackCallback
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void onPlaybackConfigChanged(java.util.List<android.media.AudioPlaybackConfiguration> r9) {
            /*
                Method dump skipped, instructions count: 380
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.lights.MiuiLightsService.AudioManagerPlaybackListener.onPlaybackConfigChanged(java.util.List):void");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.lights.MiuiLightsService$3, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements VisualizerHolder.OnDataCaptureListener {
        AnonymousClass3() {
        }

        @Override // com.android.server.lights.VisualizerHolder.OnDataCaptureListener
        public void onFrequencyCapture(int frequency, float[] frequencies) {
            synchronized (MiuiLightsService.this.mLock) {
                MiuiLightsService.this.mMusicLight.setLightLocked(frequency | (-16777216), 1, 100, 0, 0);
            }
            if (frequency == 0) {
                MiuiLightsService.this.mLightHandler.postDelayed(new Runnable() { // from class: com.android.server.lights.MiuiLightsService$3$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiLightsService.AnonymousClass3.this.lambda$onFrequencyCapture$0();
                    }
                }, 3000L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onFrequencyCapture$0() {
            if (!MiuiLightsService.this.mAudioManager.isMusicActive() && !new File("/proc/" + MiuiLightsService.this.mPlayingPid).exists()) {
                MiuiLightsService.this.releaseVisualizer();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VisualizerHolder.OnDataCaptureListener getDataCaptureListener() {
        if (this.mDataCaptureListener == null) {
            this.mDataCaptureListener = new AnonymousClass3();
        }
        return this.mDataCaptureListener;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerAudioPlaybackCallback() {
        if (this.mSupportColorfulLed && isTurnOnLight() && isTurnOnMusicLight()) {
            this.mAudioManager.registerAudioPlaybackCallback(this.mAudioManagerPlaybackCb, this.mLightHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterAudioPlaybackCallback() {
        AudioManagerPlaybackListener audioManagerPlaybackListener;
        if (this.mSupportColorfulLed && (audioManagerPlaybackListener = this.mAudioManagerPlaybackCb) != null) {
            this.mAudioManager.unregisterAudioPlaybackCallback(audioManagerPlaybackListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseVisualizer() {
        VisualizerHolder.getInstance().release();
        this.mMusicLight.turnOff();
        recoveryBatteryLight();
        this.mPlayingPid = -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isMusicLightPlaying() {
        return this.mPlayingPid != -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void turnoffBatteryLight() {
        LightImpl batteryLight = (LightImpl) this.mLightsByType[3];
        if (batteryLight.mColor != 0) {
            batteryLight.realSetLightLocked(0, 0, 0, 0, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recoveryBatteryLight() {
        if (this.mColorfulLight.mLastLightStyle != -1) {
            Slog.i("LightsService", "skip light bat , cur light id :" + this.mColorfulLight.mLastLightStyle);
            return;
        }
        LightImpl batteryLight = (LightImpl) this.mLightsByType[3];
        if (batteryLight.mColor != 0 && !batteryLight.mDisabled && !isSceneUncomfort(batteryLight.mId) && isLightEnable()) {
            batteryLight.realSetLightLocked(batteryLight.mColor, batteryLight.mMode, batteryLight.mOnMS, batteryLight.mOffMS, batteryLight.mBrightnessMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkCallerVerify(String callingPackage) {
        if (callingPackage == null) {
            throw new IllegalArgumentException("callingPackage is invalid!");
        }
        if (!this.mSupportColorfulLed) {
            throw new IllegalStateException("Current devices doesn't support ColorfulLed!");
        }
        int uid = Binder.getCallingUid();
        int appid = UserHandle.getAppId(uid);
        if (appid != 1000 && appid != 1001 && appid != 1013 && uid != 0 && uid != 2000) {
            throw new SecurityException("Disallowed call for uid " + Binder.getCallingUid());
        }
    }

    /* loaded from: classes.dex */
    private class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (MiuiLightsService.this.mSupportButtonLight) {
                ((LightImpl) MiuiLightsService.this.mLightsByType[2]).updateLight();
            }
            if (MiuiLightsService.this.mSupportLedLight) {
                ((LightImpl) MiuiLightsService.this.mLightsByType[3]).updateLight();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TimeTickReceiver extends BroadcastReceiver {
        private TimeTickReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (MiuiLightsService.this.mSupportColorGameLed && !MiuiLightsService.this.isTurnOnTimeLight() && MiuiLightsService.this.mIsWorkTime) {
                return;
            }
            MiuiLightsService.this.updateWorkState();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWorkState() {
        if (this.mSupportLedSchedule) {
            Calendar cal = Calendar.getInstance();
            long now_stamp = (cal.get(11) * 3600000) + (cal.get(12) * 60000);
            long j = this.light_end_time;
            long j2 = this.light_start_time;
            if (j >= j2) {
                if (now_stamp > j || now_stamp < j2) {
                    this.mIsWorkTime = false;
                    return;
                }
            } else if (now_stamp > j && now_stamp < j2) {
                this.mIsWorkTime = false;
                return;
            }
            this.mIsWorkTime = true;
        }
    }

    /* loaded from: classes.dex */
    private class LightContentObserver extends ContentObserver {
        public final Uri BATTERY_LIGHT_TURN_ON_URI;
        public final Uri BREATHING_LIGHT_COLOR_URI;
        public final Uri CALL_BREATHING_LIGHT_COLOR_URI;
        public final Uri LIGHT_TURN_ON_ENDTIME_URI;
        public final Uri LIGHT_TURN_ON_STARTTIME_URI;
        public final Uri LIGHT_TURN_ON_TIME_URI;
        public final Uri LIGHT_TURN_ON_URI;
        public final Uri MMS_BREATHING_LIGHT_COLOR_URI;
        public final Uri MUSIC_LIGHT_TURN_ON_URI;
        public final Uri NOTIFICATION_LIGHT_TURN_ON_URI;
        public final Uri SCREEN_BUTTONS_STATE_URI;
        public final Uri SCREEN_BUTTONS_TURN_ON_URI;
        public final Uri SINGLE_KEY_USE_ACTION_URI;

        public LightContentObserver() {
            super(MiuiLightsService.this.mLightHandler);
            this.SCREEN_BUTTONS_STATE_URI = Settings.Secure.getUriFor("screen_buttons_state");
            this.SINGLE_KEY_USE_ACTION_URI = Settings.System.getUriFor("single_key_use_enable");
            this.SCREEN_BUTTONS_TURN_ON_URI = Settings.Secure.getUriFor("screen_buttons_turn_on");
            this.BREATHING_LIGHT_COLOR_URI = Settings.System.getUriFor("breathing_light_color");
            this.CALL_BREATHING_LIGHT_COLOR_URI = Settings.System.getUriFor("call_breathing_light_color");
            this.MMS_BREATHING_LIGHT_COLOR_URI = Settings.System.getUriFor("mms_breathing_light_color");
            this.BATTERY_LIGHT_TURN_ON_URI = Settings.Secure.getUriFor("battery_light_turn_on");
            this.NOTIFICATION_LIGHT_TURN_ON_URI = Settings.Secure.getUriFor("notification_light_turn_on");
            this.LIGHT_TURN_ON_URI = Settings.Secure.getUriFor("light_turn_on");
            this.LIGHT_TURN_ON_TIME_URI = Settings.Secure.getUriFor("light_turn_on_Time");
            this.LIGHT_TURN_ON_STARTTIME_URI = Settings.Secure.getUriFor("light_turn_on_startTime");
            this.LIGHT_TURN_ON_ENDTIME_URI = Settings.Secure.getUriFor("light_turn_on_endTime");
            this.MUSIC_LIGHT_TURN_ON_URI = Settings.Secure.getUriFor("music_light_turn_on");
        }

        public void observe() {
            if (MiuiLightsService.this.mSupportButtonLight) {
                MiuiLightsService.this.mResolver.registerContentObserver(this.SCREEN_BUTTONS_STATE_URI, false, this, -1);
                if (MiuiLightsService.this.mSupportTapFingerprint) {
                    MiuiLightsService.this.mResolver.registerContentObserver(this.SINGLE_KEY_USE_ACTION_URI, false, this, -1);
                }
                MiuiLightsService.this.mResolver.registerContentObserver(this.SCREEN_BUTTONS_TURN_ON_URI, false, this, -1);
            }
            if (MiuiLightsService.this.mSupportLedLight) {
                MiuiLightsService.this.mResolver.registerContentObserver(this.BREATHING_LIGHT_COLOR_URI, false, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.CALL_BREATHING_LIGHT_COLOR_URI, false, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.MMS_BREATHING_LIGHT_COLOR_URI, false, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.BATTERY_LIGHT_TURN_ON_URI, true, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.NOTIFICATION_LIGHT_TURN_ON_URI, true, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.LIGHT_TURN_ON_URI, true, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.LIGHT_TURN_ON_TIME_URI, true, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.MUSIC_LIGHT_TURN_ON_URI, true, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.LIGHT_TURN_ON_STARTTIME_URI, true, this, -1);
                MiuiLightsService.this.mResolver.registerContentObserver(this.LIGHT_TURN_ON_ENDTIME_URI, true, this, -1);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (this.SCREEN_BUTTONS_STATE_URI.equals(uri) || this.SINGLE_KEY_USE_ACTION_URI.equals(uri) || this.SCREEN_BUTTONS_TURN_ON_URI.equals(uri)) {
                LightImpl light = (LightImpl) MiuiLightsService.this.mLightsByType[2];
                light.updateLight();
                return;
            }
            if (this.BREATHING_LIGHT_COLOR_URI.equals(uri) || this.CALL_BREATHING_LIGHT_COLOR_URI.equals(uri) || this.MMS_BREATHING_LIGHT_COLOR_URI.equals(uri)) {
                LightImpl light2 = (LightImpl) MiuiLightsService.this.mLightsByType[4];
                light2.setFlashing(uri.getLastPathSegment(), null);
                return;
            }
            if (this.BATTERY_LIGHT_TURN_ON_URI.equals(uri)) {
                LightImpl light3 = (LightImpl) MiuiLightsService.this.mLightsByType[3];
                light3.updateLight();
                return;
            }
            if (this.NOTIFICATION_LIGHT_TURN_ON_URI.equals(uri)) {
                LightImpl light4 = (LightImpl) MiuiLightsService.this.mLightsByType[4];
                light4.updateLight();
                return;
            }
            if (this.LIGHT_TURN_ON_URI.equals(uri)) {
                if (MiuiLightsService.this.isTurnOnLight()) {
                    MiuiLightsService.this.registerAudioPlaybackCallback();
                    if (MiuiLightsService.this.isTurnOnMusicLight() && MiuiLightsService.this.mAudioManager.isMusicActive()) {
                        MiuiLightsService.this.turnoffBatteryLight();
                        VisualizerHolder.getInstance().setOnDataCaptureListener(MiuiLightsService.this.getDataCaptureListener());
                    }
                } else {
                    MiuiLightsService.this.releaseVisualizer();
                    MiuiLightsService.this.unregisterAudioPlaybackCallback();
                }
                MiuiLightsService.this.updateLightState();
                return;
            }
            if (this.MUSIC_LIGHT_TURN_ON_URI.equals(uri)) {
                MiuiLightsService.this.mMusicLight.updateLight();
                if (!MiuiLightsService.this.isTurnOnMusicLight()) {
                    MiuiLightsService.this.releaseVisualizer();
                    MiuiLightsService.this.unregisterAudioPlaybackCallback();
                    return;
                } else {
                    if (MiuiLightsService.this.isTurnOnLight() && MiuiLightsService.this.isTurnOnMusicLight()) {
                        MiuiLightsService.this.registerAudioPlaybackCallback();
                        if (MiuiLightsService.this.mAudioManager.isMusicActive()) {
                            MiuiLightsService.this.turnoffBatteryLight();
                            VisualizerHolder.getInstance().setOnDataCaptureListener(MiuiLightsService.this.getDataCaptureListener());
                            return;
                        }
                        return;
                    }
                    return;
                }
            }
            if (this.LIGHT_TURN_ON_TIME_URI.equals(uri)) {
                if (MiuiLightsService.this.isTurnOnTimeLight() && MiuiLightsService.this.isTurnOnLight()) {
                    MiuiLightsService miuiLightsService = MiuiLightsService.this;
                    miuiLightsService.light_start_time = Settings.Secure.getLongForUser(miuiLightsService.mResolver, "light_turn_on_startTime", MiuiLightsService.LED_START_WORKTIME_DEF, -2);
                    MiuiLightsService miuiLightsService2 = MiuiLightsService.this;
                    miuiLightsService2.light_end_time = Settings.Secure.getLongForUser(miuiLightsService2.mResolver, "light_turn_on_endTime", MiuiLightsService.LED_END_WORKTIME_DEF, -2);
                    MiuiLightsService.this.updateWorkState();
                    MiuiLightsService.this.updateLightState();
                    return;
                }
                if (MiuiLightsService.this.isTurnOnLight()) {
                    MiuiLightsService.this.mIsWorkTime = true;
                    MiuiLightsService.this.updateLightState();
                    return;
                }
                return;
            }
            if (this.LIGHT_TURN_ON_STARTTIME_URI.equals(uri)) {
                MiuiLightsService miuiLightsService3 = MiuiLightsService.this;
                miuiLightsService3.light_start_time = Settings.Secure.getLongForUser(miuiLightsService3.mResolver, "light_turn_on_startTime", MiuiLightsService.LED_START_WORKTIME_DEF, -2);
                if (LightsService.DEBUG) {
                    Slog.i("LightsService", "onChange hour:" + (MiuiLightsService.this.light_start_time / 3600000) + " minute:" + ((MiuiLightsService.this.light_start_time % 3600000) / 60000));
                }
                if (MiuiLightsService.this.light_start_time < 0 || MiuiLightsService.this.light_start_time > MiuiLightsService.ONE_DAY) {
                    Settings.Secure.putLong(MiuiLightsService.this.mContext.getContentResolver(), "light_turn_on_startTime", MiuiLightsService.LED_START_WORKTIME_DEF);
                    MiuiLightsService.this.light_start_time = MiuiLightsService.LED_START_WORKTIME_DEF;
                }
                MiuiLightsService.this.updateWorkState();
                MiuiLightsService.this.updateLightState();
                return;
            }
            if (this.LIGHT_TURN_ON_ENDTIME_URI.equals(uri)) {
                MiuiLightsService miuiLightsService4 = MiuiLightsService.this;
                miuiLightsService4.light_end_time = Settings.Secure.getLongForUser(miuiLightsService4.mResolver, "light_turn_on_endTime", MiuiLightsService.LED_END_WORKTIME_DEF, -2);
                if (LightsService.DEBUG) {
                    Slog.i("LightsService", "onChange hour:" + (MiuiLightsService.this.light_end_time / 3600000) + " minute:" + ((MiuiLightsService.this.light_end_time % 3600000) / 60000));
                }
                if (MiuiLightsService.this.light_end_time < 0 || MiuiLightsService.this.light_end_time > MiuiLightsService.ONE_DAY) {
                    Settings.Secure.putLong(MiuiLightsService.this.mContext.getContentResolver(), "light_turn_on_endTime", MiuiLightsService.LED_END_WORKTIME_DEF);
                    MiuiLightsService.this.light_end_time = MiuiLightsService.LED_END_WORKTIME_DEF;
                }
                MiuiLightsService.this.updateWorkState();
                MiuiLightsService.this.updateLightState();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLightState() {
        this.mColorfulLight.updateLight();
        LightImpl light = (LightImpl) this.mLightsByType[3];
        light.updateLight();
        LightImpl light2 = (LightImpl) this.mLightsByType[4];
        light2.updateLight();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSceneUncomfort(int mId) {
        if (this.mSupportColorfulLed && mId != 3 && MiuiSettings.SilenceMode.getZenMode(this.mContext) == 1) {
            Slog.i("LightsService", "Scene is uncomfort , lights skip!");
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isLightEnable() {
        return this.mSupportLedLight && this.mIsWorkTime && isTurnOnLight();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTurnOnLight() {
        return Settings.Secure.getIntForUser(this.mResolver, "light_turn_on", 1, -2) == 1;
    }

    public boolean isTurnOnTimeLight() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "light_turn_on_Time", 0, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTurnOnButtonLight() {
        return Settings.Secure.getIntForUser(this.mResolver, "screen_buttons_turn_on", 1, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTurnOnBatteryLight() {
        return Settings.Secure.getIntForUser(this.mResolver, "battery_light_turn_on", 1, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTurnOnNotificationLight() {
        return Settings.Secure.getIntForUser(this.mResolver, "notification_light_turn_on", 1, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTurnOnMusicLight() {
        return this.mSupportColorfulLed && !this.mSupportColorGameLed && Settings.Secure.getIntForUser(this.mResolver, "music_light_turn_on", 1, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDisableButtonLight() {
        return this.mSupportTapFingerprint ? Settings.Secure.getIntForUser(this.mResolver, "screen_buttons_state", 0, -2) != 0 || Settings.System.getIntForUser(this.mResolver, "single_key_use_enable", 0, -2) == 1 : Settings.Secure.getIntForUser(this.mResolver, "screen_buttons_state", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addToLightCollectionLocked(LightState lightState) {
        if (this.mPreviousLights.size() > 100) {
            this.mPreviousLights.removeFirst();
        }
        this.mPreviousLights.addLast(lightState);
    }

    public void dumpLight(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("MiuiLightsService Status:");
        synchronized (this.mLock) {
            pw.println(" ZenMode:" + MiuiSettings.SilenceMode.getZenMode(this.mContext));
            pw.println(" mSupportColorFulLight:" + this.mSupportColorfulLed);
            pw.println(" mSupportGameColorFulLight:" + this.mSupportColorGameLed);
            pw.println(" Led Working Time: state " + this.mIsWorkTime + " start:" + this.light_start_time + " end:" + this.light_end_time);
            pw.println(" mSupportTapFingerprint:" + this.mSupportTapFingerprint);
            pw.println(" mSupportButtonLight:" + this.mSupportButtonLight);
            pw.println(" mSupportLedLight:" + this.mSupportLedLight);
            pw.println(" mIsLedTurnOn:" + this.mIsLedTurnOn);
            pw.println(" isLightEnable: " + isLightEnable());
            pw.println(" isTurnOnLight: " + isTurnOnLight());
            pw.println(" isTurnOnButtonLight: " + isTurnOnButtonLight());
            pw.println(" isTurnOnBatteryLight: " + isTurnOnBatteryLight());
            pw.println(" isTurnOnNotificationLight: " + isTurnOnNotificationLight());
            pw.println(" isTurnOnMusicLight: " + isTurnOnMusicLight());
            for (int i = 0; i < this.mLightsByType.length; i++) {
                if (this.mLightsByType[i] != null) {
                    pw.println(" " + this.mLightsByType[i].toString());
                }
            }
            pw.println(" " + this.mColorfulLight.toString());
            pw.println(" " + this.mMusicLight.toString());
            pw.println("  Previous Lights:");
            Iterator<LightState> it = this.mPreviousLights.iterator();
            while (it.hasNext()) {
                LightState lightstate = it.next();
                pw.print("    ");
                pw.println(lightstate.toString());
            }
        }
    }
}
