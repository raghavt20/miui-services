package com.miui.server.turbosched;

import android.util.Slog;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/* loaded from: classes.dex */
public class TurboSchedUtil {
    private static final String TAG = "TurboSched";

    /* JADX INFO: Access modifiers changed from: protected */
    public static boolean checkFileAccess(String path) {
        File file = new File(path);
        return file.exists();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static boolean readBooleanValueFromFile(String path) {
        String value = readValueFromFile(path);
        return "1".equals(value);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static String readValueFromFile(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String value = reader.readLine();
            reader.close();
            return value;
        } catch (IOException e) {
            Slog.e(TAG, "read file " + path + " failed");
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static String writeValueToFile(String path, String value) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(value);
            writer.close();
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
