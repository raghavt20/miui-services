package com.xiaomi.abtest.c;

import com.xiaomi.abtest.EnumType;

/* loaded from: classes.dex */
/* synthetic */ class f {
    static final /* synthetic */ int[] a;

    static {
        int[] iArr = new int[EnumType.DiversionType.values().length];
        a = iArr;
        try {
            iArr[EnumType.DiversionType.BY_USERID.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            a[EnumType.DiversionType.BY_SESSIONID.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            a[EnumType.DiversionType.BY_RANDOM.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            a[EnumType.DiversionType.BY_USERID_DAY.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            a[EnumType.DiversionType.BY_USERID_WEEK.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            a[EnumType.DiversionType.BY_USERID_MONTH.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
    }
}
