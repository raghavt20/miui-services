package com.xiaomi.abtest.a;

import com.xiaomi.abtest.EnumType;

/* loaded from: classes.dex */
/* synthetic */ class f {
    static final /* synthetic */ int[] a;

    static {
        int[] iArr = new int[EnumType.FlowUnitType.values().length];
        a = iArr;
        try {
            iArr[EnumType.FlowUnitType.TYPE_DOMAIN.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            a[EnumType.FlowUnitType.TYPE_LAYER.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            a[EnumType.FlowUnitType.TYPE_UNKNOWN.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            a[EnumType.FlowUnitType.TYPE_EXPERIMENT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            a[EnumType.FlowUnitType.TYPE_EXP_CONTAINER.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
    }
}
