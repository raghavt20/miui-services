package com.android.server.wm;

import android.app.IApplicationThread;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.am.IProcessPolicy;
import com.miui.app.smartpower.SmartPowerPolicyConstants;
import com.miui.server.AccessController;
import com.miui.server.process.ProcessManagerInternal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.security.CallerInfo;

/* loaded from: classes.dex */
public class WindowProcessUtils {
    private static final String TAG = WindowProcessUtils.class.getSimpleName();
    private static ActivityTaskManagerService sAtmsInstance = null;

    static synchronized ActivityTaskManagerService getActivityTaskManagerService() {
        ActivityTaskManagerService activityTaskManagerService;
        synchronized (WindowProcessUtils.class) {
            if (sAtmsInstance == null) {
                sAtmsInstance = ServiceManager.getService("activity_task");
            }
            activityTaskManagerService = sAtmsInstance;
        }
        return activityTaskManagerService;
    }

    public static boolean isProcessHasActivityInOtherTaskLocked(WindowProcessController app, int curTaskId) {
        ActivityTaskManagerService atms = getActivityTaskManagerService();
        synchronized (atms.mGlobalLock) {
            List<ActivityRecord> activities = app.getActivities();
            if (activities == null) {
                return false;
            }
            for (int i = 0; i < activities.size(); i++) {
                Task otherTask = activities.get(i).getTask();
                if (otherTask != null && curTaskId != otherTask.mTaskId && otherTask.inRecents && isTaskVisibleInRecents(otherTask)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isTaskVisibleInRecents(Task task) {
        if (task == null) {
            return false;
        }
        if (task.intent == null) {
            return true;
        }
        return task.isAvailable && (task.intent.getFlags() & SmartPowerPolicyConstants.WHITE_LIST_TYPE_PROVIDER_MAX) == 0;
    }

    public static Map<Integer, String> getPerceptibleRecentAppList(ActivityTaskManagerService atms) {
        ActivityRecord r;
        Map<Integer, String> taskPackageMap = new HashMap<>();
        synchronized (atms.mGlobalLock) {
            Task dockedStack = getMultiWindowStackLocked(atms);
            if (dockedStack != null && (r = dockedStack.topRunningActivityLocked()) != null && r.getTask() != null) {
                taskPackageMap.put(Integer.valueOf(r.getTask().mTaskId), r.packageName);
            }
            Iterator it = atms.getRecentTasks().getRawTasks().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Task task = (Task) it.next();
                if (task != null && task.getRootTask() != null) {
                    String taskPackageName = getTaskPackageNameLocked(task);
                    if (!isTaskInMultiWindowStackLocked(task) && !TextUtils.isEmpty(taskPackageName) && !TextUtils.equals(taskPackageName, AccessController.PACKAGE_SYSTEMUI) && !task.inFreeformWindowingMode() && task.isVisible()) {
                        taskPackageMap.put(Integer.valueOf(task.mTaskId), taskPackageName);
                        break;
                    }
                }
            }
        }
        Log.d(TAG, "getPerceptibleRecentAppList: " + Arrays.toString(taskPackageMap.values().toArray()));
        return taskPackageMap;
    }

    public static List<Integer> getAllTaskIdList(ActivityTaskManagerService atms) {
        ArrayList<Integer> allTaskIdList;
        synchronized (atms.mGlobalLock) {
            ArrayList<Task> allRecentTasks = atms.getRecentTasks().getRawTasks();
            allTaskIdList = new ArrayList<>();
            Iterator<Task> it = allRecentTasks.iterator();
            while (it.hasNext()) {
                Task task = it.next();
                allTaskIdList.add(Integer.valueOf(task.mTaskId));
            }
        }
        return allTaskIdList;
    }

    private static Task getMultiWindowStackLocked(ActivityTaskManagerService atms) {
        int numDisplays = atms.mTaskSupervisor.mRootWindowContainer.getChildCount();
        for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
            DisplayContent display = atms.mTaskSupervisor.mRootWindowContainer.getChildAt(displayNdx);
            Task result = display.getRootTask(new Predicate() { // from class: com.android.server.wm.WindowProcessUtils$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean isMultiWindowStackLocked;
                    isMultiWindowStackLocked = WindowProcessUtils.isMultiWindowStackLocked((Task) obj);
                    return isMultiWindowStackLocked;
                }
            });
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static boolean isTaskInMultiWindowStackLocked(Task task) {
        if (task != null) {
            task.topRunningActivityLocked();
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isMultiWindowStackLocked(Task stack) {
        return false;
    }

    private static String getTaskPackageNameLocked(Task task) {
        if (task == null || task.getBaseIntent() == null || task.getBaseIntent().getComponent() == null) {
            return null;
        }
        String packageName = task.getBaseIntent().getComponent().getPackageName();
        return packageName;
    }

    public static WindowProcessController getTaskTopApp(int taskId) {
        WindowProcessController taskTopAppLocked;
        ActivityTaskManagerService atms = getActivityTaskManagerService();
        synchronized (atms.mGlobalLock) {
            Task task = atms.getRecentTasks().getTask(taskId);
            taskTopAppLocked = getTaskTopAppLocked(task);
        }
        return taskTopAppLocked;
    }

    private static WindowProcessController getTaskRootOrTopAppLocked(Task task) {
        if (task == null) {
            return null;
        }
        ActivityRecord record = task.getRootActivity();
        if (record == null) {
            record = task.topRunningActivityLocked();
        }
        if (record == null) {
            return null;
        }
        return record.app;
    }

    private static WindowProcessController getTaskTopAppLocked(Task task) {
        ActivityRecord topActivity;
        if (task != null && (topActivity = task.topRunningActivityLocked()) != null) {
            return topActivity.app;
        }
        return null;
    }

    private static int getTaskTopAppUidLocked(Task task) {
        ActivityRecord topActivity;
        if (task == null || (topActivity = task.topRunningActivityLocked()) == null || topActivity.info.applicationInfo == null) {
            return -1;
        }
        int uid = topActivity.info.applicationInfo.uid;
        return uid;
    }

    private static String getTaskTopAppProcessNameLocked(Task task) {
        ActivityRecord topActivity;
        if (task == null || (topActivity = task.topRunningActivityLocked()) == null || topActivity.info.applicationInfo == null) {
            return null;
        }
        return topActivity.processName;
    }

    public static boolean isRemoveTaskDisabled(int taskId, String packageName, ActivityTaskManagerService atms) {
        synchronized (atms.mGlobalLock) {
            Task task = atms.getRecentTasks().getTask(taskId);
            if (task != null) {
                return TextUtils.equals(packageName, getTaskTopAppProcessNameLocked(task));
            }
            return false;
        }
    }

    public static void removeAllTasks(ProcessManagerInternal pms, int userId, ActivityTaskManagerService atms) {
        synchronized (atms.mGlobalLock) {
            List<Task> removedTasks = new ArrayList<>();
            Iterator it = atms.getRecentTasks().getRawTasks().iterator();
            while (it.hasNext()) {
                Task task = (Task) it.next();
                if (task.mUserId == userId) {
                    WindowProcessController app = getTaskRootOrTopAppLocked(task);
                    if (app == null) {
                        removedTasks.add(task);
                    } else if (pms.isTrimMemoryEnable(app.mInfo.packageName)) {
                        removedTasks.add(task);
                    }
                }
            }
            Iterator<Task> it2 = removedTasks.iterator();
            while (it2.hasNext()) {
                removeTaskLocked(it2.next(), atms);
            }
        }
    }

    public static void removeTasks(List<Integer> taskIdList, Set<Integer> whiteTaskSet, IProcessPolicy processPolicy, ActivityTaskManagerService atms, List<String> whiteList, List<Integer> whiteListTaskId) {
        if (taskIdList == null || taskIdList.isEmpty()) {
            return;
        }
        synchronized (atms.mGlobalLock) {
            List<Task> removedTasks = new ArrayList<>();
            int i = 2;
            int i2 = 0;
            int i3 = 1;
            String[] whiteListPackageNames = {"com.miui.home", "com.mi.android.globallauncher"};
            Iterator it = atms.getRecentTasks().getRawTasks().iterator();
            while (it.hasNext()) {
                Task task = (Task) it.next();
                if (task != null) {
                    String taskPackageName = getTaskPackageNameLocked(task);
                    if (whiteListTaskId != null || whiteList == null || !whiteList.contains(taskPackageName)) {
                        if (processPolicy.isLockedApplication(taskPackageName, task.mUserId)) {
                            i = 2;
                            i2 = 0;
                            i3 = 1;
                        } else if (whiteTaskSet == null || !whiteTaskSet.contains(Integer.valueOf(task.mTaskId))) {
                            if (whiteListTaskId == null || !whiteListTaskId.contains(Integer.valueOf(task.mTaskId))) {
                                if (task.mCreatedByOrganizer && task.getChildCount() >= i) {
                                    if (processPolicy.isLockedApplication(getTaskPackageNameLocked(task.getChildAt(i2).asTask()), task.mUserId) && processPolicy.isLockedApplication(getTaskPackageNameLocked(task.getChildAt(i3).asTask()), task.mUserId)) {
                                        i = 2;
                                    }
                                    if (whiteList != null && whiteList.contains(getTaskPackageNameLocked(task.getChildAt(i2).asTask())) && whiteList.contains(getTaskPackageNameLocked(task.getChildAt(i3).asTask()))) {
                                        i = 2;
                                    }
                                }
                                if (MiuiSoScManagerStub.get().isInSoScSingleMode(task)) {
                                    i = 2;
                                } else {
                                    boolean flag = false;
                                    int length = whiteListPackageNames.length;
                                    while (true) {
                                        if (i2 >= length) {
                                            break;
                                        }
                                        String whiteListPackageName = whiteListPackageNames[i2];
                                        if (!whiteListPackageName.equals(taskPackageName)) {
                                            i2++;
                                        } else {
                                            flag = true;
                                            break;
                                        }
                                    }
                                    if (flag) {
                                        i = 2;
                                        i2 = 0;
                                        i3 = 1;
                                    } else {
                                        if (taskIdList.contains(Integer.valueOf(task.mTaskId)) && (!TextUtils.isEmpty(taskPackageName) || (task.mCreatedByOrganizer && task.hasChild()))) {
                                            removedTasks.add(task);
                                        }
                                        i = 2;
                                        i2 = 0;
                                        i3 = 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Iterator<Task> it2 = removedTasks.iterator();
            while (it2.hasNext()) {
                removeTaskLocked(it2.next(), atms);
            }
        }
    }

    public static void removeTasksInPackages(List<String> packages, int userId, IProcessPolicy processPolicy, ActivityTaskManagerService atms) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        synchronized (atms.mGlobalLock) {
            List<Task> removedTasks = new ArrayList<>();
            Iterator it = atms.getRecentTasks().getRawTasks().iterator();
            while (it.hasNext()) {
                Task task = (Task) it.next();
                String taskPackageName = getTaskPackageNameLocked(task);
                if (!processPolicy.isLockedApplication(taskPackageName, userId)) {
                    if (task.mUserId == userId && !TextUtils.isEmpty(taskPackageName) && packages.contains(taskPackageName)) {
                        removedTasks.add(task);
                    }
                }
            }
            Iterator<Task> it2 = removedTasks.iterator();
            while (it2.hasNext()) {
                removeTaskLocked(it2.next(), atms);
            }
        }
    }

    public static void removeTask(int taskId, ActivityTaskManagerService atms) {
        synchronized (atms.mGlobalLock) {
            Task task = atms.getRecentTasks().getTask(taskId);
            removeTaskLocked(task, atms);
        }
    }

    private static void removeTaskLocked(Task task, ActivityTaskManagerService atms) {
        if (task == null) {
            return;
        }
        atms.mTaskSupervisor.removeTask(task, false, true, "removeTaskLocked");
    }

    public static ApplicationInfo getMultiWindowForegroundAppInfoLocked(ActivityTaskManagerService atms) {
        ActivityRecord multiWindowActivity;
        ApplicationInfo applicationInfo = null;
        synchronized (atms.mGlobalLock) {
            Task multiWindowStack = atms.mTaskSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
            if (multiWindowStack != null && (multiWindowActivity = multiWindowStack.topRunningActivityLocked()) != null) {
                applicationInfo = multiWindowActivity.info.applicationInfo;
            }
        }
        return applicationInfo;
    }

    public static int getTopRunningPidLocked() {
        ActivityRecord record;
        ActivityTaskManagerService atms = getActivityTaskManagerService();
        synchronized (atms.mGlobalLock) {
            Task topStack = atms.mTaskSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
            if (topStack != null && (record = topStack.topRunningActivityLocked()) != null && record.app != null) {
                return record.app.getPid();
            }
            return 0;
        }
    }

    public static WindowProcessController getTopRunningProcessController() {
        ActivityRecord record;
        ActivityTaskManagerService atms = getActivityTaskManagerService();
        synchronized (atms.mGlobalLock) {
            Task topStack = atms.mTaskSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
            if (topStack != null && (record = topStack.topRunningActivityLocked()) != null && record.app != null) {
                return record.app;
            }
            return null;
        }
    }

    public static ArrayList<Intent> getTaskIntentForToken(IBinder token) {
        ActivityTaskManagerService atms = getActivityTaskManagerService();
        synchronized (atms.mGlobalLock) {
            ActivityRecord activityRecord = ActivityRecord.isInRootTaskLocked(token);
            if (activityRecord != null) {
                final ArrayList<Intent> arrayList = new ArrayList<>();
                activityRecord.getTask().forAllActivities(new Consumer() { // from class: com.android.server.wm.WindowProcessUtils$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        arrayList.add(((ActivityRecord) obj).intent);
                    }
                });
                return arrayList;
            }
            return null;
        }
    }

    public static HashMap<String, Object> getTopRunningActivityInfo() {
        ActivityRecord r;
        ActivityTaskManagerService atms = getActivityTaskManagerService();
        synchronized (atms.mGlobalLock) {
            Task activityStack = atms.mTaskSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
            if (activityStack != null && (r = activityStack.topRunningActivityLocked()) != null) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("packageName", r.packageName);
                map.put("token", r.token);
                map.put("userId", Integer.valueOf(r.mUserId));
                map.put("intent", r.intent);
                return map;
            }
            return null;
        }
    }

    public static String getActivityComponentName(ActivityRecord activity) {
        return activity.shortComponentName;
    }

    public static String getCallerPackageName(ActivityTaskManagerService atms, IApplicationThread caller) {
        synchronized (atms.mGlobalLock) {
            WindowProcessController wpc = atms.getProcessController(caller);
            if (wpc != null && wpc.mInfo != null) {
                return wpc.mInfo.packageName;
            }
            return null;
        }
    }

    public static int getCallerUid(ActivityTaskManagerService atms, IApplicationThread caller) {
        synchronized (atms.mGlobalLock) {
            WindowProcessController wpc = atms.getProcessController(caller);
            if (wpc != null && wpc.mInfo != null) {
                return wpc.mInfo.uid;
            }
            return -1;
        }
    }

    public static CallerInfo getCallerInfo(ActivityTaskManagerService atms, IApplicationThread caller) {
        synchronized (atms.mGlobalLock) {
            WindowProcessController wpc = atms.getProcessController(caller);
            if (wpc == null || wpc.mInfo == null) {
                return null;
            }
            CallerInfo info = new CallerInfo();
            info.callerPkg = wpc.mInfo.packageName;
            info.callerUid = wpc.mInfo.uid;
            info.callerPid = wpc.getPid();
            info.callerProcessName = wpc.mName;
            return info;
        }
    }

    public static CallerInfo getCallerInfo(ActivityTaskManagerService atms, int callingPid, int callingUid) {
        synchronized (atms.mGlobalLock) {
            WindowProcessController wpc = atms.getProcessController(callingPid, callingUid);
            if (wpc == null || wpc.mInfo == null) {
                return null;
            }
            CallerInfo info = new CallerInfo();
            info.callerPkg = wpc.mInfo.packageName;
            info.callerUid = wpc.mInfo.uid;
            info.callerPid = wpc.getPid();
            info.callerProcessName = wpc.mName;
            return info;
        }
    }

    public static boolean isRunningOnCarDisplay(ActivityTaskManagerService atms, final String name) {
        if (name != null && (name.contains("com.miui.carlink") || name.contains("com.baidu.carlife.xiaomi"))) {
            return true;
        }
        final AtomicBoolean result = new AtomicBoolean(false);
        atms.mTaskSupervisor.mRootWindowContainer.forAllTasks(new Consumer() { // from class: com.android.server.wm.WindowProcessUtils$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowProcessUtils.lambda$isRunningOnCarDisplay$1(name, result, (Task) obj);
            }
        });
        return result.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$isRunningOnCarDisplay$1(String name, AtomicBoolean result, Task task) {
        ActivityRecord ac = task.getTopMostActivity();
        if (ac != null && name.contains(ac.packageName)) {
            String displayName = task.getDisplayContent().getDisplayInfo().name;
            if (displayName.contains("com.miui.carlink") || displayName.contains("com.miui.car.launcher") || displayName.contains("com.baidu.carlife.xiaomi")) {
                result.set(true);
            }
        }
    }

    public static boolean isPackageRunning(ActivityTaskManagerService atms, String packageName, String processName, int uid) {
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName) || uid == 0) {
            return false;
        }
        synchronized (atms.mGlobalLock) {
            if (atms.getProcessController(processName, uid) != null) {
                return true;
            }
            ArraySet<WindowProcessController> uidMap = atms.mProcessMap.getProcesses(uid);
            if (uidMap == null) {
                return false;
            }
            Iterator<WindowProcessController> it = uidMap.iterator();
            while (it.hasNext()) {
                WindowProcessController wpc = it.next();
                if (wpc != null && wpc.getThread() != null && !wpc.isCrashing() && !wpc.isNotResponding() && !(packageName + ":widgetProvider").equals(wpc.mName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean isProcessRunning(ActivityTaskManagerService atms, String processName, int uid) {
        boolean z;
        if (TextUtils.isEmpty(processName) || uid == 0) {
            return false;
        }
        synchronized (atms.mGlobalLock) {
            z = atms.getProcessController(processName, uid) != null;
        }
        return z;
    }
}
