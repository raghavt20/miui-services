package com.android.server.pm;

import android.app.job.JobParameters;
import android.app.job.JobService;

/* loaded from: classes.dex */
public class BackgroundPreinstalloptService extends JobService {
    public static final int JOBID = 1896105;

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        return getHelper().onStartJob(this, params);
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private MiuiPreinstallHelper getHelper() {
        return MiuiPreinstallHelper.getInstance();
    }
}
