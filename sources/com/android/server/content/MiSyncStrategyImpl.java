package com.android.server.content;

import android.app.job.JobInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.content.MiSyncConstants;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class MiSyncStrategyImpl implements MiSyncStrategy {
    public static final int CLEVER_MJ_STRATEGY = 1;
    public static final int DEFAULT_STRATEGY = 1;
    public static final int OFFICIAL_STRATEGY = 0;
    private static final String TAG = "Sync";
    private static final int VERSION = 1;
    private static final String XML_ATTR_ACCOUNT_NAME = "account_name";
    private static final String XML_ATTR_STRATEGY = "strategy";
    private static final String XML_ATTR_UID = "uid";
    private static final String XML_ATTR_VERSION = "version";
    public static final String XML_FILE_NAME = "mi_strategy";
    public static final int XML_FILE_VERSION = 1;
    private static final String XML_TAG_ITEM = "sync_strategy_item";
    private String mAccountName;
    private int mUid;
    private int mStrategy = 1;
    private SparseArray<ISyncStrategy> mCache = new SparseArray<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface ISyncStrategy {
        void apply(SyncOperation syncOperation, Bundle bundle, JobInfo.Builder builder);

        boolean isAllowedToRun(SyncOperation syncOperation, Bundle bundle);
    }

    public MiSyncStrategyImpl(int uid, String accountName) {
        this.mUid = uid;
        this.mAccountName = accountName;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getAccountName() {
        return this.mAccountName;
    }

    public boolean setStrategy(int strategy) {
        if (strategy == 0 || strategy == 1) {
            this.mStrategy = strategy;
            return true;
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "Illegal strategy");
            return false;
        }
        return false;
    }

    public int getStrategy() {
        return this.mStrategy;
    }

    public void writeToXML(XmlSerializer out) throws IOException {
        out.startTag(null, XML_TAG_ITEM);
        out.attribute(null, "version", Integer.toString(1));
        out.attribute(null, "uid", Integer.toString(this.mUid));
        out.attribute(null, XML_ATTR_ACCOUNT_NAME, this.mAccountName);
        out.attribute(null, XML_ATTR_STRATEGY, Integer.toString(this.mStrategy));
        out.endTag(null, XML_TAG_ITEM);
    }

    public static MiSyncStrategyImpl readFromXML(XmlPullParser parser) {
        String tagName = parser.getName();
        if (!XML_TAG_ITEM.equals(tagName)) {
            return null;
        }
        String itemVersionString = parser.getAttributeValue(null, "version");
        if (TextUtils.isEmpty(itemVersionString)) {
            Slog.e(TAG, "the version in mi strategy is null");
            return null;
        }
        try {
            int itemVersion = Integer.parseInt(itemVersionString);
            if (itemVersion < 1) {
                return null;
            }
            String uidString = parser.getAttributeValue(null, "uid");
            String accountName = parser.getAttributeValue(null, XML_ATTR_ACCOUNT_NAME);
            String strategyString = parser.getAttributeValue(null, XML_ATTR_STRATEGY);
            if (TextUtils.isEmpty(uidString) || TextUtils.isEmpty(accountName) || TextUtils.isEmpty(strategyString)) {
                return null;
            }
            try {
                int uid = Integer.parseInt(uidString);
                int strategy = Integer.parseInt(strategyString);
                MiSyncStrategyImpl miSyncStrategy = new MiSyncStrategyImpl(uid, accountName);
                miSyncStrategy.setStrategy(strategy);
                return miSyncStrategy;
            } catch (NumberFormatException e) {
                Slog.e(TAG, "error parsing item for mi strategy", e);
                return null;
            }
        } catch (NumberFormatException e2) {
            Slog.e(TAG, "error parsing version for mi strategy", e2);
            return null;
        }
    }

    public void apply(SyncOperation syncOperation, Bundle bundle, JobInfo.Builder builder) {
        getSyncStrategyInternal(this.mStrategy).apply(syncOperation, bundle, builder);
    }

    public boolean isAllowedToRun(SyncOperation syncOperation, Bundle bundle) {
        return getSyncStrategyInternal(this.mStrategy).isAllowedToRun(syncOperation, bundle);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v2 */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v4 */
    /* JADX WARN: Type inference failed for: r1v5 */
    /* JADX WARN: Type inference failed for: r2v1, types: [com.android.server.content.MiSyncStrategyImpl$CleverMJStrategy] */
    /* JADX WARN: Type inference failed for: r2v2, types: [com.android.server.content.MiSyncStrategyImpl$CleverMJStrategy] */
    private ISyncStrategy getSyncStrategyInternal(int i) {
        OfficialStrategy officialStrategy;
        if (this.mCache == null) {
            this.mCache = new SparseArray<>();
        }
        ISyncStrategy iSyncStrategy = this.mCache.get(i);
        if (iSyncStrategy != null) {
            return iSyncStrategy;
        }
        ?? r1 = 0;
        ?? r12 = 0;
        switch (i) {
            case 0:
                officialStrategy = new OfficialStrategy();
                break;
            case 1:
                officialStrategy = new CleverMJStrategy();
                break;
            default:
                officialStrategy = new CleverMJStrategy();
                break;
        }
        this.mCache.put(i, officialStrategy);
        return officialStrategy;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class OfficialStrategy implements ISyncStrategy {
        private OfficialStrategy() {
        }

        @Override // com.android.server.content.MiSyncStrategyImpl.ISyncStrategy
        public void apply(SyncOperation syncOperation, Bundle bundle, JobInfo.Builder builder) {
        }

        @Override // com.android.server.content.MiSyncStrategyImpl.ISyncStrategy
        public boolean isAllowedToRun(SyncOperation syncOperation, Bundle bundle) {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CleverMJStrategy implements ISyncStrategy {
        private static final int ALLOW_FIRST_SYNC_THRESHOLD = 3;
        private static final int ALLOW_FIRST_SYNC_THRESHOLD_FOR_BROWSER = 8;
        private static final String AUTHORITY_BROWSER = "com.miui.browser";
        private static final String AUTHORITY_CALENDAR = "com.android.calendar";
        private static final String AUTHORITY_CONTACTS = "com.android.contacts";
        private static final String AUTHORITY_GALLERY = "com.miui.gallery.cloud.provider";
        private static final String AUTHORITY_NOTES = "notes";
        private static final Set<String> REAL_TIME_STRATEGY_AUTHORITY_SET;

        private CleverMJStrategy() {
        }

        static {
            HashSet hashSet = new HashSet();
            REAL_TIME_STRATEGY_AUTHORITY_SET = hashSet;
            hashSet.add(AUTHORITY_CALENDAR);
            hashSet.add(AUTHORITY_NOTES);
            hashSet.add(AUTHORITY_CONTACTS);
            hashSet.add(AUTHORITY_GALLERY);
        }

        @Override // com.android.server.content.MiSyncStrategyImpl.ISyncStrategy
        public void apply(SyncOperation syncOperation, Bundle bundle, JobInfo.Builder builder) {
            if (syncOperation == null || syncOperation.target == null) {
                if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                    Log.d(MiSyncStrategyImpl.TAG, "injector: apply: null parameter, return");
                    return;
                }
                return;
            }
            String authority = syncOperation.target.provider;
            if (TextUtils.isEmpty(authority)) {
                if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                    Log.d(MiSyncStrategyImpl.TAG, "injector: apply: null parameter, return");
                }
            } else if (REAL_TIME_STRATEGY_AUTHORITY_SET.contains(authority)) {
                if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                    Log.d(MiSyncStrategyImpl.TAG, "injector: apply: authority is not affected by strategy, return");
                }
            } else {
                if (isFirstTimes(authority, bundle)) {
                    if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                        Log.d(MiSyncStrategyImpl.TAG, "injector: apply: first full sync, return");
                        return;
                    }
                    return;
                }
                builder.setRequiresCharging(true);
            }
        }

        @Override // com.android.server.content.MiSyncStrategyImpl.ISyncStrategy
        public boolean isAllowedToRun(SyncOperation syncOperation, Bundle bundle) {
            if (syncOperation != null && syncOperation.target != null) {
                String authority = syncOperation.target.provider;
                if (TextUtils.isEmpty(authority)) {
                    if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                        Log.d(MiSyncStrategyImpl.TAG, "injector: isAllowedToRun: null parameter, return true");
                    }
                    return true;
                }
                if (REAL_TIME_STRATEGY_AUTHORITY_SET.contains(authority)) {
                    if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                        Log.d(MiSyncStrategyImpl.TAG, "injector: isAllowedToRun: authority is not affected by strategy, return true");
                    }
                    return true;
                }
                if (isFirstTimes(authority, bundle)) {
                    if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                        Log.d(MiSyncStrategyImpl.TAG, "injector: isAllowedToRun: first full sync, return true");
                    }
                    return true;
                }
                long currentTimeMills = System.currentTimeMillis();
                boolean isInteractive = bundle.getBoolean(MiSyncConstants.Strategy.EXTRA_KEY_INTERACTIVE);
                long lastScreenOffTime = bundle.getLong(MiSyncConstants.Strategy.EXTRA_KEY_LAST_SCREEN_OFF_TIME);
                boolean isBatteryCharging = bundle.getBoolean(MiSyncConstants.Strategy.EXTRA_KEY_BATTERY_CHARGING);
                if (isBatteryCharging && !isInteractive && currentTimeMills - lastScreenOffTime > 120000) {
                    if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                        Log.d(MiSyncStrategyImpl.TAG, "injector: isAllowedToRun: condition is satisfied, return true");
                    }
                    return true;
                }
                return false;
            }
            if (Log.isLoggable(MiSyncStrategyImpl.TAG, 3)) {
                Log.d(MiSyncStrategyImpl.TAG, "injector: isAllowedToRun: null parameter, return true");
            }
            return true;
        }

        private boolean isFirstTimes(String authority, Bundle bundle) {
            int num = bundle.getInt(MiSyncConstants.Strategy.EXTRA_KEY_NUM_SYNCS, 0);
            if (AUTHORITY_BROWSER.equals(authority)) {
                if (num >= 0 && num < 8) {
                    return true;
                }
            } else if (num >= 0 && num < 3) {
                return true;
            }
            return false;
        }
    }
}
