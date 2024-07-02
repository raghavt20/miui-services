package com.android.server.wm;

import android.content.pm.ApplicationInfo;

/* loaded from: classes.dex */
public class FgWindowChangedInfo {
    final ApplicationInfo multiWindowAppInfo;
    final int pid;
    final ActivityRecord record;

    public FgWindowChangedInfo(ActivityRecord _record, ApplicationInfo _info, int _pid) {
        this.record = _record;
        this.multiWindowAppInfo = _info;
        this.pid = _pid;
    }
}
