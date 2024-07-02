package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.miui.AppOpsUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.wm.WindowProcessUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.GreezeManagerInternal;
import com.miui.server.process.ProcessManagerInternal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import miui.content.pm.PreloadedAppPolicy;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.BroadcastEvent;
import miui.os.Build;
import miui.security.AppBehavior;
import miui.security.SecurityManagerInternal;
import miui.security.WakePathChecker;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class BroadcastQueueModernStubImpl implements BroadcastQueueModernStub {
    private static final float ABNORMAL_BROADCAST_RATE = 0.6f;
    private static final String ACTION_C2DM = "com.google.android.c2dm.intent.RECEIVE";
    private static final String ACTION_MIPUSH_MESSAGE_ARRIVED = "com.xiaomi.mipush.MESSAGE_ARRIVED";
    private static final Set<String> ACTION_NFC;
    private static final int ACTIVE_ORDERED_BROADCAST_LIMIT;
    private static ArrayList<BroadcastEvent> BR_LIST = null;
    private static final boolean DEBUG = true;
    private static final Set<String> DEFER_CACHED_WHITE_LIST;
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";
    public static final int FLAG_IMMUTABLE = 67108864;
    private static final boolean IS_STABLE_VERSION;
    private static final int MAX_QUANTITY = 30;
    public static final int OP_PROCESS_OUTGOING_CALLS = 54;
    static final String TAG = "BroadcastQueueInjector";
    private static BroadcastDispatcherFeatureImpl broadcastDispatcherFeature;
    private static volatile BRReportHandler mBRHandler;
    private static ArrayList<BroadcastMap> mBroadcastMap;
    private static long mDispatchThreshold;
    private static int mFinishDeno;
    private static int mIndex;
    private static final List<String> mInternationalSpecialAction;
    private static final Object mObject;
    private static AtomicBoolean sAbnormalBroadcastWarning;
    private static final Set<String> sSpecialSkipAction;
    private static boolean sSystemBootCompleted;
    private static final ArrayList<String> sSystemSkipAction;
    private int mActivityRequestId;
    private ActivityManagerService mAmService;
    private Context mContext;
    private SecurityManagerInternal mSecurityInternal;
    private PowerManager pm = null;

    /* renamed from: -$$Nest$smisSystemBootCompleted, reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m463$$Nest$smisSystemBootCompleted() {
        return isSystemBootCompleted();
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BroadcastQueueModernStubImpl> {

        /* compiled from: BroadcastQueueModernStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BroadcastQueueModernStubImpl INSTANCE = new BroadcastQueueModernStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BroadcastQueueModernStubImpl m465provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BroadcastQueueModernStubImpl m464provideNewInstance() {
            return new BroadcastQueueModernStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        mInternationalSpecialAction = arrayList;
        arrayList.add("android.intent.action.BOOT_COMPLETED");
        arrayList.add("android.intent.action.LOCKED_BOOT_COMPLETED");
        BR_LIST = new ArrayList<>();
        mBroadcastMap = new ArrayList<>();
        mIndex = 0;
        mObject = new Object();
        mDispatchThreshold = SystemProperties.getLong("persist.broadcast.time", 3000L);
        mFinishDeno = SystemProperties.getInt("persist.broadcast.count", 5);
        IS_STABLE_VERSION = Build.IS_STABLE_VERSION;
        ACTIVE_ORDERED_BROADCAST_LIMIT = SystemProperties.getInt("persist.activebr.limit", 1000);
        sAbnormalBroadcastWarning = new AtomicBoolean(false);
        broadcastDispatcherFeature = new BroadcastDispatcherFeatureImpl();
        ArrayList<String> arrayList2 = new ArrayList<>();
        sSystemSkipAction = arrayList2;
        HashSet hashSet = new HashSet();
        sSpecialSkipAction = hashSet;
        HashSet hashSet2 = new HashSet();
        ACTION_NFC = hashSet2;
        HashSet hashSet3 = new HashSet();
        DEFER_CACHED_WHITE_LIST = hashSet3;
        arrayList2.add("android.accounts.LOGIN_ACCOUNTS_PRE_CHANGED");
        arrayList2.add("android.accounts.LOGIN_ACCOUNTS_POST_CHANGED");
        arrayList2.add("android.provider.Telephony.SECRET_CODE");
        arrayList2.add("com.android.updater.action.UPDATE_SUCCESSED");
        arrayList2.add("com.android.updater.action.OTA_UPDATE_SUCCESSED");
        arrayList2.add("miui.media.AUDIO_VOIP_RECORD_STATE_CHANGED_ACTION");
        arrayList2.add("com.android.settings.stylus.STYLUS_STATE_SOC");
        arrayList2.add("com.android.settings.stylus.STYLUS_BATTERY_NOTIFY");
        arrayList2.add("com.android.settings.stylus.STYLUS_PLACE_ERROR");
        arrayList2.add("com.xiaomi.bluetooth.action.KEYBOARD_ATTACH");
        if (FeatureParser.getBoolean("is_pad", false)) {
            arrayList2.add("miui.intent.action.ACTION_POGO_CONNECTED_STATE");
        }
        hashSet.add("android.intent.action.MEDIA_BUTTON");
        hashSet.add("android.appwidget.action.APPWIDGET_ENABLED");
        hashSet.add("android.appwidget.action.APPWIDGET_DISABLED");
        hashSet.add("android.appwidget.action.APPWIDGET_UPDATE");
        hashSet.add("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS");
        hashSet.add("android.appwidget.action.APPWIDGET_DELETED");
        hashSet.add("android.appwidget.action.APPWIDGET_RESTORED");
        hashSet.add("miui.intent.action.contentcatcher");
        hashSet.add("com.miui.contentextension.action.PACKAGE");
        hashSet2.add("com.miui.nfc.action.TRANSACTION");
        hashSet2.add("com.miui.intent.action.SWIPE_CARD");
        hashSet2.add("com.miui.nfc.action.RF_ON");
        hashSet2.add("com.miui.intent.action.DOUBLE_CLICK");
        hashSet2.add("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED");
        hashSet2.add("android.nfc.action.TRANSACTION_DETECTED");
        hashSet3.add("com.xiaomi.market");
        hashSet3.add("com.xiaomi.mipicks");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BroadcastQueueModernStubImpl getInstance() {
        return (BroadcastQueueModernStubImpl) BroadcastQueueModernStub.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(ActivityManagerService service, Context context) {
        this.mAmService = service;
        this.mContext = context;
    }

    /* loaded from: classes.dex */
    private static class BroadcastMap {
        private String action;
        private String packageName;

        public BroadcastMap(String action, String packageName) {
            this.action = action;
            this.packageName = packageName;
        }

        public boolean equals(Object obj) {
            if (obj instanceof BroadcastMap) {
                BroadcastMap broadcastMapObj = (BroadcastMap) obj;
                return this.action.equals(broadcastMapObj.action) && this.packageName.equals(broadcastMapObj.packageName);
            }
            return super.equals(obj);
        }

        public String toString() {
            String result = "action: " + this.action + ", packageName: " + this.packageName;
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class BRReportHandler extends Handler {
        static final int BROADCAST_RECORDS = 1;
        static final int BROADCAST_TIME_RECORDS = 0;

        public BRReportHandler(Looper looper) {
            super(looper, null);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    try {
                        ParceledListSlice<BroadcastEvent> reportEvents = (ParceledListSlice) msg.obj;
                        Slog.d(BroadcastQueueModernStubImpl.TAG, "reporting BROADCAST_RECORDS : " + BroadcastQueueModernStubImpl.m463$$Nest$smisSystemBootCompleted());
                        if (reportEvents != null && BroadcastQueueModernStubImpl.m463$$Nest$smisSystemBootCompleted()) {
                            MQSEventManagerDelegate.getInstance().reportBroadcastEvent(reportEvents);
                            return;
                        }
                        return;
                    } catch (Exception e) {
                        Slog.e(BroadcastQueueModernStubImpl.TAG, "report message record error.", e);
                        return;
                    }
                default:
                    Slog.w(BroadcastQueueModernStubImpl.TAG, "wrong message received of BRReportHandler");
                    return;
            }
        }
    }

    private static void notifyPowerKeeperEvent(int state, int pid, int uid, String action) {
    }

    private boolean checkSpecialAction(ProcessRecord app, String action) {
        if (action != null) {
            if (sSpecialSkipAction.contains(action) || ACTION_NFC.contains(action) || "com.xiaomi.mipush.RECEIVE_MESSAGE".equals(action)) {
                notifyPowerKeeperEvent(1, app.getPid(), app.uid, action);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean checkReceiverAppDealBroadcast(BroadcastQueue bq, BroadcastRecord r, ProcessRecord app, boolean isStatic) {
        String callee;
        String className;
        if (app != null && r != null) {
            if (r.intent != null) {
                String action = r.intent.getAction();
                String callee2 = "";
                String className2 = "";
                Intent intent = r.intent;
                if (intent.getComponent() != null) {
                    className2 = intent.getComponent().getClassName();
                    callee2 = intent.getComponent().getPackageName();
                }
                if (app.info != null && TextUtils.isEmpty(callee2)) {
                    String callee3 = app.info.packageName;
                    callee = callee3;
                } else {
                    callee = callee2;
                }
                if (r.curReceiver != null && TextUtils.isEmpty(className2)) {
                    String className3 = r.curReceiver.name;
                    className = className3;
                } else {
                    className = className2;
                }
                if (!TextUtils.equals(callee, r.callerPackage)) {
                    if (WakePathChecker.getInstance().calleeAliveMatchBlackRule(action, className, r.callerPackage, callee, r.userId, 8192, true)) {
                        return false;
                    }
                }
                if (r.callerApp != null && r.callerApp.uid == 1027 && ACTION_NFC.contains(action) && !TextUtils.isEmpty(r.intent.getPackage()) && !PendingIntentRecordImpl.containsPendingIntent(r.intent.getPackage())) {
                    PendingIntentRecordImpl.exemptTemporarily(r.intent.getPackage(), true);
                    Slog.i(TAG, "MIUILOG- Allow NFC start appliction " + r.intent.getPackage() + " background start");
                }
                if (GreezeManagerInternal.getInstance() == null) {
                    return true;
                }
                checkSpecialAction(app, r.intent.getAction());
                return true;
            }
        }
        return true;
    }

    public boolean checkApplicationAutoStart(BroadcastQueue bq, BroadcastRecord r, ResolveInfo info) {
        String reason;
        if ((Build.IS_INTERNATIONAL_BUILD && info != null && info.activityInfo != null && info.activityInfo.applicationInfo != null && info.activityInfo.applicationInfo.packageName != null && ProcessManagerInternal.checkCtsProcess(info.activityInfo.applicationInfo.packageName)) || AppOpsUtils.isXOptMode()) {
            return true;
        }
        String action = r.intent.getAction();
        if (Build.IS_INTERNATIONAL_BUILD && ACTION_C2DM.equals(action)) {
            return true;
        }
        if (r.callerApp != null && r.callerPackage != null && !ActivityManagerServiceImpl.WIDGET_PROVIDER_WHITE_LIST.contains(r.callerPackage)) {
            String widgetProcessName = r.callerPackage + ":widgetProvider";
            if (widgetProcessName.equals(r.callerApp.processName)) {
                Slog.i(TAG, "MIUILOG- Reject widget call from " + r.callerPackage);
                return false;
            }
        }
        if (r.callerApp != null && r.callerApp.uid == 1027 && ACTION_NFC.contains(action) && !TextUtils.isEmpty(r.intent.getPackage())) {
            if (!PendingIntentRecordImpl.containsPendingIntent(r.intent.getPackage())) {
                PendingIntentRecordImpl.exemptTemporarily(r.intent.getPackage(), true);
            }
            Slog.i(TAG, "MIUILOG- Allow NFC start appliction " + r.intent.getPackage() + " action: " + action);
            return true;
        }
        if (WakePathChecker.getInstance().checkBroadcastWakePath(r.intent, r.callerPackage, r.callerApp != null ? r.callerApp.info : null, info, r.userId)) {
            boolean isSystem = (info.activityInfo.applicationInfo.flags & 1) != 0 || PreloadedAppPolicy.isProtectedDataApp(info.activityInfo.applicationInfo.packageName);
            boolean isMessageArrived = ACTION_MIPUSH_MESSAGE_ARRIVED.equals(action);
            if (r.intent.getComponent() == null && ((isMessageArrived || !isSystem || r.intent.getPackage() == null) && (!isSystem || !sSystemSkipAction.contains(action)))) {
                if (!shouldStopBroadcastDispatch(info, r.intent.getPackage() == null, r.intent) && !isInternationalSpecialAction(action, info)) {
                    reason = " auto start";
                }
            }
            return true;
        }
        reason = " wake path";
        if (info != null) {
            Slog.w(TAG, "Unable to launch app " + info.activityInfo.applicationInfo.packageName + "/" + info.activityInfo.applicationInfo.uid + " for broadcast " + r.intent + ": process is not permitted to " + reason);
        }
        return false;
    }

    private boolean isInternationalSpecialAction(String action, ResolveInfo info) {
        if (!Build.IS_INTERNATIONAL_BUILD || !mInternationalSpecialAction.contains(action)) {
            return false;
        }
        int autoStartMode = AppOpsUtils.getApplicationSpecialBroadcast(this.mContext, info.activityInfo.applicationInfo.packageName, info.activityInfo.applicationInfo.uid, "BroadcastQueueImpl#specifyBroadcast");
        return autoStartMode == 0;
    }

    private boolean shouldStopBroadcastDispatch(ResolveInfo info, boolean note, Intent callee) {
        SecurityManagerInternal securityManagerInternal;
        boolean isRunning = WindowProcessUtils.isPackageRunning(this.mAmService.mActivityTaskManager, info.activityInfo.applicationInfo.packageName, info.activityInfo.processName, info.activityInfo.applicationInfo.uid);
        if (isRunning) {
            return true;
        }
        int autoStartMode = AppOpsUtils.noteApplicationAutoStart(this.mContext, info.activityInfo.applicationInfo.packageName, info.activityInfo.applicationInfo.uid, note ? "BroadcastQueueModernStubImpl#checkApplicationAutoStart#" : "BroadcastQueueModernStubImpl#specifyBroadcast");
        if (this.mSecurityInternal == null) {
            this.mSecurityInternal = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
        }
        if (callee != null && (securityManagerInternal = this.mSecurityInternal) != null) {
            securityManagerInternal.recordAppBehaviorAsync(6, info.activityInfo.applicationInfo.packageName, 1L, AppBehavior.parseIntent(callee));
        }
        return autoStartMode == 0;
    }

    public boolean checkSkipReceiver(BroadcastQueue bq, BroadcastProcessQueue queue, BroadcastRecord r, ProcessRecord app, boolean isStatic, Object info) {
        if (queue.isProcessWarm()) {
            return !checkReceiverAppDealBroadcast(bq, r, app, isStatic);
        }
        if (info instanceof ResolveInfo) {
            return !checkApplicationAutoStart(bq, r, (ResolveInfo) info);
        }
        return false;
    }

    public boolean isSkip(BroadcastRecord r, ResolveInfo info, int appOp) {
        return isSKipNotifySms(r, info.activityInfo.applicationInfo.uid, info.activityInfo.packageName, appOp);
    }

    public boolean isSkip(BroadcastRecord r, BroadcastFilter filter, int appOp) {
        return isSKipNotifySms(r, filter.receiverList.uid, filter.packageName, appOp);
    }

    boolean isSKipNotifySms(BroadcastRecord r, int uid, String packageName, int appOp) {
        if (appOp != 16) {
            return false;
        }
        Intent intent = r.intent;
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return false;
        }
        try {
            if (intent.getBooleanExtra("miui.intent.SERVICE_NUMBER", false)) {
                int mode = this.mAmService.mAppOpsService.checkOperation(10018, uid, packageName);
                if (mode != 0) {
                    Slog.w(TAG, "MIUILOG- Sms Filter packageName : " + packageName + " uid " + uid);
                    return true;
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "isSKipNotifySms", e);
        }
        return false;
    }

    boolean isSkipForUser(ResolveInfo info, boolean skip) {
        int userId = UserHandle.getUserId(info.activityInfo.applicationInfo.uid);
        if (!this.mAmService.isUserRunning(userId, 0)) {
            return true;
        }
        return skip;
    }

    static Handler getBRReportHandler() {
        if (mBRHandler == null) {
            synchronized (mObject) {
                if (mBRHandler == null) {
                    HandlerThread mBRThread = new HandlerThread("brreport-thread");
                    mBRThread.start();
                    mBRHandler = new BRReportHandler(mBRThread.getLooper());
                }
            }
        }
        return mBRHandler;
    }

    private static boolean isSystemBootCompleted() {
        if (!sSystemBootCompleted) {
            sSystemBootCompleted = "1".equals(SystemProperties.get("sys.boot_completed"));
        }
        return sSystemBootCompleted;
    }

    /* JADX WARN: Removed duplicated region for block: B:30:0x009b  */
    /* JADX WARN: Removed duplicated region for block: B:33:0x00dc  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    static void onBroadcastFinished(android.content.Intent r20, java.lang.String r21, int r22, long r23, long r25, long r27, long r29, int r31) {
        /*
            Method dump skipped, instructions count: 275
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.BroadcastQueueModernStubImpl.onBroadcastFinished(android.content.Intent, java.lang.String, int, long, long, long, long, int):void");
    }

    public int noteOperationLocked(int appOp, final int uid, String packageName, Handler handler, BroadcastRecord receiverRecord) {
        int mode = this.mAmService.mAppOpsService.checkOperation(appOp, uid, packageName);
        if (mode != 5) {
            return mode;
        }
        int userId = UserHandle.getUserId(uid);
        if (userId == 999) {
            return mode;
        }
        if (isSKipNotifySms(receiverRecord, uid, packageName, appOp)) {
            return mode;
        }
        this.mAmService.mAppOpsService.setMode(appOp, uid, packageName, 1);
        final Intent intent = new Intent("com.miui.intent.action.REQUEST_PERMISSIONS");
        intent.setPackage("com.lbe.security.miui");
        intent.addFlags(411041792);
        intent.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
        intent.putExtra("android.intent.extra.UID", uid);
        intent.putExtra("op", appOp);
        if (!receiverRecord.sticky) {
            String callerPackage = receiverRecord.callerPackage;
            int callingUid = receiverRecord.callingUid;
            if (callerPackage == null) {
                if (callingUid == 0) {
                    callerPackage = "root";
                } else if (callingUid == 2000) {
                    callerPackage = "com.android.shell";
                } else if (callingUid == 1000) {
                    callerPackage = "android";
                }
            }
            if (callerPackage == null) {
                return mode;
            }
            int requestCode = getNextRequestIdLocked();
            Intent intentNew = new Intent(receiverRecord.intent);
            intentNew.setPackage(packageName);
            intent.putExtra("android.intent.extra.INTENT", new IntentSender(this.mAmService.mPendingIntentController.getIntentSender(1, callerPackage, (String) null, callingUid, receiverRecord.userId, (IBinder) null, (String) null, requestCode, new Intent[]{intentNew}, new String[]{intentNew.resolveType(this.mContext.getContentResolver())}, 1275068416, (Bundle) null)));
        }
        Slog.i(TAG, "MIUILOG - Launching Request permission [Broadcast] uid : " + uid + "  pkg : " + packageName + " op : " + appOp);
        long delay = appOp == 54 ? 1500L : 10L;
        handler.postDelayed(new Runnable() { // from class: com.android.server.am.BroadcastQueueModernStubImpl.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    BroadcastQueueModernStubImpl.this.mContext.startActivityAsUser(intent, new UserHandle(UserHandle.getUserId(uid)));
                } catch (Exception e) {
                }
            }
        }, delay);
        return 1;
    }

    private int getNextRequestIdLocked() {
        if (this.mActivityRequestId >= Integer.MAX_VALUE) {
            this.mActivityRequestId = 0;
        }
        int i = this.mActivityRequestId + 1;
        this.mActivityRequestId = i;
        return i;
    }

    private String getPackageLabelLocked(AbnormalBroadcastRecord r) {
        CharSequence labelChar;
        String label = null;
        ProcessRecord app = getProcessRecordLocked(r.callerPackage, r.userId);
        if (app != null && app.getPkgList().size() == 1 && (labelChar = this.mContext.getPackageManager().getApplicationLabel(app.info)) != null) {
            label = labelChar.toString();
        }
        if (label == null) {
            return r.callerPackage;
        }
        return label;
    }

    private ProcessRecord getProcessRecordLocked(String processName, int userId) {
        for (int i = this.mAmService.mProcessList.getLruSizeLOSP() - 1; i >= 0; i--) {
            ProcessRecord app = (ProcessRecord) this.mAmService.mProcessList.getLruProcessesLOSP().get(i);
            if (app.getThread() != null && app.processName.equals(processName) && app.userId == userId) {
                return app;
            }
        }
        return null;
    }

    private void processAbnormalBroadcast(AbnormalBroadcastRecord r, String packageLabel, int count) {
        if (count < ACTIVE_ORDERED_BROADCAST_LIMIT * 3) {
            Slog.d(TAG, "abnormal ordered broadcast, showWarningDialog");
        }
        if (0 == 0) {
            forceStopAbnormalApp(r);
            sAbnormalBroadcastWarning.set(false);
        }
    }

    private void forceStopAbnormalApp(AbnormalBroadcastRecord r) {
        synchronized (this.mAmService) {
            Slog.d(TAG, "force-stop abnormal app:" + r.callerPackage + " userId:" + r.userId);
            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).forceStopPackage(r.callerPackage, r.userId, "abnormal ordered broadcast");
        }
    }

    private AbnormalBroadcastRecord getAbnormalBroadcastByRateIfExists(List<BroadcastRecord> broadcasts) {
        long startTime = SystemClock.uptimeMillis();
        BroadcastRecord result = broadcasts.get(0);
        int count = 1;
        for (int i = 1; i < broadcasts.size(); i++) {
            if (count == 0) {
                BroadcastRecord result2 = broadcasts.get(i);
                result = result2;
                count = 1;
            } else if (TextUtils.equals(result.intent.getAction(), broadcasts.get(i).intent.getAction()) && TextUtils.equals(result.callerPackage, broadcasts.get(i).callerPackage) && result.userId == broadcasts.get(i).userId) {
                count++;
            } else {
                count--;
            }
        }
        if (TextUtils.isEmpty(result.callerPackage) || TextUtils.equals(result.callerPackage, "android") || TextUtils.equals(result.callerPackage, "com.google.android.gms") || count < broadcasts.size() * 0.20000005f) {
            Slog.d(TAG, "abnormal broadcast not found with first loop count:" + count + " with caller:" + result);
            return null;
        }
        int count2 = 0;
        for (BroadcastRecord r : broadcasts) {
            if (TextUtils.equals(result.intent.getAction(), r.intent.getAction()) && TextUtils.equals(result.callerPackage, r.callerPackage)) {
                count2++;
            }
        }
        if (count2 < broadcasts.size() * 0.6f) {
            Slog.d(TAG, "abnormal broadcast not found with count:" + count2);
            return null;
        }
        Slog.d(TAG, "found abnormal broadcast in list by rate:" + result + " cost:" + (SystemClock.uptimeMillis() - startTime) + " ms");
        return new AbnormalBroadcastRecord(result);
    }

    private AbnormalBroadcastRecord getAbnormalBroadcastByCountIfExisted(List<BroadcastRecord> broadcasts) {
        long startTime = SystemClock.uptimeMillis();
        Map<AbnormalBroadcastRecord, Integer> abnormalBroadcastMap = new HashMap<>();
        for (BroadcastRecord r : broadcasts) {
            if (!TextUtils.equals("android", r.callerPackage) && !TextUtils.equals("com.google.android.gms", r.callerPackage)) {
                AbnormalBroadcastRecord abnormalRecord = new AbnormalBroadcastRecord(r);
                Integer count = abnormalBroadcastMap.get(abnormalRecord);
                if (count != null) {
                    abnormalBroadcastMap.put(abnormalRecord, Integer.valueOf(count.intValue() + 1));
                } else {
                    abnormalBroadcastMap.put(abnormalRecord, 1);
                }
            }
        }
        AbnormalBroadcastRecord recordWithMaxCount = null;
        int maxCount = 0;
        for (Map.Entry<AbnormalBroadcastRecord, Integer> entry : abnormalBroadcastMap.entrySet()) {
            if (entry.getValue().intValue() > maxCount) {
                AbnormalBroadcastRecord recordWithMaxCount2 = entry.getKey();
                recordWithMaxCount = recordWithMaxCount2;
                maxCount = entry.getValue().intValue();
            }
        }
        if (recordWithMaxCount == null || TextUtils.isEmpty(recordWithMaxCount.callerPackage) || maxCount < ACTIVE_ORDERED_BROADCAST_LIMIT) {
            Slog.d(TAG, "the max number of same broadcasts in queue is not large enough:" + recordWithMaxCount + " with count:" + maxCount);
            return null;
        }
        Slog.d(TAG, "found abnormal broadcast in list by max count:" + recordWithMaxCount + " cost:" + (SystemClock.uptimeMillis() - startTime) + " ms");
        return recordWithMaxCount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AbnormalBroadcastRecord {
        String action;
        String callerPackage;
        int userId;

        AbnormalBroadcastRecord(BroadcastRecord r) {
            this.action = r.intent.getAction();
            this.callerPackage = r.callerPackage;
            this.userId = r.userId;
        }

        public boolean equals(Object obj) {
            if (obj instanceof AbnormalBroadcastRecord) {
                AbnormalBroadcastRecord r = (AbnormalBroadcastRecord) obj;
                return TextUtils.equals(this.action, r.action) && TextUtils.equals(this.callerPackage, r.callerPackage) && this.userId == r.userId;
            }
            return super.equals(obj);
        }

        public int hashCode() {
            int i = 1 * 31;
            String str = this.action;
            int hashCode = i + (str == null ? 0 : str.hashCode());
            int hashCode2 = hashCode * 31;
            String str2 = this.callerPackage;
            return ((hashCode2 + (str2 != null ? str2.hashCode() : 0)) * 31) + Integer.valueOf(this.userId).hashCode();
        }

        public String toString() {
            return "AbnormalBroadcastRecord{action='" + this.action + "', callerPackage='" + this.callerPackage + "', userId=" + this.userId + '}';
        }
    }

    public static void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > 3000) {
            Slog.w(TAG, "Slow operation: processNextBroadcast " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    public void notifyFinishBroadcast(BroadcastRecord r, ProcessRecord app) {
        if (r.ordered) {
            String action = r.intent.getAction();
            int uid = -1;
            if (app != null) {
                uid = app.uid;
            }
            boolean isforeground = (r.intent.getFlags() & 268435456) != 0;
            GreezeManagerInternal.getInstance().updateOrderBCStatus(action, uid, isforeground, false);
        }
    }

    public boolean checkReceiverIfRestricted(BroadcastQueue bq, BroadcastRecord r, ProcessRecord app, boolean isStatic) {
        return (GreezeManagerInternal.getInstance() == null || app == null || r == null || r.intent == null || !GreezeManagerInternal.getInstance().isRestrictReceiver(r.intent, r.callingUid, r.callerPackage, app.uid, app.processName)) ? false : true;
    }

    public boolean isInDeferCachedWhiteList(String processName, BroadcastRecord r) {
        if (TextUtils.isEmpty(processName) || r == null || !DEFER_CACHED_WHITE_LIST.contains(processName)) {
            return false;
        }
        return r.callingPid == Process.myPid() || TextUtils.equals("android", r.callerPackage);
    }
}
