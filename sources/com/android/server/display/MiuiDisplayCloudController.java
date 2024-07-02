package com.android.server.display;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.modules.utils.TypedXmlPullParser;
import com.android.modules.utils.TypedXmlSerializer;
import com.android.server.LocalServices;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.display.aiautobrt.config.AppCategory;
import com.android.server.display.aiautobrt.config.AppCategoryConfig;
import com.android.server.display.aiautobrt.config.PackageInfo;
import com.android.server.display.thermalbrightnesscondition.config.TemperatureBrightnessPair;
import com.android.server.display.thermalbrightnesscondition.config.ThermalBrightnessConfig;
import com.android.server.display.thermalbrightnesscondition.config.ThermalConditionItem;
import com.android.server.tof.TofManagerInternal;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import miui.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class MiuiDisplayCloudController {
    private static final String APP_CATEGORY_CONFIG = "app-category-config";
    private static final String AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE = "automatic_brightness_statistics_event_enable";
    private static final String AUTO_BRIGHTNESS_STATISTICS_EVENT_MODULE_NAME = "AutomaticBrightnessStatisticsEvent";
    private static final String BCBC_APP_CONFIG = "bcbc_app_config";
    private static final String BCBC_FEATURE_MODULE_NAME = "BCBCFeature";
    private static final String BRIGHTNESS_CURVE_OPTIMIZE_POLICY_DISABLE = "brightness_curve_optimize_policy_disable";
    private static final String BRIGHTNESS_CURVE_OPTIMIZE_POLICY_MODULE_NAME = "BrightnessCurveOptimizePolicy";
    private static final String BRIGHTNESS_STATISTICS_EVENTS_ENABLE = "brightness_statistics_events_enable";
    private static final String BRIGHTNESS_STATISTICS_EVENTS_NAME = "brightnessStatisticsEvents";
    public static final String CLOUD_BACKUP_DIR_NAME = "displayconfig";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE = "enabled";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM = "item";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE = "package";
    private static final String CLOUD_BACKUP_FILE_ATTRIBUTE_VALUE = "value";
    private static final String CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE = "automatic-brightness-statistics-event-enable";
    private static final String CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE_TAG = "automatic_brightness_statistics_event_enable";
    private static final String CLOUD_BACKUP_FILE_BCBC_TAG = "bcbc";
    private static final String CLOUD_BACKUP_FILE_BCBC_TAG_APP = "bcbc-app";
    private static final String CLOUD_BACKUP_FILE_BRIGHTNESS_CURVE_OPTIMIZE_POLICY_DISABLE = "brightness-curve-optimize-policy-disable";
    private static final String CLOUD_BACKUP_FILE_BRIGHTNESS_CURVE_OPTIMIZE_POLICY_TAG = "brightness_curve_optimize_policy_disable";
    private static final String CLOUD_BACKUP_FILE_BRIGHTNESS_STATISTICS_EVENTS_ENABLE = "brightness-statistics-events-enable";
    private static final String CLOUD_BACKUP_FILE_BRIGHTNESS_STATISTICS_EVENTS_TAG = "brightness_statistics_events";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_GAME = "game-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_IMAGE = "image-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_MAP = "map-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_READER = "reader-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_UNDEFINED = "undefined-category";
    private static final String CLOUD_BACKUP_FILE_CATEGORY_TAG_VIDEO = "video-category";
    private static final String CLOUD_BACKUP_FILE_CONTACTLESS_GESTURE_COMPONENTS_TAG = "gesture_component";
    private static final String CLOUD_BACKUP_FILE_CONTACTLESS_GESTURE_TAG = "contactless_gesture";
    private static final String CLOUD_BACKUP_FILE_CUSTOM_CURVE_DISABLE = "custom-curve-disable";
    private static final String CLOUD_BACKUP_FILE_CUSTOM_CURVE_TAG = "custom_curve";
    private static final String CLOUD_BACKUP_FILE_DISABLE_RESET_SHORT_TERM_MODEL = "disable-reset-short-term-model";
    private static final String CLOUD_BACKUP_FILE_DISABLE_RESET_SHORT_TERM_MODEL_TAG = "disable_reset_short_term_model";
    private static final String CLOUD_BACKUP_FILE_INDIVIDUAL_MODEL_DISABLE = "individual-model-disable";
    private static final String CLOUD_BACKUP_FILE_INDIVIDUAL_MODEL_TAG = "individual_model";
    private static final String CLOUD_BACKUP_FILE_MANUAL_BOOST_APP_ENABLE = "manual-boost-app-enable";
    private static final String CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_LIST = "manual-boost-disable-app-list";
    private static final String CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_TAG = "manual_boost_disable_app";
    private static final String CLOUD_BACKUP_FILE_NAME = "display_cloud_backup.xml";
    private static final String CLOUD_BACKUP_FILE_OUTDOOR_THERMAL_APP_CATEGORY_TAG = "outdoor_thermal_app_category";
    private static final String CLOUD_BACKUP_FILE_OUTDOOR_THERMAL_TAG_APP = "outdoor_thermal_app_category_list";
    private static final String CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_ENABLE = "override-brightness-policy-enable";
    private static final String CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_TAG = "override_brightness_policy_enable";
    private static final String CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG = "resolution_switch";
    private static final String CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK = "resolution_switch_process_black";
    private static final String CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT = "resolution_switch_process_protect";
    private static final String CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG = "rhythmic_app_category";
    private static final String CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_IMAGE = "rhythmic_app_category_image";
    private static final String CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_READ = "rhythmic_app_category_read";
    private static final String CLOUD_BACKUP_FILE_ROOT_ELEMENT = "display-config";
    private static final String CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_ENABLE = "short-term-model-enabled";
    private static final String CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_TAG = "short-term";
    private static final String CLOUD_BACKUP_FILE_THERMAL_BRIGHTNESS = "cloud_thermal_brightness_control.xml";
    private static final String CLOUD_BACKUP_FILE_THRESHOLD_SUNLIGHT_NIT_VALUE = "threshold-sunlight-nit-value";
    private static final String CLOUD_BACKUP_FILE_THRESHOLD_SUNLIGHT_NIT_VALUE_TAG = "threshold_sunlight_nit_value";
    private static final String CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG = "touch_cover_protection_game";
    private static final String CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG_APP = "touch_cover_protection_game_app";
    private static final String CLOUD_BAKUP_FILE_TEMPERATURE_GAP_TAG = "temperature_gap";
    private static final String CLOUD_BAKUP_FILE_TEMPERATURE_GAP_VALUE = "temperature-gap-value";
    public static final long CLOUD_EVENTS_APP_CATEGORY = 8;
    public static final long CLOUD_EVENTS_BRIGHTNESS_STATS = 1;
    public static final long CLOUD_EVENTS_CUSTOM_CURVE = 16;
    public static final long CLOUD_EVENTS_INDIVIDUAL_MODEL = 32;
    public static final long CLOUD_EVENTS_TEMPERATURE_GAP = 4;
    public static final long CLOUD_EVENTS_THERMAL_CONTROL = 2;
    private static final String CLOUD_FILE_APP_CATEGORY_CONFIG = "cloud_app_brightness_category.xml";
    private static final String CLOUD_THRESHOLD_SUNLIGHT_NIT = "threshold_sunlight_nit";
    private static final String CONTACTLESS_GESTURE_COMPONENTS = "gestureComponents";
    private static final String CONTACTLESS_GESTURE_MODULE_NAME = "ContactlessGestureFeature";
    private static final String CUSTOM_CURVE_DISABLE = "custom_curve_disable";
    private static final String CUSTOM_CURVE_DISABLE_MODULE_NAME = "CustomCurveDisable";
    private static final boolean DEBUG;
    private static final String DISABLE_RESET_SHORT_TERM_MODEL = "disable_reset_short_term_model";
    private static final String DISABLE_RESET_SHORT_TERM_MODEL_MODULE_NAME = "DisableResetShortTermModel";
    private static final String INDIVIDUAL_APP_CATEGORY_CONFIG_MODULE_NAME = "IndividualAppCategoryConfig";
    private static final String INDIVIDUAL_MODEL_DISABLE = "individual_model_disable";
    private static final String INDIVIDUAL_MODEL_DISABLE_MODULE_NAME = "IndividualModelDisable";
    private static final String MANUAL_BOOST_APP_ENABLE = "manual_boost_app_enable";
    private static final String MANUAL_BOOST_APP_ENABLE_MODULE_NAME = "ManualBoostAppEnable";
    private static final String MANUAL_BOOST_DISABLE_APP_LIST = "manual_boost_disable_app_list";
    private static final String MANUAL_BOOST_DISABLE_APP_MODULE_NAME = "ManualBoostDisableAppList";
    private static final String OUTDOOR_THERMAL_APP_CATEGORY_LIST_BACKUP_FILE = "outdoor_thermal_app_category_list_backup.xml";
    private static final String OUTDOOR_THERMAL_APP_LIST = "outdoor_thermal_app_list";
    private static final String OUTDOOR_THERMAL_APP_LIST_MODULE_NAME = "outdoorThermalAppList";
    private static final String OVERRIDE_BRIGHTNESS_POLICY_ENABLE = "override_brightness_policy_enable";
    private static final String OVERRIDE_BRIGHTNESS_POLICY_MODULE_NAME = "overrideBrightnessPolicy";
    private static final String PROCESS_RESOLUTION_SWITCH_BLACK_LIST = "process_resolution_switch_black_list";
    private static final String PROCESS_RESOLUTION_SWITCH_LIST = "process_resolution_switch_list";
    private static final String PROCESS_RESOLUTION_SWITCH_PROTECT_LIST = "process_resolution_switch_protect_list";
    private static final String RESOLUTION_SWITCH_PROCESS_LIST_BACKUP_FILE = "resolution_switch_process_list_backup.xml";
    private static final String RESOLUTION_SWITCH_PROCESS_LIST_MODEULE_NAME = "resolutionSwitchProcessList";
    private static final String RHYTHMIC_APP_CATEGORY_IMAGE_LIST = "rhythmic_app_category_image_list";
    private static final String RHYTHMIC_APP_CATEGORY_LIST_BACKUP_FILE = "rhythmic_app_category_list_backup.xml";
    private static final String RHYTHMIC_APP_CATEGORY_LIST_MODULE_NAME = "rhythmicAppCategoryList";
    private static final String RHYTHMIC_APP_CATEGORY_LIST_NAME = "rhythmic_app_category_list";
    private static final String RHYTHMIC_APP_CATEGORY_READ_LIST = "rhythmic_app_category_read_list";
    private static final String SHORT_TERM_MODEL_APP_CONFIG = "short_term_model_app_config";
    private static final String SHORT_TERM_MODEL_ENABLE = "short_term_model_enable";
    private static final String SHORT_TERM_MODEL_GAME_APP_LIST = "short_term_model_game_app_list";
    private static final String SHORT_TERM_MODEL_GLOBAL_APP_LIST = "short_term_model_global_app_list";
    private static final String SHORT_TERM_MODEL_IMAGE_APP_LIST = "short_term_model_image_app_list";
    private static final String SHORT_TERM_MODEL_MAP_APP_LIST = "short_term_model_map_app_list";
    private static final String SHORT_TERM_MODEL_MODULE_NAME = "shortTermModel";
    private static final String SHORT_TERM_MODEL_READER_APP_LIST = "short_term_model_reader_app_list";
    private static final String SHORT_TERM_MODEL_VIDEO_APP_LIST = "short_term_model_video_app_list";
    private static final String SHORT_TREM_MODEL_APP_MODULE_NAME = "shortTermModelAppList";
    private static final String TAG = "MiuiDisplayCloudController";
    public static final String TEMPERATURE_GAP_MODULE_NAME = "TemperatureGap";
    private static final String TEMPERATURE_GAP_VALUE = "temperature_gap_value";
    private static final float TEMPERATURE_GAP_VALUE_DEFAULT = 0.5f;
    private static final float THRESHOLD_SUNLIGHT_NIT_DEFAULT = 160.0f;
    private static final String THRESHOLD_SUNLIGHT_NIT_MODULE_NAME = "thresholdSunlightNit";
    private static final String TOUCH_COVER_PROTECTION_GAME_APP_LIST = "touch_cover_protection_game_app_list";
    private static final String TOUCH_COVER_PROTECTION_GAME_MODE = "TouchCoverProtectionGameMode";
    private AtomicFile mAppCategoryConfigCloudFile;
    private boolean mAutoBrightnessStatisticsEventEnable;
    private boolean mBrightnessCurveOptimizePolicyDisable;
    private boolean mBrightnessStatsEventsEnable;
    private Callback mCallback;
    private Map<String, Object> mCloudEventsData;
    private long mCloudEventsSummary;
    private Context mContext;
    private boolean mCustomCurveDisable;
    private boolean mDisableResetShortTermModel;
    private AtomicFile mFile;
    private Handler mHandler;
    private boolean mIndividualModelDisable;
    private boolean mManualBoostAppEnable;
    private boolean mOverrideBrightnessPolicyEnable;
    private boolean mShortTermModelEnable;
    private AtomicFile mThermalBrightnessCloudFile;
    private TofManagerInternal mTofManagerInternal;
    private float mTemperatureGap = 0.5f;
    private float mThresholdSunlightNit = THRESHOLD_SUNLIGHT_NIT_DEFAULT;
    private List<String> mShortTermModelGameList = new ArrayList();
    private List<String> mShortTermModelVideoList = new ArrayList();
    private List<String> mShortTermModelMapList = new ArrayList();
    private List<String> mShortTermModelImageList = new ArrayList();
    private List<String> mShortTermModelReaderList = new ArrayList();
    private List<String> mShortTermModelGlobalList = new ArrayList();
    private Map<Integer, List<String>> mShortTermModelAppMapper = new HashMap();
    private List<String> mShortTermModelCloudAppCategoryList = new ArrayList();
    private List<String> mBCBCAppList = new ArrayList();
    private List<String> mTouchCoverProtectionGameList = new ArrayList();
    private List<String> mManualBoostDisableAppList = new ArrayList();
    private ArrayList<Observer> mObservers = new ArrayList<>();
    private List<CloudListener> mCloudListeners = new ArrayList();
    private List<String> mResolutionSwitchProcessProtectList = new ArrayList();
    private List<String> mResolutionSwitchProcessBlackList = new ArrayList();
    private List<String> mRhythmicImageAppList = new ArrayList();
    private List<String> mRhythmicReadAppList = new ArrayList();
    private List<String> mGestureComponents = new ArrayList();
    private List<String> mOutdoorThermalAppList = new ArrayList();

    /* loaded from: classes.dex */
    public interface Callback {
        void notifyDisableResetShortTermModel(boolean z);

        void notifyThresholdSunlightNitChanged(float f);

        void updateOutdoorThermalAppCategoryList(List<String> list);
    }

    /* loaded from: classes.dex */
    public interface CloudListener {
        void onCloudUpdated(long j, Map<String, Object> map);
    }

    /* loaded from: classes.dex */
    public interface Observer {
        void update();
    }

    static {
        DEBUG = SystemProperties.getInt("debug.miui.display.cloud.dbg", 0) != 0;
    }

    public MiuiDisplayCloudController(Looper looper, Callback callback, Context context) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mCallback = callback;
        initialization();
        registerMiuiBrightnessCloudDataObserver();
    }

    private void initialization() {
        this.mCloudEventsData = new HashMap();
        this.mShortTermModelEnable = false;
        for (int i = 0; i < 6; i++) {
            this.mShortTermModelAppMapper.put(Integer.valueOf(i), new ArrayList());
        }
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_GAME_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_VIDEO_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_MAP_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_IMAGE_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_READER_APP_LIST);
        this.mShortTermModelCloudAppCategoryList.add(SHORT_TERM_MODEL_GLOBAL_APP_LIST);
        this.mFile = getFile(CLOUD_BACKUP_FILE_NAME);
        loadLocalCloudBackup();
    }

    private void registerMiuiBrightnessCloudDataObserver() {
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new AnonymousClass1(this.mHandler));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.MiuiDisplayCloudController$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        AnonymousClass1(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            boolean changed = MiuiDisplayCloudController.this.updateDataFromCloudControl();
            if (changed) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.display.MiuiDisplayCloudController$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiDisplayCloudController.AnonymousClass1.this.lambda$onChange$0();
                    }
                });
                MiuiDisplayCloudController.this.notifyAllObservers();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChange$0() {
            MiuiDisplayCloudController.this.syncLocalBackupFromCloud();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncLocalBackupFromCloud() {
        String str;
        FileOutputStream outputStream;
        TypedXmlSerializer out;
        if (this.mFile == null) {
            return;
        }
        try {
            Slog.d(TAG, "Start syncing local backup from cloud.");
            outputStream = this.mFile.startWrite();
            try {
                out = Xml.resolveSerializer(outputStream);
                try {
                    str = TAG;
                } catch (IOException e) {
                    e = e;
                    str = TAG;
                }
            } catch (IOException e2) {
                e = e2;
                str = TAG;
            }
            try {
                out.startDocument((String) null, true);
                out.setFeature(MiuiSizeCompatService.FAST_XML, true);
                out.startTag((String) null, CLOUD_BACKUP_FILE_ROOT_ELEMENT);
                out.startTag((String) null, CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_TAG);
                FileOutputStream outputStream2 = outputStream;
                try {
                    writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_ENABLE, this.mShortTermModelEnable);
                    try {
                        writeShortTermModelAppListToXml(this.mFile, outputStream2, out);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_BCBC_TAG);
                        outputStream2 = outputStream2;
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mBCBCAppList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_BCBC_TAG_APP);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_BCBC_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mResolutionSwitchProcessProtectList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mResolutionSwitchProcessBlackList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG);
                        out.startTag((String) null, "override_brightness_policy_enable");
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_ENABLE, this.mOverrideBrightnessPolicyEnable);
                        out.endTag((String) null, "override_brightness_policy_enable");
                        out.startTag((String) null, "automatic_brightness_statistics_event_enable");
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE, this.mAutoBrightnessStatisticsEventEnable);
                        out.endTag((String) null, "automatic_brightness_statistics_event_enable");
                        out.startTag((String) null, "disable_reset_short_term_model");
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_DISABLE_RESET_SHORT_TERM_MODEL, this.mDisableResetShortTermModel);
                        out.endTag((String) null, "disable_reset_short_term_model");
                        out.startTag((String) null, CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mTouchCoverProtectionGameList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG_APP);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mRhythmicImageAppList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_IMAGE);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mRhythmicReadAppList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_READ);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_CONTACTLESS_GESTURE_TAG);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mGestureComponents, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CONTACTLESS_GESTURE_COMPONENTS_TAG);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_CONTACTLESS_GESTURE_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_OUTDOOR_THERMAL_APP_CATEGORY_TAG);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mOutdoorThermalAppList, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, CLOUD_BACKUP_FILE_OUTDOOR_THERMAL_TAG_APP);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_OUTDOOR_THERMAL_APP_CATEGORY_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_TAG);
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_MANUAL_BOOST_APP_ENABLE, this.mManualBoostAppEnable);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_TAG);
                        writeElementOfAppListToXml(this.mFile, outputStream2, out, this.mManualBoostDisableAppList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_LIST);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_BRIGHTNESS_STATISTICS_EVENTS_TAG);
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_BRIGHTNESS_STATISTICS_EVENTS_ENABLE, this.mBrightnessStatsEventsEnable);
                    } catch (IOException e3) {
                        e = e3;
                        outputStream = outputStream2;
                    }
                    try {
                        writeShortTermModelAppListToXml(this.mFile, outputStream2, out);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_BRIGHTNESS_STATISTICS_EVENTS_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_THRESHOLD_SUNLIGHT_NIT_VALUE_TAG);
                        writeFeatureValueToXml(this.mFile, outputStream2, out, "value", CLOUD_BACKUP_FILE_THRESHOLD_SUNLIGHT_NIT_VALUE, Float.valueOf(this.mThresholdSunlightNit));
                        out.endTag((String) null, CLOUD_BACKUP_FILE_THRESHOLD_SUNLIGHT_NIT_VALUE_TAG);
                        out.startTag((String) null, CLOUD_BAKUP_FILE_TEMPERATURE_GAP_TAG);
                        writeFeatureValueToXml(this.mFile, outputStream2, out, "value", CLOUD_BAKUP_FILE_TEMPERATURE_GAP_VALUE, Float.valueOf(this.mTemperatureGap));
                        out.endTag((String) null, CLOUD_BAKUP_FILE_TEMPERATURE_GAP_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_INDIVIDUAL_MODEL_TAG);
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_INDIVIDUAL_MODEL_DISABLE, this.mIndividualModelDisable);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_INDIVIDUAL_MODEL_TAG);
                        out.startTag((String) null, CLOUD_BACKUP_FILE_CUSTOM_CURVE_TAG);
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, CLOUD_BACKUP_FILE_CUSTOM_CURVE_DISABLE, this.mCustomCurveDisable);
                        out.endTag((String) null, CLOUD_BACKUP_FILE_CUSTOM_CURVE_TAG);
                        out.startTag((String) null, "brightness_curve_optimize_policy_disable");
                        writeFeatureEnableToXml(this.mFile, outputStream2, out, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, "brightness_curve_optimize_policy_disable", this.mBrightnessCurveOptimizePolicyDisable);
                        out.endTag((String) null, "brightness_curve_optimize_policy_disable");
                        out.endTag((String) null, CLOUD_BACKUP_FILE_ROOT_ELEMENT);
                        out.endDocument();
                        outputStream2.flush();
                        this.mFile.finishWrite(outputStream2);
                    } catch (IOException e4) {
                        e = e4;
                        outputStream = outputStream2;
                        this.mFile.failWrite(outputStream);
                        Slog.e(str, "Failed to write local backup" + e);
                    }
                } catch (IOException e5) {
                    e = e5;
                    outputStream = outputStream2;
                }
            } catch (IOException e6) {
                e = e6;
                outputStream = outputStream;
                this.mFile.failWrite(outputStream);
                Slog.e(str, "Failed to write local backup" + e);
            }
        } catch (IOException e7) {
            e = e7;
            str = TAG;
            outputStream = null;
        }
    }

    private void writeFeatureEnableToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String attribute, String tag, boolean enable) {
        try {
            out.startTag((String) null, tag);
            out.attributeBoolean((String) null, attribute, enable);
            out.endTag((String) null, tag);
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write local backup of feature enable" + e);
        }
    }

    private void writeFeatureValueToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, String attribute, String tag, Number value) {
        try {
            out.startTag((String) null, tag);
            out.attributeFloat((String) null, attribute, value.floatValue());
            out.endTag((String) null, tag);
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write local backup of value" + e);
        }
    }

    private void writeShortTermModelAppListToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out) {
        for (int category = 0; category < 6; category++) {
            switch (category) {
                case 0:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelGlobalList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_UNDEFINED);
                    break;
                case 1:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelGameList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_GAME);
                    break;
                case 2:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelVideoList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_VIDEO);
                    break;
                case 3:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelMapList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_MAP);
                    break;
                case 4:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelImageList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_IMAGE);
                    break;
                case 5:
                    writeElementOfAppListToXml(writeFile, outStream, out, this.mShortTermModelReaderList, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, CLOUD_BACKUP_FILE_CATEGORY_TAG_READER);
                    break;
            }
        }
    }

    private void writeElementOfAppListToXml(AtomicFile writeFile, FileOutputStream outStream, TypedXmlSerializer out, List<String> list, String attribute, String tag) {
        try {
            for (String str : list) {
                out.startTag((String) null, tag);
                out.attribute((String) null, attribute, str);
                out.endTag((String) null, tag);
            }
        } catch (IOException e) {
            writeFile.failWrite(outStream);
            Slog.e(TAG, "Failed to write element of app list to xml" + e);
        }
    }

    /* JADX WARN: Failed to find 'out' block for switch in B:17:0x0168. Please report as an issue. */
    private void readCloudDataFromXml(InputStream stream) {
        try {
            Slog.d(TAG, "Start reading cloud data from xml.");
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            this.mCloudEventsSummary = 0L;
            this.mCloudEventsData.clear();
            while (true) {
                int type = parser.next();
                char c = 1;
                if (type != 1) {
                    if (type != 3 && type != 4) {
                        String tag = parser.getName();
                        switch (tag.hashCode()) {
                            case -1856325139:
                                if (tag.equals(CLOUD_BACKUP_FILE_SHORT_TERM_MODEL_ENABLE)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case -1571076377:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT)) {
                                    c = '\b';
                                    break;
                                }
                                break;
                            case -1535712689:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_MAP)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case -1443025529:
                                if (tag.equals(CLOUD_BACKUP_FILE_CONTACTLESS_GESTURE_COMPONENTS_TAG)) {
                                    c = 15;
                                    break;
                                }
                                break;
                            case -1393502937:
                                if (tag.equals(CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_IMAGE)) {
                                    c = 14;
                                    break;
                                }
                                break;
                            case -1367734768:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_VIDEO)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case -1167375239:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_GAME)) {
                                    break;
                                }
                                break;
                            case -1050832484:
                                if (tag.equals(CLOUD_BACKUP_FILE_THRESHOLD_SUNLIGHT_NIT_VALUE)) {
                                    c = 21;
                                    break;
                                }
                                break;
                            case -881248704:
                                if (tag.equals(CLOUD_BACKUP_FILE_MANUAL_BOOST_APP_ENABLE)) {
                                    c = 17;
                                    break;
                                }
                                break;
                            case -699460490:
                                if (tag.equals(CLOUD_BACKUP_FILE_OUTDOOR_THERMAL_TAG_APP)) {
                                    c = 18;
                                    break;
                                }
                                break;
                            case -688988814:
                                if (tag.equals(CLOUD_BACKUP_FILE_TOUCH_COVER_PROTECTION_GAME_TAG_APP)) {
                                    c = '\r';
                                    break;
                                }
                                break;
                            case -481905949:
                                if (tag.equals(CLOUD_BACKUP_FILE_BRIGHTNESS_CURVE_OPTIMIZE_POLICY_DISABLE)) {
                                    c = 25;
                                    break;
                                }
                                break;
                            case -436205034:
                                if (tag.equals(CLOUD_BACKUP_FILE_BCBC_TAG_APP)) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case -296770911:
                                if (tag.equals(CLOUD_BAKUP_FILE_TEMPERATURE_GAP_VALUE)) {
                                    c = 22;
                                    break;
                                }
                                break;
                            case -292879479:
                                if (tag.equals(CLOUD_BACKUP_FILE_BRIGHTNESS_STATISTICS_EVENTS_ENABLE)) {
                                    c = 20;
                                    break;
                                }
                                break;
                            case -10597714:
                                if (tag.equals(CLOUD_BACKUP_FILE_CUSTOM_CURVE_DISABLE)) {
                                    c = 24;
                                    break;
                                }
                                break;
                            case 231968992:
                                if (tag.equals(CLOUD_BACKUP_FILE_MANUAL_BOOST_DISABLE_APP_LIST)) {
                                    c = 16;
                                    break;
                                }
                                break;
                            case 456725192:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_READER)) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 925140042:
                                if (tag.equals(CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_READ)) {
                                    c = 19;
                                    break;
                                }
                                break;
                            case 1089152535:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK)) {
                                    c = '\t';
                                    break;
                                }
                                break;
                            case 1181635811:
                                if (tag.equals(CLOUD_BACKUP_FILE_OVERRIDE_BRIGHTNESS_POLICY_ENABLE)) {
                                    c = '\n';
                                    break;
                                }
                                break;
                            case 1291267516:
                                if (tag.equals(CLOUD_BACKUP_FILE_DISABLE_RESET_SHORT_TERM_MODEL)) {
                                    c = '\f';
                                    break;
                                }
                                break;
                            case 1778573798:
                                if (tag.equals(CLOUD_BACKUP_FILE_AUTO_BRIGHTNESS_STATISTICS_EVENT_ENABLE)) {
                                    c = 11;
                                    break;
                                }
                                break;
                            case 1855674587:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_UNDEFINED)) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case 1896537104:
                                if (tag.equals(CLOUD_BACKUP_FILE_INDIVIDUAL_MODEL_DISABLE)) {
                                    c = 23;
                                    break;
                                }
                                break;
                            case 2014899504:
                                if (tag.equals(CLOUD_BACKUP_FILE_CATEGORY_TAG_IMAGE)) {
                                    c = 4;
                                    break;
                                }
                                break;
                        }
                        c = 65535;
                        switch (c) {
                            case 0:
                                this.mShortTermModelEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case 1:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelGameList);
                                break;
                            case 2:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelVideoList);
                                break;
                            case 3:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelMapList);
                                break;
                            case 4:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelImageList);
                                break;
                            case 5:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelReaderList);
                                break;
                            case 6:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mShortTermModelGlobalList);
                                break;
                            case 7:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mBCBCAppList);
                                break;
                            case '\b':
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mResolutionSwitchProcessProtectList);
                                break;
                            case '\t':
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mResolutionSwitchProcessBlackList);
                                break;
                            case '\n':
                                this.mOverrideBrightnessPolicyEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case 11:
                                this.mAutoBrightnessStatisticsEventEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case '\f':
                                this.mDisableResetShortTermModel = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case '\r':
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mTouchCoverProtectionGameList);
                                break;
                            case 14:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mRhythmicImageAppList);
                                break;
                            case 15:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mGestureComponents);
                                break;
                            case 16:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_PACKAGE, this.mManualBoostDisableAppList);
                                break;
                            case 17:
                                this.mManualBoostAppEnable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                            case 18:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mOutdoorThermalAppList);
                                break;
                            case 19:
                                saveAppListFromXml(parser, CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM, this.mRhythmicReadAppList);
                                break;
                            case 20:
                                boolean attributeBoolean = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                this.mBrightnessStatsEventsEnable = attributeBoolean;
                                if (attributeBoolean) {
                                    this.mCloudEventsSummary |= 1;
                                    break;
                                }
                                break;
                            case 21:
                                float attributeFloat = parser.getAttributeFloat((String) null, "value", THRESHOLD_SUNLIGHT_NIT_DEFAULT);
                                this.mThresholdSunlightNit = attributeFloat;
                                this.mCallback.notifyThresholdSunlightNitChanged(attributeFloat);
                                break;
                            case 22:
                                float attributeFloat2 = parser.getAttributeFloat((String) null, "value", 0.5f);
                                this.mTemperatureGap = attributeFloat2;
                                this.mCloudEventsSummary |= 4;
                                this.mCloudEventsData.put(TEMPERATURE_GAP_MODULE_NAME, Float.valueOf(attributeFloat2));
                                break;
                            case 23:
                                boolean attributeBoolean2 = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                this.mIndividualModelDisable = attributeBoolean2;
                                if (attributeBoolean2) {
                                    this.mCloudEventsSummary |= 32;
                                    break;
                                }
                                break;
                            case 24:
                                boolean attributeBoolean3 = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                this.mCustomCurveDisable = attributeBoolean3;
                                if (attributeBoolean3) {
                                    this.mCloudEventsSummary |= 16;
                                    break;
                                }
                                break;
                            case 25:
                                this.mBrightnessCurveOptimizePolicyDisable = parser.getAttributeBoolean((String) null, CLOUD_BACKUP_FILE_ATTRIBUTE_ENABLE, false);
                                break;
                        }
                    }
                } else {
                    notifyResolutionSwitchListChanged();
                    notifyRhythmicAppCategoryListChanged();
                    notifyOutdoorThermalAppCategoryListChanged();
                    notifyContactlessGestureConfigChanged();
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse local cloud backup file" + e);
        }
    }

    private void notifyOutdoorThermalAppCategoryListChanged() {
        this.mCallback.updateOutdoorThermalAppCategoryList(this.mOutdoorThermalAppList);
    }

    private void loadResolutionSwitchListFromXml(InputStream stream) {
        char c;
        try {
            Slog.d(TAG, "Start loading resolution switch process list from xml.");
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            int currentTag = 0;
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type != 4 && type != 3) {
                        String tag = parser.getName();
                        switch (tag.hashCode()) {
                            case -1571076377:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_PROTECT)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 3242771:
                                if (tag.equals(CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 1089152535:
                                if (tag.equals(CLOUD_BACKUP_FILE_RESOLUTION_SWITCH_TAG_PROCESS_BLACK)) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        c = 65535;
                        switch (c) {
                            case 0:
                                currentTag = 1;
                                break;
                            case 1:
                                currentTag = 2;
                                break;
                            case 2:
                                if (currentTag == 1) {
                                    this.mResolutionSwitchProcessProtectList.add(parser.nextText());
                                    break;
                                } else if (currentTag == 2) {
                                    this.mResolutionSwitchProcessBlackList.add(parser.nextText());
                                    break;
                                }
                                break;
                        }
                    }
                } else {
                    notifyResolutionSwitchListChanged();
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse local cloud backup file" + e);
        }
    }

    private void loadRhythmicAppCategoryListFromXml(InputStream stream) {
        char c;
        try {
            Slog.d(TAG, "Start loading app category list from xml.");
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            int currentTag = 0;
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type != 4 && type != 3) {
                        String tag = parser.getName();
                        switch (tag.hashCode()) {
                            case -1393502937:
                                if (tag.equals(CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_IMAGE)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 3242771:
                                if (tag.equals(CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 925140042:
                                if (tag.equals(CLOUD_BACKUP_FILE_RHYTHMIC_APP_CATEGORY_TAG_READ)) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        c = 65535;
                        switch (c) {
                            case 0:
                                currentTag = 1;
                                break;
                            case 1:
                                currentTag = 2;
                                break;
                            case 2:
                                if (currentTag == 1) {
                                    this.mRhythmicImageAppList.add(parser.nextText());
                                    break;
                                } else if (currentTag == 2) {
                                    this.mRhythmicReadAppList.add(parser.nextText());
                                    break;
                                }
                                break;
                        }
                    }
                } else {
                    notifyRhythmicAppCategoryListChanged();
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse local cloud backup file" + e);
        }
    }

    private void loadOutdoorThermalAppCategoryListFromXml(InputStream stream) {
        char c;
        try {
            Slog.d(TAG, "Start loading app category list from xml.");
            TypedXmlPullParser parser = Xml.resolvePullParser(stream);
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type != 4 && type != 3) {
                        String tag = parser.getName();
                        switch (tag.hashCode()) {
                            case 3242771:
                                if (tag.equals(CLOUD_BACKUP_FILE_ATTRIBUTE_ITEM)) {
                                    c = 0;
                                    break;
                                }
                                break;
                        }
                        c = 65535;
                        switch (c) {
                            case 0:
                                this.mOutdoorThermalAppList.add(parser.nextText());
                                break;
                        }
                    }
                } else {
                    notifyOutdoorThermalAppCategoryListChanged();
                    return;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to parse local cloud backup file" + e);
        }
    }

    private void saveAppListFromXml(TypedXmlPullParser parser, String attribute, List<String> list) {
        if (parser.getAttributeValue((String) null, attribute) != null) {
            list.add(parser.getAttributeValue((String) null, attribute));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateDataFromCloudControl() {
        this.mCloudEventsSummary = 0L;
        this.mCloudEventsData.clear();
        boolean updated = updateShortTermModelState();
        boolean updated2 = updated | updateBCBCAppList() | updateResolutionSwitchList() | updateTouchProtectionGameList() | updateManualBoostDisableAppList() | updateManualBoostAppEnable() | updateOutdoorThermalAppList() | updateOverrideBrightnessPolicyEnable() | updateAutoBrightnessStatisticsEventEnableState() | updateDisableResetShortTermModelState() | updateRhythmicAppCategoryList() | updateContactlessGestureConfig() | updateBrightnessStatsEventsEnable() | updateThermalBrightnessConfig() | updateThresholdSunlightNitValue() | updateTemperatureGap() | updateCustomBrightnessAppConfig() | updateCustomCurveDisable() | updateIndividualModelDisable() | updateBrightnessCurveOptimizePolicyDisable();
        this.mCloudListeners.forEach(new Consumer() { // from class: com.android.server.display.MiuiDisplayCloudController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiDisplayCloudController.this.lambda$updateDataFromCloudControl$0((MiuiDisplayCloudController.CloudListener) obj);
            }
        });
        return updated2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateDataFromCloudControl$0(CloudListener l) {
        l.onCloudUpdated(this.mCloudEventsSummary, this.mCloudEventsData);
    }

    private void loadLocalCloudBackup() {
        AtomicFile atomicFile = this.mFile;
        if (atomicFile != null && atomicFile.exists()) {
            FileInputStream inputStream = null;
            try {
                try {
                    inputStream = this.mFile.openRead();
                    readCloudDataFromXml(inputStream);
                    saveShortTermModelAppComponent(null);
                    notifyAllObservers();
                } catch (IOException e) {
                    this.mFile.delete();
                    Slog.e(TAG, "Failed to read local cloud backup" + e);
                }
                return;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        loadResolutionSwitchBackUpFileFromXml();
        loadRhythmicAppCategoryBackUpFileFromXml();
        loadOutdoorThermalAppCategoryBackUpFileFromXml();
    }

    private void loadRhythmicAppCategoryBackUpFileFromXml() {
        FileInputStream inputStream = null;
        AtomicFile file = new AtomicFile(Environment.buildPath(Environment.getProductDirectory(), new String[]{"etc/displayconfig", RHYTHMIC_APP_CATEGORY_LIST_BACKUP_FILE}));
        try {
            try {
                inputStream = file.openRead();
                loadRhythmicAppCategoryListFromXml(inputStream);
            } catch (IOException e) {
                file.delete();
                Slog.e(TAG, "Failed to read local cloud backup" + e);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void loadResolutionSwitchBackUpFileFromXml() {
        FileInputStream inputStream = null;
        AtomicFile file = new AtomicFile(Environment.buildPath(Environment.getProductDirectory(), new String[]{"etc/displayconfig", RESOLUTION_SWITCH_PROCESS_LIST_BACKUP_FILE}));
        try {
            try {
                inputStream = file.openRead();
                loadResolutionSwitchListFromXml(inputStream);
            } catch (IOException e) {
                file.delete();
                Slog.e(TAG, "Failed to read local cloud backup" + e);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void loadOutdoorThermalAppCategoryBackUpFileFromXml() {
        FileInputStream inputStream = null;
        AtomicFile file = new AtomicFile(Environment.buildPath(Environment.getProductDirectory(), new String[]{"etc/displayconfig", OUTDOOR_THERMAL_APP_CATEGORY_LIST_BACKUP_FILE}));
        try {
            try {
                inputStream = file.openRead();
                loadOutdoorThermalAppCategoryListFromXml(inputStream);
            } catch (IOException e) {
                file.delete();
                Slog.e(TAG, "Failed to read local cloud backup" + e);
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private AtomicFile getFile(String fileName) {
        return new AtomicFile(new File(Environment.buildPath(Environment.getDataSystemDirectory(), new String[]{"displayconfig"}), fileName));
    }

    private boolean updateShortTermModelState() {
        return !DEBUG && updateShortTermModelEnable() && updateShortTermModelAppList();
    }

    private boolean updateShortTermModelAppList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), SHORT_TREM_MODEL_APP_MODULE_NAME, SHORT_TERM_MODEL_APP_CONFIG, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update short term model apps from cloud.");
            return false;
        }
        JSONArray jsonArray = data.json().optJSONArray(SHORT_TERM_MODEL_APP_CONFIG);
        if (jsonArray != null) {
            JSONObject jsonObject = jsonArray.optJSONObject(0);
            Slog.d(TAG, "Update short term model apps.");
            saveShortTermModelAppComponent(jsonObject);
            return true;
        }
        return true;
    }

    private boolean updateShortTermModelEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), SHORT_TERM_MODEL_MODULE_NAME, SHORT_TERM_MODEL_ENABLE, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update short term model enable from cloud.");
            return false;
        }
        this.mShortTermModelEnable = data.json().optBoolean(SHORT_TERM_MODEL_ENABLE);
        Slog.d(TAG, "Update short term model enable: " + this.mShortTermModelEnable);
        return true;
    }

    private boolean updateAutoBrightnessStatisticsEventEnableState() {
        if (DEBUG) {
            return false;
        }
        return updateStatisticsAutoBrightnessEventEnable();
    }

    private boolean updateStatisticsAutoBrightnessEventEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), AUTO_BRIGHTNESS_STATISTICS_EVENT_MODULE_NAME, "automatic_brightness_statistics_event_enable", (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to upload automatic brightness statistics event enable from cloud.");
            return false;
        }
        this.mAutoBrightnessStatisticsEventEnable = data.json().optBoolean("automatic_brightness_statistics_event_enable");
        Slog.d(TAG, "Update automatic brightness statistics event enable: " + this.mAutoBrightnessStatisticsEventEnable);
        return true;
    }

    private boolean updateDisableResetShortTermModelState() {
        return updateDisableResetShortTermModel();
    }

    private boolean updateDisableResetShortTermModel() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), DISABLE_RESET_SHORT_TERM_MODEL_MODULE_NAME, "disable_reset_short_term_model", (String) null, false);
        if (data == null || data.json() == null) {
            return false;
        }
        boolean optBoolean = data.json().optBoolean("disable_reset_short_term_model");
        this.mDisableResetShortTermModel = optBoolean;
        this.mCallback.notifyDisableResetShortTermModel(optBoolean);
        return true;
    }

    private void saveShortTermModelAppComponent(JSONObject jsonObject) {
        char c;
        for (String str : this.mShortTermModelCloudAppCategoryList) {
            switch (str.hashCode()) {
                case -1793386205:
                    if (str.equals(SHORT_TERM_MODEL_GAME_APP_LIST)) {
                        c = 0;
                        break;
                    }
                    break;
                case -1689699578:
                    if (str.equals(SHORT_TERM_MODEL_VIDEO_APP_LIST)) {
                        c = 1;
                        break;
                    }
                    break;
                case -1617342267:
                    if (str.equals(SHORT_TERM_MODEL_MAP_APP_LIST)) {
                        c = 2;
                        break;
                    }
                    break;
                case 970256626:
                    if (str.equals(SHORT_TERM_MODEL_READER_APP_LIST)) {
                        c = 4;
                        break;
                    }
                    break;
                case 1692934694:
                    if (str.equals(SHORT_TERM_MODEL_IMAGE_APP_LIST)) {
                        c = 3;
                        break;
                    }
                    break;
                case 2029224466:
                    if (str.equals(SHORT_TERM_MODEL_GLOBAL_APP_LIST)) {
                        c = 5;
                        break;
                    }
                    break;
            }
            c = 65535;
            switch (c) {
                case 0:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelGameList);
                    this.mShortTermModelAppMapper.put(1, this.mShortTermModelGameList);
                    break;
                case 1:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelVideoList);
                    this.mShortTermModelAppMapper.put(2, this.mShortTermModelVideoList);
                    break;
                case 2:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelMapList);
                    this.mShortTermModelAppMapper.put(3, this.mShortTermModelMapList);
                    break;
                case 3:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelImageList);
                    this.mShortTermModelAppMapper.put(4, this.mShortTermModelImageList);
                    break;
                case 4:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelReaderList);
                    this.mShortTermModelAppMapper.put(5, this.mShortTermModelReaderList);
                    break;
                case 5:
                    saveObjectAsListIfNeeded(jsonObject, str, this.mShortTermModelGlobalList);
                    this.mShortTermModelAppMapper.put(0, this.mShortTermModelGlobalList);
                    break;
            }
        }
    }

    private void saveObjectAsListIfNeeded(JSONObject jsonObject, String str, List<String> list) {
        if (jsonObject == null || list == null) {
            return;
        }
        JSONArray appArray = jsonObject.optJSONArray(str);
        if (appArray != null) {
            list.clear();
            for (int i = 0; i < appArray.length(); i++) {
                Object obj = appArray.opt(i);
                if (obj != null) {
                    list.add((String) obj);
                }
            }
            return;
        }
        Slog.d(TAG, "Such category apps are removed.");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isShortTermModelEnable() {
        return this.mShortTermModelEnable;
    }

    public boolean isAutoBrightnessStatisticsEventEnable() {
        return this.mAutoBrightnessStatisticsEventEnable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Map<Integer, List<String>> getShortTermModelAppMapper() {
        return this.mShortTermModelAppMapper;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("MiuiDisplayCloudController Configuration:");
        pw.println("  mShortTermModelEnable=" + this.mShortTermModelEnable);
        pw.println("  mShortTermModelAppMapper=" + this.mShortTermModelAppMapper);
        pw.println("  mBCBCAppList=" + this.mBCBCAppList);
        pw.println("  mTouchCoverProtectionGameList=" + this.mTouchCoverProtectionGameList);
        pw.println("  mResolutionSwitchProcessProtectList=" + this.mResolutionSwitchProcessProtectList);
        pw.println("  mResolutionSwitchProcessBlackList=" + this.mResolutionSwitchProcessBlackList);
        pw.println("  mRhythmicImageAppList=" + this.mRhythmicImageAppList);
        pw.println("  mRhythmicReadAppList=" + this.mRhythmicReadAppList);
        pw.println("  mGestureComponents=" + this.mGestureComponents);
        pw.println("  mOutdoorThermalAppList=" + this.mOutdoorThermalAppList);
        pw.println("  mManualBoostAppEnable=" + this.mManualBoostAppEnable);
        pw.println("  mManualBoostDisableAppList=" + this.mManualBoostDisableAppList);
        pw.println("  mOverrideBrightnessPolicyEnable=" + this.mOverrideBrightnessPolicyEnable);
        pw.println("  mAutoBrightnessStatisticsEventEnable=" + this.mAutoBrightnessStatisticsEventEnable);
        pw.println("  mDisableResetShortTermModel=" + this.mDisableResetShortTermModel);
        pw.println("  mBrightnessStatsEventsEnable=" + this.mBrightnessStatsEventsEnable);
        pw.println("  mThresholdSunlightNit=" + this.mThresholdSunlightNit);
        pw.println("  mCustomCurveDisable=" + this.mCustomCurveDisable);
        pw.println("  mIndividualModelDisable=" + this.mIndividualModelDisable);
        pw.println("  mBrightnessCurveOptimizePolicyDisable=" + this.mBrightnessCurveOptimizePolicyDisable);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<String> getBCBCAppList() {
        return this.mBCBCAppList;
    }

    private boolean updateBCBCAppList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), BCBC_FEATURE_MODULE_NAME, BCBC_APP_CONFIG, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update BCBC apps from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(BCBC_APP_CONFIG);
            if (appArray != null) {
                this.mBCBCAppList.clear();
                Slog.d(TAG, "Update BCBC apps.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mBCBCAppList.add((String) obj);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean updateResolutionSwitchList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), RESOLUTION_SWITCH_PROCESS_LIST_MODEULE_NAME, PROCESS_RESOLUTION_SWITCH_LIST, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update Resolution switch list from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(PROCESS_RESOLUTION_SWITCH_LIST);
            if (appArray != null) {
                JSONObject jsonObject = appArray.optJSONObject(0);
                Slog.d(TAG, "Update Resolution switch list from cloud");
                saveObjectAsListIfNeeded(jsonObject, PROCESS_RESOLUTION_SWITCH_PROTECT_LIST, this.mResolutionSwitchProcessProtectList);
                saveObjectAsListIfNeeded(jsonObject, PROCESS_RESOLUTION_SWITCH_BLACK_LIST, this.mResolutionSwitchProcessBlackList);
                notifyResolutionSwitchListChanged();
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isManualBoostAppEnable() {
        return this.mManualBoostAppEnable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<String> getManualBoostDisableAppList() {
        return this.mManualBoostDisableAppList;
    }

    private boolean updateManualBoostAppEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), MANUAL_BOOST_APP_ENABLE_MODULE_NAME, MANUAL_BOOST_APP_ENABLE, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update manual boost app enable from cloud.");
            return false;
        }
        this.mManualBoostAppEnable = data.json().optBoolean(MANUAL_BOOST_APP_ENABLE);
        Slog.d(TAG, "Update manual boost app enable: " + this.mManualBoostAppEnable);
        return true;
    }

    private boolean updateOutdoorThermalAppList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), OUTDOOR_THERMAL_APP_LIST_MODULE_NAME, OUTDOOR_THERMAL_APP_LIST, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update thermal brightness boost enable apps from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(OUTDOOR_THERMAL_APP_LIST);
            if (appArray != null) {
                this.mOutdoorThermalAppList.clear();
                Slog.d(TAG, "Update thermal brightness boost enable apps.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mOutdoorThermalAppList.add((String) obj);
                    }
                }
                notifyOutdoorThermalAppCategoryListChanged();
                return true;
            }
        }
        return false;
    }

    private boolean updateManualBoostDisableAppList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), MANUAL_BOOST_DISABLE_APP_MODULE_NAME, MANUAL_BOOST_DISABLE_APP_LIST, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update manual brightness boost disable apps from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(MANUAL_BOOST_DISABLE_APP_LIST);
            if (appArray != null) {
                this.mManualBoostDisableAppList.clear();
                Slog.d(TAG, "Update manual brightness boost disable apps.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mManualBoostDisableAppList.add((String) obj);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<String> getTouchCoverProtectionGameList() {
        return this.mTouchCoverProtectionGameList;
    }

    private boolean updateTouchProtectionGameList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), TOUCH_COVER_PROTECTION_GAME_MODE, TOUCH_COVER_PROTECTION_GAME_APP_LIST, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update game apps from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(TOUCH_COVER_PROTECTION_GAME_APP_LIST);
            if (appArray != null) {
                this.mTouchCoverProtectionGameList.clear();
                Slog.d(TAG, "Update game apps.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mTouchCoverProtectionGameList.add((String) obj);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void notifyResolutionSwitchListChanged() {
        DisplayManagerServiceStub.getInstance().updateResolutionSwitchList(this.mResolutionSwitchProcessProtectList, this.mResolutionSwitchProcessBlackList);
    }

    private boolean updateOverrideBrightnessPolicyEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), OVERRIDE_BRIGHTNESS_POLICY_MODULE_NAME, "override_brightness_policy_enable", (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update override brightness policy enable from cloud.");
            return false;
        }
        this.mOverrideBrightnessPolicyEnable = data.json().optBoolean("override_brightness_policy_enable");
        Slog.d(TAG, "Update override brightness policy enable: " + this.mOverrideBrightnessPolicyEnable);
        return true;
    }

    private boolean updateBrightnessStatsEventsEnable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), BRIGHTNESS_STATISTICS_EVENTS_NAME, BRIGHTNESS_STATISTICS_EVENTS_ENABLE, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update brightness statistics events switch from cloud.");
            return false;
        }
        boolean optBoolean = data.json().optBoolean(BRIGHTNESS_STATISTICS_EVENTS_ENABLE);
        this.mBrightnessStatsEventsEnable = optBoolean;
        if (optBoolean) {
            this.mCloudEventsSummary |= 1;
        }
        Slog.d(TAG, "Update brightness statistics events: " + this.mBrightnessStatsEventsEnable);
        return true;
    }

    private boolean updateThresholdSunlightNitValue() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), THRESHOLD_SUNLIGHT_NIT_MODULE_NAME, CLOUD_THRESHOLD_SUNLIGHT_NIT, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update threshold of sunlight mode nit from cloud.");
            return false;
        }
        float optDouble = (float) data.json().optDouble(CLOUD_THRESHOLD_SUNLIGHT_NIT);
        this.mThresholdSunlightNit = optDouble;
        this.mCallback.notifyThresholdSunlightNitChanged(optDouble);
        Slog.d(TAG, "Update threshold of sunlight mode nit: " + this.mThresholdSunlightNit);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isOverrideBrightnessPolicyEnable() {
        return this.mOverrideBrightnessPolicyEnable;
    }

    public void registerObserver(Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        if (!this.mObservers.contains(observer)) {
            this.mObservers.add(observer);
        }
    }

    public void unregisterObserver(Observer observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        this.mObservers.remove(observer);
    }

    public void notifyAllObservers() {
        Iterator<Observer> it = this.mObservers.iterator();
        while (it.hasNext()) {
            Observer observer = it.next();
            observer.update();
        }
    }

    private boolean updateThermalBrightnessConfig() {
        JSONObject rootObject;
        MiuiSettings.SettingsCloudData.CloudData data;
        JSONArray conditionArray;
        try {
            MiuiSettings.SettingsCloudData.CloudData data2 = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), "temperature_control", "thermal-brightness-config", (String) null, false);
            if (data2 != null && data2.json() != null) {
                JSONObject jsonObject = data2.json();
                JSONObject rootObject2 = jsonObject.getJSONObject("thermal-brightness-config");
                ThermalBrightnessConfig config = new ThermalBrightnessConfig();
                List<ThermalConditionItem> conditions = config.getThermalConditionItem();
                JSONArray conditionArray2 = rootObject2.getJSONArray("thermal-condition-item");
                int i = 0;
                while (i < conditionArray2.length()) {
                    ThermalConditionItem condition = new ThermalConditionItem();
                    JSONObject item = conditionArray2.getJSONObject(i);
                    int identifier = item.getInt("identifier");
                    String description = item.getString("description");
                    condition.setIdentifier(identifier);
                    condition.setDescription(description);
                    List<TemperatureBrightnessPair> pairList = condition.getTemperatureBrightnessPair();
                    JSONArray pairs = item.optJSONArray("temperature-brightness-pair");
                    if (pairs == null) {
                        rootObject = rootObject2;
                        data = data2;
                        conditionArray = conditionArray2;
                    } else {
                        int j = 0;
                        while (true) {
                            rootObject = rootObject2;
                            if (j >= pairs.length()) {
                                break;
                            }
                            JSONObject pairObj = pairs.getJSONObject(j);
                            MiuiSettings.SettingsCloudData.CloudData data3 = data2;
                            float minInclusive = Float.parseFloat(pairObj.getString("min-inclusive"));
                            JSONArray conditionArray3 = conditionArray2;
                            float maxExclusive = Float.parseFloat(pairObj.getString("max-exclusive"));
                            JSONObject item2 = item;
                            float nit = Float.parseFloat(pairObj.getString("nit"));
                            TemperatureBrightnessPair pair = new TemperatureBrightnessPair();
                            pair.setMinInclusive(minInclusive);
                            pair.setMaxExclusive(maxExclusive);
                            pair.setNit(nit);
                            pairList.add(pair);
                            j++;
                            rootObject2 = rootObject;
                            data2 = data3;
                            conditionArray2 = conditionArray3;
                            item = item2;
                        }
                        data = data2;
                        conditionArray = conditionArray2;
                    }
                    conditions.add(condition);
                    i++;
                    rootObject2 = rootObject;
                    data2 = data;
                    conditionArray2 = conditionArray;
                }
                this.mCloudEventsSummary |= 2;
                Slog.d(TAG, "Update thermal brightness config json: " + jsonObject);
                writeToFile(config);
                return true;
            }
            return false;
        } catch (JSONException e) {
            Slog.d(TAG, "Update thermal brightness config exception: " + e);
            return false;
        }
    }

    private boolean updateTemperatureGap() {
        try {
            MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), TEMPERATURE_GAP_MODULE_NAME, TEMPERATURE_GAP_VALUE, (String) null, false);
            if (data != null && data.json() != null) {
                float f = (float) data.json().getDouble(TEMPERATURE_GAP_VALUE);
                this.mTemperatureGap = f;
                this.mCloudEventsSummary |= 4;
                this.mCloudEventsData.put(TEMPERATURE_GAP_MODULE_NAME, Float.valueOf(f));
                Slog.d(TAG, "Update temperature gap : " + this.mTemperatureGap);
                return true;
            }
        } catch (JSONException e) {
            Slog.d(TAG, "Update temperature gap exception: " + e);
        }
        return false;
    }

    private boolean updateContactlessGestureConfig() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), CONTACTLESS_GESTURE_MODULE_NAME, CONTACTLESS_GESTURE_COMPONENTS, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update contactless gesture config.");
        } else {
            JSONArray appArray = data.json().optJSONArray(CONTACTLESS_GESTURE_COMPONENTS);
            if (appArray != null) {
                this.mGestureComponents.clear();
                Slog.d(TAG, "Update contactless gesture config.");
                for (int i = 0; i < appArray.length(); i++) {
                    Object obj = appArray.opt(i);
                    if (obj != null) {
                        this.mGestureComponents.add((String) obj);
                    }
                }
                notifyContactlessGestureConfigChanged();
                return true;
            }
        }
        return false;
    }

    private void notifyContactlessGestureConfigChanged() {
        this.mTofManagerInternal = (TofManagerInternal) LocalServices.getService(TofManagerInternal.class);
        Slog.e(TAG, "notifyContactlessGestureConfigChanged");
        TofManagerInternal tofManagerInternal = this.mTofManagerInternal;
        if (tofManagerInternal != null) {
            tofManagerInternal.updateGestureAppConfig(this.mGestureComponents);
        }
    }

    private boolean updateRhythmicAppCategoryList() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), RHYTHMIC_APP_CATEGORY_LIST_MODULE_NAME, RHYTHMIC_APP_CATEGORY_LIST_NAME, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to update app category list from cloud.");
        } else {
            JSONArray appArray = data.json().optJSONArray(RHYTHMIC_APP_CATEGORY_LIST_NAME);
            if (appArray != null) {
                JSONObject jsonObject = appArray.optJSONObject(0);
                Slog.d(TAG, "Update app category list from cloud");
                saveObjectAsListIfNeeded(jsonObject, RHYTHMIC_APP_CATEGORY_IMAGE_LIST, this.mRhythmicImageAppList);
                saveObjectAsListIfNeeded(jsonObject, RHYTHMIC_APP_CATEGORY_READ_LIST, this.mRhythmicReadAppList);
                notifyRhythmicAppCategoryListChanged();
                return true;
            }
        }
        return false;
    }

    private void notifyRhythmicAppCategoryListChanged() {
        DisplayManagerServiceStub.getInstance().updateRhythmicAppCategoryList(this.mRhythmicImageAppList, this.mRhythmicReadAppList);
    }

    public void writeToFile(ThermalBrightnessConfig config) {
        if (this.mThermalBrightnessCloudFile == null) {
            this.mThermalBrightnessCloudFile = getFile(CLOUD_BACKUP_FILE_THERMAL_BRIGHTNESS);
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = this.mThermalBrightnessCloudFile.startWrite();
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
            String str = null;
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
            fastXmlSerializer.startTag(null, "thermal-brightness-config");
            Iterator<ThermalConditionItem> it = config.getThermalConditionItem().iterator();
            while (it.hasNext()) {
                ThermalConditionItem condition = it.next();
                fastXmlSerializer.startTag(str, "thermal-condition-item");
                int identifier = condition.getIdentifier();
                Object description = condition.getDescription();
                writeTagToXml(fastXmlSerializer, "identifier", Integer.valueOf(identifier));
                writeTagToXml(fastXmlSerializer, "description", description);
                for (TemperatureBrightnessPair pair : condition.getTemperatureBrightnessPair()) {
                    fastXmlSerializer.startTag(str, "temperature-brightness-pair");
                    float minInclusive = pair.getMinInclusive();
                    float maxExclusive = pair.getMaxExclusive();
                    float nit = pair.getNit();
                    writeTagToXml(fastXmlSerializer, "min-inclusive", Float.valueOf(minInclusive));
                    writeTagToXml(fastXmlSerializer, "max-exclusive", Float.valueOf(maxExclusive));
                    writeTagToXml(fastXmlSerializer, "nit", Float.valueOf(nit));
                    fastXmlSerializer.endTag(null, "temperature-brightness-pair");
                    it = it;
                    str = null;
                }
                fastXmlSerializer.endTag(null, "thermal-condition-item");
                it = it;
                str = null;
            }
            fastXmlSerializer.endTag(null, "thermal-brightness-config");
            fastXmlSerializer.endDocument();
            outputStream.flush();
            this.mThermalBrightnessCloudFile.finishWrite(outputStream);
        } catch (IOException e) {
            this.mThermalBrightnessCloudFile.failWrite(outputStream);
            e.printStackTrace();
        }
    }

    private void writeTagToXml(XmlSerializer out, String tag, Object value) throws IOException {
        out.startTag(null, tag);
        out.text(String.valueOf(value));
        out.endTag(null, tag);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addCloudListener(CloudListener listener) {
        if (!this.mCloudListeners.contains(listener)) {
            this.mCloudListeners.add(listener);
            listener.onCloudUpdated(this.mCloudEventsSummary, this.mCloudEventsData);
        }
    }

    private boolean updateCustomBrightnessAppConfig() {
        JSONObject rootObject;
        String str = "name";
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), INDIVIDUAL_APP_CATEGORY_CONFIG_MODULE_NAME, APP_CATEGORY_CONFIG, (String) null, false);
        if (data != null && data.json() != null) {
            try {
                JSONObject jsonObject = data.json();
                JSONObject rootObject2 = jsonObject.getJSONObject(APP_CATEGORY_CONFIG);
                AppCategoryConfig config = new AppCategoryConfig();
                List<AppCategory> categories = config.getCategory();
                JSONArray jsonCategoryArray = rootObject2.getJSONArray("category");
                int i = 0;
                while (i < jsonCategoryArray.length()) {
                    AppCategory category = new AppCategory();
                    List<PackageInfo> pkgInfoList = category.getPkg();
                    JSONObject categoryItem = jsonCategoryArray.getJSONObject(i);
                    int categoryId = categoryItem.getInt("id");
                    String categoryName = categoryItem.getString(str);
                    JSONArray pkgArray = categoryItem.getJSONArray("pkg");
                    MiuiSettings.SettingsCloudData.CloudData data2 = data;
                    int j = 0;
                    while (true) {
                        rootObject = rootObject2;
                        try {
                            if (j < pkgArray.length()) {
                                JSONObject jsonPkgInfoItem = pkgArray.getJSONObject(j);
                                String pkgName = jsonPkgInfoItem.optString(str);
                                PackageInfo pkgInfo = new PackageInfo();
                                pkgInfo.setCateId(categoryId);
                                pkgInfo.setName(pkgName);
                                pkgInfoList.add(pkgInfo);
                                j++;
                                rootObject2 = rootObject;
                                str = str;
                            }
                        } catch (JSONException e) {
                            e = e;
                            Slog.e(TAG, "Update  custom app category config exception: " + e);
                            e.printStackTrace();
                            return false;
                        }
                    }
                    category.setId(categoryId);
                    category.setName(categoryName);
                    categories.add(category);
                    i++;
                    rootObject2 = rootObject;
                    data = data2;
                    str = str;
                }
                if (DEBUG) {
                    Slog.d(TAG, "Update custom app category config json: " + jsonObject);
                }
                writeAppCategoryConfigToFile(config);
                this.mCloudEventsSummary |= 8;
                return true;
            } catch (JSONException e2) {
                e = e2;
            }
        } else {
            return false;
        }
    }

    private void writeAppCategoryConfigToFile(AppCategoryConfig config) {
        if (this.mAppCategoryConfigCloudFile == null) {
            this.mAppCategoryConfigCloudFile = getFile(CLOUD_FILE_APP_CATEGORY_CONFIG);
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = this.mAppCategoryConfigCloudFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
            String str = null;
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
            fastXmlSerializer.startTag(null, APP_CATEGORY_CONFIG);
            Iterator<AppCategory> it = config.getCategory().iterator();
            while (it.hasNext()) {
                AppCategory category = it.next();
                fastXmlSerializer.startTag(str, "category");
                int id = category.getId();
                String name = category.getName();
                fastXmlSerializer.attribute(str, "id", Integer.toString(id));
                fastXmlSerializer.attribute(str, "name", name);
                for (PackageInfo appInfo : category.getPkg()) {
                    fastXmlSerializer.startTag(str, "pkg");
                    int cateId = appInfo.getCateId();
                    String pkgName = appInfo.getName();
                    fastXmlSerializer.attribute(null, "cateId", Integer.toString(cateId));
                    fastXmlSerializer.attribute(null, "name", pkgName);
                    fastXmlSerializer.endTag(null, "pkg");
                    it = it;
                    category = category;
                    str = null;
                }
                fastXmlSerializer.endTag(null, "category");
                it = it;
                str = null;
            }
            fastXmlSerializer.endTag(null, APP_CATEGORY_CONFIG);
            fastXmlSerializer.endDocument();
            outputStream.flush();
            this.mAppCategoryConfigCloudFile.finishWrite(outputStream);
        } catch (IOException e) {
            this.mAppCategoryConfigCloudFile.failWrite(outputStream);
            e.printStackTrace();
        }
    }

    private boolean updateCustomCurveDisable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), CUSTOM_CURVE_DISABLE_MODULE_NAME, CUSTOM_CURVE_DISABLE, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to upload custom curve disable from cloud.");
            return false;
        }
        boolean optBoolean = data.json().optBoolean(CUSTOM_CURVE_DISABLE);
        this.mCustomCurveDisable = optBoolean;
        if (optBoolean) {
            this.mCloudEventsSummary |= 16;
        }
        Slog.d(TAG, "Update custom curve disable: " + this.mCustomCurveDisable);
        return true;
    }

    private boolean updateBrightnessCurveOptimizePolicyDisable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), BRIGHTNESS_CURVE_OPTIMIZE_POLICY_MODULE_NAME, "brightness_curve_optimize_policy_disable", (String) null, false);
        if (data == null || data.json() == null) {
            return false;
        }
        this.mBrightnessCurveOptimizePolicyDisable = data.json().optBoolean("brightness_curve_optimize_policy_disable");
        return true;
    }

    private boolean updateIndividualModelDisable() {
        MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), INDIVIDUAL_MODEL_DISABLE_MODULE_NAME, INDIVIDUAL_MODEL_DISABLE, (String) null, false);
        if (data == null || data.json() == null) {
            Slog.w(TAG, "Failed to upload individual model disable from cloud.");
            return false;
        }
        boolean optBoolean = data.json().optBoolean(INDIVIDUAL_MODEL_DISABLE);
        this.mIndividualModelDisable = optBoolean;
        if (optBoolean) {
            this.mCloudEventsSummary |= 32;
        }
        Slog.d(TAG, "Update individual model disable: " + this.mIndividualModelDisable);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBrightnessCurveOptimizePolicyDisable() {
        return this.mBrightnessCurveOptimizePolicyDisable;
    }
}
