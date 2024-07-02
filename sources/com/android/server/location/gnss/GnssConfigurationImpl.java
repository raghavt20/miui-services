package com.android.server.location.gnss;

import com.android.server.location.LocationDumpLogStub;
import com.miui.base.MiuiStubRegistry;
import java.util.Properties;

/* loaded from: classes.dex */
public class GnssConfigurationImpl implements GnssConfigurationStub {
    private static final String CONFIG_NFW_PROXY_APPS = "NFW_PROXY_APPS";
    private static final String CONFIG_SUPL_HOST = "SUPL_HOST";
    private static final String QX_SUPL_ADDRESS = "supl.qxwz.com";
    private static final String XTY_SUPL_ADDRESS = "supl.bd-caict.com";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssConfigurationImpl> {

        /* compiled from: GnssConfigurationImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssConfigurationImpl INSTANCE = new GnssConfigurationImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public GnssConfigurationImpl m1802provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public GnssConfigurationImpl m1801provideNewInstance() {
            return new GnssConfigurationImpl();
        }
    }

    public void loadPropertiesFromCarrierConfig(Properties properties) {
        if (GnssCollectDataStub.getInstance().isCnSimInserted()) {
            boolean CaictState = GnssCollectDataStub.getInstance().getSuplState();
            LocationDumpLogStub.getInstance().addToBugreport(2, "Caictstate is : " + CaictState);
            if (CaictState) {
                properties.setProperty(CONFIG_SUPL_HOST, XTY_SUPL_ADDRESS);
            } else {
                properties.setProperty(CONFIG_SUPL_HOST, QX_SUPL_ADDRESS);
            }
        }
        GnssLocationProviderStub.getInstance().setNfwProxyAppConfig(properties, CONFIG_NFW_PROXY_APPS);
    }
}
