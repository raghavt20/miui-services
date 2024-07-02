package com.android.server.display.statistics;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.display.statistics.AggregationEvent;
import java.util.Map;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class OneTrackUploaderHelper {
    public static final String ADVANCED_BRIGHTNESS_AGGREGATION_EVENT_NAME = "advanced_brightness_aggregation";
    public static final String ADVANCED_EVENT_NAME = "advanced_brightness";
    public static final String AON_FLARE_AGGREGATION_EVENT_NAME = "aon_flare_aggregation";
    private static final String BRIGHTNESS_EVENT_APP_ID = "31000000084";
    public static final String BRIGHTNESS_EVENT_NAME = "brightness";
    public static final String BRIGHTNESS_QUOTA_AGGREGATION_EVENT_NAME = "brightness_quota_aggregation";
    public static final String CUSTOM_BRIGHTNESS_AGGREGATION_EVENT_NAME = "custom_brightness_aggregation";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String KEY_ACC_VALUES = "acc_values";
    private static final String KEY_ACTUAL_NIT = "actual_nit";
    private static final String KEY_AFFECT_FACTOR_FLAG = "affect_factor_flag";
    private static final String KEY_ALL_STATS_ENTRIES = "all_stats_entries";
    private static final String KEY_AMBIENT_LUX = "ambient_lux";
    private static final String KEY_AMBIENT_LUX_SPAN = "ambient_lux_span";
    private static final String KEY_ASSIST_AMBIENT_LUX = "assist_ambient_lux";
    private static final String KEY_AUTO_BRIGHTNESS_ANIMATION_DURATION = "auto_brightness_animation_duration";
    private static final String KEY_BRIGHTNESS_CHANGED_STATUS = "brightness_changed_state";
    private static final String KEY_BRIGHTNESS_RESTRICTED_ENABLE = "brightness_restricted_enable";
    private static final String KEY_CURRENT_ANIMATE_VALUE = "current_animate_value";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_DEFAULT_SPLINE_ERROR = "default_spline_error";
    private static final String KEY_DISPLAY_GRAY_SCALE = "display_gray_scale";
    private static final String KEY_EVENT_TYPE = "type";
    private static final String KEY_EXP_ID = "exp_id";
    private static final String KEY_EXTRA = "extra";
    private static final String KEY_HDR_LAYER_ENABLE = "hdr_layer_enable";
    private static final String KEY_IS_DEFAULT_CONFIG = "is_default_config";
    private static final String KEY_IS_USE_LIGHT_FOV_OPTIMIZATION = "is_use_light_fov_optimization";
    private static final String KEY_LAST_ASSIST_AMBIENT_LUX = "last_assist_ambient_lux";
    private static final String KEY_LAST_MAIN_AMBIENT_LUX = "last_main_ambient_lux";
    private static final String KEY_LONG_TERM_MODEL_SPLINE_ERROR = "long_term_model_spline_error";
    private static final String KEY_LOW_POWER_MODE_FLAG = "low_power_mode_flag";
    private static final String KEY_MAIN_AMBIENT_LUX = "main_ambient_lux";
    private static final String KEY_ORIENTATION = "orientation";
    private static final String KEY_ORIGINAL_NIT = "original_nit";
    private static final String KEY_PREVIOUS_BRIGHTNESS = "previous_brightness";
    private static final String KEY_PREVIOUS_BRIGHTNESS_SPAN = "previous_brightness_span";
    private static final String KEY_SCREEN_BRIGHTNESS = "screen_brightness";
    private static final String KEY_SCREEN_BRIGHTNESS_SPAN = "screen_brightness_span";
    private static final String KEY_TARGET_ANIMATE_VALUE = "target_animate_value";
    private static final String KEY_TIME_STAMP = "time_stamp";
    private static final String KEY_TOP_PACKAGE = "top_package";
    private static final String KEY_USER_BRIGHTNESS = "user_brightness";
    private static final String KEY_USER_DATA_POINT = "user_data_point";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    public static final String THERMAL_AGGREGATION_EVENT_NAME = "thermal_aggregation";
    private static final String DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
    public static final boolean IS_INTERNATIONAL_BUILD = SystemProperties.get("ro.product.mod_device", "").contains("_global");

    public static void reportAggregatedEventsToServer(Context context, Map<String, AggregationEvent> map, String eventName, int expId) {
        AggregationEvent event;
        if (DEVICE_REGION.equals("IN") || map.size() == 0) {
            return;
        }
        final Intent intent = getBrightnessEventIntent(context);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, eventName);
        intent.putExtra(KEY_EXP_ID, expId);
        map.forEach(new BiConsumer() { // from class: com.android.server.display.statistics.OneTrackUploaderHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                intent.putExtra((String) obj, ((AggregationEvent) obj2).toString());
            }
        });
        if (BRIGHTNESS_QUOTA_AGGREGATION_EVENT_NAME.equals(eventName) && (event = map.get(AggregationEvent.BrightnessAggregationEvent.EVENT_BRIGHTNESS_ADJUST_TIMES)) != null) {
            Map<Object, Object> quotaEvents = event.getQuotaEvents();
            quotaEvents.forEach(new BiConsumer() { // from class: com.android.server.display.statistics.OneTrackUploaderHelper$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    OneTrackUploaderHelper.lambda$reportAggregatedEventsToServer$1(intent, obj, obj2);
                }
            });
        }
        startService(context, intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$reportAggregatedEventsToServer$1(Intent intent, Object key, Object value) {
        if (key != null && value != null) {
            intent.putExtra(getEventName((Integer) key), (Integer) value);
        }
    }

    public static void reportToOneTrack(Context context, BrightnessEvent event) {
        if (DEVICE_REGION.equals("IN")) {
            return;
        }
        Intent intent = getBrightnessEventIntent(context);
        updateBrightnessEventIntent(intent, event);
        startService(context, intent);
    }

    public static void reportToOneTrack(Context context, AdvancedEvent event) {
        if (DEVICE_REGION.equals("IN")) {
            return;
        }
        Intent intent = getBrightnessEventIntent(context);
        updateAdvancedEventIntent(intent, event);
        startService(context, intent);
    }

    private static void startService(Context context, Intent intent) {
        if (!IS_INTERNATIONAL_BUILD) {
            intent.setFlags(3);
        }
        try {
            context.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.w("BrightnessDataProcessor", "Failed to upload brightness event! " + e.getMessage());
        }
    }

    public static void updateBrightnessEventIntent(Intent intent, BrightnessEvent event) {
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, BRIGHTNESS_EVENT_NAME).putExtra("type", event.getEventType()).putExtra(KEY_AMBIENT_LUX, event.getAmbientLux()).putExtra(KEY_SCREEN_BRIGHTNESS, event.getScreenBrightness()).putExtra(KEY_ORIENTATION, event.getOrientation()).putExtra(KEY_TOP_PACKAGE, event.getForegroundPackage()).putExtra(KEY_TIME_STAMP, event.getTimeStamp()).putExtra(KEY_EXTRA, event.getExtra()).putExtra(KEY_PREVIOUS_BRIGHTNESS, event.getPreviousBrightness()).putExtra(KEY_IS_DEFAULT_CONFIG, event.isDefaultConfig()).putExtra(KEY_USER_DATA_POINT, event.getUserDataPoint()).putExtra(KEY_LOW_POWER_MODE_FLAG, event.getLowPowerModeFlag()).putExtra(KEY_CURRENT_USER_ID, event.getUserId()).putExtra(KEY_ALL_STATS_ENTRIES, event.getSwitchStats().toString()).putExtra(KEY_AFFECT_FACTOR_FLAG, event.getAffectFactorFlag()).putExtra(KEY_SCREEN_BRIGHTNESS_SPAN, event.getCurBrightnessSpanIndex()).putExtra(KEY_PREVIOUS_BRIGHTNESS_SPAN, event.getPreBrightnessSpanIndex()).putExtra(KEY_AMBIENT_LUX_SPAN, event.getLuxSpanIndex()).putExtra(KEY_DISPLAY_GRAY_SCALE, event.getDisplayGrayScale()).putExtra(KEY_ACTUAL_NIT, event.getActualNit()).putExtra(KEY_ORIGINAL_NIT, event.getOriginalNit()).putExtra(KEY_HDR_LAYER_ENABLE, event.getHdrLayerEnable()).putExtra(KEY_BRIGHTNESS_RESTRICTED_ENABLE, event.getBrightnessRestrictedEnable()).putExtra(KEY_MAIN_AMBIENT_LUX, event.getMainAmbientLux()).putExtra(KEY_ASSIST_AMBIENT_LUX, event.getAssistAmbientLux()).putExtra(KEY_LAST_MAIN_AMBIENT_LUX, event.getLastMainAmbientLux()).putExtra(KEY_LAST_ASSIST_AMBIENT_LUX, event.getLastAssistAmbientLux()).putExtra(KEY_ACC_VALUES, event.getAccValues()).putExtra(KEY_IS_USE_LIGHT_FOV_OPTIMIZATION, event.getIsUseLightFovOptimization()).putExtra(KEY_EXP_ID, event.getExpId());
    }

    public static void updateAdvancedEventIntent(Intent intent, AdvancedEvent event) {
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, ADVANCED_EVENT_NAME).putExtra("type", event.getEventType()).putExtra(KEY_AUTO_BRIGHTNESS_ANIMATION_DURATION, event.getAutoBrightnessAnimationDuration()).putExtra(KEY_CURRENT_ANIMATE_VALUE, event.getCurrentAnimateValue()).putExtra(KEY_TARGET_ANIMATE_VALUE, event.getTargetAnimateValue()).putExtra(KEY_USER_BRIGHTNESS, event.getUserBrightness()).putExtra(KEY_LONG_TERM_MODEL_SPLINE_ERROR, event.getLongTermModelSplineError()).putExtra(KEY_DEFAULT_SPLINE_ERROR, event.getDefaultSplineError()).putExtra(KEY_BRIGHTNESS_CHANGED_STATUS, event.getBrightnessChangedState()).putExtra(KEY_EXTRA, event.getExtra()).putExtra(KEY_TIME_STAMP, event.getTimeStamp()).putExtra(KEY_BRIGHTNESS_RESTRICTED_ENABLE, event.getBrightnessRestrictedEnable());
    }

    private static String getEventName(Integer key) {
        switch (key.intValue()) {
            case 0:
                return AggregationEvent.BrightnessAggregationEvent.EVENT_MANUAL_ADJUST_TIMES;
            case 1:
                return AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_ADJUST_TIMES;
            case 2:
                return AggregationEvent.BrightnessAggregationEvent.EVENT_AUTO_MANUAL_ADJUST_TIMES;
            case 3:
                return AggregationEvent.BrightnessAggregationEvent.EVENT_WINDOW_ADJUST_TIMES;
            case 4:
                return AggregationEvent.BrightnessAggregationEvent.EVENT_SUNLIGHT_ADJUST_TIMES;
            default:
                return null;
        }
    }

    private static Intent getBrightnessEventIntent(Context context) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, context.getPackageName()).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, BRIGHTNESS_EVENT_APP_ID);
        return intent;
    }
}
