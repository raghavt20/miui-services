package com.android.server.audio;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioDeviceAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.AudioServiceInjector;
import android.media.AudioSystem;
import android.media.MiuiAudioRecord;
import android.media.MiuiXlog;
import android.media.audiofx.AudioEffectCenter;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioMixingRule;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CombinedVibration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.audio.AudioDeviceBroker;
import com.android.server.audio.AudioParameterClient;
import com.android.server.audio.AudioService;
import com.android.server.audio.MQSUtils;
import com.android.server.audio.dolbyeffect.DolbyEffectController;
import com.android.server.audio.feature.FeatureAdapter;
import com.android.server.audio.foldable.FoldableAdapter;
import com.android.server.audio.pad.PadAdapter;
import com.android.server.content.SyncManagerStubImpl;
import com.android.server.pm.CloudControlPreinstallService;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.security.AccessControlImpl;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import miui.android.animation.internal.AnimTask;
import miui.app.StorageRestrictedPathManager;
import miui.tipclose.TipHelperProxy;
import miui.util.AudioManagerHelper;

@MiuiStubHead(manifestName = "com.android.server.audio.AudioServiceStub$$")
/* loaded from: classes.dex */
public class AudioServiceStubImpl extends AudioServiceStub {
    private static final String ABNORMAL_AUDIO_INFO_EVENT_NAME = "check_audio_route_for_bluetooth";
    public static final String ACTION_VOLUME_BOOST = "miui.intent.action.VOLUME_BOOST";
    private static final String AUDIO_ADSP_SPATIALIZER_AVAILABLE = "ro.vendor.audio.adsp.spatial";
    private static final String AUDIO_ADSP_SPATIALIZER_ENABLE = "persist.vendor.audio.dolbysurround.enable";
    private static final int AUDIO_BT_CONFIG_PROP = SystemProperties.getInt("ro.vendor.audio.bt.adapter", 0);
    private static final String AUDIO_CAMERA_BT_RECORD_SUPPORT = "ro.vendor.audio.camera.bt.record.support";
    private static final String AUDIO_CINEMA_MODE_SUPPORT = "persist.vendor.audio.cinema.support";
    private static final String AUDIO_DOLBY_CONTROL_SUPPORT = "vendor.audio.dolby.control.support";
    private static final String AUDIO_ONETRACK_SILENT_OBSERVER = "audio_silent_observer";
    private static final String AUDIO_PARAMETER_DEFAULT = "sound_transmit_enable=0";
    private static final String AUDIO_PARAMETER_HA = "sound_transmit_enable=6";
    private static final String AUDIO_PARAMETER_KARAOKE_OFF = "audio_karaoke_enable=0";
    private static final String AUDIO_PARAMETER_KARAOKE_ON = "audio_karaoke_enable=1";
    private static final String AUDIO_PARAMETER_KTV_OFF = "audio_karaoke_ktvmode=disable";
    private static final String AUDIO_PARAMETER_KTV_ON = "audio_karaoke_ktvmode=enable";
    private static final String AUDIO_PARAMETER_WT = "sound_transmit_enable=1";
    private static final String AUDIO_POWER_SAVE_SETTING = "persist.vendor.audio.power.save.setting";
    private static final String AUDIO_SILENT_LEVEL = "audio_silent_level";
    private static final String AUDIO_SILENT_LOCATION = "audio_silent_location";
    private static final String AUDIO_SILENT_REASON = "audio_silent_reason";
    private static final String AUDIO_SILENT_TYPE = "audio_silent_type";
    private static final String CAMERA_AUDIO_HEADSET_STATE = "audio_headset_state";
    private static final String CAMERA_PACKAGE_NAME = "com.android.camera";
    private static final String[] DEFAULT_AUDIO_PARAMETERS;
    private static final int DELAY_NOTIFY_BT_MAX_NUM = 3;
    private static final int DELAY_NOTIFY_BT_STOPBLUETOOTHSCO_MS = 200;
    public static final String EXTRA_BOOST_STATE = "volume_boost_state";
    private static final int FLAG_REALLY_TIME_MODE_ENABLED = 1;
    private static final int FLAG_SOUND_LEAK_PROTECTION_ENABLED = 2;
    private static final String HEADPHONES_EVENT_NAME = "headphones";
    static List<String> HIFI_NOT_SUPPORT_DEVICE_LIST = null;
    private static final String KEY_PERSIST_CUMULATIVE_PLAYBACK_MS = "key_persist_cumulative_playback_ms";
    private static final String KEY_PERSIST_NOTIFICATION_DATE = "key_persist_notification_date";
    private static final String KEY_PERSIST_PLAYBACK_CONTINUOUS_MS = "key_persist_playback_continuous_ms";
    private static final String KEY_PERSIST_PLAYBACK_HIGH_VOICE = "key_persist_playback_high_voice";
    private static final String KEY_PERSIST_PLAYBACK_START_MS = "key_persist_playback_start_ms";
    public static final int LONG_TIME_PROMPT = 2;
    public static final int MAX_VOLUME_LONG_TIME = 3;
    private static final int MAX_VOLUME_VALID_TIME_INTERVAL = 604800000;
    private static final int MIUI_MAX_MUSIC_VOLUME_STEP = 15;
    private static final int MQSSERVER_REPORT_RATE_MS;
    private static final int MSG_DATA_TRACK = 1;
    private static final int MSG_MQSSERVER_REPORT = 6;
    private static final int MSG_REPORT_ABNORMAL_AUDIO_STATE = 5;
    private static final int MSG_REPORT_AUDIO_SILENT_OBSERVER = 4;
    private static final int MSG_REPORT_MULTI_VOIP_DAILY_USE_FOR_BT_CONNECT = 2;
    private static final int MSG_REPORT_WAVEFORM_INFO_TRACK = 3;
    private static final int MSG_UPDATE_AUDIO_MODE = 7;
    private static final int MSG_UPDATE_MEDIA_STATE = 9;
    private static final int MSG_UPDATE_MEETING_MODE = 8;
    private static final int MUSIC_ACTIVE_CONTINUOUS_MS_MAX = 3600000;
    public static final int MUSIC_ACTIVE_CONTINUOUS_POLL_PERIOD_MS = 60000;
    private static final int MUSIC_ACTIVE_DATA_REPORT_INTERVAL = 60000;
    private static final int MUSIC_ACTIVE_RETRY_POLL_PERIOD_MS = 30000;
    private static final int NOTE_USB_HEADSET_PLUG = 1397122662;
    public static final int READ_MUSIC_PLAY_BACK_DELAY = 10000;
    private static final int REPORT_AUDIO_EXCEPTION_INFO_INTERVAL_TIME = 6000;
    private static final int SCO_DEVICES_MAX_NUM = 10;
    private static final String STATE_AUDIO_SCO_CONNECTED = "connect";
    private static final String STATE_AUDIO_SCO_DISCONNECTED = "disconnect";
    private static final String TAG = "AudioServiceStubImpl";
    private static final String[] TRANSMIT_AUDIO_PARAMETERS;
    private static final String WAVEFORM_EVENT_NAME = "waveform_info";
    public static boolean mIsAudioPlaybackTriggerSupported;
    private final boolean REALLY_TIME_MODE_ENABLED;
    private final boolean SOUND_LEAK_PROTECTION_ENABLED;
    private int cameraToastServiceRiid;
    private int mA2dpDeviceConnectedState;
    private AudioGameEffect mAudioGameEffect;
    private AudioManager mAudioManager;
    private int mAudioMode;
    private final List<AudioParameterClient> mAudioParameterClientList;
    private AudioPowerSaveModeObserver mAudioPowerSaveModeObserver;
    private AudioQueryWeatherService mAudioQueryWeatherService;
    private AudioService mAudioService;
    private AudioThermalObserver mAudioThermalObserver;
    private BtHelper mBtHelper;
    private String mBtName;
    private CloudServiceThread mCloudService;
    private List<String> mCommunicationRouteClients;
    private Context mContext;
    private long mContinuousMs;
    public long mCumulativePlaybackEndTime;
    public long mCumulativePlaybackStartTime;
    private int mDelayNotifyBtStopScoCount;
    private AudioDeviceBroker mDeviceBroker;
    private EffectChangeBroadcastReceiver mEffectChangeBroadcastReceiver;
    private FeatureAdapter mFeatureAdapter;
    private FoldableAdapter mFoldableAdapter;
    private GameAudioEnhancer mGameAudioEnhancer;
    private Handler mHandler;
    private int mHeadsetDeviceConnectedState;
    private int mHighLevel;
    private boolean mInDelayNotifyBtStopSco;
    private final boolean mIsSupportCinemaMode;
    private final boolean mIsSupportFWAudioEffectCenter;
    private boolean mIsSupportedReportAudioRouteState;
    private MiAudioServiceMTK mMiAudioServiceMTK;
    private MiuiXlog mMiuiXlog;
    private int mModeOwnerPid;
    private MQSUtils mMqsUtils;
    private String mMultiVoipPackages;
    private long mMusicPlaybackContinuousMs;
    private long mMusicPlaybackContinuousMsTotal;
    private float mMusicVolumeDb;
    private int mMusicVolumeIndex;
    private NotificationManager mNm;
    private LocalDate mNotificationDate;
    private int mNotificationTimes;
    private PadAdapter mPadAdapter;
    private AudioParameterClient.ClientDeathListener mParameterClientDeathListener;
    public long mPlaybackEndTime;
    public long mPlaybackStartTime;
    private AudioDeviceAttributes mPreferredCommunicationDevice;
    private float[] mPrescaleAbsoluteVolume;
    int mReceiveNotificationDevice;
    private String[] mRegisterContentName;
    private String mRequesterPackageForAudioMode;
    private int mScoBtState;
    private final LinkedList<AudioDeviceBroker.CommunicationDeviceInfo> mSetScoCommunicationDevice;
    private long mStartMs;
    private boolean mSuperVoiceVolumeOn;
    private boolean mSuperVoiceVolumeSupported;
    private boolean mSuperVolumeOn;
    private final int mVolumeAttenuation;
    private boolean mVolumeBoostEnabled;
    private HandlerThread mWorkerThread;
    public boolean markBluetoothheadsetstub;
    private long sLastAudioStateExpTime;
    private int sLastReportAudioErrorType;
    private boolean isSupportPollAudioMicStatus = true;
    private final boolean mIsAdspSpatializerAvailable = SystemProperties.getBoolean(AUDIO_ADSP_SPATIALIZER_AVAILABLE, false);
    private final boolean mIsSupportedCameraRecord = "true".equals(SystemProperties.get(AUDIO_CAMERA_BT_RECORD_SUPPORT));
    private final boolean mIsSupportedDolbyEffectControl = "true".equals(SystemProperties.get(AUDIO_DOLBY_CONTROL_SUPPORT));
    private final int mSettingAudioPowerSave = SystemProperties.getInt(AUDIO_POWER_SAVE_SETTING, 0);

    /* loaded from: classes.dex */
    public interface AudioPowerSaveEnable {
        public static final int AUDIOPOWERSAVE_AUDIOBIN_ENABLE = 4;
        public static final int AUDIOPOWERSAVE_MAIN_ENABLE = 1;
        public static final int AUDIOPOWERSAVE_OUTPUT_ENABLE = 2;
        public static final int AUDIOPOWERSAVE_VOICETRIGGER_ENABLE = 8;
        public static final int AUDIOPOWERSAVE_VOLUME_ENABLE = 16;
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AudioServiceStubImpl> {

        /* compiled from: AudioServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AudioServiceStubImpl INSTANCE = new AudioServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public AudioServiceStubImpl m771provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public AudioServiceStubImpl m770provideNewInstance() {
            return new AudioServiceStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        HIFI_NOT_SUPPORT_DEVICE_LIST = arrayList;
        arrayList.add("scorpio");
        HIFI_NOT_SUPPORT_DEVICE_LIST.add("lithium");
        MQSSERVER_REPORT_RATE_MS = SystemProperties.getInt("ro.vendor.audio.onetrack.upload.interval", 0);
        mIsAudioPlaybackTriggerSupported = true;
        DEFAULT_AUDIO_PARAMETERS = new String[]{AUDIO_PARAMETER_DEFAULT, AUDIO_PARAMETER_DEFAULT, AUDIO_PARAMETER_KARAOKE_OFF, AUDIO_PARAMETER_KTV_OFF};
        TRANSMIT_AUDIO_PARAMETERS = new String[]{AUDIO_PARAMETER_WT, AUDIO_PARAMETER_HA, AUDIO_PARAMETER_KARAOKE_ON, AUDIO_PARAMETER_KTV_ON};
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class EffectChangeBroadcastReceiver extends BroadcastReceiver {
        private EffectChangeBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("miui.intent.action.ACTION_AUDIO_EFFECT_CHANGED".equals(action)) {
                AudioServiceStubImpl.this.sendEffectRefresh();
            }
        }
    }

    public AudioServiceStubImpl() {
        int i = AUDIO_BT_CONFIG_PROP;
        this.REALLY_TIME_MODE_ENABLED = (i & 1) != 0;
        this.SOUND_LEAK_PROTECTION_ENABLED = (i & 2) != 0;
        this.mIsSupportCinemaMode = "true".equals(SystemProperties.get(AUDIO_CINEMA_MODE_SUPPORT));
        this.mIsSupportFWAudioEffectCenter = SystemProperties.getBoolean("ro.vendor.audio.fweffect", false);
        this.sLastAudioStateExpTime = 0L;
        this.sLastReportAudioErrorType = -1;
        this.mHeadsetDeviceConnectedState = 0;
        this.mScoBtState = 10;
        this.mA2dpDeviceConnectedState = 0;
        this.mCommunicationRouteClients = new LinkedList();
        this.mSetScoCommunicationDevice = new LinkedList<>();
        this.mVolumeAttenuation = SystemProperties.getInt("ro.vendor.audio.playbackcapture.attenuation", 20);
        this.mDelayNotifyBtStopScoCount = 0;
        this.mInDelayNotifyBtStopSco = false;
        this.mMultiVoipPackages = "";
        this.mModeOwnerPid = 0;
        this.mMiuiXlog = new MiuiXlog();
        this.mPrescaleAbsoluteVolume = new float[]{0.5f, 0.7f, 0.85f, 0.9f, 0.95f};
        this.mRegisterContentName = new String[]{"zen_mode", "notification_sound", "calendar_alert", "notes_alert", "sms_received_sound", "sms_received_sound_slot_1", "sms_received_sound_slot_2", "random_note_mode_random_sound_number", "random_note_mode_sequence_sound_number", "random_note_mode_sequence_time_interval_ms", "random_note_mode_mute_time_interval_ms"};
        this.mVolumeBoostEnabled = false;
        this.mSuperVoiceVolumeSupported = SystemProperties.get("ro.vendor.audio.voice.super_volume").equals("true");
        this.mReceiveNotificationDevice = BroadcastQueueModernStubImpl.FLAG_IMMUTABLE;
        this.mNotificationTimes = 0;
        this.cameraToastServiceRiid = -1;
        this.mSuperVolumeOn = false;
        this.mNotificationDate = null;
        this.mMusicPlaybackContinuousMs = 0L;
        this.mMusicPlaybackContinuousMsTotal = 0L;
        this.mPlaybackStartTime = 0L;
        this.mPlaybackEndTime = 0L;
        this.mStartMs = 0L;
        this.mContinuousMs = 0L;
        this.mHighLevel = 0;
        this.mCumulativePlaybackStartTime = 0L;
        this.mCumulativePlaybackEndTime = 0L;
        this.markBluetoothheadsetstub = false;
        this.mAudioParameterClientList = new ArrayList();
        this.mParameterClientDeathListener = null;
        this.mSuperVoiceVolumeOn = false;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mWorkerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new H(this.mWorkerThread.getLooper());
    }

    public boolean realTimeModeEnabled() {
        Context context = this.mContext;
        if (context != null) {
            String packageName = context.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (!"android.media.audio.cts".equals(packageName)) {
                return this.REALLY_TIME_MODE_ENABLED;
            }
            return false;
        }
        return false;
    }

    public boolean soundLeakProtectionEnabled() {
        return this.SOUND_LEAK_PROTECTION_ENABLED;
    }

    public void startAudioQueryWeatherService(Context context) {
        this.mContext = context;
        AudioQueryWeatherService audioQueryWeatherService = new AudioQueryWeatherService(this.mContext);
        this.mAudioQueryWeatherService = audioQueryWeatherService;
        audioQueryWeatherService.onCreate();
        CloudServiceThread cloudServiceThread = new CloudServiceThread(this.mContext);
        this.mCloudService = cloudServiceThread;
        cloudServiceThread.start();
    }

    public void startPollAudioMicStatus(Context context) {
        Log.d(TAG, "startPollAudioMicStatus");
        if (this.isSupportPollAudioMicStatus) {
            AudioDeviceMoniter.getInstance(context).startPollAudioMicStatus();
        }
    }

    public boolean isSupportSteplessVolume(int stream, String callingPackage) {
        return stream == 3 && !isCtsVerifier(callingPackage);
    }

    public int getMusicVolumeStep(int stream, String callingPackage, int maxVolume) {
        if (isSupportSteplessVolume(stream, callingPackage)) {
            Log.d(TAG, "adjustStreamVolume(): SupportSteplessVolume, maxVolume = " + maxVolume);
            return maxVolume / 15;
        }
        return 1;
    }

    public int getAbsoluteVolumeIndex(int index, int indexMax) {
        int step = indexMax / AnimTask.MAX_PAGE_SIZE;
        if (index == 0) {
            return 0;
        }
        if (index > 0 && index <= step * 5) {
            BigDecimal bd = new BigDecimal(Math.ceil((index - step) / step));
            int pos = bd.intValue();
            return ((int) (indexMax * this.mPrescaleAbsoluteVolume[pos])) / 10;
        }
        return (indexMax + 5) / 10;
    }

    public int getRingerMode(Context context, int mode) {
        int miuiMode = AudioManagerHelper.getValidatedRingerMode(context, mode);
        Log.d(TAG, "getRingerMode originMode" + mode + " destMode=" + miuiMode);
        return miuiMode;
    }

    public boolean musicVolumeAdjustmentAllowed(int zenMode, int streamAlias, ContentResolver cr) {
        if (streamAlias == 3 && zenMode == 1) {
            return isMuteMusicFromMIUI(cr);
        }
        return false;
    }

    public boolean enableVoiceVolumeBoost(int dir, boolean isMax, int device, int alias, String pkg, int mode, Context context) {
        if (isCtsVerifier(pkg) || !needEnableVoiceVolumeBoost(dir, isMax, device, alias, mode)) {
            return false;
        }
        if (dir == 1 && !this.mVolumeBoostEnabled) {
            setVolumeBoost(true, context);
            return true;
        }
        if (dir != -1 || !this.mVolumeBoostEnabled) {
            return false;
        }
        setVolumeBoost(false, context);
        return true;
    }

    public void updateVolumeBoostState(int audioMode, int modeOwnerPid, Context context) {
        if (audioMode == 2 && this.mVolumeBoostEnabled) {
            setVolumeBoost(false, context);
        } else if (audioMode == 0) {
            TipHelperProxy.getInstance().hideTipForPhone();
        }
        this.mAudioMode = audioMode;
        Log.d(TAG, "updateVolumeBoostState audiomode " + this.mAudioMode);
    }

    public boolean superVoiceVolumeChanged(int device, int alias, int mode, String pkg, int dir, int curIdx, int maxIdx, Context context) {
        if (device != 2 || alias != 0) {
            return false;
        }
        if ((mode != 2 && mode != 3) || !this.mSuperVoiceVolumeSupported || isCtsVerifier(pkg)) {
            return false;
        }
        Log.d(TAG, "SuperVoiceVolumeChanged device=" + device + " stream=" + alias + " mode=" + mode + " dir=" + dir + " cur=" + curIdx + " max=" + maxIdx);
        if (dir == 1 && curIdx + 10 == maxIdx && !this.mSuperVoiceVolumeOn) {
            setSuperVoiceVolume(true, context);
            return true;
        }
        if (dir != -1 || curIdx != maxIdx || !this.mSuperVoiceVolumeOn) {
            return false;
        }
        setSuperVoiceVolume(false, context);
        return true;
    }

    public void onUpdateAudioMode(int audioMode, String requesterPackage, Context context) {
        this.mAudioMode = audioMode;
        if (audioMode == 2 || audioMode == 3) {
            TipHelperProxy.getInstance().showTipForPhone(false, requesterPackage);
        } else if (audioMode == 0) {
            TipHelperProxy.getInstance().hideTipForPhone();
        }
        Log.d(TAG, "onUpdateAudioMode audiomode " + this.mAudioMode + " package:" + requesterPackage);
        this.mRequesterPackageForAudioMode = requesterPackage;
        this.mHandler.obtainMessage(7, Integer.valueOf(audioMode)).sendToTarget();
    }

    public void onUpdateMediaState(boolean mediaActive) {
        Log.d(TAG, "onUpdateMediaState mediaActive=" + mediaActive);
        this.mHandler.obtainMessage(9, Boolean.valueOf(mediaActive)).sendToTarget();
    }

    public void enableHifiVolume(int stream, int direction, int oldIndex, int indexMax, Context context) {
        if (shouldAdjustHiFiVolume(stream, direction, oldIndex, indexMax, context)) {
            adjustHiFiVolume(direction, context);
        }
    }

    public void setHifiVolume(Context context, int volume) {
        AudioManagerHelper.setHiFiVolume(context, volume);
    }

    public void showDeviceConnectNotification(final Context context, int device, final boolean isShow) {
        if ((this.mReceiveNotificationDevice & device) != 0 && context != null) {
            if (this.mNm == null) {
                this.mNm = (NotificationManager) context.getSystemService("notification");
            }
            new Handler().post(new Runnable() { // from class: com.android.server.audio.AudioServiceStubImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    if (isShow) {
                        AudioServiceStubImpl.this.createNotification(context);
                    } else {
                        AudioServiceStubImpl.this.mNm.cancel(AudioServiceStubImpl.NOTE_USB_HEADSET_PLUG);
                    }
                }
            });
        }
    }

    public void showVisualEffectNotification(Context context, int uid, int event) {
        AudioServiceInjector.showNotification(uid, event, context);
    }

    public void showVisualEffect(Context context, String action, List<AudioPlaybackConfiguration> apcList, Handler handler) {
        AudioServiceInjector.startAudioVisualIfsatisfiedWith(action, apcList, handler);
        reportAudioStatus(context, apcList);
    }

    public void reportAudioHwState(Context context) {
        MQSAudioHardware.getInstance(context).onetrack();
    }

    public void registerContentObserverForMiui(ContentResolver contentResolver, boolean notifyForDescendents, ContentObserver observer, int userAll) {
        contentResolver.registerContentObserver(Settings.System.getUriFor("mute_music_at_silent"), notifyForDescendents, observer, userAll);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("sound_assist_key"), notifyForDescendents, observer);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("key_ignore_music_focus_req"), notifyForDescendents, observer);
        int i = 0;
        while (true) {
            String[] strArr = this.mRegisterContentName;
            if (i < strArr.length) {
                contentResolver.registerContentObserver(Settings.System.getUriFor(strArr[i]), notifyForDescendents, observer);
                i++;
            } else {
                return;
            }
        }
    }

    public void uriState(Uri uri, Context context, ContentResolver contentResolver) {
        if (uri.equals(Settings.Global.getUriFor("sound_assist_key"))) {
            PlaybackActivityMonitorStub.get().loadSoundAssistSettings(contentResolver);
            PlaybackActivityMonitorStub.get().resetPlayerVolume(context);
        } else if (uri.equals(Settings.Global.getUriFor("key_ignore_music_focus_req"))) {
            PlaybackActivityMonitorStub.get().loadSoundAssistSettings(contentResolver);
        }
    }

    boolean isSoundAssistantFunction(Uri uri) {
        return uri.equals(Settings.Global.getUriFor("key_ignore_music_focus_req")) || uri.equals(Settings.Global.getUriFor("sound_assist_key"));
    }

    public void adjustVolume(AudioPlaybackConfiguration apc, float volume) {
        if (volume <= 1.0f) {
            if (volume < MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
                volume = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            }
            PlaybackActivityMonitorStub.get().setPlayerVolume(apc, volume, TAG, this.mContext);
        }
    }

    public void dumpMediaSound(PrintWriter pw) {
        pw.print("  mOpenSoundAssist=");
        pw.println(PlaybackActivityMonitorStub.get().isSoundAssistOpen());
        pw.print("  mIgnrMusicFocusReq=");
        pw.println(PlaybackActivityMonitorStub.get().isForceIgnoreGranted());
    }

    public IBinder createMiuiAudioRecord(ParcelFileDescriptor sharedMem, long size) {
        return new MiuiAudioRecord(sharedMem.getFileDescriptor(), size);
    }

    public IBinder createAudioRecordForLoopbackWithClient(ParcelFileDescriptor sharedMem, long size, IBinder token) {
        return new MiuiAudioRecord(sharedMem.getFileDescriptor(), size, token);
    }

    public String getNotificationUri(String type) {
        int SunriseTimeHours = this.mAudioQueryWeatherService.getSunriseTimeHours();
        int SunriseTimeMins = this.mAudioQueryWeatherService.getSunriseTimeMins();
        int SunsetTimeHours = this.mAudioQueryWeatherService.getSunsetTimeHours();
        int SunsetTimeMins = this.mAudioQueryWeatherService.getSunsetTimeMins();
        AudioServiceInjector.setDefaultTimeZoneStatus(this.mAudioQueryWeatherService.getDefaultTimeZoneStatus());
        AudioServiceInjector.setSunriseAndSunsetTime(SunriseTimeHours, SunriseTimeMins, SunsetTimeHours, SunsetTimeMins);
        AudioServiceInjector.checkSunriseAndSunsetTimeUpdate(this.mContext);
        return AudioServiceInjector.getNotificationUri(type);
    }

    public void foldInit() {
        FoldHelper.init();
    }

    public void foldEnable() {
        FoldHelper.enable();
    }

    public void foldDisable() {
        FoldHelper.disable();
    }

    public void startMqsServer(Context context) {
        MQSserver.getInstance(context);
    }

    public static boolean isBluetoothHeadsetDevice(BluetoothClass bluetoothClass) {
        if (bluetoothClass == null) {
            return false;
        }
        int deviceClass = bluetoothClass.getDeviceClass();
        return deviceClass == 1048 || deviceClass == 1028;
    }

    public void setBluetoothHeadset(BluetoothDevice btDevice) {
        BluetoothClass bluetoothClass = btDevice.getBluetoothClass();
        if (bluetoothClass == null) {
            Log.w(TAG, "bluetoothClass is null");
            return;
        }
        this.markBluetoothheadsetstub = isBluetoothHeadsetDevice(bluetoothClass);
        int deviceClass = bluetoothClass.getDeviceClass();
        int majorClass = bluetoothClass.getMajorDeviceClass();
        Log.d(TAG, "majorClass:" + Integer.toHexString(majorClass) + ", deviceClass:" + Integer.toHexString(deviceClass) + ", markBluetoothhead:" + this.markBluetoothheadsetstub);
    }

    public boolean getBluetoothHeadset() {
        return this.markBluetoothheadsetstub;
    }

    public void setStreamMusicOrBluetoothScoIndex(Context context, final int index, final int stream, final int device) {
        new Thread(new Runnable() { // from class: com.android.server.audio.AudioServiceStubImpl.2
            @Override // java.lang.Runnable
            public void run() {
                int i;
                try {
                    Log.d(AudioServiceStubImpl.TAG, "setStreamMusicOrBluetoothScoIndex : stream=" + stream + " index=" + index + " device=" + Integer.toHexString(device));
                    if (stream == 3 && (AudioSystem.DEVICE_OUT_ALL_A2DP_SET.contains(Integer.valueOf(device)) || (i = device) == 2 || i == 67108864 || i == 4 || i == 8)) {
                        String param = "audio.volume.stream.music.device." + AudioServiceStubImpl.this.getDeviceToStringForStreamMusic(device) + "=" + Integer.toString(index / 10);
                        AudioSystem.setParameters(param);
                    } else if (stream != 6 || !AudioSystem.DEVICE_OUT_ALL_SCO_SET.contains(Integer.valueOf(device))) {
                        Log.e(AudioServiceStubImpl.TAG, "other device");
                    } else {
                        String param2 = "audio.volume.stream.bluetoothsco=" + Integer.toString(index);
                        AudioSystem.setParameters(param2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(AudioServiceStubImpl.TAG, "erroe for setStreamMusicOrBluetoothScoIndex");
                }
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDeviceToStringForStreamMusic(int device) {
        Log.d(TAG, "getDeviceToString : device=" + device);
        switch (device) {
            case 2:
                return "speaker";
            case 4:
                return "wired.headset";
            case 8:
                return "wired.headphone";
            case 128:
                return "bluetooth";
            case BroadcastQueueModernStubImpl.FLAG_IMMUTABLE /* 67108864 */:
                return "usb.headset";
            default:
                Log.d(TAG, "getDeviceToString : other devices");
                return "other devices";
        }
    }

    private void reportAudioStatus(Context context, List<AudioPlaybackConfiguration> apcList) {
        MQSUtils mqs = new MQSUtils(context);
        mqs.reportAudioVisualDailyUse(apcList);
        if (mqs.needToReport()) {
            mqs.reportAudioButtonStatus();
            mqs.reportVibrateStatus();
            reportAudioHwState(context);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createNotification(Context context) {
        this.mNotificationTimes++;
        Intent it = new Intent();
        it.setClassName("com.miui.misound", "com.miui.misound.HeadsetSettingsActivity");
        PendingIntent pit = PendingIntent.getActivity(context, 0, it, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        String channel = SystemNotificationChannels.USB;
        Notification.Builder builder = new Notification.Builder(context, channel).setSmallIcon(R.drawable.textfield_disabled).setWhen(0L).setOngoing(true).setDefaults(0).setColor(context.getColor(R.color.system_notification_accent_color)).setCategory("sys").setVisibility(1).setContentIntent(pit).setContentTitle(context.getString(286196265)).setContentText(context.getString(286196263));
        Notification notify = builder.build();
        this.mNm.notify(NOTE_USB_HEADSET_PLUG, notify);
    }

    private void adjustHiFiVolume(int direction, Context context) {
        int currentHiFiVolume = AudioManagerHelper.getHiFiVolume(context);
        if (direction == -1) {
            AudioManagerHelper.setHiFiVolume(context, currentHiFiVolume - 10);
        } else if (direction == 1 && currentHiFiVolume < 100) {
            AudioManagerHelper.setHiFiVolume(context, currentHiFiVolume + 10);
        }
    }

    private boolean shouldAdjustHiFiVolume(int streamType, int direction, int streamIndex, int maxIndex, Context context) {
        if (HIFI_NOT_SUPPORT_DEVICE_LIST.contains(Build.DEVICE) || streamType != 3 || !AudioManagerHelper.isHiFiMode(context)) {
            return false;
        }
        int currentHiFiVolume = AudioManagerHelper.getHiFiVolume(context);
        boolean adjustDownHiFiVolume = direction == -1 && currentHiFiVolume > 0;
        boolean adjustUpHiFiVolume = direction == 1 && streamIndex == maxIndex;
        return adjustDownHiFiVolume || adjustUpHiFiVolume;
    }

    private boolean needEnableVoiceVolumeBoost(int dir, boolean isMax, int device, int alias, int mode) {
        Log.d(TAG, "needEnableVoiceVolumeBoost" + mode + " ismax=" + isMax + " device=" + device + " alias=" + alias + " dir=" + dir);
        if (mode != 2 || alias != 0 || device != 1 || !"manual".equals(SystemProperties.get("ro.vendor.audio.voice.volume.boost"))) {
            return false;
        }
        if (dir == 1 && isMax) {
            return true;
        }
        return dir == -1 && isMax;
    }

    private void setVolumeBoost(boolean boostEnabled, Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        String params = "voice_volume_boost=" + (boostEnabled ? "true" : "false");
        Log.d(TAG, "params:" + params);
        am.setParameters(params);
        this.mVolumeBoostEnabled = boostEnabled;
        sendVolumeBoostBroadcast(boostEnabled, context);
    }

    private void sendVolumeBoostBroadcast(boolean boostEnabled, Context context) {
        long ident = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent(ACTION_VOLUME_BOOST);
            intent.putExtra(EXTRA_BOOST_STATE, boostEnabled);
            context.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setSuperVoiceVolume(boolean boostEnabled, Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        String params = "SUPER_VOICE_VOLUME=" + (boostEnabled ? "on" : "off");
        Log.d(TAG, "params:" + params);
        am.setParameters(params);
        this.mSuperVoiceVolumeOn = boostEnabled;
    }

    public boolean isCtsVerifier(String callingPackage) {
        if (callingPackage != null && (callingPackage.startsWith("com.android.cts") || "android.media.cts".equals(callingPackage))) {
            return true;
        }
        if (callingPackage != null && callingPackage.startsWith("com.google.android.gts")) {
            return true;
        }
        if (callingPackage != null && callingPackage.startsWith("android.media.audio.cts")) {
            return true;
        }
        return false;
    }

    public void updateNotificationMode(Context context) {
        AudioServiceInjector.updateNotificationMode(context);
    }

    private boolean isMuteMusicFromMIUI(ContentResolver cr) {
        int muteMusic = Settings.System.getIntForUser(cr, "mute_music_at_silent", 0, -3);
        return muteMusic == 1;
    }

    public void adjustDefaultStreamVolumeForMiui(int[] defaultStreamVolume) {
        if (isApplyMiuiCustom()) {
            AudioServiceInjector.adjustDefaultStreamVolume(defaultStreamVolume);
        }
    }

    private boolean isApplyMiuiCustom() {
        return !SystemProperties.getBoolean("ro.vendor.audio.skip_miui_volume_custom", false);
    }

    public boolean isAudioPlaybackTriggerSupported() {
        return mIsAudioPlaybackTriggerSupported;
    }

    public void onShowHearingProtectionNotification(Context cx, int msgId) {
        if (msgId == 2) {
            return;
        }
        startHearingProtectionService(cx, msgId);
    }

    private void startHearingProtectionService(Context cx, int msgId) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.miui.misound.hearingprotection.notification");
            intent.setComponent(new ComponentName("com.miui.misound", "com.miui.misound.hearingprotection.HearingProtectionService"));
            intent.putExtra("notificationId", msgId);
            cx.startForegroundService(intent);
        } catch (Exception e) {
            Log.e(TAG, "fail to start HearingProtectionService");
        }
    }

    private ComponentName transportDataToService(Context cx, long beginTime, long endTime, boolean isHigh) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.miui.misound.write.data");
            intent.setComponent(new ComponentName("com.miui.misound", "com.miui.misound.hearingprotection.HearingProtectionService"));
            intent.putExtra("beginMillis", beginTime);
            intent.putExtra("endMillis", endTime);
            intent.putExtra("isHighPitch", isHigh);
            return cx.startForegroundService(intent);
        } catch (Exception e) {
            Log.e(TAG, "fail to transport data to service");
            return null;
        }
    }

    public void readPlaybackMsSettings(Context cx) {
        this.mStartMs = Settings.Global.getLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_START_MS, 0L);
        this.mContinuousMs = Settings.Global.getLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_CONTINUOUS_MS, 0L);
        this.mHighLevel = Settings.Global.getInt(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_HIGH_VOICE, 0);
        this.mCumulativePlaybackStartTime = Settings.Global.getLong(this.mContext.getContentResolver(), KEY_PERSIST_CUMULATIVE_PLAYBACK_MS, 0L);
        String dateTime = Settings.Global.getString(cx.getContentResolver(), KEY_PERSIST_NOTIFICATION_DATE);
        if (dateTime != null) {
            this.mNotificationDate = LocalDate.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        Log.i(TAG, "readPlaybackMsSettings: startMs: " + this.mStartMs + " continuousMs: " + this.mContinuousMs + " highLevel: " + this.mHighLevel + " mNotificationDate=" + this.mNotificationDate + " mCumulativePlaybackStartTime: " + this.mCumulativePlaybackStartTime);
    }

    public boolean insertPlaybackMsToHealth(Context cx) {
        if (this.mStartMs != 0) {
            long j = this.mContinuousMs;
            if (j != 0) {
                ComponentName componentName = transportDataToService(cx, j, j + j, this.mHighLevel == 1);
                if (componentName == null) {
                    return false;
                }
                persistPlaybackMsToSettings(cx, true, 0, "readPlaybackMsSettings");
                return true;
            }
        }
        return true;
    }

    public void persistPlaybackMsToSettings(Context cx, boolean isNeedClearData, int highLevel, String from) {
        Log.i(TAG, "persistPlaybackMsToSettings: isNeedClearData: " + isNeedClearData + " mPlaybackStartTime: " + this.mPlaybackStartTime + " mMusicPlaybackContinuousMsTotal: " + this.mMusicPlaybackContinuousMsTotal + " highLevel: " + highLevel + " from: " + from);
        Settings.Global.putLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_START_MS, isNeedClearData ? 0L : this.mPlaybackStartTime);
        Settings.Global.putLong(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_CONTINUOUS_MS, isNeedClearData ? 0L : this.mMusicPlaybackContinuousMsTotal);
        Settings.Global.putInt(cx.getContentResolver(), KEY_PERSIST_PLAYBACK_HIGH_VOICE, highLevel);
    }

    private void persistCumulativePlaybackStartMsToSettings() {
        Log.d(TAG, "persistCumulativePlaybackStartMsToSettings: mCumulativePlaybackStartTime: " + this.mCumulativePlaybackStartTime);
        Settings.Global.putLong(this.mContext.getContentResolver(), KEY_PERSIST_CUMULATIVE_PLAYBACK_MS, this.mCumulativePlaybackStartTime);
    }

    private void persistNotificationDateToSettings(Context cx, LocalDate localDate) {
        Log.d(TAG, "persistNotificationDateToSettings: localDate: " + localDate);
        Settings.Global.putString(cx.getContentResolver(), KEY_PERSIST_NOTIFICATION_DATE, localDate.toString());
    }

    public boolean onCheckMusicPlaybackContinuous(Context context, int i, boolean z, Set<Integer> set) {
        long j;
        boolean z2 = (i == 128 || i == 256) && !getBluetoothHeadset();
        if (!set.contains(Integer.valueOf(i))) {
            j = 0;
        } else {
            if (!z2) {
                if (AudioSystem.isStreamActive(3, 0)) {
                    this.mMusicPlaybackContinuousMs += AccessControlImpl.LOCK_TIME_OUT;
                    this.mMusicPlaybackContinuousMsTotal += AccessControlImpl.LOCK_TIME_OUT;
                    Log.d(TAG, "music isActive ,start loop =" + this.mMusicPlaybackContinuousMs + "，MUSIC_ACTIVE_CONTINUOUS_MS_MAX = " + MUSIC_ACTIVE_CONTINUOUS_MS_MAX);
                    if (this.mMusicPlaybackContinuousMs >= SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED) {
                        Log.d(TAG, "music isActive max ,post warning dialog");
                        this.mMusicPlaybackContinuousMs = 0L;
                        persistNotificationDateToSettings(context, LocalDate.now());
                    }
                    if (this.mMusicPlaybackContinuousMsTotal >= AccessControlImpl.LOCK_TIME_OUT) {
                        Log.d(TAG, "need to report hearing data");
                        long j2 = this.mPlaybackStartTime;
                        transportDataToService(context, j2, j2 + this.mMusicPlaybackContinuousMsTotal, z);
                        this.mPlaybackStartTime = System.currentTimeMillis();
                        this.mMusicPlaybackContinuousMsTotal = 0L;
                    }
                    persistPlaybackMsToSettings(context, false, z ? 1 : 0, "music isActive ,start loop");
                    return true;
                }
                if (AudioSystem.isStreamActive(3, MUSIC_ACTIVE_RETRY_POLL_PERIOD_MS)) {
                    this.mMusicPlaybackContinuousMs += AccessControlImpl.LOCK_TIME_OUT;
                    this.mMusicPlaybackContinuousMsTotal += AccessControlImpl.LOCK_TIME_OUT;
                    Log.d(TAG, "isRencentActive true,need retry again");
                    persistPlaybackMsToSettings(context, false, z ? 1 : 0, "isRencentActive true");
                    return true;
                }
                Log.d(TAG, "isRencentActive false,reset time " + this.mMusicPlaybackContinuousMs);
                updatePlaybackTime(false);
                long j3 = this.mPlaybackStartTime;
                transportDataToService(context, j3, j3 + this.mMusicPlaybackContinuousMsTotal, z);
                persistPlaybackMsToSettings(context, true, 0, "isRencentActive false");
                this.mMusicPlaybackContinuousMs = 0L;
                this.mMusicPlaybackContinuousMsTotal = 0L;
                return false;
            }
            j = 0;
        }
        Log.d(TAG, "device is not support,reset time calculation ");
        updatePlaybackTime(false);
        long j4 = this.mPlaybackStartTime;
        transportDataToService(context, j4, j4 + this.mMusicPlaybackContinuousMsTotal, z);
        persistPlaybackMsToSettings(context, true, 0, "device is not support");
        this.mMusicPlaybackContinuousMs = j;
        this.mMusicPlaybackContinuousMsTotal = j;
        return false;
    }

    public void updateCumulativePlaybackTime(boolean start) {
        long systemTime = System.currentTimeMillis();
        Log.d(TAG, " updateCumulativePlaybackTime start=" + start + " systemTime=" + systemTime);
        if (start) {
            this.mCumulativePlaybackStartTime = systemTime;
            persistCumulativePlaybackStartMsToSettings();
        } else {
            this.mCumulativePlaybackEndTime = systemTime;
        }
    }

    public void updatePlaybackTime(boolean start) {
        long systemTime = System.currentTimeMillis();
        if (start) {
            this.mPlaybackStartTime = systemTime;
        } else {
            this.mPlaybackEndTime = systemTime;
        }
        Log.d(TAG, " updatePlaybackTime start=" + start + " systemTime=" + systemTime + " throd=" + Math.abs(this.mPlaybackEndTime - this.mPlaybackStartTime));
    }

    public boolean onTrigger(List<AudioPlaybackConfiguration> configs) {
        if (configs == null) {
            return false;
        }
        for (AudioPlaybackConfiguration config : configs) {
            if (isMusicPlayerActive(config)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMusicPlayerActive(AudioPlaybackConfiguration apc) {
        if (apc == null) {
            return false;
        }
        return (apc.getAudioAttributes().getUsage() == 1 || apc.getAudioAttributes().getVolumeControlStream() == 3) && apc.getPlayerState() == 2;
    }

    public void customMinStreamVolume(int[] minStreamVolume) {
        AudioServiceInjector.customMinStreamVolume(minStreamVolume);
    }

    public int[] getAudioPolicyMatchUids(HashMap<IBinder, AudioService.AudioPolicyProxy> policys) {
        List<Integer> uidList = new ArrayList<>();
        for (AudioService.AudioPolicyProxy policy : policys.values()) {
            if (policy.mProjection != null) {
                Iterator it = policy.getMixes().iterator();
                while (it.hasNext()) {
                    AudioMix mix = (AudioMix) it.next();
                    int[] matchUidArray = getIntPredicates(4, mix, new ToIntFunction() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda0
                        @Override // java.util.function.ToIntFunction
                        public final int applyAsInt(Object obj) {
                            int intProp;
                            intProp = ((AudioMixingRule.AudioMixMatchCriterion) obj).getIntProp();
                            return intProp;
                        }
                    });
                    List<Integer> listCollect = (List) Arrays.stream(matchUidArray).boxed().collect(Collectors.toList());
                    uidList.addAll(listCollect);
                }
            }
        }
        int[] arrays = uidList.stream().mapToInt(new AudioServiceStubImpl$$ExternalSyntheticLambda1()).toArray();
        return arrays;
    }

    private int[] getIntPredicates(final int rule, AudioMix mix, ToIntFunction<AudioMixingRule.AudioMixMatchCriterion> getPredicate) {
        return mix.getRule().getCriteria().stream().filter(new Predicate() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AudioServiceStubImpl.lambda$getIntPredicates$1(rule, (AudioMixingRule.AudioMixMatchCriterion) obj);
            }
        }).mapToInt(getPredicate).toArray();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getIntPredicates$1(int rule, AudioMixingRule.AudioMixMatchCriterion criterion) {
        return criterion.getRule() == rule;
    }

    public void startCameraRecordService(Context context, AudioRecordingConfiguration audioConfig, int eventType, int riidNow) {
        if (this.mIsSupportedCameraRecord && audioConfig != null) {
            if (riidNow == this.cameraToastServiceRiid) {
                Log.d(TAG, "the riid is exist, do not startCameraRecordService again  " + riidNow);
                return;
            }
            this.cameraToastServiceRiid = riidNow;
            int cameraAudioHeadsetState = Settings.Global.getInt(context.getContentResolver(), CAMERA_AUDIO_HEADSET_STATE, -1);
            if (audioConfig.getClientPackageName().equals("com.android.camera") && cameraAudioHeadsetState == 1 && audioConfig.getClientAudioSource() == 5) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.miui.audiomonitor", "com.miui.audiomonitor.MiuiCameraBTRecordService"));
                    intent.putExtra("packageName", "com.android.camera");
                    if (audioConfig.getAudioDevice() != null) {
                        int deviceType = audioConfig.getAudioDevice().getType();
                        intent.putExtra("deviceType", deviceType);
                        Log.d(TAG, String.format("packageName %s deviceType %d eventType %d", "com.android.camera", Integer.valueOf(deviceType), Integer.valueOf(eventType)));
                    }
                    intent.putExtra("eventType", eventType);
                    context.startForegroundService(intent);
                } catch (Exception e) {
                    Log.e(TAG, "fail to startCameraRecordService ");
                }
            }
        }
    }

    public void stopCameraRecordService(Context context, AudioRecordingConfiguration audioConfig, int riid) {
        if (this.mIsSupportedCameraRecord) {
            Log.d(TAG, "stopCameraRecordService riidNow " + riid);
            if (audioConfig != null && audioConfig.getClientPackageName().equals("com.android.camera")) {
                this.cameraToastServiceRiid = -1;
            }
        }
    }

    public void handleSpeakerChanged(Context context, int pid, boolean speakerOn) {
        Log.d(TAG, "handleSpeakerChanged audiomode " + this.mAudioMode);
        int i = this.mAudioMode;
        if (i == 2 || i == 3) {
            AudioServiceInjector.handleSpeakerChanged(pid, speakerOn, Binder.getCallingUid());
        }
    }

    public void updateAudioParameterClients(IBinder binder, String targetParameter) {
        handleParameters(targetParameter);
        if (Arrays.asList(TRANSMIT_AUDIO_PARAMETERS).contains(targetParameter)) {
            addAudioParameterClient(binder, targetParameter);
        } else if (Arrays.asList(DEFAULT_AUDIO_PARAMETERS).contains(targetParameter)) {
            removeAudioParameterClient(binder, targetParameter, true);
        }
    }

    private AudioParameterClient getAudioParameterClient(IBinder binder, String targetParameter) {
        synchronized (this.mAudioParameterClientList) {
            Iterator<AudioParameterClient> iterator = this.mAudioParameterClientList.listIterator();
            while (iterator.hasNext()) {
                AudioParameterClient client = iterator.next();
                if (client.getBinder() == binder && client.getTargetParameter().equals(targetParameter)) {
                    return client;
                }
            }
            return null;
        }
    }

    private AudioParameterClient removeAudioParameterClient(IBinder binder, String targetParameter, boolean unregister) {
        AudioParameterClient client = getAudioParameterClient(binder, targetParameter);
        synchronized (this.mAudioParameterClientList) {
            if (client == null) {
                return null;
            }
            this.mAudioParameterClientList.remove(client);
            if (unregister) {
                client.unregisterDeathRecipient();
            }
            return client;
        }
    }

    private AudioParameterClient addAudioParameterClient(IBinder binder, String targetParameter) {
        AudioParameterClient client;
        synchronized (this.mAudioParameterClientList) {
            client = removeAudioParameterClient(binder, targetParameter, false);
            if (this.mParameterClientDeathListener == null) {
                this.mParameterClientDeathListener = new AudioParameterClient.ClientDeathListener() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda7
                    @Override // com.android.server.audio.AudioParameterClient.ClientDeathListener
                    public final void onBinderDied(IBinder iBinder, String str) {
                        AudioServiceStubImpl.this.lambda$addAudioParameterClient$2(iBinder, str);
                    }
                };
            }
            if (client == null) {
                client = new AudioParameterClient(binder, targetParameter);
                client.setClientDiedListener(this.mParameterClientDeathListener);
                client.registerDeathRecipient();
            }
            this.mAudioParameterClientList.add(0, client);
        }
        return client;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$addAudioParameterClient$2(IBinder diedBinder, String diedTargetParameter) {
        int i = 0;
        while (true) {
            String[] strArr = TRANSMIT_AUDIO_PARAMETERS;
            if (i < strArr.length) {
                if (strArr[i].equals(diedTargetParameter)) {
                    removeAudioParameterClient(diedBinder, diedTargetParameter, true);
                    if (this.mAudioParameterClientList.size() > 0) {
                        AudioSystem.setParameters(this.mAudioParameterClientList.get(0).getTargetParameter());
                    } else {
                        AudioSystem.setParameters(DEFAULT_AUDIO_PARAMETERS[i]);
                    }
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void notifyBtStateToDolbyEffectController(Context context, AudioDeviceBroker.BtDeviceInfo btInfo) {
        if (this.mIsSupportedDolbyEffectControl) {
            Bundle bundle = new Bundle();
            bundle.putString(CloudControlPreinstallService.ConnectEntity.DEVICE, btInfo.mDevice.getAddress());
            bundle.putString("profile", BluetoothProfile.getProfileName(btInfo.mProfile));
            bundle.putString("state", BluetoothProfile.getConnectionStateName(btInfo.mState));
            DolbyEffectController.getInstance(context).btStateChangedFromDeviceBroker(bundle);
        }
    }

    public AudioService getMiAudioService(Context context) {
        return new MiAudioService(context);
    }

    public void onSystemReadyMiAudioServiceMTK() {
        this.mMiAudioServiceMTK.onSystemReady();
    }

    public void onRotationUpdateMiAudioServiceMTK(Integer rotation) {
        this.mMiAudioServiceMTK.onRotationUpdate(rotation);
    }

    public void adjustStreamVolumeMiAudioServiceMTK(int streamType, int direction, int flags, String callingPackage, String caller, int uid, int pid, String attributionTag, boolean hasModifyAudioSettings, int keyEventMode) {
        this.mMiAudioServiceMTK.adjustStreamVolume(streamType, direction, flags, callingPackage, caller, uid, pid, attributionTag, hasModifyAudioSettings, keyEventMode);
    }

    public void startAudioGameEffect(Context context) {
        this.mAudioGameEffect = new AudioGameEffect(context);
    }

    public void startGameAudioEnhancer() {
        if (SystemProperties.getBoolean("ro.vendor.audio.game.mode", false) || SystemProperties.getBoolean("ro.vendor.audio.game.effect", false)) {
            this.mGameAudioEnhancer = new GameAudioEnhancer(this.mContext, this.mWorkerThread.getLooper());
        }
    }

    public void startAudioPowerSaveModeObserver(Context context) {
        if ((this.mSettingAudioPowerSave & 1) != 0) {
            this.mAudioPowerSaveModeObserver = new AudioPowerSaveModeObserver(this.mContext);
        }
    }

    public void reSetAudioParam() {
        AudioPowerSaveModeObserver audioPowerSaveModeObserver;
        if ((this.mSettingAudioPowerSave & 1) != 0 && (audioPowerSaveModeObserver = this.mAudioPowerSaveModeObserver) != null) {
            audioPowerSaveModeObserver.reSetAudioPowerParam();
        }
    }

    public void startAudioThermalObserver(Context context) {
        if (this.mIsSupportCinemaMode) {
            this.mAudioThermalObserver = new AudioThermalObserver(this.mContext);
        }
    }

    public void reSetAudioCinemaModeThermal() {
        AudioThermalObserver audioThermalObserver;
        if (this.mIsSupportCinemaMode && (audioThermalObserver = this.mAudioThermalObserver) != null) {
            audioThermalObserver.reSetAudioCinemaModeThermal();
        }
    }

    private void handleParameters(String keyValuePairs) {
        if (PadAdapter.ENABLE && keyValuePairs != null && keyValuePairs.contains("remote_record_mode")) {
            this.mHandler.obtainMessage(8, keyValuePairs).sendToTarget();
            return;
        }
        String[] kvpairs = keyValuePairs.split(StorageRestrictedPathManager.SPLIT_MULTI_PATH);
        for (String pair : kvpairs) {
            String[] kv = pair.split("=");
            if ("audio_sys_with_mic".equals(kv[0])) {
                boolean enable = "1".equals(kv[1]);
                if (!enable) {
                    this.mAudioService.setStreamVolumeInt(3, this.mMusicVolumeIndex, 2);
                } else if (this.mMusicVolumeDb > (-this.mVolumeAttenuation)) {
                    int musicMaxIndex = this.mAudioService.getStreamMaxVolume(3);
                    this.mAudioService.setStreamVolumeInt(3, musicMaxIndex, 2);
                }
            } else if ("audio_playback_capture_for_screen".equals(kv[0]) && "true".equals(kv[1])) {
                int deviceStreamVolume = this.mAudioService.getDeviceStreamVolume(3, 2);
                this.mMusicVolumeIndex = deviceStreamVolume;
                this.mMusicVolumeDb = AudioSystem.getStreamVolumeDB(3, deviceStreamVolume, 2);
            }
        }
    }

    public void initAudioImplStatus(AudioService service, AudioDeviceBroker deviceBroker, BtHelper btHelper) {
        this.mAudioService = service;
        this.mDeviceBroker = deviceBroker;
        this.mBtHelper = btHelper;
    }

    public void init(Context context, AudioService service) {
        Log.d(TAG, "initContext ...");
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mAudioService = service;
        this.mMqsUtils = new MQSUtils(context);
        this.mMiAudioServiceMTK = new MiAudioServiceMTK(context, service);
        this.mIsSupportedReportAudioRouteState = this.mMiuiXlog.checkXlogPermission(ABNORMAL_AUDIO_INFO_EVENT_NAME, 0);
        int i = MQSSERVER_REPORT_RATE_MS;
        if (i > 0) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(6), i);
        }
        if (PadAdapter.ENABLE) {
            this.mPadAdapter = new PadAdapter(context);
        }
        if (FoldableAdapter.ENABLE) {
            this.mFoldableAdapter = new FoldableAdapter(context, this.mWorkerThread.getLooper());
        }
        if (FeatureAdapter.ENABLE) {
            this.mFeatureAdapter = new FeatureAdapter(context);
        }
        if (this.mIsSupportFWAudioEffectCenter) {
            initEffectChangeBroadcastReceiver();
        }
    }

    public void onAudioServerDied() {
        Log.d(TAG, "onAudioServerDied ...");
        if (FoldableAdapter.ENABLE) {
            this.mFoldableAdapter.onAudioServerDied();
        }
    }

    public void updateConcurrentVoipInfo(List<AudioService.SetModeDeathHandler> setModeDeathHandlers) {
        if (setModeDeathHandlers != null && setModeDeathHandlers.size() > 1) {
            List<String> clients = new ArrayList<>();
            for (AudioService.SetModeDeathHandler handler : setModeDeathHandlers) {
                clients.add(handler.getPackage());
            }
            this.mHandler.obtainMessage(1, clients).sendToTarget();
        }
    }

    public void initEffectChangeBroadcastReceiver() {
        this.mEffectChangeBroadcastReceiver = new EffectChangeBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("miui.intent.action.ACTION_AUDIO_EFFECT_CHANGED");
        this.mContext.registerReceiver(this.mEffectChangeBroadcastReceiver, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendEffectRefresh() {
        Log.d(TAG, "sendEffectRefresh");
        try {
            AudioEffectCenter audioEffectCenter = AudioEffectCenter.getInstance(this.mContext);
            Intent intent = new Intent("miui.intent.action.ACTION_AUDIO_EFFECT_REFRESH");
            Bundle bundle = new Bundle();
            bundle.putBoolean("dolby_available", audioEffectCenter.isEffectAvailable("dolby"));
            bundle.putBoolean("dolby_active", audioEffectCenter.isEffectActive("dolby"));
            bundle.putBoolean("misound_available", audioEffectCenter.isEffectAvailable("misound"));
            bundle.putBoolean("misound_active", audioEffectCenter.isEffectActive("misound"));
            bundle.putBoolean("none_available", audioEffectCenter.isEffectAvailable("none"));
            bundle.putBoolean("surround_available", audioEffectCenter.isEffectAvailable("surround"));
            bundle.putBoolean("surround_active", audioEffectCenter.isEffectActive("surround"));
            bundle.putBoolean("spatial_available", audioEffectCenter.isEffectAvailable("spatial"));
            bundle.putBoolean("spatial_active", audioEffectCenter.isEffectActive("spatial"));
            intent.putExtra("bundle", bundle);
            this.mContext.sendBroadcast(intent);
        } catch (RuntimeException e) {
            Log.d(TAG, "sendEffectRefresh error");
            e.printStackTrace();
        }
    }

    public void handleWaveformInfoTracked(CombinedVibration effect, String opPkg, VibrationAttributes attrs) {
        if (this.mMiuiXlog.checkXlogPermission(WAVEFORM_EVENT_NAME, 0) && (effect instanceof CombinedVibration.Mono)) {
            VibrationEffect.Composed composed = ((CombinedVibration.Mono) effect).getEffect();
            ArrayList<VibrationEffectSegment> segments = new ArrayList<>(composed.getSegments());
            VibrationEffectSegment segment = (VibrationEffectSegment) composed.getSegments().get(0);
            int segmentCount = segments.size();
            if (segment instanceof StepSegment) {
                MQSUtils.WaveformInfo vibratorInfo = new MQSUtils.WaveformInfo();
                vibratorInfo.opPkg = opPkg;
                vibratorInfo.attrs = attrs.toString();
                Log.d(TAG, "WaveformInfo segmentCount: " + segmentCount);
                if (segmentCount <= 4) {
                    ArrayList<String> segmentsToString = (ArrayList) segments.stream().map(new Function() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda2
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String obj2;
                            obj2 = ((VibrationEffectSegment) obj).toString();
                            return obj2;
                        }
                    }).collect(Collectors.toCollection(new AudioServiceStubImpl$$ExternalSyntheticLambda3()));
                    String result = String.join(", ", segmentsToString);
                    vibratorInfo.effect = result;
                } else {
                    ArrayList<VibrationEffectSegment> newSegments = new ArrayList<>(segments.subList(0, 4));
                    ArrayList<String> newSegmentsToString = (ArrayList) newSegments.stream().map(new Function() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda4
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String obj2;
                            obj2 = ((VibrationEffectSegment) obj).toString();
                            return obj2;
                        }
                    }).collect(Collectors.toCollection(new AudioServiceStubImpl$$ExternalSyntheticLambda3()));
                    String newResult = String.join(", ", newSegmentsToString);
                    vibratorInfo.effect = newResult;
                }
                this.mHandler.obtainMessage(3, vibratorInfo).sendToTarget();
            }
        }
    }

    public void reportAudioSilentObserverToOnetrack(int level, String location, String silent_reason, int silent_type) {
        if (this.mMiuiXlog.checkXlogPermission(AUDIO_ONETRACK_SILENT_OBSERVER, level)) {
            Bundle info = new Bundle();
            info.putInt(AUDIO_SILENT_LEVEL, level);
            info.putString(AUDIO_SILENT_LOCATION, location);
            info.putString(AUDIO_SILENT_REASON, silent_reason);
            info.putInt(AUDIO_SILENT_TYPE, silent_type);
            Message msg = Message.obtain();
            msg.what = 4;
            msg.setData(info);
            this.mHandler.sendMessage(msg);
        }
    }

    public void reportNotificationEventToOnetrack(String isTurnOn) {
        Log.d(TAG, "reportNotificationEventToOnetrack, isTurnOn: " + isTurnOn);
        LocalDateTime date_time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String current_time = date_time.format(formatter);
        String result = String.format("{\"name\":\"audio_notification_alias\",\"audio_event\":{\"notification_isalias_ring\":\"%s\" , \"current_time\":\"%s\"}, \"dgt\":\"null\",\"audio_ext\":\"null\" }", isTurnOn, current_time);
        Log.d(TAG, "result: " + result);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Exception e) {
            Log.d(TAG, "reportNotificationEventToOnetrack exception : " + e);
        }
    }

    public void setPutGlobalInt(Object settingslock, SettingsAdapter settings, ContentResolver contentresolver, boolean enabled) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (settingslock) {
                settings.putGlobalInt(contentresolver, "spatial_audio_feature_enable", enabled ? 1 : 0);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void handleMicrophoneMuteChanged(boolean muted) {
        if (PadAdapter.ENABLE) {
            this.mPadAdapter.handleMicrophoneMuteChanged(muted);
        }
    }

    public void handleRecordEventUpdate(int audioSource) {
        if (PadAdapter.ENABLE) {
            this.mPadAdapter.handleRecordEventUpdate(audioSource);
        }
    }

    public void spatializerSetFeature(SettingsAdapter settings, SpatializerHelper spatializerhelper, ContentResolver contentresolver, int num) {
        boolean featureEnabled = settings.getGlobalInt(contentresolver, "spatial_audio_feature_enable", num) == 1;
        spatializerhelper.setFeatureEnabled(featureEnabled);
    }

    public boolean isStreamVolumeInt(int mode, boolean isLeConnectedForDeviceBroker) {
        return (mode == 2 || mode == 3) && isLeConnectedForDeviceBroker;
    }

    public int getModeDirectly(AudioService.SetModeDeathHandler currentModeHandler) {
        Log.v(TAG, "When ble is connected, getMode directly to avoid long waiting times.");
        if (currentModeHandler != null) {
            return currentModeHandler.getMode();
        }
        return 0;
    }

    public void startDolbyEffectController(Context context) {
        if (this.mIsSupportedDolbyEffectControl) {
            Log.d(TAG, "startDolbyEffectControl");
            DolbyEffectController.getInstance(context).init();
        }
    }

    public int enableSuperIndex(int mStreamType, int mIndexSuper, int mIndexMax) {
        int superIndexAdd = SystemProperties.getInt("ro.vendor.audio.volume_super_index_add", -1);
        if (superIndexAdd == -1) {
            return mIndexSuper;
        }
        if (mStreamType == 3 || mStreamType == 2 || mStreamType == 5) {
            int mIndexSuper2 = mIndexMax + superIndexAdd;
            Log.d(TAG, "SuperVolume: mIndexSuper is " + mIndexSuper2 + " mStreamType is " + mStreamType);
            return mIndexSuper2;
        }
        return mIndexSuper;
    }

    public int setSuperIndex(int index, int device, int mStreamType, int mIndexMax) {
        int maxIndex = mIndexMax / 10;
        if ((mStreamType == 3 || mStreamType == 2 || mStreamType == 5) && device == 2) {
            Log.d(TAG, "SuperVolume: setSuperIndex index=" + index + " device=" + device + " mStreamType=" + mStreamType);
            String params = new String("");
            if (index > maxIndex) {
                String params2 = (params + "SpkVolIdx=" + index) + ";SuperStream=" + mStreamType;
                this.mSuperVolumeOn = true;
                AudioSystem.setParameters(params2);
                Log.d(TAG, "SuperVolume: mSuperVolumeOn = true   setParameters: " + params2);
            } else if (this.mSuperVolumeOn) {
                String params3 = (params + "SpkVolIdx=" + index) + ";SuperStream=" + mStreamType;
                this.mSuperVolumeOn = false;
                AudioSystem.setParameters(params3);
                Log.d(TAG, "SuperVolume: mSuperVolumeOn = false  setParameters: " + params3);
            }
        }
        return index > maxIndex ? maxIndex : index;
    }

    public int getSuperIndex(int mStreamType, int mIndexSuper, int mIndexMax, Set<Integer> deviceSet, Context context) {
        String pkg = context.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (mIndexSuper != -1 && deviceSet.size() == 1 && deviceSet.contains(2) && pkg != null && !isCtsVerifier(pkg)) {
            Log.d(TAG, "SuperVolume: getMxIndex: " + mIndexSuper + ";mStreamType is " + mStreamType + ";pkg is " + pkg);
            return mIndexSuper;
        }
        return mIndexMax;
    }

    public void notifyVolumeChangedToDolbyEffectController(Context context, int stream, int newVolumeIndex) {
        if (this.mIsSupportedDolbyEffectControl && stream == 3) {
            DolbyEffectController.getInstance(context).receiveVolumeChanged(newVolumeIndex);
        }
    }

    public void createKeyErrorNotification(Context context, int notificationID) {
        this.mNm = (NotificationManager) context.getSystemService("notification");
        Intent keyErrorIntent = new Intent();
        keyErrorIntent.setAction("android.server.Volume_Key_Error");
        PendingIntent keyErrorPit = PendingIntent.getBroadcast(context, 0, keyErrorIntent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        Intent intent = new Intent();
        intent.setAction("android.server.Volume_Key_NoBlock");
        PendingIntent pit = PendingIntent.getBroadcast(context, 0, intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        String channel = SystemNotificationChannels.USB_HEADSET;
        Notification.Builder builder = new Notification.Builder(context, channel).setSmallIcon(R.drawable.textfield_disabled).setWhen(0L).setOngoing(false).setAutoCancel(true).setDefaults(0).setColor(context.getColor(R.color.system_notification_accent_color)).setCategory("recommendation").setVisibility(1).setContentTitle(context.getString(286196785)).setContentText(context.getString(286196784)).addAction(R.drawable.textfield_disabled, context.getString(286196391), keyErrorPit).addAction(R.drawable.textfield_disabled, context.getString(286196390), pit);
        Notification notify = builder.build();
        this.mNm.notify(notificationID, notify);
    }

    public boolean delayNotifyBtStopBluetoothScoIfNeed(int pid, String eventSource) {
        boolean isScoRequestExisting;
        if (eventSource != null && eventSource.startsWith("setSpeakerphoneOn")) {
            resetNotifyBtStopScoStatus(eventSource);
            return false;
        }
        int modeOwnerPidFromAudioService = this.mAudioService.getModeOwnerPid();
        if (modeOwnerPidFromAudioService == 0 || this.mModeOwnerPid == 0) {
            Log.d(TAG, "modeOwnerPidFromAudioService：" + modeOwnerPidFromAudioService + ", mModeOwnerPid： " + this.mModeOwnerPid);
            resetNotifyBtStopScoStatus(eventSource);
            return false;
        }
        Log.d(TAG, "modeOwnerPidFromAudioService：" + modeOwnerPidFromAudioService + ", mModeOwnerPid： " + this.mModeOwnerPid);
        if ((modeOwnerPidFromAudioService == pid || pid != this.mModeOwnerPid || !this.mDeviceBroker.isBluetoothScoRequestForPid(modeOwnerPidFromAudioService)) && (modeOwnerPidFromAudioService != pid || this.mModeOwnerPid != pid || !this.mDeviceBroker.isBluetoothScoRequestForPid(this.mAudioService.getNextModeOwnerPid(pid)))) {
            isScoRequestExisting = false;
        } else {
            isScoRequestExisting = true;
        }
        if (isScoRequestExisting) {
            this.mInDelayNotifyBtStopSco = true;
            return true;
        }
        resetNotifyBtStopScoStatus(eventSource);
        return false;
    }

    public void delayNotifyBtStopBluetoothSco(Handler brokerHandler, String eventSource) {
        if (brokerHandler != null) {
            brokerHandler.sendMessageDelayed(brokerHandler.obtainMessage(115, eventSource), 200L);
        }
    }

    public void updateModeOwnerPid(int pid, String eventSource) {
        Log.d(TAG, "updateModeOwnerPid, pid: " + pid + ", eventSource: " + eventSource);
        this.mModeOwnerPid = pid;
        clearSetScoCommunicationDevice(pid);
        notifyBtStopBluetoothSco(eventSource);
    }

    public void onNotifyBtStopBluetoothSco(Handler brokerHandler, String eventSource) {
        if (!this.mInDelayNotifyBtStopSco) {
            Log.d(TAG, "onNotifyBtStopBluetoothSco: ignore current request");
        }
        this.mDelayNotifyBtStopScoCount++;
        int nextPidFromAudioService = this.mAudioService.getNextModeOwnerPid(this.mModeOwnerPid);
        boolean isContinueDelayNotify = nextPidFromAudioService != 0 && this.mDeviceBroker.isBluetoothScoRequestForPid(nextPidFromAudioService) && nextPidFromAudioService != this.mModeOwnerPid && this.mDelayNotifyBtStopScoCount <= 3;
        if (isContinueDelayNotify) {
            delayNotifyBtStopBluetoothSco(brokerHandler, eventSource);
            Log.d(TAG, "onNotifyBtStopBluetoothSco: continue delay notify bt stopBluetoothSco");
        } else {
            notifyBtStopBluetoothSco(eventSource);
        }
    }

    private void notifyBtStopBluetoothSco(String eventSource) {
        if (!this.mInDelayNotifyBtStopSco) {
            return;
        }
        resetNotifyBtStopScoStatus(eventSource);
        if (!this.mDeviceBroker.isBluetoothScoRequested()) {
            this.mBtHelper.stopBluetoothSco(eventSource);
            reportMultiVoipDailyUseForBtConnect(STATE_AUDIO_SCO_DISCONNECTED);
        } else {
            Log.d(TAG, "don't need notify bt stopBluetoothSco modeOwnerPid: " + this.mModeOwnerPid + ", eventSource: " + eventSource);
            reportMultiVoipDailyUseForBtConnect(STATE_AUDIO_SCO_CONNECTED);
        }
    }

    private void resetNotifyBtStopScoStatus(String eventSource) {
        if (this.mInDelayNotifyBtStopSco) {
            this.mDelayNotifyBtStopScoCount = 0;
            this.mInDelayNotifyBtStopSco = false;
            Log.d(TAG, "resetNotifyBtStopScoStatus, eventSource: " + eventSource);
        }
    }

    private void reportMultiVoipDailyUseForBtConnect(String scoState) {
        this.mHandler.obtainMessage(2, scoState).sendToTarget();
    }

    public void setA2dpDeviceClassForOneTrack(String deviceClassName) {
        if (this.mMiuiXlog.checkXlogPermission(HEADPHONES_EVENT_NAME, 0)) {
            AudioSystem.setParameters("a2dp_device_class_name=" + deviceClassName);
        }
    }

    public void handleReceiveBtEventChanged(Intent intent) {
        if (!this.mIsSupportedReportAudioRouteState) {
            return;
        }
        String action = intent.getAction();
        if (action.equals("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
            BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE", BluetoothDevice.class);
            this.mHeadsetDeviceConnectedState = btDevice != null ? 2 : 0;
            if (btDevice != null) {
                this.mBtName = btDevice.getName();
                return;
            } else {
                if (this.mA2dpDeviceConnectedState == 0) {
                    this.mBtName = "";
                    return;
                }
                return;
            }
        }
        if (action.equals("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED")) {
            this.mScoBtState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
        }
    }

    public void updateBluetoothActiveDevice(int state, int profile, String name) {
        if (this.mIsSupportedReportAudioRouteState && profile == 2) {
            this.mA2dpDeviceConnectedState = state;
            if (state == 2) {
                this.mBtName = name;
            } else if (this.mHeadsetDeviceConnectedState == 0) {
                this.mBtName = "";
            }
        }
    }

    public void setPreferredCommunicationDevice(AudioDeviceAttributes device, LinkedList<AudioDeviceBroker.CommunicationRouteClient> routeClients) {
        if (this.mIsSupportedReportAudioRouteState) {
            List<String> scoClients = new ArrayList<>();
            Iterator<AudioDeviceBroker.CommunicationRouteClient> it = routeClients.iterator();
            while (it.hasNext()) {
                AudioDeviceBroker.CommunicationRouteClient client = it.next();
                if (client.getDevice() != null) {
                    String clientInfo = "pid=" + client.getPid() + " deviceType=" + client.getDevice().getType() + " cb=" + client.getBinder();
                    scoClients.add(clientInfo);
                }
            }
            Log.d(TAG, "setPreferredCommunicationDevice： clients=" + Arrays.toString(scoClients.toArray()));
            this.mPreferredCommunicationDevice = device;
            this.mCommunicationRouteClients = scoClients;
        }
    }

    public void onCheckAudioRoute(String eventSource) {
        if (!this.mIsSupportedReportAudioRouteState) {
            return;
        }
        if (TextUtils.isEmpty(eventSource)) {
            Log.d(TAG, "onCheckAudioRoute: invalid eventSource");
            return;
        }
        if (this.mA2dpDeviceConnectedState != 2 && this.mHeadsetDeviceConnectedState != 2) {
            Log.d(TAG, "onCheckAudioRoute: no active bt device");
            return;
        }
        Log.d(TAG, "onCheckAudioRoute: " + eventSource);
        boolean isReport = false;
        int errorType = -1;
        if (this.mA2dpDeviceConnectedState == 0) {
            errorType = 4;
            isReport = true;
        }
        StringBuilder builderSource = new StringBuilder(eventSource);
        if (!isReport && this.mAudioMode == 3 && !checkAudioRouteForBtConnected()) {
            AudioDeviceAttributes audioDeviceAttributes = this.mPreferredCommunicationDevice;
            boolean audioBaseScoState = audioDeviceAttributes != null && audioDeviceAttributes.getType() == 7;
            if (audioBaseScoState) {
                errorType = 7;
            } else if (!isSetScoCommunicationDevice()) {
                errorType = 3;
                builderSource.append(" app don't request sco");
            } else if (!this.mDeviceBroker.isBluetoothScoRequested()) {
                errorType = 3;
                builderSource.append(" app request sco, but stop sco");
            } else {
                errorType = 0;
            }
            isReport = true;
        }
        if (isReport) {
            Log.d(TAG, "[TF-BT] onCheckAudioRoute: errorType=" + errorType);
            reportAbnormalAudioStatus(errorType, builderSource.toString(), "", "maybeError");
        }
    }

    private boolean isSetScoCommunicationDevice() {
        boolean isAppSetScoRequest;
        synchronized (this.mSetScoCommunicationDevice) {
            isAppSetScoRequest = false;
            Iterator<AudioDeviceBroker.CommunicationDeviceInfo> it = this.mSetScoCommunicationDevice.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                AudioDeviceBroker.CommunicationDeviceInfo deviceInfo = it.next();
                if (deviceInfo.mPid == this.mModeOwnerPid && deviceInfo.mDevice != null) {
                    isAppSetScoRequest = true;
                    break;
                }
            }
            Log.d(TAG, "isSetScoCommunicationDevice=" + isAppSetScoRequest);
        }
        return isAppSetScoRequest;
    }

    private boolean checkAudioRouteForBtConnected() {
        int voiceDevice = this.mAudioService.getDeviceForStream(0);
        if (!AudioSystem.DEVICE_OUT_ALL_SCO_SET.contains(Integer.valueOf(voiceDevice))) {
            Log.d(TAG, "checkAudioRouteForBtConnected: route is error voiceDevice=0x" + Integer.toHexString(voiceDevice));
            return false;
        }
        return true;
    }

    public void reportAbnormalAudioStatus(int errorType, String eventSource, String packageName, String audioState) {
        String packageName2;
        Context context;
        int i;
        if (this.mIsSupportedReportAudioRouteState) {
            if (errorType == -1) {
                Log.w(TAG, "reportAudioStatus： audio route is normal");
                return;
            }
            if (audioState == "ok" && !this.mMiuiXlog.checkXlogPermission(ABNORMAL_AUDIO_INFO_EVENT_NAME, 1)) {
                Log.d(TAG, "reportAbnormalAudioStatus: TR6_XLOG_DISABLE");
                return;
            }
            if (TextUtils.isEmpty(eventSource)) {
                Log.w(TAG, "reportAudioStatus： eventSource is empty");
                return;
            }
            if (errorType != 1 && errorType != 2 && this.mA2dpDeviceConnectedState == 0 && this.mHeadsetDeviceConnectedState == 0) {
                Log.d(TAG, "reportAudioStatus： no active bt device，errorType=" + errorType);
                return;
            }
            if (TextUtils.isEmpty(packageName) && ((i = this.mAudioMode) == 2 || i == 3)) {
                packageName2 = this.mRequesterPackageForAudioMode;
            } else if (TextUtils.isEmpty(packageName) && (context = this.mContext) != null) {
                packageName2 = context.getPackageName();
            } else {
                packageName2 = packageName;
            }
            long currentTime = SystemClock.uptimeMillis();
            if (currentTime - this.sLastAudioStateExpTime < 6000 && errorType == this.sLastReportAudioErrorType) {
                this.sLastAudioStateExpTime = currentTime;
                Log.d(TAG, "reportAudioStatus: the upload interval has not exceeded 6000 s, errorType=" + errorType + " eventSource=" + eventSource + " packageName=" + packageName2);
                return;
            }
            this.sLastReportAudioErrorType = errorType;
            this.sLastAudioStateExpTime = currentTime;
            String audioEventSource = eventSource + " modePid=" + this.mModeOwnerPid + " routeClients=" + this.mCommunicationRouteClients + " mScoBtState=" + this.mScoBtState;
            MQSUtils.AudioStateTrackData audioTrackData = new MQSUtils.AudioStateTrackData(errorType, audioEventSource, packageName2, this.mBtName, this.mA2dpDeviceConnectedState, this.mHeadsetDeviceConnectedState, audioState);
            this.mHandler.obtainMessage(5, audioTrackData).sendToTarget();
            return;
        }
        Log.d(TAG, "reportAudioStatus： no report permission");
    }

    public void onSetCommunicationDeviceForClient(AudioDeviceBroker.CommunicationDeviceInfo deviceInfo) {
        if (this.mIsSupportedReportAudioRouteState) {
            synchronized (this.mSetScoCommunicationDevice) {
                if (deviceInfo != null) {
                    if (deviceInfo.mDevice != null) {
                        if (deviceInfo.mDevice != null && deviceInfo.mDevice.getType() == 7) {
                            int scoDeviceNum = this.mSetScoCommunicationDevice.size();
                            if (scoDeviceNum > 10) {
                                this.mSetScoCommunicationDevice.subList(0, scoDeviceNum / 2).clear();
                            }
                            Log.d(TAG, "onSetCommunicationDeviceForClient: add deviceInfo=" + deviceInfo);
                            this.mSetScoCommunicationDevice.add(deviceInfo);
                        }
                    }
                }
            }
        }
    }

    private void clearSetScoCommunicationDevice(final int skipPid) {
        if (this.mIsSupportedReportAudioRouteState) {
            synchronized (this.mSetScoCommunicationDevice) {
                Log.d(TAG, "clearSetScoCommunicationDevice: skipPid=" + skipPid);
                this.mSetScoCommunicationDevice.removeIf(new Predicate() { // from class: com.android.server.audio.AudioServiceStubImpl$$ExternalSyntheticLambda6
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return AudioServiceStubImpl.lambda$clearSetScoCommunicationDevice$5(skipPid, (AudioDeviceBroker.CommunicationDeviceInfo) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$clearSetScoCommunicationDevice$5(int skipPid, AudioDeviceBroker.CommunicationDeviceInfo deviceInfo) {
        return deviceInfo.mPid != skipPid;
    }

    public void handleLowBattery(int batteryPct, int audioControlStatus) {
        if (FeatureAdapter.ENABLE) {
            this.mFeatureAdapter.handleLowBattery(batteryPct, audioControlStatus);
        }
    }

    public void onPlayerTracked(AudioPlaybackConfiguration apc) {
        if (FeatureAdapter.ENABLE) {
            this.mFeatureAdapter.onPlayerTracked(apc);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAudioModeUpdate(int audioMode) {
        if (PadAdapter.ENABLE) {
            this.mPadAdapter.handleAudioModeUpdate(audioMode);
        }
        if (FoldableAdapter.ENABLE) {
            this.mFoldableAdapter.onUpdateAudioMode(audioMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMediaStateUpdate(boolean mediaActive) {
        if (FoldableAdapter.ENABLE) {
            this.mFoldableAdapter.onUpdateMediaState(mediaActive);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleMeetingModeUpdate(String parameter) {
        if (PadAdapter.ENABLE) {
            this.mPadAdapter.handleMeetingModeUpdate(parameter);
        }
    }

    public boolean isAdspSpatializerAvailable() {
        Context context = this.mContext;
        if (context != null) {
            String packageName = context.getPackageManager().getNameForUid(Binder.getCallingUid());
            if ("com.tencent.qqmusic".equals(packageName)) {
                return this.mIsAdspSpatializerAvailable;
            }
            return false;
        }
        return false;
    }

    public boolean isAdspSpatializerEnable() {
        return isAdspSpatializerAvailable() && SystemProperties.getBoolean(AUDIO_ADSP_SPATIALIZER_ENABLE, false);
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    List<String> clients = (List) msg.obj;
                    Log.d(AudioServiceStubImpl.TAG, "updateConcurrentVoipInfo voip apps : " + Arrays.toString(clients.toArray()));
                    if (AudioServiceStubImpl.this.mAudioService.isBluetoothA2dpOn()) {
                        AudioServiceStubImpl.this.mMultiVoipPackages = Arrays.toString(clients.toArray());
                    }
                    MQSUtils.trackConcurrentVoipInfo(AudioServiceStubImpl.this.mContext, clients);
                    return;
                case 2:
                    AudioServiceStubImpl.this.mMqsUtils.reportBtMultiVoipDailyUse(AudioServiceStubImpl.this.mContext, (String) msg.obj, AudioServiceStubImpl.this.mMultiVoipPackages);
                    return;
                case 3:
                    if (AudioServiceStubImpl.this.mMqsUtils != null) {
                        AudioServiceStubImpl.this.mMqsUtils.reportWaveformInfo((MQSUtils.WaveformInfo) msg.obj);
                        return;
                    }
                    return;
                case 4:
                    AudioServiceStubImpl.this.mMqsUtils.reportAudioSilentObserverToOnetrack(msg.getData().getInt(AudioServiceStubImpl.AUDIO_SILENT_LEVEL), msg.getData().getString(AudioServiceStubImpl.AUDIO_SILENT_LOCATION), msg.getData().getString(AudioServiceStubImpl.AUDIO_SILENT_REASON), msg.getData().getInt(AudioServiceStubImpl.AUDIO_SILENT_TYPE));
                    return;
                case 5:
                    AudioServiceStubImpl.this.mMqsUtils.reportAbnormalAudioStatus((MQSUtils.AudioStateTrackData) msg.obj);
                    return;
                case 6:
                    MQSserver.getInstance(AudioServiceStubImpl.this.mContext).asynReportData();
                    AudioServiceStubImpl.this.mHandler.sendMessageDelayed(AudioServiceStubImpl.this.mHandler.obtainMessage(6), AudioServiceStubImpl.MQSSERVER_REPORT_RATE_MS);
                    return;
                case 7:
                    AudioServiceStubImpl.this.handleAudioModeUpdate(((Integer) msg.obj).intValue());
                    return;
                case 8:
                    AudioServiceStubImpl.this.handleMeetingModeUpdate((String) msg.obj);
                    return;
                case 9:
                    AudioServiceStubImpl.this.handleMediaStateUpdate(((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }
}
