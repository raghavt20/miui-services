package com.android.server.cloud;

import com.google.gson.Gson;

/* loaded from: classes.dex */
public class JsonUtil {
    private static Gson sGson;

    private JsonUtil() {
    }

    public static synchronized Gson getInstance() {
        Gson gson;
        synchronized (JsonUtil.class) {
            if (sGson == null) {
                sGson = new Gson();
            }
            gson = sGson;
        }
        return gson;
    }

    public static String toJson(Object object) {
        return getInstance().toJson(object);
    }

    public static <T> T fromJson(String str, Class<T> cls) {
        try {
            return (T) getInstance().fromJson(str, (Class) cls);
        } catch (Exception e) {
            return null;
        }
    }
}
