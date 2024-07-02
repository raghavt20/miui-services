package com.android.server.net;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import miui.process.IActivityChangeListener;
import miui.process.ProcessManager;
import miui.telephony.TelephonyManagerEx;
import miui.util.FeatureParser;

/* loaded from: classes.dex */
public class MiuiNetworkPolicyQosUtils {
    private static final String ACTION_CLOUD_MEETING_LIST_DONE = "com.android.phone.intent.action.CLOUD_MEETING_LIST_DONE";
    private static final String ACTIVITY_STATUS = "com.android.phone.intent.action.ACTIVITY_STATUS";
    private static final String AN_WECHAT_SCAN_UI = "com.tencent.mm.plugin.scanner.ui.BaseScanUI";
    private static final boolean DEBUG = true;
    private static final int DSCP_DEFAULT_STATE = 0;
    private static final int DSCP_INIT_STATE = 1;
    private static final int DSCP_QOS_STATE = 2;
    private static final int DSCP_TOS_EF = 184;
    private static final String EF_AN_TIKTOK = "com.ss.android.ugc.aweme.main.MainActivity";
    private static final String EF_AN_WECHAT = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    private static final String[] EF_MIMO_AN;
    private static final String[] EF_MIMO_PN;
    private static final String EF_PN_TIKTOK = "com.ss.android.ugc.aweme";
    private static final String EF_PN_WECHAT = "com.tencent.mm";
    private static final String GAME_ENABLED = "gameEnabled";
    private static final String GAME_SCENE = "com.android.phone.intent.action.GAME_SCENE";
    private static final int IP;
    public static final boolean IS_QCOM;
    private static final String LATENCY_ACTION_CHANGE_LEVEL = "com.android.phone.intent.action.CHANGE_LEVEL";
    private static final String LATENCY_KEY_SCENE = "scene";
    private static final String[] LOCAL_EF_APP_LIST = {"com.tencent.tmgp.sgame", "com.tencent.tmgp.pubgmhd", "com.tencent.lolm"};
    private static final String[] MEETING_OPTIMIZATION_WHITE_LIST_DEFAULT;
    private static final String MEETING_VOIP_ENABLED = "meetingVoipEnabled";
    private static final String MEETING_VOIP_SCENE = "com.android.phone.intent.action.MEETING_VOIP_SCENE";
    private static final String MIMO_SCENE_EXTRA = "mimo_scene";
    private static final String NOTIFACATION_RECEIVER_PACKAGE = "com.android.phone";
    private static final int SCENE_DEFAULT = -1;
    private static final int SCENE_VOIP = 3;
    private static final int SCENE_WECHAT_SCAN = 1;
    private static final String TAG = "MiuiNetworkPolicyQosUtils";
    private static final int UDP;
    private static final String WECHAT_VOIP_ENABLED = "wechatVoipEnabled";
    private static final String WECHAT_VOIP_SCENE = "com.android.phone.intent.action.WECHAT_VOIP_SCENE";
    private static final ArrayList<ActivityStatusScene> mActivityStatusScene;
    private static List<String> mMeetingAppsAN;
    private static List<String> mMeetingAppsPN;
    private final Context mContext;
    private int mDscpState;
    private Set<String> mEFAppsPN;
    private final Handler mHandler;
    private MiuiNetworkManagementService mNetMgrService;
    private QosInfo mQosInfo;
    private ConcurrentHashMap<String, Integer> mUidMap = new ConcurrentHashMap<>();
    private boolean mIsMobileDataOn = false;
    private IActivityChangeListener.Stub mActivityChangeListener = new IActivityChangeListener.Stub() { // from class: com.android.server.net.MiuiNetworkPolicyQosUtils.1
        public void onActivityChanged(ComponentName preName, ComponentName curName) {
            Log.i(MiuiNetworkPolicyQosUtils.TAG, "onActivityChanged pre:" + preName + ",cur:" + curName + ",data=" + MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn);
            if (preName == null || curName == null) {
                return;
            }
            String cur = curName.toString();
            String pre = preName.toString();
            int size = MiuiNetworkPolicyQosUtils.mMeetingAppsAN.size();
            for (int i = 0; i < size; i++) {
                if (!TextUtils.isEmpty(cur) && cur.contains((CharSequence) MiuiNetworkPolicyQosUtils.mMeetingAppsAN.get(i)) && MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn) {
                    Log.i(MiuiNetworkPolicyQosUtils.TAG, "onActivityChanged setQos true");
                    MiuiNetworkPolicyQosUtils.this.updateVoipStatus(cur, true, i);
                    MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils = MiuiNetworkPolicyQosUtils.this;
                    miuiNetworkPolicyQosUtils.setQos(true, miuiNetworkPolicyQosUtils.getUidFromMap((String) MiuiNetworkPolicyQosUtils.mMeetingAppsPN.get(i)), MiuiNetworkPolicyQosUtils.UDP);
                } else if (!TextUtils.isEmpty(pre) && pre.contains((CharSequence) MiuiNetworkPolicyQosUtils.mMeetingAppsAN.get(i))) {
                    Log.i(MiuiNetworkPolicyQosUtils.TAG, "onActivityChanged setQos false");
                    MiuiNetworkPolicyQosUtils.this.updateVoipStatus(pre, false, i);
                    MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils2 = MiuiNetworkPolicyQosUtils.this;
                    miuiNetworkPolicyQosUtils2.setQos(false, miuiNetworkPolicyQosUtils2.getUidFromMap((String) MiuiNetworkPolicyQosUtils.mMeetingAppsPN.get(i)), MiuiNetworkPolicyQosUtils.UDP);
                }
            }
            if (!TextUtils.isEmpty(cur) && cur.contains(MiuiNetworkPolicyQosUtils.AN_WECHAT_SCAN_UI) && MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn) {
                Log.i(MiuiNetworkPolicyQosUtils.TAG, "onActivityChanged enter ScanQR");
                MiuiNetworkPolicyQosUtils.this.enableLLMMode(true, 1);
            } else if (!TextUtils.isEmpty(pre) && pre.contains(MiuiNetworkPolicyQosUtils.AN_WECHAT_SCAN_UI)) {
                Log.i(MiuiNetworkPolicyQosUtils.TAG, "onActivityChanged exit ScanQR");
                MiuiNetworkPolicyQosUtils.this.enableLLMMode(false, -1);
            }
            Intent intent = new Intent();
            intent.setAction(MiuiNetworkPolicyQosUtils.ACTIVITY_STATUS);
            intent.setPackage(MiuiNetworkPolicyQosUtils.NOTIFACATION_RECEIVER_PACKAGE);
            boolean changed = false;
            Iterator it = MiuiNetworkPolicyQosUtils.mActivityStatusScene.iterator();
            while (it.hasNext()) {
                ActivityStatusScene as = (ActivityStatusScene) it.next();
                if (!TextUtils.isEmpty(cur) && MiuiNetworkPolicyQosUtils.this.isActivityNameContains(cur, as.EF_AN) && MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn) {
                    intent.putExtra(as.sceneStatusExtra, true);
                    changed = true;
                } else if (!TextUtils.isEmpty(pre) && MiuiNetworkPolicyQosUtils.this.isActivityNameContains(pre, as.EF_AN)) {
                    intent.putExtra(as.sceneStatusExtra, false);
                    changed = true;
                }
            }
            if (changed) {
                MiuiNetworkPolicyQosUtils.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
        }
    };
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyQosUtils.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            Log.i(MiuiNetworkPolicyQosUtils.TAG, "receive action" + action);
            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                NetworkInfo nwInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (nwInfo != null && nwInfo.getType() == 0) {
                    if (nwInfo.getState() == NetworkInfo.State.CONNECTED && !MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn) {
                        MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn = true;
                        Log.i(MiuiNetworkPolicyQosUtils.TAG, "mIsMobileDataOn=" + MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn);
                        return;
                    } else {
                        if (nwInfo.getState() == NetworkInfo.State.DISCONNECTED && MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn) {
                            MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn = false;
                            Log.i(MiuiNetworkPolicyQosUtils.TAG, "mIsMobileDataOn=" + MiuiNetworkPolicyQosUtils.this.mIsMobileDataOn);
                            if (MiuiNetworkPolicyQosUtils.this.mQosInfo != null && MiuiNetworkPolicyQosUtils.this.mQosInfo.getQosStatus()) {
                                MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils = MiuiNetworkPolicyQosUtils.this;
                                miuiNetworkPolicyQosUtils.setQos(false, miuiNetworkPolicyQosUtils.mQosInfo.getUid(), MiuiNetworkPolicyQosUtils.this.mQosInfo.getProtocol());
                                return;
                            }
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            if (action.equals(MiuiNetworkPolicyQosUtils.ACTION_CLOUD_MEETING_LIST_DONE)) {
                MiuiNetworkPolicyQosUtils.this.processMeetingListChanged();
            }
        }
    };

    static {
        String[] strArr = {EF_PN_WECHAT, EF_PN_TIKTOK};
        EF_MIMO_PN = strArr;
        String[] strArr2 = {EF_AN_WECHAT, EF_AN_TIKTOK};
        EF_MIMO_AN = strArr2;
        IP = OsConstants.IPPROTO_IP;
        UDP = OsConstants.IPPROTO_UDP;
        IS_QCOM = "qcom".equals(FeatureParser.getString("vendor"));
        ArrayList<ActivityStatusScene> arrayList = new ArrayList<>();
        mActivityStatusScene = arrayList;
        arrayList.add(new ActivityStatusScene(strArr, strArr2, MIMO_SCENE_EXTRA));
        mMeetingAppsPN = new ArrayList();
        mMeetingAppsAN = new ArrayList();
        MEETING_OPTIMIZATION_WHITE_LIST_DEFAULT = new String[]{EF_PN_WECHAT, EF_AN_WECHAT, "com.tencent.wemeet.app", "com.tencent.wemeet.sdk.meeting.inmeeting.InMeetingActivity", "com.ss.android.lark", "com.ss.android.vc.meeting.module.multi.ByteRTCMeetingActivityInstance", "com.alibaba.android.rimet", "com.alibaba.android.teleconf.mozi.activity.TeleVideoConfActivity", "com.ss.android.lark.kami", "com.ss.android.vc.meeting.module.multi.ByteRTCMeetingActivity"};
    }

    /* loaded from: classes.dex */
    public static class ActivityStatusScene {
        String[] EF_AN;
        String[] EF_PN;
        String sceneStatusExtra;

        public ActivityStatusScene(String[] efPn, String[] efAn, String sceneExtra) {
            this.EF_PN = efPn;
            this.EF_AN = efAn;
            this.sceneStatusExtra = sceneExtra;
        }
    }

    public MiuiNetworkPolicyQosUtils(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        setDscpStatus(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isActivityNameContains(String anString, String[] sceneString) {
        for (String str : sceneString) {
            if (anString.contains(str)) {
                return true;
            }
        }
        return false;
    }

    public void systemReady(MiuiNetworkManagementService networkMgr) {
        this.mNetMgrService = networkMgr;
        initMeetingWhiteList(this.mContext);
        enableQos(false);
        registerActivityChangeListener();
        enableQos(true);
        updateQosUid();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction(ACTION_CLOUD_MEETING_LIST_DONE);
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        this.mQosInfo = new QosInfo();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initMeetingWhiteList(Context context) {
        if (mMeetingAppsPN.size() > 0 || mMeetingAppsAN.size() > 0) {
            mMeetingAppsPN.clear();
            mMeetingAppsAN.clear();
        }
        String pkgNames = Settings.Global.getString(context.getContentResolver(), "meeting_optimization_white_list_pkg_name");
        if (TextUtils.isEmpty(pkgNames)) {
            initDefaultMeetingWhiteList();
            pkgNames = Settings.Global.getString(context.getContentResolver(), "meeting_optimization_white_list_pkg_name");
        }
        Log.i(TAG, "initMeetingWhiteList pkgNames=" + pkgNames);
        String[] meetings = pkgNames.split(",");
        for (int i = 0; i <= meetings.length - 1; i++) {
            if (i % 2 == 0) {
                mMeetingAppsPN.add(meetings[i]);
            } else {
                mMeetingAppsAN.add(meetings[i]);
            }
        }
    }

    private void initDefaultMeetingWhiteList() {
        List<String> defaultName = Arrays.asList(MEETING_OPTIMIZATION_WHITE_LIST_DEFAULT);
        if (defaultName != null && defaultName.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean flag = false;
            for (String str : defaultName) {
                if (flag) {
                    sb.append(",");
                } else {
                    flag = true;
                }
                sb.append(str);
            }
            Settings.Global.putString(this.mContext.getContentResolver(), "meeting_optimization_white_list_pkg_name", sb.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerActivityChangeListener() {
        if (this.mActivityChangeListener != null) {
            List<String> targetActivities = new ArrayList<>();
            List<String> targetPackages = new ArrayList<>();
            Iterator<ActivityStatusScene> it = mActivityStatusScene.iterator();
            while (it.hasNext()) {
                ActivityStatusScene as = it.next();
                for (int i = 0; i < as.EF_PN.length; i++) {
                    if (!targetPackages.contains(as.EF_PN[i])) {
                        targetPackages.add(as.EF_PN[i]);
                    }
                }
                for (int i2 = 0; i2 < as.EF_AN.length; i2++) {
                    if (!targetActivities.contains(as.EF_AN[i2])) {
                        targetActivities.add(as.EF_AN[i2]);
                    }
                }
            }
            for (String meetingAppsPN : mMeetingAppsPN) {
                if (!targetPackages.contains(meetingAppsPN)) {
                    targetPackages.add(meetingAppsPN);
                }
            }
            targetActivities.add(AN_WECHAT_SCAN_UI);
            ProcessManager.unregisterActivityChanageListener(this.mActivityChangeListener);
            ProcessManager.registerActivityChangeListener(targetPackages, targetActivities, this.mActivityChangeListener);
        }
    }

    private static boolean isUidValidForQos(int uid) {
        return UserHandle.isApp(uid);
    }

    private synchronized void enableQos(final boolean enable) {
        if (isQosReadyForEnable(enable) && this.mNetMgrService != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyQosUtils.2
                @Override // java.lang.Runnable
                public void run() {
                    boolean rst = MiuiNetworkPolicyQosUtils.this.mNetMgrService.enableQos(enable);
                    Log.i(MiuiNetworkPolicyQosUtils.TAG, "enableQos rst=" + rst);
                    if (rst) {
                        MiuiNetworkPolicyQosUtils.this.updateDscpStatus(enable);
                    }
                }
            });
            return;
        }
        Log.i(TAG, "enableQos return by invalid value!!!");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void setQos(final boolean add, final int uid, final int protocol) {
        if (isQosFeatureEnabled()) {
            if (isUidValidForQos(uid) && this.mNetMgrService != null && isQosReadyForSetValue(add)) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyQosUtils.3
                    @Override // java.lang.Runnable
                    public void run() {
                        boolean rst = MiuiNetworkPolicyQosUtils.this.mNetMgrService.setQos(protocol, uid, MiuiNetworkPolicyQosUtils.DSCP_TOS_EF, add);
                        Log.i(MiuiNetworkPolicyQosUtils.TAG, "setQos rst=" + rst + ",add=" + add + ",uid=" + uid + ",protocol=" + protocol);
                        if (rst) {
                            MiuiNetworkPolicyQosUtils.this.updateDscpStatus(add);
                            MiuiNetworkPolicyQosUtils.this.updateQosInfo(add, uid, protocol);
                        }
                    }
                });
                return;
            }
            Log.i(TAG, "setQos return by invalid value!!!");
        }
    }

    private void setDscpStatus(int dscpStatus) {
        this.mDscpState = dscpStatus;
    }

    private boolean isQosReadyForSetValue(boolean action) {
        int i = this.mDscpState;
        if (action) {
            if (i != 1) {
                return false;
            }
        } else if (i != 2) {
            return false;
        }
        return true;
    }

    private boolean isQosReadyForEnable(boolean action) {
        int i = this.mDscpState;
        if (action) {
            if (i != 0) {
                return false;
            }
        } else if (i != 1) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDscpStatus(boolean action) {
        if (!action) {
            if (this.mDscpState == 1) {
                setDscpStatus(0);
                return;
            } else {
                setDscpStatus(1);
                return;
            }
        }
        if (this.mDscpState == 0) {
            setDscpStatus(1);
        } else {
            setDscpStatus(2);
        }
    }

    private Set<String> getEFApps() {
        Set<String> appList = new HashSet<>();
        int i = 0;
        while (true) {
            String[] strArr = LOCAL_EF_APP_LIST;
            if (i < strArr.length) {
                appList.add(strArr[i]);
                i++;
            } else {
                return appList;
            }
        }
    }

    private void updateQosUid() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        PackageManager pm = this.mContext.getPackageManager();
        List<UserInfo> users = um.getUsers();
        Set<String> eFApps = getEFApps();
        this.mEFAppsPN = eFApps;
        if (!eFApps.isEmpty() || !mMeetingAppsPN.isEmpty()) {
            removeAll();
            for (UserInfo user : users) {
                List<PackageInfo> apps = pm.getInstalledPackagesAsUser(0, user.id);
                for (PackageInfo app : apps) {
                    if (app.packageName != null && app.applicationInfo != null && (this.mEFAppsPN.contains(app.packageName) || mMeetingAppsPN.contains(app.packageName))) {
                        int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                        addUidToMap(app.packageName, uid);
                    }
                }
            }
        }
    }

    private void addUidToMap(String packageName, int uid) {
        if (!this.mUidMap.containsKey(packageName)) {
            this.mUidMap.put(packageName, Integer.valueOf(uid));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getUidFromMap(String packageName) {
        if (this.mUidMap.get(packageName) == null) {
            return -1;
        }
        return this.mUidMap.get(packageName).intValue();
    }

    private boolean hasUidFromMap(int uid) {
        int i = 0;
        while (true) {
            String[] strArr = LOCAL_EF_APP_LIST;
            if (i < strArr.length) {
                if (getUidFromMap(strArr[i]) != uid) {
                    i++;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private void removeUidFromMap(String packageName) {
        this.mUidMap.remove(packageName);
    }

    private void removeAll() {
        this.mUidMap.clear();
    }

    public void updateAppPN(String packageName, int uid, boolean action) {
        List<String> list;
        Set<String> set = this.mEFAppsPN;
        if ((set != null && set.contains(packageName)) || ((list = mMeetingAppsPN) != null && list.contains(packageName))) {
            if (action) {
                addUidToMap(packageName, uid);
            } else {
                removeUidFromMap(packageName);
            }
        }
    }

    public void updateQosForUidStateChange(int uid, int oldUidState, int newUidState) {
        if (isUidValidForQos(uid) && isQosEnabledForUid(uid, oldUidState) != isQosEnabledForUid(uid, newUidState)) {
            updateQosForUidState(uid, newUidState);
        }
    }

    private boolean isQosEnabledForUid(int uid, int state) {
        return state == 2 && hasUidFromMap(uid);
    }

    private void updateQosForUidState(int uid, int state) {
        if (isQosEnabledForUid(uid, state) && this.mIsMobileDataOn) {
            Log.i(TAG, "updateQosForUidState mIsMobileDataOn" + this.mIsMobileDataOn);
            gameSceneNotification(true);
            setQos(true, uid, IP);
        } else {
            gameSceneNotification(false);
            setQos(false, uid, IP);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class QosInfo {
        private boolean enabled;
        private int protocol;
        private int uid;

        public QosInfo() {
            clear();
        }

        public void updateAll(boolean enabled, int uid, int protocol) {
            this.enabled = enabled;
            this.uid = uid;
            this.protocol = protocol;
        }

        public boolean getQosStatus() {
            return this.enabled;
        }

        public int getUid() {
            return this.uid;
        }

        public int getProtocol() {
            return this.protocol;
        }

        public void clear() {
            this.enabled = false;
            this.uid = 0;
            this.protocol = -1;
        }

        public String toString() {
            return "enabled=" + this.enabled + " uid=" + this.uid + " protocol=" + this.protocol;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processMeetingListChanged() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyQosUtils.5
            @Override // java.lang.Runnable
            public void run() {
                MiuiNetworkPolicyQosUtils miuiNetworkPolicyQosUtils = MiuiNetworkPolicyQosUtils.this;
                miuiNetworkPolicyQosUtils.initMeetingWhiteList(miuiNetworkPolicyQosUtils.mContext);
                MiuiNetworkPolicyQosUtils.this.registerActivityChangeListener();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateQosInfo(boolean enabled, int uid, int protocol) {
        QosInfo qosInfo = this.mQosInfo;
        if (qosInfo == null) {
            return;
        }
        if (enabled) {
            if (qosInfo.getQosStatus()) {
                Log.i(TAG, "updateQosInfo enabled but already true");
                return;
            } else {
                this.mQosInfo.updateAll(enabled, uid, protocol);
                return;
            }
        }
        if (!qosInfo.getQosStatus()) {
            Log.i(TAG, "updateQosInfo disable but already false");
        } else {
            this.mQosInfo.clear();
        }
    }

    private void weChatVoipNotification(boolean enable) {
        Log.i(TAG, "weChatVoipNotification enable=" + enable);
        Intent intent = new Intent();
        intent.setAction(WECHAT_VOIP_SCENE);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(WECHAT_VOIP_ENABLED, enable);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void meetingVoipNotification(boolean enable) {
        Log.i(TAG, "meetingVoipNotification enable=" + enable);
        Intent intent = new Intent();
        intent.setAction(MEETING_VOIP_SCENE);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(MEETING_VOIP_ENABLED, enable);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private static boolean isQosFeatureEnabled() {
        boolean deviceRst = "cepheus".equals(Build.DEVICE) || "raphael".equals(Build.DEVICE) || "davinci".equals(Build.DEVICE) || "crux".equals(Build.DEVICE) || "tucana".equals(Build.DEVICE) || "cmi".equals(Build.DEVICE) || "umi".equals(Build.DEVICE) || "picasso".equals(Build.DEVICE) || "phoenix".equals(Build.DEVICE) || "lmi".equals(Build.DEVICE) || "lmipro".equals(Build.DEVICE) || "vangogh".equals(Build.DEVICE) || "cas".equals(Build.DEVICE) || "apollo".equals(Build.DEVICE);
        boolean isQosAllowed = IS_QCOM;
        return deviceRst || isQosAllowed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void updateVoipStatus(String name, boolean enableStatus, int index) {
        if (!TextUtils.isEmpty(name) && name.contains(mMeetingAppsAN.get(index))) {
            if (MiuiNetworkPolicyManagerService.isMobileTcFeatureAllowed()) {
                mobileTcEnabledStatusChanged(enableStatus);
            }
            if (name.contains(EF_AN_WECHAT)) {
                weChatVoipNotification(enableStatus);
            } else {
                meetingVoipNotification(enableStatus);
            }
        }
        if (TelephonyManagerEx.getDefault().isPlatform8550() || TelephonyManagerEx.getDefault().isPlatform8650()) {
            Log.d(TAG, "8550 platform DE3.0");
            enableLLMMode(enableStatus, enableStatus ? 3 : -1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableLLMMode(boolean enable, int scene) {
        Log.i(TAG, "enableLLMMode enable=" + enable + ",scene=" + scene);
        Intent intent = new Intent();
        intent.setAction(LATENCY_ACTION_CHANGE_LEVEL);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(LATENCY_KEY_SCENE, scene);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void mobileTcEnabledStatusChanged(boolean z) {
        Log.i(TAG, "mobileTcEnabledStatusChanged for wechat enable=" + z);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(13, z ? 1 : 0, 0));
    }

    private void gameSceneNotification(boolean enable) {
        if (miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        Log.i(TAG, "gameSceneNotification enable=" + enable);
        Intent intent = new Intent();
        intent.setAction(GAME_SCENE);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(GAME_ENABLED, enable);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }
}
