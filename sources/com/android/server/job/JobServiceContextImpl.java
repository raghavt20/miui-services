package com.android.server.job;

import android.content.Context;
import android.content.Intent;
import com.android.server.LocalServices;
import com.android.server.am.AutoStartManagerServiceStub;
import com.android.server.job.controllers.JobStatus;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import miui.os.Build;

@MiuiStubHead(manifestName = "com.android.server.job.JobServiceContextStub$$")
/* loaded from: classes.dex */
public class JobServiceContextImpl extends JobServiceContextStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<JobServiceContextImpl> {

        /* compiled from: JobServiceContextImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final JobServiceContextImpl INSTANCE = new JobServiceContextImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public JobServiceContextImpl m1663provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public JobServiceContextImpl m1662provideNewInstance() {
            return new JobServiceContextImpl();
        }
    }

    public boolean checkIfCancelJob(JobServiceContext jobContext, Context context, Intent service, Context.BindServiceFlags bindFlags, JobStatus job) {
        if (Build.IS_INTERNATIONAL_BUILD || job == null || AutoStartManagerServiceStub.getInstance().isAllowStartService(context, service, job.getUserId(), job.getUid())) {
            return false;
        }
        int jobId = job.getJobId();
        int uid = job.getUid();
        JobSchedulerInternal internal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        if (internal != null) {
            internal.cancelJob(uid, job.getNamespace(), jobId);
            return true;
        }
        return true;
    }
}
