package com.xiaomi.abtest.d;

import android.text.TextUtils;
import com.android.server.pm.CloudControlPreinstallService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/* loaded from: classes.dex */
public class i {
    public static final int a = 10000;
    public static final int b = 15000;
    public static final String c = "OT_SID";
    public static final String d = "OT_ts";
    public static final String e = "OT_net";
    public static final String f = "OT_sender";
    public static final String g = "OT_protocol";
    private static String h = "HttpUtil";
    private static final String i = "GET";
    private static final String j = "POST";
    private static final String k = "&";
    private static final String l = "=";
    private static final String m = "UTF-8";
    private static final String n = "miui_sdkconfig_jafej!@#)(*e@!#";

    private i() {
    }

    public static String a(String str) throws IOException {
        return a(str, null, false);
    }

    public static String a(String str, Map<String, String> map) throws IOException {
        return a(str, map, true);
    }

    public static String a(String str, Map<String, String> map, boolean z) throws IOException {
        return a(i, str, map, z);
    }

    public static String b(String str, Map<String, String> map, boolean z) throws IOException {
        return a(j, str, map, z);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r11v0, types: [java.util.Map<java.lang.String, java.lang.String>, java.util.Map] */
    /* JADX WARN: Type inference failed for: r11v1, types: [java.io.OutputStream] */
    /* JADX WARN: Type inference failed for: r11v10 */
    /* JADX WARN: Type inference failed for: r11v11 */
    /* JADX WARN: Type inference failed for: r11v12, types: [java.io.OutputStream] */
    /* JADX WARN: Type inference failed for: r11v13 */
    /* JADX WARN: Type inference failed for: r11v15 */
    /* JADX WARN: Type inference failed for: r11v3 */
    /* JADX WARN: Type inference failed for: r11v4 */
    /* JADX WARN: Type inference failed for: r11v5, types: [java.io.OutputStream] */
    /* JADX WARN: Type inference failed for: r11v7 */
    /* JADX WARN: Type inference failed for: r11v8 */
    /* JADX WARN: Type inference failed for: r11v9, types: [java.io.OutputStream] */
    /* JADX WARN: Type inference failed for: r12v0, types: [boolean] */
    /* JADX WARN: Type inference failed for: r12v1, types: [java.net.HttpURLConnection] */
    /* JADX WARN: Type inference failed for: r12v2 */
    /* JADX WARN: Type inference failed for: r12v3 */
    /* JADX WARN: Type inference failed for: r12v4, types: [java.net.HttpURLConnection] */
    /* JADX WARN: Type inference failed for: r12v9, types: [java.net.HttpURLConnection] */
    /* JADX WARN: Type inference failed for: r2v0 */
    /* JADX WARN: Type inference failed for: r2v1 */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.io.InputStream] */
    /* JADX WARN: Type inference failed for: r2v3 */
    /* JADX WARN: Type inference failed for: r2v4 */
    private static String a(String str, String str2, Map<String, String> map, boolean z) {
        String a2;
        InputStream inputStream;
        ?? r2 = 0;
        r2 = 0;
        r2 = 0;
        try {
            if (map == 0) {
                a2 = null;
            } else {
                try {
                    a2 = a((Map<String, String>) map, (boolean) z);
                } catch (Exception e2) {
                    e = e2;
                    map = 0;
                    z = 0;
                    inputStream = null;
                    k.b(h, "HttpUtils POST 上传异常", e);
                    j.a(inputStream);
                    j.a((OutputStream) map);
                    j.a((HttpURLConnection) z);
                    return null;
                } catch (Throwable th) {
                    th = th;
                    map = 0;
                    z = 0;
                    j.a((InputStream) r2);
                    j.a((OutputStream) map);
                    j.a((HttpURLConnection) z);
                    throw th;
                }
            }
            z = (HttpURLConnection) new URL((!i.equals(str) || a2 == null) ? str2 : str2 + "? " + a2).openConnection();
            try {
                z.setConnectTimeout(10000);
                z.setReadTimeout(b);
                try {
                    if (i.equals(str)) {
                        z.setRequestMethod(i);
                    } else if (j.equals(str) && a2 != null) {
                        z.setRequestMethod(j);
                        z.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        z.setDoOutput(true);
                        byte[] bytes = a2.getBytes(m);
                        map = z.getOutputStream();
                        try {
                            map.write(bytes, 0, bytes.length);
                            map.flush();
                            map = map;
                            int responseCode = z.getResponseCode();
                            inputStream = z.getInputStream();
                            byte[] b2 = j.b(inputStream);
                            k.a(h, String.format("HttpUtils POST 上传成功 url: %s, code: %s", str2, Integer.valueOf(responseCode)));
                            String str3 = new String(b2, m);
                            j.a(inputStream);
                            j.a((OutputStream) map);
                            j.a((HttpURLConnection) z);
                            return str3;
                        } catch (Exception e3) {
                            e = e3;
                            inputStream = null;
                            k.b(h, "HttpUtils POST 上传异常", e);
                            j.a(inputStream);
                            j.a((OutputStream) map);
                            j.a((HttpURLConnection) z);
                            return null;
                        } catch (Throwable th2) {
                            th = th2;
                            j.a((InputStream) r2);
                            j.a((OutputStream) map);
                            j.a((HttpURLConnection) z);
                            throw th;
                        }
                    }
                    byte[] b22 = j.b(inputStream);
                    k.a(h, String.format("HttpUtils POST 上传成功 url: %s, code: %s", str2, Integer.valueOf(responseCode)));
                    String str32 = new String(b22, m);
                    j.a(inputStream);
                    j.a((OutputStream) map);
                    j.a((HttpURLConnection) z);
                    return str32;
                } catch (Exception e4) {
                    e = e4;
                    k.b(h, "HttpUtils POST 上传异常", e);
                    j.a(inputStream);
                    j.a((OutputStream) map);
                    j.a((HttpURLConnection) z);
                    return null;
                }
                map = 0;
                int responseCode2 = z.getResponseCode();
                inputStream = z.getInputStream();
            } catch (Exception e5) {
                e = e5;
                map = 0;
                inputStream = null;
            } catch (Throwable th3) {
                th = th3;
                map = 0;
            }
        } catch (Throwable th4) {
            th = th4;
            r2 = j;
        }
    }

    private static String a(Map<String, String> map, boolean z) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            try {
                if (!TextUtils.isEmpty(entry.getKey())) {
                    if (sb.length() > 0) {
                        sb.append(k);
                    }
                    sb.append(URLEncoder.encode(entry.getKey(), m));
                    sb.append(l);
                    sb.append(URLEncoder.encode(entry.getValue() == null ? "null" : entry.getValue(), m));
                }
            } catch (UnsupportedEncodingException e2) {
                k.b(h, "format params failed");
            }
        }
        if (z) {
            String a2 = a(map);
            if (sb.length() > 0) {
                sb.append(k);
            }
            sb.append(URLEncoder.encode(CloudControlPreinstallService.ConnectEntity.SIGN, m));
            sb.append(l);
            sb.append(URLEncoder.encode(a2, m));
        }
        return sb.toString();
    }

    public static String a(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        if (map != null) {
            ArrayList<String> arrayList = new ArrayList(map.keySet());
            Collections.sort(arrayList);
            for (String str : arrayList) {
                if (!TextUtils.isEmpty(str)) {
                    sb.append(str);
                    sb.append(map.get(str));
                }
            }
        }
        sb.append(n);
        return b.c(sb.toString());
    }
}
