package com.android.server.am;

import miui.process.ProcessManager;

/* loaded from: classes.dex */
class ProcessPriorityInfo {
    ProcessRecord app = null;
    int maxAdj = ProcessManager.DEFAULT_MAX_ADJ;
    int maxProcState = 20;
}
