package com.android.server.location;

/* loaded from: classes.dex */
public class Order {
    private static String flag;
    private static OnChangeListener onChangeListener;

    /* loaded from: classes.dex */
    public interface OnChangeListener {
        void onChange();
    }

    public static void setOnChangeListener(OnChangeListener onChange) {
        onChangeListener = onChange;
    }

    public static String getFlag() {
        return flag;
    }

    public static void setFlag(String flag2) {
        flag = flag2;
        onChangeListener.onChange();
    }
}
