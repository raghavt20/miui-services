package com.android.server.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioSystem;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;

/* loaded from: classes.dex */
public class AudioGameEffect {
    private static final String CURRENT_PLATFORM = "Build.BRAND";
    private static final String CURRENT_REGION = "ro.miui.build.region";
    private static final int HEADSET_PLUG_IN = 1;
    private static final int HEADSET_PLUG_OUT = 0;
    private static final String IS_CEREGION = "sys.audio.ceregion";
    private static final String IS_SUPPORT_SPEAKER = "persist.vendor.audio.fpsop.game.effect.speaker";
    private static final int MSG_SET_PROCESS_LISTENER = 1;
    private static final int MSG_START_GAME_EFFECT = 2;
    private static final int MSG_STOP_GAME_EFFECT = 3;
    private static final String PM_SERVICE_NAME = "ProcessManager";
    private static final int QUERY_PM_SERVICE_DELAY_MS = 1000;
    private static final int QUERY_PM_SERVICE_MAX_TIMES = 20;
    private static final int SEND_PARAMETER_DELAY_MS = 500;
    private static final int[] SUPPORTED_DEVICES = {8, 7, 26, 3, 4, 22};
    private static final String TAG = "AudioGameEffect";
    private final Context mContext;
    private Map<String, ArrayList<String>> mDeviceEffects;
    private Set<String> mEnablePackages;
    private Set<String> mFpsPackages;
    private Map<String, String> mGameEffects;
    private GameEffectHandler mHandler;
    private final BroadcastReceiver mHeadSetReceiver;
    private ArrayList<String> mHeadsetEffects;
    private ArrayList<String> mSpeakerEffects;
    private Thread mThread;
    private final String SETTING_PKG_NAME = "game_effect_packages";
    private final String PREFIX_PARAMETER_ON = "misound_fps_effect=T-";
    private final String PARAMETER_OFF = "misound_fps_effect=F-0-0-0-0";
    private int mQueryPMServiceTime = 0;
    private String mCurrentPlatform = SystemProperties.get(CURRENT_PLATFORM, "");
    private boolean mIsSupportSpeaker = SystemProperties.getBoolean(IS_SUPPORT_SPEAKER, false);
    private String mCurrentEnablePkg = "";
    private String mCurrentDevice = "";
    private volatile boolean isEffectOn = false;
    private final IForegroundInfoListener.Stub mForegroundInfoListener = new IForegroundInfoListener.Stub() { // from class: com.android.server.audio.AudioGameEffect.1
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) throws RemoteException {
            Log.i(AudioGameEffect.TAG, "foreground change to " + foregroundInfo.mForegroundPackageName + ", last foreground is " + foregroundInfo.mLastForegroundPackageName);
            if (AudioGameEffect.this.isPackageEnabled(foregroundInfo.mLastForegroundPackageName)) {
                AudioGameEffect.this.mCurrentEnablePkg = "";
                if (AudioGameEffect.this.isEffectOn) {
                    AudioGameEffect.this.sendMsgDelay(3, 500L);
                }
            }
            if (AudioGameEffect.this.isPackageEnabled(foregroundInfo.mForegroundPackageName)) {
                AudioGameEffect.this.mCurrentEnablePkg = foregroundInfo.mForegroundPackageName;
                if (!AudioGameEffect.this.isCurrentDeviceSupported()) {
                    Log.i(AudioGameEffect.TAG, "current device not support");
                } else if (AudioGameEffect.this.isSpatialAudioEnabled()) {
                    Log.i(AudioGameEffect.TAG, "spatial audio enabled, return");
                } else {
                    AudioGameEffect.this.sendMsgDelay(2, 500L);
                }
            }
        }
    };

    public AudioGameEffect(Context cxt) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.audio.AudioGameEffect.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i(AudioGameEffect.TAG, "onReceive, action=" + action);
                if (!"".equals(AudioGameEffect.this.mCurrentEnablePkg)) {
                    if ("android.intent.action.HEADSET_PLUG".equals(action)) {
                        int state = intent.getIntExtra("state", -1);
                        if (state == 1) {
                            AudioGameEffect.this.mCurrentDevice = "headsets";
                            AudioGameEffect.this.sendMsgDelay(2, 500L);
                            return;
                        } else {
                            if (state == 0) {
                                AudioGameEffect.this.mCurrentDevice = "speaker";
                                if (AudioGameEffect.this.isCurrentSceneSupported()) {
                                    AudioGameEffect.this.sendMsgDelay(2, 500L);
                                    return;
                                } else {
                                    AudioGameEffect.this.sendMsgDelay(3, 500L);
                                    return;
                                }
                            }
                            return;
                        }
                    }
                    if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                        int state2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                        if (state2 == 2) {
                            AudioGameEffect.this.mCurrentDevice = "headsets";
                            AudioGameEffect.this.sendMsgDelay(2, 500L);
                        } else if (state2 == 0) {
                            AudioGameEffect.this.mCurrentDevice = "speaker";
                            if (AudioGameEffect.this.isCurrentSceneSupported()) {
                                AudioGameEffect.this.sendMsgDelay(2, 500L);
                            } else {
                                AudioGameEffect.this.sendMsgDelay(3, 500L);
                            }
                        }
                    }
                }
            }
        };
        this.mHeadSetReceiver = broadcastReceiver;
        this.mContext = cxt;
        initLocalPackagesAndEffects();
        GameEffectThread gameEffectThread = new GameEffectThread();
        this.mThread = gameEffectThread;
        gameEffectThread.start();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        cxt.registerReceiver(broadcastReceiver, filter);
    }

    private void getEnablePackagesFromCloudSettings() {
        String pkgs = Settings.Global.getString(this.mContext.getContentResolver(), "game_effect_packages");
        Log.i(TAG, "get enable packages from setting: " + pkgs);
        if (pkgs != null && !"".equals(pkgs)) {
            this.mEnablePackages.clear();
            String[] packages = pkgs.split(",");
            for (int i = 0; i < packages.length; i++) {
                if (!"".equals(packages[i])) {
                    this.mEnablePackages.add(packages[i]);
                }
            }
        }
    }

    private void getEnableSpeakerFromCloudSettings() {
        boolean speaker = SystemProperties.getBoolean(IS_SUPPORT_SPEAKER, false);
        Log.i(TAG, "get speaker from global setting: " + speaker);
        this.mIsSupportSpeaker = speaker;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPackageEnabled(String pkgName) {
        getEnablePackagesFromCloudSettings();
        return this.mEnablePackages.contains(pkgName);
    }

    private boolean isFpsPackages(String pkgName) {
        return this.mFpsPackages.contains(pkgName);
    }

    /* loaded from: classes.dex */
    private class GameEffectThread extends Thread {
        GameEffectThread() {
            super("GameEffectThread");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            super.run();
            Looper.prepare();
            AudioGameEffect.this.mHandler = new GameEffectHandler();
            AudioGameEffect.this.mQueryPMServiceTime = 0;
            AudioGameEffect.this.initProcessListenerAsync();
            Looper.loop();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GameEffectHandler extends Handler {
        GameEffectHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (ServiceManager.getService("ProcessManager") != null) {
                        ProcessManager.registerForegroundInfoListener(AudioGameEffect.this.mForegroundInfoListener);
                        return;
                    }
                    AudioGameEffect audioGameEffect = AudioGameEffect.this;
                    int i = audioGameEffect.mQueryPMServiceTime;
                    audioGameEffect.mQueryPMServiceTime = i + 1;
                    if (i < 20) {
                        Log.w(AudioGameEffect.TAG, "process manager service not published, wait 1 second");
                        AudioGameEffect.this.initProcessListenerAsync();
                        return;
                    } else {
                        Log.e(AudioGameEffect.TAG, "failed to get ProcessManager service");
                        return;
                    }
                case 2:
                    AudioGameEffect.this.startGameEffect();
                    return;
                case 3:
                    AudioGameEffect.this.stopGameEffect();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initProcessListenerAsync() {
        sendMsgDelay(1, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x002c, code lost:
    
        r3 = r3 + 1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean isCurrentDeviceSupported() {
        /*
            r9 = this;
            android.content.Context r0 = r9.mContext
            java.lang.String r1 = "audio"
            java.lang.Object r0 = r0.getSystemService(r1)
            android.media.AudioManager r0 = (android.media.AudioManager) r0
            r1 = 2
            android.media.AudioDeviceInfo[] r1 = r0.getDevices(r1)
            int r2 = r1.length
            r3 = 0
        L11:
            r4 = 1
            if (r3 >= r2) goto L2f
            r5 = r1[r3]
            r6 = 0
        L17:
            int[] r7 = com.android.server.audio.AudioGameEffect.SUPPORTED_DEVICES
            int r8 = r7.length
            if (r6 >= r8) goto L2c
            r7 = r7[r6]
            int r8 = r5.getType()
            if (r7 != r8) goto L29
            java.lang.String r2 = "headsets"
            r9.mCurrentDevice = r2
            return r4
        L29:
            int r6 = r6 + 1
            goto L17
        L2c:
            int r3 = r3 + 1
            goto L11
        L2f:
            java.lang.String r2 = "speaker"
            r9.mCurrentDevice = r2
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.AudioGameEffect.isCurrentDeviceSupported():boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCurrentSceneSupported() {
        String currentIsCERegion = SystemProperties.get(IS_CEREGION, "false");
        String currentRegion = SystemProperties.get(CURRENT_REGION, "");
        if (currentIsCERegion.equals("false") && !currentRegion.equals("cn")) {
            Log.i(TAG, "Non ceRegion, return");
            return false;
        }
        if (isSpatialAudioEnabled()) {
            Log.i(TAG, "spatial audio enabled, return");
            return false;
        }
        getEnableSpeakerFromCloudSettings();
        if (this.mCurrentDevice == "speaker" && !this.mIsSupportSpeaker) {
            Log.i(TAG, "AudioGameEffect does not support speaker");
            return false;
        }
        if (!isFpsPackages(this.mCurrentEnablePkg) && this.mCurrentDevice == "speaker") {
            Log.i(TAG, "not FPS speaker no need to startGameEffect");
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSpatialAudioEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "spatial_audio_feature_enable", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startGameEffect() {
        String paramIndex = this.mGameEffects.get(this.mCurrentEnablePkg);
        if (paramIndex == null || !isCurrentSceneSupported()) {
            Log.e(TAG, "no parameter found for package: " + this.mCurrentEnablePkg);
            return;
        }
        String parameterSuffix = this.mDeviceEffects.get(this.mCurrentDevice).get(Integer.parseInt(paramIndex));
        this.isEffectOn = true;
        Log.i(TAG, "startGameEffect parameter=misound_fps_effect=T-" + parameterSuffix);
        AudioSystem.setParameters("misound_fps_effect=T-" + parameterSuffix);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopGameEffect() {
        this.isEffectOn = false;
        Log.i(TAG, "stopGameEffect parameter=misound_fps_effect=F-0-0-0-0");
        AudioSystem.setParameters("misound_fps_effect=F-0-0-0-0");
    }

    /* JADX WARN: Removed duplicated region for block: B:39:0x0197 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:47:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:52:0x01ab A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:59:? A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void parseAudioGameEffectXml() {
        /*
            Method dump skipped, instructions count: 443
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.AudioGameEffect.parseAudioGameEffectXml():void");
    }

    private void initLocalPackagesAndEffects() {
        this.mEnablePackages = new ArraySet();
        this.mFpsPackages = new ArraySet();
        this.mGameEffects = new ArrayMap();
        this.mDeviceEffects = new ArrayMap();
        this.mHeadsetEffects = new ArrayList<>();
        this.mSpeakerEffects = new ArrayList<>();
        parseAudioGameEffectXml();
        this.mDeviceEffects.put("headsets", this.mHeadsetEffects);
        this.mDeviceEffects.put("speaker", this.mSpeakerEffects);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMsgDelay(int mesWhat, long delay) {
        Message msg = this.mHandler.obtainMessage(mesWhat);
        this.mHandler.sendMessageDelayed(msg, delay);
    }
}
