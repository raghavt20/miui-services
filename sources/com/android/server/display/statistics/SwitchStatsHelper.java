package com.android.server.display.statistics;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.os.BackgroundThread;
import com.android.server.app.GameManagerServiceStubImpl;
import com.android.server.display.expertmode.ExpertData;
import com.android.server.display.statistics.BrightnessEvent;
import com.android.server.display.statistics.SwitchStatsHelper;
import java.util.ArrayList;
import java.util.List;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class SwitchStatsHelper {
    private static final String AI_DISPLAY_MODE = "ai_display_mode";
    private static final String BACKGROUND_BLUR_ENABLE = "background_blur_enable";
    private static final boolean BACKGROUND_BLUR_SUPPORTED;
    private static final String COLOR_GAMUT_MODE = "color_gamut_mode";
    public static final String DC_BACK_LIGHT_SWITCH = "dc_back_light";
    private static final String EXPERT_DATA = "expert_data";
    private static final String IS_SMART_FPS = "is_smart_fps";
    private static final String MIUI_SCREEN_COMPAT = "miui_screen_compat";
    private static final String SCREEN_ENHANCE_ENGINE_GALLERY_AI_MODE_STATUS = "screen_enhance_engine_gallery_ai_mode_status";
    private static final boolean SUPPORT_RESOLUTION_SWITCH;
    private static final boolean SUPPORT_SMART_FPS = FeatureParser.getBoolean("support_smart_fps", false);
    public static final String USER_REFRESH_RATE = "user_refresh_rate";
    private static SwitchStatsHelper mInstance;
    private static int[] mScreenResolutionSupported;
    private boolean mAdaptiveSleepEnable;
    private boolean mAiDisplayModeEnable;
    private boolean mAutoBrightnessSettingsEnable;
    private boolean mBackgroundBlurEnable;
    private int mColorGamutMode;
    private Context mContext;
    private boolean mDarkModeSettingsEnable;
    private boolean mDcBacklightSettingsEnable;
    private boolean mDozeAlwaysOn;
    private boolean mReadModeSettingsEnable;
    private int mRefreshRateFromDeviceFeature;
    private ContentResolver mResolver;
    private int mScreenColorLevel;
    private boolean mScreenCompat;
    private int mScreenOptimizeSettingsMode;
    private boolean mScreenTrueToneEnable;
    private boolean mSmartRefreshRateEnable;
    private boolean mSunlightSettingsEnable;
    private boolean mSupportAdaptiveSleep;
    private boolean mSupportAiDisplayMode;
    private boolean mSupportExpertMode;
    private int mUserRefreshRate;
    private ArrayList<BrightnessEvent.SwitchStatEntry> mSwitchStats = new ArrayList<>();
    private Handler mBgHandler = new Handler(BackgroundThread.getHandler().getLooper());
    private SettingsObserver mSettingsObserver = new SettingsObserver(this.mBgHandler);

    /* JADX WARN: Code restructure failed: missing block: B:4:0x0019, code lost:
    
        if (r0.length > 1) goto L8;
     */
    static {
        /*
            java.lang.String r0 = "support_smart_fps"
            r1 = 0
            boolean r0 = miui.util.FeatureParser.getBoolean(r0, r1)
            com.android.server.display.statistics.SwitchStatsHelper.SUPPORT_SMART_FPS = r0
            java.lang.String r0 = "screen_resolution_supported"
            int[] r0 = miui.util.FeatureParser.getIntArray(r0)
            com.android.server.display.statistics.SwitchStatsHelper.mScreenResolutionSupported = r0
            if (r0 == 0) goto L1c
            int r0 = r0.length
            r2 = 1
            if (r0 <= r2) goto L1c
            goto L1d
        L1c:
            r2 = r1
        L1d:
            com.android.server.display.statistics.SwitchStatsHelper.SUPPORT_RESOLUTION_SWITCH = r2
            java.lang.String r0 = "persist.sys.background_blur_supported"
            boolean r0 = android.os.SystemProperties.getBoolean(r0, r1)
            com.android.server.display.statistics.SwitchStatsHelper.BACKGROUND_BLUR_SUPPORTED = r0
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.statistics.SwitchStatsHelper.<clinit>():void");
    }

    public SwitchStatsHelper(Context context) {
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        readConfigFromDeviceFeature();
        loadSmartSwitches();
        registerSettingsObserver();
    }

    public static SwitchStatsHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SwitchStatsHelper(context);
        }
        return mInstance;
    }

    private void loadSmartSwitches() {
        if (SUPPORT_SMART_FPS) {
            boolean z = Settings.System.getInt(this.mResolver, IS_SMART_FPS, -1) == -1;
            this.mSmartRefreshRateEnable = z;
            if (z) {
                Settings.System.putIntForUser(this.mResolver, IS_SMART_FPS, 1, -2);
            }
        }
        if (SUPPORT_RESOLUTION_SWITCH) {
            boolean z2 = Settings.System.getInt(this.mResolver, MIUI_SCREEN_COMPAT, -1) == -1;
            this.mScreenCompat = z2;
            if (z2) {
                Settings.System.putIntForUser(this.mResolver, MIUI_SCREEN_COMPAT, 1, -2);
            }
        }
    }

    protected void registerSettingsObserver() {
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("sunlight_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_optimize_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(DC_BACK_LIGHT_SWITCH), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("ui_night_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_color_level"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_enabled"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("user_refresh_rate"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(IS_SMART_FPS), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("doze_always_on"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_true_tone"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(MIUI_SCREEN_COMPAT), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("adaptive_sleep"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("expert_data"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Global.getUriFor(SCREEN_ENHANCE_ENGINE_GALLERY_AI_MODE_STATUS), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(BACKGROUND_BLUR_ENABLE), false, this.mSettingsObserver, -1);
        loadSettings();
    }

    private void loadSettings() {
        this.mDcBacklightSettingsEnable = Settings.System.getIntForUser(this.mResolver, DC_BACK_LIGHT_SWITCH, -1, -2) == 1;
        this.mDarkModeSettingsEnable = Settings.Secure.getIntForUser(this.mResolver, "ui_night_mode", -1, -2) == 2;
        this.mSunlightSettingsEnable = Settings.System.getIntForUser(this.mResolver, "sunlight_mode", 0, -2) == 1;
        this.mAutoBrightnessSettingsEnable = Settings.System.getIntForUser(this.mResolver, "screen_brightness_mode", 0, -2) == 1;
        this.mScreenOptimizeSettingsMode = Settings.System.getIntForUser(this.mResolver, "screen_optimize_mode", MiuiSettings.ScreenEffect.DEFAULT_SCREEN_OPTIMIZE_MODE, -2);
        this.mReadModeSettingsEnable = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_enabled", 0, -2) != 0;
        this.mScreenColorLevel = Settings.System.getIntForUser(this.mResolver, "screen_color_level", -1, -2);
        this.mUserRefreshRate = Settings.System.getIntForUser(this.mResolver, "user_refresh_rate", -1, -2);
        this.mSmartRefreshRateEnable = Settings.System.getIntForUser(this.mResolver, IS_SMART_FPS, -1, -2) == 1;
        this.mDozeAlwaysOn = Settings.Secure.getIntForUser(this.mResolver, "doze_always_on", -1, -2) == 1;
        this.mScreenTrueToneEnable = Settings.System.getIntForUser(this.mResolver, "screen_true_tone", 0, -2) == 1;
        this.mScreenCompat = Settings.System.getIntForUser(this.mResolver, MIUI_SCREEN_COMPAT, -1, -2) == 1;
        this.mAdaptiveSleepEnable = Settings.System.getIntForUser(this.mResolver, "adaptive_sleep", -1, -2) == 1;
        updateColorGamutMode();
        this.mAiDisplayModeEnable = "true".equals(Settings.Global.getStringForUser(this.mResolver, SCREEN_ENHANCE_ENGINE_GALLERY_AI_MODE_STATUS, -2));
        this.mBackgroundBlurEnable = Settings.Secure.getInt(this.mResolver, BACKGROUND_BLUR_ENABLE, 0) == 1;
    }

    private void updateColorGamutMode() {
        if (this.mScreenOptimizeSettingsMode == 4) {
            ExpertData data = ExpertData.getFromDatabase(this.mContext);
            if (data == null) {
                data = ExpertData.getDefaultValue();
            }
            this.mColorGamutMode = data.getByCookie(0);
            return;
        }
        this.mColorGamutMode = -1;
    }

    private void readConfigFromDeviceFeature() {
        this.mSupportAdaptiveSleep = this.mContext.getResources().getBoolean(R.bool.config_allowStartActivityForLongPressOnPowerInSetup);
        this.mRefreshRateFromDeviceFeature = FeatureParser.getInteger("defaultFps", -1);
        this.mSupportAiDisplayMode = FeatureParser.getBoolean("support_AI_display", false);
        this.mSupportExpertMode = FeatureParser.getBoolean("support_display_expert_mode", false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSettingsChangeEvent(Uri uri) {
        String str;
        String str2;
        char c;
        String lastPathSegment = uri.getLastPathSegment();
        switch (lastPathSegment.hashCode()) {
            case -1763718536:
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                if (lastPathSegment.equals(str2)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1187891250:
                str = "adaptive_sleep";
                if (lastPathSegment.equals(str)) {
                    c = '\f';
                    str2 = "sunlight_mode";
                    break;
                }
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case -1168424008:
                if (lastPathSegment.equals("user_refresh_rate")) {
                    c = 7;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case -1111615120:
                if (lastPathSegment.equals("screen_true_tone")) {
                    c = '\n';
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case -879722010:
                if (lastPathSegment.equals(MIUI_SCREEN_COMPAT)) {
                    c = 11;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case -814132163:
                if (lastPathSegment.equals(SCREEN_ENHANCE_ENGINE_GALLERY_AI_MODE_STATUS)) {
                    c = 14;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case -693072130:
                if (lastPathSegment.equals("screen_brightness_mode")) {
                    c = 3;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case -101820922:
                if (lastPathSegment.equals("doze_always_on")) {
                    c = '\t';
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 140109694:
                if (lastPathSegment.equals(DC_BACK_LIGHT_SWITCH)) {
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    c = 0;
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 671593557:
                if (lastPathSegment.equals("screen_color_level")) {
                    c = 6;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 743595722:
                if (lastPathSegment.equals(BACKGROUND_BLUR_ENABLE)) {
                    c = 15;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 1186889717:
                if (lastPathSegment.equals("ui_night_mode")) {
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    c = 1;
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 1540120734:
                if (lastPathSegment.equals(IS_SMART_FPS)) {
                    c = '\b';
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 1951466655:
                if (lastPathSegment.equals("expert_data")) {
                    c = '\r';
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 1962624818:
                if (lastPathSegment.equals("screen_optimize_mode")) {
                    c = 4;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            case 2119453483:
                if (lastPathSegment.equals("screen_paper_mode_enabled")) {
                    c = 5;
                    str = "adaptive_sleep";
                    str2 = "sunlight_mode";
                    break;
                }
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
            default:
                str = "adaptive_sleep";
                str2 = "sunlight_mode";
                c = 65535;
                break;
        }
        String str3 = str;
        switch (c) {
            case 0:
                this.mDcBacklightSettingsEnable = Settings.System.getIntForUser(this.mResolver, DC_BACK_LIGHT_SWITCH, -1, -2) == 1;
                return;
            case 1:
                this.mDarkModeSettingsEnable = Settings.Secure.getIntForUser(this.mResolver, "ui_night_mode", -1, -2) == 2;
                return;
            case 2:
                this.mSunlightSettingsEnable = Settings.System.getIntForUser(this.mResolver, str2, 0, -2) == 1;
                return;
            case 3:
                this.mAutoBrightnessSettingsEnable = Settings.System.getIntForUser(this.mResolver, "screen_brightness_mode", 0, -2) == 1;
                return;
            case 4:
                this.mScreenOptimizeSettingsMode = Settings.System.getIntForUser(this.mResolver, "screen_optimize_mode", MiuiSettings.ScreenEffect.DEFAULT_SCREEN_OPTIMIZE_MODE, -2);
                updateColorGamutMode();
                return;
            case 5:
                this.mReadModeSettingsEnable = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_enabled", 0, -2) != 0;
                return;
            case 6:
                this.mScreenColorLevel = Settings.System.getIntForUser(this.mResolver, "screen_color_level", -1, -2);
                return;
            case 7:
                this.mUserRefreshRate = Settings.System.getIntForUser(this.mResolver, "user_refresh_rate", -1, -2);
                return;
            case '\b':
                this.mSmartRefreshRateEnable = Settings.System.getIntForUser(this.mResolver, IS_SMART_FPS, -1, -2) == 1;
                return;
            case '\t':
                this.mDozeAlwaysOn = Settings.Secure.getIntForUser(this.mResolver, "doze_always_on", -1, -2) == 1;
                return;
            case '\n':
                this.mScreenTrueToneEnable = Settings.System.getIntForUser(this.mResolver, "screen_true_tone", 0, -2) == 1;
                return;
            case 11:
                this.mScreenCompat = Settings.System.getIntForUser(this.mResolver, MIUI_SCREEN_COMPAT, -1, -2) == 1;
                return;
            case '\f':
                this.mAdaptiveSleepEnable = Settings.Secure.getIntForUser(this.mResolver, str3, -1, -2) == 1;
                return;
            case '\r':
                updateColorGamutMode();
                return;
            case 14:
                this.mAiDisplayModeEnable = "true".equals(Settings.Global.getStringForUser(this.mResolver, SCREEN_ENHANCE_ENGINE_GALLERY_AI_MODE_STATUS, -2));
                return;
            case 15:
                this.mBackgroundBlurEnable = Settings.Secure.getInt(this.mResolver, BACKGROUND_BLUR_ENABLE, 0) == 1;
                return;
            default:
                return;
        }
    }

    public boolean isDcBacklightSettingsEnable() {
        return this.mDcBacklightSettingsEnable;
    }

    public boolean isSunlightSettingsEnable() {
        return this.mSunlightSettingsEnable;
    }

    public boolean isDarkModeSettingsEnable() {
        return this.mDarkModeSettingsEnable;
    }

    public boolean isAutoBrightnessSettingsEnable() {
        return this.mAutoBrightnessSettingsEnable;
    }

    public boolean isReadModeSettingsEnable() {
        return this.mReadModeSettingsEnable;
    }

    public int getScreenOptimizeSettingsMode() {
        return this.mScreenOptimizeSettingsMode;
    }

    public List<BrightnessEvent.SwitchStatEntry> getAllSwitchStats() {
        updateSwitchStatsValue();
        return this.mSwitchStats;
    }

    public void updateSwitchStatsValue() {
        this.mSwitchStats.clear();
        BrightnessEvent.SwitchStatEntry statEvent = new BrightnessEvent.SwitchStatEntry(0, "screen_brightness_mode", this.mAutoBrightnessSettingsEnable);
        this.mSwitchStats.add(statEvent);
        BrightnessEvent.SwitchStatEntry statEvent2 = new BrightnessEvent.SwitchStatEntry(0, DC_BACK_LIGHT_SWITCH, this.mDcBacklightSettingsEnable);
        this.mSwitchStats.add(statEvent2);
        BrightnessEvent.SwitchStatEntry statEvent3 = new BrightnessEvent.SwitchStatEntry(0, "ui_night_mode", this.mDarkModeSettingsEnable);
        this.mSwitchStats.add(statEvent3);
        BrightnessEvent.SwitchStatEntry statEvent4 = new BrightnessEvent.SwitchStatEntry(0, "sunlight_mode", this.mSunlightSettingsEnable && !this.mAutoBrightnessSettingsEnable);
        this.mSwitchStats.add(statEvent4);
        BrightnessEvent.SwitchStatEntry statEvent5 = new BrightnessEvent.SwitchStatEntry(1, "screen_optimize_mode", this.mScreenOptimizeSettingsMode);
        this.mSwitchStats.add(statEvent5);
        BrightnessEvent.SwitchStatEntry statEvent6 = new BrightnessEvent.SwitchStatEntry(0, "screen_paper_mode_enabled", this.mReadModeSettingsEnable);
        this.mSwitchStats.add(statEvent6);
        BrightnessEvent.SwitchStatEntry statEvent7 = new BrightnessEvent.SwitchStatEntry(1, "screen_color_level", getColorLevelCode(this.mScreenColorLevel));
        this.mSwitchStats.add(statEvent7);
        BrightnessEvent.SwitchStatEntry statEvent8 = new BrightnessEvent.SwitchStatEntry(1, "dynamic_refresh_rate", getCurrentRefreshRate());
        this.mSwitchStats.add(statEvent8);
        BrightnessEvent.SwitchStatEntry statEvent9 = new BrightnessEvent.SwitchStatEntry(0, "doze_always_on", this.mDozeAlwaysOn);
        this.mSwitchStats.add(statEvent9);
        BrightnessEvent.SwitchStatEntry statEvent10 = new BrightnessEvent.SwitchStatEntry(0, "screen_true_tone", this.mScreenTrueToneEnable);
        this.mSwitchStats.add(statEvent10);
        BrightnessEvent.SwitchStatEntry statEvent11 = new BrightnessEvent.SwitchStatEntry(1, "screen_resolution", getCurrentScreenResolution());
        this.mSwitchStats.add(statEvent11);
        BrightnessEvent.SwitchStatEntry statEvent12 = new BrightnessEvent.SwitchStatEntry(0, MIUI_SCREEN_COMPAT, getCurrentScreenCompat());
        this.mSwitchStats.add(statEvent12);
        if (this.mSupportAdaptiveSleep) {
            BrightnessEvent.SwitchStatEntry statEvent13 = new BrightnessEvent.SwitchStatEntry(0, "adaptive_sleep", this.mAdaptiveSleepEnable);
            this.mSwitchStats.add(statEvent13);
        }
        if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            BrightnessEvent.SwitchStatEntry statEvent14 = new BrightnessEvent.SwitchStatEntry(1, "close_lid_display_setting", getCurrentCloseLidDisplaySetting());
            this.mSwitchStats.add(statEvent14);
        }
        if (this.mSupportExpertMode) {
            BrightnessEvent.SwitchStatEntry statEvent15 = new BrightnessEvent.SwitchStatEntry(1, COLOR_GAMUT_MODE, this.mColorGamutMode);
            this.mSwitchStats.add(statEvent15);
        }
        if (this.mSupportAiDisplayMode) {
            BrightnessEvent.SwitchStatEntry statEvent16 = new BrightnessEvent.SwitchStatEntry(0, AI_DISPLAY_MODE, this.mAiDisplayModeEnable);
            this.mSwitchStats.add(statEvent16);
        }
        if (BACKGROUND_BLUR_SUPPORTED) {
            BrightnessEvent.SwitchStatEntry statEvent17 = new BrightnessEvent.SwitchStatEntry(0, BACKGROUND_BLUR_ENABLE, this.mBackgroundBlurEnable);
            this.mSwitchStats.add(statEvent17);
        }
    }

    private int getCurrentRefreshRate() {
        if (SUPPORT_SMART_FPS && this.mSmartRefreshRateEnable) {
            return -120;
        }
        boolean defaultFps = SystemProperties.getBoolean("ro.vendor.fps.switch.default", false);
        if (!defaultFps) {
            return SystemProperties.getInt("persist.vendor.dfps.level", -1);
        }
        int i = this.mUserRefreshRate;
        return i != -1 ? i : this.mRefreshRateFromDeviceFeature;
    }

    private int getCurrentScreenResolution() {
        String screenResolution = SystemProperties.get(GameManagerServiceStubImpl.MIUI_RESOLUTION, (String) null);
        if (!TextUtils.isEmpty(screenResolution)) {
            return Integer.valueOf(screenResolution.split(",")[0]).intValue();
        }
        return 1440;
    }

    private int getCurrentCloseLidDisplaySetting() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "close_lid_display_setting", 1);
    }

    private boolean getCurrentScreenCompat() {
        boolean isQhd = false;
        if (SUPPORT_RESOLUTION_SWITCH) {
            int currentScreenResolution = getCurrentScreenResolution();
            int[] iArr = mScreenResolutionSupported;
            isQhd = currentScreenResolution == Math.max(iArr[0], iArr[1]);
        }
        return isQhd && this.mScreenCompat;
    }

    private int getColorLevelCode(int mScreenColorLevel) {
        int validRGB = 16777215 & mScreenColorLevel;
        int value_R = validRGB >> 16;
        int value_G = (validRGB >> 8) & 255;
        int value_B = validRGB & 255;
        if (mScreenColorLevel == -1) {
            return 0;
        }
        if (value_R == value_G && value_G == value_B) {
            return 0;
        }
        return value_R > value_G ? value_R > value_B ? 1 : 3 : value_B < value_G ? 2 : 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$0(Uri uri) {
            SwitchStatsHelper.this.handleSettingsChangeEvent(uri);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, final Uri uri) {
            SwitchStatsHelper.this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.statistics.SwitchStatsHelper$SettingsObserver$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SwitchStatsHelper.SettingsObserver.this.lambda$onChange$0(uri);
                }
            });
        }
    }
}
