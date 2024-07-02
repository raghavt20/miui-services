package com.xiaomi.abtest.c;

import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.d.k;
import java.util.List;
import java.util.TreeSet;

/* loaded from: classes.dex */
public class d extends e {
    private static final String n = "Experiment";

    public d(int i, String str, EnumType.FlowUnitType flowUnitType, int i2, EnumType.FlowUnitStatus flowUnitStatus, TreeSet<Integer> treeSet, String str2, String str3, String str4) {
        super(i, str, flowUnitType, i2, flowUnitStatus, str3, str4);
        this.j = treeSet;
        this.l = a.a(str2);
    }

    @Override // com.xiaomi.abtest.c.e
    public void a(com.xiaomi.abtest.b.a aVar, List<e> list) {
        k.d(n, String.format("Add this experiment, id: %d, name: %s", Integer.valueOf(a()), b()));
        list.add(this);
    }
}
