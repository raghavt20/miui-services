package com.android.server.pm;

import android.content.Intent;
import android.os.Bundle;
import java.util.List;

/* loaded from: classes.dex */
public interface PackageEventRecorderInternal {
    void commitDeletedEvents(int i, List<String> list);

    boolean deleteAllEventRecords(int i);

    Bundle getPackageEventRecords(int i);

    Bundle getSourcePackage(String str, int i, boolean z);

    void recordPackageActivate(String str, int i, String str2);

    void recordPackageFirstLaunch(int i, String str, String str2, Intent intent);

    void recordPackageRemove(int[] iArr, String str, String str2, boolean z, Bundle bundle);

    void recordPackageUpdate(int[] iArr, String str, String str2, Bundle bundle);
}
