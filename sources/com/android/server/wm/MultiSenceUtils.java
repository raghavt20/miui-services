package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.miui.server.multisence.SingleWindowInfo;
import com.miui.server.stability.DumpSysInfoUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class MultiSenceUtils {
    private static final String TAG = "MultiSenceUtils";
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.multisence.debug.on", false);
    private static MiuiFreeFormManagerService mffms = (MiuiFreeFormManagerService) MiuiFreeFormManagerServiceStub.getInstance();
    private static WindowManagerService service = ServiceManager.getService(DumpSysInfoUtil.WINDOW);

    /* loaded from: classes.dex */
    public static class MultiSenceUtilsHolder {
        private static final MultiSenceUtils INSTANCE = new MultiSenceUtils();
    }

    private MultiSenceUtils() {
        LOG_IF_DEBUG("MultiSenceUtils init");
    }

    public static MultiSenceUtils getInstance() {
        if (mffms == null) {
            mffms = (MiuiFreeFormManagerService) MiuiFreeFormManagerServiceStub.getInstance();
        }
        if (service == null) {
            service = ServiceManager.getService(DumpSysInfoUtil.WINDOW);
        }
        return MultiSenceUtilsHolder.INSTANCE;
    }

    public Map<String, SingleWindowInfo> getWindowsNeedToSched() {
        SingleWindowInfo singleWindow;
        Map<String, SingleWindowInfo> sWindows = new HashMap<>();
        final ArrayList<WindowState> windowlist = new ArrayList<>();
        Map<String, SingleWindowInfo.FreeFormInfo> mFreeFormLocation = new HashMap<>();
        Map<String, SingleWindowInfo.FreeFormInfo> mFloatWindowLocation = new HashMap<>();
        WindowManagerService windowManagerService = service;
        if (windowManagerService == null) {
            return null;
        }
        boolean z = true;
        windowManagerService.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.MultiSenceUtils$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MultiSenceUtils.lambda$getWindowsNeedToSched$1(windowlist, (WindowState) obj);
            }
        }, true);
        MiuiFreeFormManagerService miuiFreeFormManagerService = mffms;
        if (miuiFreeFormManagerService == null) {
            return null;
        }
        Iterator<Integer> it = miuiFreeFormManagerService.mFreeFormActivityStacks.keySet().iterator();
        while (it.hasNext()) {
            Integer taskid = it.next();
            try {
                MiuiFreeFormActivityStack stack = mffms.mFreeFormActivityStacks.get(taskid);
                String name = stack.getStackPackageName();
                if (stack == null) {
                    z = true;
                } else if (name != null) {
                    int pid = -1;
                    try {
                        ActivityRecord r = stack.mTask.getTopActivity(false, z);
                        pid = r.getPid();
                    } catch (Exception e) {
                        Slog.e(TAG, "get pid failed: " + e.toString());
                    }
                    Rect mBounds = stack.getMiuiFreeFormStackInfo().bounds;
                    float mFreeformScale = stack.getMiuiFreeFormStackInfo().freeFormScale;
                    int right = mBounds.left + ((int) ((mBounds.right - mBounds.left) * mFreeformScale));
                    Iterator<Integer> it2 = it;
                    int bottom = mBounds.top + ((int) ((mBounds.bottom - mBounds.top) * mFreeformScale));
                    Rect realBounds = new Rect(mBounds.left, mBounds.top, right, bottom);
                    SingleWindowInfo.WindowForm realWindowForm = getFreeformWindowForm(stack);
                    SingleWindowInfo.FreeFormInfo detailInfo = new SingleWindowInfo.FreeFormInfo(realWindowForm, realBounds, pid);
                    mFreeFormLocation.put(name, detailInfo);
                    LOG_IF_DEBUG("FreeForm: " + name + ", Pid: " + pid);
                    it = it2;
                    z = true;
                }
            } catch (Exception e2) {
                Slog.e(TAG, "get freeform app name failed, error: " + e2.toString());
                it = it;
                z = true;
            }
        }
        for (int i = 0; i < windowlist.size(); i++) {
            WindowState windowInfo = windowlist.get(i);
            String name2 = windowInfo.getOwningPackage();
            if (mFreeFormLocation.containsKey(name2)) {
                singleWindow = new SingleWindowInfo(name2, SingleWindowInfo.AppType.COMMON).setWindowingModeString("freeform").setVisiable(true).setgetWindowForm(mFreeFormLocation.get(name2).mWindowForm).setPid(mFreeFormLocation.get(name2).mPid).setLayerOrder(i).setUid(windowInfo.getUid()).setRectValue(mFreeFormLocation.get(name2).mRect);
            } else if (mFloatWindowLocation.containsKey(name2)) {
                LOG_IF_DEBUG("this is floating window: " + name2);
                singleWindow = new SingleWindowInfo(name2, SingleWindowInfo.AppType.COMMON).setWindowingModeString(getWindowMode(windowInfo)).setVisiable(true).setPid(windowInfo.getPid()).setLayerOrder(i).setUid(windowInfo.getUid());
            } else {
                singleWindow = new SingleWindowInfo(name2, SingleWindowInfo.AppType.COMMON).setWindowingModeString(getWindowMode(windowInfo)).setVisiable(true).setPid(windowInfo.getPid()).setLayerOrder(i).setUid(windowInfo.getUid());
            }
            if (sWindows.containsKey(name2)) {
                SingleWindowInfo swi = sWindows.get(name2);
                int windowCount = swi.getWindowCount();
                swi.setWindowCount(windowCount + 1);
            } else {
                sWindows.put(name2, singleWindow);
            }
        }
        return sWindows;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getWindowsNeedToSched$1(ArrayList windowlist, WindowState w) {
        if (w.getTaskFragment() != null) {
            if (w.getTaskFragment().shouldBeVisible((ActivityRecord) null)) {
                boolean shouldAdd = true;
                if (w.getOwningPackage().equals(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME) || w.getOwningPackage().equals("com.lbe.security.miui")) {
                    Task rootTask = w.getRootTask();
                    ActivityRecord baseActivity = rootTask == null ? null : rootTask.getActivity(new Predicate() { // from class: com.android.server.wm.MultiSenceUtils$$ExternalSyntheticLambda0
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return MultiSenceUtils.lambda$getWindowsNeedToSched$0((ActivityRecord) obj);
                        }
                    }, false);
                    String baseName = baseActivity != null ? baseActivity.getProcessName() : null;
                    if (!ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME.equals(baseName)) {
                        shouldAdd = false;
                    }
                }
                if (shouldAdd) {
                    windowlist.add(w);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getWindowsNeedToSched$0(ActivityRecord r) {
        return !r.finishing;
    }

    private SingleWindowInfo.WindowForm getFreeformWindowForm(MiuiFreeFormActivityStack stack) {
        if (stack.inPinMode()) {
            return SingleWindowInfo.WindowForm.MUTIL_FREEDOM_PIN;
        }
        if (stack.isInMiniFreeFormMode()) {
            return SingleWindowInfo.WindowForm.MUTIL_FREEDOM_MINI;
        }
        if (stack.isInFreeFormMode()) {
            return SingleWindowInfo.WindowForm.MUTIL_FREEDOM;
        }
        return SingleWindowInfo.WindowForm.MUTIL_FREEDOM;
    }

    private String getWindowMode(WindowState newFocus) {
        if (newFocus == null) {
            return "unknown";
        }
        Configuration configuration = newFocus.getConfiguration();
        return WindowConfiguration.windowingModeToString(configuration.windowConfiguration.getWindowingMode());
    }

    private void LOG_IF_DEBUG(String log) {
        if (DEBUG) {
            Slog.d(TAG, log);
        }
    }
}
