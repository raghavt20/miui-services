package com.xiaomi.abtest.d;

import android.text.TextUtils;
import com.android.server.input.padkeyboard.iic.CommunicationUtil;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* loaded from: classes.dex */
public class b {
    private static final String a = "DigestUtil";
    private static final char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final char[] c = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    static MessageDigest a(String str) {
        try {
            return MessageDigest.getInstance(str);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static MessageDigest a() {
        return a("MD5");
    }

    public static byte[] a(byte[] bArr) {
        return a().digest(bArr);
    }

    public static byte[] b(String str) {
        return a(a(str, "UTF-8"));
    }

    public static String b(byte[] bArr) {
        return a(a(bArr), true);
    }

    public static String c(String str) {
        return a(b(str), true);
    }

    private static MessageDigest b() {
        return a("SHA-256");
    }

    private static MessageDigest c() {
        return a("SHA1");
    }

    public static String d(String str) {
        return a(g(str), true);
    }

    public static String e(String str) {
        return a(f(str), true);
    }

    public static byte[] f(String str) {
        return c(a(str, "UTF-8"));
    }

    public static byte[] c(byte[] bArr) {
        return b().digest(bArr);
    }

    public static byte[] g(String str) {
        return c().digest(a(str, "UTF-8"));
    }

    public static String d(byte[] bArr) {
        return a(c(bArr), true);
    }

    public static String a(byte[] bArr, boolean z) {
        return new String(a(bArr, z ? b : c));
    }

    private static char[] a(byte[] bArr, char[] cArr) {
        char[] cArr2 = new char[bArr.length << 1];
        int i = 0;
        for (byte b2 : bArr) {
            int i2 = i + 1;
            cArr2[i] = cArr[(b2 & CommunicationUtil.COMMAND_KEYBOARD_STATUS) >>> 4];
            i = i2 + 1;
            cArr2[i2] = cArr[b2 & 15];
        }
        return cArr2;
    }

    private static byte[] a(String str, String str2) {
        if (str == null) {
            return null;
        }
        try {
            return str.getBytes(str2);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String e(byte[] bArr) {
        String format;
        if (bArr != null) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(bArr);
                format = String.format("%1$032X", new BigInteger(1, messageDigest.digest()));
            } catch (Exception e) {
                k.b(a, "getMD5 exception: " + e);
            }
            return format.toLowerCase();
        }
        format = "";
        return format.toLowerCase();
    }

    public static String h(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return e(str.getBytes());
    }
}
