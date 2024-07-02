package com.xiaomi.abtest.a;

import com.xiaomi.abtest.ABTest;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class d implements Runnable {
    final /* synthetic */ ABTest.OnLoadRemoteConfigListener a;
    final /* synthetic */ a b;

    /* JADX INFO: Access modifiers changed from: package-private */
    public d(a aVar, ABTest.OnLoadRemoteConfigListener onLoadRemoteConfigListener) {
        this.b = aVar;
        this.a = onLoadRemoteConfigListener;
    }

    @Override // java.lang.Runnable
    public void run() {
        this.b.e();
        ABTest.OnLoadRemoteConfigListener onLoadRemoteConfigListener = this.a;
        if (onLoadRemoteConfigListener != null) {
            onLoadRemoteConfigListener.onLoadCompleted();
        }
    }
}
