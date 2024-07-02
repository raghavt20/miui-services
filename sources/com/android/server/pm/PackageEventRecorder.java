package com.android.server.pm;

import android.app.AppGlobals;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.pm.pkg.PackageStateInternal;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import miui.os.Build;

/* loaded from: classes.dex */
public class PackageEventRecorder implements PackageEventRecorderInternal {
    private static final char ATTR_DELIMITER = ' ';
    private static final String ATTR_EVENT_TIME_MILLIS = "eventTimeMillis";
    private static final String ATTR_ID = "id";
    private static final String ATTR_INSTALLER = "installerPackageName";
    private static final String ATTR_IS_REMOVED_FULLY = "isRemovedFully";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_USER_IDS = "userIds";
    private static final char ATTR_USER_IDS_DELIMITER = ',';
    private static final char ATTR_VALUE_DELIMITER = '=';
    private static final String BUNDLE_KEY_RECORDS = "packageEventRecords";
    private static final boolean DEBUG = false;
    private static final int ERROR_PENDING_ADDED_NUM = 5000;
    private static final int ERROR_PENDING_DELETED_NUM = 6000;
    private static final long MAX_DELAY_TIME_MILLIS = 60000;
    private static final long MAX_FILE_SIZE = 5242880;
    private static final int MAX_NUM_ACTIVATION_RECORDS = 50;
    private static final int MAX_NUM_RECORDS_READ_ONCE = 50;
    private static final int MESSAGE_DELETE_RECORDS = 2;
    private static final int MESSAGE_WRITE_FILE = 1;
    private static final long SCHEDULE_DELAY_TIME_MILLIS = 10000;
    private static final String TAG = "PackageEventRecorder";
    public static final int TYPE_FIRST_LAUNCH = 1;
    public static final int TYPE_REMOVE = 3;
    public static final int TYPE_UPDATE = 2;
    private static final List<Integer> VALID_TYPES;
    private static final int WARN_PENDING_ADDED_NUM = 2500;
    private static final int WARN_PENDING_DELETED_NUM = 3000;
    private final LinkedList<ActivationRecord> mActivationRecords;
    private final SparseArray<List<PackageEventBase>> mAllPendingAddedEvents;
    private final SparseArray<List<String>> mAllPendingDeletedEvents;
    boolean mCheckCalling;
    final Handler mHandler;
    private long mLastScheduledTimeMillis;
    private final SparseArray<File> mTypeToFile;

    static {
        ArrayList arrayList = new ArrayList();
        VALID_TYPES = arrayList;
        arrayList.add(1);
        arrayList.add(2);
        arrayList.add(3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class RecorderHandler extends Handler {
        private final PackageEventRecorder recorder;

        /* JADX INFO: Access modifiers changed from: package-private */
        public RecorderHandler(PackageEventRecorder recorder, Looper looper) {
            super(looper);
            this.recorder = recorder;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (this.recorder.getLock(((Integer) msg.obj).intValue())) {
                        this.recorder.writeAppendLocked(((Integer) msg.obj).intValue());
                    }
                    return;
                case 2:
                    synchronized (this.recorder.getLock(((Integer) msg.obj).intValue())) {
                        this.recorder.deleteEventRecordsLocked(((Integer) msg.obj).intValue());
                    }
                    return;
                default:
                    Slog.e(PackageEventRecorder.TAG, "unknown message " + msg);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageEventRecorder(File dir, Function<PackageEventRecorder, RecorderHandler> handlerSuppler, boolean checkCalling) {
        SparseArray<File> sparseArray = new SparseArray<>();
        this.mTypeToFile = sparseArray;
        this.mAllPendingAddedEvents = new SparseArray<>();
        this.mAllPendingDeletedEvents = new SparseArray<>();
        this.mLastScheduledTimeMillis = 0L;
        this.mActivationRecords = new LinkedList<>();
        if (!dir.exists()) {
            dir.mkdir();
        }
        sparseArray.put(1, new File(dir, "active-events.txt"));
        sparseArray.put(2, new File(dir, "update-events.txt"));
        sparseArray.put(3, new File(dir, "remove-events.txt"));
        Iterator<Integer> it = VALID_TYPES.iterator();
        while (it.hasNext()) {
            int type = it.next().intValue();
            this.mAllPendingAddedEvents.put(type, new ArrayList());
            this.mAllPendingDeletedEvents.put(type, new ArrayList());
        }
        this.mCheckCalling = checkCalling;
        this.mHandler = handlerSuppler.apply(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Object getLock(int type) {
        return this.mTypeToFile.get(type);
    }

    private File getWrittenFile(int type) {
        return this.mTypeToFile.get(type);
    }

    private File getBackupFile(int type) {
        return new File(getWrittenFile(type).getAbsolutePath() + ".backup");
    }

    private List<PackageEventBase> getPendingAddedEvents(int type) {
        return this.mAllPendingAddedEvents.get(type);
    }

    private List<String> getPendingDeletedEvents(int type) {
        return this.mAllPendingDeletedEvents.get(type);
    }

    private synchronized long getNextScheduledTime() {
        if (this.mLastScheduledTimeMillis <= SystemClock.uptimeMillis()) {
            this.mLastScheduledTimeMillis = SystemClock.uptimeMillis() + 10000;
        } else if (this.mLastScheduledTimeMillis - SystemClock.uptimeMillis() <= 50000) {
            this.mLastScheduledTimeMillis += 10000;
        } else {
            this.mLastScheduledTimeMillis = SystemClock.uptimeMillis() + 60000;
        }
        return this.mLastScheduledTimeMillis;
    }

    public static boolean isEnabled() {
        return !Build.IS_INTERNATIONAL_BUILD;
    }

    private File getWrittenFileLocked(int type) {
        File writtenFile = getWrittenFile(type);
        File backupFile = getBackupFile(type);
        try {
            if (writtenFile.exists()) {
                if (backupFile.exists()) {
                    if (!writtenFile.delete()) {
                        Slog.e(TAG, "fail to delete damaged file " + writtenFile.getAbsolutePath());
                        return null;
                    }
                } else if (!writtenFile.renameTo(backupFile)) {
                    Slog.e(TAG, "fail to rename " + writtenFile.getAbsolutePath() + " to " + backupFile.getAbsolutePath());
                    return null;
                }
                FileUtils.copy(backupFile, writtenFile);
            } else if (backupFile.exists()) {
                FileUtils.copy(backupFile, writtenFile);
            }
            return writtenFile;
        } catch (IOException e) {
            Slog.e(TAG, "error happened in getWrittenFileLocked", e);
            if (writtenFile.exists() && !writtenFile.delete()) {
                Slog.e(TAG, "fail to delete damaged file " + writtenFile.getAbsolutePath(), e);
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeAppendLocked(int type) {
        List<PackageEventBase> pendingAdded = getPendingAddedEvents(type);
        if (pendingAdded.isEmpty()) {
            return;
        }
        System.currentTimeMillis();
        File targetFile = getWrittenFileLocked(type);
        if (targetFile == null) {
            return;
        }
        if (targetFile.length() >= MAX_FILE_SIZE) {
            targetFile.delete();
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(targetFile, true));
            try {
                for (PackageEventBase packageEvent : pendingAdded) {
                    packageEvent.WriteToTxt(bufferedWriter);
                    bufferedWriter.newLine();
                }
                bufferedWriter.flush();
                bufferedWriter.close();
                pendingAdded.clear();
                File backupFile = getBackupFile(type);
                if (backupFile.exists() && !backupFile.delete()) {
                    Slog.e(TAG, "succeed to write to file " + targetFile.getAbsolutePath() + " ,but fail to delete backup file " + backupFile.getAbsolutePath());
                }
            } finally {
            }
        } catch (IOException e) {
            Slog.e(TAG, "fail to write append to " + targetFile.getAbsolutePath(), e);
            if (!targetFile.delete()) {
                Slog.e(TAG, "fail to delete witten file " + targetFile.getAbsolutePath());
            }
        }
    }

    private File getReadFileLocked(int type) {
        File writtenFile = getWrittenFile(type);
        File backupFile = getBackupFile(type);
        if (writtenFile.exists()) {
            if (backupFile.exists()) {
                if (!writtenFile.delete()) {
                    Slog.e(TAG, "fail to delete damaged file " + writtenFile.getAbsolutePath());
                    return null;
                }
                return backupFile;
            }
            return writtenFile;
        }
        if (backupFile.exists()) {
            return backupFile;
        }
        try {
            boolean success = writtenFile.createNewFile();
            if (success) {
                return writtenFile;
            }
            return null;
        } catch (IOException e) {
            Slog.e(TAG, "fail to create file " + writtenFile.getAbsolutePath(), e);
            return null;
        }
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public Bundle getPackageEventRecords(int type) {
        Bundle recordsLocked;
        if (!checkCallingPackage()) {
            return null;
        }
        synchronized (getLock(type)) {
            recordsLocked = getRecordsLocked(type);
        }
        return recordsLocked;
    }

    private Bundle getRecordsLocked(int type) {
        System.currentTimeMillis();
        if (!VALID_TYPES.contains(Integer.valueOf(type))) {
            Slog.e(TAG, "invalid package event type " + type);
            return null;
        }
        File targetFile = getReadFileLocked(type);
        if (targetFile == null) {
            return null;
        }
        Bundle result = new Bundle();
        List<Bundle> records = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(targetFile));
            int lineNum = 0;
            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if (line == null || records.size() > 50) {
                        break;
                    }
                    lineNum++;
                    Bundle oneRecord = PackageEventBase.buildBundleFromRecord(line, lineNum, type);
                    if (oneRecord != null) {
                        records.add(oneRecord);
                    }
                } finally {
                }
            }
            bufferedReader.close();
            for (int i = 0; records.size() + i < 50 && i < getPendingAddedEvents(type).size(); i++) {
                records.add(getPendingAddedEvents(type).get(i).buildBundle());
            }
            result.putParcelableList(BUNDLE_KEY_RECORDS, records);
            return result;
        } catch (IOException e) {
            Slog.e(TAG, "fail to resolve records from " + targetFile.getAbsolutePath(), e);
            return null;
        }
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public boolean deleteAllEventRecords(int type) {
        boolean z = false;
        if (!checkCallingPackage()) {
            return false;
        }
        synchronized (getLock(type)) {
            Slog.i(TAG, "deleting all package event records, type " + type);
            getPendingAddedEvents(type).clear();
            getPendingDeletedEvents(type).clear();
            getWrittenFile(type).delete();
            getBackupFile(type).delete();
            if (!getWrittenFile(type).exists() && !getBackupFile(type).exists()) {
                z = true;
            }
        }
        return z;
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public void commitDeletedEvents(int type, List<String> eventIds) {
        if (!checkCallingPackage()) {
            return;
        }
        if (eventIds.size() >= 6000) {
            Slog.e(TAG, "add too many deleted package events, abandon it");
            return;
        }
        synchronized (getLock(type)) {
            List<String> pendingDeleted = getPendingDeletedEvents(type);
            int total = pendingDeleted.size() + eventIds.size();
            if (total >= 6000) {
                Slog.e(TAG, "too many pending deleted package events in memory, clear it");
                pendingDeleted.clear();
            } else if (total >= WARN_PENDING_DELETED_NUM) {
                Slog.e(TAG, "too many pending deleted package events in memory, please try later");
            }
            this.mHandler.removeMessages(2, Integer.valueOf(type));
            pendingDeleted.addAll(eventIds);
        }
        Message message = this.mHandler.obtainMessage(2, Integer.valueOf(type));
        this.mHandler.sendMessageAtTime(message, getNextScheduledTime());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteEventRecordsLocked(int type) {
        List<String> pendingDeleted = getPendingDeletedEvents(type);
        if (pendingDeleted.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        File targetFile = getReadFileLocked(type);
        if (targetFile == null) {
            return;
        }
        ArrayList<String> reservedRecords = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(targetFile));
            int lineNum = 0;
            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    lineNum++;
                    String id = PackageEventBase.resolveIdFromRecord(line);
                    if (TextUtils.isEmpty(id)) {
                        printLogWhileResolveTxt(6, "fail to resolve attr id ", lineNum);
                    } else if (!pendingDeleted.contains(id)) {
                        reservedRecords.add(line);
                    }
                } finally {
                }
            }
            bufferedReader.close();
            File targetFile2 = getWrittenFileLocked(type);
            if (targetFile2 == null) {
                return;
            }
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(targetFile2));
                try {
                    Iterator<String> it = reservedRecords.iterator();
                    while (it.hasNext()) {
                        bufferedWriter.write(it.next());
                        bufferedWriter.newLine();
                    }
                    bufferedWriter.close();
                    File backupFile = getBackupFile(type);
                    if (!backupFile.delete()) {
                        Slog.e(TAG, "succeed to write to file " + targetFile2.getAbsolutePath() + " ,but fail to delete backup file " + backupFile.getAbsolutePath());
                    }
                    Iterator<PackageEventBase> iterator = getPendingAddedEvents(type).iterator();
                    while (iterator.hasNext()) {
                        PackageEventBase packageEvent = iterator.next();
                        if (!pendingDeleted.contains(packageEvent.id)) {
                            break;
                        } else {
                            iterator.remove();
                        }
                    }
                    pendingDeleted.clear();
                    Slog.d(TAG, "cost " + (System.currentTimeMillis() - start) + "ms in deleteEventRecordsLocked");
                } finally {
                }
            } catch (IOException e) {
                Slog.e(TAG, "fail to write to " + targetFile2.getAbsolutePath(), e);
                if (!targetFile2.delete()) {
                    Slog.e(TAG, "fail to delete witten file " + targetFile2.getAbsolutePath());
                }
            }
        } catch (IOException e2) {
            Slog.e(TAG, "fail to read from " + targetFile.getAbsolutePath(), e2);
        }
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public void recordPackageFirstLaunch(int userId, String pkgName, String installer, Intent intent) {
        if (!isEnabled()) {
            return;
        }
        PackageFirstLaunchEvent event = new PackageFirstLaunchEvent(System.currentTimeMillis(), pkgName, new int[]{userId}, installer);
        if (!commitAddedEvents(event)) {
            return;
        }
        intent.putExtra("miuiActiveId", event.getId());
        intent.putExtra("miuiActiveTime", PackageEventBase.resolveEventTimeMillis(event.getId()));
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public void recordPackageUpdate(int[] userIds, String pkgName, String installer, Bundle extras) {
        if (!isEnabled()) {
            return;
        }
        PackageUpdateEvent event = new PackageUpdateEvent(System.currentTimeMillis(), pkgName, userIds, installer);
        if (!commitAddedEvents(event)) {
            return;
        }
        extras.putString("miuiUpdateId", event.getId());
        extras.putLong("miuiUpdateTime", PackageEventBase.resolveEventTimeMillis(event.getId()));
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public void recordPackageRemove(int[] userIds, String pkgName, String installer, boolean isRemovedFully, Bundle extras) {
        long eventTimeMillis;
        if (!isEnabled()) {
            return;
        }
        synchronized (PackageRemoveEvent.class) {
            try {
                eventTimeMillis = System.currentTimeMillis();
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        PackageRemoveEvent event = new PackageRemoveEvent(eventTimeMillis, pkgName, userIds, installer, isRemovedFully);
        if (commitAddedEvents(event)) {
            extras.putString("miuiRemoveId", event.getId());
            extras.putLong("miuiRemoveTime", eventTimeMillis);
        }
    }

    private boolean commitAddedEvents(PackageEventBase packageEvent) {
        if (!packageEvent.isValid()) {
            Slog.e(TAG, "invalid package event " + packageEvent + " ,reject to write to file");
            return false;
        }
        synchronized (getLock(packageEvent.getEventType())) {
            List<PackageEventBase> pendingAdded = getPendingAddedEvents(packageEvent.getEventType());
            if (pendingAdded.size() + 1 >= 5000) {
                Slog.e(TAG, "too many pending added package events in memory, clear it");
                pendingAdded.clear();
            } else if (pendingAdded.size() >= 2500) {
                Slog.e(TAG, "too many pending added package events in memory, please try later");
            }
            this.mHandler.removeMessages(1, Integer.valueOf(packageEvent.getEventType()));
            pendingAdded.add(packageEvent);
        }
        Message message = this.mHandler.obtainMessage(1, Integer.valueOf(packageEvent.getEventType()));
        this.mHandler.sendMessageAtTime(message, getNextScheduledTime());
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void printLogWhileResolveTxt(int priority, String message, int lineNum) {
        Log.println_native(3, priority, TAG, message + " at line " + lineNum);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void printLogWhileResolveTxt(int priority, String message, int lineNum, Throwable tr) {
        Log.println_native(3, priority, TAG, message + " at line " + lineNum + '\n' + Log.getStackTraceString(tr));
    }

    private boolean checkCallingPackage() {
        if (!this.mCheckCalling) {
            return true;
        }
        try {
            String[] pkgNames = AppGlobals.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            if (pkgNames == null) {
                return false;
            }
            for (String pkgName : pkgNames) {
                if (MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE.equals(pkgName)) {
                    return true;
                }
            }
            Slog.w(TAG, "open only to com.miui.analytics now");
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "fail to get package names for uid " + Binder.getCallingUid(), e);
            return false;
        }
    }

    /* loaded from: classes.dex */
    private static class PackageUpdateEvent extends PackageEventBase {
        private PackageUpdateEvent(long eventTime, String pkgName, int[] userIds, String installer) {
            super(buildPackageEventId(eventTime, 2), pkgName, userIds, installer);
        }
    }

    /* loaded from: classes.dex */
    private static class PackageFirstLaunchEvent extends PackageEventBase {
        private PackageFirstLaunchEvent(long eventTime, String pkgName, int[] userIds, String installer) {
            super(buildPackageEventId(eventTime, 1), pkgName, userIds, installer);
        }
    }

    /* loaded from: classes.dex */
    private static class PackageRemoveEvent extends PackageEventBase {
        private final boolean isRemovedFully;

        private PackageRemoveEvent(long eventTime, String pkgName, int[] userIds, String installer, boolean isRemovedFully) {
            super(buildPackageEventId(eventTime, 3), pkgName, userIds, installer);
            this.isRemovedFully = isRemovedFully;
        }

        @Override // com.android.server.pm.PackageEventRecorder.PackageEventBase
        void WriteToTxt(BufferedWriter bufferedWriter) throws IOException {
            super.WriteToTxt(bufferedWriter);
            bufferedWriter.write("isRemovedFully=" + this.isRemovedFully);
            bufferedWriter.write(32);
        }

        private Bundle buildBundle() {
            Bundle result = buildBundle();
            result.putBoolean(PackageEventRecorder.ATTR_IS_REMOVED_FULLY, this.isRemovedFully);
            return result;
        }

        @Override // com.android.server.pm.PackageEventRecorder.PackageEventBase
        public String toString() {
            return "PackageEvent{id='" + ((PackageEventBase) this).id + "', type='" + resolveEventType(((PackageEventBase) this).id) + "', packageName='" + ((PackageEventBase) this).packageName + "', userIds=" + Arrays.toString(((PackageEventBase) this).userIds) + ", installerPackageName='" + ((PackageEventBase) this).installer + "', isRemovedFully=" + this.isRemovedFully + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static abstract class PackageEventBase {
        private final String id;
        private final String installer;
        private final String packageName;
        private final int[] userIds;

        private PackageEventBase(String id, String packageName, int[] userIds, String installerPackageName) {
            this.id = id;
            this.packageName = packageName;
            this.userIds = userIds;
            this.installer = installerPackageName;
        }

        public boolean isValid() {
            return (TextUtils.isEmpty(this.id) || TextUtils.isEmpty(this.packageName) || !isUserIdsValid()) ? false : true;
        }

        private boolean isUserIdsValid() {
            int[] iArr;
            return !(getEventType() == 3 || (iArr = this.userIds) == null || iArr.length <= 0) || getEventType() == 3;
        }

        void WriteToTxt(BufferedWriter bufferedWriter) throws IOException {
            bufferedWriter.write(this.id);
            bufferedWriter.write(32);
            bufferedWriter.write("packageName=" + this.packageName);
            bufferedWriter.write(32);
            int[] iArr = this.userIds;
            if (iArr != null && iArr.length > 0) {
                bufferedWriter.write("userIds=");
                bufferedWriter.write(String.valueOf(this.userIds[0]));
                for (int i = 1; i < this.userIds.length; i++) {
                    bufferedWriter.write(PackageEventRecorder.ATTR_USER_IDS_DELIMITER + String.valueOf(this.userIds[i]));
                }
                bufferedWriter.write(32);
            }
            if (!TextUtils.isEmpty(this.installer)) {
                bufferedWriter.write("installerPackageName=" + this.installer);
                bufferedWriter.write(32);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int getEventType() {
            return resolveEventType(this.id);
        }

        static String buildPackageEventId(long eventTime, int type) {
            return eventTime + String.valueOf(type);
        }

        static long resolveEventTimeMillis(String id) {
            return Long.parseLong(id.substring(0, id.length() - 1));
        }

        static int resolveEventType(String id) {
            return Integer.parseInt(id.substring(id.length() - 1));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String resolveIdFromRecord(String record) {
            if (!TextUtils.isEmpty(record) && record.contains(String.valueOf(PackageEventRecorder.ATTR_DELIMITER))) {
                return record.substring(0, record.indexOf(32));
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static Bundle buildBundleFromRecord(String record, int lineNum, int desireType) {
            boolean z;
            Bundle result = new Bundle();
            try {
                String id = resolveIdFromRecord(record);
                if (TextUtils.isEmpty(id)) {
                    PackageEventRecorder.printLogWhileResolveTxt(6, "fail to resolve attr id", lineNum);
                    return null;
                }
                result.putString(PackageEventRecorder.ATTR_ID, id);
                result.putLong(PackageEventRecorder.ATTR_EVENT_TIME_MILLIS, resolveEventTimeMillis(id));
                int actualType = resolveEventType(id);
                if (actualType != desireType) {
                    return null;
                }
                try {
                    ArrayMap<String, String> attrs = new ArrayMap<>();
                    String[] split = record.substring(record.indexOf(32) + 1).split(String.valueOf(PackageEventRecorder.ATTR_DELIMITER));
                    int length = split.length;
                    int i = 0;
                    while (i < length) {
                        String attr = split[i];
                        String[] pv = attr.split(String.valueOf(PackageEventRecorder.ATTR_VALUE_DELIMITER));
                        String id2 = id;
                        if (pv.length != 2) {
                            PackageEventRecorder.printLogWhileResolveTxt(6, "bad attr format :" + attr, lineNum);
                            z = true;
                        } else {
                            z = true;
                            attrs.put(pv[0], pv[1]);
                        }
                        i++;
                        id = id2;
                    }
                    if (TextUtils.isEmpty(attrs.get("packageName"))) {
                        PackageEventRecorder.printLogWhileResolveTxt(6, "attr packageName is missing or invalid", lineNum);
                        return null;
                    }
                    result.putString("packageName", attrs.get("packageName"));
                    if (!TextUtils.isEmpty(attrs.get(PackageEventRecorder.ATTR_USER_IDS))) {
                        int[] userIds = Arrays.stream(attrs.get(PackageEventRecorder.ATTR_USER_IDS).split(String.valueOf(PackageEventRecorder.ATTR_USER_IDS_DELIMITER))).mapToInt(new ToIntFunction() { // from class: com.android.server.pm.PackageEventRecorder$PackageEventBase$$ExternalSyntheticLambda0
                            @Override // java.util.function.ToIntFunction
                            public final int applyAsInt(Object obj) {
                                return Integer.parseInt((String) obj);
                            }
                        }).toArray();
                        if (userIds != null && userIds.length != 0) {
                            result.putIntArray(PackageEventRecorder.ATTR_USER_IDS, userIds);
                        }
                        PackageEventRecorder.printLogWhileResolveTxt(6, "fail to resolve attr userIds", lineNum);
                        return null;
                    }
                    if (!TextUtils.isEmpty(attrs.get(PackageEventRecorder.ATTR_INSTALLER))) {
                        result.putString(PackageEventRecorder.ATTR_INSTALLER, attrs.get(PackageEventRecorder.ATTR_INSTALLER));
                    }
                    if (actualType == 3) {
                        if (!attrs.containsKey(PackageEventRecorder.ATTR_IS_REMOVED_FULLY)) {
                            PackageEventRecorder.printLogWhileResolveTxt(6, "missing attr isRemovedFully", lineNum);
                            return null;
                        }
                        result.putBoolean(PackageEventRecorder.ATTR_IS_REMOVED_FULLY, Boolean.parseBoolean(attrs.get(PackageEventRecorder.ATTR_IS_REMOVED_FULLY)));
                    }
                    return result;
                } catch (Exception e) {
                    e = e;
                    PackageEventRecorder.printLogWhileResolveTxt(6, "fail to resolve record", lineNum, e);
                    return null;
                }
            } catch (Exception e2) {
                e = e2;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Bundle buildBundle() {
            Bundle result = new Bundle();
            result.putString(PackageEventRecorder.ATTR_ID, this.id);
            result.putLong(PackageEventRecorder.ATTR_EVENT_TIME_MILLIS, resolveEventTimeMillis(this.id));
            result.putString("packageName", this.packageName);
            int[] iArr = this.userIds;
            if (iArr != null && iArr.length > 0) {
                result.putIntArray(PackageEventRecorder.ATTR_USER_IDS, iArr);
            }
            if (!TextUtils.isEmpty(this.installer)) {
                result.putString(PackageEventRecorder.ATTR_INSTALLER, this.installer);
            }
            return result;
        }

        public String toString() {
            return "PackageEvent{id='" + this.id + "', type='" + resolveEventType(this.id) + "', packageName='" + this.packageName + "', userIds=" + Arrays.toString(this.userIds) + ", installerPackageName='" + this.installer + "'}";
        }

        public String getId() {
            return this.id;
        }
    }

    /* loaded from: classes.dex */
    private static class ActivationRecord {
        final String activatedPackage;
        final String sourcePackage;
        final int userId;

        private ActivationRecord(String activatedPackage, int userId, String sourcePackage) {
            this.activatedPackage = activatedPackage;
            this.userId = userId;
            this.sourcePackage = sourcePackage;
        }

        public String toString() {
            return "ActivationRecord{activatedPackage='" + this.activatedPackage + "', userId=" + this.userId + ", sourcePackage='" + this.sourcePackage + "'}";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldRecordPackageActivate(String activatedPackage, String sourcePackage, int userId, PackageStateInternal pkgState) {
        return isEnabled() && pkgState != null && pkgState.getUserStateOrDefault(userId).isNotLaunched() && pkgState.getUserStateOrDefault(userId).isStopped() && !TextUtils.isEmpty(activatedPackage) && !TextUtils.isEmpty(sourcePackage);
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public void recordPackageActivate(String activatedPackage, int userId, String sourcePackage) {
        if (this.mActivationRecords.size() >= 50) {
            this.mActivationRecords.removeFirst();
        }
        this.mActivationRecords.addLast(new ActivationRecord(activatedPackage, userId, sourcePackage));
        Log.d(TAG, "new package first launch record : " + this.mActivationRecords.getLast());
    }

    @Override // com.android.server.pm.PackageEventRecorderInternal
    public Bundle getSourcePackage(String activatedPackage, int userId, boolean clear) {
        if (!isEnabled() || TextUtils.isEmpty(activatedPackage)) {
            return null;
        }
        Iterator<ActivationRecord> iterator = this.mActivationRecords.iterator();
        String sourcePackage = null;
        while (true) {
            if (!iterator.hasNext()) {
                break;
            }
            ActivationRecord item = iterator.next();
            if (activatedPackage.equals(item.activatedPackage) && userId == item.userId) {
                if (clear) {
                    iterator.remove();
                    Log.d(TAG, "remove package first launch record : " + item);
                }
                sourcePackage = item.sourcePackage;
            }
        }
        if (TextUtils.isEmpty(sourcePackage)) {
            return null;
        }
        Bundle result = new Bundle();
        result.putString("activate_source", sourcePackage);
        return result;
    }
}
