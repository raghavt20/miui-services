package com.android.server.display.statistics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class BrightnessEvent {
    public static final int EVENT_AUTO_CHANGED_BRIGHTNESS = 1;
    public static final int EVENT_AUTO_MANUAL_CHANGED_BRIGHTNESS = 2;
    public static final int EVENT_BRIGHTNESS_UNDEFINED = -1;
    public static final int EVENT_DISABLE_AUTO_BRIGHTNESS = 8;
    public static final int EVENT_MANUAL_CHANGED_BRIGHTNESS = 0;
    public static final int EVENT_SUNLIGHT_CHANGED_BRIGHTNESS = 4;
    public static final int EVENT_WINDOW_CHANGED_BRIGHTNESS = 3;
    private boolean brightness_restricted_enable;
    private int current_user_id;
    private int expId;
    private boolean hdr_layer_enable;
    private boolean is_use_light_fov_optimization;
    private boolean low_power_mode_flag;
    private int orientation;
    private float previous_brightness;
    private float screen_brightness;
    private long time_stamp;
    private int type;
    private float ambient_lux = -1.0f;
    private String top_package = "";
    private String extra = "";
    private boolean is_default_config = true;
    private float user_data_point = -1.0f;
    private List<SwitchStatEntry> all_stats_entries = new ArrayList();
    private int affect_factor_flag = -1;
    private int screen_brightness_span = -1;
    private int previous_brightness_span = -1;
    private int ambient_lux_span = -1;
    private Map<Integer, Long> brightness_usage_map = new HashMap();
    private float display_gray_scale = -1.0f;
    private float original_nit = -1.0f;
    private float actual_nit = -1.0f;
    private float main_ambient_lux = -1.0f;
    private float assist_ambient_lux = -1.0f;
    private float last_main_ambient_lux = -1.0f;
    private float last_assist_ambient_lux = -1.0f;
    private float[] acc_values = new float[3];

    public BrightnessEvent setEventType(int type) {
        this.type = type;
        return this;
    }

    public int getEventType() {
        return this.type;
    }

    public BrightnessEvent setAmbientLux(float ambientLux) {
        this.ambient_lux = ambientLux;
        return this;
    }

    public float getAmbientLux() {
        return this.ambient_lux;
    }

    public BrightnessEvent setScreenBrightness(float screenBrightness) {
        this.screen_brightness = screenBrightness;
        return this;
    }

    public float getScreenBrightness() {
        return this.screen_brightness;
    }

    public BrightnessEvent setOrientation(int orientation) {
        this.orientation = orientation;
        return this;
    }

    public int getOrientation() {
        return this.orientation;
    }

    public BrightnessEvent setForegroundPackage(String top_package) {
        this.top_package = top_package;
        return this;
    }

    public String getForegroundPackage() {
        return this.top_package;
    }

    public BrightnessEvent setTimeStamp(long timeStamp) {
        this.time_stamp = timeStamp;
        return this;
    }

    public long getTimeStamp() {
        return this.time_stamp;
    }

    public BrightnessEvent setExtra(String string) {
        this.extra = string;
        return this;
    }

    public String getExtra() {
        return this.extra;
    }

    public static String timestamp2String(long time) {
        Date d = new Date(time);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss");
        return sf.format(d);
    }

    public BrightnessEvent setPreviousBrightness(float previousBrightness) {
        this.previous_brightness = previousBrightness;
        return this;
    }

    public float getPreviousBrightness() {
        return this.previous_brightness;
    }

    public BrightnessEvent setUserDataPoint(float userDataPoint) {
        this.user_data_point = userDataPoint;
        return this;
    }

    public float getUserDataPoint() {
        return this.user_data_point;
    }

    public BrightnessEvent setIsDefaultConfig(boolean defaultConfig) {
        this.is_default_config = defaultConfig;
        return this;
    }

    public boolean isDefaultConfig() {
        return this.is_default_config;
    }

    public BrightnessEvent setLowPowerModeFlag(boolean enable) {
        this.low_power_mode_flag = enable;
        return this;
    }

    public boolean getLowPowerModeFlag() {
        return this.low_power_mode_flag;
    }

    public BrightnessEvent setUserId(int userId) {
        this.current_user_id = userId;
        return this;
    }

    public int getUserId() {
        return this.current_user_id;
    }

    public BrightnessEvent setSwitchStats(List<SwitchStatEntry> all_stats_events) {
        this.all_stats_entries = all_stats_events;
        return this;
    }

    public List<SwitchStatEntry> getSwitchStats() {
        return this.all_stats_entries;
    }

    public BrightnessEvent setAffectFactorFlag(int flag) {
        this.affect_factor_flag = flag;
        return this;
    }

    public int getAffectFactorFlag() {
        return this.affect_factor_flag;
    }

    public BrightnessEvent setCurBrightnessSpanIndex(int span) {
        this.screen_brightness_span = span;
        return this;
    }

    public int getCurBrightnessSpanIndex() {
        return this.screen_brightness_span;
    }

    public BrightnessEvent setPreBrightnessSpanIndex(int span) {
        this.previous_brightness_span = span;
        return this;
    }

    public int getPreBrightnessSpanIndex() {
        return this.previous_brightness_span;
    }

    public BrightnessEvent setLuxSpanIndex(int span) {
        this.ambient_lux_span = span;
        return this;
    }

    public int getLuxSpanIndex() {
        return this.ambient_lux_span;
    }

    public BrightnessEvent setBrightnessUsageMap(Map<Integer, Long> brightness_usage_map) {
        this.brightness_usage_map = brightness_usage_map;
        return this;
    }

    public Map<Integer, Long> getBrightnessUsageMap() {
        return this.brightness_usage_map;
    }

    public BrightnessEvent setDisplayGrayScale(float gray) {
        this.display_gray_scale = gray;
        return this;
    }

    public float getDisplayGrayScale() {
        return this.display_gray_scale;
    }

    public BrightnessEvent setActualNit(float nit) {
        this.actual_nit = nit;
        return this;
    }

    public float getActualNit() {
        return this.actual_nit;
    }

    public BrightnessEvent setHdrLayerEnable(boolean enable) {
        this.hdr_layer_enable = enable;
        return this;
    }

    public boolean getHdrLayerEnable() {
        return this.hdr_layer_enable;
    }

    public BrightnessEvent setBrightnessRestrictedEnable(boolean enable) {
        this.brightness_restricted_enable = enable;
        return this;
    }

    public boolean getBrightnessRestrictedEnable() {
        return this.brightness_restricted_enable;
    }

    public BrightnessEvent setMainAmbientLux(float lux) {
        this.main_ambient_lux = lux;
        return this;
    }

    public float getMainAmbientLux() {
        return this.main_ambient_lux;
    }

    public BrightnessEvent setAssistAmbientLux(float lux) {
        this.assist_ambient_lux = lux;
        return this;
    }

    public float getAssistAmbientLux() {
        return this.assist_ambient_lux;
    }

    public BrightnessEvent setLastMainAmbientLux(float lux) {
        this.last_main_ambient_lux = lux;
        return this;
    }

    public float getLastMainAmbientLux() {
        return this.last_main_ambient_lux;
    }

    public BrightnessEvent setLastAssistAmbientLux(float lux) {
        this.last_assist_ambient_lux = lux;
        return this;
    }

    public float getLastAssistAmbientLux() {
        return this.last_assist_ambient_lux;
    }

    public BrightnessEvent setAccValues(float[] accValues) {
        this.acc_values = accValues;
        return this;
    }

    public float[] getAccValues() {
        return this.acc_values;
    }

    public BrightnessEvent setIsUseLightFovOptimization(boolean isUseLightFovOptimization) {
        this.is_use_light_fov_optimization = isUseLightFovOptimization;
        return this;
    }

    public boolean getIsUseLightFovOptimization() {
        return this.is_use_light_fov_optimization;
    }

    public BrightnessEvent setOriginalNit(float nit) {
        this.original_nit = nit;
        return this;
    }

    public float getOriginalNit() {
        return this.original_nit;
    }

    public int getExpId() {
        return this.expId;
    }

    public String toSimpleString() {
        return "{" + this.type + "," + this.ambient_lux + "," + this.screen_brightness + "," + this.orientation + "," + this.top_package + "," + this.extra + ", " + this.previous_brightness + "," + this.is_default_config + "," + this.user_data_point + "," + this.low_power_mode_flag + "," + this.current_user_id + "}";
    }

    public String toString() {
        return "{type:" + this.type + ",orientation:" + this.orientation + ",top_package:" + this.top_package + ",screen_brightness:" + this.screen_brightness + ",previous_brightness:" + this.previous_brightness + ",ambient_lux:" + this.ambient_lux + ",user_data_point:" + this.user_data_point + ",is_default_config:" + this.is_default_config + ",screen_brightness_span:" + this.screen_brightness_span + ",previous_brightness_span:" + this.previous_brightness_span + ",ambient_lux_span:" + this.ambient_lux_span + ",all_stats_entries:" + this.all_stats_entries + ",affect_factor_flag:" + this.affect_factor_flag + ",display_gray_scale:" + this.display_gray_scale + ",low_power_mode_flag:" + this.low_power_mode_flag + ",current_user_id:" + this.current_user_id + ",original_nit:" + this.original_nit + ",actual_nit:" + this.actual_nit + ",hdr_layer_enable:" + this.hdr_layer_enable + ",brightness_restricted_enable:" + this.brightness_restricted_enable + ",extra:" + this.extra + ",time_stamp:" + timestamp2String(this.time_stamp) + ",main_ambient_lux:" + this.main_ambient_lux + ",assist_ambient_lux:" + this.assist_ambient_lux + ",last_main_ambient_lux:" + this.last_main_ambient_lux + ",last_assist_ambient_lux:" + this.last_assist_ambient_lux + ",acc_values:" + Arrays.toString(this.acc_values) + ",expId:" + this.expId + "}";
    }

    /* loaded from: classes.dex */
    public static class SwitchStatEntry {
        public static final int TYPE_BOOLEAN = 0;
        public static final int TYPE_INTEGER = 1;
        public boolean b_value;
        public int i_value;
        public String key;
        public int type;

        public SwitchStatEntry(int type, String key, int i_value) {
            if (type == 0) {
                throw new IllegalArgumentException("Type and value are incompatible,the expected type is TYPE_INTEGER.");
            }
            this.type = type;
            this.key = key;
            this.i_value = i_value;
        }

        public SwitchStatEntry(int type, String key, boolean b_value) {
            if (type == 1) {
                throw new IllegalArgumentException("type and value are incompatible,the expected type is TYPE_BOOLEAN.");
            }
            this.type = type;
            this.key = key;
            this.b_value = b_value;
            this.i_value = -1;
        }

        public String toString() {
            return "{key:" + this.key + ", value:" + (this.type == 0 ? Boolean.valueOf(this.b_value) : Integer.valueOf(this.i_value)) + "}";
        }
    }
}
