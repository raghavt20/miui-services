package com.xiaomi.abtest.d;

import android.text.TextUtils;
import android.util.Log;

/* loaded from: classes.dex */
public class k {
    private static final int b = 3000;
    private static final String c = "ABTest-Api-";
    private static final int d = 0;
    private static final int e = 1;
    private static final int f = 2;
    private static final int g = 3;
    private static final int h = 4;
    public static boolean a = false;
    private static boolean i = false;
    private static boolean j = false;

    public static void a() {
        try {
            String d2 = a.d();
            String a2 = m.a("debug.abtest.log");
            Log.d("ABTestSdk", "logOn: " + a2 + ",pkg:" + d2);
            j = (TextUtils.isEmpty(a2) || TextUtils.isEmpty(d2) || !TextUtils.equals(d2, a2)) ? false : true;
            b();
        } catch (Exception e2) {
            Log.e("ABTestSdk", "LogUtil static initializer: " + e2.toString());
        }
        Log.d("ABTestSdk", "log on: " + j);
    }

    public static void a(String str, String str2) {
        if (a) {
            a(a(str), str2, 3);
        }
    }

    public static void a(String str, String str2, Throwable th) {
        if (a) {
            Log.d(a(str), str2, th);
        }
    }

    public static void b(String str, String str2) {
        if (a) {
            a(a(str), str2, 0);
        }
    }

    public static void b(String str, String str2, Throwable th) {
        if (a) {
            Log.e(a(str), str2, th);
        }
    }

    public static void c(String str, String str2) {
        if (a) {
            a(a(str), str2, 1);
        }
    }

    public static void c(String str, String str2, Throwable th) {
        if (a) {
            Log.w(a(str), str2, th);
        }
    }

    public static void d(String str, String str2) {
        if (a) {
            a(a(str), str2, 2);
        }
    }

    public static void d(String str, String str2, Throwable th) {
        if (a) {
            Log.i(a(str), str2, th);
        }
    }

    private static void a(String str, String str2, int i2) {
        if (str2 == null) {
            return;
        }
        int i3 = 0;
        while (i3 <= str2.length() / b) {
            int i4 = i3 * b;
            i3++;
            int min = Math.min(str2.length(), i3 * b);
            if (i4 < min) {
                String substring = str2.substring(i4, min);
                switch (i2) {
                    case 0:
                        Log.e(str, substring);
                        break;
                    case 1:
                        Log.w(str, substring);
                        break;
                    case 2:
                        Log.i(str, substring);
                        break;
                    case 3:
                        Log.d(str, substring);
                        break;
                    case 4:
                        Log.v(str, substring);
                        break;
                }
            }
        }
    }

    public static String a(String str) {
        return c + str;
    }

    public static void a(boolean z) {
        i = z;
        b();
    }

    private static void b() {
        a = i || j;
        Log.d("ABTestSdk", "updateDebugSwitch sEnable: " + a + " sDebugMode：" + i + " sDebugProperty：" + j);
    }
}
