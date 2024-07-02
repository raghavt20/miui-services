package com.miui.server;

import android.content.Context;
import android.os.Binder;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiCommonCloudServiceStub;
import com.android.server.SystemService;
import com.android.server.am.ProcessUtils;
import com.miui.app.MiuiFreeDragServiceInternal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.freedrag.FilterRule;
import miui.freedrag.FilterRuleData;
import miui.freedrag.IMiuiFreeDragService;

/* loaded from: classes.dex */
public class MiuiFreeDragService extends IMiuiFreeDragService.Stub implements MiuiFreeDragServiceInternal {
    public static final String SERVICE_NAME = "MiuiFreeDragService";
    private static MiuiFreeDragService instance;
    private final ArrayMap<String, ArraySet<String>> mPackagesWithMiuiDragAndDropMetaData = new ArrayMap<>();

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiFreeDragService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = MiuiFreeDragService.getInstance();
        }

        public void onStart() {
            publishBinderService(MiuiFreeDragService.SERVICE_NAME, this.mService);
        }
    }

    private MiuiFreeDragService() {
        LocalServices.addService(MiuiFreeDragServiceInternal.class, this);
    }

    public static synchronized MiuiFreeDragService getInstance() {
        MiuiFreeDragService miuiFreeDragService;
        synchronized (MiuiFreeDragService.class) {
            if (instance == null) {
                instance = new MiuiFreeDragService();
            }
            miuiFreeDragService = instance;
        }
        return miuiFreeDragService;
    }

    @Override // com.miui.app.MiuiFreeDragServiceInternal
    public void notifyHasMiuiDragAndDropMetaData(String packageName, String className) {
        synchronized (this.mPackagesWithMiuiDragAndDropMetaData) {
            ArraySet<String> set = this.mPackagesWithMiuiDragAndDropMetaData.get(packageName);
            if (set == null) {
                set = new ArraySet<>();
            }
            set.add(className);
            this.mPackagesWithMiuiDragAndDropMetaData.put(packageName, set);
        }
    }

    public List<FilterRule> getFilterRules() {
        String packageName = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        if (packageName == null || packageName.isEmpty()) {
            Slog.e(SERVICE_NAME, "getFilterRules packageName = " + packageName + ", can't drag!");
            return null;
        }
        FilterRuleData filterRuleData = (FilterRuleData) MiuiCommonCloudServiceStub.getInstance().getDataByModuleName("miui_free_drag");
        if (filterRuleData == null) {
            Slog.e(SERVICE_NAME, "getFilterRules filterRuleData = null, can't drag!");
            return null;
        }
        List<FilterRule> rules = new ArrayList<>();
        for (FilterRule rule : filterRuleData.getRules()) {
            if (rule.getPackageName().isEmpty() || rule.getPackageName().equals(packageName)) {
                rules.add(rule);
            }
        }
        synchronized (this.mPackagesWithMiuiDragAndDropMetaData) {
            if (this.mPackagesWithMiuiDragAndDropMetaData.containsKey(packageName)) {
                ArraySet<String> activityClassSet = this.mPackagesWithMiuiDragAndDropMetaData.get(packageName);
                Iterator<String> it = activityClassSet.iterator();
                while (it.hasNext()) {
                    String activityClass = it.next();
                    FilterRule filterRule = new FilterRule(packageName, activityClass, "");
                    rules.add(filterRule);
                }
            }
        }
        return rules;
    }
}
