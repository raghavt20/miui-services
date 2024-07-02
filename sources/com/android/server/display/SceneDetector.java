package com.android.server.display;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.display.AutomaticBrightnessControllerStub;
import com.android.server.display.SceneDetector;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.xiaomi.aon.IAONFlareService;
import com.xiaomi.aon.IMiAONListener;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class SceneDetector {
    private static final String ACTION_CLIENT_SERVICE = "com.xiaomi.aon.AonFlareService";
    private static final int AON_WAIT_DATA_STATE = 2;
    private static final int AON_WAIT_REGISTER_STATE = 1;
    private static final float DEFAULT_FRAMES_PER_SECOND = 1.0f;
    private static final int DEFAULT_MAX_DELAY_TIME = 5000;
    private static final int MSG_CHECK_AON_DATA_RETURN = 1;
    private static final int MSG_UPDATE_BIND_AON_STATUS = 2;
    private static final int MSG_UPDATE_UNBIND_AON_STATUS = 3;
    private static final int SUPPRESS_DARKEN_EVENT = 1;
    private static final String TAG = "SceneDetector";
    private static final int TYPE_AON_FLARE = 5;
    private static boolean sDEBUG = false;
    private float mAmbientLux;
    private int mAonFlareMaxDelayTime;
    private IAONFlareService mAonFlareService;
    private boolean mAutomaticBrightnessEnable;
    private AutomaticBrightnessControllerImpl mBrightnessControllerImpl;
    private Context mContext;
    private AutomaticBrightnessControllerStub.DualSensorPolicyListener mDualSensorPolicyListener;
    private boolean mIsMainDarkenEvent;
    private Handler mMainHandler;
    private float mMaxAonFlareEnableLux;
    private float mMinAonFlareEnableLux;
    private float mPreAmbientLux;
    private final ComponentName mServiceComponent;
    private boolean mServiceConnected;
    private int mAonState = 1;
    private IBinder.DeathRecipient mDeathRecipient = new AnonymousClass1();
    private ServiceConnection mServiceConnection = new AnonymousClass2();
    private IMiAONListener mMiAONListener = new AnonymousClass3();
    private Handler mSceneDetectorHandler = new SceneDetectorHandler(BackgroundThread.get().getLooper());

    public SceneDetector(AutomaticBrightnessControllerStub.DualSensorPolicyListener listener, AutomaticBrightnessControllerImpl brightnessControllerImpl, Handler handler, Context context) {
        this.mMainHandler = handler;
        this.mContext = context;
        this.mDualSensorPolicyListener = listener;
        this.mBrightnessControllerImpl = brightnessControllerImpl;
        Resources resources = context.getResources();
        this.mMaxAonFlareEnableLux = resources.getFloat(285671469);
        this.mMinAonFlareEnableLux = resources.getFloat(285671473);
        this.mAonFlareMaxDelayTime = resources.getInteger(285933583);
        String componentName = resources.getString(286195874);
        this.mServiceComponent = getAonFlareServiceComponent(componentName);
    }

    public void updateAmbientLux(int event, float lux, final boolean isMainDarkenEvent) {
        final float preLux = this.mDualSensorPolicyListener.getAmbientLux();
        this.mAmbientLux = lux;
        this.mSceneDetectorHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SceneDetector.this.lambda$updateAmbientLux$0(isMainDarkenEvent);
            }
        });
        if (!isMainDarkenEvent || lux > this.mMaxAonFlareEnableLux || lux < this.mMinAonFlareEnableLux) {
            this.mDualSensorPolicyListener.updateAmbientLux(event, lux, true, true);
        } else {
            this.mDualSensorPolicyListener.updateAmbientLux(event, lux, true, false);
            this.mSceneDetectorHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    SceneDetector.this.lambda$updateAmbientLux$1(preLux);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAmbientLux$0(boolean isMainDarkenEvent) {
        this.mBrightnessControllerImpl.notifyUpdateBrightness();
        this.mIsMainDarkenEvent = isMainDarkenEvent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tryToBindAonFlareService() {
        if (this.mServiceComponent == null) {
            return;
        }
        if (this.mServiceConnected && this.mAonFlareService != null) {
            return;
        }
        int userId = ActivityManager.getCurrentUser();
        Intent intent = new Intent(ACTION_CLIENT_SERVICE);
        intent.setComponent(this.mServiceComponent);
        intent.addFlags(SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX);
        if (!this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, new UserHandle(userId))) {
            Slog.e(TAG, "unable to bind service: bindService failed " + intent);
        }
        this.mBrightnessControllerImpl.notifyUpdateBrightness();
    }

    private ComponentName getAonFlareServiceComponent(String name) {
        if (name != null) {
            return ComponentName.unflattenFromString(name);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAutoBrightness() {
        this.mMainHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SceneDetector.this.lambda$updateAutoBrightness$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateAutoBrightness$2() {
        this.mDualSensorPolicyListener.updateAmbientLux(AutomaticBrightnessControllerStub.HANDLE_MAIN_LUX_EVENT, this.mAmbientLux, true, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: checkAonFlareStatus, reason: merged with bridge method [inline-methods] */
    public void lambda$updateAmbientLux$1(float preLux) {
        if (sDEBUG) {
            Slog.i(TAG, "checkAonFlareStatus mIsMainDarkenEvent:" + this.mIsMainDarkenEvent + "mAonState: " + this.mAonState);
        }
        if (this.mAonState == 1) {
            this.mPreAmbientLux = preLux;
            if (this.mServiceConnected && this.mAonFlareService != null) {
                registerAonFlareListener();
            } else {
                updateAutoBrightness();
            }
        }
    }

    private void registerAonFlareListener() {
        try {
            this.mAonFlareService.registerListener(5, 1.0f, 5000, this.mMiAONListener);
            this.mSceneDetectorHandler.sendEmptyMessageDelayed(1, this.mAonFlareMaxDelayTime);
            this.mAonState = 2;
            Slog.i(TAG, "registerAonFlareListener: register aon flare listener success.");
        } catch (RemoteException e) {
            Slog.e(TAG, "registerAonFlareListener: register aon flare listener failed.");
        }
    }

    public void configure(boolean enable) {
        if (!enable && this.mAutomaticBrightnessEnable) {
            this.mAutomaticBrightnessEnable = false;
            this.mSceneDetectorHandler.removeMessages(2);
            this.mSceneDetectorHandler.removeMessages(3);
            Message msg = this.mSceneDetectorHandler.obtainMessage(3);
            msg.sendToTarget();
            return;
        }
        if (enable && !this.mAutomaticBrightnessEnable) {
            this.mAutomaticBrightnessEnable = true;
            this.mSceneDetectorHandler.removeMessages(2);
            this.mSceneDetectorHandler.removeMessages(3);
            Message msg2 = this.mSceneDetectorHandler.obtainMessage(2);
            msg2.sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterAonFlareListener() {
        try {
            this.mAonFlareService.unregisterListener(5, this.mMiAONListener);
            this.mSceneDetectorHandler.removeMessages(1);
            this.mAonState = 1;
            Slog.i(TAG, "unregisterAonFlareListener: unregister aon flare listener success.");
        } catch (RemoteException e) {
            Slog.e(TAG, "unregisterAonFlareListener: unregister aon flare listener failed.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.SceneDetector$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements IBinder.DeathRecipient {
        AnonymousClass1() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            SceneDetector.this.mSceneDetectorHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SceneDetector.AnonymousClass1.this.lambda$binderDied$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$binderDied$0() {
            SceneDetector.this.mAonState = 1;
            SceneDetector.this.resetServiceConnectedStatus();
            SceneDetector.this.tryToBindAonFlareService();
            Slog.w(SceneDetector.TAG, "Process of service has died, try to bind it.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetServiceConnectedStatus() {
        IAONFlareService iAONFlareService = this.mAonFlareService;
        if (iAONFlareService != null && this.mServiceConnected) {
            iAONFlareService.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            this.mContext.unbindService(this.mServiceConnection);
            this.mAonFlareService = null;
            this.mServiceConnected = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.SceneDetector$2, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements ServiceConnection {
        AnonymousClass2() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, final IBinder service) {
            SceneDetector.this.mSceneDetectorHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SceneDetector.AnonymousClass2.this.lambda$onServiceConnected$0(service);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceConnected$0(IBinder service) {
            try {
                SceneDetector.this.mAonFlareService = IAONFlareService.Stub.asInterface(service);
                SceneDetector.this.mServiceConnected = true;
                service.linkToDeath(SceneDetector.this.mDeathRecipient, 0);
                Slog.i(SceneDetector.TAG, "onServiceConnected: aon flare service connected.");
            } catch (RemoteException e) {
                Slog.e(SceneDetector.TAG, "onServiceConnected: aon flare service connect failed.");
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            SceneDetector.this.mSceneDetectorHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SceneDetector.AnonymousClass2.this.lambda$onServiceDisconnected$1();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceDisconnected$1() {
            Slog.i(SceneDetector.TAG, "onServiceDisconnected: aon flare service disconnected.");
            SceneDetector.this.mAonFlareService = null;
            SceneDetector.this.mAonState = 1;
            SceneDetector.this.mServiceConnected = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.SceneDetector$3, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends IMiAONListener.Stub {
        AnonymousClass3() {
        }

        public void onCallbackListener(int type, final int[] data) throws RemoteException {
            if (type == 5) {
                SceneDetector.this.mSceneDetectorHandler.post(new Runnable() { // from class: com.android.server.display.SceneDetector$3$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        SceneDetector.AnonymousClass3.this.lambda$onCallbackListener$0(data);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onCallbackListener$0(int[] data) {
            if (SceneDetector.this.mAonState == 2) {
                if (data[0] == 1) {
                    SceneDetector.this.mBrightnessControllerImpl.notifyAonFlareEvents(1, SceneDetector.this.mPreAmbientLux);
                    Slog.i(SceneDetector.TAG, "aon flare algo suppress this darken event!");
                } else {
                    SceneDetector.this.mBrightnessControllerImpl.notifyAonFlareEvents(2, SceneDetector.this.mPreAmbientLux);
                    if (SceneDetector.this.mIsMainDarkenEvent) {
                        SceneDetector.this.updateAutoBrightness();
                        Slog.i(SceneDetector.TAG, "aon flare algo not suppress this darken event!");
                    }
                }
                SceneDetector.this.unregisterAonFlareListener();
            }
        }

        public void onImageAvailiable(int frameId) {
        }
    }

    /* loaded from: classes.dex */
    private final class SceneDetectorHandler extends Handler {
        public SceneDetectorHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (SceneDetector.this.mAonState == 2) {
                        SceneDetector.this.mBrightnessControllerImpl.notifyAonFlareEvents(3, SceneDetector.this.mPreAmbientLux);
                        if (SceneDetector.this.mIsMainDarkenEvent) {
                            SceneDetector.this.updateAutoBrightness();
                        }
                        SceneDetector.this.unregisterAonFlareListener();
                        return;
                    }
                    return;
                case 2:
                    SceneDetector.this.tryToBindAonFlareService();
                    return;
                case 3:
                    if (SceneDetector.this.mAonState == 2) {
                        SceneDetector.this.unregisterAonFlareListener();
                    }
                    SceneDetector.this.mIsMainDarkenEvent = false;
                    SceneDetector.this.resetServiceConnectedStatus();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Scene Detector State:");
        pw.println("  mMaxAonFlareEnableLux=" + this.mMaxAonFlareEnableLux);
        pw.println("  mMinAonFlareEnableLux=" + this.mMinAonFlareEnableLux);
        pw.println("  mAonFlareMaxDelayTime=" + this.mAonFlareMaxDelayTime);
        sDEBUG = DisplayDebugConfig.DEBUG_ABC;
    }
}
