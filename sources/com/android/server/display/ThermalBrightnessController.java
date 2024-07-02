package com.android.server.display;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseArrayMap;
import com.android.internal.os.BackgroundThread;
import com.android.server.display.MiuiDisplayCloudController;
import com.android.server.display.ThermalBrightnessController;
import com.android.server.display.thermalbrightnesscondition.config.TemperatureBrightnessPair;
import com.android.server.display.thermalbrightnesscondition.config.ThermalBrightnessConfig;
import com.android.server.display.thermalbrightnesscondition.config.ThermalConditionItem;
import com.android.server.display.thermalbrightnesscondition.config.XmlParser;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class ThermalBrightnessController implements MiuiDisplayCloudController.CloudListener {
    private static final String CLOUD_BACKUP_CONFIG_FILE = "cloud_thermal_brightness_control.xml";
    private static final String CLOUD_BACKUP_FILE_NAME = "display_cloud_backup.xml";
    private static final String CLOUD_BAKUP_FILE_TEMP_GAP_VALUE = "temperature-gap-value";
    private static final int CONDITION_ID_FOR_NTC_THERMAL = -3;
    protected static boolean DEBUG = false;
    private static final String DEFAULT_CONFIG_FILE = "thermal_brightness_control.xml";
    private static final String DOLBY_OVERRIDE_DESC = "DOLBY-VISION";
    private static final String ETC_DIR = "etc";
    private static final int EVENT_CONDITION_CHANGE = 2;
    private static final int EVENT_NTC_TEMPERATURE_CHANGE = 16;
    private static final int EVENT_OUTDOOR_CHANGE = 4;
    private static final int EVENT_SAFETY_BRIGHTNESS_CHANGE = 8;
    private static final int EVENT_TEMPERATURE_CHANGE = 1;
    private static final String FILE_NTC_TEMPERATURE = "display_therm_temp";
    private static final String FILE_SAFETY_BRIGHTNESS = "thermal_max_brightness";
    private static final String FILE_SKIN_TEMPERATURE = "board_sensor_temp";
    private static final String FILE_THERMAL_CONDITION_ID = "sconfig";
    private static final String MAX_BRIGHTNESS_FILE = "/sys/class/thermal/thermal_message/board_sensor_temp";
    private static final int NTC_TEMPERATURE_GAP = 1;
    private static final int OUTDOOR_THERMAL_ID = -4;
    private static final String TAG = "ThermalBrightnessController";
    private static final String THERMAL_BRIGHTNESS_CONFIG_DIR = "displayconfig";
    private static final String THERMAL_CONFIG_DIR = "/sys/class/thermal/thermal_message";
    private Callback mCallback;
    private Context mContext;
    private volatile int mCurrentActualThermalConditionId;
    private volatile float mCurrentAppliedNtcTemperature;
    private volatile float mCurrentAppliedTemperature;
    private volatile int mCurrentAppliedThermalConditionId;
    private ThermalConditionItem mCurrentCondition;
    private volatile float mCurrentNtcTemperature;
    private volatile float mCurrentTemperature;
    private ThermalConditionItem mDefaultCondition;
    private DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private String mForegroundAppPackageName;
    private volatile boolean mIsDolbyEnabled;
    private volatile boolean mIsHdrLayerPresent;
    private volatile boolean mIsInOutdoorHighTempState;
    private boolean mIsThermalBrightnessBoostEnable;
    private final float mMinBrightnessInOutdoorHighTemperature;
    private volatile boolean mNeedOverrideCondition;
    private boolean mNeedThermalBrightnessBoost;
    private OutdoorDetector mOutdoorDetector;
    private List<String> mOutdoorThermalAppList;
    private ThermalConditionItem mOverrideCondition;
    private volatile boolean mScreenOn;
    private final SettingsObserver mSettingsObserver;
    private boolean mSunlightModeActive;
    private final float mTemperatureLevelCritical;
    private final float mTemperatureLevelEmergency;
    private final boolean mThermalBrightnessControlNtcAvailable;
    private final float mThresholdLuxEnterOutdoor;
    private boolean mUseAutoBrightness;
    private String mLoadedFileFrom = "null";
    private float mCurrentMaxThermalBrightness = Float.NaN;
    protected List<ThermalConditionItem> mThermalConditions = new ArrayList();
    private SparseArrayMap<Integer, Float> mCacheInfo = new SparseArrayMap<>();
    private SparseArray<Float> mMiniTemperatureThresholds = new SparseArray<>();
    private SparseArray<ThermalConditionItem> mConditionIdMaps = new SparseArray<>();
    private float mThermalSafetyBrightness = Float.NaN;
    private float mTemperatureGap = 0.5f;
    private ArrayList<ThermalListener> mThermalListener = new ArrayList<>();
    private final Runnable mLoadFileRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.loadThermalConfig();
        }
    };
    private final Runnable mUpdateOverrideConditionRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.updateOverrideCondition();
        }
    };
    private final Runnable mUpdateOutdoorRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.checkOutdoorState();
        }
    };
    private final Runnable mUpdateConditionRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda3
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.lambda$new$1();
        }
    };
    private final Runnable mUpdateTemperatureRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda4
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.lambda$new$2();
        }
    };
    private final Runnable mUpdateSafetyBrightnessRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda5
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.lambda$new$3();
        }
    };
    private final Runnable mUpdateNtcTemperatureRunnable = new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda6
        @Override // java.lang.Runnable
        public final void run() {
            ThermalBrightnessController.this.lambda$new$4();
        }
    };
    private Handler mBackgroundHandler = new Handler(BackgroundThread.get().getLooper());

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface Callback {
        void onThermalBrightnessChanged(float f);
    }

    /* loaded from: classes.dex */
    public interface ThermalListener {
        void thermalConfigChanged(List<ThermalConditionItem> list);
    }

    private native void nativeInit();

    public ThermalBrightnessController(Context context, Looper looper, Callback callback, DisplayPowerControllerImpl displayPowerControllerImpl) {
        this.mContext = context;
        this.mCallback = callback;
        this.mDisplayPowerControllerImpl = displayPowerControllerImpl;
        SettingsObserver settingsObserver = new SettingsObserver(this.mBackgroundHandler);
        this.mSettingsObserver = settingsObserver;
        this.mTemperatureLevelEmergency = this.mContext.getResources().getFloat(285671480);
        this.mTemperatureLevelCritical = this.mContext.getResources().getFloat(285671479);
        this.mMinBrightnessInOutdoorHighTemperature = this.mContext.getResources().getFloat(285671477);
        float f = this.mContext.getResources().getFloat(285671482);
        this.mThresholdLuxEnterOutdoor = f;
        this.mThermalBrightnessControlNtcAvailable = this.mContext.getResources().getBoolean(285540488);
        this.mIsThermalBrightnessBoostEnable = this.mContext.getResources().getBoolean(285540486);
        this.mOutdoorDetector = new OutdoorDetector(context, this, f);
        int screenBrightnessModeSetting = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2);
        this.mUseAutoBrightness = screenBrightnessModeSetting == 1;
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ThermalBrightnessController.this.loadThermalConfig();
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, settingsObserver, -1);
        nativeInit();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadThermalConfig() {
        this.mThermalConditions.clear();
        ThermalBrightnessConfig config = loadConfigFromFile();
        if (config == null) {
            Slog.w(TAG, "config file was not found!");
            return;
        }
        Slog.i(TAG, "load thermal config from: " + this.mLoadedFileFrom);
        this.mThermalConditions.addAll(config.getThermalConditionItem());
        this.mThermalListener.forEach(new Consumer() { // from class: com.android.server.display.ThermalBrightnessController$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ThermalBrightnessController.this.lambda$loadThermalConfig$0((ThermalBrightnessController.ThermalListener) obj);
            }
        });
        for (ThermalConditionItem condition : this.mThermalConditions) {
            int id = condition.getIdentifier();
            String description = condition.getDescription();
            if (id == 0) {
                this.mDefaultCondition = condition;
            }
            if (DOLBY_OVERRIDE_DESC.equals(description)) {
                this.mOverrideCondition = condition;
            }
            this.mConditionIdMaps.append(id, condition);
            List<TemperatureBrightnessPair> pairs = condition.getTemperatureBrightnessPair();
            float miniTemperature = Float.NaN;
            for (TemperatureBrightnessPair pair : pairs) {
                float minTemp = pair.getMinInclusive();
                if (Float.isNaN(miniTemperature) || minTemp < miniTemperature) {
                    miniTemperature = minTemp;
                }
            }
            this.mMiniTemperatureThresholds.append(id, Float.valueOf(miniTemperature));
        }
        updateConditionState(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadThermalConfig$0(ThermalListener l) {
        l.thermalConfigChanged(this.mThermalConditions);
    }

    protected void onTemperatureEvent(int event) {
        if ((event & 2) != 0) {
            this.mBackgroundHandler.removeCallbacks(this.mUpdateTemperatureRunnable);
            this.mBackgroundHandler.post(this.mUpdateTemperatureRunnable);
        }
    }

    protected void onConditionEvent(int event) {
        if ((event & 2) != 0) {
            this.mBackgroundHandler.removeCallbacks(this.mUpdateConditionRunnable);
            this.mBackgroundHandler.post(this.mUpdateConditionRunnable);
        }
    }

    protected void onSafetyBrightnessEvent(int event) {
        if ((event & 2) != 0) {
            this.mBackgroundHandler.removeCallbacks(this.mUpdateSafetyBrightnessRunnable);
            this.mBackgroundHandler.post(this.mUpdateSafetyBrightnessRunnable);
        }
    }

    protected void onNtcTemperatureEvent(int event) {
        if ((event & 2) != 0) {
            this.mBackgroundHandler.removeCallbacks(this.mUpdateNtcTemperatureRunnable);
            this.mBackgroundHandler.post(this.mUpdateNtcTemperatureRunnable);
        }
    }

    private boolean updateSkinTemperature() {
        String value = readThermalConfigFromNode(FILE_SKIN_TEMPERATURE);
        if (value != null) {
            try {
                float temperature = Float.parseFloat(value) / 1000.0f;
                if (DEBUG) {
                    Slog.d(TAG, "updateSkinTemperature: temperature: " + temperature);
                }
                this.mCurrentTemperature = temperature;
                if (!asSameTemperature(temperature, this.mCurrentAppliedTemperature, false)) {
                    this.mCurrentAppliedTemperature = temperature;
                    checkOutdoorState();
                    updateThermalBrightnessBoostCondition();
                    Slog.d(TAG, "updateSkinTemperature: actual temperature: " + this.mCurrentTemperature + ", applied temperature: " + this.mCurrentAppliedTemperature);
                    return true;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void updateConditionState(int event) {
        boolean changed = false;
        if (event == 1) {
            changed = updateSkinTemperature();
        } else if (event == 2) {
            changed = false | updateThermalCondition();
        } else if (event == 4) {
            changed = false | updateOutdoorState();
            this.mDisplayPowerControllerImpl.notifyOutDoorHighTempState(this.mIsInOutdoorHighTempState);
        } else if (event == 8) {
            changed = false | updateSafetyBrightness();
        } else if (event == 16) {
            changed = false | updateNtcTemperature();
        }
        if (changed) {
            boolean thermalBrightnessChanged = updateThermalBrightnessIfNeeded();
            this.mDisplayPowerControllerImpl.startDetailThermalUsageStatsOnThermalChanged(this.mCurrentAppliedThermalConditionId, this.mCurrentAppliedTemperature, thermalBrightnessChanged);
            if (thermalBrightnessChanged) {
                this.mCallback.onThermalBrightnessChanged(this.mCurrentMaxThermalBrightness);
            }
        }
    }

    private boolean updateThermalCondition() {
        String value = readThermalConfigFromNode(FILE_THERMAL_CONDITION_ID);
        if (value != null) {
            try {
                int id = Integer.parseInt(value);
                if (this.mCurrentAppliedThermalConditionId != id || this.mNeedOverrideCondition || this.mNeedThermalBrightnessBoost) {
                    Slog.d(TAG, "updateThermalCondition: condition changed: " + id);
                    this.mCurrentActualThermalConditionId = id;
                    if (this.mNeedThermalBrightnessBoost) {
                        id = -4;
                    }
                    ThermalConditionItem condition = this.mConditionIdMaps.get(id);
                    if (this.mNeedOverrideCondition) {
                        condition = this.mOverrideCondition;
                    }
                    if (condition == null && this.mDefaultCondition != null) {
                        Slog.w(TAG, "Thermal condition (id=" + this.mCurrentActualThermalConditionId + ") is not configured in file, apply default condition!");
                        condition = this.mDefaultCondition;
                    }
                    ThermalConditionItem thermalConditionItem = this.mCurrentCondition;
                    if (condition != thermalConditionItem) {
                        if (thermalConditionItem != null && condition != null) {
                            Slog.d(TAG, "condition changed from: [id=" + this.mCurrentCondition.getIdentifier() + ", name=" + this.mCurrentCondition.getDescription() + "] to [id=" + condition.getIdentifier() + ", name=" + condition.getDescription() + "]");
                        }
                        this.mCurrentCondition = condition;
                        this.mCurrentAppliedThermalConditionId = condition.getIdentifier();
                        return true;
                    }
                    return false;
                }
                return false;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private boolean updateSafetyBrightness() {
        String value = readThermalConfigFromNode(FILE_SAFETY_BRIGHTNESS);
        if (value != null) {
            float nit = Float.parseFloat(value);
            if (this.mThermalSafetyBrightness != nit) {
                this.mThermalSafetyBrightness = nit == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X ? Float.NaN : nit;
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean updateNtcTemperature() {
        String value = readThermalConfigFromNode(FILE_NTC_TEMPERATURE);
        if (value != null) {
            try {
                float temperature = Float.parseFloat(value) / 1000.0f;
                if (DEBUG) {
                    Slog.d(TAG, "updateNtcTemperature: temperature: " + temperature);
                }
                this.mCurrentNtcTemperature = temperature;
                if (!asSameTemperature(temperature, this.mCurrentAppliedNtcTemperature, true)) {
                    this.mCurrentAppliedNtcTemperature = temperature;
                    Slog.d(TAG, "updateNtcTemperature: actual temperature: " + this.mCurrentNtcTemperature + ", applied temperature: " + this.mCurrentAppliedNtcTemperature);
                    return true;
                }
                return false;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override // com.android.server.display.MiuiDisplayCloudController.CloudListener
    public void onCloudUpdated(long summary, Map<String, Object> data) {
        if ((2 & summary) != 0) {
            this.mBackgroundHandler.removeCallbacks(this.mLoadFileRunnable);
            this.mBackgroundHandler.post(this.mLoadFileRunnable);
        }
        if ((4 & summary) != 0) {
            this.mTemperatureGap = ((Float) data.get(MiuiDisplayCloudController.TEMPERATURE_GAP_MODULE_NAME)).floatValue();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$1() {
        updateConditionState(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$2() {
        updateConditionState(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$3() {
        updateConditionState(8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$4() {
        updateConditionState(16);
    }

    public void outDoorStateChanged() {
        updateConditionState(4);
    }

    private boolean updateOutdoorState() {
        boolean state = this.mOutdoorDetector.isOutdoorState();
        if (state != this.mIsInOutdoorHighTempState) {
            this.mIsInOutdoorHighTempState = state;
            return true;
        }
        return false;
    }

    public void checkOutdoorState() {
        if (this.mScreenOn && ((this.mIsThermalBrightnessBoostEnable && (this.mUseAutoBrightness || this.mSunlightModeActive)) || (this.mCurrentTemperature >= this.mTemperatureLevelCritical && this.mCurrentTemperature < this.mTemperatureLevelEmergency))) {
            this.mOutdoorDetector.detect(true);
        } else {
            this.mOutdoorDetector.detect(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isInOutdoorCriticalTemperature() {
        return this.mIsInOutdoorHighTempState && this.mCurrentAppliedTemperature >= this.mTemperatureLevelCritical && this.mCurrentAppliedTemperature < this.mTemperatureLevelEmergency;
    }

    private boolean updateThermalBrightnessIfNeeded() {
        float restrictedBrightness = updateMaxThermalBrightness();
        if (this.mThermalBrightnessControlNtcAvailable) {
            float restrictedNtcBrightness = updateMaxNtcTempBrightness();
            if (!Float.isNaN(restrictedNtcBrightness)) {
                restrictedBrightness = Float.isNaN(restrictedBrightness) ? restrictedNtcBrightness : Math.min(restrictedBrightness, restrictedNtcBrightness);
            }
        }
        if (Float.isNaN(restrictedBrightness) && Float.isNaN(this.mCurrentMaxThermalBrightness)) {
            return false;
        }
        if (this.mIsInOutdoorHighTempState && this.mCurrentTemperature >= this.mTemperatureLevelCritical && this.mCurrentTemperature < this.mTemperatureLevelEmergency) {
            restrictedBrightness = Math.max(restrictedBrightness, this.mMinBrightnessInOutdoorHighTemperature);
        }
        float f = this.mThermalSafetyBrightness;
        if (f != -1.0f && !Float.isNaN(f)) {
            restrictedBrightness = Math.min(restrictedBrightness, this.mThermalSafetyBrightness);
        }
        if (restrictedBrightness == this.mCurrentMaxThermalBrightness) {
            return false;
        }
        this.mCurrentMaxThermalBrightness = restrictedBrightness;
        Slog.d(TAG, "updateThermalBrightnessState: condition id: " + this.mCurrentAppliedThermalConditionId + ", temperature: " + this.mCurrentTemperature + ", maximum thermal brightness: " + this.mCurrentMaxThermalBrightness + ", outdoor: " + this.mIsInOutdoorHighTempState + ", safety thermal brightness: " + this.mThermalSafetyBrightness);
        return true;
    }

    private float updateMaxThermalBrightness() {
        if (this.mCurrentCondition == null) {
            ThermalConditionItem thermalConditionItem = this.mDefaultCondition;
            if (thermalConditionItem != null) {
                this.mCurrentCondition = thermalConditionItem;
                Slog.w(TAG, "updateMaxThermalBrightness: mCurrentCondition is null, initialized with default condition.");
            } else {
                Slog.w(TAG, "updateMaxThermalBrightness: no valid conditions!");
                return Float.NaN;
            }
        }
        int conditionId = this.mCurrentCondition.getIdentifier();
        Float miniTemperature = this.mMiniTemperatureThresholds.get(conditionId);
        if (miniTemperature != null && this.mCurrentAppliedTemperature < miniTemperature.floatValue()) {
            if (DEBUG) {
                Slog.d(TAG, "updateMaxThermalBrightness: Current skin temperature is less than the minimum temperature threshold: " + miniTemperature);
            }
            return Float.NaN;
        }
        Float cachedNit = (Float) this.mCacheInfo.get(conditionId, Integer.valueOf((int) this.mCurrentAppliedTemperature));
        if (cachedNit != null) {
            if (DEBUG) {
                Slog.d(TAG, "updateMaxThermalBrightness: Max thermal brightness already cached: " + cachedNit);
            }
            return cachedNit.floatValue();
        }
        List<TemperatureBrightnessPair> pairs = this.mCurrentCondition.getTemperatureBrightnessPair();
        TemperatureBrightnessPair target = null;
        Iterator<TemperatureBrightnessPair> it = pairs.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            TemperatureBrightnessPair pair = it.next();
            float minTemp = pair.getMinInclusive();
            float maxTemp = pair.getMaxExclusive();
            if (this.mCurrentAppliedTemperature >= minTemp && this.mCurrentAppliedTemperature < maxTemp) {
                target = pair;
                break;
            }
        }
        if (target == null) {
            Slog.e(TAG, "Current temperature " + this.mCurrentAppliedTemperature + " does not match in config: " + conditionId);
            return Float.NaN;
        }
        float value = target.getNit();
        this.mCacheInfo.add(conditionId, Integer.valueOf((int) this.mCurrentAppliedTemperature), Float.valueOf(value));
        Slog.d(TAG, "updateMaxThermalBrightness: get brightness threshold: " + value);
        return value;
    }

    private float updateMaxNtcTempBrightness() {
        ThermalConditionItem ntcTempCondition = this.mConditionIdMaps.get(-3);
        if (ntcTempCondition == null) {
            return Float.NaN;
        }
        List<TemperatureBrightnessPair> pairs = ntcTempCondition.getTemperatureBrightnessPair();
        TemperatureBrightnessPair target = null;
        Iterator<TemperatureBrightnessPair> it = pairs.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            TemperatureBrightnessPair pair = it.next();
            float minTemp = pair.getMinInclusive();
            float maxTemp = pair.getMaxExclusive();
            if (this.mCurrentAppliedNtcTemperature >= minTemp && this.mCurrentAppliedNtcTemperature < maxTemp) {
                target = pair;
                break;
            }
        }
        if (target == null) {
            return Float.NaN;
        }
        float value = target.getNit();
        Slog.d(TAG, "updateMaxNtcTempBrightness: get brightness threshold: " + value);
        return value;
    }

    private String readThermalConfigFromNode(String file) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            try {
                try {
                    try {
                        reader = new BufferedReader(new FileReader(new File(THERMAL_CONFIG_DIR, file)));
                        while (true) {
                            String info = reader.readLine();
                            if (info == null) {
                                break;
                            }
                            builder.append(info);
                        }
                        String sb = builder.toString();
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return sb;
                    } catch (Throwable th) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                    e3.printStackTrace();
                    if (reader == null) {
                        return null;
                    }
                    reader.close();
                    return null;
                }
            } catch (FileNotFoundException e4) {
                e4.printStackTrace();
                if (reader == null) {
                    return null;
                }
                reader.close();
                return null;
            }
        } catch (IOException e5) {
            e5.printStackTrace();
            return null;
        }
    }

    private ThermalBrightnessConfig loadConfigFromFile() {
        File defaultFile = Environment.buildPath(Environment.getProductDirectory(), new String[]{ETC_DIR, "displayconfig", DEFAULT_CONFIG_FILE});
        File cloudFile = Environment.buildPath(Environment.getDataSystemDirectory(), new String[]{"displayconfig", CLOUD_BACKUP_CONFIG_FILE});
        ThermalBrightnessConfig config = parseConfig(cloudFile);
        if (config != null) {
            this.mLoadedFileFrom = "cloud_file";
            return config;
        }
        ThermalBrightnessConfig config2 = parseConfig(defaultFile);
        if (config2 != null) {
            this.mLoadedFileFrom = "static_file";
            return config2;
        }
        return null;
    }

    private ThermalBrightnessConfig parseConfig(File configFile) {
        if (!configFile.exists()) {
            return null;
        }
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(configFile));
            try {
                ThermalBrightnessConfig read = XmlParser.read(in);
                in.close();
                return read;
            } catch (Throwable th) {
                try {
                    in.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException | DatatypeConfigurationException | XmlPullParserException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean asSameTemperature(float a, float b, boolean isNtcTemperatureGap) {
        if (a == b) {
            return true;
        }
        if (!(Float.isNaN(a) && Float.isNaN(b)) && Math.abs(((int) a) - ((int) b)) >= 1) {
            return isNtcTemperatureGap ? Math.abs(a - b) < 1.0f : Math.abs(a - b) < this.mTemperatureGap;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateScreenState(int displayState, int displayPolicy) {
        boolean screenOn = displayState == 2 && (displayPolicy == 3 || displayPolicy == 2);
        if (screenOn != this.mScreenOn) {
            this.mScreenOn = screenOn;
            this.mBackgroundHandler.removeCallbacks(this.mUpdateOutdoorRunnable);
            this.mBackgroundHandler.post(this.mUpdateOutdoorRunnable);
        }
    }

    public void setHdrLayerPresent(boolean isHdrLayerPresent) {
        if (this.mIsHdrLayerPresent != isHdrLayerPresent) {
            this.mIsHdrLayerPresent = isHdrLayerPresent;
            this.mBackgroundHandler.removeCallbacks(this.mUpdateOverrideConditionRunnable);
            this.mBackgroundHandler.post(this.mUpdateOverrideConditionRunnable);
        }
    }

    public void setDolbyEnabled(boolean enable) {
        if (this.mIsDolbyEnabled != enable) {
            this.mIsDolbyEnabled = enable;
            this.mBackgroundHandler.removeCallbacks(this.mUpdateOverrideConditionRunnable);
            this.mBackgroundHandler.post(this.mUpdateOverrideConditionRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateOverrideCondition() {
        boolean needOverride = this.mIsHdrLayerPresent || this.mIsDolbyEnabled;
        if (this.mNeedOverrideCondition != needOverride) {
            this.mNeedOverrideCondition = needOverride;
            updateConditionState(2);
        }
    }

    public void updateOutdoorThermalAppCategoryList(List<String> outdoorThermalAppList) {
        this.mOutdoorThermalAppList = outdoorThermalAppList;
    }

    public void updateForegroundApp(String foregroundAppPackageName) {
        if (this.mIsThermalBrightnessBoostEnable) {
            this.mForegroundAppPackageName = foregroundAppPackageName;
            updateThermalBrightnessBoostCondition();
        }
    }

    private void updateThermalBrightnessBoostCondition() {
        boolean needThermalBrightnessBoost = this.mOutdoorThermalAppList != null && this.mIsInOutdoorHighTempState && this.mOutdoorThermalAppList.contains(this.mForegroundAppPackageName);
        if (this.mNeedThermalBrightnessBoost != needThermalBrightnessBoost) {
            this.mNeedThermalBrightnessBoost = needThermalBrightnessBoost;
            updateConditionState(2);
        }
    }

    public void setSunlightState(boolean active) {
        if (active != this.mSunlightModeActive) {
            this.mSunlightModeActive = active;
            this.mBackgroundHandler.removeCallbacks(this.mUpdateOutdoorRunnable);
            this.mBackgroundHandler.post(this.mUpdateOutdoorRunnable);
        }
    }

    public void dump(PrintWriter pw) {
        DEBUG = DisplayDebugConfig.DEBUG_DPC;
        pw.println("");
        pw.println("Thermal Brightness Controller: ");
        pw.println("  mCurrentCondition=" + toStringCondition(this.mCurrentCondition));
        pw.println("  mDefaultCondition=" + toStringCondition(this.mDefaultCondition));
        pw.println("  mOverrideCondition=" + toStringCondition(this.mOverrideCondition));
        pw.println("  mCurrentAppliedThermalConditionId=" + this.mCurrentAppliedThermalConditionId);
        pw.println("  mCurrentActualThermalConditionId=" + this.mCurrentActualThermalConditionId);
        pw.println("  mCurrentAppliedTemperature=" + this.mCurrentAppliedTemperature);
        pw.println("  mCurrentTemperature=" + this.mCurrentTemperature);
        pw.println("  mCurrentThermalMaxBrightness=" + this.mCurrentMaxThermalBrightness);
        pw.println("  mMiniTemperatureThresholds=" + this.mMiniTemperatureThresholds);
        pw.println("  mIsInOutdoorHighTempState=" + this.mIsInOutdoorHighTempState);
        pw.println("  mIsThermalBrightnessBoostEnable=" + this.mIsThermalBrightnessBoostEnable);
        pw.println("  mNeedThermalBrightnessBoost=" + this.mNeedThermalBrightnessBoost);
        pw.println("  mScreenOn=" + this.mScreenOn);
        pw.println("  mNeedOverrideCondition=" + this.mNeedOverrideCondition);
        pw.println("  mTemperatureLevelEmergency=" + this.mTemperatureLevelEmergency);
        pw.println("  mTemperatureLevelCritical=" + this.mTemperatureLevelCritical);
        pw.println("  mMinBrightnessInOutdoorHighTemperature=" + this.mMinBrightnessInOutdoorHighTemperature);
        pw.println("  mThresholdLuxEnterOutdoor=" + this.mThresholdLuxEnterOutdoor);
        pw.println("  mThermalSafetyBrightness=" + this.mThermalSafetyBrightness);
        pw.println("  mTemperatureGap=" + this.mTemperatureGap);
        pw.println("  mThermalBrightnessControlNtcAvailable=" + this.mThermalBrightnessControlNtcAvailable);
        pw.println("  mCurrentNtcTemperature=" + this.mCurrentNtcTemperature);
        pw.println("  mCurrentAppliedNtcTemperature=" + this.mCurrentAppliedNtcTemperature);
        pw.println("  Thermal brightness config:");
        pw.println("  mLoadedFileFrom:" + this.mLoadedFileFrom);
        for (ThermalConditionItem conditionItem : this.mThermalConditions) {
            pw.println("  identifier: " + conditionItem.getIdentifier());
            pw.println("  description: " + conditionItem.getDescription());
            formatDumpTemperatureBrightnessPair(conditionItem.getTemperatureBrightnessPair(), pw);
        }
    }

    private void formatDumpTemperatureBrightnessPair(List<TemperatureBrightnessPair> pairs, PrintWriter pw) {
        pw.println("  temperature-brightness pair: ");
        StringBuilder sbTemperature = null;
        StringBuilder sbNit = null;
        boolean needsHeaders = true;
        String separator = "";
        int size = pairs.size();
        for (int i = 0; i < pairs.size(); i++) {
            TemperatureBrightnessPair pair = pairs.get(i);
            if (needsHeaders) {
                sbTemperature = new StringBuilder("    temperature: ");
                sbNit = new StringBuilder("            nit: ");
                needsHeaders = false;
            }
            String strTemperature = "[" + toStrFloatForDump(pair.getMinInclusive()) + "," + toStrFloatForDump(pair.getMaxExclusive()) + ")";
            String strNit = toStrFloatForDump(pair.getNit());
            int maxLen = Math.max(strTemperature.length(), strNit.length());
            String format = separator + "%" + maxLen + "s";
            separator = ", ";
            sbTemperature.append(TextUtils.formatSimple(format, new Object[]{strTemperature}));
            sbNit.append(TextUtils.formatSimple(format, new Object[]{strNit}));
            if (sbTemperature.length() > 80 || i == size - 1) {
                pw.println(sbTemperature);
                pw.println(sbNit);
                needsHeaders = true;
                separator = "";
            }
        }
    }

    private static String toStringCondition(ThermalConditionItem condition) {
        if (condition == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{identifier: ").append(condition.getIdentifier()).append(", description: ").append(condition.getDescription()).append("}");
        return builder.toString();
    }

    private static String toStrFloatForDump(float value) {
        if (value == MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            return "0";
        }
        if (value < 0.1f) {
            return String.format(Locale.US, "%.3f", Float.valueOf(value));
        }
        if (value < 1.0f) {
            return String.format(Locale.US, "%.2f", Float.valueOf(value));
        }
        if (value < 10.0f) {
            return String.format(Locale.US, "%.1f", Float.valueOf(value));
        }
        return TextUtils.formatSimple("%d", new Object[]{Integer.valueOf(Math.round(value))});
    }

    public void addThermalListener(ThermalListener listener) {
        if (listener != null) {
            this.mThermalListener.add(listener);
        }
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    updateScreenBrightnessMode();
                    return;
                default:
                    return;
            }
        }

        private void updateScreenBrightnessMode() {
            int screenBrightnessModeSetting = Settings.System.getIntForUser(ThermalBrightnessController.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2);
            boolean useAutoBrightness = screenBrightnessModeSetting == 1;
            if (useAutoBrightness != ThermalBrightnessController.this.mUseAutoBrightness) {
                ThermalBrightnessController.this.mUseAutoBrightness = useAutoBrightness;
                ThermalBrightnessController.this.mBackgroundHandler.removeCallbacks(ThermalBrightnessController.this.mUpdateOutdoorRunnable);
                ThermalBrightnessController.this.mBackgroundHandler.post(ThermalBrightnessController.this.mUpdateOutdoorRunnable);
            }
        }
    }
}
