package com.xiaomi.abtest.c;

import android.text.TextUtils;
import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.d.h;
import com.xiaomi.abtest.d.k;
import com.xiaomi.abtest.d.l;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/* loaded from: classes.dex */
public class e {
    private static final String n = "FlowUnit";
    protected int a;
    protected String b;
    protected EnumType.FlowUnitType c;
    protected int d;
    protected EnumType.FlowUnitStatus e;
    protected String f;
    protected String g;
    protected EnumType.DiversionType h;
    protected List<e> i;
    protected Set<Integer> j;
    protected Map<String, String> k;
    protected a l;
    protected int m;

    public e(int i, String str, EnumType.FlowUnitType flowUnitType, int i2, EnumType.FlowUnitStatus flowUnitStatus, String str2, String str3) {
        this.a = i;
        this.b = str;
        this.c = flowUnitType;
        this.d = i2;
        this.e = flowUnitStatus;
        this.f = str2;
        this.g = str3;
        this.k = new HashMap();
    }

    public e(int i, String str, EnumType.FlowUnitType flowUnitType, int i2, EnumType.FlowUnitStatus flowUnitStatus, int i3, EnumType.DiversionType diversionType, String str2, String str3) {
        this(i, str, flowUnitType, i2, flowUnitStatus, str2, str3);
        this.m = i3;
        this.h = diversionType;
    }

    public String toString() {
        String str = "";
        String str2 = "";
        for (Map.Entry<String, String> entry : this.k.entrySet()) {
            str2 = str2 + entry.getKey() + "=" + entry.getValue() + ",";
        }
        Set<Integer> set = this.j;
        if (set != null) {
            Iterator<Integer> it = set.iterator();
            while (it.hasNext()) {
                str = str + String.valueOf(it.next()) + ",";
            }
        }
        return String.format("%d,%s,%s,params:[%s] bucketIds:[%s]", Integer.valueOf(a()), b(), c().toString(), str2, str);
    }

    public void a(e eVar) {
        this.i.add(eVar);
    }

    public boolean a(int i) {
        return this.j.contains(Integer.valueOf(i));
    }

    public boolean a(com.xiaomi.abtest.b.a aVar) {
        a aVar2 = this.l;
        if (aVar2 == null) {
            return true;
        }
        try {
            return aVar2.a(aVar.c());
        } catch (Exception e) {
            k.b(n, e.getMessage());
            return false;
        }
    }

    public void a(com.xiaomi.abtest.b.a aVar, List<e> list) {
        k.c(n, "Base class FlowUnit has no traffic method");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int b(com.xiaomi.abtest.b.a aVar) {
        byte[] bArr;
        switch (f.a[this.h.ordinal()]) {
            case 1:
                if (l.b(aVar.a())) {
                    bArr = aVar.a().getBytes();
                    break;
                } else {
                    if (com.xiaomi.abtest.d.d.f.equals(b()) || com.xiaomi.abtest.d.d.e.equals(b())) {
                        if (l.b(aVar.b())) {
                            bArr = aVar.b().getBytes();
                            break;
                        } else {
                            bArr = "000".getBytes();
                            break;
                        }
                    }
                    bArr = null;
                    break;
                }
                break;
            case 2:
                if (!TextUtils.isEmpty(aVar.b())) {
                    bArr = aVar.b().getBytes();
                    break;
                }
                bArr = null;
                break;
            case 3:
                bArr = new byte[20];
                new Random().nextBytes(bArr);
                break;
            case 4:
                bArr = (aVar.a() + "-" + Calendar.getInstance().get(6)).getBytes();
                break;
            case 5:
                bArr = (aVar.a() + "-" + Calendar.getInstance().get(3)).getBytes();
                break;
            case 6:
                bArr = (aVar.a() + "-" + Calendar.getInstance().get(2)).getBytes();
                break;
            default:
                bArr = null;
                break;
        }
        if (bArr == null) {
            return -1;
        }
        return h.a(bArr, bArr.length, this.m) % com.xiaomi.abtest.d.d.a.intValue();
    }

    public void a(Map<String, String> map) {
        this.k.putAll(map);
    }

    public void b(int i) {
        this.a = i;
    }

    public void a(String str) {
        this.b = str;
    }

    public void a(EnumType.FlowUnitType flowUnitType) {
        this.c = flowUnitType;
    }

    public void c(int i) {
        this.d = i;
    }

    public void a(EnumType.FlowUnitStatus flowUnitStatus) {
        this.e = flowUnitStatus;
    }

    public void b(String str) {
        this.f = str;
    }

    public void a(EnumType.DiversionType diversionType) {
        this.h = diversionType;
    }

    public void c(String str) {
        this.g = str;
    }

    public int a() {
        return this.a;
    }

    public String b() {
        return this.b;
    }

    public EnumType.FlowUnitType c() {
        return this.c;
    }

    public int d() {
        return this.d;
    }

    public EnumType.FlowUnitStatus e() {
        return this.e;
    }

    public EnumType.DiversionType f() {
        return this.h;
    }

    public List<e> g() {
        return this.i;
    }

    public Set<Integer> h() {
        return this.j;
    }

    public Map<String, String> i() {
        return this.k;
    }

    public String j() {
        return this.f;
    }

    public String k() {
        return this.g;
    }
}
