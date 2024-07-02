package com.xiaomi.abtest.d;

/* loaded from: classes.dex */
public class h {
    public static final int[] a = {101, 131, 1313, 13131, 131313, 1313131, 13131313, 31, 131313133, 13131315, 7, 17, 171, 137, 10243, 13};

    public static int a(byte[] bArr, int i, int i2) {
        int i3 = i & (-4);
        int i4 = 0;
        for (int i5 = 0; i5 < i3; i5 += 4) {
            int i6 = ((bArr[i5] & 255) | ((bArr[i5 + 1] & 255) << 8) | ((bArr[i5 + 2] & 255) << 16) | (bArr[i5 + 3] << 24)) * (-862048943);
            int i7 = i2 ^ (((i6 << 15) | (i6 >>> 17)) * 461845907);
            i2 = (((i7 >>> 19) | (i7 << 13)) * 5) - 430675100;
        }
        switch (i & 3) {
            case 3:
                i4 = (bArr[i3 + 2] & 255) << 16;
            case 2:
                i4 |= (bArr[i3 + 1] & 255) << 8;
            case 1:
                int i8 = ((bArr[i3] & 255) | i4) * (-862048943);
                i2 ^= ((i8 >>> 17) | (i8 << 15)) * 461845907;
                break;
        }
        int i9 = i2 ^ i;
        int i10 = (i9 ^ (i9 >>> 16)) * (-2048144789);
        int i11 = (i10 ^ (i10 >>> 13)) * (-1028477387);
        return (i11 ^ (i11 >>> 16)) & Integer.MAX_VALUE;
    }
}
