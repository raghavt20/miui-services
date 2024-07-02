package com.android.server;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.system.StructStat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Slog;
import com.android.server.appcacheopt.AppCacheOptimizerStub;
import com.android.server.pm.Installer;
import com.xiaomi.NetworkBoost.slaservice.FormatBytesUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import miui.app.backup.BackupFileInfo;
import miui.app.backup.IGetFileInfoCallback;
import miui.app.backup.IListDirCallback;
import miui.app.backup.IMiuiRestoreManager;
import miui.app.backup.IRestoreListener;
import miui.app.backup.ITaskCommonCallback;
import miui.app.backup.ITransferDataCallback;

/* loaded from: classes.dex */
public class MiuiRestoreManagerService extends IMiuiRestoreManager.Stub {
    private static final int DEFAULT_THREAD_COUNT = 3;
    private static final int ERROR = 1;
    private static final int ERROR_CODE_BAD_SEINFO = -6;
    private static final int ERROR_CODE_INVOKE_FAIL = -1;
    private static final int ERROR_CODE_NO_APP_INFO = -2;
    private static final int ERROR_FILE_CHANGED = -7;
    private static final int ERROR_NONE = 0;
    private static final int FLAG_MOVE_COPY_MODE = 2;
    private static final int FLAG_MOVE_RENAME_MODE = 0;
    private static final int FLAG_MOVE_WITH_LOCK = 4;
    private static final String PACKAGE_BACKUP = "com.miui.backup";
    private static final String PACKAGE_CLOUD_BACKUP = "com.miui.cloudbackup";
    private static final String PACKAGE_HUANJI = "com.miui.huanji";
    public static final String SERVICE_NAME = "miui.restore.service";
    private static final String TAG = "MiuiRestoreManagerService";
    private final Map<Integer, AppDataRootPath> mAppDataRootPathMap;
    private Context mContext;
    private Installer mInstaller;
    private Executor mInstallerExecutor;
    private Executor mListDataExecutor;
    private final RemoteCallbackList<IRestoreListener> mRestoreObservers = new RemoteCallbackList<>();
    private Executor mTransferExecutor;

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiRestoreManagerService mService;

        public Lifecycle(Context context, Installer installer) {
            super(context);
            this.mService = new MiuiRestoreManagerService(context, installer);
        }

        public void onStart() {
            publishBinderService(MiuiRestoreManagerService.SERVICE_NAME, this.mService);
        }
    }

    public MiuiRestoreManagerService(Context context, Installer installer) {
        Slog.d(TAG, "init MiuiRestoreManagerService");
        this.mContext = context;
        this.mInstaller = installer;
        this.mInstallerExecutor = Executors.newFixedThreadPool(3);
        this.mListDataExecutor = Executors.newSingleThreadExecutor();
        this.mTransferExecutor = Executors.newFixedThreadPool(5);
        this.mAppDataRootPathMap = new ConcurrentHashMap();
    }

    public boolean moveData(String sourcePath, String destPath, String packageName, int userId, boolean isSdcardData, int flag) {
        if (Binder.getCallingUid() != 6100) {
            Slog.w(TAG, "caller is not backup uid");
            return false;
        }
        if (!TextUtils.isEmpty(sourcePath) && !TextUtils.isEmpty(destPath)) {
            if (!TextUtils.isEmpty(packageName)) {
                Slog.d(TAG, "call move:dp=" + destPath + ",pkg=" + packageName + ",userId=" + userId);
                if (isSdcardData) {
                    return moveSdcardData(sourcePath, destPath, packageName, userId, flag);
                }
                if (!sourcePath.contains(packageName) || !destPath.contains(packageName)) {
                    Slog.d(TAG, "the path does not match the package name");
                    return false;
                }
                long token = Binder.clearCallingIdentity();
                try {
                    ApplicationInfo info = AppGlobals.getPackageManager().getApplicationInfo(packageName, FormatBytesUtil.KB, userId);
                    if (info == null) {
                        Slog.e(TAG, "package application info is null");
                        return false;
                    }
                    this.mInstallerExecutor.execute(new MoveDataTask(this.mInstaller, sourcePath, destPath, packageName, info.uid, userId, info.seInfo, flag));
                    return true;
                } catch (Exception e) {
                    Slog.e(TAG, "get package application info fail ", e);
                    return false;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
        Slog.w(TAG, "path or packageName is null");
        return false;
    }

    private boolean moveSdcardData(String sourcePath, String destPath, String packageName, int userId, int flag) {
        this.mInstallerExecutor.execute(new MoveDataTask(this.mInstaller, sourcePath, destPath, packageName, 0, userId, "", flag));
        return true;
    }

    public int backupAppData(String packageName, String[] paths, String[] domains, String[] parentPaths, String[] excludePaths, ParcelFileDescriptor outFd, int type, ITaskCommonCallback callback) {
        if (Binder.getCallingUid() != 6100) {
            Slog.w(TAG, "caller is not backup uid");
            return 1;
        }
        if (paths != null && domains != null) {
            Slog.d(TAG, "call backupData:paths=" + Arrays.toString(paths));
            this.mInstallerExecutor.execute(new BackupDataTask(packageName, paths, domains, parentPaths, excludePaths, outFd, type, callback));
            return 0;
        }
        Slog.w(TAG, "path or domains is null");
        return 1;
    }

    private AppDataRootPath getAppDataRootPath(int deviceUid) {
        AppDataRootPath rootPath = this.mAppDataRootPathMap.get(Integer.valueOf(deviceUid));
        if (rootPath == null) {
            String external = getExternalAppDataRootPathOrNull(deviceUid);
            String internal = getInternalAppDataRootPathOrNull(this.mContext, deviceUid);
            AppDataRootPath rootPath2 = new AppDataRootPath(external, internal);
            this.mAppDataRootPathMap.put(Integer.valueOf(deviceUid), rootPath2);
            Slog.w(TAG, "app data root：" + rootPath2);
            return rootPath2;
        }
        return rootPath;
    }

    private static String getExternalAppDataRootPathOrNull(int deviceUid) {
        File appDataParentFile;
        Environment.UserEnvironment environment = new Environment.UserEnvironment(deviceUid);
        File[] files = environment.buildExternalStorageAppDataDirs(PACKAGE_CLOUD_BACKUP);
        if (files != null && files.length > 0 && (appDataParentFile = files[0].getParentFile()) != null) {
            return appDataParentFile.getPath();
        }
        return null;
    }

    private static String getInternalAppDataRootPathOrNull(Context context, int deviceUid) {
        File appDataParentFile;
        try {
            File appDataFile = context.createPackageContextAsUser(PACKAGE_CLOUD_BACKUP, 2, UserHandle.of(deviceUid)).getDataDir();
            if (appDataFile != null && (appDataParentFile = appDataFile.getParentFile()) != null) {
                return appDataParentFile.getPath();
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "package name not find");
            return null;
        }
    }

    public boolean transferData(String src, String targetBase, String targetRelative, String targetPkgName, int targetMode, boolean copy, int userId, boolean isExternal, ITransferDataCallback callback) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        Slog.d(TAG, "call transfer data, src= " + src + ", targetBase= " + targetBase + ", targetRelative= " + targetRelative + ", targetPkgName= " + targetPkgName + ", targetMode= " + targetMode + ", copy= " + copy + ", userId= " + userId + ", isExternal= " + isExternal + ", uid= " + callingUid + ", pid= " + callingPid);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                if (!checkIsLegalPackageAndCallingUid(callingUid, PACKAGE_CLOUD_BACKUP)) {
                    Slog.w(TAG, "not cloud backup");
                    Binder.restoreCallingIdentity(token);
                    return false;
                }
                try {
                    if (!TextUtils.isEmpty(src) && !TextUtils.isEmpty(targetBase)) {
                        if (!TextUtils.isEmpty(targetRelative) && !TextUtils.isEmpty(targetPkgName)) {
                            AppDataRootPath appDataRootPath = getAppDataRootPath(UserHandle.getUserId(callingUid));
                            String rootPath = isExternal ? appDataRootPath.external : appDataRootPath.internal;
                            if (rootPath == null) {
                                Slog.w(TAG, "null root path");
                                Binder.restoreCallingIdentity(token);
                                return false;
                            }
                            if (src.startsWith(rootPath) && targetBase.startsWith(rootPath)) {
                                if (callback == null) {
                                    Slog.w(TAG, "no callback");
                                    Binder.restoreCallingIdentity(token);
                                    return false;
                                }
                                this.mTransferExecutor.execute(new TransferDataTask(src, targetBase, targetRelative, targetPkgName, targetMode, copy, userId, isExternal, callback));
                                Binder.restoreCallingIdentity(token);
                                return true;
                            }
                            Slog.w(TAG, "illegal path");
                            Binder.restoreCallingIdentity(token);
                            return false;
                        }
                    }
                    Slog.w(TAG, "invalid params");
                    Binder.restoreCallingIdentity(token);
                    return false;
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:43:0x00af A[Catch: all -> 0x00d4, TRY_LEAVE, TryCatch #0 {all -> 0x00d4, blocks: (B:3:0x0038, B:5:0x0041, B:7:0x0049, B:9:0x0051, B:13:0x005b, B:15:0x0061, B:18:0x006b, B:20:0x0077, B:22:0x007b, B:27:0x0089, B:29:0x008d, B:35:0x009c, B:37:0x00a0, B:43:0x00af, B:47:0x00bb, B:50:0x00c5), top: B:2:0x0038 }] */
    /* JADX WARN: Removed duplicated region for block: B:46:0x00b9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean getDataFileInfo(java.lang.String r11, miui.app.backup.IGetFileInfoCallback r12) {
        /*
            r10 = this;
            int r0 = android.os.Binder.getCallingUid()
            int r1 = android.os.Binder.getCallingPid()
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "call get data file info, path= "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r11)
            java.lang.String r3 = ", uid= "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r0)
            java.lang.String r3 = ", pid= "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r1)
            java.lang.String r2 = r2.toString()
            java.lang.String r3 = "MiuiRestoreManagerService"
            android.util.Slog.d(r3, r2)
            long r4 = android.os.Binder.clearCallingIdentity()
            java.lang.String r2 = "com.miui.cloudbackup"
            boolean r2 = r10.checkIsLegalPackageAndCallingUid(r0, r2)     // Catch: java.lang.Throwable -> Ld4
            r6 = 0
            if (r2 != 0) goto L5b
            java.lang.String r2 = "com.miui.backup"
            boolean r2 = r10.checkIsLegalPackageAndCallingUid(r0, r2)     // Catch: java.lang.Throwable -> Ld4
            if (r2 != 0) goto L5b
            java.lang.String r2 = "com.miui.huanji"
            boolean r2 = r10.checkIsLegalPackageAndCallingUid(r0, r2)     // Catch: java.lang.Throwable -> Ld4
            if (r2 != 0) goto L5b
            java.lang.String r2 = "not cloud backup or huanji"
            android.util.Slog.w(r3, r2)     // Catch: java.lang.Throwable -> Ld4
            android.os.Binder.restoreCallingIdentity(r4)
            return r6
        L5b:
            boolean r2 = android.text.TextUtils.isEmpty(r11)     // Catch: java.lang.Throwable -> Ld4
            if (r2 == 0) goto L6b
            java.lang.String r2 = "invalid params"
            android.util.Slog.w(r3, r2)     // Catch: java.lang.Throwable -> Ld4
            android.os.Binder.restoreCallingIdentity(r4)
            return r6
        L6b:
            int r2 = android.os.UserHandle.getUserId(r0)     // Catch: java.lang.Throwable -> Ld4
            com.android.server.MiuiRestoreManagerService$AppDataRootPath r2 = r10.getAppDataRootPath(r2)     // Catch: java.lang.Throwable -> Ld4
            java.lang.String r7 = r2.external     // Catch: java.lang.Throwable -> Ld4
            if (r7 != 0) goto L85
            java.lang.String r7 = r2.internal     // Catch: java.lang.Throwable -> Ld4
            if (r7 != 0) goto L85
            java.lang.String r7 = "null root path"
            android.util.Slog.w(r3, r7)     // Catch: java.lang.Throwable -> Ld4
            android.os.Binder.restoreCallingIdentity(r4)
            return r6
        L85:
            r7 = 0
            r8 = 1
            if (r7 != 0) goto L98
            java.lang.String r9 = r2.external     // Catch: java.lang.Throwable -> Ld4
            if (r9 == 0) goto L96
            java.lang.String r9 = r2.external     // Catch: java.lang.Throwable -> Ld4
            boolean r9 = r11.startsWith(r9)     // Catch: java.lang.Throwable -> Ld4
            if (r9 == 0) goto L96
            goto L98
        L96:
            r9 = r6
            goto L99
        L98:
            r9 = r8
        L99:
            r7 = r9
            if (r7 != 0) goto Lab
            java.lang.String r9 = r2.internal     // Catch: java.lang.Throwable -> Ld4
            if (r9 == 0) goto La9
            java.lang.String r9 = r2.internal     // Catch: java.lang.Throwable -> Ld4
            boolean r9 = r11.startsWith(r9)     // Catch: java.lang.Throwable -> Ld4
            if (r9 == 0) goto La9
            goto Lab
        La9:
            r9 = r6
            goto Lac
        Lab:
            r9 = r8
        Lac:
            r7 = r9
            if (r7 != 0) goto Lb9
            java.lang.String r8 = "illegal path"
            android.util.Slog.w(r3, r8)     // Catch: java.lang.Throwable -> Ld4
            android.os.Binder.restoreCallingIdentity(r4)
            return r6
        Lb9:
            if (r12 != 0) goto Lc5
            java.lang.String r8 = "no callback"
            android.util.Slog.w(r3, r8)     // Catch: java.lang.Throwable -> Ld4
            android.os.Binder.restoreCallingIdentity(r4)
            return r6
        Lc5:
            java.util.concurrent.Executor r3 = r10.mListDataExecutor     // Catch: java.lang.Throwable -> Ld4
            com.android.server.MiuiRestoreManagerService$GetDataFileInfoTask r6 = new com.android.server.MiuiRestoreManagerService$GetDataFileInfoTask     // Catch: java.lang.Throwable -> Ld4
            r6.<init>(r11, r12)     // Catch: java.lang.Throwable -> Ld4
            r3.execute(r6)     // Catch: java.lang.Throwable -> Ld4
            android.os.Binder.restoreCallingIdentity(r4)
            return r8
        Ld4:
            r2 = move-exception
            android.os.Binder.restoreCallingIdentity(r4)
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiRestoreManagerService.getDataFileInfo(java.lang.String, miui.app.backup.IGetFileInfoCallback):boolean");
    }

    /* JADX WARN: Removed duplicated region for block: B:44:0x00cf A[Catch: all -> 0x010d, TRY_LEAVE, TryCatch #0 {all -> 0x010d, blocks: (B:3:0x0054, B:5:0x005d, B:7:0x0065, B:9:0x006d, B:13:0x0077, B:19:0x0087, B:21:0x0093, B:23:0x0097, B:28:0x00a6, B:30:0x00aa, B:36:0x00ba, B:38:0x00be, B:44:0x00cf, B:48:0x00db, B:51:0x00e5, B:56:0x0103), top: B:2:0x0054 }] */
    /* JADX WARN: Removed duplicated region for block: B:47:0x00d9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean listDataDir(java.lang.String r21, long r22, int r24, miui.app.backup.IListDirCallback r25) {
        /*
            Method dump skipped, instructions count: 274
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiRestoreManagerService.listDataDir(java.lang.String, long, int, miui.app.backup.IListDirCallback):boolean");
    }

    private boolean checkIsLegalPackageAndCallingUid(int callingUid, String packageName) {
        try {
            String[] packages = AppGlobals.getPackageManager().getPackagesForUid(callingUid);
            if (packages != null) {
                for (String pkgName : packages) {
                    if (TextUtils.equals(packageName, pkgName) && AppGlobals.getPackageManager().checkUidSignatures(callingUid, Process.myUid()) == 0) {
                        return true;
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "get package info fail ", e);
        }
        return false;
    }

    public void registerRestoreListener(IRestoreListener listener) {
        this.mRestoreObservers.register(listener);
    }

    public void unregisterRestoreListener(IRestoreListener listener) {
        this.mRestoreObservers.unregister(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyMoveTaskEnd(String packageName, int userId, int errorCode) {
        synchronized (this.mRestoreObservers) {
            int cnt = this.mRestoreObservers.beginBroadcast();
            for (int i = 0; i < cnt; i++) {
                try {
                    this.mRestoreObservers.getBroadcastItem(i).onRestoreEnd(packageName, userId, errorCode);
                } catch (RemoteException e) {
                    Slog.e(TAG, "RemoteException error in notifyMoveTaskEnd ", e);
                }
            }
            this.mRestoreObservers.finishBroadcast();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MoveDataTask implements Runnable {
        String destPath;
        int flag;
        Installer installer;
        String packageName;
        int packageUserId;
        String seInfo;
        MiuiRestoreManagerService service;
        String sourcePath;
        int userId;

        private MoveDataTask(MiuiRestoreManagerService service, Installer installer, String sourcePath, String destPath, String packageName, int packageUserId, int userId, String seInfo, int flag) {
            this.service = service;
            this.installer = installer;
            this.sourcePath = sourcePath;
            this.destPath = destPath;
            this.packageName = packageName;
            this.packageUserId = packageUserId;
            this.userId = userId;
            this.seInfo = seInfo;
            this.flag = flag;
        }

        @Override // java.lang.Runnable
        public void run() {
            Slog.d(MiuiRestoreManagerService.TAG, "execute move:dp=" + this.destPath + ",pkg=" + this.packageName + ",userId=" + this.userId + ",uid=" + this.packageUserId);
            boolean result = false;
            int tag = 0;
            int i = this.flag;
            if ((i & 2) != 0) {
                tag = 0 | 2;
            }
            if ((i & 4) != 0) {
                tag |= 4;
            }
            try {
                AppCacheOptimizerStub.getInstance().umountByPackageName(this.packageName, this.userId);
                Installer installer = this.installer;
                String str = this.sourcePath;
                String str2 = this.destPath;
                int i2 = this.packageUserId;
                result = installer.moveData(str, str2, i2, i2, this.seInfo, tag);
                AppCacheOptimizerStub.getInstance().mountByPackageName(this.packageName);
            } catch (Exception e) {
                Slog.w(MiuiRestoreManagerService.TAG, "move data error ", e);
            }
            Slog.d(MiuiRestoreManagerService.TAG, "result=" + result + ",pkg=" + this.packageName + ",userId=" + this.userId);
            this.service.notifyMoveTaskEnd(this.packageName, this.userId, result ? 0 : 1);
        }
    }

    /* loaded from: classes.dex */
    private class TransferDataTask implements Runnable {
        final ITransferDataCallback callback;
        final boolean copy;
        final boolean isExternal;
        final String src;
        final String targetBase;
        final int targetMode;
        final String targetPkgName;
        final String targetRelative;
        final int userId;

        public TransferDataTask(String src, String targetBase, String targetRelative, String targetPkgName, int targetMode, boolean copy, int userId, boolean isExternal, ITransferDataCallback callback) {
            this.src = src;
            this.targetBase = targetBase;
            this.targetRelative = targetRelative;
            this.targetPkgName = targetPkgName;
            this.targetMode = targetMode;
            this.copy = copy;
            this.userId = userId;
            this.isExternal = isExternal;
            this.callback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            int errorCode;
            int gid;
            String seInfo;
            StructStat curStat;
            try {
                List<String> tmpStatList = new ArrayList<>();
                errorCode = MiuiRestoreManagerService.this.mInstaller.getDataFileStat(this.src, tmpStatList);
                if (errorCode == 0) {
                    StructStat preStat = null;
                    if (this.copy || this.isExternal) {
                        preStat = parseStructStatFromList(tmpStatList);
                    }
                    ApplicationInfo info = AppGlobals.getPackageManager().getApplicationInfo(this.targetPkgName, 0L, this.userId);
                    if (info == null) {
                        throw new QueryAppInfoErrorException("no app info with pkg: " + this.targetPkgName);
                    }
                    if (info.seInfo == null) {
                        throw new BadSeInfoException("se info is null");
                    }
                    int uid = info.uid;
                    if (this.isExternal) {
                        gid = preStat == null ? -1 : preStat.st_gid;
                        seInfo = "";
                    } else {
                        gid = info.uid;
                        seInfo = info.seInfo;
                    }
                    errorCode = MiuiRestoreManagerService.this.mInstaller.transferData(this.src, this.targetBase, this.targetRelative, this.copy, uid, gid, this.targetMode, seInfo, this.isExternal);
                    tmpStatList.clear();
                    if (errorCode == 0 && this.copy && preStat != null && MiuiRestoreManagerService.this.mInstaller.getDataFileStat(this.src, tmpStatList) != 0 && (curStat = parseStructStatFromList(tmpStatList)) != null && (preStat.st_mtime != curStat.st_mtime || preStat.st_size != curStat.st_size)) {
                        throw new FileChangedException("file has been changed");
                    }
                }
            } catch (RemoteException | QueryAppInfoErrorException e) {
                errorCode = -2;
                Slog.w(MiuiRestoreManagerService.TAG, "query app info failed ", e);
            } catch (BadSeInfoException e2) {
                errorCode = -6;
                Slog.w(MiuiRestoreManagerService.TAG, "se info is null ", e2);
            } catch (FileChangedException e3) {
                errorCode = MiuiRestoreManagerService.ERROR_FILE_CHANGED;
                Slog.w(MiuiRestoreManagerService.TAG, "file changed ", e3);
            } catch (Installer.InstallerException e4) {
                errorCode = -1;
                Slog.w(MiuiRestoreManagerService.TAG, "transfer data dir error ", e4);
            }
            try {
                this.callback.onTransferDataEnd(errorCode, this.src, TextUtils.concat(this.targetBase, this.targetRelative).toString());
            } catch (RemoteException e5) {
                Slog.e(MiuiRestoreManagerService.TAG, "error when notify onTransferDataEnd ", e5);
            }
        }

        private StructStat parseStructStatFromList(List<String> stat) {
            return new StructStat(Long.parseLong(stat.get(0)), Long.parseLong(stat.get(1)), (int) Long.parseLong(stat.get(2)), Long.parseLong(stat.get(3)), (int) Long.parseLong(stat.get(4)), (int) Long.parseLong(stat.get(5)), Long.parseLong(stat.get(6)), Long.parseLong(stat.get(7)), Long.parseLong(stat.get(8)), Long.parseLong(stat.get(9)), Long.parseLong(stat.get(10)), Long.parseLong(stat.get(11)), Long.parseLong(stat.get(12)));
        }
    }

    /* loaded from: classes.dex */
    private class GetDataFileInfoTask implements Runnable {
        final IGetFileInfoCallback callback;
        final String path;

        public GetDataFileInfoTask(String path, IGetFileInfoCallback callback) {
            this.path = path;
            this.callback = callback;
        }

        /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:18:0x0052 -> B:10:0x0058). Please report as a decompilation issue!!! */
        @Override // java.lang.Runnable
        public void run() {
            int errorCode;
            BackupFileInfo info = null;
            try {
                List<String> infoList = new ArrayList<>();
                errorCode = MiuiRestoreManagerService.this.mInstaller.getDataFileInfo(this.path, infoList);
                if (errorCode == 0) {
                    if (infoList.isEmpty()) {
                        info = new BackupFileInfo(this.path, true);
                    } else {
                        String md5Hex = infoList.get(0);
                        String sha1Hex = infoList.get(1);
                        String size = infoList.get(2);
                        info = new BackupFileInfo(this.path, false, hex2Base64(md5Hex), hex2Base64(sha1Hex), Long.parseLong(size));
                    }
                }
            } catch (Installer.InstallerException e) {
                errorCode = -1;
                Slog.w(MiuiRestoreManagerService.TAG, "get data file info error ", e);
            }
            try {
                this.callback.onGetDataFileInfoEnd(errorCode, this.path, info);
            } catch (RemoteException e2) {
                Slog.e(MiuiRestoreManagerService.TAG, "error when notify onGetDataFileInfoEnd ", e2);
            }
        }

        private String hex2Base64(String hex) {
            if (TextUtils.isEmpty(hex)) {
                return "";
            }
            return Base64.encodeToString(HexFormat.of().parseHex(hex), 2);
        }
    }

    /* loaded from: classes.dex */
    private class ListDataDirTask implements Runnable {
        final IListDirCallback callback;
        final int maxCount;
        final String path;
        final long start;

        public ListDataDirTask(String path, long start, int maxCount, IListDirCallback callback) {
            this.path = path;
            this.start = start;
            this.maxCount = maxCount;
            this.callback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            int errorCode;
            String[] data = new String[0];
            long[] offset = new long[1];
            try {
                List<String> list = new ArrayList<>();
                errorCode = MiuiRestoreManagerService.this.mInstaller.listDataDir(this.path, this.start, this.maxCount, list, offset);
                if (errorCode == 0) {
                    data = (String[]) list.toArray(data);
                }
            } catch (Installer.InstallerException e) {
                Slog.w(MiuiRestoreManagerService.TAG, "list data dir error ", e);
                errorCode = -1;
            }
            try {
                this.callback.onListDataDirEnd(errorCode, this.path, data, offset[0]);
            } catch (RemoteException e2) {
                Slog.e(MiuiRestoreManagerService.TAG, "error when notify onListDataDirEnd ", e2);
            }
        }
    }

    /* loaded from: classes.dex */
    private class BackupDataTask implements Runnable {
        private ITaskCommonCallback callback;
        private String[] domains;
        private String[] excludePaths;
        private ParcelFileDescriptor outFd;
        private String packageName;
        private String[] parentPaths;
        private String[] paths;
        private int type;

        public BackupDataTask(String packageName, String[] paths, String[] domains, String[] parentPaths, String[] excludePaths, ParcelFileDescriptor outFd, int type, ITaskCommonCallback callback) {
            this.packageName = packageName;
            this.paths = paths;
            this.domains = domains;
            this.parentPaths = parentPaths;
            this.excludePaths = excludePaths;
            this.outFd = outFd;
            this.type = type;
            this.callback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            int errorCode = 0;
            String exception = null;
            try {
                try {
                    try {
                        this.callback.onTaskEnd(MiuiRestoreManagerService.this.mInstaller.backupAppData(this.packageName, this.paths, this.domains, this.parentPaths, this.excludePaths, this.outFd), (String) null, (String) null);
                    } catch (Exception e) {
                        Slog.e(MiuiRestoreManagerService.TAG, "error when notify onTaskEnd ", e);
                    }
                    FileOutputStream out = new FileOutputStream(this.outFd.getFileDescriptor());
                    byte[] buf = new byte[4];
                    try {
                        out.write(buf);
                        out.close();
                    } catch (IOException e2) {
                        e = e2;
                        Slog.e(MiuiRestoreManagerService.TAG, "error when write EOF ", e);
                    }
                } catch (Installer.InstallerException e3) {
                    errorCode = 1;
                    exception = e3.getMessage();
                    Slog.w(MiuiRestoreManagerService.TAG, "backupData ", e3);
                    try {
                        this.callback.onTaskEnd(1, exception, (String) null);
                    } catch (Exception e4) {
                        Slog.e(MiuiRestoreManagerService.TAG, "error when notify onTaskEnd ", e4);
                    }
                    FileOutputStream out2 = new FileOutputStream(this.outFd.getFileDescriptor());
                    byte[] buf2 = new byte[4];
                    try {
                        out2.write(buf2);
                        out2.close();
                    } catch (IOException e5) {
                        e = e5;
                        Slog.e(MiuiRestoreManagerService.TAG, "error when write EOF ", e);
                    }
                }
            } catch (Throwable th) {
                try {
                    this.callback.onTaskEnd(errorCode, exception, (String) null);
                } catch (Exception e6) {
                    Slog.e(MiuiRestoreManagerService.TAG, "error when notify onTaskEnd ", e6);
                }
                FileOutputStream out3 = new FileOutputStream(this.outFd.getFileDescriptor());
                byte[] buf3 = new byte[4];
                try {
                    out3.write(buf3);
                    out3.close();
                    throw th;
                } catch (IOException e7) {
                    Slog.e(MiuiRestoreManagerService.TAG, "error when write EOF ", e7);
                    throw th;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AppDataRootPath {
        final String external;
        final String internal;

        public AppDataRootPath(String external, String internal) {
            this.external = external;
            this.internal = internal;
        }

        public String toString() {
            return "AppDataRootPath{external='" + this.external + "', internal='" + this.internal + "'}";
        }
    }

    /* loaded from: classes.dex */
    private static class BadFileDescriptorException extends Exception {
        public BadFileDescriptorException(String msg) {
            super(msg);
        }
    }

    /* loaded from: classes.dex */
    private static class QueryAppInfoErrorException extends Exception {
        public QueryAppInfoErrorException(String msg) {
            super(msg);
        }
    }

    /* loaded from: classes.dex */
    private static class BadSeInfoException extends Exception {
        public BadSeInfoException(String msg) {
            super(msg);
        }
    }

    /* loaded from: classes.dex */
    private static class FileChangedException extends Exception {
        public FileChangedException(String msg) {
            super(msg);
        }
    }
}
