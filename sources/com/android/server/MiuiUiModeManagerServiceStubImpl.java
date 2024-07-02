package com.android.server;

import android.R;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.MathUtils;
import android.util.Slog;
import com.android.server.twilight.TwilightState;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.PrintWriter;
import java.util.Calendar;

@MiuiStubHead(manifestName = "com.android.server.MiuiUiModeManagerServiceStub$$")
/* loaded from: classes.dex */
public class MiuiUiModeManagerServiceStubImpl extends MiuiUiModeManagerServiceStub {
    private static float A = 0.0f;
    private static final int AUTO_NIGHT_DEFAULT_END_TIME = 360;
    private static final int AUTO_NIGHT_DEFAULT_START_TIME = 1080;
    private static final String AUTO_NIGHT_END_TIME = "auto_night_end_time";
    private static final String AUTO_NIGHT_START_TIME = "auto_night_start_time";
    private static float B = 0.0f;
    private static float C = 0.0f;
    private static final String DARK_MODE_ENABLE = "dark_mode_enable";
    private static final String DARK_MODE_ENABLE_BY_POWER_SAVE = "dark_mode_enable_by_power_save";
    private static final String DARK_MODE_ENABLE_BY_SETTING = "dark_mode_enable_by_setting";
    private static final String DARK_MODE_SWITCH_NOW = "dark_mode_switch_now";
    private static final int GAMMA_SPACE_MIN = 0;
    private static final String GET_SUN_TIME_FROM_CLOUD = "get_sun_time_from_cloud";
    private static float R = 0.0f;
    private static final String TAG = "MiuiUiModeManagerServiceStubImpl";
    private Context mContext;
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.1
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            BrightnessInfo info = MiuiUiModeManagerServiceStubImpl.this.mContext.getDisplay().getBrightnessInfo();
            if (info == null) {
                return;
            }
            float mMaximumBrightness = info.brightnessMaximum;
            float mMinimumBrightness = info.brightnessMinimum;
            float mBrightnessValue = info.brightness;
            MiuiUiModeManagerServiceStubImpl miuiUiModeManagerServiceStubImpl = MiuiUiModeManagerServiceStubImpl.this;
            miuiUiModeManagerServiceStubImpl.updateAlpha(miuiUiModeManagerServiceStubImpl.mContext, mBrightnessValue, mMaximumBrightness, mMinimumBrightness);
        }
    };
    private DisplayManager mDisplayManager;
    private ForceDarkUiModeModeManager mForceDarkUiModeModeManager;
    private UiModeManagerService mUiModeManagerService;
    private static final boolean IS_MEXICO_TELCEL = "mx_telcel".equals(SystemProperties.get("ro.miui.customized.region"));
    private static final boolean IS_JP_KDDI = "jp_kd".equals(SystemProperties.get("ro.miui.customized.region"));
    private static final int GAMMA_SPACE_MAX = Resources.getSystem().getInteger(R.integer.thumbnail_width_tv);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiUiModeManagerServiceStubImpl> {

        /* compiled from: MiuiUiModeManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiUiModeManagerServiceStubImpl INSTANCE = new MiuiUiModeManagerServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiUiModeManagerServiceStubImpl m254provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiUiModeManagerServiceStubImpl m253provideNewInstance() {
            return new MiuiUiModeManagerServiceStubImpl();
        }
    }

    public void init(UiModeManagerService uiModeManagerService) {
        this.mUiModeManagerService = uiModeManagerService;
        Context context = uiModeManagerService.getContext();
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        registUIModeScaleChangeObserver(uiModeManagerService, this.mContext);
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, new Handler(), 8L);
        try {
            Slog.i(TAG, "mNightMode: " + uiModeManagerService.getService().getNightMode());
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure communicating with uimode manager", e);
        }
        this.mForceDarkUiModeModeManager = new ForceDarkUiModeModeManager(uiModeManagerService);
        R = this.mContext.getResources().getFloat(285671453);
        A = this.mContext.getResources().getFloat(285671450);
        B = this.mContext.getResources().getFloat(285671451);
        C = this.mContext.getResources().getFloat(285671452);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAlpha(Context context, float mBrightness, float mMaxBrightness, float mMinBrightness) {
        float ratio = convertLinearToGammaFloat(mBrightness, mMinBrightness, mMaxBrightness) / GAMMA_SPACE_MAX;
        double alpha = 0.0d;
        if (ratio < 0.4d && ratio > 0.1d) {
            alpha = Math.sqrt((0.4d - ratio) / 2.5d);
        }
        if (ratio <= 0.1d) {
            alpha = (ratio + 0.1366d) / 0.683d;
        }
        float setalpha = (float) alpha;
        Settings.System.putFloat(context.getContentResolver(), "contrast_alpha", setalpha);
    }

    private int convertLinearToGammaFloat(float val, float min, float max) {
        float ret;
        float normalizedVal = MathUtils.norm(min, max, val) * 12.0f;
        if (normalizedVal <= 1.0f) {
            ret = MathUtils.sqrt(normalizedVal) * R;
        } else {
            float ret2 = A;
            ret = (ret2 * MathUtils.log(normalizedVal - B)) + C;
        }
        return Math.round(MathUtils.lerp(0, GAMMA_SPACE_MAX, ret));
    }

    public void onBootPhase(int phase) {
        this.mForceDarkUiModeModeManager.onBootPhase(phase);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return this.mForceDarkUiModeModeManager.onTransact(code, data, reply, flags);
    }

    private void registUIModeScaleChangeObserver(final UiModeManagerService service, final Context context) {
        ContentObserver uiModeScaleChangedObserver = new ContentObserver(new Handler()) { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                int uiModeType = Settings.System.getInt(context.getContentResolver(), "ui_mode_scale", 1);
                UiModeManagerServiceProxy.mDefaultUiModeType.set(service, uiModeType);
                synchronized (MiuiUiModeManagerServiceStubImpl.this.mUiModeManagerService.getInnerLock()) {
                    if (service.mSystemReady) {
                        service.updateLocked(0, 0);
                    }
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("ui_mode_scale"), false, uiModeScaleChangedObserver);
        uiModeScaleChangedObserver.onChange(false);
    }

    public boolean getForceDarkAppDefaultEnable(String packageName) {
        return ForceDarkAppListProvider.getInstance().getForceDarkAppDefaultEnable(packageName);
    }

    public void dump(PrintWriter pw) {
        this.mForceDarkUiModeModeManager.dump(pw);
    }

    public void setDarkModeStatus(int uiMode) {
        boolean isNightMode = (uiMode & 48) == 32;
        Settings.System.putInt(this.mContext.getContentResolver(), "dark_mode_enable", isNightMode ? 1 : 0);
    }

    public void modifySettingValue(int type, int value) {
        switch (type) {
            case 1:
                Settings.System.putInt(this.mContext.getContentResolver(), DARK_MODE_ENABLE_BY_SETTING, value);
                return;
            case 2:
                if (Settings.System.getInt(this.mContext.getContentResolver(), DARK_MODE_SWITCH_NOW, 0) == 1) {
                    Settings.System.putInt(this.mContext.getContentResolver(), DARK_MODE_SWITCH_NOW, value);
                    return;
                }
                return;
            case 3:
                Settings.System.putInt(this.mContext.getContentResolver(), DARK_MODE_ENABLE_BY_POWER_SAVE, value);
                return;
            default:
                return;
        }
    }

    public boolean getSwitchValue() {
        return Settings.System.getInt(this.mContext.getContentResolver(), DARK_MODE_SWITCH_NOW, 0) == 1;
    }

    public TwilightState getDefaultTwilightState() {
        Slog.i(TAG, "cannot get real sunrise and sunset time, return default time or old time");
        int sunRiseTime = Settings.System.getInt(this.mContext.getContentResolver(), AUTO_NIGHT_END_TIME, AUTO_NIGHT_DEFAULT_END_TIME);
        int sunSetTime = Settings.System.getInt(this.mContext.getContentResolver(), AUTO_NIGHT_START_TIME, AUTO_NIGHT_DEFAULT_START_TIME);
        long sunSetTimeMillis = getAlarmInMills(sunSetTime, sunRiseTime, false);
        long sunRiseTimeMillis = getAlarmInMills(sunSetTime, sunRiseTime, true);
        TwilightState lastState = new TwilightState(sunRiseTimeMillis, sunSetTimeMillis);
        return lastState;
    }

    private long getAlarmInMills(int startTime, int endTime, boolean isSunRise) {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(5);
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int currentTime = (hour * 60) + minute;
        if (isSunRise) {
            if (currentTime >= startTime) {
                calendar.set(5, day + 1);
            } else {
                calendar.set(5, day);
            }
            calendar.set(11, endTime / 60);
            calendar.set(12, endTime % 60);
        } else {
            if (currentTime < endTime) {
                calendar.set(5, day - 1);
            } else {
                calendar.set(5, day);
            }
            calendar.set(11, startTime / 60);
            calendar.set(12, startTime % 60);
        }
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTimeInMillis();
    }

    public void updateSunRiseSetTime(TwilightState state) {
        if (state != null) {
            int sunsetTime = calculateMinuteFormat(state.sunsetTimeMillis());
            int sunriseTime = calculateMinuteFormat(state.sunriseTimeMillis());
            Slog.i(TAG, "today state: sunrise：" + getTimeInString(sunriseTime) + "  sunset：" + getTimeInString(sunsetTime));
            Settings.System.putInt(this.mContext.getContentResolver(), AUTO_NIGHT_START_TIME, sunsetTime);
            Settings.System.putInt(this.mContext.getContentResolver(), AUTO_NIGHT_END_TIME, sunriseTime);
        }
    }

    public TwilightState getTwilightState(long sunriseTimeMillis, long sunsetTimeMillis) {
        int sunsetTime = calculateMinuteFormat(sunsetTimeMillis);
        int sunriseTime = calculateMinuteFormat(sunriseTimeMillis);
        long sunsetTimeMillis2 = getAlarmInMills(sunsetTime, sunriseTime, false);
        long sunriseTimeMillis2 = getAlarmInMills(sunsetTime, sunriseTime, true);
        return new TwilightState(sunriseTimeMillis2, sunsetTimeMillis2);
    }

    private int calculateMinuteFormat(long sunTimeMillis) {
        int sunHour = Integer.parseInt(String.valueOf(DateFormat.format("HH", sunTimeMillis)));
        int sunMinute = Integer.parseInt(String.valueOf(DateFormat.format("mm", sunTimeMillis)));
        int sunTime = (sunHour * 60) + sunMinute;
        return sunTime;
    }

    private String getTimeInString(int time) {
        return (time / 60) + ":" + (time % 60);
    }
}
