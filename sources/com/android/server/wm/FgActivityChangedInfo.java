package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import com.android.server.wm.ActivityRecord;

/* loaded from: classes.dex */
public class FgActivityChangedInfo {
    final ApplicationInfo multiWindowAppInfo;
    final int pid;
    final ActivityRecord record;
    final ActivityRecord.State state;

    public FgActivityChangedInfo(ActivityRecord _record, ActivityRecord.State state, int pid, ApplicationInfo _info) {
        this.record = _record;
        this.state = state;
        this.pid = pid;
        this.multiWindowAppInfo = _info;
    }

    public String getPackageName() {
        return this.record.packageName;
    }
}
