package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.MiuiBgThread;
import com.xiaomi.joyose.smartop.gamebooster.scenerecognize.IPhashSceneRecognizeInterface;

/* loaded from: classes.dex */
public final class MiuiRecognizeScene {
    private static final long BIND_FAIL_RETRY_TIME = 60000;
    private static final boolean DEBUG = false;
    private static final String JOYOSE_PACKAGE = "com.xiaomi.joyose";
    private static final String JOYOSE_SERVICE_CLASS = "com.xiaomi.joyose.smartop.gamebooster.scenerecognize.SceneRecognizeService";
    private static final String RECOG_SCENE_ENABLE = "persist.miui.recog.scene.enable";
    public static final boolean REC_ENABLE = SystemProperties.getBoolean(RECOG_SCENE_ENABLE, false);
    private static final String TAG = "MiuiRecognizeScene";
    private Handler mBgHandler;
    private Context mContext;
    private IPhashSceneRecognizeInterface mRecognizeService;
    private Runnable mBindServiceRunnable = new Runnable() { // from class: com.android.server.wm.MiuiRecognizeScene$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            MiuiRecognizeScene.this.lambda$new$0();
        }
    };
    private IBinder.DeathRecipient mDeathHandler = new IBinder.DeathRecipient() { // from class: com.android.server.wm.MiuiRecognizeScene.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MiuiRecognizeScene.this.sendRebindServiceMsg();
        }
    };
    private final ServiceConnection mPerformanceConnection = new ServiceConnection() { // from class: com.android.server.wm.MiuiRecognizeScene.2
        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName arg0) {
            MiuiRecognizeScene.this.mRecognizeService = null;
            if (MiuiRecognizeScene.this.mContext != null) {
                MiuiRecognizeScene.this.mContext.unbindService(MiuiRecognizeScene.this.mPerformanceConnection);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            MiuiRecognizeScene.this.mRecognizeService = IPhashSceneRecognizeInterface.Stub.asInterface(arg1);
            MiuiRecognizeScene.this.mBgHandler.removeCallbacks(MiuiRecognizeScene.this.mBindServiceRunnable);
            try {
                MiuiRecognizeScene.this.mRecognizeService.asBinder().linkToDeath(MiuiRecognizeScene.this.mDeathHandler, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public MiuiRecognizeScene(Context context) {
        this.mContext = context;
        Handler handler = new Handler(MiuiBgThread.get().getLooper());
        this.mBgHandler = handler;
        if (REC_ENABLE) {
            handler.postDelayed(this.mBindServiceRunnable, 10000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        Slog.d(TAG, "start bind service");
        bindService();
    }

    public String recognizeScene(String packageName) {
        IPhashSceneRecognizeInterface iPhashSceneRecognizeInterface = this.mRecognizeService;
        if (iPhashSceneRecognizeInterface == null) {
            return "null";
        }
        try {
            String result = iPhashSceneRecognizeInterface.recognizeScene(packageName);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "recognizeScene exception ", e);
            return "null";
        }
    }

    private void bindService() {
        if (this.mContext == null || this.mRecognizeService != null) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setClassName(JOYOSE_PACKAGE, JOYOSE_SERVICE_CLASS);
            if (!this.mContext.bindServiceAsUser(intent, this.mPerformanceConnection, 1, UserHandle.OWNER)) {
                sendRebindServiceMsg();
            }
        } catch (Exception e) {
            Log.e(TAG, "bindService exception ", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendRebindServiceMsg() {
        this.mBgHandler.removeCallbacks(this.mBindServiceRunnable);
        this.mBgHandler.postDelayed(this.mBindServiceRunnable, 60000L);
    }
}
