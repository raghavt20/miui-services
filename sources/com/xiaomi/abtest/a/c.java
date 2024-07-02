package com.xiaomi.abtest.a;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class c implements Runnable {
    final /* synthetic */ int a;
    final /* synthetic */ a b;

    /* JADX INFO: Access modifiers changed from: package-private */
    public c(a aVar, int i) {
        this.b = aVar;
        this.a = i;
    }

    /* JADX WARN: Code restructure failed: missing block: B:4:0x003c, code lost:
    
        if (r0 != false) goto L6;
     */
    @Override // java.lang.Runnable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void run() {
        /*
            r3 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "mIsAppForeground: "
            java.lang.StringBuilder r0 = r0.append(r1)
            com.xiaomi.abtest.a.a r1 = r3.b
            boolean r1 = com.xiaomi.abtest.a.a.a(r1)
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r1 = ", mIsLoadConfigWhenBackground: "
            java.lang.StringBuilder r0 = r0.append(r1)
            com.xiaomi.abtest.a.a r1 = r3.b
            boolean r1 = com.xiaomi.abtest.a.a.b(r1)
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "ExpPlatformManager"
            com.xiaomi.abtest.d.k.a(r1, r0)
            com.xiaomi.abtest.a.a r0 = r3.b
            boolean r0 = com.xiaomi.abtest.a.a.a(r0)
            if (r0 != 0) goto L3e
            com.xiaomi.abtest.a.a r0 = r3.b
            boolean r0 = com.xiaomi.abtest.a.a.b(r0)
            if (r0 == 0) goto L44
        L3e:
            com.xiaomi.abtest.a.a r0 = r3.b
            r1 = 0
            r0.a(r1)
        L44:
            com.xiaomi.abtest.a.a r0 = r3.b
            android.os.Handler r0 = com.xiaomi.abtest.a.a.c(r0)
            int r1 = r3.a
            int r1 = r1 * 1000
            long r1 = (long) r1
            r0.postDelayed(r3, r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.abtest.a.c.run():void");
    }
}
