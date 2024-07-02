package com.miui.server.multisence;

import android.os.Trace;
import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/* loaded from: classes.dex */
public class MultiSenceServiceUtils {
    private static final String TAG = "MultiSenceService";

    public static void msLogD(boolean isDebug, String log) {
        if (!isDebug) {
            return;
        }
        Slog.d(TAG, log);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void msLogW(boolean isDebug, String log) {
        if (!isDebug) {
            return;
        }
        Slog.w(TAG, log);
    }

    static void msTraceBegin(String tips) {
        if (!MultiSenceConfig.DEBUG_TRACE) {
            return;
        }
        Trace.traceBegin(32L, tips);
    }

    static void msTraceEnd() {
        if (!MultiSenceConfig.DEBUG_TRACE) {
            return;
        }
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean writeToFile(String filePath, String value) {
        StringBuilder sb;
        FileOutputStream fos = null;
        boolean success = true;
        try {
            try {
                File file = new File(filePath);
                fos = new FileOutputStream(file);
                fos.write(value.getBytes());
            } catch (Exception e) {
                success = false;
                Slog.e(TAG, e.toString());
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e2) {
                        e = e2;
                        sb = new StringBuilder();
                        Slog.e(TAG, sb.append("optSched IOexception : ").append(e.toString()).toString());
                        return success;
                    }
                }
            }
            try {
                fos.close();
            } catch (IOException e3) {
                e = e3;
                sb = new StringBuilder();
                Slog.e(TAG, sb.append("optSched IOexception : ").append(e.toString()).toString());
                return success;
            }
            return success;
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "optSched IOexception : " + e4.toString());
                }
            }
            throw th;
        }
    }

    static String readFromFile(String filePath) {
        StringBuilder sb;
        FileInputStream fis = null;
        StringBuilder ret = new StringBuilder();
        try {
            try {
                File file = new File(filePath);
                fis = new FileInputStream(file);
                while (true) {
                    int readData = fis.read();
                    if (readData != -1) {
                        ret.append((char) readData);
                    } else {
                        try {
                            break;
                        } catch (IOException e) {
                            e = e;
                            sb = new StringBuilder();
                            Slog.e(TAG, sb.append("optSched IOexception : ").append(e.toString()).toString());
                            return ret.toString();
                        }
                    }
                }
                fis.close();
            } catch (Throwable th) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "optSched IOexception : " + e2.toString());
                    }
                }
                throw th;
            }
        } catch (Exception e3) {
            Slog.e(TAG, e3.toString());
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e4) {
                    e = e4;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("optSched IOexception : ").append(e.toString()).toString());
                    return ret.toString();
                }
            }
        }
        return ret.toString();
    }
}
