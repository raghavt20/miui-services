package com.android.server.wm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewRootImpl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.RelativeLayout;
import com.miui.server.stability.DumpSysInfoUtil;
import com.xiaomi.abtest.d.d;
import miui.os.UserHandleEx;
import miuix.animation.Folme;
import miuix.animation.base.AnimConfig;
import miuix.animation.controller.AnimState;
import miuix.animation.listener.TransitionListener;
import miuix.animation.property.ViewProperty;

/* loaded from: classes.dex */
public class MiuiMultiWindowRecommendController {
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_OFFSETY = 25.0f;
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_OUTSET = 15.0f;
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_RADIUS = 350.0f;
    public static final int MULTI_WINDOW_RECOMMEND_SHADOW_V2_COLOR = 1929379840;
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_V2_DISPERSION = 1.0f;
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_Y = 20.0f;
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_V2_RADIUS = 350.0f;
    private static final String TAG = "MiuiMultiWindowRecommendController";
    private Context mContext;
    private RelativeLayout mFreeFormRecommendIconContainer;
    private FreeFormRecommendLayout mFreeFormRecommendLayout;
    private WindowManager.LayoutParams mLayoutParams;
    MiuiMultiWindowRecommendHelper mRecommendHelper;
    WindowManagerService mService;
    private AnimConfig mShowHideAnimConfig;
    private RelativeLayout mSplitScreenRecommendIconContainer;
    private SplitScreenRecommendLayout mSplitScreenRecommendLayout;
    WindowManager mWindowManager;
    private static final int MULTI_WINDOW_RECOMMEND_RADIUS = (int) TypedValue.applyDimension(1, 18.0f, Resources.getSystem().getDisplayMetrics());
    public static final float MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X = 0.0f;
    public static final float[] MULTI_WINDOW_RECOMMEND_SHADOW_RESET_COLOR = {MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X};
    public static final float[] MULTI_WINDOW_RECOMMEND_SHADOW_COLOR = {MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 0.4f};
    private RecommendDataEntry mSpiltScreenRecommendDataEntry = new RecommendDataEntry();
    private RecommendDataEntry mFreeFormRecommendDataEntry = new RecommendDataEntry();
    private long mRecommendAutoDisapperTime = 5000;
    private long mAnimationDelay = 1150;
    View.OnClickListener splitScreenClickListener = new View.OnClickListener() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.1
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            Slog.d(MiuiMultiWindowRecommendController.TAG, "click split screen recommend view");
            MiuiMultiWindowRecommendController.this.removeSplitScreenRecommendView();
            MiuiMultiWindowRecommendController.this.enterSplitScreen();
        }
    };
    Runnable removeSplitScreenRecommendRunnable = new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.2
        @Override // java.lang.Runnable
        public void run() {
            try {
                Slog.d(MiuiMultiWindowRecommendController.TAG, "removeSplitScreenRecommendRunnable");
                MiuiMultiWindowRecommendController.this.removeSplitScreenRecommendView();
            } catch (Exception e) {
                Slog.d(MiuiMultiWindowRecommendController.TAG, " removeSplitScreenRecommendRunnable error: ", e);
            }
        }
    };
    Runnable removeFreeFormRecomendRunnable = new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.3
        @Override // java.lang.Runnable
        public void run() {
            try {
                Slog.d(MiuiMultiWindowRecommendController.TAG, "removeFreeFormRecomendRunnable");
                MiuiMultiWindowRecommendController.this.removeFreeFormRecommendView();
            } catch (Exception e) {
                Slog.d(MiuiMultiWindowRecommendController.TAG, " removeFreeFormRecomendRunnable error: ", e);
            }
        }
    };
    private Runnable dismissSplitScreenRecommendViewRunnable = new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.4
        @Override // java.lang.Runnable
        public void run() {
            try {
                if (MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout != null) {
                    Slog.d(MiuiMultiWindowRecommendController.TAG, "dismissSplitScreenRecommendViewRunnable ");
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController.startMultiWindowRecommendAnimation(miuiMultiWindowRecommendController.mSplitScreenRecommendLayout, false);
                }
            } catch (Exception e) {
                Slog.d(MiuiMultiWindowRecommendController.TAG, " dismissSplitScreenRecommendViewRunnable error: ", e);
                e.printStackTrace();
            }
        }
    };
    private Runnable dismissFreeFormRecommendViewRunnable = new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.5
        @Override // java.lang.Runnable
        public void run() {
            try {
                if (MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout != null) {
                    Slog.d(MiuiMultiWindowRecommendController.TAG, "dismissFreeFormRecommendViewRunnable ");
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController.startMultiWindowRecommendAnimation(miuiMultiWindowRecommendController.mFreeFormRecommendLayout, false);
                }
            } catch (Exception e) {
                Slog.d(MiuiMultiWindowRecommendController.TAG, " dismissFreeFormRecommendViewRunnable error: ", e);
                e.printStackTrace();
            }
        }
    };
    View.OnClickListener freeFormClickListener = new View.OnClickListener() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.8
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            Slog.d(MiuiMultiWindowRecommendController.TAG, "click free form recommend view");
            MiuiMultiWindowRecommendController.this.removeFreeFormRecommendView();
            MiuiMultiWindowRecommendController.this.enterSmallFreeForm();
        }
    };

    public MiuiMultiWindowRecommendController(Context context, WindowManagerService service, MiuiMultiWindowRecommendHelper miuiMultiWindowRecommendHelper) {
        if (context != null) {
            this.mContext = context;
            this.mWindowManager = (WindowManager) context.getSystemService(DumpSysInfoUtil.WINDOW);
            this.mService = service;
            this.mRecommendHelper = miuiMultiWindowRecommendHelper;
            initFolmeConfig();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enterSplitScreen() {
        Slog.d(TAG, "enterSplitScreen ");
        if (this.mSpiltScreenRecommendDataEntry != null) {
            MiuiMultiWindowUtils.invoke(this.mService.mAtmService.mTaskOrganizerController, "startTasksForSystem", new Object[]{Integer.valueOf(this.mSpiltScreenRecommendDataEntry.getPrimaryTaskId()), Integer.valueOf(this.mSpiltScreenRecommendDataEntry.getSecondaryTaskId())});
            if (this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager != null) {
                this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager.trackSplitScreenRecommendClickEvent(this.mSpiltScreenRecommendDataEntry.getPrimaryPackageName() + d.h + this.mSpiltScreenRecommendDataEntry.getSecondaryPackageName(), getApplicationName(this.mSpiltScreenRecommendDataEntry.getPrimaryPackageName()) + d.h + getApplicationName(this.mSpiltScreenRecommendDataEntry.getSecondaryPackageName()));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enterSmallFreeForm() {
        Slog.d(TAG, "enterSmallFreeForm ");
        if (this.mFreeFormRecommendDataEntry != null) {
            MiuiMultiWindowUtils.invoke(this.mService.mAtmService, "launchMiniFreeFormWindowVersion2", new Object[]{Integer.valueOf(this.mFreeFormRecommendDataEntry.getFreeFormTaskId()), 2, "enterSmallFreeFormByFreeFormRecommend", new Rect()});
            if (this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager != null) {
                this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager.trackFreeFormRecommendClickEvent(this.mFreeFormRecommendDataEntry.getFreeformPackageName(), getApplicationName(this.mFreeFormRecommendDataEntry.getFreeformPackageName()));
            }
        }
    }

    public static Drawable loadDrawableByPackageName(String packageName, Context context, int userId) {
        try {
            PackageManager packageManager = context.getPackageManager();
            Drawable drawable = packageManager.getUserBadgedIcon(packageManager.getApplicationIcon(packageName), UserHandleEx.getUserHandle(userId));
            return drawable;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "not found application by pkgName " + packageName);
            return null;
        }
    }

    public void addSplitScreenRecommendView(final RecommendDataEntry recommendDataEntry) {
        Slog.d(TAG, " addSplitScreenRecommendView start");
        this.mService.mAtmService.mUiHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.6
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (!MiuiMultiWindowRecommendController.this.mRecommendHelper.hasNotification() && MiuiMultiWindowRecommendController.this.mRecommendHelper.isSplitScreenRecommendValid(recommendDataEntry)) {
                        MiuiMultiWindowRecommendController.this.mSpiltScreenRecommendDataEntry = recommendDataEntry;
                        MiuiMultiWindowRecommendController miuiMultiWindowRecommendController = MiuiMultiWindowRecommendController.this;
                        miuiMultiWindowRecommendController.mSplitScreenRecommendLayout = (SplitScreenRecommendLayout) LayoutInflater.from(miuiMultiWindowRecommendController.mContext).inflate(285999163, (ViewGroup) null);
                        int statusBarHeight = Math.max(MiuiMultiWindowRecommendController.getStatusBarHeight(MiuiMultiWindowRecommendController.this.mService.getDefaultDisplayContentLocked().getInsetsStateController(), false), MiuiMultiWindowRecommendController.getDisplayCutoutHeight(MiuiMultiWindowRecommendController.this.mService.getDefaultDisplayContentLocked().mDisplayFrames));
                        MiuiMultiWindowRecommendController miuiMultiWindowRecommendController2 = MiuiMultiWindowRecommendController.this;
                        miuiMultiWindowRecommendController2.mLayoutParams = miuiMultiWindowRecommendController2.mSplitScreenRecommendLayout.createLayoutParams(statusBarHeight);
                        MiuiMultiWindowRecommendController miuiMultiWindowRecommendController3 = MiuiMultiWindowRecommendController.this;
                        miuiMultiWindowRecommendController3.mSplitScreenRecommendIconContainer = miuiMultiWindowRecommendController3.mSplitScreenRecommendLayout.getSplitScreenIconContainer();
                        MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout.setSplitScreenIcon(MiuiMultiWindowRecommendController.loadDrawableByPackageName(recommendDataEntry.getPrimaryPackageName(), MiuiMultiWindowRecommendController.this.mContext, recommendDataEntry.getPrimaryUserId()), MiuiMultiWindowRecommendController.loadDrawableByPackageName(recommendDataEntry.getSecondaryPackageName(), MiuiMultiWindowRecommendController.this.mContext, recommendDataEntry.getSecondaryUserId()));
                        MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout.setOnClickListener(MiuiMultiWindowRecommendController.this.splitScreenClickListener);
                        Folme.useAt(new View[]{MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout}).touch().handleTouchOf(MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout, new AnimConfig[0]);
                        for (int i = 0; i < 2; i++) {
                            MiuiMultiWindowRecommendController.this.mWindowManager.addView(MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout, MiuiMultiWindowRecommendController.this.mLayoutParams);
                            if (WindowInspector.getGlobalWindowViews().contains(MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout)) {
                                MiuiMultiWindowRecommendController.this.setSplitScreenRecommendState(true);
                                MiuiMultiWindowRecommendController miuiMultiWindowRecommendController4 = MiuiMultiWindowRecommendController.this;
                                miuiMultiWindowRecommendController4.startMultiWindowRecommendAnimation(miuiMultiWindowRecommendController4.mSplitScreenRecommendLayout, true);
                                MiuiMultiWindowRecommendController.this.removeSplitScreenRecommendViewForTimer();
                                if (MiuiMultiWindowRecommendController.this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager != null) {
                                    MiuiFreeformTrackManager miuiFreeformTrackManager = MiuiMultiWindowRecommendController.this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager;
                                    String str = MiuiMultiWindowRecommendController.this.mSpiltScreenRecommendDataEntry.getPrimaryPackageName() + d.h + MiuiMultiWindowRecommendController.this.mSpiltScreenRecommendDataEntry.getSecondaryPackageName();
                                    StringBuilder sb = new StringBuilder();
                                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController5 = MiuiMultiWindowRecommendController.this;
                                    StringBuilder append = sb.append(miuiMultiWindowRecommendController5.getApplicationName(miuiMultiWindowRecommendController5.mSpiltScreenRecommendDataEntry.getPrimaryPackageName())).append(d.h);
                                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController6 = MiuiMultiWindowRecommendController.this;
                                    miuiFreeformTrackManager.trackSplitScreenRecommendExposeEvent(str, append.append(miuiMultiWindowRecommendController6.getApplicationName(miuiMultiWindowRecommendController6.mSpiltScreenRecommendDataEntry.getSecondaryPackageName())).toString());
                                    return;
                                }
                                return;
                            }
                        }
                        Slog.d(MiuiMultiWindowRecommendController.TAG, " addSplitScreenRecommendView twice fail ");
                        return;
                    }
                    Slog.d(MiuiMultiWindowRecommendController.TAG, "addSplitScreenRecommendView: hasNotification or invalid, return");
                    MiuiMultiWindowRecommendController.this.mRecommendHelper.setLastSplitScreenRecommendTime(System.currentTimeMillis());
                    MiuiMultiWindowRecommendController.this.mRecommendHelper.clearRecentAppList();
                } catch (Exception e) {
                    MiuiMultiWindowRecommendController.this.removeSplitScreenRecommendViewForTimer();
                    Slog.d(MiuiMultiWindowRecommendController.TAG, " addSplitScreenRecommendView fail", e);
                }
            }
        }, this.mAnimationDelay);
    }

    public void addFreeFormRecommendView(final RecommendDataEntry recommendDataEntry) {
        Slog.d(TAG, " addFreeFormRecommendView start");
        this.mService.mAtmService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.7
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MiuiMultiWindowRecommendController.this.mFreeFormRecommendDataEntry = recommendDataEntry;
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController.mFreeFormRecommendLayout = (FreeFormRecommendLayout) LayoutInflater.from(miuiMultiWindowRecommendController.mContext).inflate(285999119, (ViewGroup) null);
                    int statusBarHeight = Math.max(MiuiMultiWindowRecommendController.getStatusBarHeight(MiuiMultiWindowRecommendController.this.mService.getDefaultDisplayContentLocked().getInsetsStateController(), false), MiuiMultiWindowRecommendController.getDisplayCutoutHeight(MiuiMultiWindowRecommendController.this.mService.getDefaultDisplayContentLocked().mDisplayFrames));
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController2 = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController2.mLayoutParams = miuiMultiWindowRecommendController2.mFreeFormRecommendLayout.createLayoutParams(statusBarHeight);
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController3 = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController3.mFreeFormRecommendIconContainer = miuiMultiWindowRecommendController3.mFreeFormRecommendLayout.getFreeFormIconContainer();
                    MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout.setFreeFormIcon(MiuiMultiWindowRecommendController.loadDrawableByPackageName(recommendDataEntry.getFreeformPackageName(), MiuiMultiWindowRecommendController.this.mContext, recommendDataEntry.getFreeformUserId()));
                    MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout.setOnClickListener(MiuiMultiWindowRecommendController.this.freeFormClickListener);
                    Folme.useAt(new View[]{MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout}).touch().handleTouchOf(MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout, new AnimConfig[0]);
                    for (int i = 0; i < 2; i++) {
                        MiuiMultiWindowRecommendController.this.mWindowManager.addView(MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout, MiuiMultiWindowRecommendController.this.mLayoutParams);
                        if (WindowInspector.getGlobalWindowViews().contains(MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout)) {
                            MiuiMultiWindowRecommendController.this.setFreeFormRecommendState(true);
                            MiuiMultiWindowRecommendController miuiMultiWindowRecommendController4 = MiuiMultiWindowRecommendController.this;
                            miuiMultiWindowRecommendController4.startMultiWindowRecommendAnimation(miuiMultiWindowRecommendController4.mFreeFormRecommendLayout, true);
                            MiuiMultiWindowRecommendController.this.removeFreeFormRecommendViewForTimer();
                            if (MiuiMultiWindowRecommendController.this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager != null) {
                                MiuiMultiWindowRecommendController.this.mRecommendHelper.mFreeFormManagerService.mFreeFormGestureController.mTrackManager.trackFreeFormRecommendExposeEvent(recommendDataEntry.getFreeformPackageName(), MiuiMultiWindowRecommendController.this.getApplicationName(recommendDataEntry.getFreeformPackageName()));
                                return;
                            }
                            return;
                        }
                    }
                    Slog.d(MiuiMultiWindowRecommendController.TAG, " addFreeFormRecommendView twice fail");
                } catch (Exception e) {
                    MiuiMultiWindowRecommendController.this.removeFreeFormRecommendViewForTimer();
                    Slog.d(MiuiMultiWindowRecommendController.TAG, " addFreeFormRecommendView fail", e);
                }
            }
        });
    }

    public void removeSplitScreenRecommendViewForTimer() {
        Slog.d(TAG, "removeSplitScreenRecommendViewForTimer");
        this.mService.mAtmService.mUiHandler.removeCallbacks(this.dismissSplitScreenRecommendViewRunnable);
        this.mService.mAtmService.mUiHandler.postDelayed(this.dismissSplitScreenRecommendViewRunnable, this.mRecommendAutoDisapperTime);
    }

    public void removeFreeFormRecommendViewForTimer() {
        Slog.d(TAG, "removeFreeFormRecommendViewForTimer");
        this.mService.mAtmService.mUiHandler.removeCallbacks(this.dismissFreeFormRecommendViewRunnable);
        this.mService.mAtmService.mUiHandler.postDelayed(this.dismissFreeFormRecommendViewRunnable, this.mRecommendAutoDisapperTime);
    }

    public void initFolmeConfig() {
        this.mShowHideAnimConfig = new AnimConfig().setEase(-2, new float[]{0.75f, 0.35f}).addListeners(new TransitionListener[]{new TransitionListener() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.9
            public void onBegin(Object toTag) {
                Slog.d(MiuiMultiWindowRecommendController.TAG, "mShowHideAnimConfig onBegin: toTag= " + toTag);
                if ("freeFormRecommendShow".equals(toTag) && MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout != null) {
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController.setCornerRadiusAndShadow(miuiMultiWindowRecommendController.mFreeFormRecommendLayout);
                    return;
                }
                if ("splitScreenRecommendShow".equals(toTag) && MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout != null) {
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController2 = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController2.setCornerRadiusAndShadow(miuiMultiWindowRecommendController2.mSplitScreenRecommendLayout);
                } else if ("freeFormRecommendHide".equals(toTag) && MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout != null) {
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController3 = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController3.resetShadow(miuiMultiWindowRecommendController3.mFreeFormRecommendLayout);
                } else if ("splitScreenRecommendHide".equals(toTag) && MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout != null) {
                    MiuiMultiWindowRecommendController miuiMultiWindowRecommendController4 = MiuiMultiWindowRecommendController.this;
                    miuiMultiWindowRecommendController4.resetShadow(miuiMultiWindowRecommendController4.mSplitScreenRecommendLayout);
                }
            }

            public void onComplete(Object toTag) {
                super.onComplete(toTag);
                Slog.d(MiuiMultiWindowRecommendController.TAG, "mShowHideAnimConfig onComplete: toTag= " + toTag);
                if ("freeFormRecommendHide".equals(toTag) && MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout != null) {
                    MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout.setVisibility(4);
                    MiuiMultiWindowRecommendController.this.removeFreeFormRecommendView();
                } else if ("splitScreenRecommendHide".equals(toTag) && MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout != null) {
                    MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout.setVisibility(4);
                    MiuiMultiWindowRecommendController.this.removeSplitScreenRecommendView();
                }
            }
        }});
    }

    public void setCornerRadiusAndShadow(View view) {
        ViewRootImpl viewRootImpl;
        if (view != null && (viewRootImpl = view.getViewRootImpl()) != null) {
            Slog.d(TAG, " setCornerRadiusAndShadow: view= " + view);
            if (RecommendUtils.isSupportMiuiShadowV2()) {
                RecommendUtils.setMiShadowV2(viewRootImpl.getSurfaceControl(), MULTI_WINDOW_RECOMMEND_SHADOW_V2_COLOR, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 20.0f, 350.0f, 1.0f, MULTI_WINDOW_RECOMMEND_RADIUS, new RectF(), this.mContext);
            } else {
                RecommendUtils.setShadowSettingsInTransactionForSurfaceControl(viewRootImpl.getSurfaceControl(), 1, 350.0f, MULTI_WINDOW_RECOMMEND_SHADOW_COLOR, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 25.0f, 15.0f, 1);
            }
        }
    }

    public void resetShadow(View view) {
        ViewRootImpl viewRootImpl;
        if (view != null && (viewRootImpl = view.getViewRootImpl()) != null) {
            Slog.d(TAG, " resetShadow: view= " + view);
            if (RecommendUtils.isSupportMiuiShadowV2()) {
                RecommendUtils.resetMiShadowV2(viewRootImpl.getSurfaceControl(), this.mContext);
            } else {
                RecommendUtils.resetShadowSettingsInTransactionForSurfaceControl(viewRootImpl.getSurfaceControl(), 0, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_RESET_COLOR, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, 1);
            }
        }
    }

    public void startMultiWindowRecommendAnimation(View view, boolean appear) {
        Slog.d(TAG, " startMultiWindowRecommendAnimation view= " + view + " appear= " + appear);
        if (view == null) {
            return;
        }
        Folme.useAt(new View[]{view}).state().cancel();
        AnimState hiddenState = null;
        AnimState shownState = null;
        if (view instanceof FreeFormRecommendLayout) {
            hiddenState = new AnimState("freeFormRecommendHide").add(ViewProperty.SCALE_X, 0.949999988079071d).add(ViewProperty.SCALE_Y, 0.949999988079071d).add(ViewProperty.ALPHA, 0.0d);
            shownState = new AnimState("freeFormRecommendShow").add(ViewProperty.SCALE_X, 1.0d).add(ViewProperty.SCALE_Y, 1.0d).add(ViewProperty.ALPHA, 1.0d);
        } else if (view instanceof SplitScreenRecommendLayout) {
            hiddenState = new AnimState("splitScreenRecommendHide").add(ViewProperty.SCALE_X, 0.949999988079071d).add(ViewProperty.SCALE_Y, 0.949999988079071d).add(ViewProperty.ALPHA, 0.0d);
            shownState = new AnimState("splitScreenRecommendShow").add(ViewProperty.SCALE_X, 1.0d).add(ViewProperty.SCALE_Y, 1.0d).add(ViewProperty.ALPHA, 1.0d);
        }
        if (appear) {
            Folme.useAt(new View[]{view}).state().fromTo(hiddenState, shownState, new AnimConfig[]{this.mShowHideAnimConfig});
        } else {
            Folme.useAt(new View[]{view}).state().fromTo(shownState, hiddenState, new AnimConfig[]{this.mShowHideAnimConfig});
        }
    }

    public void removeSplitScreenRecommendView() {
        Slog.d(TAG, " removeSplitScreenRecommendView mSplitScreenRecommendLayout= " + this.mSplitScreenRecommendLayout);
        this.mService.mAtmService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.10
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout != null) {
                    try {
                        MiuiMultiWindowRecommendController.this.mWindowManager.removeView(MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout);
                        MiuiMultiWindowRecommendController.this.mLayoutParams = null;
                        MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout = null;
                        MiuiMultiWindowRecommendController.this.setSplitScreenRecommendState(false);
                    } catch (Exception e) {
                        MiuiMultiWindowRecommendController.this.mLayoutParams = null;
                        MiuiMultiWindowRecommendController.this.mSplitScreenRecommendLayout = null;
                        MiuiMultiWindowRecommendController.this.setSplitScreenRecommendState(false);
                        Slog.d(MiuiMultiWindowRecommendController.TAG, " removeSplitScreenRecommendView fail", e);
                    }
                }
            }
        });
    }

    public void removeFreeFormRecommendView() {
        Slog.d(TAG, " removeFreeFormRecommendView mFreeFormRecommendLayout= " + this.mFreeFormRecommendLayout);
        this.mService.mAtmService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.11
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout != null) {
                    try {
                        MiuiMultiWindowRecommendController.this.mWindowManager.removeView(MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout);
                        MiuiMultiWindowRecommendController.this.mLayoutParams = null;
                        MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout = null;
                        MiuiMultiWindowRecommendController.this.setFreeFormRecommendState(false);
                    } catch (Exception e) {
                        MiuiMultiWindowRecommendController.this.mLayoutParams = null;
                        MiuiMultiWindowRecommendController.this.mFreeFormRecommendLayout = null;
                        MiuiMultiWindowRecommendController.this.setFreeFormRecommendState(false);
                    }
                }
            }
        });
    }

    public SplitScreenRecommendLayout getSplitScreenRecommendLayout() {
        return this.mSplitScreenRecommendLayout;
    }

    private void setRadius(View content, final float radius) {
        content.setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.server.wm.MiuiMultiWindowRecommendController.12
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        content.setClipToOutline(true);
    }

    public boolean inSplitScreenRecommendState() {
        RecommendDataEntry recommendDataEntry = this.mSpiltScreenRecommendDataEntry;
        if (recommendDataEntry != null) {
            return recommendDataEntry.getSplitScreenRecommendState();
        }
        return false;
    }

    public void setSplitScreenRecommendState(boolean inRecommend) {
        Slog.d(TAG, "setSplitScreenRecommendState:  mSpiltScreenRecommendDataEntry= " + this.mSpiltScreenRecommendDataEntry + " inRecommend= " + inRecommend);
        RecommendDataEntry recommendDataEntry = this.mSpiltScreenRecommendDataEntry;
        if (recommendDataEntry != null) {
            recommendDataEntry.setSplitScreenRecommendState(inRecommend);
        }
    }

    public boolean inFreeFormRecommendState() {
        RecommendDataEntry recommendDataEntry = this.mFreeFormRecommendDataEntry;
        if (recommendDataEntry != null) {
            return recommendDataEntry.getFreeFormRecommendState();
        }
        return false;
    }

    public void setFreeFormRecommendState(boolean inRecommend) {
        RecommendDataEntry recommendDataEntry = this.mFreeFormRecommendDataEntry;
        if (recommendDataEntry != null) {
            recommendDataEntry.setFreeFormRecommendState(inRecommend);
        }
    }

    public void removeRecommendView() {
        removeFreeFormRecommendView();
        removeSplitScreenRecommendView();
    }

    public String getApplicationName(String packageName) {
        ApplicationInfo applicationInfo;
        PackageManager packageManager = null;
        try {
            packageManager = this.mService.mAtmService.mContext.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return "";
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    public static int getDisplayCutoutHeight(DisplayFrames displayFrames) {
        if (displayFrames == null) {
            return 0;
        }
        DisplayCutout cutout = displayFrames.mInsetsState.getDisplayCutout();
        if (displayFrames.mRotation == 0) {
            int displayCutoutHeight = cutout.getSafeInsetTop();
            return displayCutoutHeight;
        }
        if (displayFrames.mRotation == 2) {
            int displayCutoutHeight2 = cutout.getSafeInsetBottom();
            return displayCutoutHeight2;
        }
        if (displayFrames.mRotation == 1) {
            int displayCutoutHeight3 = cutout.getSafeInsetLeft();
            return displayCutoutHeight3;
        }
        if (displayFrames.mRotation != 3) {
            return 0;
        }
        int displayCutoutHeight4 = cutout.getSafeInsetRight();
        return displayCutoutHeight4;
    }

    public static int getStatusBarHeight(InsetsStateController insetsStateController, boolean ignoreVisibility) {
        Rect frame;
        if (insetsStateController == null) {
            return 0;
        }
        int statusBarHeight = 0;
        SparseArray<InsetsSourceProvider> providers = insetsStateController.getSourceProviders();
        for (int i = providers.size() - 1; i >= 0; i--) {
            InsetsSourceProvider provider = providers.valueAt(i);
            if (provider.getSource().getType() == WindowInsets.Type.statusBars() && ((ignoreVisibility || provider.getSource().isVisible()) && (frame = provider.getSource().getFrame()) != null && !frame.isEmpty())) {
                statusBarHeight = Math.min(frame.height(), frame.width());
            }
        }
        return statusBarHeight;
    }
}
