package com.xiaomi.abtest.a;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import com.xiaomi.abtest.ABTest;
import com.xiaomi.abtest.ABTestConfig;
import com.xiaomi.abtest.EnumType;
import com.xiaomi.abtest.ExperimentInfo;
import com.xiaomi.abtest.c.g;
import com.xiaomi.abtest.d.i;
import com.xiaomi.abtest.d.k;
import com.xiaomi.abtest.d.l;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class a {
    private static final String a = "ExpPlatformManager";
    private static final String b = "id";
    private static final String c = "type";
    private static final String d = "status";
    private static final String e = "bucketIds";
    private static final String f = "name";
    private static final String g = "fid";
    private static final String h = "fPath";
    private static final String i = "xPath";
    private static final String j = "conditionString";
    private static final String k = "diversionType";
    private static final String l = "hashSeed";
    private static final String m = "parameters";
    private static final String n = "children";
    private static final ExecutorService o = Executors.newSingleThreadExecutor();
    private static volatile a y;
    private int s;
    private boolean u;
    private Handler v;
    private boolean w;
    private String x;
    private long z;
    private Set<String> p = new TreeSet();
    private Map<String, com.xiaomi.abtest.c.b> q = new HashMap();
    private Map<String, Map<String, Map<String, ExperimentInfo>>> r = new HashMap();
    private boolean t = true;
    private Application.ActivityLifecycleCallbacks A = new e(this);

    public static void a(ABTestConfig aBTestConfig) {
        if (y == null) {
            synchronized (a.class) {
                if (y == null) {
                    y = new a(aBTestConfig);
                }
            }
        }
    }

    public void a() {
        this.p.clear();
        this.q.clear();
        this.r.clear();
        this.s = 0;
        this.u = false;
        this.w = false;
        this.t = true;
        this.x = "";
        this.z = 0L;
        ((Application) com.xiaomi.abtest.d.a.a().getApplicationContext()).unregisterActivityLifecycleCallbacks(this.A);
        y = null;
    }

    public static a b() {
        return y;
    }

    private a(ABTestConfig aBTestConfig) {
        if (aBTestConfig != null) {
            this.x = aBTestConfig.getLayerName();
            boolean isDisableLoadTimer = aBTestConfig.isDisableLoadTimer();
            this.w = isDisableLoadTimer;
            if (!isDisableLoadTimer && aBTestConfig.getLoadConfigInterval() > 0) {
                if (aBTestConfig.getLoadConfigInterval() < 7200) {
                    this.s = com.xiaomi.abtest.d.d.j;
                } else {
                    this.s = aBTestConfig.getLoadConfigInterval();
                }
            }
        }
        o.execute(new b(this));
        a(com.xiaomi.abtest.d.a.a());
    }

    private void d() {
        int i2 = this.s;
        if (i2 <= 0) {
            i2 = com.xiaomi.abtest.d.d.j;
        }
        this.v = new Handler(Looper.getMainLooper());
        this.v.postDelayed(new c(this, i2), i2 * 1000);
    }

    public void a(boolean z) {
        this.u = z;
    }

    public void a(String str) {
        if (TextUtils.isEmpty(str)) {
            k.a(a, "appName is empty, skip it!");
            return;
        }
        this.p.add(str);
        a((ABTest.OnLoadRemoteConfigListener) null);
        if (!this.w) {
            d();
        }
    }

    public void c() {
        this.z = SystemClock.elapsedRealtime();
        String a2 = com.xiaomi.abtest.d.f.a(com.xiaomi.abtest.d.d.i);
        k.a(a, String.format("load local config finished, cost: %s ms", Long.valueOf(SystemClock.elapsedRealtime() - this.z)));
        k.a(a, "localConfig : " + a2);
        if (l.b(a2)) {
            a(a2, true);
        }
    }

    public void a(ABTest.OnLoadRemoteConfigListener onLoadRemoteConfigListener) {
        o.execute(new d(this, onLoadRemoteConfigListener));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void e() {
        if (this.p.size() == 0) {
            k.a(a, "no appNames to load remote configuration");
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = this.p.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(com.xiaomi.abtest.d.d.h);
        }
        if (sb.length() > 0) {
            StringBuilder sb2 = new StringBuilder(com.xiaomi.abtest.d.d.g);
            sb2.append(sb.substring(0, sb.length() - 1));
            if (!TextUtils.isEmpty(this.x)) {
                sb2.append("/");
                sb2.append(this.x);
            }
            try {
                k.a(a, "load remote configuration, url: : " + sb2.toString());
                String a2 = i.a(sb2.toString());
                k.a(a, "load remote configuration, response: " + a2);
                if (a2 != null) {
                    a(a2, false);
                }
            } catch (Exception e2) {
                k.b(a, "load remote configuration error : " + e2.getMessage());
            }
        }
    }

    private void a(String str, boolean z) {
        String jSONObject;
        StringBuilder sb;
        JSONObject jSONObject2;
        JSONObject jSONObject3;
        k.a(a, "parse expConfig start");
        JSONObject jSONObject4 = null;
        try {
            try {
                jSONObject2 = new JSONObject(str);
            } catch (Exception e2) {
                e = e2;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            JSONObject optJSONObject = jSONObject2.optJSONObject("expInfo");
            Iterator<String> keys = optJSONObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                this.q.put(next, (com.xiaomi.abtest.c.b) b(optJSONObject.optJSONObject(next)));
            }
            if (z) {
                k.a(a, String.format("parse expInfo finished. It takes %s ms from the beginning of reading the local config to now", Long.valueOf(SystemClock.elapsedRealtime() - this.z)));
            }
            HashMap hashMap = new HashMap();
            JSONObject optJSONObject2 = jSONObject2.optJSONObject("whitelist");
            Iterator<String> keys2 = optJSONObject2.keys();
            while (keys2.hasNext()) {
                String next2 = keys2.next();
                JSONObject optJSONObject3 = optJSONObject2.optJSONObject(next2);
                Iterator<String> keys3 = optJSONObject3.keys();
                HashMap hashMap2 = new HashMap();
                while (keys3.hasNext()) {
                    String next3 = keys3.next();
                    JSONObject optJSONObject4 = optJSONObject3.optJSONObject(next3);
                    Iterator<String> keys4 = optJSONObject4.keys();
                    HashMap hashMap3 = new HashMap();
                    while (keys4.hasNext()) {
                        String next4 = keys4.next();
                        JSONObject jSONObject5 = optJSONObject2;
                        if (TextUtils.isEmpty(this.x)) {
                            jSONObject3 = optJSONObject3;
                        } else {
                            jSONObject3 = optJSONObject3;
                            String substring = next4.substring(("/" + next2 + com.xiaomi.abtest.d.d.b).length());
                            if (TextUtils.isEmpty(substring) || !substring.toUpperCase().contains(this.x.toUpperCase())) {
                                k.a(a, "the fPath " + next4 + "doesn't meet the filter conditions, skip it!");
                                keys4.remove();
                                optJSONObject2 = jSONObject5;
                                optJSONObject3 = jSONObject3;
                            }
                        }
                        ExperimentInfo a2 = a(optJSONObject4.optJSONObject(next4));
                        if (a2 != null) {
                            hashMap3.put(next4, a2);
                        }
                        optJSONObject2 = jSONObject5;
                        optJSONObject3 = jSONObject3;
                    }
                    JSONObject jSONObject6 = optJSONObject2;
                    JSONObject jSONObject7 = optJSONObject3;
                    if (hashMap3.size() == 0) {
                        keys3.remove();
                    } else {
                        hashMap2.put(next3, hashMap3);
                    }
                    optJSONObject2 = jSONObject6;
                    optJSONObject3 = jSONObject7;
                }
                JSONObject jSONObject8 = optJSONObject2;
                if (hashMap2.size() == 0) {
                    keys2.remove();
                } else {
                    hashMap.put(next2, hashMap2);
                }
                optJSONObject2 = jSONObject8;
            }
            if (hashMap.size() > 0) {
                this.r = hashMap;
            }
            if (z) {
                k.a(a, String.format("parse whitelist finished. It takes %s ms from the beginning of reading the local config to now", Long.valueOf(SystemClock.elapsedRealtime() - this.z)));
            }
            jSONObject = jSONObject2.toString();
            sb = new StringBuilder();
        } catch (Exception e3) {
            e = e3;
            jSONObject4 = jSONObject2;
            k.b(a, "parseExpConfig error : " + e.getMessage());
            if (jSONObject4 != null) {
                jSONObject = jSONObject4.toString();
                sb = new StringBuilder();
                k.a(a, sb.append("save the data to local file, data : ").append(jSONObject).toString());
                com.xiaomi.abtest.d.f.a(com.xiaomi.abtest.d.d.i, jSONObject);
            }
            return;
        } catch (Throwable th2) {
            th = th2;
            jSONObject4 = jSONObject2;
            if (jSONObject4 != null) {
                String jSONObject9 = jSONObject4.toString();
                k.a(a, "save the data to local file, data : " + jSONObject9);
                com.xiaomi.abtest.d.f.a(com.xiaomi.abtest.d.d.i, jSONObject9);
            }
            throw th;
        }
        k.a(a, sb.append("save the data to local file, data : ").append(jSONObject).toString());
        com.xiaomi.abtest.d.f.a(com.xiaomi.abtest.d.d.i, jSONObject);
    }

    private ExperimentInfo a(JSONObject jSONObject) {
        if (jSONObject == null) {
            return null;
        }
        ExperimentInfo experimentInfo = new ExperimentInfo();
        experimentInfo.expId = jSONObject.optInt("expId");
        experimentInfo.xpath = jSONObject.optString(i);
        experimentInfo.params = new HashMap();
        JSONObject optJSONObject = jSONObject.optJSONObject("params");
        if (optJSONObject != null) {
            Iterator<String> keys = optJSONObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                experimentInfo.params.put(next, optJSONObject.optString(next));
            }
        }
        return experimentInfo;
    }

    private void a(Map<String, ExperimentInfo> map) {
        String replaceFirst;
        for (Map.Entry<String, ExperimentInfo> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.contains(com.xiaomi.abtest.d.d.b)) {
                replaceFirst = key.replaceFirst(com.xiaomi.abtest.d.d.b, com.xiaomi.abtest.d.d.d);
            } else if (key.contains(com.xiaomi.abtest.d.d.c)) {
                replaceFirst = key.replaceFirst(com.xiaomi.abtest.d.d.c, com.xiaomi.abtest.d.d.d);
            }
            if (map.containsKey(replaceFirst)) {
                ExperimentInfo experimentInfo = map.get(replaceFirst);
                ExperimentInfo value = entry.getValue();
                HashMap hashMap = new HashMap();
                hashMap.putAll(experimentInfo.getParams());
                hashMap.putAll(value.getParams());
                value.setParams(hashMap);
            }
        }
        HashMap hashMap2 = new HashMap();
        for (Map.Entry<String, ExperimentInfo> entry2 : map.entrySet()) {
            String key2 = entry2.getKey();
            if (key2.contains(com.xiaomi.abtest.d.d.d)) {
                String replaceFirst2 = key2.replaceFirst(com.xiaomi.abtest.d.d.d, com.xiaomi.abtest.d.d.b);
                if (!map.containsKey(replaceFirst2)) {
                    String replaceFirst3 = key2.replaceFirst(com.xiaomi.abtest.d.d.d, com.xiaomi.abtest.d.d.c);
                    if (!map.containsKey(replaceFirst3)) {
                        hashMap2.put(replaceFirst2, entry2.getValue());
                        hashMap2.put(replaceFirst3, entry2.getValue());
                    }
                }
            }
        }
        map.putAll(hashMap2);
    }

    public Map<String, ExperimentInfo> a(com.xiaomi.abtest.b.a aVar) {
        com.xiaomi.abtest.c.e eVar = (com.xiaomi.abtest.c.b) this.q.get(aVar.d());
        if (eVar == null) {
            k.c(a, String.format("no appDomain found for appId:%s,appName:%s", aVar.a(), aVar.d()));
            return null;
        }
        List<com.xiaomi.abtest.c.e> arrayList = new ArrayList<>();
        eVar.a(aVar, arrayList);
        Map<String, ExperimentInfo> hashMap = new HashMap<>();
        for (com.xiaomi.abtest.c.e eVar2 : arrayList) {
            ExperimentInfo experimentInfo = new ExperimentInfo();
            experimentInfo.setExpId(eVar2.a());
            experimentInfo.setContainerId(eVar2.d());
            experimentInfo.setLayerId(a(eVar2.d(), eVar));
            experimentInfo.setXpath(eVar2.j());
            experimentInfo.setParams(eVar2.i());
            hashMap.put(eVar2.k(), experimentInfo);
        }
        k.a(a, "get data from the expInfo: " + hashMap.toString());
        if (this.r.get(aVar.d()) == null || !this.r.get(aVar.d()).containsKey(aVar.a())) {
            k.a(a, "no data needed in whitelist");
        } else {
            for (Map.Entry<String, ExperimentInfo> entry : this.r.get(aVar.d()).get(aVar.a()).entrySet()) {
                hashMap.put(entry.getKey(), entry.getValue());
            }
        }
        k.a(a, "get data from the expInfo and whitelist: " + hashMap.toString());
        a(hashMap);
        k.a(a, "get data from the expInfo and whitelist and launch params: " + hashMap.toString());
        return hashMap;
    }

    private int a(int i2, com.xiaomi.abtest.c.e eVar) {
        if (eVar.a() == i2) {
            return eVar.d();
        }
        if (eVar.g() != null) {
            Iterator<com.xiaomi.abtest.c.e> it = eVar.g().iterator();
            while (it.hasNext()) {
                int a2 = a(i2, it.next());
                if (a2 > 0) {
                    return a2;
                }
            }
            return 0;
        }
        return 0;
    }

    private void a(Context context) {
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(this.A);
    }

    private com.xiaomi.abtest.c.e b(JSONObject jSONObject) {
        com.xiaomi.abtest.c.e bVar;
        EnumType.FlowUnitType valueOf = EnumType.FlowUnitType.valueOf(jSONObject.optString("type"));
        EnumType.FlowUnitStatus valueOf2 = EnumType.FlowUnitStatus.valueOf(jSONObject.optString("status"));
        int i2 = 0;
        switch (f.a[valueOf.ordinal()]) {
            case 1:
                JSONArray optJSONArray = jSONObject.optJSONArray(e);
                TreeSet treeSet = new TreeSet();
                for (int i3 = 0; i3 < optJSONArray.length(); i3++) {
                    treeSet.add(Integer.valueOf(optJSONArray.optInt(i3)));
                }
                bVar = new com.xiaomi.abtest.c.b(jSONObject.optInt(b), jSONObject.optString(f), valueOf, jSONObject.optInt(g), valueOf2, treeSet, jSONObject.optString(j), jSONObject.optString(i), jSONObject.optString(h));
                break;
            case 2:
                String optString = jSONObject.optString(f);
                if (!TextUtils.isEmpty(this.x) && (TextUtils.isEmpty(optString) || (!com.xiaomi.abtest.d.d.e.equals(optString) && !com.xiaomi.abtest.d.d.f.equals(optString) && !optString.toUpperCase().contains(this.x.toUpperCase())))) {
                    k.a(a, "the layerName " + optString + "doesn't meet the filter conditions, skip it!");
                    return null;
                }
                EnumType.DiversionType valueOf3 = EnumType.DiversionType.valueOf(jSONObject.optString(k));
                if (valueOf3 == null) {
                    k.c(a, String.format("invalid diversionType:%s", jSONObject.optString(k)));
                    return null;
                }
                bVar = new g(jSONObject.optInt(b), jSONObject.optString(f), valueOf, jSONObject.optInt(g), valueOf2, valueOf3, jSONObject.optInt(l), jSONObject.optString(i), jSONObject.optString(h));
                break;
                break;
            case 3:
            case 4:
                JSONArray optJSONArray2 = jSONObject.optJSONArray(e);
                TreeSet treeSet2 = new TreeSet();
                for (int i4 = 0; i4 < optJSONArray2.length(); i4++) {
                    treeSet2.add(Integer.valueOf(optJSONArray2.optInt(i4)));
                }
                bVar = new com.xiaomi.abtest.c.d(jSONObject.optInt(b), jSONObject.optString(f), valueOf, jSONObject.optInt(g), valueOf2, treeSet2, jSONObject.optString(j), jSONObject.optString(i), jSONObject.optString(h));
                break;
            case 5:
                EnumType.DiversionType valueOf4 = EnumType.DiversionType.valueOf(jSONObject.optString(k));
                if (valueOf4 == null) {
                    k.c(a, String.format("invalid diversionType:", jSONObject.optString(k)));
                    return null;
                }
                bVar = new com.xiaomi.abtest.c.c(jSONObject.optInt(b), jSONObject.optString(f), valueOf, jSONObject.optInt(g), valueOf2, jSONObject.optInt(l), valueOf4, jSONObject.optString(i), jSONObject.optString(h));
                break;
            default:
                bVar = null;
                break;
        }
        JSONObject optJSONObject = jSONObject.optJSONObject(m);
        Iterator<String> keys = optJSONObject.keys();
        Map<String, String> hashMap = new HashMap<>();
        while (keys.hasNext()) {
            String next = keys.next();
            hashMap.put(next, optJSONObject.optString(next));
        }
        bVar.a(hashMap);
        JSONArray optJSONArray3 = jSONObject.optJSONArray(n);
        if (optJSONArray3 != null) {
            while (i2 < optJSONArray3.length()) {
                com.xiaomi.abtest.c.e b2 = b(optJSONArray3.optJSONObject(i2));
                if (b2 != null) {
                    bVar.a(b2);
                } else {
                    optJSONArray3.remove(i2);
                    i2--;
                }
                i2++;
            }
        }
        return bVar;
    }
}
