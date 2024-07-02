package com.android.server;

import android.os.StrictMode;
import android.util.Slog;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public final class PsiParser {
    private static final String PSI_ROOT = "/proc/pressure";
    private static final String TAG = PsiParser.class.getSimpleName();
    private static final List<String> PSI_FILES = Arrays.asList("/proc/pressure/memory", "/proc/pressure/cpu", "/proc/pressure/io");

    /* loaded from: classes.dex */
    public enum ResourceType {
        MEM,
        CPU,
        IO
    }

    public static Psi currentParsedPsiState(ResourceType type) {
        String raw = currentRawPsiString(type);
        Psi psi = Psi.parseFromRawString(raw);
        return psi;
    }

    public static String currentRawPsiString(ResourceType type) {
        StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        String filepath = PSI_FILES.get(type.ordinal());
        try {
            try {
                if (new File(filepath).exists()) {
                    return IoUtils.readFileAsString(filepath);
                }
            } catch (IOException e) {
                Slog.e(TAG, "Could not read " + filepath, e);
            }
            return "";
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    /* loaded from: classes.dex */
    public static final class Prop {
        public final float avg10;
        public final float avg300;
        public final float avg60;
        public final long total;

        private Prop(float avg10, float avg60, float avg300, long total) {
            this.avg10 = avg10;
            this.avg60 = avg60;
            this.avg300 = avg300;
            this.total = total;
        }

        public static Prop parseFromLine(String line) {
            String[] arr = line.split(" ");
            float avg10 = Float.parseFloat(arr[1].split("=")[1]);
            float avg60 = Float.parseFloat(arr[2].split("=")[1]);
            float avg300 = Float.parseFloat(arr[3].split("=")[1]);
            long total = Long.parseLong(arr[4].split("=")[1]);
            return new Prop(avg10, avg60, avg300, total);
        }
    }

    /* loaded from: classes.dex */
    public static final class Psi {
        public final Prop full;
        public final Prop some;

        private Psi(Prop some, Prop full) {
            this.some = some;
            this.full = full;
        }

        public static Psi parseFromRawString(String raw) {
            String[] lines = raw.split("\\R");
            Prop some = Prop.parseFromLine(lines[0]);
            Prop full = Prop.parseFromLine(lines[1]);
            return new Psi(some, full);
        }
    }
}
