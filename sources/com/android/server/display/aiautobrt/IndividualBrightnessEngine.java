package com.android.server.display.aiautobrt;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.display.DisplayDebugConfig;
import com.android.server.display.aiautobrt.IndividualBrightnessEngine;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.xiaomi.aiautobrt.IIndividualBrightnessService;
import com.xiaomi.aiautobrt.IIndividualCallback;
import com.xiaomi.aiautobrt.IndividualModelEvent;
import com.xiaomi.aiautobrt.IndividualTrainEvent;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

/* loaded from: classes.dex */
public class IndividualBrightnessEngine {
    private static final String ACTION_CLIENT_SERVICE = "com.miui.aiautobrt.service.AiService";
    protected static final String MODEL_STATE_BEST_INDICATOR = "best_indicator";
    protected static final String MODEL_STATE_REASON_BACKUP = "backup";
    protected static final String MODEL_STATE_REASON_DEFAULT = "default";
    protected static final String MODEL_STATE_REASON_FORCED = "forced_operate";
    protected static final String MODEL_STATE_REASON_TRAIN_FINISHED = "train_finished";
    protected static final String MODEL_STATE_REASON_USER = "user_operate";
    private static final String TAG = "CbmController-IndividualEngine";
    private static boolean sDebug;
    private final Handler mBgHandler;
    private final Context mContext;
    private CustomPersistentDataStore mDataStore;
    private final EngineCallback mEngineCallback;
    private final Handler mHandler;
    private final IndividualEventNormalizer mIndividualEventNormalizer;
    private final ComponentName mModelComponent;
    private volatile IIndividualBrightnessService mModelService;
    private boolean mModelValid;
    private volatile boolean mModelValidationInProgress;
    private volatile boolean mNeedBindService;
    private String mModelValidStateReason = "default";
    private final IIndividualCallback mIndividualCallback = new AnonymousClass1();
    private final ModelBindRecord mModelBindRecord = new ModelBindRecord();

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface EngineCallback {
        void onAbTestExperimentUpdated(int i, int i2);

        void onExperimentUpdated(int i, boolean z);

        void onPredictFinished(float f, int i, float f2);

        void onTrainIndicatorsFinished(IndividualTrainEvent individualTrainEvent);

        void onValidatedBrightness(float f);

        void validateModelMonotonicity();
    }

    public IndividualBrightnessEngine(Context context, IndividualEventNormalizer normalizer, Looper looper, ComponentName component, EngineCallback callback, Handler bgHandler) {
        this.mContext = context;
        this.mIndividualEventNormalizer = normalizer;
        this.mHandler = new Handler(looper);
        this.mBgHandler = bgHandler;
        this.mModelComponent = component;
        this.mEngineCallback = callback;
    }

    public void uploadBrightnessModelEvent(IndividualModelEvent event, boolean enable) {
        if (this.mModelService == null || !this.mModelBindRecord.mIsBound) {
            tryToBindModelService();
            return;
        }
        if (enable) {
            try {
                this.mModelService.onEventChanged(event);
                if (sDebug) {
                    Slog.d(TAG, "uploadBrightnessModelEvent: event: " + event);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, e.toString());
            }
        }
    }

    public void preparePredictBrightness(IndividualModelEvent event) {
        if (this.mModelService == null || !this.mModelBindRecord.mIsBound) {
            tryToBindModelService();
            return;
        }
        try {
            if (!this.mModelValidationInProgress) {
                Slog.d(TAG, "preparePredictBrightness: event: " + event);
            }
            this.mModelService.predictBrightness(event);
        } catch (RemoteException e) {
            Slog.e(TAG, e.toString());
        }
    }

    private void tryToBindModelService() {
        if (this.mModelComponent == null || !this.mNeedBindService) {
            return;
        }
        if (this.mModelBindRecord.mIsBound && this.mModelService != null) {
            return;
        }
        Slog.i(TAG, "tryToBindModelService: try to bind model service.");
        int userId = ActivityManager.getCurrentUser();
        Intent intent = new Intent(ACTION_CLIENT_SERVICE);
        intent.setComponent(this.mModelComponent);
        intent.addFlags(SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX);
        if (!this.mContext.bindServiceAsUser(intent, this.mModelBindRecord, 67108865, new UserHandle(userId))) {
            Slog.e(TAG, "Unable to bind service: bindService failed " + intent);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void attach(IBinder service) {
        try {
            this.mModelService = IIndividualBrightnessService.Stub.asInterface(service);
            this.mModelService.asBinder().linkToDeath(this.mModelBindRecord, 0);
            this.mModelService.attach(this.mIndividualCallback);
            this.mModelService.provideInterfaceVersion(2);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to bind service bound!");
        }
        this.mModelBindRecord.mIsBound = true;
        Slog.w(TAG, "Service has bound successfully.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void detach() {
        if (this.mModelService != null) {
            try {
                this.mModelService.asBinder().unlinkToDeath(this.mModelBindRecord, 0);
                this.mModelService.detach();
            } catch (RemoteException | NoSuchElementException e) {
                Slog.e(TAG, "Process of service has died, detach from it.");
            }
            this.mContext.unbindService(this.mModelBindRecord);
            this.mModelBindRecord.mIsBound = false;
            this.mModelService = null;
            this.mNeedBindService = false;
            Slog.w(TAG, "Process of service has died, detach from it.");
        }
    }

    public void onBootCompleted() {
        Slog.i(TAG, "onBootCompleted: boot completed.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.aiautobrt.IndividualBrightnessEngine$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends IIndividualCallback.Stub {
        AnonymousClass1() {
        }

        public void onTrainFinished() throws RemoteException {
            IndividualBrightnessEngine.this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    IndividualBrightnessEngine.AnonymousClass1.this.lambda$onTrainFinished$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onTrainFinished$0() {
            Slog.d(IndividualBrightnessEngine.TAG, "Model train is finished.");
            IndividualBrightnessEngine.this.mModelValidationInProgress = true;
            IndividualBrightnessEngine.this.mEngineCallback.validateModelMonotonicity();
        }

        public void onPredictFinished(float lux, float appId, final float brightness) throws RemoteException {
            if (IndividualBrightnessEngine.this.mModelValidationInProgress) {
                IndividualBrightnessEngine.this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$1$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        IndividualBrightnessEngine.AnonymousClass1.this.lambda$onPredictFinished$1(brightness);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onPredictFinished$1(float brightness) {
            IndividualBrightnessEngine.this.mEngineCallback.onValidatedBrightness(brightness);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onUpdateExperiment$2(int expId, boolean enable) {
            IndividualBrightnessEngine.this.mEngineCallback.onExperimentUpdated(expId, enable);
        }

        public void onUpdateExperiment(final int expId, final boolean enable) throws RemoteException {
            IndividualBrightnessEngine.this.mHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    IndividualBrightnessEngine.AnonymousClass1.this.lambda$onUpdateExperiment$2(expId, enable);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onTrainIndicatorsFinished$3(IndividualTrainEvent event) {
            IndividualBrightnessEngine.this.mEngineCallback.onTrainIndicatorsFinished(event);
        }

        public void onTrainIndicatorsFinished(final IndividualTrainEvent event) throws RemoteException {
            IndividualBrightnessEngine.this.mBgHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    IndividualBrightnessEngine.AnonymousClass1.this.lambda$onTrainIndicatorsFinished$3(event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onAbTestExperimentUpdated$4(int expId, int flag) {
            IndividualBrightnessEngine.this.mEngineCallback.onAbTestExperimentUpdated(expId, flag);
        }

        public void onAbTestExperimentUpdated(final int expId, final int flag) throws RemoteException {
            IndividualBrightnessEngine.this.mHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    IndividualBrightnessEngine.AnonymousClass1.this.lambda$onAbTestExperimentUpdated$4(expId, flag);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ModelBindRecord implements ServiceConnection, IBinder.DeathRecipient {
        private volatile boolean mIsBound;

        private ModelBindRecord() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, final IBinder service) {
            if (service != null) {
                IndividualBrightnessEngine.this.mHandler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$ModelBindRecord$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        IndividualBrightnessEngine.ModelBindRecord.this.lambda$onServiceConnected$0(service);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onServiceConnected$0(IBinder service) {
            IndividualBrightnessEngine.this.attach(service);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Slog.d(IndividualBrightnessEngine.TAG, "Service disconnected.");
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Handler handler = IndividualBrightnessEngine.this.mHandler;
            final IndividualBrightnessEngine individualBrightnessEngine = IndividualBrightnessEngine.this;
            handler.post(new Runnable() { // from class: com.android.server.display.aiautobrt.IndividualBrightnessEngine$ModelBindRecord$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    IndividualBrightnessEngine.this.detach();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isModelValid() {
        return this.mModelValid;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setModelValid(boolean valid, String reason) {
        if (valid && !this.mModelValid) {
            this.mModelValid = true;
            this.mModelValidStateReason = reason;
            CustomPersistentDataStore customPersistentDataStore = this.mDataStore;
            if (customPersistentDataStore != null) {
                customPersistentDataStore.storeIndividualModelEnabled(true);
            }
            Slog.d(TAG, "setModelValid: model is valid due to: " + reason);
            return;
        }
        if (!valid && this.mModelValid) {
            this.mModelValid = false;
            this.mModelValidStateReason = reason;
            CustomPersistentDataStore customPersistentDataStore2 = this.mDataStore;
            if (customPersistentDataStore2 != null) {
                customPersistentDataStore2.storeIndividualModelEnabled(false);
            }
            Slog.d(TAG, "setModelInvalid: model is invalid due to: " + reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setModeValidFromXml(boolean enabled) {
        this.mModelValid = enabled;
        this.mModelValidStateReason = MODEL_STATE_REASON_BACKUP;
        Slog.d(TAG, "setModeValidFromXml: model is " + (this.mModelValid ? " valid" : " invalid") + " due to: " + MODEL_STATE_REASON_BACKUP);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setDataStore(CustomPersistentDataStore dataStore) {
        this.mDataStore = dataStore;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void completeModelValidation() {
        this.mModelValidationInProgress = false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isVerificationInProgress() {
        return this.mModelValidationInProgress;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void bindServiceDueToBrightnessAdjust(boolean needBindService) {
        if (this.mNeedBindService != needBindService) {
            this.mNeedBindService = needBindService;
            Slog.d(TAG, "Try to bind service due to brightness adjust.");
            tryToBindModelService();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        sDebug = DisplayDebugConfig.DEBUG_CBM;
        pw.println("  mModelService=" + this.mModelService);
        pw.println("  isBound=" + this.mModelBindRecord.mIsBound);
        pw.println("  mModelValid=" + this.mModelValid);
        pw.println("  mModelValidStateReason=" + this.mModelValidStateReason);
        pw.println("  mModelValidationInProgress=" + this.mModelValidationInProgress);
    }
}
