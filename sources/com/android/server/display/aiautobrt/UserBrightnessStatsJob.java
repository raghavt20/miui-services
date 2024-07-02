package com.android.server.display.aiautobrt;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.display.DisplayManagerServiceStub;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class UserBrightnessStatsJob extends JobService {
    private static final long DEBUG_INTERVAL;
    private static final int JOB_ID = 34769;
    private static final String TAG = "CbmController-StatsJob";
    private static final boolean sDebug;

    static {
        sDebug = SystemProperties.getInt("debug.miui.display.JobService.dbg", 0) != 0;
        DEBUG_INTERVAL = TimeUnit.MINUTES.toMillis(15L);
    }

    public static void scheduleJob(Context context) {
        Slog.d(TAG, "Start schedule job.");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        JobInfo pending = jobScheduler.getPendingJob(JOB_ID);
        JobInfo.Builder requiresDeviceIdle = new JobInfo.Builder(JOB_ID, new ComponentName(context, (Class<?>) UserBrightnessStatsJob.class)).setRequiresDeviceIdle(true);
        boolean z = sDebug;
        JobInfo jobInfo = requiresDeviceIdle.setPeriodic(z ? DEBUG_INTERVAL : TimeUnit.HOURS.toMillis(24L)).build();
        if (pending != null && !pending.equals(jobInfo)) {
            Slog.d(TAG, "scheduleJob: cancel.");
            jobScheduler.cancel(JOB_ID);
            pending = null;
        }
        if (pending == null) {
            Slog.d(TAG, "scheduleJob: schedule.");
            jobScheduler.schedule(jobInfo);
        }
        if (z) {
            Slog.d(TAG, "Schedule job use debug interval, interval: " + DEBUG_INTERVAL);
        }
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        Slog.d(TAG, "Start user manual adjustment stats.");
        DisplayManagerServiceStub.getInstance().startCbmStatsJob();
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        Slog.d(TAG, "Stop job.");
        return false;
    }
}
