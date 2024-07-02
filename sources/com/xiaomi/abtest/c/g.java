package com.xiaomi.abtest.c;

import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.d.k;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class g extends e {
    private static final String n = "Layer";
    private EnumType.FlowUnitType o;

    public g(int i, String str, EnumType.FlowUnitType flowUnitType, int i2, EnumType.FlowUnitStatus flowUnitStatus, EnumType.DiversionType diversionType, int i3, String str2, String str3) {
        super(i, str, flowUnitType, i2, flowUnitStatus, i3, diversionType, str2, str3);
        this.o = EnumType.FlowUnitType.TYPE_UNKNOWN;
        this.i = new ArrayList();
    }

    @Override // com.xiaomi.abtest.c.e
    public void a(e eVar) {
        if (this.i == null) {
            k.c(n, "children haven't been initialized");
            return;
        }
        if (!eVar.c().equals(EnumType.FlowUnitType.TYPE_DOMAIN) && !eVar.c().equals(EnumType.FlowUnitType.TYPE_EXPERIMENT) && !eVar.c().equals(EnumType.FlowUnitType.TYPE_EXP_CONTAINER)) {
            k.c(n, "added child must be TYPE_DOMAIN or TYPE_EXPERIMENT or TYPE_EXPERIMENT");
        } else if (this.o.equals(EnumType.FlowUnitType.TYPE_UNKNOWN) || this.o.equals(eVar.c())) {
            this.i.add(eVar);
        } else {
            k.c(n, "child of layer must be ");
        }
    }

    @Override // com.xiaomi.abtest.c.e
    public boolean a(com.xiaomi.abtest.b.a aVar) {
        return false;
    }

    @Override // com.xiaomi.abtest.c.e
    public void a(com.xiaomi.abtest.b.a aVar, List<e> list) {
        k.d(n, String.format("id: %d, name: %s", Integer.valueOf(a()), b()));
        if (this.i.size() <= 0) {
            k.d(n, "no subdomain or experiment in this layer.");
            return;
        }
        int b = b(aVar);
        k.d(n, String.format("bucketId: %s", Integer.valueOf(b)));
        if (b != -1) {
            for (e eVar : this.i) {
                if (eVar.a(b)) {
                    if (eVar.a(aVar)) {
                        eVar.a(aVar, list);
                    } else {
                        k.d(n, String.format("check condition failed for:%s", eVar.toString()));
                    }
                }
            }
            return;
        }
        k.d(n, "bucketId is illegal, stop traffic");
    }
}
