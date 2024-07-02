package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/* loaded from: classes.dex */
public class WifiDrvUEventObserver extends UEventObserver {
    private static final int ACTION_CHIP_RESET = 0;
    private static final int ACTION_L1_SER = 2;
    private static final int ACTION_L3_SER = 3;
    private static final int ACTION_ROAMING_NOTIFY = 10;
    private static final int ACTION_WIFI_RESET = 1;
    public static final int COEX_FDD_MODE = 3;
    public static final int COEX_HBD_MODE = 2;
    private static final String COEX_MODE_EVENT = "coex_mode";
    public static final int COEX_NO_BT = 0;
    public static final int COEX_TDD_MODE = 1;
    private static final boolean DEBUG = false;
    private static final String EXTRA_WBH_MODE_TYPE = "extra_wbh_mode_type";
    private static final String L1_SER = "l1ser";
    private static final String L3_SER = "l3ser";
    private static final int MAX_REC_SIZE = 30;
    public static final int MTK_DATASTALL_COEX_LOW = 516;
    public static final int MTK_DATASTALL_FCS_ERROR = 515;
    public static final int MTK_DATASTALL_NONE = 0;
    public static final int MTK_DATASTALL_NUD_FAILURE = 518;
    public static final int MTK_DATASTALL_PM_CHANGE_FAIL = 517;
    public static final int MTK_DATASTALL_RX_REORDER = 514;
    public static final int MTK_DATASTALL_TX_DROP = 513;
    public static final int MTK_DATASTALL_TX_HANG = 512;
    private static final String MTK_RECOVERY_EVENT = "recoveryNotify";
    private static final String MTK_ROAM_NOTIFY_EVENT = "roam";
    private static final String MTK_ROAM_SUCCESS = "Status:SUCCESS";
    private static final String MTK_TRX_ABNORMAL_EVENT = "abnormaltrx";
    private static final String TAG = "WifiDrvUEventObserver";
    private static final String UEVENT_PATH = "DEVPATH=/devices/virtual/misc/wlan";
    public static final String WBH_MODE_CHANGED = "android.net.wifi.COEX_MODE_STATE_CHANGED";
    private static final String WIFI_RESET = "reset";
    private static LinkedList<UeventRecord> mUeventRecord = new LinkedList<>();
    private final Map<Integer, String> MTK_DATASTALL_TYPE = Map.of(512, "DIR:TX,Event:Hang", Integer.valueOf(MTK_DATASTALL_TX_DROP), "DIR:TX,event:AbDrop", Integer.valueOf(MTK_DATASTALL_FCS_ERROR), "DIR:RX,Event:AbCRC", Integer.valueOf(MTK_DATASTALL_RX_REORDER), "DIR:RX,event:AbReorder", Integer.valueOf(MTK_DATASTALL_COEX_LOW), "DIR:TXRX,event:BTWifiCoexLow", Integer.valueOf(MTK_DATASTALL_PM_CHANGE_FAIL), "DIR:TX,Event:pmStateChangeFail", Integer.valueOf(MTK_DATASTALL_NUD_FAILURE), "DIR:RX,event:AbArpNoResponse");
    private int lastCoexMode = 0;
    private WifiDrvUeventCallback mCallback;
    private Context mContext;

    /* loaded from: classes.dex */
    public interface WifiDrvUeventCallback {
        void onCoexModeChange(int i);

        void onDataStall(int i);

        void onRecoveryComplete(int i, boolean z);
    }

    public WifiDrvUEventObserver(Context context, WifiDrvUeventCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
    }

    public void onUEvent(UEventObserver.UEvent event) {
        String coexMode;
        try {
            String coexMode2 = event.get(COEX_MODE_EVENT);
            String mtkTrxAbnormal = event.get(MTK_TRX_ABNORMAL_EVENT);
            String mtkRecoveryEvent = event.get(MTK_RECOVERY_EVENT);
            String mtkRoamNotify = event.get(MTK_ROAM_NOTIFY_EVENT);
            if (coexMode2 != null) {
                int curCoexMode = Integer.parseInt(coexMode2);
                int i = this.lastCoexMode;
                if (curCoexMode != i && (curCoexMode >= 2 || i >= 2)) {
                    UeventRecord uRec = new UeventRecord(COEX_MODE_EVENT, coexMode2);
                    addUeventRec(uRec);
                    this.mCallback.onCoexModeChange(curCoexMode);
                    Intent intent = new Intent(WBH_MODE_CHANGED);
                    intent.putExtra(EXTRA_WBH_MODE_TYPE, curCoexMode);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
                }
                this.lastCoexMode = curCoexMode;
            }
            if (mtkTrxAbnormal != null) {
                int type = getDatastallType(mtkTrxAbnormal);
                if (type != 0) {
                    this.mCallback.onDataStall(type);
                }
                UeventRecord uRec2 = new UeventRecord(MTK_TRX_ABNORMAL_EVENT, mtkTrxAbnormal);
                addUeventRec(uRec2);
            }
            if (mtkRecoveryEvent != null) {
                int action = -1;
                Boolean recoveryState = false;
                int reason = -1;
                String[] recoveryInfo = mtkRecoveryEvent.split(",");
                int length = recoveryInfo.length;
                int i2 = 0;
                while (i2 < length) {
                    String str = recoveryInfo[i2];
                    if (str == null || str.equals("")) {
                        coexMode = coexMode2;
                    } else {
                        String key = str.substring(0, str.indexOf(58));
                        String value = str.substring(str.indexOf(58) + 1);
                        coexMode = coexMode2;
                        if (key.equals(WIFI_RESET)) {
                            action = 1;
                            if (value != null && !value.equals("")) {
                                recoveryState = Boolean.valueOf(Integer.parseInt(value) == 0);
                            }
                        } else if (key.equals(L1_SER)) {
                            action = 2;
                            if (value != null && !value.equals("")) {
                                recoveryState = Boolean.valueOf(Integer.parseInt(value) == 0);
                            }
                        } else if (key.equals(L3_SER)) {
                            action = 3;
                            if (value != null && !value.equals("")) {
                                recoveryState = Boolean.valueOf(Integer.parseInt(value) == 0);
                            }
                        } else if (key.equals(EdgeSuppressionManager.EdgeSuppressionHandler.MSG_DATA_REASON) && value != null && !value.equals("")) {
                            reason = Integer.parseInt(value);
                        }
                    }
                    i2++;
                    coexMode2 = coexMode;
                }
                if (reason == 1) {
                    this.mCallback.onRecoveryComplete(action, recoveryState.booleanValue());
                }
            }
            if (mtkRoamNotify != null) {
                boolean isRoamSuccess = mtkRoamNotify.contains(MTK_ROAM_SUCCESS);
                this.mCallback.onRecoveryComplete(10, isRoamSuccess);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not parse event " + event);
        }
    }

    public void start() {
        Log.d(TAG, "startObserving");
        startObserving(UEVENT_PATH);
    }

    public void stop() {
        Log.d(TAG, "stopObserving");
        stopObserving();
    }

    private int getDatastallType(String datastallEvent) {
        Log.d(TAG, "event: " + datastallEvent);
        if (TextUtils.isEmpty(datastallEvent)) {
            return 0;
        }
        Iterator<Integer> it = this.MTK_DATASTALL_TYPE.keySet().iterator();
        while (it.hasNext()) {
            int key = it.next().intValue();
            if (!TextUtils.isEmpty(this.MTK_DATASTALL_TYPE.get(Integer.valueOf(key))) && datastallEvent.contains(this.MTK_DATASTALL_TYPE.get(Integer.valueOf(key)))) {
                Log.d(TAG, "datastall type: " + key);
                return key;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UeventRecord {
        private String mEvent;
        private String mInfo;
        private long mTime = System.currentTimeMillis();

        public UeventRecord(String event, String info) {
            this.mEvent = event;
            this.mInfo = info;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("time=");
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(this.mTime);
            sb.append(String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c));
            sb.append(" event=");
            String str = this.mEvent;
            if (str == null) {
                str = "<null>";
            }
            sb.append(str);
            sb.append(" info=");
            String str2 = this.mInfo;
            sb.append(str2 != null ? str2 : "<null>");
            return sb.toString();
        }
    }

    private static int getUeventRecCount() {
        LinkedList<UeventRecord> linkedList = mUeventRecord;
        if (linkedList == null) {
            return 0;
        }
        return linkedList.size();
    }

    private void addUeventRec(UeventRecord rec) {
        LinkedList<UeventRecord> linkedList;
        if (rec == null || (linkedList = mUeventRecord) == null) {
            return;
        }
        if (linkedList.size() >= 30) {
            mUeventRecord.removeFirst();
        }
        mUeventRecord.addLast(rec);
    }

    private static UeventRecord getUeventRec(int index) {
        if (mUeventRecord == null || index >= getUeventRecCount()) {
            return null;
        }
        return mUeventRecord.get(index);
    }

    public static void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiDrvUEvent:");
        pw.println(" total records=" + getUeventRecCount());
        for (int i = 0; i < getUeventRecCount(); i++) {
            pw.println(" rec[" + i + "]: " + getUeventRec(i));
            pw.flush();
        }
    }
}
