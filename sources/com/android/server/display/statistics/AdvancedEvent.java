package com.android.server.display.statistics;

import android.os.Parcel;
import java.text.SimpleDateFormat;
import java.util.Date;

/* loaded from: classes.dex */
public class AdvancedEvent {
    public static final int BRIGHTNESS_CHANGE_STATE_DECREASE = 1;
    public static final int BRIGHTNESS_CHANGE_STATE_EQUAL = 2;
    public static final int BRIGHTNESS_CHANGE_STATE_INCREASE = 0;
    public static final int BRIGHTNESS_CHANGE_STATE_RESET = 3;
    public static final int EVENT_AUTO_BRIGHTNESS_ANIMATION_INFO = 1;
    public static final int EVENT_SCHEDULE_ADVANCED_EVENT = 2;
    private float auto_brightness_animation_duration;
    private int brightness_changed_state;
    private boolean brightness_restricted_enable;
    private int current_animate_value;
    private float default_spline_error;
    private String extra;
    private int interrupt_brightness_animation_times;
    private float long_term_model_spline_error;
    private int target_animate_value;
    private long time_stamp;
    private int type;
    private int user_brightness;
    private int user_reset_brightness_mode_times;

    public AdvancedEvent() {
        this.current_animate_value = -1;
        this.target_animate_value = -1;
        this.user_brightness = -1;
        this.auto_brightness_animation_duration = -1.0f;
        this.long_term_model_spline_error = -1.0f;
        this.default_spline_error = -1.0f;
        this.brightness_changed_state = -1;
        this.extra = "";
    }

    private AdvancedEvent(Parcel in) {
        this.current_animate_value = -1;
        this.target_animate_value = -1;
        this.user_brightness = -1;
        this.auto_brightness_animation_duration = -1.0f;
        this.long_term_model_spline_error = -1.0f;
        this.default_spline_error = -1.0f;
        this.brightness_changed_state = -1;
        this.extra = "";
        this.type = in.readInt();
        this.interrupt_brightness_animation_times = in.readInt();
        this.user_reset_brightness_mode_times = in.readInt();
        this.current_animate_value = in.readInt();
        this.target_animate_value = in.readInt();
        this.user_brightness = in.readInt();
        this.auto_brightness_animation_duration = in.readFloat();
        this.long_term_model_spline_error = in.readFloat();
        this.default_spline_error = in.readFloat();
        this.brightness_changed_state = in.readInt();
        this.extra = in.readString();
        this.time_stamp = in.readLong();
        this.brightness_restricted_enable = in.readBoolean();
    }

    public AdvancedEvent setEventType(int type) {
        this.type = type;
        return this;
    }

    public int getEventType() {
        return this.type;
    }

    public AdvancedEvent setInterruptBrightnessAnimationTimes(int times) {
        this.interrupt_brightness_animation_times = times;
        return this;
    }

    public int getInterruptBrightnessAnimationTimes() {
        return this.interrupt_brightness_animation_times;
    }

    public AdvancedEvent setUserResetBrightnessModeTimes(int times) {
        this.user_reset_brightness_mode_times = times;
        return this;
    }

    public int getUserResetBrightnessModeTimes() {
        return this.user_reset_brightness_mode_times;
    }

    public AdvancedEvent setCurrentAnimateValue(int value) {
        this.current_animate_value = value;
        return this;
    }

    public int getCurrentAnimateValue() {
        return this.current_animate_value;
    }

    public AdvancedEvent setTargetAnimateValue(int value) {
        this.target_animate_value = value;
        return this;
    }

    public int getTargetAnimateValue() {
        return this.target_animate_value;
    }

    public AdvancedEvent setUserBrightness(int value) {
        this.user_brightness = value;
        return this;
    }

    public int getUserBrightness() {
        return this.user_brightness;
    }

    public AdvancedEvent setAutoBrightnessAnimationDuration(float duration) {
        this.auto_brightness_animation_duration = duration;
        return this;
    }

    public float getAutoBrightnessAnimationDuration() {
        return this.auto_brightness_animation_duration;
    }

    public AdvancedEvent setLongTermModelSplineError(float error) {
        this.long_term_model_spline_error = error;
        return this;
    }

    public float getLongTermModelSplineError() {
        return this.long_term_model_spline_error;
    }

    public AdvancedEvent setDefaultSplineError(float error) {
        this.default_spline_error = error;
        return this;
    }

    public float getDefaultSplineError() {
        return this.default_spline_error;
    }

    public AdvancedEvent setBrightnessChangedState(int state) {
        this.brightness_changed_state = state;
        return this;
    }

    public int getBrightnessChangedState() {
        return this.brightness_changed_state;
    }

    public AdvancedEvent setExtra(String extra) {
        this.extra = extra;
        return this;
    }

    public String getExtra() {
        return this.extra;
    }

    public AdvancedEvent setTimeStamp(long timeStamp) {
        this.time_stamp = timeStamp;
        return this;
    }

    public long getTimeStamp() {
        return this.time_stamp;
    }

    public AdvancedEvent setBrightnessRestrictedEnable(boolean enable) {
        this.brightness_restricted_enable = enable;
        return this;
    }

    public boolean getBrightnessRestrictedEnable() {
        return this.brightness_restricted_enable;
    }

    public static String timestampToString(long time) {
        Date d = new Date(time);
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:sss");
        return sf.format(d);
    }

    public String convertToString() {
        return "type:" + this.type + ", auto_brightness_animation_duration:" + this.auto_brightness_animation_duration + ", current_animate_value:" + this.current_animate_value + ", target_animate_value:" + this.target_animate_value + ", user_brightness:" + this.user_brightness + ", long_term_model_spline_error:" + this.long_term_model_spline_error + ", default_spline_error:" + this.default_spline_error + ", brightness_changed_state:" + getBrightnessChangedState(this.brightness_changed_state) + ", extra:" + this.extra + ", time_stamp:" + timestampToString(this.time_stamp) + ", brightness_restricted_enable:" + this.brightness_restricted_enable;
    }

    private String getBrightnessChangedState(int state) {
        switch (state) {
            case 0:
                return "brightness increase";
            case 1:
                return "brightness decrease";
            case 2:
                return "brightness equal";
            default:
                return "brightness reset";
        }
    }
}
