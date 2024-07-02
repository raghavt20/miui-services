package com.android.server.powerconsumpiton;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.power.PowerManagerServiceStub;
import com.android.server.wm.IMiuiWindowStateEx;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

/* loaded from: classes.dex */
public final class PowerConsumptionServiceController {
    protected static final String TAG = "PowerConsumptionServiceController";
    private static boolean sDEBUG = false;
    private HashMap<String, int[]> mActivityPolicies;
    private long mClickScreenTime;
    private Context mContext;
    private IMiuiWindowStateEx mCurMiuiWindowStateEx;
    private HashMap<String, int[]> mCurPolicies;
    private PowerManager.WakeLock mDimWakeLock;
    private Handler mExeHandler;
    private final PowerConsumptionServiceCouldData mPowerConsumptionServiceCouldData;
    private PowerManager mPowerManager;
    private PowerManagerServiceStub mPowerManagerServiceImpl;
    private SystemStatusListener mSystemStatusListener;
    private HashMap<String, int[]> mWindowPolicies;
    private boolean mIsContentKeyInWhiteList = false;
    private String mCurActivityName = new String();
    private String mCurWindowName = new String();

    public PowerConsumptionServiceController(PowerConsumptionServiceCouldData powerConsumptionServiceCouldData, SystemStatusListener systemStatusListener, Handler exeHandler, Context context) {
        this.mPowerConsumptionServiceCouldData = powerConsumptionServiceCouldData;
        this.mSystemStatusListener = systemStatusListener;
        this.mExeHandler = exeHandler;
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        init();
    }

    private void init() {
        this.mActivityPolicies = this.mPowerConsumptionServiceCouldData.getActivityPolicy();
        this.mWindowPolicies = this.mPowerConsumptionServiceCouldData.getWindowPolicies();
        this.mPowerManagerServiceImpl = PowerManagerServiceStub.get();
        this.mCurPolicies = new HashMap<>();
        PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(6, "WAKEUP-FROM-CLOUD-DIM");
        this.mDimWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
    }

    public boolean getContentKeyInWhiteList() {
        if (sDEBUG) {
            Slog.d(TAG, "mIsContentKeyInWhiteList " + this.mIsContentKeyInWhiteList);
        }
        return this.mIsContentKeyInWhiteList;
    }

    private int[] getActivityPolicy(String name) {
        int[] iArr = new int[0];
        int[] policy = this.mActivityPolicies.get(name);
        return policy;
    }

    private int[] getWindowPolicies(String name) {
        int[] iArr = new int[0];
        int[] policy = this.mWindowPolicies.get(name);
        return policy;
    }

    public void noteStartActivityForPowerConsumption(String name) {
        int[] activityPolicies = getActivityPolicy(name);
        this.mCurActivityName = name;
        if (activityPolicies != null && activityPolicies.length > 0 && this.mSystemStatusListener.isPowerSaveMode()) {
            int flag = activityPolicies[0];
            boolean isUpdate = false;
            if ((flag & 16) != 0) {
                isUpdate = true;
                this.mIsContentKeyInWhiteList = true;
            }
            if (isUpdate) {
                int[] policyCopy = new int[activityPolicies.length];
                System.arraycopy(activityPolicies, 0, policyCopy, 0, activityPolicies.length);
                this.mCurPolicies.put(name, policyCopy);
                return;
            } else {
                if (sDEBUG) {
                    Slog.d(TAG, "this policy " + flag + " is working now ");
                    return;
                }
                return;
            }
        }
        this.mIsContentKeyInWhiteList = false;
    }

    public void noteFoucsChangeForPowerConsumptionLocked(String name, IMiuiWindowStateEx miuiWindowStateEx) {
        int[] windowPolicies = getWindowPolicies(name);
        int[] curPolicies = this.mCurPolicies.get(this.mCurWindowName);
        String str = this.mCurWindowName;
        if (str != name && curPolicies != null) {
            curPolicies[0] = curPolicies[0] & (-33);
            this.mCurPolicies.put(str, curPolicies);
        }
        IMiuiWindowStateEx iMiuiWindowStateEx = this.mCurMiuiWindowStateEx;
        if (iMiuiWindowStateEx != null && iMiuiWindowStateEx.isDimWindow()) {
            this.mCurMiuiWindowStateEx.setMiuiUserActivityTimeOut(-1);
            if (this.mDimWakeLock.isHeld()) {
                this.mDimWakeLock.release();
            }
        }
        this.mCurWindowName = name;
        this.mCurMiuiWindowStateEx = miuiWindowStateEx;
        boolean isUpdate = false;
        if (windowPolicies != null && windowPolicies.length > 0) {
            int policy = windowPolicies[0];
            if ((policy & 32) != 0 && !this.mSystemStatusListener.isCameraOpen() && windowPolicies.length > 1) {
                int dimTime = windowPolicies[1];
                boolean isDimAssist = windowPolicies[2] == 1;
                if (miuiWindowStateEx.isCloudDimWindowingMode()) {
                    miuiWindowStateEx.setMiuiUserActivityTimeOut(dimTime);
                    miuiWindowStateEx.setDimAssist(isDimAssist);
                    miuiWindowStateEx.setIsDimWindow(true);
                    isUpdate = true;
                    if (miuiWindowStateEx.isKeepScreenOnFlag()) {
                        this.mDimWakeLock.acquire();
                    } else {
                        long timeout = miuiWindowStateEx.getScreenOffTime();
                        this.mDimWakeLock.acquire(timeout);
                    }
                    long timeout2 = SystemClock.elapsedRealtime();
                    this.mClickScreenTime = timeout2;
                }
            }
            if (isUpdate) {
                int[] policyCopy = new int[windowPolicies.length];
                System.arraycopy(windowPolicies, 0, policyCopy, 0, windowPolicies.length);
                this.mCurPolicies.put(name, policyCopy);
            }
        }
    }

    public void updateDimState(boolean dimEnable, long brightWaitTime) {
        this.mPowerManagerServiceImpl.updateDimState(dimEnable, brightWaitTime);
    }

    public void setScreenOffTimeOutDelay() {
        PowerManager.WakeLock wakeLock;
        IMiuiWindowStateEx iMiuiWindowStateEx = this.mCurMiuiWindowStateEx;
        if (iMiuiWindowStateEx != null && iMiuiWindowStateEx.isDimWindow() && !this.mCurMiuiWindowStateEx.isKeepScreenOnFlag() && (wakeLock = this.mDimWakeLock) != null) {
            wakeLock.acquire(this.mCurMiuiWindowStateEx.getScreenOffTime());
        }
    }

    public void setVsyncRate(IMiuiWindowStateEx w, int rate) {
        if (sDEBUG) {
            Slog.d(TAG, "Set Vsync Rate, VSync Rate is: " + rate);
        }
        w.setVsyncRate(rate);
    }

    public void resetVsyncRate(IMiuiWindowStateEx w) {
        if (sDEBUG) {
            Slog.d(TAG, "Reset Vsync Rate.");
        }
        w.resetVsyncRate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        int[] activityPolicy = this.mCurPolicies.get(this.mCurActivityName);
        int[] windowPolicy = this.mCurPolicies.get(this.mCurWindowName);
        pw.println("  mCurActivityName=" + this.mCurActivityName + "; mCurPolicy=" + Arrays.toString(activityPolicy));
        pw.println("  mCurWindowName=" + this.mCurWindowName + "; mCurPolicy=" + Arrays.toString(windowPolicy));
    }
}
