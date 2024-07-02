package com.android.server.aiinput;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.server.SystemService;
import com.xiaomi.aiinput.IAIInputTextManager;
import com.xiaomi.aiinput.IAIInputTextManagerClient;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/* loaded from: classes.dex */
public class AIInputTextManagerService extends IAIInputTextManager.Stub {
    private static final HashSet<String> BLACK_LIST;
    private static final String MSG_PERMISSION_DENIAL = "Permission Denial: AI_INPUT_INFORMATION";
    private static final String PERMISSION_AI_INPUT_INFORMATION = "com.miui.aiinput.permission.AI_INPUT_INFORMATION";
    public static final String SERVICE_NAME = "aiinputtext";
    public static final String TAG = "AIInputTextManagerService";
    private static AIInputTextManagerService instance;
    private final Context mContext;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.aiinput.AIInputTextManagerService.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            AIInputTextManagerService.this.mManagerClient = null;
        }
    };
    private ManagerClient mManagerClient;
    private final PackageManager mPackageManager;

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final AIInputTextManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = AIInputTextManagerService.getInstance(context);
        }

        public void onStart() {
            publishBinderService(AIInputTextManagerService.SERVICE_NAME, this.mService);
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        BLACK_LIST = hashSet;
        hashSet.add("com.miui.voiceassist");
    }

    /* loaded from: classes.dex */
    private static class ManagerClient {
        private IAIInputTextManagerClient mClient;
        private String mContext;

        public ManagerClient(IAIInputTextManagerClient client, String ctx) {
            this.mClient = client;
            this.mContext = ctx;
        }

        public boolean equals(Object obj) {
            return this.mClient.equals(((ManagerClient) obj).mClient) && this.mContext.equals(((ManagerClient) obj).mContext);
        }

        public IAIInputTextManagerClient getClient() {
            return this.mClient;
        }
    }

    private AIInputTextManagerService(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    public static synchronized AIInputTextManagerService getInstance(Context context) {
        AIInputTextManagerService aIInputTextManagerService;
        synchronized (AIInputTextManagerService.class) {
            if (instance == null) {
                instance = new AIInputTextManagerService(context);
            }
            aIInputTextManagerService = instance;
        }
        return aIInputTextManagerService;
    }

    public void addClient(IAIInputTextManagerClient client, String ctx, String pkg) throws RemoteException {
        if (client == null) {
            return;
        }
        Iterator<String> it = BLACK_LIST.iterator();
        while (it.hasNext()) {
            String blackCtx = it.next();
            if (ctx.contains(blackCtx) || TextUtils.equals(pkg, blackCtx)) {
                return;
            }
        }
        ManagerClient managerClient = this.mManagerClient;
        if (managerClient != null && managerClient.getClient() != null) {
            try {
                this.mManagerClient.getClient().asBinder().unlinkToDeath(this.mDeathRecipient, 0);
            } catch (NoSuchElementException e) {
            }
        }
        client.asBinder().linkToDeath(this.mDeathRecipient, 0);
        this.mManagerClient = new ManagerClient(client, ctx);
    }

    public IAIInputTextManagerClient getAIInputTextClient() {
        ManagerClient managerClient;
        Context context = this.mContext;
        if (context == null) {
            return null;
        }
        context.enforceCallingPermission(PERMISSION_AI_INPUT_INFORMATION, MSG_PERMISSION_DENIAL);
        if (isDenialFromUid(Binder.getCallingUid()) || (managerClient = this.mManagerClient) == null) {
            return null;
        }
        return managerClient.getClient();
    }

    private boolean isDenialFromUid(int uid) {
        PackageManager packageManager;
        String[] packages;
        if (this.mContext == null || (packageManager = this.mPackageManager) == null || (packages = packageManager.getPackagesForUid(uid)) == null || packages.length <= 0) {
            return true;
        }
        return true ^ TextUtils.equals(packages[0], "com.miui.voiceassist");
    }
}
