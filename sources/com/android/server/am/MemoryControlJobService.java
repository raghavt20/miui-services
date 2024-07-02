package com.android.server.am;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import java.util.Calendar;

/* loaded from: classes.dex */
public class MemoryControlJobService extends JobService {
    private static final int DETECTION_JOB_ID = 80901;
    private static final int KILL_JOB_ID = 80902;
    private static final String TAG = "MemoryStandardProcessControl";
    private final int SCREEN_STATE_OFF = 2;
    public Runnable mFinishCallback = new Runnable() { // from class: com.android.server.am.MemoryControlJobService.1
        @Override // java.lang.Runnable
        public void run() {
            Slog.i(MemoryControlJobService.TAG, "Complete callback");
            synchronized (MemoryControlJobService.this.mFinishCallback) {
                if (MemoryControlJobService.this.mStarted) {
                    MemoryControlJobService memoryControlJobService = MemoryControlJobService.this;
                    memoryControlJobService.jobFinished(memoryControlJobService.mJobParams, false);
                    MemoryControlJobService.this.mStarted = false;
                }
            }
            MemoryControlJobService.killSchedule(MemoryControlJobService.sContext);
        }
    };
    private JobParameters mJobParams;
    private boolean mStarted;
    private static long PERIODIC_DETECTION_TIME = 1200000;
    private static MemoryStandardProcessControl mspc = null;
    private static Context sContext = null;

    public static void detectionSchedule(Context context) {
        if (sContext == null) {
            sContext = context;
        }
        Slog.d(TAG, "detectionSchedule is called");
        ComponentName serviceName = new ComponentName("android", MemoryControlJobService.class.getName());
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo.Builder detectionBuilder = new JobInfo.Builder(DETECTION_JOB_ID, serviceName);
        detectionBuilder.setRequiresBatteryNotLow(true);
        detectionBuilder.setPeriodic(PERIODIC_DETECTION_TIME);
        js.schedule(detectionBuilder.build());
    }

    public static void killSchedule(Context context) {
        if (sContext == null) {
            sContext = context;
        }
        Slog.d(TAG, "killSchedule is called");
        long tomorrow3AM = offsetFromTodayMidnight(1, 3).getTimeInMillis();
        long nextScheduleTime = tomorrow3AM - System.currentTimeMillis();
        ComponentName serviceName = new ComponentName("android", MemoryControlJobService.class.getName());
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo.Builder killBuilder = new JobInfo.Builder(KILL_JOB_ID, serviceName);
        killBuilder.setRequiresBatteryNotLow(true);
        killBuilder.setMinimumLatency(nextScheduleTime);
        js.schedule(killBuilder.build());
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        if (mspc == null) {
            mspc = (MemoryStandardProcessControl) MemoryStandardProcessControlStub.getInstance();
        }
        int screenState = mspc.mScreenState;
        boolean enable = mspc.isEnable();
        if (enable) {
            if (params.getJobId() == DETECTION_JOB_ID) {
                if (screenState != 2) {
                    mspc.callPeriodicDetection();
                }
                return false;
            }
            if (params.getJobId() == KILL_JOB_ID) {
                if (screenState == 2) {
                    mspc.callKillProcess(true);
                }
                this.mJobParams = params;
                synchronized (this.mFinishCallback) {
                    this.mStarted = true;
                }
                mspc.callRunnable(this.mFinishCallback);
                return true;
            }
        }
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        int jobId = params.getJobId();
        if (jobId == KILL_JOB_ID) {
            this.mStarted = false;
            mspc.callRunnable(this.mFinishCallback);
        }
        if (MemoryStandardProcessControl.DEBUG) {
            Slog.d(TAG, "onStopJob, jobId: " + jobId);
        }
        return false;
    }

    private static Calendar offsetFromTodayMidnight(int nDays, int nHours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(11, nHours);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        calendar.add(5, nDays);
        return calendar;
    }
}
