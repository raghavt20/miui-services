package com.android.server.location.gnss.exp;

import android.location.LocationRequest;
import android.location.util.identity.CallerIdentity;

/* loaded from: classes.dex */
public class GnssRequestBean {
    Object callbackType;
    CallerIdentity identity;
    boolean isForegroundService;
    LocationRequest locationRequest;
    int permissionLevel;
    String provider;
    boolean removeByOpt;
}
