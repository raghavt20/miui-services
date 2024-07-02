package com.android.server.wm;

import android.app.WindowConfiguration;
import android.app.servertransaction.BoundsCompat;
import android.app.servertransaction.BoundsCompatInfoChangeItem;
import android.app.servertransaction.BoundsCompatStub;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.MiuiAppSizeCompatModeStub;
import android.view.WindowManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import java.io.PrintWriter;
import java.util.function.Consumer;

@MiuiStubHead(manifestName = "com.android.server.wm.BoundsCompatControllerStub$$")
/* loaded from: classes.dex */
public class BoundsCompatController extends BoundsCompatControllerStub {
    private Rect mBounds;
    private boolean mBoundsCompatEnabled;
    private boolean mDispatchNeeded;
    private Rect mDispatchedBounds;
    private int mDispatchedState;
    private boolean mFixedAspectRatioAvailable;
    private int mGravity;
    private ActivityRecord mOwner;
    private int mState;
    private float mFixedAspectRatio = -1.0f;
    private float mGlobalScale = -1.0f;
    private float mCurrentScale = 1.0f;

    /* loaded from: classes.dex */
    public @interface AspectRatioPolicy {
        public static final int DEFAULT = 0;
        public static final int FULL_SCREEN_MODE = 1;
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BoundsCompatController> {

        /* compiled from: BoundsCompatController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BoundsCompatController INSTANCE = new BoundsCompatController();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public BoundsCompatController m2456provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public BoundsCompatController m2455provideNewInstance() {
            return new BoundsCompatController();
        }
    }

    public void initBoundsCompatController(ActivityRecord owner) {
        this.mOwner = owner;
    }

    public void clearSizeCompatMode(Task task, final boolean isClearConfig, final ActivityRecord excluded) {
        task.mDisplayCompatAvailable = false;
        task.forAllActivities(new Consumer() { // from class: com.android.server.wm.BoundsCompatController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BoundsCompatController.lambda$clearSizeCompatMode$0(excluded, isClearConfig, (ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$clearSizeCompatMode$0(ActivityRecord excluded, boolean isClearConfig, ActivityRecord ar) {
        if (ar == excluded) {
            return;
        }
        if (!isClearConfig) {
            ar.clearCompatDisplayInsets();
            return;
        }
        ar.clearSizeCompatMode();
        if (!ar.attachedToProcess()) {
            return;
        }
        try {
            ar.mBoundsCompatControllerStub.interceptScheduleTransactionIfNeeded((ClientTransactionItem) null);
        } catch (RemoteException e) {
        }
    }

    private void applyPolicyIfNeeded(ActivityInfo info) {
        Task task;
        this.mFixedAspectRatioAvailable = false;
        float fixedAspectRatio = -1.0f;
        if (((!this.mOwner.mAtmService.mWindowManager.mPolicy.isDisplayFolded() && !MiuiAppSizeCompatModeStub.get().isFlip()) || MiuiAppSizeCompatModeStub.get().isFlipFolded()) && (task = this.mOwner.getTask()) != null && task.realActivity != null && !task.inMultiWindowMode()) {
            if (MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
                if (getActivityFlipCompatMode() == 0) {
                    fixedAspectRatio = MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X;
                } else {
                    fixedAspectRatio = 1.7206428f;
                }
            } else {
                fixedAspectRatio = ActivityTaskManagerServiceStub.get().getAspectRatio(info.packageName);
            }
            this.mGravity = ActivityTaskManagerServiceStub.get().getAspectGravity(info.packageName);
        }
        if (Float.compare(this.mFixedAspectRatio, fixedAspectRatio) != 0) {
            this.mFixedAspectRatio = fixedAspectRatio;
        }
        if (fixedAspectRatio != -1.0f && ActivityTaskManagerServiceStub.hasDefinedAspectRatio(fixedAspectRatio)) {
            this.mFixedAspectRatioAvailable = true;
        }
    }

    void addCallbackIfNeeded(ClientTransaction transaction) {
        if (this.mDispatchNeeded) {
            this.mDispatchNeeded = false;
            transaction.addCallback(BoundsCompatInfoChangeItem.obtain(this.mDispatchedState, this.mDispatchedBounds, this.mCurrentScale));
        }
    }

    void adjustWindowParamsIfNeededLocked(WindowState win, WindowManager.LayoutParams attrs) {
        if (win.mActivityRecord == null || !win.mActivityRecord.inMiuiSizeCompatMode()) {
            return;
        }
        if (win.mAttrs.layoutInDisplayCutoutMode == 0 || attrs.layoutInDisplayCutoutMode == 0) {
            win.mAttrs.layoutInDisplayCutoutMode = 1;
            attrs.layoutInDisplayCutoutMode = 1;
            attrs.flags |= 256;
        }
    }

    void clearAppBoundsIfNeeded(Configuration resolvedConfig) {
        Rect resolvedAppBounds = resolvedConfig.windowConfiguration.getAppBounds();
        if (resolvedAppBounds != null && BoundsCompatStub.get().isFixedAspectRatioModeEnabled(this.mState)) {
            resolvedAppBounds.setEmpty();
        }
    }

    void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "MiuiSizeCompat info:");
        pw.print(prefix + "  mState=0x" + Integer.toHexString(this.mState));
        pw.print(" mCompatBounds=" + this.mBounds);
        if (this.mDispatchNeeded) {
            pw.print(" mDispatchNeeded=true");
        }
        if (this.mState != this.mDispatchedState || !this.mBounds.equals(this.mDispatchedBounds)) {
            pw.print(" mDispatchedState=" + Integer.toHexString(this.mDispatchedState));
            pw.print(" mDispatchedBounds=" + this.mDispatchedBounds);
        }
        if (this.mFixedAspectRatio != -1.0f) {
            pw.print(" mFixedAspectRatio=" + this.mFixedAspectRatio);
        }
        if (this.mGlobalScale != -1.0f) {
            pw.println();
            pw.print(prefix + "  mGlobalScale=" + this.mGlobalScale);
            pw.print(" mCurrentScale=" + this.mCurrentScale);
        }
        pw.println();
    }

    private String aspectRatioPolicyToString(int policy) {
        switch (policy) {
            case 0:
                return "Default";
            case 1:
                return "FullScreen";
            default:
                return Integer.toString(policy);
        }
    }

    void dumpBounds(PrintWriter pw, String prefix, String title, WindowConfiguration winConfig) {
        pw.print(prefix + title);
        pw.print(" Bounds=");
        Rect bounds = winConfig.getBounds();
        pw.print("(" + bounds.left + "," + bounds.top + ")");
        pw.print("(" + bounds.width() + "x" + bounds.height() + ")");
        Rect appBounds = winConfig.getAppBounds();
        if (appBounds != null) {
            pw.print(" AppBounds=");
            pw.print("(" + appBounds.left + "," + appBounds.top + ")");
            pw.print("(" + appBounds.width() + "x" + appBounds.height() + ")");
        }
        pw.println();
    }

    public boolean interceptScheduleTransactionIfNeeded(ClientTransactionItem originalTransactionItem) throws RemoteException {
        if (!this.mDispatchNeeded) {
            return false;
        }
        this.mDispatchNeeded = false;
        ClientTransaction transaction = ClientTransaction.obtain(this.mOwner.app.getThread(), this.mOwner.token);
        transaction.addCallback(BoundsCompatInfoChangeItem.obtain(this.mDispatchedState, this.mDispatchedBounds, this.mCurrentScale));
        if (originalTransactionItem != null) {
            transaction.addCallback(originalTransactionItem);
        }
        this.mOwner.mAtmService.getLifecycleManager().scheduleTransaction(transaction);
        return true;
    }

    boolean isBoundsCompatEnabled() {
        return this.mBoundsCompatEnabled;
    }

    boolean isDisplayCompatAvailable(Task task) {
        if ((task.mAtmService.mWindowManager.mPolicy.isDisplayFolded() && !MiuiAppSizeCompatModeStub.get().isFlip()) || !MiuiAppSizeCompatModeStub.get().isFlipFolded()) {
            if (task.getDisplayId() != 0 || !task.isActivityTypeStandard() || task.inMultiWindowMode() || ActivityTaskManagerServiceStub.isForcedResizeableDisplayCompatPolicy(task.mDisplayCompatPolicy)) {
                return false;
            }
            if (ActivityTaskManagerServiceStub.isForcedUnresizeableDisplayCompatPolicy(task.mDisplayCompatPolicy)) {
                return true;
            }
            return (task.mResizeMode == 2 || task.mResizeMode == 1) ? false : true;
        }
        if (task.mDisplayCompatAvailable && task.inMultiWindowMode()) {
            return false;
        }
        return task.mDisplayCompatAvailable;
    }

    public boolean isFixedAspectRatioAvailable() {
        return this.mFixedAspectRatioAvailable;
    }

    public boolean isFixedAspectRatioEnabled() {
        return BoundsCompat.getInstance().isFixedAspectRatioModeEnabled(this.mState);
    }

    public void prepareBoundsCompat() {
        this.mState = 0;
        if (this.mOwner.isActivityTypeStandardOrUndefined() || (MiuiAppSizeCompatModeStub.get().isFlip() && this.mOwner.getActivityType() == 2)) {
            applyPolicyIfNeeded(this.mOwner.info);
        }
    }

    boolean inMiuiSizeCompatMode() {
        Rect rect = this.mDispatchedBounds;
        return (rect == null || rect.isEmpty()) ? false : true;
    }

    float getCurrentMiuiSizeCompatScale(float currentScale) {
        return this.mCurrentScale;
    }

    int shouldRelaunchInMiuiSizeCompatMode(int changes, int configChanged, int lastReportedDisplayId) {
        if (this.mOwner.getDisplayId() == lastReportedDisplayId && (changes & 128) != 0) {
            return configChanged | 3328;
        }
        return configChanged;
    }

    boolean shouldNotCreateCompatDisplayInsets() {
        return !this.mOwner.mAtmService.mForceResizableActivities && BoundsCompatStub.get().isFixedAspectRatioModeEnabled(this.mState);
    }

    boolean areBoundsLetterboxed() {
        return inMiuiSizeCompatMode() && this.mOwner.areBoundsLetterboxed();
    }

    boolean shouldShowLetterboxUi() {
        Task task;
        ActivityRecord activityRecord = this.mOwner;
        if (activityRecord != null && activityRecord.inMiuiSizeCompatMode() && (task = this.mOwner.getTask()) != null) {
            return taskContainsActivityRecord(task);
        }
        return false;
    }

    private boolean taskContainsActivityRecord(Task task) {
        for (int j = 0; j < task.getChildCount(); j++) {
            if (task.isLeafTask()) {
                ActivityRecord ar = task.getChildAt(j).asActivityRecord();
                if (ar != null && ar.isVisible()) {
                    ActivityRecord activityRecord = this.mOwner;
                    if (ar == activityRecord) {
                        return true;
                    }
                    int currentOrientation = activityRecord.getResolvedOverrideConfiguration().orientation;
                    int bottomOrientation = ar.getResolvedOverrideConfiguration().orientation;
                    return (currentOrientation == 0 || bottomOrientation == 0 || currentOrientation == bottomOrientation) ? false : true;
                }
            } else {
                for (int i = task.getChildCount() - 1; i >= 0; i--) {
                    Task t = task.getChildAt(i).asTask();
                    if (t != null && task.isVisible()) {
                        return taskContainsActivityRecord(t);
                    }
                }
            }
        }
        return false;
    }

    void resolveBoundsCompat(Configuration resolvedConfig, Configuration parentConfig, Rect sizeCompatBounds, float sizeCompatScale) {
        int activityMode = BoundsCompatUtils.getInstance().getFlipCompatModeByActivity(this.mOwner);
        int appMode = BoundsCompatUtils.getInstance().getFlipCompatModeByApp(this.mOwner.mAtmService, this.mOwner.packageName);
        if (appMode != 0 && activityMode == 0) {
            this.mDispatchNeeded = true;
        }
        boolean isFixedAspectRatioModeEnabled = BoundsCompat.getInstance().isFixedAspectRatioModeEnabled(this.mState);
        this.mBoundsCompatEnabled = isFixedAspectRatioModeEnabled;
        if (isFixedAspectRatioModeEnabled) {
            if (this.mBounds == null) {
                this.mBounds = new Rect();
            }
            this.mBounds.set(resolvedConfig.windowConfiguration.getBounds());
            if (this.mDispatchedBounds == null) {
                this.mDispatchedBounds = new Rect();
            }
            if (!this.mDispatchedBounds.equals(this.mBounds)) {
                this.mDispatchedBounds.set(this.mBounds);
                this.mDispatchNeeded = true;
            }
        } else if (BoundsCompat.getInstance().isFixedAspectRatioModeEnabled(this.mDispatchedState)) {
            this.mBounds.setEmpty();
            this.mDispatchedBounds.setEmpty();
            this.mDispatchNeeded = true;
        }
        int i = this.mState;
        if (i != this.mDispatchedState) {
            this.mDispatchedState = i;
            this.mDispatchNeeded = true;
        }
    }

    public Rect getDispatchedBounds() {
        return this.mDispatchedBounds;
    }

    public void updateDisplayCompatModeAvailableIfNeeded(Task task) {
        int policy;
        if (task.realActivity != null && task.mDisplayCompatPolicy != (policy = ActivityTaskManagerServiceStub.get().getPolicy(task.realActivity.getPackageName()))) {
            task.mDisplayCompatPolicy = policy;
        }
        boolean isAvailable = isDisplayCompatAvailable(task);
        if (task.mDisplayCompatAvailable != isAvailable) {
            task.mDisplayCompatAvailable = isAvailable;
            if (!isAvailable) {
                clearSizeCompatMode(task, false);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x0087  */
    /* JADX WARN: Removed duplicated region for block: B:22:0x00a5  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x00b5  */
    /* JADX WARN: Removed duplicated region for block: B:28:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    boolean adaptCompatConfiguration(android.content.res.Configuration r9, android.content.res.Configuration r10, com.android.server.wm.DisplayContent r11) {
        /*
            r8 = this;
            boolean r0 = r8.canUseFixedAspectRatio(r9)
            r1 = 0
            r2 = 1065353216(0x3f800000, float:1.0)
            r3 = -1082130432(0xffffffffbf800000, float:-1.0)
            if (r0 == 0) goto Lbe
            com.android.server.wm.ActivityRecord r0 = r8.mOwner
            r0.clearSizeCompatModeWithoutConfigChange()
            android.app.WindowConfiguration r0 = r9.windowConfiguration
            android.graphics.Rect r0 = r0.getAppBounds()
            if (r11 == 0) goto Lbd
            android.view.DisplayInfo r4 = r11.mDisplayInfo
            if (r4 != 0) goto L1e
            goto Lbd
        L1e:
            float r1 = r8.mGlobalScale
            int r1 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
            if (r1 != 0) goto L4a
            android.view.DisplayInfo r1 = r11.mDisplayInfo
            java.lang.String r1 = r1.uniqueId
            java.lang.String r4 = r11.mCurrentUniqueDisplayId
            boolean r1 = r1.equals(r4)
            if (r1 == 0) goto L4a
            com.android.server.wm.BoundsCompatUtils r1 = com.android.server.wm.BoundsCompatUtils.getInstance()
            com.android.server.wm.ActivityRecord r4 = r8.mOwner
            android.content.pm.ActivityInfo r4 = r4.info
            java.lang.String r4 = r4.packageName
            int r5 = r8.getActivityFlipCompatMode()
            android.app.WindowConfiguration r6 = r9.windowConfiguration
            android.graphics.Rect r6 = r6.getBounds()
            float r1 = r1.getGlobalScaleByName(r4, r5, r6)
            r8.mGlobalScale = r1
        L4a:
            int r1 = r9.orientation
            r4 = 2
            if (r1 == r4) goto L59
            android.util.MiuiAppSizeCompatModeStub r1 = android.util.MiuiAppSizeCompatModeStub.get()
            boolean r1 = r1.isFlipFolded()
            if (r1 == 0) goto L62
        L59:
            float r1 = r8.mGlobalScale
            int r3 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1))
            if (r3 == 0) goto L62
            r8.mCurrentScale = r1
            goto L64
        L62:
            r8.mCurrentScale = r2
        L64:
            android.app.WindowConfiguration r1 = r9.windowConfiguration
            r2 = 17
            int r1 = com.android.server.wm.BoundsCompatUtils.getCompatGravity(r1, r2)
            r8.mGravity = r1
            android.app.servertransaction.BoundsCompat r2 = android.app.servertransaction.BoundsCompat.getInstance()
            float r3 = r8.mFixedAspectRatio
            com.android.server.wm.ActivityRecord r1 = r8.mOwner
            int r5 = r8.getRequestedConfigurationOrientation(r1)
            int r6 = r8.mGravity
            float r7 = r8.mCurrentScale
            r4 = r9
            android.graphics.Rect r1 = r2.computeCompatBounds(r3, r4, r5, r6, r7)
            int r2 = r10.densityDpi
            if (r2 != 0) goto L8b
            int r2 = r9.densityDpi
            r10.densityDpi = r2
        L8b:
            android.app.WindowConfiguration r2 = r10.windowConfiguration
            r2.setBounds(r1)
            android.app.WindowConfiguration r2 = r10.windowConfiguration
            r2.setAppBounds(r1)
            android.util.MiuiAppSizeCompatModeImpl r2 = android.util.MiuiAppSizeCompatModeImpl.getInstance()
            com.android.server.wm.ActivityRecord r3 = r8.mOwner
            java.lang.String r3 = r3.getName()
            boolean r2 = r2.shouldUseMaxBoundsFullscreen(r3)
            if (r2 != 0) goto Laa
            android.app.WindowConfiguration r3 = r10.windowConfiguration
            r3.setMaxBounds(r1)
        Laa:
            com.android.server.wm.BoundsCompatUtils r3 = com.android.server.wm.BoundsCompatUtils.getInstance()
            r3.adaptCompatBounds(r10, r11)
            boolean r3 = r8.mFixedAspectRatioAvailable
            if (r3 == 0) goto Lbb
            int r3 = r8.mState
            r3 = r3 | 8
            r8.mState = r3
        Lbb:
            r3 = 1
            return r3
        Lbd:
            return r1
        Lbe:
            r8.mGlobalScale = r3
            r8.mCurrentScale = r2
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.BoundsCompatController.adaptCompatConfiguration(android.content.res.Configuration, android.content.res.Configuration, com.android.server.wm.DisplayContent):boolean");
    }

    private int getRequestedConfigurationOrientation(ActivityRecord ar) {
        int requestedOrientation;
        int sensorRotation;
        boolean useOriginRequestOrientation = ar.mOriginRequestOrientation == ar.mOriginScreenOrientation || ar.mOriginRequestOrientation != -2;
        if (useOriginRequestOrientation) {
            requestedOrientation = ar.mOriginRequestOrientation;
        } else {
            requestedOrientation = ar.mOriginScreenOrientation;
        }
        if (requestedOrientation == 5) {
            if (ar.mDisplayContent != null) {
                return ar.mDisplayContent.getNaturalOrientation();
            }
        } else {
            if (requestedOrientation == 14) {
                return ar.getConfiguration().orientation;
            }
            if (ActivityInfo.isFixedOrientationLandscape(requestedOrientation)) {
                return 2;
            }
            if (ActivityInfo.isFixedOrientationPortrait(requestedOrientation)) {
                return 1;
            }
            if (requestedOrientation == 4 && ar.mDisplayContent != null) {
                DisplayRotation displayRotation = ar.mDisplayContent.getDisplayRotation();
                WindowOrientationListener listener = displayRotation != null ? displayRotation.getOrientationListener() : null;
                if (listener != null) {
                    sensorRotation = listener.getProposedRotation();
                } else {
                    sensorRotation = -1;
                }
                int sensorOrientation = rotationToOrientation(sensorRotation);
                if (sensorOrientation != -1) {
                    return sensorOrientation;
                }
            }
        }
        return 0;
    }

    private int rotationToOrientation(int rotation) {
        switch (rotation) {
            case 0:
            case 2:
                return 1;
            case 1:
            case 3:
                return 2;
            default:
                return -1;
        }
    }

    private boolean canUseFixedAspectRatio(Configuration parentConfiguration) {
        if (this.mFixedAspectRatioAvailable) {
            if (MiuiAppSizeCompatModeStub.get().isFlip() || this.mOwner.getUid() >= 10000) {
                if (!MiuiAppSizeCompatModeStub.get().isFlipFolded() || !ActivityTaskManagerServiceStub.get().shouldNotApplyAspectRatio(this.mOwner)) {
                    if (MiuiAppSizeCompatModeStub.get().isFlipFolded() && getActivityFlipCompatMode() == 0) {
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

    public boolean isDebugEnable() {
        return MiuiSizeCompatService.DEBUG;
    }

    public Rect getScaledBounds(Rect bounds) {
        Rect realFrame = new Rect();
        realFrame.set(bounds);
        realFrame.scale(this.mCurrentScale);
        realFrame.offsetTo(bounds.left, bounds.top);
        return realFrame;
    }

    private int getActivityFlipCompatMode() {
        int activityMode = BoundsCompatUtils.getInstance().getFlipCompatModeByActivity(this.mOwner);
        int appMode = BoundsCompatUtils.getInstance().getFlipCompatModeByApp(this.mOwner.mAtmService, this.mOwner.packageName);
        if (activityMode != -1) {
            return activityMode;
        }
        if (appMode == -1) {
            return -1;
        }
        return appMode;
    }
}
