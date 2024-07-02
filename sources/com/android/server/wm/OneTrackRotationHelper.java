package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.devicestate.DeviceStateManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.WindowManager;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.cameracovered.CameraBlackCoveredManager;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.miui.server.input.edgesuppression.EdgeSuppressionFactory;
import com.miui.server.input.stylus.MiuiStylusShortcutManager;
import com.miui.server.stability.DumpSysInfoUtil;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import miui.util.MiuiMultiDisplayTypeInfo;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class OneTrackRotationHelper {
    private static final String APP_ID = "31000000779";
    private static final String DEVICE_TYPE;
    private static final boolean ENABLE_TRACK;
    private static final String EVENT_NAME = "screen_use_duration";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final boolean IS_FLIP;
    private static final boolean IS_FOLD;
    private static final boolean IS_TABLET;
    private static final String ONETRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final long ONE_TRACE_INTERVAL_DEBUG = 120000;
    private static final int ON_DEVICE_FOLD_CHANGED = 7;
    private static final int ON_FOREGROUND_WINDOW_CHANGED = 2;
    private static final int ON_NEXT_SENDING_COMING = 5;
    private static final int ON_ROTATION_CHANGED = 1;
    private static final int ON_SCREEN_STATE_CHANGED = 3;
    private static final int ON_SHUT_DOWN = 4;
    private static final int ON_TODAY_IS_OVER = 6;
    private static final String PACKAGE = "android";
    private static final String PACKAGE_NAME = "android";
    private static final int SCREEN_OFF = 3;
    private static final int SCREEN_ON = 1;
    private static final String SCREEN_ON_NOTIFICATION = "com.android.systemui:NOTIFICATION";
    private static final String SERVICE_PACKAGE_NAME = "com.miui.analytics";
    private static final String TAG = "OneTrackRotationHelper";
    private static final String TIP = "866.2.1.1.28680";
    private static final String TIP_FOR_SCREENDATA = "866.2.1.1.32924";
    private static volatile OneTrackRotationHelper sInstance;
    private long currentTimeMillis;
    private String lastForegroundPkg;
    ActivityManager mAm;
    Context mContext;
    Handler mHandler;
    KeyguardManager mKm;
    PowerManager mPm;
    private RotationStateMachine mRotationStateMachine;
    private BroadcastReceiver mScreenStateReceiver;
    HandlerThread mThread;
    WindowManager mWm;
    private static boolean DEBUG = false;
    private static boolean mIsScreenOnNotification = false;
    private static final long ONE_TRACE_INTERVAL = 7200000;
    private static long mReportInterval = ONE_TRACE_INTERVAL;
    private boolean mIsInit = false;
    private final float MIN_TIME = 0.2f;

    static {
        boolean z = "tablet".equals(SystemProperties.get("ro.build.characteristics", "")) || SystemProperties.getBoolean("ro.config.tablet", false);
        IS_TABLET = z;
        boolean isFoldDevice = MiuiMultiDisplayTypeInfo.isFoldDevice();
        IS_FOLD = isFoldDevice;
        boolean isFlipDevice = MiuiMultiDisplayTypeInfo.isFlipDevice();
        IS_FLIP = isFlipDevice;
        DEVICE_TYPE = z ? "tablet" : isFoldDevice ? "fold" : isFlipDevice ? "flip" : EdgeSuppressionFactory.TYPE_NORMAL;
        ENABLE_TRACK = isFoldDevice || z || isFlipDevice;
    }

    public static synchronized OneTrackRotationHelper getInstance() {
        OneTrackRotationHelper oneTrackRotationHelper;
        synchronized (OneTrackRotationHelper.class) {
            if (sInstance == null) {
                sInstance = new OneTrackRotationHelper();
            }
            oneTrackRotationHelper = sInstance;
        }
        return oneTrackRotationHelper;
    }

    private OneTrackRotationHelper() {
        Slog.i(TAG, "onetrack-rotation: enable = " + ENABLE_TRACK);
    }

    public void init() {
        if (!ENABLE_TRACK) {
            return;
        }
        Slog.i(TAG, "onetrack-rotation init");
        Application application = ActivityThread.currentActivityThread().getApplication();
        this.mContext = application;
        if (application == null) {
            Slog.e(TAG, "init OneTrackRotationHelper mContext = null");
            return;
        }
        this.mRotationStateMachine = new RotationStateMachine(this);
        this.mAm = (ActivityManager) this.mContext.getSystemService("activity");
        this.mWm = (WindowManager) this.mContext.getSystemService(DumpSysInfoUtil.WINDOW);
        this.mPm = (PowerManager) this.mContext.getSystemService("power");
        KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService(MiuiStylusShortcutManager.SCENE_KEYGUARD);
        this.mKm = keyguardManager;
        if (this.mAm == null || this.mWm == null || this.mPm == null || keyguardManager == null) {
            Slog.e(TAG, "getSystemService failed service:" + (this.mAm == null ? " AM" : "") + (this.mWm == null ? " WM" : "") + (this.mPm == null ? " PM" : "") + (this.mKm == null ? " KM" : ""));
            return;
        }
        initOneTrackRotationThread();
        initializeData();
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.OneTrackRotationHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                OneTrackRotationHelper.this.initAllListeners();
            }
        });
    }

    private void initializeData() {
        Context context;
        String packageName = "";
        ActivityManager activityManager = this.mAm;
        if (activityManager != null) {
            try {
                List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
                if (tasks.size() != 0) {
                    packageName = tasks.get(0).topActivity.getPackageName();
                }
            } catch (Exception e) {
                Slog.e(TAG, "initializeData packageName e= " + e);
            }
        }
        boolean screenState = false;
        PowerManager powerManager = this.mPm;
        if (powerManager != null && this.mKm != null) {
            screenState = powerManager.isScreenOn() && !this.mKm.isKeyguardLocked();
        }
        WindowManager windowManager = this.mWm;
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getRotation();
        }
        boolean folded = false;
        if ((IS_FOLD || IS_FLIP) && (context = this.mContext) != null) {
            folded = Settings.Global.getInt(context.getContentResolver(), "device_posture", 0) == 1;
        }
        this.mRotationStateMachine.init(packageName, screenState, folded, 0);
    }

    private int getDisplayRotation() {
        if (!this.mIsInit) {
            return 0;
        }
        return this.mWm.getDefaultDisplay().getRotation();
    }

    private boolean getScreenState() {
        if (!this.mIsInit) {
            return false;
        }
        return this.mPm.isScreenOn();
    }

    private boolean getKeyguardLocked() {
        if (!this.mIsInit) {
            return false;
        }
        return this.mKm.isKeyguardLocked();
    }

    public boolean isScreenRealUnlocked() {
        return this.mIsInit && getScreenState() && !getKeyguardLocked();
    }

    private String getTopAppName() {
        if (!this.mIsInit) {
            return "";
        }
        try {
            List<ActivityManager.RunningTaskInfo> tasks = this.mAm.getRunningTasks(1);
            return tasks.size() != 0 ? tasks.get(0).topActivity.getPackageName() : "";
        } catch (Exception e) {
            Slog.e(TAG, "getTopAppName e= " + e);
            return "";
        }
    }

    private void initOneTrackRotationThread() {
        HandlerThread handlerThread = new HandlerThread("OneTrackRotationThread");
        this.mThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mThread.getLooper()) { // from class: com.android.server.wm.OneTrackRotationHelper.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        OneTrackRotationHelper.this.mRotationStateMachine.onRotationChanged(msg);
                        return;
                    case 2:
                        OneTrackRotationHelper.this.mRotationStateMachine.onForegroundAppChanged(msg);
                        return;
                    case 3:
                        OneTrackRotationHelper.this.mRotationStateMachine.onScreenStateChanged(msg);
                        return;
                    case 4:
                        OneTrackRotationHelper.this.mRotationStateMachine.onShutDown();
                        return;
                    case 5:
                        OneTrackRotationHelper.this.mRotationStateMachine.onNextSending();
                        return;
                    case 6:
                        OneTrackRotationHelper.this.mRotationStateMachine.onTodayIsOver();
                        return;
                    case 7:
                        OneTrackRotationHelper.this.mRotationStateMachine.onDeviceFoldChanged(msg);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initAllListeners() {
        Context context = this.mContext;
        if (context == null) {
            Slog.e(TAG, "initAllListeners mContext = null");
            return;
        }
        if (IS_FOLD || IS_FLIP) {
            DeviceStateManager deviceStateManager = (DeviceStateManager) context.getSystemService(DeviceStateManager.class);
            if (deviceStateManager != null) {
                deviceStateManager.registerCallback(new HandlerExecutor(this.mHandler), new DeviceStateManager.FoldStateListener(this.mContext, new Consumer() { // from class: com.android.server.wm.OneTrackRotationHelper$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        OneTrackRotationHelper.this.reportDeviceFolded(((Boolean) obj).booleanValue());
                    }
                }));
            } else {
                Slog.v(TAG, "deviceStateManager == null");
            }
        }
        this.mScreenStateReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.OneTrackRotationHelper.2
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                if (!OneTrackRotationHelper.this.mIsInit) {
                    return;
                }
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2128145023:
                        if (action.equals("android.intent.action.SCREEN_OFF")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 823795052:
                        if (action.equals("android.intent.action.USER_PRESENT")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1947666138:
                        if (action.equals("android.intent.action.ACTION_SHUTDOWN")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 2039811242:
                        if (action.equals("android.intent.action.REBOOT")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        Message message1 = Message.obtain(OneTrackRotationHelper.this.mHandler, 3);
                        message1.obj = new AppUsageMessageObj(intent);
                        message1.sendToTarget();
                        return;
                    case 2:
                    case 3:
                        Message message2 = Message.obtain(OneTrackRotationHelper.this.mHandler, 4);
                        message2.obj = new AppUsageMessageObj(intent);
                        OneTrackRotationHelper.this.mHandler.sendMessageAtFrontOfQueue(message2);
                        return;
                    default:
                        return;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.REBOOT");
        try {
            this.mContext.registerReceiver(this.mScreenStateReceiver, filter);
            this.mIsInit = true;
            Slog.i(TAG, "OneTrackRotationHelper init successfully");
        } catch (Exception e) {
            Slog.e(TAG, "initAllListeners e = " + e);
        }
    }

    public void reportRotationChanged(int displayId, int rotation) {
        if (!ENABLE_TRACK || !this.mIsInit || displayId != 0) {
            return;
        }
        Message message = Message.obtain(this.mHandler, 1);
        message.arg1 = displayId;
        message.arg2 = rotation;
        message.obj = new AppUsageMessageObj();
        message.sendToTarget();
    }

    public void reportPackageForeground(String packageName) {
        if (ENABLE_TRACK && this.mIsInit && !TextUtils.equals(packageName, this.lastForegroundPkg)) {
            Message message = Message.obtain(this.mHandler, 2);
            message.obj = new AppUsageMessageObj(packageName);
            message.sendToTarget();
            this.lastForegroundPkg = packageName;
        }
    }

    public void reportDeviceFolded(boolean folded) {
        if (!ENABLE_TRACK || !this.mIsInit) {
            return;
        }
        Message message = Message.obtain(this.mHandler, 7);
        message.obj = new AppUsageMessageObj(Boolean.valueOf(folded));
        message.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareNextSending() {
        if (DEBUG) {
            Slog.i(TAG, "prepareNextSending");
        }
        this.mHandler.removeMessages(5);
        this.mHandler.sendEmptyMessageDelayed(5, mReportInterval);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Type inference failed for: r4v2, types: [java.time.ZonedDateTime] */
    public void prepareFinalSendingInToday() {
        if (DEBUG) {
            Slog.i(TAG, "prepareFinalSendingInToday");
        }
        long now = System.currentTimeMillis();
        LocalDate localDate = LocalDate.now().plusDays(1L);
        LocalDateTime dateTime = LocalDateTime.of(localDate.getYear(), localDate.getMonth(), localDate.getDayOfMonth(), 0, 0, 0);
        long nextDay = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        this.mRotationStateMachine.setToday(now);
        this.mHandler.removeMessages(6);
        this.mHandler.sendEmptyMessageDelayed(6, nextDay - now);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportOneTrack(ArrayList<String> dataList, int reportDate) {
        String str;
        try {
            try {
                try {
                    if (isReportXiaomiServer()) {
                        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
                        intent.setPackage("com.miui.analytics");
                        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
                        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
                        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
                        intent.putExtra("tip", TIP);
                        Bundle params = new Bundle();
                        params.putStringArrayList("all_app_usage_time", dataList);
                        intent.putExtras(params);
                        intent.putExtra("model_type", DEVICE_TYPE);
                        intent.putExtra("report_date", reportDate);
                        this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                    } else {
                        Intent intent2 = new Intent("onetrack.action.TRACK_EVENT");
                        intent2.setPackage("com.miui.analytics");
                        intent2.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
                        intent2.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
                        intent2.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
                        intent2.putExtra("PROJECT_ID", "thirdappadaptation");
                        intent2.putExtra("TOPIC", "topic_ods_pubsub_event_di_31000000779");
                        intent2.putExtra("PRIVATE_KEY_ID", "9f3945ec5765512b0ca43029da3f62aa93613c93");
                        intent2.putExtra("tip", TIP);
                        Bundle params2 = new Bundle();
                        params2.putStringArrayList("all_app_usage_time", dataList);
                        intent2.putExtras(params2);
                        intent2.putExtra("model_type", DEVICE_TYPE);
                        intent2.putExtra("report_date", reportDate);
                        intent2.setFlags(2);
                        this.mContext.startServiceAsUser(intent2, UserHandle.CURRENT);
                    }
                    if (DEBUG) {
                        str = TAG;
                        try {
                            Slog.i(str, "reportOneTrack");
                        } catch (Exception e) {
                            e = e;
                            Slog.e(str, "reportOneTrack e = " + e);
                        }
                    }
                } catch (Exception e2) {
                    e = e2;
                    str = TAG;
                    Slog.e(str, "reportOneTrack e = " + e);
                }
            } catch (Exception e3) {
                e = e3;
                str = TAG;
                Slog.e(str, "reportOneTrack e = " + e);
            }
        } catch (Exception e4) {
            e = e4;
            str = TAG;
        }
    }

    private boolean isReportXiaomiServer() {
        String region = SystemProperties.get("ro.miui.region", "");
        if (DEBUG) {
            Slog.i(TAG, "the region is :" + region);
        }
        return region.equals("CN") || region.equals("RU");
    }

    public void trackScreenData(final int wakefulness, final String details) {
        if (!ENABLE_TRACK || !this.mIsInit) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.OneTrackRotationHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                OneTrackRotationHelper.this.lambda$trackScreenData$0(wakefulness, details);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: sendOneTrackForScreenData, reason: merged with bridge method [inline-methods] */
    public void lambda$trackScreenData$0(int wakefulness, String details) {
        if (details != null && details.equals(SCREEN_ON_NOTIFICATION)) {
            mIsScreenOnNotification = true;
            return;
        }
        if (wakefulness == 1) {
            this.currentTimeMillis = System.currentTimeMillis();
        }
        if (wakefulness == 3) {
            mIsScreenOnNotification = false;
            reportOneTrackForScreenData();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recordTimeForFlip(float duration) {
        String key = "screen_direction_" + castToSting(this.mRotationStateMachine.mDisplayRotation);
        float existingDuration = ((Float) this.mRotationStateMachine.filpUsageDate.getOrDefault(key, Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X))).floatValue();
        this.mRotationStateMachine.filpUsageDate.put(key, Float.valueOf(existingDuration + duration));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportOneTrackForScreenData() {
        float duration = this.currentTimeMillis != 0 ? ((float) (System.currentTimeMillis() - this.currentTimeMillis)) / 1000.0f : 0.0f;
        if (duration != MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X && duration > 0.2f) {
            boolean z = IS_FLIP;
            if (z && this.mRotationStateMachine.mFolded) {
                this.mRotationStateMachine.oneTrackRotationHelper.recordTimeForFlip(duration);
            }
            reportOneTrackForScreenData(duration);
            if (z) {
                resetFilpUsageDate();
            }
        }
        this.currentTimeMillis = 0L;
    }

    private void reportOneTrackForScreenData(float duration) {
        String str;
        String str2;
        float duration2;
        float duration3;
        try {
            str = "_time";
            str2 = "screen_direction_";
        } catch (Exception e) {
            e = e;
        }
        try {
            if (isReportXiaomiServer()) {
                Intent intent = new Intent("onetrack.action.TRACK_EVENT");
                intent.setPackage("com.miui.analytics");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
                intent.putExtra("tip", TIP_FOR_SCREENDATA);
                intent.putExtra("model_type", DEVICE_TYPE);
                if (IS_FOLD || IS_FLIP) {
                    if (this.mRotationStateMachine.mFolded) {
                        intent.putExtra("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_OUTTER);
                    } else {
                        intent.putExtra("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_INNER);
                    }
                }
                if (IS_FLIP && this.mRotationStateMachine.mFolded) {
                    int[] directions = {0, 1, 2, 3};
                    duration3 = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                    int length = directions.length;
                    int i = 0;
                    while (i < length) {
                        int direction = directions[i];
                        String str3 = str2;
                        String key = str3 + castToSting(direction);
                        float time = ((Float) this.mRotationStateMachine.filpUsageDate.get(key)).floatValue();
                        String str4 = str;
                        String timeKey = str3 + castToSting(direction) + str4;
                        intent.putExtra(timeKey, time);
                        duration3 += time;
                        i++;
                        str2 = str3;
                        str = str4;
                    }
                } else {
                    duration3 = duration;
                }
                intent.putExtra("use_duration", duration3);
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                return;
            }
            Intent intent2 = new Intent("onetrack.action.TRACK_EVENT");
            intent2.setPackage("com.miui.analytics");
            intent2.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
            intent2.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
            intent2.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "android");
            intent2.putExtra("tip", TIP_FOR_SCREENDATA);
            intent2.putExtra("model_type", DEVICE_TYPE);
            if (IS_FOLD || IS_FLIP) {
                if (this.mRotationStateMachine.mFolded) {
                    intent2.putExtra("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_OUTTER);
                } else {
                    intent2.putExtra("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_INNER);
                }
            }
            if (IS_FLIP && this.mRotationStateMachine.mFolded) {
                int i2 = 0;
                int[] directions2 = {0, 1, 2, 3};
                duration2 = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                int length2 = directions2.length;
                while (i2 < length2) {
                    int direction2 = directions2[i2];
                    String str5 = str2;
                    String key2 = str5 + castToSting(direction2);
                    float time2 = ((Float) this.mRotationStateMachine.filpUsageDate.get(key2)).floatValue();
                    String str6 = str;
                    String timeKey2 = str5 + castToSting(direction2) + str6;
                    intent2.putExtra(timeKey2, time2);
                    duration2 += time2;
                    i2++;
                    str2 = str5;
                    str = str6;
                }
            } else {
                duration2 = duration;
            }
            intent2.putExtra("use_duration", duration2);
            intent2.setFlags(2);
            this.mContext.startServiceAsUser(intent2, UserHandle.CURRENT);
        } catch (Exception e2) {
            e = e2;
            Slog.e(TAG, "reportOneTrackForScreenData e = " + e);
        }
    }

    private String castToSting(int direction) {
        switch (direction) {
            case 0:
                return "up";
            case 1:
                return "left";
            case 2:
                return "down";
            case 3:
                return "right";
            default:
                return "unknown";
        }
    }

    private void resetFilpUsageDate() {
        for (String key : this.mRotationStateMachine.filpUsageDate.keySet()) {
            this.mRotationStateMachine.filpUsageDate.put(key, Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AppUsageMessageObj {
        public Object obj;
        public long time = System.currentTimeMillis();

        public AppUsageMessageObj() {
        }

        public AppUsageMessageObj(Object obj) {
            this.obj = obj;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class RotationStateMachine {
        private static final int DATA_CACHED_THRESHOLD = 30;
        private static final int DATA_CACHED_THRESHOLD_DEBUG = 15;
        private static int mCacheThreshold;
        private long dateOfToday;
        private boolean isNextSending;
        private String mCurPackageName;
        private int[] mDirection;
        private int mDisplayRotation;
        private final OneTrackRotationHelper oneTrackRotationHelper;
        private boolean mScreenState = true;
        private boolean mFolded = false;
        private int mLaunchCount = -1;
        private long mStartTime = -1;
        private long mEndTime = -1;
        private final HashMap<String, int[][]> appUsageDataCache = new HashMap<>();
        private boolean initialized = false;
        private boolean isShutDown = false;
        private HashMap<String, Float> filpUsageDate = new HashMap<>();

        static {
            mCacheThreshold = OneTrackRotationHelper.DEBUG ? 15 : 30;
        }

        public RotationStateMachine(OneTrackRotationHelper helper) {
            this.oneTrackRotationHelper = helper;
        }

        public void init(String packageName, boolean screenState, boolean folded, int rotation) {
            if (this.initialized) {
                return;
            }
            this.mCurPackageName = packageName;
            this.mScreenState = screenState;
            this.mFolded = folded;
            this.mDisplayRotation = rotation;
            this.mStartTime = System.currentTimeMillis();
            this.mLaunchCount = 0;
            this.mDirection = new int[4];
            this.initialized = true;
            this.filpUsageDate.put("screen_direction_up", Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
            this.filpUsageDate.put("screen_direction_left", Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
            this.filpUsageDate.put("screen_direction_down", Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
            this.filpUsageDate.put("screen_direction_right", Float.valueOf(MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X));
            if ("android".equals(SystemProperties.get("debug.onetrack.log", ""))) {
                Slog.i(OneTrackRotationHelper.TAG, "init DEBUG = true");
                OneTrackRotationHelper.DEBUG = true;
                OneTrackRotationHelper.mReportInterval = OneTrackRotationHelper.ONE_TRACE_INTERVAL_DEBUG;
                mCacheThreshold = 15;
            }
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "init RotationStateMachine  package = " + this.mCurPackageName + " screenState = " + this.mScreenState + " rotation = " + this.mDisplayRotation + " launchCount = " + this.mLaunchCount + " direction = " + Arrays.toString(this.mDirection) + " folded = " + this.mFolded);
            }
            this.oneTrackRotationHelper.prepareNextSending();
            this.oneTrackRotationHelper.prepareFinalSendingInToday();
        }

        public void onForegroundAppChanged(Message foregroundMessage) {
            AppUsageMessageObj aumObj = (AppUsageMessageObj) foregroundMessage.obj;
            String newPackageName = (String) aumObj.obj;
            if (!TextUtils.isEmpty(newPackageName) && !newPackageName.equals(this.mCurPackageName)) {
                if (OneTrackRotationHelper.DEBUG) {
                    Slog.i(OneTrackRotationHelper.TAG, "onForegroundAppChanged  pkgname=" + newPackageName);
                }
                long newStartTime = aumObj.time;
                setEndTime(newStartTime);
                this.mLaunchCount++;
                if (OneTrackRotationHelper.IS_FLIP && this.mFolded) {
                    switch (this.mDisplayRotation) {
                        case 0:
                            int[] iArr = this.mDirection;
                            iArr[0] = iArr[0] + 1;
                            break;
                        case 1:
                            int[] iArr2 = this.mDirection;
                            iArr2[1] = iArr2[1] + 1;
                            break;
                        case 2:
                            int[] iArr3 = this.mDirection;
                            iArr3[2] = iArr3[2] + 1;
                            break;
                        case 3:
                            int[] iArr4 = this.mDirection;
                            iArr4[3] = iArr4[3] + 1;
                            break;
                    }
                }
                reportAppUsageData();
                updateNewPackage(newPackageName);
                resetStartTime(newStartTime, "new-package");
                resetLaunchCount();
                resetDirectionCount();
            }
        }

        public void onScreenStateChanged(Message screenStateMessage) {
            AppUsageMessageObj aumObj = (AppUsageMessageObj) screenStateMessage.obj;
            Intent intent = (Intent) aumObj.obj;
            if (intent == null) {
                return;
            }
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "onScreenStateChanged " + intent.getAction());
            }
            String action = intent.getAction();
            long newStartTime = aumObj.time;
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                if (!this.mScreenState) {
                    updateScreenState(true);
                    resetStartTime(newStartTime, "screen-on");
                }
                if (OneTrackRotationHelper.mIsScreenOnNotification) {
                    this.oneTrackRotationHelper.currentTimeMillis = System.currentTimeMillis();
                    OneTrackRotationHelper.mIsScreenOnNotification = false;
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                setEndTime(newStartTime);
                boolean realScreenState = this.oneTrackRotationHelper.isScreenRealUnlocked();
                if (!realScreenState && this.mScreenState) {
                    reportAppUsageData();
                }
                updateScreenState(realScreenState);
                if (realScreenState) {
                    resetStartTime(newStartTime, "real-screen-on");
                }
            }
        }

        public void onRotationChanged(Message rotationMessage) {
            AppUsageMessageObj aumObj = (AppUsageMessageObj) rotationMessage.obj;
            int displayId = rotationMessage.arg1;
            int rotation = rotationMessage.arg2;
            long newStartTime = aumObj.time;
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "onRotationChanged " + displayId + "  " + rotation);
            }
            if (displayId == 0 && rotation != this.mDisplayRotation) {
                setEndTime(newStartTime);
                if (this.mScreenState) {
                    reportRotationChange(rotation);
                    reportAppUsageData();
                }
                if (OneTrackRotationHelper.IS_FLIP && this.mFolded) {
                    float duration = this.oneTrackRotationHelper.currentTimeMillis != 0 ? ((float) (System.currentTimeMillis() - this.oneTrackRotationHelper.currentTimeMillis)) / 1000.0f : MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                    this.oneTrackRotationHelper.recordTimeForFlip(duration);
                }
                updateRotation(rotation);
                if (OneTrackRotationHelper.IS_FLIP && this.mFolded) {
                    this.oneTrackRotationHelper.currentTimeMillis = System.currentTimeMillis();
                }
                resetStartTime(newStartTime, "rotation-changed");
                resetDirectionCount();
            }
        }

        public void onDeviceFoldChanged(Message foldMessage) {
            AppUsageMessageObj aumObj = (AppUsageMessageObj) foldMessage.obj;
            boolean folded = ((Boolean) aumObj.obj).booleanValue();
            long newStartTime = aumObj.time;
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "onDeviceFoldChanged " + folded);
            }
            if (this.mFolded != folded) {
                setEndTime(newStartTime);
                if (this.mScreenState) {
                    reportAppUsageData();
                }
                this.oneTrackRotationHelper.reportOneTrackForScreenData();
                updateFolded(folded);
                if (this.oneTrackRotationHelper.mPm.isScreenOn()) {
                    this.oneTrackRotationHelper.currentTimeMillis = System.currentTimeMillis();
                }
                resetStartTime(newStartTime, "fold-changed");
            }
        }

        public void onNextSending() {
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "onNextSending");
            }
            this.isNextSending = true;
            if (this.mScreenState) {
                long now = System.currentTimeMillis();
                this.mEndTime = now;
                reportAppUsageData();
                resetStartTime(now, "next-sending");
            } else {
                reportAppUsageData();
            }
            this.isNextSending = false;
        }

        public void onShutDown() {
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "onShutDown");
            }
            this.isShutDown = true;
            if (this.mScreenState) {
                long now = System.currentTimeMillis();
                this.mEndTime = now;
                reportAppUsageData();
                resetStartTime(now, "shutdown");
                return;
            }
            reportAppUsageData();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onTodayIsOver() {
            if (OneTrackRotationHelper.DEBUG) {
                Slog.i(OneTrackRotationHelper.TAG, "onTodayIsOver");
            }
            this.isNextSending = true;
            if (this.mScreenState) {
                long now = System.currentTimeMillis();
                this.mEndTime = now;
                reportAppUsageData();
                resetStartTime(now, "today-is-over-sending");
            } else {
                reportAppUsageData();
            }
            this.isNextSending = false;
            this.oneTrackRotationHelper.prepareFinalSendingInToday();
        }

        private void resetStartTime(long time, String reason) {
            this.mStartTime = time;
            if (OneTrackRotationHelper.DEBUG) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String sTime = simpleDateFormat.format(new Date(time));
                Slog.i(OneTrackRotationHelper.TAG, "start recording new data  pkgname=" + this.mCurPackageName + " rotation = " + this.mDisplayRotation + " folded = " + this.mFolded + " startTime = " + sTime + " reason = " + reason);
            }
        }

        private void resetLaunchCount() {
            this.mLaunchCount = 0;
        }

        private void resetDirectionCount() {
            Arrays.fill(this.mDirection, 0);
        }

        private void setEndTime(long time) {
            this.mEndTime = time;
        }

        private void updateNewPackage(String packageName) {
            this.mCurPackageName = packageName;
        }

        private void updateScreenState(boolean state) {
            this.mScreenState = state;
        }

        private void updateRotation(int rotation) {
            this.mDisplayRotation = rotation;
        }

        private void updateFolded(boolean folded) {
            this.mFolded = folded;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setToday(long time) {
            this.dateOfToday = time;
        }

        private void reportAppUsageData() {
            if (this.mScreenState) {
                long collectData = collectData();
                String str = this.mCurPackageName;
                int[][] iArr = this.appUsageDataCache.get(str);
                if (iArr == null) {
                    if (OneTrackRotationHelper.IS_FLIP) {
                        iArr = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, 2, 7);
                    } else {
                        iArr = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, 2, 3);
                    }
                    this.appUsageDataCache.put(str, iArr);
                }
                boolean z = this.mFolded;
                switch (this.mDisplayRotation) {
                    case 0:
                    case 2:
                        iArr[z ? 1 : 0][0] = (int) (r8[0] + collectData);
                        break;
                    case 1:
                    case 3:
                        iArr[z ? 1 : 0][1] = (int) (r8[1] + collectData);
                        break;
                }
                if (OneTrackRotationHelper.IS_FLIP && this.mFolded) {
                    int[] iArr2 = iArr[z ? 1 : 0];
                    int i = iArr2[3];
                    int[] iArr3 = this.mDirection;
                    iArr2[3] = i + iArr3[0];
                    int[] iArr4 = iArr[z ? 1 : 0];
                    iArr4[4] = iArr4[4] + iArr3[1];
                    int[] iArr5 = iArr[z ? 1 : 0];
                    iArr5[5] = iArr5[5] + iArr3[2];
                    int[] iArr6 = iArr[z ? 1 : 0];
                    iArr6[6] = iArr6[6] + iArr3[3];
                }
                int[] iArr7 = iArr[z ? 1 : 0];
                iArr7[2] = iArr7[2] + this.mLaunchCount;
                if (OneTrackRotationHelper.DEBUG) {
                    Slog.i(OneTrackRotationHelper.TAG, "generate new data =  packageName = " + str + " rotation = " + this.mDisplayRotation + " duration = " + collectData + " " + (OneTrackRotationHelper.IS_FLIP ? "direction[0][90][180][270] = [" + this.mDirection[0] + "][" + this.mDirection[1] + "][" + this.mDirection[2] + "][" + this.mDirection[3] + "]" : "") + " cache size = " + this.appUsageDataCache.size());
                }
            }
            int size = this.appUsageDataCache.size();
            if (this.isShutDown || size >= mCacheThreshold || this.isNextSending) {
                if (OneTrackRotationHelper.DEBUG) {
                    Slog.i(OneTrackRotationHelper.TAG, "reportMergedAppUsageData sendAll");
                }
                sendAllAppUsageData();
                this.oneTrackRotationHelper.prepareNextSending();
            }
        }

        private void reportRotationChange(int rotation) {
            if (this.mScreenState) {
                switch (rotation) {
                    case 0:
                        int[] iArr = this.mDirection;
                        iArr[0] = iArr[0] + 1;
                        return;
                    case 1:
                        int[] iArr2 = this.mDirection;
                        iArr2[1] = iArr2[1] + 1;
                        return;
                    case 2:
                        int[] iArr3 = this.mDirection;
                        iArr3[2] = iArr3[2] + 1;
                        return;
                    case 3:
                        int[] iArr4 = this.mDirection;
                        iArr4[3] = iArr4[3] + 1;
                        return;
                    default:
                        return;
                }
            }
        }

        private long collectData() {
            if (TextUtils.isEmpty(this.mCurPackageName)) {
                if (OneTrackRotationHelper.DEBUG) {
                    Slog.i(OneTrackRotationHelper.TAG, "collectData empty package");
                }
                return 0L;
            }
            long duration = this.mEndTime - this.mStartTime;
            if (duration < 1000) {
                if (OneTrackRotationHelper.DEBUG) {
                    Slog.i(OneTrackRotationHelper.TAG, "collectData drop this data");
                }
                return 0L;
            }
            return duration / 1000;
        }

        private ArrayList<String> prepareAllData() {
            ArrayList<String> dataList = new ArrayList<>();
            for (String packageName : this.appUsageDataCache.keySet()) {
                int[][] detail = this.appUsageDataCache.get(packageName);
                if (detail != null) {
                    try {
                        if (detail[0][0] != 0 || detail[0][1] != 0 || detail[0][2] != 0) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put(CameraBlackCoveredManager.EXTRA_PACKAGE_NAME, packageName);
                            jsonData.put("portrait", detail[0][0]);
                            jsonData.put("landscape", detail[0][1]);
                            jsonData.put("start_number", detail[0][2]);
                            if (OneTrackRotationHelper.IS_FOLD || OneTrackRotationHelper.IS_FLIP) {
                                jsonData.put("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_INNER);
                            }
                            dataList.add(jsonData.toString());
                        }
                        if (OneTrackRotationHelper.IS_FOLD && (detail[1][0] != 0 || detail[1][1] != 0 || detail[1][2] != 0)) {
                            JSONObject jsonData2 = new JSONObject();
                            jsonData2.put(CameraBlackCoveredManager.EXTRA_PACKAGE_NAME, packageName);
                            jsonData2.put("portrait", detail[1][0]);
                            jsonData2.put("landscape", detail[1][1]);
                            jsonData2.put("start_number", detail[1][2]);
                            jsonData2.put("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_OUTTER);
                            dataList.add(jsonData2.toString());
                        }
                        if (OneTrackRotationHelper.IS_FLIP && (detail[1][0] != 0 || detail[1][1] != 0 || detail[1][2] != 0)) {
                            JSONObject jsonData3 = new JSONObject();
                            jsonData3.put(CameraBlackCoveredManager.EXTRA_PACKAGE_NAME, packageName);
                            jsonData3.put("portrait", detail[1][0]);
                            jsonData3.put("landscape", detail[1][1]);
                            jsonData3.put("start_number", detail[1][2]);
                            jsonData3.put("screen_type", MiuiFreeformTrackManager.CommonTrackConstants.SCREEN_TYPE_OUTTER);
                            jsonData3.put("screen_direction_0", detail[1][3]);
                            jsonData3.put("screen_direction_90", detail[1][4]);
                            jsonData3.put("screen_direction_180", detail[1][5]);
                            jsonData3.put("screen_direction_270", detail[1][6]);
                            dataList.add(jsonData3.toString());
                        }
                    } catch (JSONException e) {
                        Slog.e(OneTrackRotationHelper.TAG, "prepareAllData e = " + e);
                    }
                }
            }
            if (dataList.size() == 0) {
                return null;
            }
            return dataList;
        }

        private void sendAllAppUsageData() {
            if (this.appUsageDataCache.size() == 0) {
                return;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            String dateTime = simpleDateFormat.format(new Date(this.dateOfToday));
            int intDateTime = Integer.parseInt(dateTime);
            ArrayList<String> dataList = prepareAllData();
            if (OneTrackRotationHelper.DEBUG) {
                Slog.v(OneTrackRotationHelper.TAG, "sendAllAppUsageData data = " + dataList);
            }
            if (dataList != null) {
                this.oneTrackRotationHelper.reportOneTrack(dataList, intDateTime);
            }
            this.appUsageDataCache.clear();
            resetLaunchCount();
            resetDirectionCount();
        }
    }
}
