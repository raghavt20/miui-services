package com.xiaomi.NetworkBoost.slaservice;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.xiaomi.NetworkBoost.StatusManager;
import java.util.HashSet;
import miui.process.ForegroundInfo;

/* loaded from: classes.dex */
public class SLAToast {
    private static final long DELAY_TIME = 100;
    private static String TAG = "SLM-SRV-SLAToast";
    private static final int TOAST_LOOPER = 1;
    private static final int TOAST_TIME = 2000;
    private static HashSet<String> mApps;
    private static SLAAppLib mSLAAppLib;
    private boolean isLinkTurboEnable;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private StatusManager mStatusManager = null;
    private StatusManager.IAppStatusListener mAppStatusListener = new StatusManager.IAppStatusListener() { // from class: com.xiaomi.NetworkBoost.slaservice.SLAToast.1
        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            Log.i(SLAToast.TAG, "foreground uid:" + Integer.toString(foregroundInfo.mForegroundUid) + ", isColdStart:" + foregroundInfo.isColdStart() + ", isLinkTurboEnable:" + SLAToast.this.isLinkTurboEnable);
            if (foregroundInfo.isColdStart() && SLAToast.this.isLinkTurboEnable && SLAToast.mApps.contains(Integer.toString(foregroundInfo.mForegroundUid))) {
                Message msg = Message.obtain();
                msg.what = 1;
                SLAToast.this.mHandler.sendMessageAtTime(msg, SLAToast.DELAY_TIME);
            }
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onUidGone(int uid, boolean disabled) {
        }

        @Override // com.xiaomi.NetworkBoost.StatusManager.IAppStatusListener
        public void onAudioChanged(int mode) {
        }
    };

    public SLAToast(Context context) {
        Log.i(TAG, "SLAToast");
        this.mContext = context;
        mApps = new HashSet<>();
        initSlaToastHandler();
        registerAppStatusListener();
    }

    private void registerAppStatusListener() {
        try {
            StatusManager statusManager = StatusManager.getInstance(this.mContext);
            this.mStatusManager = statusManager;
            statusManager.registerAppStatusListener(this.mAppStatusListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
    }

    private void initSlaToastHandler() {
        HandlerThread handlerThread = new HandlerThread("SlaToastHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.xiaomi.NetworkBoost.slaservice.SLAToast.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        try {
                            Toast.makeText(SLAToast.this.mContext, 286196577, 2000).show();
                            return;
                        } catch (Exception e) {
                            Log.e(SLAToast.TAG, "show slatoast" + e);
                            return;
                        }
                    default:
                        return;
                }
            }
        };
    }

    public void setLinkTurboStatus(boolean enable) {
        this.isLinkTurboEnable = enable;
    }

    public static void setLinkTurboUidList(String uidList) {
        mApps.clear();
        if (uidList == null) {
            return;
        }
        String[] temp = uidList.split(",");
        for (String str : temp) {
            mApps.add(str);
        }
    }

    public HandlerThread getSLAToastHandlerThread() {
        return this.mHandlerThread;
    }
}
