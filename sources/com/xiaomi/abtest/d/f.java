package com.xiaomi.abtest.d;

import android.text.TextUtils;
import android.util.LruCache;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

/* loaded from: classes.dex */
public class f {
    private static final String a = "FileUtil";
    private static final String b = "abtest";
    private static LruCache<String, a> c = new g(1048576);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class a {
        String a;

        private a() {
        }

        /* synthetic */ a(g gVar) {
            this();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v2, types: [com.xiaomi.abtest.d.g] */
    /* JADX WARN: Type inference failed for: r0v3 */
    /* JADX WARN: Type inference failed for: r0v4, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r0v5, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r0v6 */
    /* JADX WARN: Type inference failed for: r0v7 */
    /* JADX WARN: Type inference failed for: r0v8 */
    public static void a(String str, String str2) {
        BufferedWriter bufferedWriter;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return;
        }
        ?? r0 = 0;
        r0 = 0;
        try {
            try {
                a aVar = new a(r0);
                aVar.a = str2;
                c.put(str, aVar);
                String a2 = a();
                File file = new File(a2);
                if (!file.exists()) {
                    file.mkdirs();
                }
                File file2 = new File(a2, str);
                if (!file2.exists()) {
                    file2.createNewFile();
                }
                bufferedWriter = new BufferedWriter(new FileWriter(file2), 1024);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            bufferedWriter.write(str2);
            bufferedWriter.flush();
            j.a(bufferedWriter);
        } catch (Exception e2) {
            r0 = bufferedWriter;
            e = e2;
            k.c(a, "put error:" + e.toString());
            j.a((Closeable) r0);
        } catch (Throwable th2) {
            r0 = bufferedWriter;
            th = th2;
            j.a((Closeable) r0);
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v4, types: [com.xiaomi.abtest.d.g] */
    /* JADX WARN: Type inference failed for: r0v6, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r0v8 */
    public static String a(String str) {
        BufferedReader bufferedReader;
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        a aVar = c.get(str);
        if (aVar != null) {
            return aVar.a;
        }
        ?? r0 = 0;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                File file = new File(a(), str);
                StringBuilder sb = new StringBuilder();
                if (file.exists()) {
                    bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    while (true) {
                        try {
                            String readLine = bufferedReader.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (Exception e) {
                            e = e;
                            r0 = bufferedReader;
                            k.c(a, "get error:" + e.toString());
                            j.a((Closeable) r0);
                            return "";
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader2 = bufferedReader;
                            j.a(bufferedReader2);
                            throw th;
                        }
                    }
                } else {
                    bufferedReader = null;
                }
                String sb2 = sb.toString();
                a aVar2 = new a(r0);
                aVar2.a = sb2;
                c.put(str, aVar2);
                j.a(bufferedReader);
                return sb2;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e2) {
            e = e2;
        }
    }

    private static String a() {
        return c("abtest");
    }

    private static String c(String str) {
        String str2 = com.xiaomi.abtest.d.a.a().getFilesDir().getAbsolutePath() + File.separator + str;
        File file = new File(str2);
        if (!file.exists()) {
            try {
                file.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return str2;
    }

    public static void b(String str) {
        try {
            if (TextUtils.isEmpty(str)) {
                return;
            }
            c.remove(str);
            File file = new File(a(), str);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        } catch (Exception e) {
            k.c(a, "clear error:" + e.toString());
        }
    }
}
