package com.android.server.am;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.content.SyncManagerStubImpl;
import com.miui.server.security.AccessControlImpl;

/* loaded from: classes.dex */
public class ProcessProphetJobService extends JobService {
    private static final int JOBID = 10236;
    private static final String TAG = "ProcessProphetJob";
    private static boolean DEBUG = SystemProperties.getBoolean("persist.sys.procprophet.debug", false);
    private static Context sContext = null;

    public static void schedule(Context context) {
        sContext = context;
        JobInfo.Builder builder = new JobInfo.Builder(JOBID, new ComponentName(context, (Class<?>) ProcessProphetJobService.class));
        if (DEBUG) {
            builder.setMinimumLatency(12 * AccessControlImpl.LOCK_TIME_OUT);
        } else {
            builder.setMinimumLatency(12 * SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED);
            builder.setRequiresDeviceIdle(true);
            builder.setRequiresBatteryNotLow(true);
        }
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        try {
            if (js.schedule(builder.build()) != 1) {
                Slog.e(TAG, "Fail to schedule.");
            } else if (DEBUG) {
                Slog.i(TAG, "Job has been built & will start after 12 minutes.");
            } else {
                Slog.i(TAG, "Job has been built & will start after 12 hours.");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Fail to schedule by: " + e);
        }
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        Slog.i(TAG, "Job executed.");
        ProcessProphetStub.getInstance().notifyIdleUpdate();
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        Slog.w(TAG, "Stop job, reason: " + params.getStopReason());
        return false;
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        schedule(sContext);
        Slog.i(TAG, "Job has been destroyed & rescheduled.");
    }
}
