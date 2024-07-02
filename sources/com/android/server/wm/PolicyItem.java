package com.android.server.wm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class PolicyItem implements Serializable {
    private static final long serialVersionUID = 202006110800L;
    private final List<PackageConfiguration> mList = new ArrayList();
    private int mScpmVersion = 0;
    private String mCurrentVersion = "";
    private int mLocalVersion = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PolicyItem(Set<String> policySet) {
        policySet.forEach(new Consumer() { // from class: com.android.server.wm.PolicyItem$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PolicyItem.this.lambda$new$0((String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(String str) {
        PackageConfiguration pkgConfig = new PackageConfiguration(str);
        this.mList.add(pkgConfig);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<PackageConfiguration> getPackageConfigurationList() {
        return this.mList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getScpmVersion() {
        return this.mScpmVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setScpmVersion(int scpmVersion) {
        this.mScpmVersion = scpmVersion;
    }

    String getCurrentVersion() {
        return this.mCurrentVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurrentVersion(String currentVersion) {
        this.mCurrentVersion = currentVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLocalVersion() {
        return this.mLocalVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLocalVersion(int localVersion) {
        this.mLocalVersion = localVersion;
    }

    boolean isMismatch(Set<String> policySet) {
        if (this.mList.isEmpty() || this.mList.size() != policySet.size()) {
            return true;
        }
        Iterator<PackageConfiguration> iterator = this.mList.iterator();
        while (iterator.hasNext()) {
            if (!policySet.contains(iterator.next().mName)) {
                return true;
            }
        }
        return false;
    }
}
