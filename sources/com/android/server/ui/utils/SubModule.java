package com.android.server.ui.utils;

import java.util.ArrayList;

/* loaded from: classes.dex */
public class SubModule {
    public static int ID_BASE = 0;
    public static final int ID_OD;
    public static final String MODULE_VERSION = "1.0";
    public static final ArrayList<String> SUB_MODULE_LIST;

    static {
        ID_BASE = -1;
        int i = (-1) + 1;
        ID_BASE = i;
        ID_OD = i;
        ArrayList<String> arrayList = new ArrayList<>();
        SUB_MODULE_LIST = arrayList;
        arrayList.add(i, "od");
    }
}
