package com.android.server.usage;

import android.app.usage.UsageEvents;
import android.content.Context;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.utils.quota.Categorizer;
import com.android.server.utils.quota.Category;
import com.android.server.utils.quota.CountQuotaTracker;
import com.miui.base.MiuiStubRegistry;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class UsageStatsServiceImpl implements UsageStatsServiceStub {
    private static final int LIMIT_COUNT = 10000;
    private static final int QUOTA_CATEGORY_NUM = 38;
    private static final int SHRINK_THRESHOLD = 100000;
    private static final int WINDOW_MS = 86400000;
    private boolean hasInit = false;
    private CountQuotaTracker mQuotaTracker;
    private static final String TAG = UsageStatsServiceImpl.class.getSimpleName();
    private static final String[] QUOTA_CATEGORY_TAGS = new String[38];
    private static final Category[] QUOTA_CATEGORIES = new Category[38];
    private static final HashMap<String, Integer> QUOTA_CATEGORY_TAGS_MAP = new HashMap<>(38);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<UsageStatsServiceImpl> {

        /* compiled from: UsageStatsServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final UsageStatsServiceImpl INSTANCE = new UsageStatsServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public UsageStatsServiceImpl m2381provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public UsageStatsServiceImpl m2380provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.usage.UsageStatsServiceImpl is marked as singleton");
        }
    }

    public void init(Context context) {
        for (int i = 0; i < 38; i++) {
            String[] strArr = QUOTA_CATEGORY_TAGS;
            strArr[i] = "UsageEvent_Quota_Category_" + i;
            QUOTA_CATEGORIES[i] = new Category(strArr[i]);
            QUOTA_CATEGORY_TAGS_MAP.put(strArr[i], Integer.valueOf(i));
        }
        Categorizer quotaCategorizer = new Categorizer() { // from class: com.android.server.usage.UsageStatsServiceImpl$$ExternalSyntheticLambda0
            public final Category getCategory(int i2, String str, String str2) {
                return UsageStatsServiceImpl.lambda$init$0(i2, str, str2);
            }
        };
        this.mQuotaTracker = new CountQuotaTracker(context, quotaCategorizer);
        for (int i2 = 0; i2 < 38; i2++) {
            this.mQuotaTracker.setCountLimit(QUOTA_CATEGORIES[i2], 10000, 86400000L);
        }
        this.hasInit = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Category lambda$init$0(int userId, String packageName, String tag) {
        int catIdx = QUOTA_CATEGORY_TAGS_MAP.get(tag).intValue();
        return QUOTA_CATEGORIES[catIdx];
    }

    public boolean isEventInQuota(UsageEvents.Event event, int userId) {
        if (!this.hasInit) {
            Slog.w(TAG, "UsageStatsServiceImpl not init, so can't do isEventInQuota.", new Throwable());
            return true;
        }
        int et = event.mEventType;
        String pkg = event.mPackage;
        String tag = QUOTA_CATEGORY_TAGS[et];
        return this.mQuotaTracker.noteEvent(userId, pkg, tag);
    }

    public void dumpCountQuotaTracker(IndentingPrintWriter idpw) {
        if (!this.hasInit) {
            Slog.w(TAG, "UsageStatsServiceImpl not init, so can't dumpCountQuotaTracker.");
        } else {
            idpw.println();
            this.mQuotaTracker.dump(idpw);
        }
    }

    public boolean isLimitExceed(List<UsageEvents.Event> events) {
        if (events.size() < SHRINK_THRESHOLD) {
            return false;
        }
        Slog.w(TAG, "Events exceed 100000! Reserved event end at timeStamp: " + events.get(events.size() - 1).getTimeStamp() + " to avoid OOM.");
        return true;
    }
}
