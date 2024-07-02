package com.android.server.wm;

import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class SplitScreenRecommendPredictHelper {
    private static final String TAG = "SplitScreenRecommendPredictHelper";
    MiuiMultiWindowRecommendHelper mMiuiMultiWindowRecommendHelper;

    public SplitScreenRecommendPredictHelper(MiuiMultiWindowRecommendHelper miuiMultiWindowRecommendHelper) {
        this.mMiuiMultiWindowRecommendHelper = miuiMultiWindowRecommendHelper;
    }

    public List<SplitScreenRecommendTaskInfo> getFrequentSwitchedTask(List<SplitScreenRecommendTaskInfo> list) {
        List<SplitScreenRecommendTaskInfo> deduplicatedTaskList = getDeduplicatedTaskList(list);
        List<SplitScreenRecommendTaskInfo> candidateTaskList = new ArrayList<>();
        List<Integer> candidateTaskIndexList = new ArrayList<>();
        if (deduplicatedTaskList.size() < 4) {
            Slog.d(TAG, "appSwitchList size is " + deduplicatedTaskList.size() + " less than 4");
            return null;
        }
        for (int i = 0; i < deduplicatedTaskList.size(); i++) {
            final SplitScreenRecommendTaskInfo taskInfo = deduplicatedTaskList.get(i);
            if (deduplicatedTaskList.stream().filter(new Predicate() { // from class: com.android.server.wm.SplitScreenRecommendPredictHelper.1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    SplitScreenRecommendTaskInfo splitScreenRecommendTaskInfo = taskInfo;
                    return splitScreenRecommendTaskInfo != null && splitScreenRecommendTaskInfo.getTaskId() == ((SplitScreenRecommendTaskInfo) obj).getTaskId();
                }
            }).count() >= 2) {
                candidateTaskList.add(taskInfo);
                candidateTaskIndexList.add(Integer.valueOf(i));
            }
        }
        int i2 = candidateTaskList.size();
        if (i2 != 4) {
            Slog.d(TAG, "not find frequent switched task");
        } else {
            if (candidateTaskIndexList.get(0).intValue() == candidateTaskIndexList.get(1).intValue() - 1 && candidateTaskIndexList.get(2).intValue() == candidateTaskIndexList.get(3).intValue() - 1 && candidateTaskIndexList.get(1).intValue() == candidateTaskIndexList.get(2).intValue() - 1) {
                Task task1 = candidateTaskList.get(0).getTask();
                Task task2 = candidateTaskList.get(1).getTask();
                synchronized (this.mMiuiMultiWindowRecommendHelper.mFreeFormManagerService.mActivityTaskManagerService.getGlobalLock()) {
                    if (task1.supportsSplitScreenWindowingMode() && task2.supportsSplitScreenWindowingMode()) {
                        if (task1.getWindowingMode() != 1 || task2.getWindowingMode() != 1) {
                            Slog.d(TAG, " task windowingMode is not fullscreen");
                            return null;
                        }
                        if (RecommendUtils.isInSplitScreenWindowingMode(task1) || RecommendUtils.isInSplitScreenWindowingMode(task2)) {
                            Slog.d(TAG, " task isInSplitScreenWindowingMode");
                            return null;
                        }
                        List<SplitScreenRecommendTaskInfo> frequentSwitchedTaskList = new ArrayList<>(2);
                        frequentSwitchedTaskList.add(candidateTaskList.get(0));
                        frequentSwitchedTaskList.add(candidateTaskList.get(1));
                        Slog.d(TAG, "find frequent switched task, task 1: " + frequentSwitchedTaskList.get(0).getPkgName() + " task 2: " + frequentSwitchedTaskList.get(1).getPkgName());
                        return frequentSwitchedTaskList;
                    }
                    Slog.d(TAG, " not supportsSplitScreenWindowingMode");
                    return null;
                }
            }
            Slog.d(TAG, "not find frequent switched task, index is not contiguous");
        }
        return null;
    }

    private List<SplitScreenRecommendTaskInfo> getDeduplicatedTaskList(List<SplitScreenRecommendTaskInfo> list) {
        ArrayList arrayList = new ArrayList();
        for (SplitScreenRecommendTaskInfo taskInfo : list) {
            if (arrayList.isEmpty() || taskInfo.getTaskId() != ((SplitScreenRecommendTaskInfo) arrayList.get(arrayList.size() - 1)).getTaskId()) {
                arrayList.add(taskInfo);
            }
        }
        return arrayList;
    }
}
