package com.xiaomi.abtest.c;

import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.d.k;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/* loaded from: classes.dex */
public class b extends e {
    private static final String n = "Domain";

    public b(int i, String str, EnumType.FlowUnitType flowUnitType, int i2, EnumType.FlowUnitStatus flowUnitStatus, TreeSet<Integer> treeSet, String str2, String str3, String str4) {
        super(i, str, flowUnitType, i2, flowUnitStatus, str3, str4);
        this.i = new ArrayList();
        this.j = treeSet;
        this.l = a.a(str2);
    }

    @Override // com.xiaomi.abtest.c.e
    public void a(e eVar) {
        if (this.i == null) {
            k.c(n, "children haven't been initialized");
        } else if (!eVar.c().equals(EnumType.FlowUnitType.TYPE_LAYER)) {
            k.c(n, "added child must be TYPE_LAYER");
        } else {
            this.i.add(eVar);
        }
    }

    @Override // com.xiaomi.abtest.c.e
    public void a(com.xiaomi.abtest.b.a aVar, List<e> list) {
        k.d(n, String.format("id: %d, name: %s", Integer.valueOf(a()), b()));
        if (this.i.size() <= 0) {
            k.d(n, "no layer in this domain.");
            return;
        }
        Iterator<e> it = this.i.iterator();
        while (it.hasNext()) {
            it.next().a(aVar, list);
        }
    }
}
