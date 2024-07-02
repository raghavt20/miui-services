package com.android.server.location.gnss.operators;

import android.os.SystemProperties;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.Arrays;

/* loaded from: classes.dex */
public class GnssForCommonOperatorImpl implements GnssForOperatorCommonStub {
    private static final String CUSTOM_MCC_MNC = "25020";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssForCommonOperatorImpl> {

        /* compiled from: GnssForCommonOperatorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssForCommonOperatorImpl INSTANCE = new GnssForCommonOperatorImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssForCommonOperatorImpl m1856provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssForCommonOperatorImpl m1855provideNewInstance() {
            return new GnssForCommonOperatorImpl();
        }
    }

    public boolean isNiWhiteListOperator() {
        String mccmnc = SystemProperties.get("persist.sys.mcc.mnc", "");
        ArrayList<String> list = new ArrayList<>(Arrays.asList(CUSTOM_MCC_MNC));
        return list.contains(mccmnc);
    }
}
