package com.android.server.display.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public abstract class AggregationEvent {
    public abstract Map<Object, List<Float>> getCacheDataMap();

    public abstract Map<Object, Object> getQuotaEvents();

    public abstract String toString();

    /* loaded from: classes.dex */
    public static class BrightnessAggregationEvent extends AggregationEvent {
        public static final String EVENT_AUTO_ADJUST_TIMES = "auto_adj_times";
        public static final String EVENT_AUTO_MANUAL_ADJUST_APP_RANKING = "auto_manual_adj_app_ranking";
        public static final String EVENT_AUTO_MANUAL_ADJUST_AVG_NITS_LUX_SPAN = "auto_manual_adj_avg_nits_lux_span";
        public static final String EVENT_AUTO_MANUAL_ADJUST_DISPLAY_MODE = "auto_manual_adj_display_mode";
        public static final String EVENT_AUTO_MANUAL_ADJUST_HIGH_LUX_SPAN = "auto_manual_adj_high_lux_span";
        public static final String EVENT_AUTO_MANUAL_ADJUST_LOW_LUX_SPAN = "auto_manual_adj_low_lux_span";
        public static final String EVENT_AUTO_MANUAL_ADJUST_LUX_SPAN = "auto_manual_adj_lux_span";
        public static final String EVENT_AUTO_MANUAL_ADJUST_TIMES = "auto_manual_adj_times";
        public static final String EVENT_AVERAGE_BRIGHTNESS = "average_brightness";
        public static final String EVENT_BRIGHTNESS_ADJUST_TIMES = "brightness_adj_times";
        public static final String EVENT_BRIGHTNESS_USAGE = "brightness_usage";
        public static final String EVENT_HBM_USAGE = "hbm_usage";
        public static final String EVENT_HDR_USAGE = "hdr_usage";
        public static final String EVENT_HDR_USAGE_APP_USAGE = "hdr_usage_app_usage";
        public static final String EVENT_MANUAL_ADJUST_TIMES = "manual_adj_times";
        public static final String EVENT_OVERRIDE_ADJUST_APP_RANKING = "override_adj_app_ranking";
        public static final String EVENT_SUNLIGHT_ADJUST_TIMES = "sunlight_adj_times";
        public static final String EVENT_SWITCH_STATS = "switch_stats";
        public static final String EVENT_WINDOW_ADJUST_TIMES = "window_adj_times";
        public static final String KEY_SWITCH_STATS_DETAILS = "switch_stats_details";
        public static final String KEY_USAGE_VALUE = "usage_value";
        private Map<Object, Object> mBrightnessQuotaEvents = new HashMap();
        private Map<Object, List<Float>> mCacheDataMap = new HashMap();

        @Override // com.android.server.display.statistics.AggregationEvent
        public String toString() {
            return this.mBrightnessQuotaEvents.toString();
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, Object> getQuotaEvents() {
            return this.mBrightnessQuotaEvents;
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, List<Float>> getCacheDataMap() {
            return this.mCacheDataMap;
        }
    }

    /* loaded from: classes.dex */
    public static class AdvancedAggregationEvent extends AggregationEvent {
        public static final String EVENT_INTERRUPT_ANIMATION_TIMES = "interrupt_animation_times";
        public static final String EVENT_RESET_BRIGHTNESS_MODE_TIMES = "reset_brightness_mode_times";
        public static final String KEY_INTERRUPT_TIMES = "interrupt_times";
        public static final String KEY_RESET_TIMES = "reset_times";
        private Map<Object, Object> mAdvancedQuotaEvents = new HashMap();

        @Override // com.android.server.display.statistics.AggregationEvent
        public String toString() {
            return this.mAdvancedQuotaEvents.toString();
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, Object> getQuotaEvents() {
            return this.mAdvancedQuotaEvents;
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, List<Float>> getCacheDataMap() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static class ThermalAggregationEvent extends AggregationEvent {
        public static final String EVENT_THERMAL_AVERAGE_TEMPERATURE = "thermal_average_temperature";
        public static final String EVENT_THERMAL_BRIGHTNESS_RESTRICTED_ADJUST_HIGH_TIMES = "thermal_brightness_restricted_adj_high_times";
        public static final String EVENT_THERMAL_BRIGHTNESS_RESTRICTED_USAGE = "thermal_brightness_restricted_usage";
        public static final String EVENT_THERMAL_DETAIL_RESTRICTED_USAGE = "thermal_detail_restricted_usage";
        public static final String EVENT_THERMAL_DETAIL_UNRESTRICTED_USAGE = "thermal_detail_unrestricted_usage";
        public static final String EVENT_THERMAL_OUTDOOR_USAGE = "thermal_outdoor_usage";
        public static final String EVENT_THERMAL_USAGE = "thermal_usage";
        public static final String KEY_AVERAGE_VALUE = "average";
        public static final String KEY_RESTRICTED_USAGE_VALUE = "restricted_usage";
        public static final String KEY_UNRESTRICTED_USAGE_VALUE = "unrestricted_usage";
        private Map<Object, Object> mThermalQuotaEvents = new HashMap();
        private Map<Object, List<Float>> mThermalCacheDataMap = new HashMap();

        @Override // com.android.server.display.statistics.AggregationEvent
        public String toString() {
            return this.mThermalQuotaEvents.toString();
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, Object> getQuotaEvents() {
            return this.mThermalQuotaEvents;
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, List<Float>> getCacheDataMap() {
            return this.mThermalCacheDataMap;
        }
    }

    /* loaded from: classes.dex */
    public static class CbmAggregationEvent extends AggregationEvent {
        protected static final String EVENT_CUSTOM_BRIGHTNESS_ADJUST = "custom_brightness_adj";
        protected static final String EVENT_CUSTOM_BRIGHTNESS_USAGE = "custom_brightness_usage";
        protected static final String EVENT_INDIVIDUAL_MODEL_PREDICT = "individual_model_predict";
        protected static final String EVENT_INDIVIDUAL_MODEL_TRAIN = "individual_model_train";
        protected static final String KEY_CURRENT_INDIVIDUAL_MODEL_MAE = "cur_train_mae";
        protected static final String KEY_CURRENT_INDIVIDUAL_MODEL_MAPE = "cur_train_mape";
        private static final String KEY_CUSTOM_CURVE_AUTO_BRIGHTNESS_ADJUST = "custom_curve_auto_brt_adj";
        private static final String KEY_CUSTOM_CURVE_AUTO_BRIGHTNESS_MANUAL_ADJUST = "custom_curve_auto_brt_manual_adj";
        private static final String KEY_CUSTOM_CURVE_DURATION = "custom_curve_duration";
        private static final String KEY_DEFAULT_AUTO_BRIGHTNESS_ADJUST = "default_auto_brt_adj";
        private static final String KEY_DEFAULT_AUTO_BRIGHTNESS_MANUAL_ADJUST = "default_auto_brt_manual_adj";
        private static final String KEY_DEFAULT_AUTO_DURATION = "default_auto_duration";
        private static final String KEY_INDIVIDUAL_MODEL_AUTO_BRIGHTNESS_ADJUST = "model_auto_brt_adj";
        private static final String KEY_INDIVIDUAL_MODEL_AUTO_BRIGHTNESS_MANUAL_ADJUST = "model_auto_brt_manual_adj";
        private static final String KEY_INDIVIDUAL_MODEL_DURATION = "model_duration";
        protected static final String KEY_INDIVIDUAL_MODEL_PREDICT_AVERAGE_DURATION = "model_predict_average_duration";
        protected static final String KEY_INDIVIDUAL_MODEL_PREDICT_TIMEOUT_COUNT = "model_predict_timeout_count";
        protected static final String KEY_INDIVIDUAL_MODEL_TRAIN_COUNT = "model_train_count";
        protected static final String KEY_INDIVIDUAL_MODEL_TRAIN_INDICATORS = "model_train_indicators";
        protected static final String KEY_INDIVIDUAL_MODEL_TRAIN_LOSS = "train_loss";
        protected static final String KEY_INDIVIDUAL_MODEL_TRAIN_SAMPLE_NUM = "sample_num";
        protected static final String KEY_INDIVIDUAL_MODEL_TRAIN_TIMESTAMP = "timestamp";
        protected static final String KEY_INDIVIDUAL_MODEL_VALIDATION_FAIL_COUNT = "model_validation_fail_count";
        protected static final String KEY_INDIVIDUAL_MODEL_VALIDATION_SUCCESS_COUNT = "model_validation_success_count";
        protected static final String KEY_PREVIOUS_INDIVIDUAL_MODEL_MAE = "pre_train_mae";
        protected static final String KEY_PREVIOUS_INDIVIDUAL_MODEL_MAPE = "pre_train_mape";
        private final Map<Object, Object> mCbmQuotaEvents = new HashMap();
        private final Map<Object, List<Float>> mCbmCacheDataMap = new HashMap();

        @Override // com.android.server.display.statistics.AggregationEvent
        public String toString() {
            return this.mCbmQuotaEvents.toString();
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, Object> getQuotaEvents() {
            return this.mCbmQuotaEvents;
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, List<Float>> getCacheDataMap() {
            return this.mCbmCacheDataMap;
        }

        public static String getCbmAutoAdjustQuotaName(int cbmState, boolean isManuallySet) {
            switch (cbmState) {
                case 1:
                    return isManuallySet ? KEY_CUSTOM_CURVE_AUTO_BRIGHTNESS_MANUAL_ADJUST : KEY_CUSTOM_CURVE_AUTO_BRIGHTNESS_ADJUST;
                case 2:
                    return isManuallySet ? KEY_INDIVIDUAL_MODEL_AUTO_BRIGHTNESS_MANUAL_ADJUST : KEY_INDIVIDUAL_MODEL_AUTO_BRIGHTNESS_ADJUST;
                default:
                    return isManuallySet ? KEY_DEFAULT_AUTO_BRIGHTNESS_MANUAL_ADJUST : KEY_DEFAULT_AUTO_BRIGHTNESS_ADJUST;
            }
        }

        public static String getCbmBrtUsageQuotaName(int cbmState) {
            switch (cbmState) {
                case 1:
                    return KEY_CUSTOM_CURVE_DURATION;
                case 2:
                    return KEY_INDIVIDUAL_MODEL_DURATION;
                default:
                    return KEY_DEFAULT_AUTO_DURATION;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class AonFlareAggregationEvent extends AggregationEvent {
        public static final String EVENT_FLARE_MANUAL_ADJUST_TIMES = "flare_manual_adjust_times";
        public static final String EVENT_FLARE_SCENE_CHECK_TIMES = "flare_scene_check_times";
        public static final String EVENT_FLARE_SUPPRESS_DARKEN_HOUR = "flare_suppress_darken_hour";
        public static final String EVENT_FLARE_SUPPRESS_DARKEN_LUX_SPAN = "flare_suppress_darken_lux_span";
        public static final String EVENT_FLARE_USER_RESET_BRIGHTNESS_MODE_TIMES = "flare_user_reset_brightness_mode_times";
        public static final String KEY_FLARE_NOT_SUPPRESS_DARKEN_MANUAL_ADJUST_DECREASE_TIMES = "3";
        public static final String KEY_FLARE_NOT_SUPPRESS_DARKEN_MANUAL_ADJUST_INCREASE_TIMES = "4";
        public static final String KEY_FLARE_NOT_SUPPRESS_DARKEN_USER_RESET_BRIGHTNESS_MODE_TIMES = "2";
        public static final String KEY_FLARE_SUPPRESS_DARKEN_MANUAL_ADJUST_DECREASE_TIMES = "1";
        public static final String KEY_FLARE_SUPPRESS_DARKEN_MANUAL_ADJUST_INCREASE_TIMES = "2";
        public static final String KEY_FLARE_SUPPRESS_DARKEN_USER_RESET_BRIGHTNESS_MODE_TIMES = "1";
        public static final String KEY_IN_FLARE_SCENE_TIMES = "1";
        public static final String KEY_NOT_IN_FLARE_SCENE_TIMES = "2";
        public static final String KEY_NOT_REPORT_IN_TIME_TIMES = "3";
        private Map<Object, Object> mAonFlareQuotaEvents = new HashMap();

        @Override // com.android.server.display.statistics.AggregationEvent
        public String toString() {
            return this.mAonFlareQuotaEvents.toString();
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, Object> getQuotaEvents() {
            return this.mAonFlareQuotaEvents;
        }

        @Override // com.android.server.display.statistics.AggregationEvent
        public Map<Object, List<Float>> getCacheDataMap() {
            return null;
        }
    }
}
