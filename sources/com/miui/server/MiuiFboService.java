package com.miui.server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.SystemService;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProviderUtils;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.miui.app.MiuiFboServiceInternal;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import miui.fbo.IFbo;
import miui.fbo.IFboManager;
import miui.hardware.CldManager;

/* loaded from: classes.dex */
public class MiuiFboService extends IFboManager.Stub implements MiuiFboServiceInternal {
    private static final int APP_GC_AND_DISCARD = 4;
    private static final String APP_ID = "31000000454";
    public static final int CHANGE_USB_STATUS = 6;
    private static final String CONNECT_NATIVESERVICE_NAME = "FboNativeService";
    public static final String CONTINUE = "continue";
    private static final String ENABLED_FBO = "persist.sys.stability.miui_fbo_enable";
    private static final int EUA_PECYCLE_FREEBLOCK = 6;
    private static final int EUA_UNMAP = 7;
    private static final String FALSE = "false";
    private static final String FBO_EVENT_NAME = "fbo_event";
    private static final String FBO_START_COUNT = "persist.sys.stability.miui_fbo_start_count";
    private static final int FBO_STATECTL = 3;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String HAL_DEFAULT = "default";
    private static final String HAL_INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.fbo@1.0::IFbo";
    private static final String HAL_SERVICE_NAME = "vendor.xiaomi.hardware.fbo@1.0::IFbo";
    private static final String HANDLER_NAME = "fboServiceWork";
    private static final int IS_FBO_SUPPORTED = 1;
    private static final String MIUI_FBO_PROCESSED_DONE = "miui.intent.action.FBO_PROCESSED_DONE";
    public static final String MIUI_FBO_RECEIVER_START = "miui.intent.action.start";
    public static final String MIUI_FBO_RECEIVER_STARTAGAIN = "miui.intent.action.startAgain";
    public static final String MIUI_FBO_RECEIVER_STOP = "miui.intent.action.stop";
    public static final String MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER = "miui.intent.action.transferFboTrigger";
    private static final String NATIVE_SERVICE_KEY = "persist.sys.fboservice.ctrl";
    private static final String NATIVE_SOCKET_NAME = "fbs_native_socket";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final int OVERLOAD_FBO_SUPPORTED = 5;
    private static final String PACKAGE = "android";
    public static final String SERVICE_NAME = "miui.fbo.service";
    public static final int START_FBO = 1;
    public static final int START_FBO_AGAIN = 2;
    public static final String STOP = "stop";
    public static final String STOPDUETOBATTERYTEMPERATURE = "stopDueTobatteryTemperature";
    public static final String STOPDUETOSCREEN = "stopDueToScreen";
    public static final int STOP_DUETO_BATTERYTEMPERATURE = 5;
    public static final int STOP_DUETO_SCREEN = 4;
    public static final int STOP_FBO = 3;
    private static final int TRIGGER_CLD = 2;
    private static final String TRUE = "true";
    private static CldManager cldManager;
    private static LocalSocket interactClientSocket;
    private static AlarmManager mAlarmManager;
    private static String mCurrentPackageName;
    private static PendingIntent mPendingIntent;
    private static LocalServerSocket mServerSocket;
    private int batteryLevel;
    private int batteryStatus;
    private int batteryTemperature;
    private volatile boolean cldStrategyStatus;
    private Context mContext;
    private volatile IFbo mFboNativeService;
    private volatile FboServiceHandler mFboServiceHandler;
    private HandlerThread mFboServiceThread;
    private String mVdexPath;
    private boolean screenOn;
    private String stagingData;
    private static final String TAG = MiuiFboService.class.getSimpleName();
    private static volatile MiuiFboService sInstance = null;
    private static boolean mKeepRunning = true;
    private static boolean mFinishedApp = false;
    private static OutputStream outputStream = null;
    private static InputStream inputStream = null;
    private static ArrayList<String> packageNameList = new ArrayList<>();
    private static int listSize = 0;
    private static ArrayList<String> mStopReason = new ArrayList<>();
    private static final Object mLock = new Object();
    private static Long mFragmentCount = 0L;
    private static ArrayList<String> packageName = new ArrayList<>();
    private static ArrayList<PendingIntent> pendingIntentList = new ArrayList<>();
    private int batteryStatusMark = 0;
    private boolean messageHasbeenSent = false;
    private boolean nativeIsRunning = false;
    private boolean globalSwitch = false;
    private boolean dueToScreenWait = false;
    private int screenOnTimes = 0;
    private boolean enableNightJudgment = true;
    private boolean usbState = true;
    private boolean mDriverSupport = false;
    private int mCurrentBatteryLevel = 0;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FboServiceHandler extends Handler {
        public FboServiceHandler(Looper looper) {
            super(looper, null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiFboService.this.messageHasbeenSent = false;
                    Slog.d(MiuiFboService.TAG, "current screenOn" + MiuiFboService.this.screenOn + "current batteryStatus" + MiuiFboService.this.batteryStatus + "current batteryLevel" + MiuiFboService.this.batteryLevel + "current batteryTemperature" + MiuiFboService.this.batteryTemperature + "screenOnTimes" + MiuiFboService.this.screenOnTimes);
                    if (MiuiFboService.this.screenOnTimes == 0 && !MiuiFboService.this.screenOn && ((MiuiFboService.this.batteryStatus > 0 || MiuiFboService.this.batteryLevel > 75) && MiuiFboService.this.batteryTemperature < 400)) {
                        MiuiFboService miuiFboService = MiuiFboService.this;
                        miuiFboService.mCurrentBatteryLevel = miuiFboService.batteryLevel;
                        try {
                            if (MiuiFboService.listSize >= 0) {
                                MiuiFboService.mCurrentPackageName = (String) MiuiFboService.packageNameList.remove(MiuiFboService.listSize);
                                MiuiFboService.this.mFboNativeService.startF2fsGC();
                                ApplicationInfo applicationInfo = MiuiFboService.this.mContext.getPackageManager().getApplicationInfo(MiuiFboService.mCurrentPackageName, 1024);
                                Slog.d(MiuiFboService.TAG, "vdex path:" + applicationInfo.sourceDir);
                                MiuiFboService.this.mVdexPath = applicationInfo.sourceDir;
                                MiuiFboService.this.mFboNativeService.FBO_trigger(MiuiFboService.mCurrentPackageName, MiuiFboService.this.mDriverSupport, MiuiFboService.this.mVdexPath.substring(0, MiuiFboService.this.mVdexPath.lastIndexOf("/")));
                                MiuiFboService.this.setNativeIsRunning(true);
                                MiuiFboService.this.setGlobalSwitch(true);
                            }
                            MiuiFboService.listSize--;
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    if (MiuiFboService.this.batteryTemperature >= 500) {
                        MiuiFboService.mStopReason.add("batteryTemperature >= 500");
                        Slog.d(MiuiFboService.TAG, "do not meet the conditions exit");
                        return;
                    } else {
                        MiuiFboService unused = MiuiFboService.sInstance;
                        MiuiFboService.setAlarm(MiuiFboService.MIUI_FBO_RECEIVER_START, null, SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION, false);
                        MiuiFboService.this.screenOnTimes = 0;
                        MiuiFboService.mStopReason.add("screenStatus:" + MiuiFboService.this.screenOn + ",batteryStatus:" + MiuiFboService.this.batteryStatus + ",batteryLevel:" + MiuiFboService.this.batteryLevel + ",batteryTemperature:" + MiuiFboService.this.batteryTemperature + ",screenOnTimes:" + MiuiFboService.this.screenOnTimes);
                        return;
                    }
                case 2:
                    Slog.d(MiuiFboService.TAG, "current screenOn" + MiuiFboService.this.screenOn + "current batteryStatus" + MiuiFboService.this.batteryStatus + "current batteryLevel" + MiuiFboService.this.batteryLevel + "current batteryTemperature" + MiuiFboService.this.batteryTemperature + "screenOnTimes" + MiuiFboService.this.screenOnTimes);
                    if (MiuiFboService.this.screenOnTimes == 0 && !MiuiFboService.this.screenOn && ((MiuiFboService.this.batteryStatus > 0 || MiuiFboService.this.batteryLevel > 75) && MiuiFboService.this.batteryTemperature < 400)) {
                        MiuiFboService miuiFboService2 = MiuiFboService.this;
                        miuiFboService2.mCurrentBatteryLevel = miuiFboService2.batteryLevel;
                        try {
                            MiuiFboService.useCldStrategy(1);
                            MiuiFboService.this.mFboNativeService.FBO_stateCtl("continue," + MiuiFboService.this.stagingData);
                            MiuiFboService.this.mFboNativeService.FBO_trigger(MiuiFboService.mCurrentPackageName, MiuiFboService.this.mDriverSupport, MiuiFboService.this.mVdexPath.substring(0, MiuiFboService.this.mVdexPath.lastIndexOf("/")));
                            MiuiFboService.this.sendStopOrContinueToHal("continue");
                            MiuiFboService.this.setNativeIsRunning(true);
                            MiuiFboService.this.setDueToScreenWait(false);
                            return;
                        } catch (Exception e2) {
                            Slog.d(MiuiFboService.TAG, "fail to execute START_FBO_AGAIN");
                            return;
                        }
                    }
                    if (MiuiFboService.this.batteryTemperature >= 500) {
                        MiuiFboService.mStopReason.add("stop because batteryTemperature >= 500");
                        Slog.d(MiuiFboService.TAG, "do not meet the conditions exit");
                        return;
                    } else {
                        MiuiFboService unused2 = MiuiFboService.sInstance;
                        MiuiFboService.setAlarm(MiuiFboService.MIUI_FBO_RECEIVER_STARTAGAIN, null, 600000L, false);
                        MiuiFboService.this.screenOnTimes = 0;
                        MiuiFboService.mStopReason.add("screenStatus:" + MiuiFboService.this.screenOn + ",batteryStatus:" + MiuiFboService.this.batteryStatus + ",batteryLevel:" + MiuiFboService.this.batteryLevel + ",batteryTemperature:" + MiuiFboService.this.batteryTemperature + ",screenOnTimes:" + MiuiFboService.this.screenOnTimes);
                        return;
                    }
                case 3:
                    try {
                        if (!MiuiFboService.mFinishedApp) {
                            MiuiFboService.useCldStrategy(1);
                        }
                        MiuiFboService.this.reportFboEvent();
                        MiuiFboService.mFinishedApp = false;
                        MiuiFboService.this.mFboNativeService.FBO_stateCtl("stop");
                        MiuiFboService.this.sendStopOrContinueToHal("stop");
                        MiuiFboService.this.setNativeIsRunning(false);
                        MiuiFboService.this.setGlobalSwitch(false);
                        MiuiFboService.clearBroadcastData();
                        MiuiFboService.sInstance.userAreaExtend();
                        SystemProperties.set(MiuiFboService.NATIVE_SERVICE_KEY, MiuiFboService.FALSE);
                        MiuiFboService.this.mFboServiceHandler.removeCallbacksAndMessages(null);
                        MiuiFboService.this.setEnableNightJudgment(false);
                        MiuiFboService.this.messageHasbeenSent = false;
                        Slog.d(MiuiFboService.TAG, "All stop,exit");
                        return;
                    } catch (Exception e3) {
                        Slog.d(MiuiFboService.TAG, "fail to execute STOP_FBO");
                        return;
                    }
                case 4:
                    try {
                        MiuiFboService.useCldStrategy(0);
                        MiuiFboService miuiFboService3 = MiuiFboService.this;
                        miuiFboService3.stagingData = miuiFboService3.mFboNativeService.FBO_stateCtl("stop");
                        Slog.d(MiuiFboService.TAG, "stopDueToScreen && sreenStagingData:" + MiuiFboService.this.stagingData);
                        MiuiFboService.this.sendStopOrContinueToHal("stop");
                        MiuiFboService.this.setNativeIsRunning(false);
                        MiuiFboService.this.setDueToScreenWait(true);
                        MiuiFboService unused3 = MiuiFboService.sInstance;
                        MiuiFboService.setAlarm(MiuiFboService.MIUI_FBO_RECEIVER_STARTAGAIN, null, 600000L, false);
                        MiuiFboService.this.screenOnTimes = 0;
                        MiuiFboService.mStopReason.add("stop because screen on");
                        return;
                    } catch (Exception e4) {
                        Slog.d(MiuiFboService.TAG, "fail to execute STOP_DUETO_SCREEN");
                        return;
                    }
                case 5:
                    try {
                        MiuiFboService.useCldStrategy(0);
                        MiuiFboService miuiFboService4 = MiuiFboService.this;
                        miuiFboService4.stagingData = miuiFboService4.mFboNativeService.FBO_stateCtl("stop");
                        Slog.d(MiuiFboService.TAG, "stopDueTobatteryTemperature && batteryTemperatureStagingData:" + MiuiFboService.this.stagingData);
                        MiuiFboService.this.sendStopOrContinueToHal("stop");
                        MiuiFboService.this.setNativeIsRunning(false);
                        MiuiFboService.mStopReason.add("stop because battery,batteryStatus:" + MiuiFboService.this.batteryStatus + ",batteryLevel:" + MiuiFboService.this.batteryLevel + ",batteryTemperature:" + MiuiFboService.this.batteryTemperature);
                        return;
                    } catch (Exception e5) {
                        Slog.d(MiuiFboService.TAG, "fail to execute STOP_DUETO_BATTERYTEMPERATURE");
                        return;
                    }
                case 6:
                    try {
                        String state = FileUtils.readTextFile(new File("/sys/class/android_usb/android0/state"), 128, "");
                        MiuiFboService.this.setUsbState("CONFIGURED".equals(state.trim()));
                        Slog.d(MiuiFboService.TAG, "USBState:" + MiuiFboService.this.getUsbState());
                        return;
                    } catch (Exception e6) {
                        Slog.e(MiuiFboService.TAG, "Failed to determine if device was on USB", e6);
                        MiuiFboService.this.setUsbState(true);
                        return;
                    }
                default:
                    Slog.d(MiuiFboService.TAG, "Unrecognized message command");
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class AlarmReceiver extends BroadcastReceiver {
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            Slog.d(MiuiFboService.TAG, "received the broadcast and intent.action : " + intent.getAction() + ",intent.getStringExtra:" + intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME));
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1989312766:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_STARTAGAIN)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -467317708:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1208808190:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_START)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1285920230:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_STOP)) {
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
                    if (MiuiFboService.sInstance.isWithinTheTimeInterval() && !MiuiFboService.sInstance.getGlobalSwitch()) {
                        MiuiFboService.sInstance.deliverMessage(MiuiFboService.packageNameList.toString(), 1, 1000L);
                        Slog.d(MiuiFboService.TAG, "execute START_FBO");
                        return;
                    }
                    return;
                case 1:
                    MiuiFboService.sInstance.deliverMessage(MiuiFboService.packageNameList.toString(), 2, 1000L);
                    Slog.d(MiuiFboService.TAG, "execute START_FBO_AGAIN");
                    return;
                case 2:
                    if (intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME) != null && intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME).length() > 0) {
                        MiuiFboService.sInstance.FBO_trigger(intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME));
                        Slog.d(MiuiFboService.TAG, "execute TRANSFERFBOTRIGGER and appList:" + intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME));
                        return;
                    }
                    return;
                case 3:
                    if (MiuiFboService.sInstance.getGlobalSwitch() || MiuiFboService.sInstance.mFboNativeService != null) {
                        MiuiFboService.sInstance.deliverMessage("stop", 3, 1000L);
                        Slog.d(MiuiFboService.TAG, "execute STOP");
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MiuiFboService forSystemServerInitialization(Context context) {
        SystemProperties.set(NATIVE_SERVICE_KEY, FALSE);
        this.mContext = context;
        cldManager = CldManager.getInstance(context);
        mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        IntentFilter intentFilter = new IntentFilter();
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        intentFilter.addAction(MIUI_FBO_RECEIVER_START);
        intentFilter.addAction(MIUI_FBO_RECEIVER_STARTAGAIN);
        intentFilter.addAction(MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER);
        intentFilter.addAction(MIUI_FBO_RECEIVER_STOP);
        this.mContext.registerReceiver(alarmReceiver, intentFilter);
        SystemProperties.set(FBO_START_COUNT, "0");
        return getInstance();
    }

    public static MiuiFboService getInstance() {
        if (sInstance == null) {
            synchronized (MiuiFboService.class) {
                if (sInstance == null) {
                    sInstance = new MiuiFboService();
                }
            }
        }
        return sInstance;
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiFboService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = MiuiFboService.getInstance().forSystemServerInitialization(context);
        }

        public void onStart() {
            publishBinderService(MiuiFboService.SERVICE_NAME, this.mService);
        }
    }

    private MiuiFboService() {
        initFboSocket();
        HandlerThread handlerThread = new HandlerThread(HANDLER_NAME);
        this.mFboServiceThread = handlerThread;
        handlerThread.start();
        this.mFboServiceHandler = new FboServiceHandler(this.mFboServiceThread.getLooper());
        LocalServices.addService(MiuiFboServiceInternal.class, this);
    }

    private static void initFboSocket() {
        Thread thread = new Thread(new Runnable() { // from class: com.miui.server.MiuiFboService.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MiuiFboService.mServerSocket = new LocalServerSocket(MiuiFboService.NATIVE_SOCKET_NAME);
                } catch (IOException e) {
                    e.printStackTrace();
                    MiuiFboService.mKeepRunning = false;
                }
                while (MiuiFboService.mKeepRunning) {
                    try {
                        try {
                            MiuiFboService.interactClientSocket = MiuiFboService.mServerSocket.accept();
                            MiuiFboService.inputStream = MiuiFboService.interactClientSocket.getInputStream();
                            MiuiFboService.outputStream = MiuiFboService.interactClientSocket.getOutputStream();
                            byte[] bytes = new byte[102400];
                            MiuiFboService.inputStream.read(bytes);
                            String dataReceived = new String(bytes);
                            String dataReceived2 = dataReceived.substring(0, dataReceived.indexOf(125));
                            Slog.d(MiuiFboService.TAG, "Receive data from native:" + dataReceived2);
                            if (dataReceived2.contains("pkg:")) {
                                MiuiFboService.mFinishedApp = true;
                                MiuiFboService.sInstance.cldStrategyStatus = true;
                                MiuiFboService.useCldStrategy(1);
                                MiuiFboService.sInstance.cldStrategyStatus = false;
                                MiuiFboService.aggregateBroadcastData(dataReceived2);
                                MiuiFboService.sInstance.deliverMessage(MiuiFboService.packageNameList.toString(), 1, 0L);
                                if (MiuiFboService.listSize < 0) {
                                    MiuiFboService.sInstance.deliverMessage("stop", 3, 0L);
                                }
                                try {
                                    MiuiFboService.inputStream.close();
                                    MiuiFboService.outputStream.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            } else {
                                if (MiuiFboService.sInstance.mDriverSupport) {
                                    String halReturnData = (String) MiuiFboService.callHalFunction(dataReceived2, 2);
                                    String[] split = halReturnData != null ? halReturnData.split(":") : null;
                                    Long beforeCleanup = Long.valueOf(Long.parseLong(split[split.length - 2]));
                                    Long afterCleanup = Long.valueOf(Long.parseLong(split[split.length - 1]));
                                    MiuiFboService.mFragmentCount = Long.valueOf(MiuiFboService.mFragmentCount.longValue() + (beforeCleanup.longValue() - afterCleanup.longValue()));
                                }
                                MiuiFboService.outputStream.write("{send message to native}".getBytes());
                                MiuiFboService.inputStream.close();
                                MiuiFboService.outputStream.close();
                            }
                        } catch (Throwable th) {
                            try {
                                MiuiFboService.inputStream.close();
                                MiuiFboService.outputStream.close();
                            } catch (IOException e3) {
                                e3.printStackTrace();
                            }
                            throw th;
                        }
                    } catch (Exception e4) {
                        e4.printStackTrace();
                        MiuiFboService.writeFailToHal();
                        try {
                            MiuiFboService.inputStream.close();
                            MiuiFboService.outputStream.close();
                        } catch (IOException e5) {
                            e5.printStackTrace();
                        }
                    }
                }
            }
        }, "fboSocketThread");
        thread.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void aggregateBroadcastData(String dataReceived) {
        try {
            synchronized (mLock) {
                String[] splitDataReceived = dataReceived.split(":");
                packageName.add(splitDataReceived[1]);
                Long beforeCleaning = Long.valueOf(Long.parseLong(splitDataReceived[3]));
                Long afterCleaning = Long.valueOf(Long.parseLong(splitDataReceived[4]));
                mFragmentCount = Long.valueOf(mFragmentCount.longValue() + (beforeCleaning.longValue() - afterCleaning.longValue()));
                sInstance.reportFboProcessedBroadcast();
                outputStream.write("{broadcast send success}".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            writeFailToNative();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void writeFailToHal() {
        try {
            OutputStream outputStream2 = outputStream;
            if (outputStream2 != null) {
                outputStream2.write("{fail}".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeFailToNative() {
        try {
            OutputStream outputStream2 = outputStream;
            if (outputStream2 != null) {
                outputStream2.write("{broadcast send fail}".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFboEvent() {
        try {
            try {
                Intent intent = new Intent("onetrack.action.TRACK_EVENT");
                intent.setPackage("com.miui.analytics");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, FBO_EVENT_NAME);
                int startCount = SystemProperties.getInt(FBO_START_COUNT, 0) + 1;
                intent.putExtra("start_count", String.valueOf(startCount));
                SystemProperties.set(FBO_START_COUNT, String.valueOf(startCount));
                if (!mFinishedApp) {
                    intent.putExtra("fragment_count", "Defragmentation of any app has not been completed");
                } else {
                    intent.putExtra("fragment_count", String.valueOf(mFragmentCount));
                }
                intent.putExtra("stop_reason", String.valueOf(mStopReason));
                intent.setFlags(1);
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                Slog.d(TAG, "fbo event report data startCount:" + startCount + ",fragment_count:" + mFragmentCount + ",stopReason:" + mStopReason);
            } catch (Exception e) {
                Slog.e(TAG, "Upload onetrack exception!", e);
            }
        } finally {
            mStopReason.clear();
        }
    }

    private void reportFboProcessedBroadcast() {
        Intent intent = new Intent(MIUI_FBO_PROCESSED_DONE);
        intent.putExtra("resultNumber", mFragmentCount);
        intent.putStringArrayListExtra("resultPkg", packageName);
        this.mContext.sendBroadcast(intent);
        Slog.d(TAG, "sendBroadcast and resultNumber:" + mFragmentCount + "resultPkg" + packageName);
    }

    public boolean FBO_isSupport() {
        try {
            this.mDriverSupport = ((Boolean) callHalFunction(null, 1)).booleanValue();
            Slog.d(TAG, "mDriverSupport = " + this.mDriverSupport);
        } catch (Exception e) {
            Slog.e(TAG, "crash in the isSupportFbo" + e);
        }
        return true;
    }

    public boolean FBO_new_isSupport(String val) {
        try {
            this.mDriverSupport = ((Boolean) callHalFunction(val, 5)).booleanValue();
            Slog.d(TAG, "mDriverSupport = " + this.mDriverSupport);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "crash in the isSupportFbo" + e);
            return true;
        }
    }

    public void FBO_new_trigger(String appList, boolean flag) {
        if (flag) {
            setEnableNightJudgment(false);
        } else {
            setEnableNightJudgment(true);
        }
        sInstance.FBO_trigger(appList);
    }

    public void FBO_trigger(String appList) {
        if (!checkPolicy()) {
            Slog.d(TAG, "Current policy disables fbo functionality");
            return;
        }
        if (getGlobalSwitch() || this.messageHasbeenSent) {
            Slog.d(TAG, "Currently executing fbo or waiting for execution message, so store application list");
            suspendTransferFboTrigger(appList, false, true);
            return;
        }
        if (!isWithinTheTimeInterval() && !getGlobalSwitch()) {
            Slog.d(TAG, "execute suspendTransferFboTrigger()");
            suspendTransferFboTrigger(appList, true, false);
            return;
        }
        if (getUsbState()) {
            Slog.d(TAG, "will not execute fbo service because usbState");
            return;
        }
        this.mFboNativeService = getNativeService();
        try {
            if (this.mFboNativeService != null && isWithinTheTimeInterval() && !getGlobalSwitch()) {
                removePendingIntentList();
                formatAppList(appList);
                listSize = packageNameList.size() - 1;
                Slog.i(TAG, "appList is:" + packageNameList);
                setAlarm(MIUI_FBO_RECEIVER_START, null, SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION, false);
                sendStopMessage();
                this.messageHasbeenSent = true;
                this.screenOnTimes = 0;
            }
        } catch (Exception e) {
            Slog.e(TAG, "crash in the FBO_trigger:" + e);
        }
    }

    public void FBO_notifyFragStatus() {
        try {
            reportFboProcessedBroadcast();
        } catch (Exception e) {
            Slog.e(TAG, "crash in the notifyFragStatus:" + e);
        }
    }

    private static void formatAppList(String appList) {
        packageNameList.clear();
        String[] split = appList.split(",");
        for (String str : split) {
            String[] splitFinal = str.split("\"");
            packageNameList.add(splitFinal[1]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void setAlarm(String action, String appList, long delayTime, boolean record) {
        Intent intent = new Intent();
        intent.setAction(action);
        if (appList != null) {
            intent.putExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME, appList);
        }
        PendingIntent broadcast = PendingIntent.getBroadcast(sInstance.mContext, 0, intent, BroadcastQueueModernStubImpl.FLAG_IMMUTABLE);
        mPendingIntent = broadcast;
        if (record) {
            pendingIntentList.add(broadcast);
        }
        mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + delayTime, mPendingIntent);
    }

    private IFbo getNativeService() {
        IBinder binder;
        SystemProperties.set(NATIVE_SERVICE_KEY, TRUE);
        try {
            Thread.sleep(1000L);
            binder = ServiceManager.getService(CONNECT_NATIVESERVICE_NAME);
            if (binder != null) {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.miui.server.MiuiFboService.2
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        Slog.w(MiuiFboService.TAG, "FboNativeService died; reconnecting");
                        MiuiFboService.this.mFboNativeService = null;
                    }
                }, 0);
            }
        } catch (Exception e) {
            binder = null;
            SystemProperties.set(NATIVE_SERVICE_KEY, FALSE);
        }
        if (binder != null) {
            this.mFboNativeService = IFbo.Stub.asInterface(binder);
            return this.mFboNativeService;
        }
        this.mFboNativeService = null;
        Slog.w(TAG, "IFbo not found; trying again");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Object callHalFunction(String writeData, int halFunctionName) {
        HwParcel hidl_reply = new HwParcel();
        try {
            IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", "default");
            if (hwService != null) {
                HwParcel hidl_request = new HwParcel();
                hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                if (writeData != null) {
                    hidl_request.writeString(writeData);
                }
                hwService.transact(halFunctionName, hidl_request, hidl_reply, 0);
                hidl_reply.verifySuccess();
                hidl_request.releaseTemporaryStorage();
                switch (halFunctionName) {
                    case 1:
                    case 5:
                        return Boolean.valueOf(hidl_reply.readBool());
                    case 2:
                    case 6:
                        return hidl_reply.readString();
                }
            }
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "crash in the callHalFunction:" + e);
            return null;
        } finally {
            hidl_reply.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void userAreaExtend() {
        try {
            String euaInfo = (String) callHalFunction(null, 6);
            if (euaInfo != null) {
                String[] strArray = euaInfo.split(":");
                int ufsPecycle = Integer.parseInt(strArray[0]);
                int supportEua = Integer.parseInt(strArray[1]);
                this.mFboNativeService.EUA_ReserveSpace(ufsPecycle, supportEua);
                if (supportEua == 1) {
                    this.mFboNativeService.EUA_CheckPinFileLba();
                }
                Slog.d(TAG, "ufsPecycle:" + ufsPecycle + ",supportEua:" + supportEua);
            }
        } catch (Exception e) {
            Slog.e(TAG, "crash in the userAreaExtend:" + e);
        }
    }

    private static void suspendTransferFboTrigger(String appList, boolean cancel, boolean record) {
        PendingIntent pendingIntent;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int minuteOfDay = (hour * 60) + minute;
        int startTime = 1440 - minuteOfDay;
        if (cancel && (pendingIntent = mPendingIntent) != null) {
            mAlarmManager.cancel(pendingIntent);
        }
        removePendingIntentList();
        setAlarm(MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER, appList, startTime * 60 * 1000, record);
        Slog.d(TAG, "suspendTransferFboTrigger and suspendTime:" + startTime);
    }

    private static void removePendingIntentList() {
        if (pendingIntentList.size() > 0) {
            for (int i = pendingIntentList.size() - 1; i >= 0; i--) {
                PendingIntent PendingIntent = pendingIntentList.remove(i);
                mAlarmManager.cancel(PendingIntent);
            }
            Slog.d(TAG, "removePendingIntentList");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWithinTheTimeInterval() {
        if (!getEnableNightJudgment()) {
            return true;
        }
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int minuteOfDay = (hour * 60) + minute;
        if (minuteOfDay >= 0 && minuteOfDay <= 300) {
            Slog.d(TAG, "until five in the morning :" + (MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY - minuteOfDay));
            return true;
        }
        return false;
    }

    private void sendStopMessage() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int stopTime = 300 - ((hour * 60) + minute);
        setAlarm(MIUI_FBO_RECEIVER_STOP, null, stopTime * 60 * 1000, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendStopOrContinueToHal(String cmd) {
        if (!this.mDriverSupport) {
            return;
        }
        new HwParcel();
        try {
            callHalFunction(cmd, 3);
            Slog.d(TAG, "sendStopOrContinueToHal:" + cmd);
        } catch (Exception e) {
            Slog.e(TAG, "crash in the sendStopOrContinueToHal:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void useCldStrategy(int val) {
        try {
            if (cldManager.isCldSupported() && sInstance.cldStrategyStatus) {
                cldManager.triggerCld(val);
            }
        } catch (Exception e) {
            Slog.e(TAG, "crash in the useCldStrategy:" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void clearBroadcastData() {
        mFragmentCount = 0L;
        packageName.clear();
    }

    @Override // com.miui.app.MiuiFboServiceInternal
    public void deliverMessage(String data, int number, long sleepTime) {
        Message message = sInstance.mFboServiceHandler.obtainMessage();
        message.what = number;
        message.obj = data;
        sInstance.mFboServiceHandler.sendMessageDelayed(message, sleepTime);
        Slog.d(TAG, "msg.what = " + message.what + "send message time = " + System.currentTimeMillis());
    }

    @Override // com.miui.app.MiuiFboServiceInternal
    public void setBatteryInfos(int batteryStatus, int batteryLevel, int batteryTemperature) {
        if (batteryStatus != this.batteryStatusMark) {
            this.batteryStatusMark = batteryStatus;
            sInstance.deliverMessage("", 6, 10000L);
        }
        if (this.mCurrentBatteryLevel - batteryLevel > 5 && batteryStatus <= 0 && getGlobalSwitch()) {
            deliverMessage("stop", 3, 0L);
        }
        setBatteryStatus(batteryStatus);
        setBatteryLevel(batteryLevel);
        setBatteryTemperature(batteryTemperature);
    }

    @Override // com.miui.app.MiuiFboServiceInternal
    public void setScreenStatus(boolean screenOn) {
        this.screenOnTimes++;
        this.screenOn = screenOn;
    }

    public void setBatteryStatus(int batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setBatteryTemperature(int batteryTemperature) {
        this.batteryTemperature = batteryTemperature;
    }

    public void setNativeIsRunning(boolean nativeIsRunning) {
        this.nativeIsRunning = nativeIsRunning;
    }

    @Override // com.miui.app.MiuiFboServiceInternal
    public boolean getNativeIsRunning() {
        return this.nativeIsRunning;
    }

    public void setGlobalSwitch(boolean globalSwitch) {
        this.globalSwitch = globalSwitch;
    }

    @Override // com.miui.app.MiuiFboServiceInternal
    public boolean getGlobalSwitch() {
        return this.globalSwitch;
    }

    public void setDueToScreenWait(boolean dueToScreenWait) {
        this.dueToScreenWait = dueToScreenWait;
    }

    @Override // com.miui.app.MiuiFboServiceInternal
    public boolean getDueToScreenWait() {
        return this.dueToScreenWait;
    }

    public void setEnableNightJudgment(boolean enableNightJudgment) {
        this.enableNightJudgment = enableNightJudgment;
    }

    public boolean getEnableNightJudgment() {
        return this.enableNightJudgment;
    }

    public void setUsbState(boolean usbState) {
        this.usbState = usbState;
    }

    public boolean getUsbState() {
        return this.usbState;
    }

    public boolean checkPolicy() {
        return SystemProperties.getBoolean(ENABLED_FBO, false);
    }
}
