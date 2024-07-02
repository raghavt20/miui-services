package com.android.server;

import android.os.FileObserver;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public abstract class FixedFileObserver {
    private static final HashMap<File, Set<FixedFileObserver>> sObserverLists = new HashMap<>();
    private final int mMask;
    private FileObserver mObserver;
    private final File mRootPath;

    public abstract void onEvent(int i, String str);

    public FixedFileObserver(String path) {
        this(path, 4095);
    }

    public FixedFileObserver(String path, int mask) {
        this.mRootPath = new File(path);
        this.mMask = mask;
    }

    public void startWatching() {
        HashMap<File, Set<FixedFileObserver>> hashMap = sObserverLists;
        synchronized (hashMap) {
            if (!hashMap.containsKey(this.mRootPath)) {
                hashMap.put(this.mRootPath, new HashSet());
            }
            final Set<FixedFileObserver> fixedObservers = hashMap.get(this.mRootPath);
            FileObserver fileObserver = fixedObservers.size() > 0 ? fixedObservers.iterator().next().mObserver : new FileObserver(this.mRootPath.getPath()) { // from class: com.android.server.FixedFileObserver.1
                @Override // android.os.FileObserver
                public void onEvent(int event, String path) {
                    for (FixedFileObserver fixedObserver : fixedObservers) {
                        if ((fixedObserver.mMask & event) != 0) {
                            fixedObserver.onEvent(event, path);
                        }
                    }
                }
            };
            this.mObserver = fileObserver;
            fileObserver.startWatching();
            fixedObservers.add(this);
        }
    }

    public void stopWatching() {
        HashMap<File, Set<FixedFileObserver>> hashMap = sObserverLists;
        synchronized (hashMap) {
            Set<FixedFileObserver> fixedObservers = hashMap.get(this.mRootPath);
            if (fixedObservers != null && this.mObserver != null) {
                fixedObservers.remove(this);
                if (fixedObservers.size() == 0) {
                    this.mObserver.stopWatching();
                }
                this.mObserver = null;
            }
        }
    }

    protected void finalize() {
        stopWatching();
    }
}
