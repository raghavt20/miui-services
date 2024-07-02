package com.android.server.audio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.MiuiXlog;
import android.media.Spatializer;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.os.Build;

/* loaded from: classes.dex */
public class MQSUtils {
    private static final String AUDIO_EVENT_ID = "31000000086";
    private static final String AUDIO_EVENT_NAME = "audio_button";
    private static final int AUDIO_VISUAL_ENABLED = 1;
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String HAPTIC_FEEDBACK_INFINITE_INTENSITY = "haptic_feedback_infinite_intensity";
    private static final String IS_HEARING_ASSIST_SUPPORT = "sound_transmit_ha_support";
    private static final String IS_TRANSMIT_SUPPORT = "sound_transmit_support";
    private static final String KEY_PARAM_RINGTONE_DEVICE = "set_spk_ring_filter_mask";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String PACKAGE_NAME = "com.miui.analytics";
    private static final String PARAM_RINGTONE_DEVICE_OFF = "set_spk_ring_filter_mask=3";
    private static final String PARAM_RINGTONE_DEVICE_ON = "set_spk_ring_filter_mask=0";
    private static final int SOUND_ASSIST_ENABLED = 1;
    private static final String SUPPORT_HEARING_ASSIST = "sound_transmit_ha_support=true";
    private static final String SUPPORT_TRANSMIT = "sound_transmit_support=true";
    private static final String TAG = "MiSound.MQSUtils";
    private static final String VIBRATOR_EVENT_ID = "31000000089";
    private static final String VIBRATOR_EVENT_NAME = "haptic_status";
    private static int day = 0;
    private static int month = 0;
    private static final String none = "none";
    private static int year;
    private Context mContext;
    private MiuiXlog mMiuiXlog = new MiuiXlog();
    private static String mMqsModuleId = "mqs_audio_data_21031000";
    private static float VIBRATION_DEFAULT_INFINITE_INTENSITY = 1.0f;
    private static final Set<String> mMusicWhiteList = new HashSet(Arrays.asList("com.netease.cloudmusic", "com.tencent.qqmusic", "com.iloen.melon", "mp3.player.freemusic", "com.kugou.android", "cn.kuwo.player", "com.google.android.apps.youtube.music", "com.tencent.blackkey", "cmccwm.mobilemusic", "com.migu.music.mini", "com.ting.mp3.android", "com.blueocean.musicplayer", "com.tencent.ibg.joox", "com.kugou.android.ringtone", "com.shoujiduoduo.dj", "com.spotify.music", "com.shoujiduoduo.ringtone", "com.hiby.music", "com.miui.player", "com.google.android.music", "com.tencent.ibg.joox", "com.skysoft.kkbox.android", "com.sofeh.android.musicstudio3", "com.gamestar.perfectpiano", "com.opalastudios.pads", "com.magix.android.mmjam", "com.musicplayer.playermusic", "com.gaana", "com.maxmpz.audioplayer", "com.melodis.midomiMusicIdentifier.freemium", "com.mixvibes.remixlive", "com.starmakerinteractive.starmaker", "com.smule.singandroid", "com.djit.apps.stream", "tunein.service", "com.shazam.android", "com.jangomobile.android", "com.pandoralite", "com.tube.hqmusic", "com.amazon.avod.thirdpartyclient", "com.atmusic.app", "com.rubycell.pianisthd", "com.agminstruments.drumpadmachine", "com.playermusic.musicplayerapp", "com.famousbluemedia.piano", "com.apple.android.music", "mb32r.musica.gratis.music.player.free.download", "com.famousbluemedia.yokee", "com.ss.android.ugc.trill"));

    /* loaded from: classes.dex */
    public static class WaveformInfo {
        public String attrs;
        public String effect;
        public String opPkg;
    }

    public MQSUtils(Context context) {
        this.mContext = context;
    }

    public static boolean trackConcurrentVoipInfo(Context context, List<String> clients) {
        if (context == null || clients == null || clients.size() == 0) {
            return false;
        }
        try {
            MQSUtils util = new MQSUtils(context);
            if (!util.isReportXiaomiServer()) {
                return false;
            }
            Bundle params = new Bundle();
            params.putString("voip_packages", Arrays.toString(clients.toArray()));
            context.startService(configureIntent(AUDIO_EVENT_ID, "concurrent_voip", params));
            return true;
        } catch (Exception e) {
            Log.d(TAG, "trackConcurrentVoipInfo exception : " + e);
            return false;
        }
    }

    public void reportAbnormalAudioStatus(AudioStateTrackData audioStateTrackData) {
        Log.d(TAG, "[TF-BT] reportAbnormalAudioStatus: " + audioStateTrackData);
        if (this.mContext == null || audioStateTrackData == null || !isReportXiaomiServer()) {
            return;
        }
        String result = String.format("{\"name\":\"check_audio_route_for_bluetooth\",\"audio_event\":{\"audio_route_abnormal_reason\":\"%s\",\"audio_exception_details\":\"%s\",\"package_name\":\"%s\",\"btName\":\"%s\",\"a2dp_connect_state\":\"%d\",\"sco_connect_state\":\"%d\",\"audio_status_name\":\"%s\"},\"dgt\":\"null\",\"audio_ext\":\"null\" }", audioStateTrackData.mAbnormalReason, audioStateTrackData.mEventSource, audioStateTrackData.mPackageName, audioStateTrackData.mBtName, Integer.valueOf(audioStateTrackData.mA2dpConnectState), Integer.valueOf(audioStateTrackData.mScoConnectState), audioStateTrackData.mAudioStateName);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Exception e) {
            Log.d(TAG, "[TF-BT] reportAbnormalAudioStatus exception : " + e);
        }
    }

    private static Intent configureIntent(String appId, String name, Bundle params) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
        intent.setFlags(3);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, appId);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, name);
        intent.putExtras(params);
        return intent;
    }

    public boolean needToReport() {
        Calendar calendar = Calendar.getInstance();
        if (year == calendar.get(1) && month == calendar.get(2) + 1 && day == calendar.get(5)) {
            return false;
        }
        year = calendar.get(1);
        month = calendar.get(2) + 1;
        day = calendar.get(5);
        Slog.d(TAG, "needToReport year: " + year + "month: " + month + "day: " + day);
        return true;
    }

    private boolean checkWirelessTransmissionSupport() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        String transmitSupport = audioManager.getParameters(IS_TRANSMIT_SUPPORT);
        return transmitSupport != null && transmitSupport.length() >= 1 && SUPPORT_TRANSMIT.equals(transmitSupport);
    }

    private boolean checkHearingAssistSupport() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        String hearingAssistSupport = audioManager.getParameters(IS_HEARING_ASSIST_SUPPORT);
        return hearingAssistSupport != null && hearingAssistSupport.length() >= 1 && SUPPORT_HEARING_ASSIST.equals(hearingAssistSupport);
    }

    private boolean checkVoiceprintNoiseReductionSupport() {
        return SystemProperties.getBoolean("ro.vendor.audio.voip.assistant", false);
    }

    private String checkAudioVisualStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.sfx.audiovisual", false)) {
            Slog.d(TAG, "device not support AudioVisual");
            return none;
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "audio_visual_screen_lock_on", 0) == 1) {
            return "open";
        }
        return "close";
    }

    private String checkHarmankardonStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.sfx.harmankardon", false)) {
            Slog.d(TAG, "device not support harmankardon");
            return none;
        }
        int flag = Settings.Global.getInt(this.mContext.getContentResolver(), "settings_system_harman_kardon_enable", 0);
        if (1 == flag) {
            return "open";
        }
        return "close";
    }

    private String checkMultiAppVolumeStatus() {
        int status = Settings.Global.getInt(this.mContext.getContentResolver(), "sound_assist_key", 0);
        if (status == 1) {
            return "open";
        }
        return "close";
    }

    private String checkHIFIStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.hifi", false)) {
            Slog.d(TAG, "device not support HIFI");
            return none;
        }
        boolean status = SystemProperties.getBoolean("persist.vendor.audio.hifi", false);
        Slog.i(TAG, "HiFi Switch status is :" + status);
        if (status) {
            return "open";
        }
        return "close";
    }

    private String checkMultiSoundStatus() {
        int status = Settings.Global.getInt(this.mContext.getContentResolver(), "key_ignore_music_focus_req", 0);
        if (status == 1) {
            return "open";
        }
        return "close";
    }

    private String checkAllowSpeakerToRing() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.ring.filter", false)) {
            Slog.d(TAG, "device not support AllowSpeakerToRing");
            return none;
        }
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        String paramStr = mAudioManager.getParameters(KEY_PARAM_RINGTONE_DEVICE);
        if (PARAM_RINGTONE_DEVICE_ON.equals(paramStr)) {
            return "open";
        }
        if (PARAM_RINGTONE_DEVICE_OFF.equals(paramStr)) {
            return "close";
        }
        return "Undefined";
    }

    private String checkEarsCompensationStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.sfx.earadj", false)) {
            Slog.d(TAG, "device not support EarsCompensation");
            return none;
        }
        String status = SystemProperties.get("persist.vendor.audio.ears.compensation.state", "");
        Slog.i(TAG, "ears compensation status is :" + status);
        if (status.equals("1")) {
            return "open";
        }
        return "close";
    }

    private String checkDolbySwitchStatus() {
        if (!SystemProperties.getBoolean("ro.vendor.audio.dolby.dax.support", false)) {
            Slog.d(TAG, "device not support dolby audio");
            return none;
        }
        boolean status = SystemProperties.getBoolean("persist.vendor.audio.misound.disable", false);
        Slog.i(TAG, "Dolby Switch status is :" + status);
        if (status) {
            return "open";
        }
        return "close";
    }

    private String check3DAudioSwitchStatus() {
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        Spatializer mSpatializer = mAudioManager.getSpatializer();
        if (mSpatializer.getImmersiveAudioLevel() > 0) {
            boolean spatializer_enabled = SystemProperties.getBoolean("ro.audio.spatializer_enabled", false);
            if (!spatializer_enabled) {
                Slog.d(TAG, "device not support 3D audio");
                return none;
            }
            boolean status = Settings.Global.getInt(this.mContext.getContentResolver(), "spatial_audio_feature_enable", 0) == 1;
            Slog.i(TAG, "3D Audio Switch status is :" + status);
            return status ? "open" : "close";
        }
        int flag = SystemProperties.getInt("ro.vendor.audio.feature.spatial", 0);
        if (flag == 0 || 1 == flag) {
            Slog.d(TAG, "device not support 3D audio");
            return none;
        }
        boolean status2 = SystemProperties.getBoolean("persist.vendor.audio.3dsurround.enable", false);
        Slog.i(TAG, "3D Audio Switch status is :" + status2);
        return status2 ? "open" : "close";
    }

    private String checkSpatialAudioSwitchStatus() {
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        Spatializer mSpatializer = mAudioManager.getSpatializer();
        if (mSpatializer.getImmersiveAudioLevel() > 0) {
            int mode = mSpatializer.getDesiredHeadTrackingMode();
            boolean status = mode == 1;
            Slog.i(TAG, "Spatial Audio Switch status is :" + status);
            return status ? "open" : "close";
        }
        int flag = SystemProperties.getInt("ro.vendor.audio.feature.spatial", 0);
        if (flag == 0 || 2 == flag) {
            Slog.d(TAG, "device not support spatial audio");
            return none;
        }
        boolean status2 = SystemProperties.getBoolean("persist.vendor.audio.spatial.enable", false);
        Slog.i(TAG, "Spatial Audio Switch status is :" + status2);
        return status2 ? "open" : "close";
    }

    private String checkMisoundStatus() {
        String status = SystemProperties.get("persist.vendor.audio.sfx.hd.music.state", "");
        Slog.i(TAG, "misound status is :" + status);
        if (status.equals("1")) {
            return "open";
        }
        if (status.equals("0")) {
            return "close";
        }
        return none;
    }

    private String checkMisoundHeadphoneType() {
        String type = SystemProperties.get("persist.vendor.audio.sfx.hd.type", "");
        Slog.i(TAG, "misound headphone type is :" + type);
        return type;
    }

    private String checkMisoundEqStatus() {
        String status;
        String eq = SystemProperties.get("persist.vendor.audio.sfx.hd.eq", "");
        Slog.i(TAG, "misound eq is :" + eq);
        if (eq.equals("")) {
            status = "null";
        } else if (eq.equals("0.000000,0.000000,0.000000,0.000000,0.000000,0.000000,0.000000")) {
            status = "close";
        } else {
            status = "open";
        }
        Slog.i(TAG, "misound eq status is :" + status);
        return status;
    }

    public void reportAudioSilentObserverToOnetrack(int level, String location, String reason, int type) {
        Log.d(TAG, "reportAudioSilentObserverToOnetrack, level: " + level + ", location: " + location + ", reason: " + reason + ", type: " + type);
        LocalDateTime date_time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String current_time = date_time.format(formatter);
        String result = String.format("{\"name\":\"audio_silent_observer\",\"audio_event\":{\"scenario\":\"%s\", \"location\":\"%s\", \"silent_reason\":\"%s\", \"level\":\"%d\", \"silent_type\":\"%d\", \"source_sink\":\"%s\", \"audio_device\":\"%s\", \"extra_info\":\"%s\"}, \"dgt\":\"null\",\"audio_ext\":\"null\" }", "", location, reason, Integer.valueOf(level), Integer.valueOf(type), "", "", current_time);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Exception e) {
            Log.d(TAG, "reportAudioSilentObserverToOnetrack exception : " + e);
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(32:2|3|4|(2:169|170)|6|7|8|(31:9|(1:11)(3:158|159|(2:161|162)(1:163))|12|13|(1:15)(2:156|157)|16|17|(1:19)(2:151|(2:153|154)(1:155))|20|21|(1:23)(2:149|150)|24|25|(1:27)(2:144|(2:146|147)(1:148))|28|29|(1:31)(2:139|(2:141|142)(1:143))|32|33|(1:35)(2:134|(2:136|137)(1:138))|36|37|(1:39)(2:129|(2:131|132)(1:133))|40|41|(1:43)(2:121|(2:123|124)(1:125))|44|45|46|47|48)|(4:(2:50|51)(2:111|(2:113|114)(26:115|53|54|55|56|(3:58|59|60)(2:105|106)|61|62|63|64|(2:66|67)(1:99)|68|69|(2:71|72)(1:98)|73|74|(2:76|77)(1:97)|78|79|(2:81|82)(1:96)|83|(1:85)|87|88|89|91))|88|89|91)|52|53|54|55|56|(0)(0)|61|62|63|64|(0)(0)|68|69|(0)(0)|73|74|(0)(0)|78|79|(0)(0)|83|(0)|87) */
    /* JADX WARN: Code restructure failed: missing block: B:100:0x0224, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x0228, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Removed duplicated region for block: B:105:0x01bf  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x01a9  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x01cd  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x01df  */
    /* JADX WARN: Removed duplicated region for block: B:76:0x01f1  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x0203  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x0215 A[Catch: Exception -> 0x0224, TRY_LEAVE, TryCatch #6 {Exception -> 0x0224, blocks: (B:63:0x01c5, B:67:0x01ce, B:68:0x01d6, B:72:0x01e0, B:73:0x01e8, B:77:0x01f2, B:78:0x01fa, B:82:0x0204, B:83:0x020c, B:85:0x0215, B:96:0x0208, B:97:0x01f6, B:98:0x01e4, B:99:0x01d2), top: B:62:0x01c5 }] */
    /* JADX WARN: Removed duplicated region for block: B:96:0x0208 A[Catch: Exception -> 0x0224, TryCatch #6 {Exception -> 0x0224, blocks: (B:63:0x01c5, B:67:0x01ce, B:68:0x01d6, B:72:0x01e0, B:73:0x01e8, B:77:0x01f2, B:78:0x01fa, B:82:0x0204, B:83:0x020c, B:85:0x0215, B:96:0x0208, B:97:0x01f6, B:98:0x01e4, B:99:0x01d2), top: B:62:0x01c5 }] */
    /* JADX WARN: Removed duplicated region for block: B:97:0x01f6 A[Catch: Exception -> 0x0224, TryCatch #6 {Exception -> 0x0224, blocks: (B:63:0x01c5, B:67:0x01ce, B:68:0x01d6, B:72:0x01e0, B:73:0x01e8, B:77:0x01f2, B:78:0x01fa, B:82:0x0204, B:83:0x020c, B:85:0x0215, B:96:0x0208, B:97:0x01f6, B:98:0x01e4, B:99:0x01d2), top: B:62:0x01c5 }] */
    /* JADX WARN: Removed duplicated region for block: B:98:0x01e4 A[Catch: Exception -> 0x0224, TryCatch #6 {Exception -> 0x0224, blocks: (B:63:0x01c5, B:67:0x01ce, B:68:0x01d6, B:72:0x01e0, B:73:0x01e8, B:77:0x01f2, B:78:0x01fa, B:82:0x0204, B:83:0x020c, B:85:0x0215, B:96:0x0208, B:97:0x01f6, B:98:0x01e4, B:99:0x01d2), top: B:62:0x01c5 }] */
    /* JADX WARN: Removed duplicated region for block: B:99:0x01d2 A[Catch: Exception -> 0x0224, TryCatch #6 {Exception -> 0x0224, blocks: (B:63:0x01c5, B:67:0x01ce, B:68:0x01d6, B:72:0x01e0, B:73:0x01e8, B:77:0x01f2, B:78:0x01fa, B:82:0x0204, B:83:0x020c, B:85:0x0215, B:96:0x0208, B:97:0x01f6, B:98:0x01e4, B:99:0x01d2), top: B:62:0x01c5 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void reportAudioButtonStatus() {
        /*
            Method dump skipped, instructions count: 597
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.MQSUtils.reportAudioButtonStatus():void");
    }

    public void reportAudioVisualDailyUse(List<AudioPlaybackConfiguration> currentPlayback) {
        String status = checkAudioVisualStatus();
        status.equals("open");
    }

    private String checkHapticStatus() {
        int status = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 1000, -2);
        if (status == 1) {
            return "open";
        }
        if (status == 0) {
            return "close";
        }
        return none;
    }

    private float getHapticFeedbackFloatLevel() {
        return Settings.System.getFloatForUser(this.mContext.getContentResolver(), HAPTIC_FEEDBACK_INFINITE_INTENSITY, VIBRATION_DEFAULT_INFINITE_INTENSITY, -2);
    }

    public void reportBtMultiVoipDailyUse(Context context, String scoState, String multiVoipPackages) {
        if (context == null) {
            return;
        }
        Slog.i(TAG, "reportBtMultiVoipDailyUse start, scoState=" + scoState + ", multiVoipPackages=" + multiVoipPackages);
        try {
            if (!isReportXiaomiServer()) {
                return;
            }
            Bundle params = new Bundle();
            params.putString("sco_state_internal", scoState);
            params.putString("voip_packages", multiVoipPackages);
            context.startService(configureIntent(AUDIO_EVENT_ID, "multi_voip_communication_for_bluetooth_device", params));
        } catch (Exception e) {
            e.printStackTrace();
            Slog.d(TAG, "error for reportBtMultiVoipDailyUse");
        }
    }

    public void reportWaveformInfo(WaveformInfo vibratorInfo) {
        Slog.i(TAG, "report WaveformInfo start.");
        String result = String.format("{\"name\":\"waveform_info\",\"audio_event\":{\"waveform_package_name\":\"%s\",\"waveform_usage\":\"%s\",\"waveform_effect\":\"%s\"},\"dgt\":\"null\",\"audio_ext\":\"null\" }", vibratorInfo.opPkg, vibratorInfo.attrs, vibratorInfo.effect);
        Log.d(TAG, "reportWaveformInfo:" + result);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Throwable th) {
            Log.e(TAG, "can not use miuiXlogSend!!!");
        }
    }

    public void reportVibrateStatus() {
        if (!isReportXiaomiServer()) {
            return;
        }
        Slog.i(TAG, "reportVibrateStatus start.");
        int vibrate_ring = Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_when_ringing", 1);
        int vibrate_silent = Settings.System.getInt(this.mContext.getContentResolver(), "vibrate_in_silent", 1);
        String haptic_status = checkHapticStatus();
        float haptic_level = getHapticFeedbackFloatLevel();
        try {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            if (!isGlobalBuild()) {
                intent.setFlags(2);
            }
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, VIBRATOR_EVENT_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, VIBRATOR_EVENT_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
            Bundle params = new Bundle();
            if (vibrate_ring == 1) {
                intent.putExtra("status_ring", true);
            } else {
                intent.putExtra("status_ring", false);
            }
            if (vibrate_silent == 1) {
                intent.putExtra("status_silent", true);
            } else {
                intent.putExtra("status_silent", false);
            }
            if (haptic_status.equals("open")) {
                intent.putExtra("haptic_intensity_status", true);
                intent.putExtra("haptic_intensity_position", haptic_level);
            }
            intent.putExtras(params);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            this.mContext.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Slog.d(TAG, "erroe for reportVibrate");
        }
    }

    private boolean isGlobalBuild() {
        return Build.IS_INTERNATIONAL_BUILD;
    }

    private boolean isNotAllowedRegion() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("IN") || region.equals("");
    }

    private boolean isReportXiaomiServer() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("CN") || region.equals("RU");
    }

    /* loaded from: classes.dex */
    static class AudioStateTrackData {
        final int mA2dpConnectState;
        final String mAbnormalReason;
        final String mAudioStateName;
        final String mBtName;
        final String mEventSource;
        final String mPackageName;
        final int mScoConnectState;

        public AudioStateTrackData(int errorType, String eventSource, String packageName, String btName, int a2dpConnectState, int scoConnectState, String audioStateName) {
            this.mAbnormalReason = audioErrorTypeToString(errorType);
            this.mEventSource = eventSource;
            this.mPackageName = packageName;
            this.mBtName = btName;
            this.mA2dpConnectState = a2dpConnectState;
            this.mScoConnectState = scoConnectState;
            this.mAudioStateName = audioStateName;
        }

        public static String audioErrorTypeToString(int errorType) {
            switch (errorType) {
                case -1:
                    return "ok";
                case 0:
                default:
                    return "unknown error:" + errorType;
                case 1:
                    return "modeTimeOutException";
                case 2:
                    return "ScoTimeOutException";
                case 3:
                    return "externalAppScoException";
                case 4:
                    return "btConnectionException";
                case 5:
                    return "audioScoException";
                case 6:
                    return "bluetoothScoException";
                case 7:
                    return "audioPolicyManagerException";
                case 8:
                    return "audioLeaky";
            }
        }

        public String toString() {
            return "AudioStateTrackData: mAbnormalReason=" + this.mAbnormalReason + ", mEventSource=" + this.mEventSource + ", mPackageName=" + this.mPackageName + ", mBtName=" + this.mBtName + ", mScoConnectState=" + this.mScoConnectState + ", mA2dpConnectState=" + this.mA2dpConnectState;
        }
    }
}
