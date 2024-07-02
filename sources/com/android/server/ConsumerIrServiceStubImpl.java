package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.MiuiBatteryStatsService;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;

@MiuiStubHead(manifestName = "com.android.server.ConsumerIrServiceStub$$")
/* loaded from: classes.dex */
public class ConsumerIrServiceStubImpl extends ConsumerIrServiceStub {
    private static final String DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
    private static final String EVENT_APP_ID = "31000000091";
    private static final String EVENT_NAME = "ir";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final String KEY_EVENT_TYPE = "ir_status";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String TAG = "ConsumerIrService";
    private Context mContext;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ConsumerIrServiceStubImpl> {

        /* compiled from: ConsumerIrServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ConsumerIrServiceStubImpl INSTANCE = new ConsumerIrServiceStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public ConsumerIrServiceStubImpl m17provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public ConsumerIrServiceStubImpl m16provideNewInstance() {
            return new ConsumerIrServiceStubImpl();
        }
    }

    public void reportIrToOneTrack(Context context) {
        if (DEVICE_REGION.equals("IN")) {
            return;
        }
        this.mContext = context;
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.ConsumerIrServiceStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ConsumerIrServiceStubImpl.this.lambda$reportIrToOneTrack$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$reportIrToOneTrack$0() {
        try {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, EVENT_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, this.mContext.getPackageName());
            Bundle params = new Bundle();
            if (DEVICE_REGION.equals("CN")) {
                intent.setFlags(2);
            }
            intent.putExtra(KEY_EVENT_TYPE, true);
            intent.putExtras(params);
            this.mContext.startService(intent);
        } catch (Exception e) {
            Slog.e(TAG, "error reportIrToOneTrack:" + e.toString());
        }
        Slog.d(TAG, "reportIrToOneTrack");
    }
}
