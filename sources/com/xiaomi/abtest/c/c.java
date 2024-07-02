package com.xiaomi.abtest.c;

import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.d.k;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class c extends e {
    private static final String n = "ExpContainer";

    public c(int i, String str, EnumType.FlowUnitType flowUnitType, int i2, EnumType.FlowUnitStatus flowUnitStatus, int i3, EnumType.DiversionType diversionType, String str2, String str3) {
        super(i, str, flowUnitType, i2, flowUnitStatus, i3, diversionType, str2, str3);
        this.i = new ArrayList();
    }

    @Override // com.xiaomi.abtest.c.e
    public boolean a(int i) {
        return true;
    }

    @Override // com.xiaomi.abtest.c.e
    public boolean a(com.xiaomi.abtest.b.a aVar) {
        return true;
    }

    @Override // com.xiaomi.abtest.c.e
    public void a(com.xiaomi.abtest.b.a aVar, List<e> list) {
        k.d(n, String.format("id: %d, name: %s", Integer.valueOf(a()), b()));
        if (this.i.size() <= 0) {
            k.d(n, "no experiment in this ExpContainer.");
            return;
        }
        int b = b(aVar);
        k.d(n, String.format("bucketId: %s", Integer.valueOf(b)));
        if (b != -1) {
            for (e eVar : this.i) {
                if (eVar.a(b) && eVar.a(aVar)) {
                    eVar.a(aVar, list);
                    return;
                }
            }
            return;
        }
        k.d(n, "bucketId is illegal, stop traffic");
    }
}
