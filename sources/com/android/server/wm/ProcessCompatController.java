package com.android.server.wm;

import android.app.servertransaction.BoundsCompatInfoChangeItem;
import android.app.servertransaction.BoundsCompatStub;
import android.app.servertransaction.ClientTransaction;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.List;

/* loaded from: classes.dex */
public class ProcessCompatController implements ProcessCompatControllerStub {
    private static final String TAG = "ProcessCompat";
    private ActivityTaskManagerService mAtm;
    private float mFixedAspectRatio;
    private WindowProcessController mProcess;
    private int mState = 0;
    private Rect mBounds = new Rect(0, 0, 1, 1);
    private float mGlobalScale = -1.0f;
    private float mCurrentScale = 1.0f;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessCompatController> {

        /* compiled from: ProcessCompatController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessCompatController INSTANCE = new ProcessCompatController();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ProcessCompatController m2763provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ProcessCompatController m2762provideNewInstance() {
            return new ProcessCompatController();
        }
    }

    public void initBoundsCompatController(ActivityTaskManagerService atm, WindowProcessController process) {
        this.mAtm = atm;
        this.mProcess = process;
        String packageName = process.mInfo.packageName;
        if (MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            if (BoundsCompatUtils.getInstance().getFlipCompatModeByApp(this.mAtm, packageName) == 0) {
                this.mFixedAspectRatio = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
            } else {
                this.mFixedAspectRatio = 1.7206428f;
            }
        } else {
            this.mFixedAspectRatio = ActivityTaskManagerServiceStub.get().getAspectRatio(packageName);
        }
        updateState();
    }

    public Configuration getCompatConfiguration(Configuration config) {
        if (isFixedAspectRatioEnabled()) {
            if (this.mGlobalScale == -1.0f) {
                int mode = BoundsCompatUtils.getInstance().getFlipCompatModeByApp(this.mAtm, this.mProcess.mInfo.packageName);
                this.mGlobalScale = BoundsCompatUtils.getInstance().getGlobalScaleByName(this.mProcess.mName, mode, config.windowConfiguration.getBounds());
            }
            int mode2 = config.orientation;
            if (mode2 == 2 || MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
                float f = this.mGlobalScale;
                if (f != -1.0f) {
                    this.mCurrentScale = f;
                    return BoundsCompatUtils.getInstance().getCompatConfiguration(config, this.mFixedAspectRatio, this.mAtm.mWindowManager.getDefaultDisplayContentLocked(), this.mCurrentScale);
                }
            }
            this.mCurrentScale = 1.0f;
            return BoundsCompatUtils.getInstance().getCompatConfiguration(config, this.mFixedAspectRatio, this.mAtm.mWindowManager.getDefaultDisplayContentLocked(), this.mCurrentScale);
        }
        return config;
    }

    public boolean isFixedAspectRatioEnabled() {
        return BoundsCompatStub.get().isFixedAspectRatioModeEnabled(this.mState);
    }

    public void sendCompatState() {
        if (this.mProcess.getThread() == null) {
            return;
        }
        ClientTransaction transaction = ClientTransaction.obtain(this.mProcess.getThread(), (IBinder) null);
        transaction.addCallback(BoundsCompatInfoChangeItem.obtain(this.mState, this.mBounds, this.mCurrentScale));
        try {
            this.mAtm.getLifecycleManager().scheduleTransaction(transaction);
        } catch (RemoteException e) {
        }
    }

    public void updateConfiguration(Configuration config) {
        updateState();
    }

    public void onSetThread() {
        if (this.mState != 0) {
            sendCompatState();
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        if (this.mGlobalScale == -1.0f) {
            return;
        }
        synchronized (this) {
            pw.println(prefix + "MiuiSizeCompat Scale:");
            pw.print(prefix + " mGlobalScale=" + this.mGlobalScale + " mCurrentScale=" + this.mCurrentScale);
            pw.println();
        }
    }

    private void updateState() {
        int newState;
        int newState2 = this.mState;
        if (canUseFixedAspectRatio()) {
            newState = newState2 | 8;
            Slog.i(TAG, "Set " + this.mProcess.mName + " fixed-aspect-ratio " + this.mFixedAspectRatio);
        } else {
            newState = newState2 & (-9);
        }
        if (this.mState != newState) {
            Slog.i(TAG, "Update " + this.mProcess.mName + " comapt state " + this.mState + "->" + newState);
            this.mState = newState;
            sendCompatState();
        }
    }

    public boolean canUseFixedAspectRatio() {
        if (this.mFixedAspectRatio > MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X) {
            if (MiuiAppSizeCompatModeStub.get().isFlip() || this.mProcess.mUid >= 10000) {
                if ((!this.mAtm.mWindowManager.mPolicy.isDisplayFolded() || MiuiAppSizeCompatModeStub.get().isFlipFolded()) && !shouldNotUseFixedAspectRatio()) {
                    if (MiuiAppSizeCompatModeStub.get().isFlipFolded() && BoundsCompatUtils.getInstance().getFlipCompatModeByApp(this.mAtm, this.mProcess.mInfo.packageName) == 0) {
                        return false;
                    }
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public boolean shouldNotUseFixedAspectRatio() {
        List<ActivityRecord> mActivities = this.mProcess.getActivities();
        if (mActivities.isEmpty()) {
            return false;
        }
        int lastIndex = mActivities.size() - 1;
        ActivityRecord topRecord = mActivities.get(lastIndex);
        return topRecord.inFreeformWindowingMode();
    }
}
