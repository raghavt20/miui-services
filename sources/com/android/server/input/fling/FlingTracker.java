package com.android.server.input.fling;

import android.app.AlarmManager;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import android.view.MotionEvent;
import com.android.server.input.MiuiInputThread;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import java.util.Arrays;
import java.util.HashMap;
import miui.hardware.input.overscroller.FlingInfo;
import miui.hardware.input.overscroller.FlingUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class FlingTracker {
    private static final String DOWN_NUMBER = "down_number";
    private static final String FLING_NUMBER = "fling_number";
    private static final int MAX_ARRAY_LEN = 2;
    private static final String PACKAGE_NAME = "package_name";
    private static final int REPORT_INTERVAL = 20;
    private static final String TAG = "FlingTracker";
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private int[] mEventByDeviceId;
    private HashMap<String, FlingInfo> mEventCount = new HashMap<>();
    private String DIR_NAME = "FlingInfo";
    private String FILE_NAME = "fling_info";
    private final long INTERVAL_DAY = SystemProperties.getInt("debug.fling.timeout", 86400000);
    private final Object mLock = new Object();
    private final AlarmManager.OnAlarmListener mEventAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.input.fling.FlingTracker.1
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            FlingTracker.this.reportEvent();
            FlingTracker.this.updateAlarmForTrack();
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAlarmForTrack() {
        if (FlingUtil.sDisableReport) {
            return;
        }
        this.mAlarmManager.setExact(1, System.currentTimeMillis() + this.INTERVAL_DAY, "report_fling_event", this.mEventAlarmListener, MiuiInputThread.getHandler());
    }

    public FlingTracker(Context context) {
        this.mContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        int[] iArr = new int[2];
        this.mEventByDeviceId = iArr;
        Arrays.fill(iArr, 0);
        updateAlarmForTrack();
    }

    public void trackEvent(String pkgName, int downTimes, int flingTimes) {
        if (FlingUtil.sDisableReport) {
            return;
        }
        synchronized (this.mLock) {
            if (!this.mEventCount.containsKey(pkgName)) {
                this.mEventCount.put(pkgName, new FlingInfo(pkgName));
            }
            int[] iArr = this.mEventCount.get(pkgName).mEventTimes;
            iArr[0] = iArr[0] + downTimes;
            int[] iArr2 = this.mEventCount.get(pkgName).mEventTimes;
            iArr2[1] = iArr2[1] + flingTimes;
        }
    }

    public void reportEvent() {
        synchronized (this.mLock) {
            int size = this.mEventCount.size();
            if (size == 0) {
                return;
            }
            int[] iArr = this.mEventByDeviceId;
            int i = iArr[0];
            int totalDownTimes = iArr[1] + i;
            if (totalDownTimes != 0 && i / (totalDownTimes + 0.0d) >= 0.2d) {
                Log.w(TAG, "reportEvent: drop events from total: " + totalDownTimes + ", from device -1: " + this.mEventByDeviceId[0]);
                return;
            }
            int totalFlingTimes = 0;
            int nums = 0;
            JSONArray arr = new JSONArray();
            for (String pkgName : this.mEventCount.keySet()) {
                nums++;
                FlingInfo info = this.mEventCount.get(pkgName);
                int downTimes = info.mEventTimes[0];
                int flingTimes = info.mEventTimes[1];
                totalFlingTimes += flingTimes;
                JSONObject obj = new JSONObject();
                try {
                    obj.put("package_name", pkgName);
                    obj.put(DOWN_NUMBER, downTimes);
                    obj.put(FLING_NUMBER, flingTimes);
                    arr.put(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (nums % 20 == 0 || nums == size) {
                    FlingOneTrackHelper.getInstance(this.mContext).trackAppFlingEvent(arr);
                    arr = new JSONArray();
                }
                if (nums == size) {
                    FlingOneTrackHelper.getInstance(this.mContext).trackAllAppFlingEvent(totalDownTimes, totalFlingTimes);
                }
            }
            this.mEventCount.clear();
            Arrays.fill(this.mEventByDeviceId, 0);
        }
    }

    public void registerPointerEventListener() {
        if (FlingUtil.sDisableReport) {
            return;
        }
        MiuiGestureMonitor.getInstance(this.mContext).registerPointerEventListener(new TrackMotionListener());
    }

    /* loaded from: classes.dex */
    class TrackMotionListener implements MiuiGestureListener {
        TrackMotionListener() {
        }

        @Override // com.miui.server.input.gesture.MiuiGestureListener
        public void onPointerEvent(MotionEvent motionEvent) {
            synchronized (FlingTracker.this.mLock) {
                int deviceId = motionEvent.getDeviceId();
                int action = motionEvent.getActionMasked();
                if (action == 0) {
                    int index = deviceId == -1 ? 0 : 1;
                    int[] iArr = FlingTracker.this.mEventByDeviceId;
                    iArr[index] = iArr[index] + 1;
                }
            }
        }
    }
}
