package com.miui.server.migard;

import com.android.server.am.IGameProcessAction;

/* loaded from: classes.dex */
public abstract class MiGardInternal {
    public abstract void addGameProcessCompactor(IGameProcessAction.IGameProcessActionConfig iGameProcessActionConfig);

    public abstract void addGameProcessKiller(IGameProcessAction.IGameProcessActionConfig iGameProcessActionConfig);

    public abstract void notifyGameBackground();

    public abstract void notifyGameForeground(String str);

    public abstract void notifyProcessDied(int i);

    public abstract void onProcessKilled(int i, int i2, String str, String str2);

    public abstract void onProcessStart(int i, int i2, String str, String str2);

    public abstract void onVpnConnected(String str, boolean z);

    public abstract void reclaimBackgroundForGame(long j);
}
