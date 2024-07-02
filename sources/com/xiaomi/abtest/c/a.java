package com.xiaomi.abtest.c;

import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.d.k;
import com.xiaomi.abtest.d.l;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class a {
    private static final String a = "Condition";
    private EnumType.ConditionRelation b;
    private ArrayList<C0002a> c;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.xiaomi.abtest.c.a$a, reason: collision with other inner class name */
    /* loaded from: classes.dex */
    public static class C0002a {
        public String a;
        public EnumType.ConditionOperator b;
        public Object c;

        C0002a() {
        }

        public String toString() {
            return this.a + "\t" + this.b.name() + "\t" + String.valueOf(this.c);
        }
    }

    public static a a(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        try {
            JSONObject jSONObject = new JSONObject(str);
            a aVar = new a();
            aVar.c = new ArrayList<>();
            aVar.b = EnumType.ConditionRelation.valueOf(jSONObject.optString("relation"));
            JSONArray optJSONArray = jSONObject.optJSONArray("filters");
            for (int i = 0; i < optJSONArray.length(); i++) {
                JSONObject optJSONObject = optJSONArray.optJSONObject(i);
                C0002a c0002a = new C0002a();
                c0002a.a = optJSONObject.optString("propertyName");
                c0002a.c = optJSONObject.optString("propertyValue");
                c0002a.b = EnumType.ConditionOperator.valueOf(optJSONObject.optInt("operator"));
                if (c0002a.b == EnumType.ConditionOperator.OP_IN) {
                    String[] split = ((String) c0002a.c).split(",");
                    HashSet hashSet = new HashSet();
                    for (String str2 : split) {
                        hashSet.add(str2);
                    }
                    c0002a.c = hashSet;
                }
                aVar.c.add(c0002a);
            }
            return aVar;
        } catch (Exception e) {
            k.a(a, "", e);
            return null;
        }
    }

    public boolean a(Map<String, String> map) throws Exception {
        boolean z;
        boolean z2;
        if (map == null && b().size() > 0) {
            return false;
        }
        if (this.b == EnumType.ConditionRelation.AND) {
            z = true;
        } else if (this.b == EnumType.ConditionRelation.OR) {
            z = false;
        } else {
            throw new Exception("invalid relation");
        }
        Iterator<C0002a> it = this.c.iterator();
        while (it.hasNext()) {
            C0002a next = it.next();
            String str = map.get(next.a);
            if (str == null) {
                return false;
            }
            if (next.b == EnumType.ConditionOperator.OP_EQ) {
                z2 = str.equals(next.c);
            } else {
                if (next.b == EnumType.ConditionOperator.OP_GT) {
                    if (l.a(str) && l.a((String) next.c)) {
                        z2 = Double.parseDouble(str) > Double.parseDouble((String) next.c);
                    } else if (next.c instanceof String) {
                        z2 = str.compareTo((String) next.c) > 0;
                    } else {
                        Object[] objArr = new Object[3];
                        objArr[0] = next.a;
                        objArr[1] = str != null ? str.getClass().getName() : "null";
                        objArr[2] = next.c.getClass().getName();
                        throw new Exception(String.format("%s value type not match, get:%s but need:%s", objArr));
                    }
                } else if (next.b == EnumType.ConditionOperator.OP_GE) {
                    if (l.a(str) && l.a((String) next.c)) {
                        z2 = Double.parseDouble(str) >= Double.parseDouble((String) next.c);
                    } else if (next.c instanceof String) {
                        z = z && str.compareTo((String) next.c) >= 0;
                        z2 = false;
                    } else {
                        Object[] objArr2 = new Object[3];
                        objArr2[0] = next.a;
                        objArr2[1] = str != null ? str.getClass().getName() : "null";
                        objArr2[2] = next.c.getClass().getName();
                        throw new Exception(String.format("%s value type not match, get:%s but need:%s", objArr2));
                    }
                } else if (next.b == EnumType.ConditionOperator.OP_LT) {
                    if (l.a(str) && l.a((String) next.c)) {
                        z2 = Double.parseDouble(str) < Double.parseDouble((String) next.c);
                    } else if (next.c instanceof String) {
                        z2 = str.compareTo((String) next.c) < 0;
                    } else {
                        Object[] objArr3 = new Object[3];
                        objArr3[0] = next.a;
                        objArr3[1] = str != null ? str.getClass().getName() : "null";
                        objArr3[2] = next.c.getClass().getName();
                        throw new Exception(String.format("%s value type not match, get:%s but need:%s", objArr3));
                    }
                } else if (next.b == EnumType.ConditionOperator.OP_LE) {
                    if (l.a(str) && l.a((String) next.c)) {
                        z2 = Double.parseDouble(str) <= Double.parseDouble((String) next.c);
                    } else if (next.c instanceof String) {
                        z2 = str.compareTo((String) next.c) <= 0;
                    } else {
                        Object[] objArr4 = new Object[3];
                        objArr4[0] = next.a;
                        objArr4[1] = str != null ? str.getClass().getName() : "null";
                        objArr4[2] = next.c.getClass().getName();
                        throw new Exception(String.format("%s value type not match, get:%s but need:%s", objArr4));
                    }
                } else if (next.b == EnumType.ConditionOperator.OP_IN) {
                    if (!(next.c instanceof HashSet)) {
                        throw new Exception("operator is IN, but property value is not a SET");
                    }
                    boolean contains = ((HashSet) next.c).contains(str);
                    System.out.printf("%s contains %s:%s\n", next.a, str, Boolean.valueOf(contains));
                    z2 = contains;
                } else {
                    throw new Exception("invalid operator");
                }
            }
            if (!z2 && this.b == EnumType.ConditionRelation.AND) {
                return false;
            }
            if (z2 && this.b == EnumType.ConditionRelation.OR) {
                return true;
            }
            if (this.b == EnumType.ConditionRelation.AND) {
                z = z && z2;
            } else if (this.b == EnumType.ConditionRelation.OR) {
                z = z || z2;
            }
        }
        return z;
    }

    public EnumType.ConditionRelation a() {
        return this.b;
    }

    public ArrayList<C0002a> b() {
        return this.c;
    }

    public void a(EnumType.ConditionRelation conditionRelation) {
        this.b = conditionRelation;
    }

    public void a(ArrayList<C0002a> arrayList) {
        this.c = arrayList;
    }
}
