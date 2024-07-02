package com.android.server.pm;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.os.RemoteException;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/* loaded from: classes.dex */
public abstract class MiuiPreinstallConfig {
    private static final String TAG = MiuiPreinstallConfig.class.getSimpleName();

    protected abstract List<File> getCustAppList();

    protected abstract List<String> getLegacyPreinstallList(boolean z, boolean z2);

    protected abstract List<File> getPreinstallDirs();

    protected abstract List<File> getVanwardAppList();

    protected abstract boolean needIgnore(String str, String str2);

    protected abstract boolean needLegacyPreinstall(String str, String str2);

    /* JADX INFO: Access modifiers changed from: protected */
    public void readLineToSet(File file, Set<String> set) {
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                try {
                    if (!file.getName().equals("vanward_applist")) {
                        while (true) {
                            String line = bufferedReader.readLine();
                            if (line == null) {
                                break;
                            } else {
                                set.add(line.trim());
                            }
                        }
                    } else {
                        IActivityManager am = ActivityManagerNative.getDefault();
                        Locale curLocale = am.getConfiguration().locale;
                        while (true) {
                            String line2 = bufferedReader.readLine();
                            if (line2 == null) {
                                break;
                            }
                            String[] ss = line2.trim().split("\\s+");
                            if (ss.length == 2 && isValidIme(ss[1], curLocale)) {
                                set.add(ss[0]);
                            }
                        }
                    }
                    bufferedReader.close();
                } catch (Throwable th) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (RemoteException | IOException e) {
                Slog.d(TAG, "readLineToSet: " + e);
            }
        }
    }

    private static boolean isValidIme(String locale, Locale curLocale) {
        String[] locales = locale.split(",");
        for (int i = 0; i < locales.length; i++) {
            if (locales[i].startsWith(curLocale.toString()) || locales[i].equals("*") || locales[i].startsWith(curLocale.getLanguage() + "_*")) {
                return true;
            }
        }
        return false;
    }
}
