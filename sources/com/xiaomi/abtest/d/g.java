package com.xiaomi.abtest.d;

import android.text.TextUtils;
import android.util.LruCache;
import com.xiaomi.abtest.d.f;

/* loaded from: classes.dex */
final class g extends LruCache<String, f.a> {
    /* JADX INFO: Access modifiers changed from: package-private */
    public g(int i) {
        super(i);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.util.LruCache
    /* renamed from: a, reason: merged with bridge method [inline-methods] */
    public int sizeOf(String str, f.a aVar) {
        if (aVar == null || TextUtils.isEmpty(aVar.a)) {
            return 0;
        }
        return aVar.a.length();
    }
}
