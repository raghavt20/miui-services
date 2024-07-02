package com.android.server.inputmethod;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Insets;
import android.graphics.drawable.Icon;
import android.inputmethodservice.InputMethodAnalyticsUtil;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.inputmethod.IInputMethodSessionCallback;
import com.android.internal.inputmethod.IRemoteInputConnection;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.inputmethod.InputMethodMenuController;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.server.inputmethod.InputMethodUtils;
import com.android.server.policy.DisplayTurnoverManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miuix.appcompat.app.AlertDialog;

@MiuiStubHead(manifestName = "com.android.server.inputmethod.InputMethodManagerServiceStub$$")
/* loaded from: classes.dex */
public class InputMethodManagerServiceImpl extends InputMethodManagerServiceStub {
    public static final boolean DEBUG = true;
    private static final int IME_LIST_VIEW_VISIBLE_NUMBER = 3;
    private static final String MIRROR_INPUT_STATE = "mirror_input_state";
    public static final String MIUIXPACKAGE = "miuix.stub";
    public static final String MIUI_HOME = "com.miui.home";
    public static final String TAG = "InputMethodManagerServiceImpl";
    private static final List<String> customizedInputMethodList;
    Context dialogContext;
    InputMethodBindingController mBindingController;
    Context mContext;
    Handler mHandler;
    private MiuiInputMethodEventListener mMiuiInputMethodEventListener;
    private ContentObserver mObserver;
    private IBinder mMonitorBinder = null;
    private volatile int mSynergyOperate = 0;
    private volatile int mLastAcceptStatus = 0;
    public int noCustomizedCheckedItem = -1;
    public InputMethodInfo[] mNoCustomizedIms = null;
    public int[] mNoCustomizedSubtypeIds = null;
    private boolean mScreenStatus = false;
    private boolean isExpanded = false;
    private boolean mScreenOffLastTime = false;
    private boolean callStartInputOrWindowGainedFocusFlag = false;
    private int mSessionId = 0;
    private boolean mMirrorInputState = true;
    private int mMirrorDisplayId = 0;
    private boolean mIsFromRemoteDevice = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<InputMethodManagerServiceImpl> {

        /* compiled from: InputMethodManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final InputMethodManagerServiceImpl INSTANCE = new InputMethodManagerServiceImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public InputMethodManagerServiceImpl m1645provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public InputMethodManagerServiceImpl m1644provideNewInstance() {
            return new InputMethodManagerServiceImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        customizedInputMethodList = arrayList;
        arrayList.add("com.iflytek.inputmethod.miui");
        arrayList.add("com.baidu.input_mi");
        arrayList.add("com.sohu.inputmethod.sogou.xiaomi");
        arrayList.add("com.android.cts.mockime");
    }

    public static InputMethodManagerServiceImpl getInstance() {
        return (InputMethodManagerServiceImpl) InputMethodManagerServiceStub.getInstance();
    }

    public void init(Handler handler, InputMethodBindingController bindingController, Context context) {
        this.mHandler = handler;
        this.mBindingController = bindingController;
        this.mContext = context;
        registerObserver(context);
        try {
            Context context2 = this.mContext;
            if (context2 != null) {
                this.mMirrorInputState = Settings.Secure.getInt(context2.getContentResolver(), MIRROR_INPUT_STATE) != 1;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Exception cause:Failed to read Settings for MIRROR_INPUT_STATE:" + e);
        }
    }

    public void enableSystemIMEsIfThereIsNoEnabledIME(List<InputMethodInfo> methodList, InputMethodUtils.InputMethodSettings settings) {
        if (Build.IS_CM_CUSTOMIZATION_TEST || methodList == null || settings == null) {
            return;
        }
        List<Pair<String, ArrayList<String>>> enabledInputMethodsList = settings.getEnabledInputMethodsAndSubtypeListLocked();
        InputMethodInfo systemInputMethod = null;
        for (int i = 0; i < methodList.size(); i++) {
            InputMethodInfo inputMethodInfo = methodList.get(i);
            if ((inputMethodInfo.getServiceInfo().applicationInfo.flags & 1) != 0) {
                systemInputMethod = inputMethodInfo;
            }
            if (enabledInputMethodsList != null) {
                for (Pair<String, ArrayList<String>> pair : enabledInputMethodsList) {
                    if (TextUtils.equals((CharSequence) pair.first, inputMethodInfo.getId())) {
                        return;
                    }
                }
            }
        }
        if (systemInputMethod != null) {
            settings.appendAndPutEnabledInputMethodLocked(systemInputMethod.getId(), false);
        }
    }

    public void onSwitchIME(Context context, InputMethodInfo curInputMethodInfo, String lastInputMethodId, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList, InputMethodUtils.InputMethodSettings inputMethodSettings, ArrayMap<String, InputMethodInfo> methodMap) {
        if (TextUtils.equals(curInputMethodInfo.getId(), lastInputMethodId)) {
            return;
        }
        InputMethodAnalyticsUtil.addNotificationPanelRecord(context, curInputMethodInfo.getPackageName());
    }

    public boolean onMiuiTransact(int code, Parcel data, Parcel reply, int flags, IRemoteInputConnection inputContext, int imeWindowVis, Handler handler) {
        InputMethodManagerInternal service;
        try {
            switch (code) {
                case 16777210:
                    int readInt = data.readInt();
                    this.mMirrorDisplayId = readInt;
                    this.mIsFromRemoteDevice = readInt != 0;
                    Slog.i(TAG, "set mIsFromRemoteDevice as:" + this.mIsFromRemoteDevice + ",displayId as:" + this.mMirrorDisplayId + " via mirror");
                    reply.writeNoException();
                    return true;
                case DisplayTurnoverManager.CODE_TURN_OFF_SUB_DISPLAY /* 16777211 */:
                    this.mSynergyOperate = data.readInt();
                    reply.writeNoException();
                    return true;
                case DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY /* 16777212 */:
                    this.mMonitorBinder = null;
                    unregisterPointerEventListener();
                    reply.writeNoException();
                    return true;
                case 16777213:
                    this.mMonitorBinder = data.readStrongBinder();
                    registerPointerEventListener();
                    reply.writeNoException();
                    return true;
                case 16777214:
                    int callingUid = Binder.getCallingUid();
                    if (callingUid != 1000) {
                        throw new SecurityException("Calling CODE_INPUT_TEXT unsafe by " + callingUid);
                    }
                    if ((imeWindowVis & 2) != 0 && (service = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)) != null) {
                        service.hideCurrentInputMethod(16);
                    }
                    String text = data.readString();
                    if (inputContext != null) {
                        commitTextForSynergy(inputContext, text, 1);
                    }
                    reply.writeNoException();
                    reply.writeInt(1);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean notifyAcceptInput(int status) {
        if (this.mMonitorBinder == null) {
            return false;
        }
        Parcel request = Parcel.obtain();
        this.mLastAcceptStatus = status;
        try {
            request.writeInterfaceToken("com.android.synergy.Callback");
            request.writeInt(status);
            request.writeInt(this.mMirrorDisplayId);
            this.mMonitorBinder.transact(1, request, null, 1);
            return true;
        } catch (Exception e) {
            this.mMonitorBinder = null;
            e.printStackTrace();
            return false;
        } finally {
            request.recycle();
        }
    }

    public boolean synergyOperate() {
        if (this.mMonitorBinder == null || this.mSynergyOperate != 1) {
            return false;
        }
        return this.mMirrorInputState || this.mIsFromRemoteDevice;
    }

    public void sendKeyboardCaps() {
        Slog.d(TAG, "Send caps event from keyboard.");
        isPad();
    }

    public boolean isPad() {
        return Build.IS_TABLET;
    }

    public boolean isCustomizedInputMethod(String inputMethodId) {
        int endIndex;
        String inputMethodName = "";
        if (!TextUtils.isEmpty(inputMethodId) && (endIndex = inputMethodId.indexOf(47)) > 0) {
            inputMethodName = inputMethodId.substring(0, endIndex);
        }
        return customizedInputMethodList.contains(inputMethodName);
    }

    public void initNoCustomizedInputMethodData(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> noCustomizedInputMethodList, String lastInputMethodId, int lastInputMethodSubtypeId, int NOT_A_SUBTYPE_ID) {
        int subtypeId;
        Slog.v(TAG, "initNoCustomizedInputMethodData  noCustomizedInputMethodList:" + noCustomizedInputMethodList);
        this.noCustomizedCheckedItem = -1;
        int N = noCustomizedInputMethodList.size();
        this.mNoCustomizedIms = new InputMethodInfo[N];
        this.mNoCustomizedSubtypeIds = new int[N];
        for (int i = 0; i < N; i++) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = noCustomizedInputMethodList.get(i);
            this.mNoCustomizedIms[i] = item.mImi;
            this.mNoCustomizedSubtypeIds[i] = item.mSubtypeId;
            if (this.mNoCustomizedIms[i].getId().equals(lastInputMethodId) && ((subtypeId = this.mNoCustomizedSubtypeIds[i]) == NOT_A_SUBTYPE_ID || ((lastInputMethodSubtypeId == NOT_A_SUBTYPE_ID && subtypeId == 0) || subtypeId == lastInputMethodSubtypeId))) {
                this.noCustomizedCheckedItem = i;
            }
        }
    }

    public List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> filterNotCustomziedInputMethodList(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        Slog.v(TAG, "filterNotCustomziedInputMethodList.");
        List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> noCustomizedInputMethodList = new ArrayList<>();
        Iterator<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> iterator = imList.iterator();
        while (iterator.hasNext()) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = iterator.next();
            if (!isCustomizedInputMethod(item.mImi.getId())) {
                noCustomizedInputMethodList.add(item);
                iterator.remove();
            }
        }
        return noCustomizedInputMethodList;
    }

    public void generateView(boolean isNotContainsCustomizedInputMethod, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList, AlertDialog.Builder builder, final InputMethodMenuController.ImeSubtypeListAdapter noCustomziedImeListAdapter, final InputMethodMenuController.ImeSubtypeListAdapter customziedImeListAdapter, Context context) {
        Slog.v(TAG, "Start genertating customized view.");
        this.dialogContext = context;
        if (imList.size() == 0) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) this.dialogContext.getSystemService(LayoutInflater.class);
        int customViewId = this.dialogContext.getResources().getIdentifier("input_method_switch_no_customized_list_view", "layout", MIUIXPACKAGE);
        View customView = inflater.inflate(customViewId, (ViewGroup) null);
        if (customView == null) {
            return;
        }
        int noCustomizedListViewId = this.dialogContext.getResources().getIdentifier("noCustomizedListView", "id", MIUIXPACKAGE);
        final ListView noCustomListView = (ListView) customView.findViewById(noCustomizedListViewId);
        if (noCustomListView == null) {
            return;
        }
        int indicatorId = this.dialogContext.getResources().getIdentifier("indicator", "id", MIUIXPACKAGE);
        final ImageView indicator = (ImageView) customView.findViewById(indicatorId);
        int noCustomizedListViewTitleId = this.dialogContext.getResources().getIdentifier("noCustomizedListViewTitle", "id", MIUIXPACKAGE);
        View noCustomizedListViewTitle = customView.findViewById(noCustomizedListViewTitleId);
        this.isExpanded = customziedImeListAdapter.mCheckedItem == -1;
        final int openIndicatorDrawableId = this.dialogContext.getResources().getIdentifier("listview_open_indicator", "drawable", MIUIXPACKAGE);
        final int closeIndicatorDrawableId = this.dialogContext.getResources().getIdentifier("listview_close_indicator", "drawable", MIUIXPACKAGE);
        if (!this.isExpanded) {
            noCustomListView.setVisibility(8);
        } else {
            noCustomListView.setVisibility(0);
            indicator.setImageResource(openIndicatorDrawableId);
            setNoCustomizedInputMethodListViewHeight(noCustomListView, noCustomziedImeListAdapter);
        }
        noCustomListView.setAdapter((ListAdapter) noCustomziedImeListAdapter);
        noCustomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.1
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                customziedImeListAdapter.mCheckedItem = -1;
                customziedImeListAdapter.notifyDataSetChanged();
                noCustomziedImeListAdapter.mCheckedItem = position;
                noCustomziedImeListAdapter.notifyDataSetChanged();
            }
        });
        noCustomizedListViewTitle.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.2
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                if (!InputMethodManagerServiceImpl.this.isExpanded) {
                    indicator.setImageResource(openIndicatorDrawableId);
                    noCustomListView.setVisibility(0);
                    InputMethodManagerServiceImpl.setNoCustomizedInputMethodListViewHeight(noCustomListView, noCustomziedImeListAdapter);
                    InputMethodManagerServiceImpl.this.isExpanded = true;
                    return;
                }
                indicator.setImageResource(closeIndicatorDrawableId);
                noCustomListView.setVisibility(8);
                InputMethodManagerServiceImpl.this.isExpanded = false;
            }
        });
        builder.setView(customView);
        if (isNotContainsCustomizedInputMethod) {
            Slog.v(TAG, "There's no customized ime currently, so customized ime view will hidden");
            int dividerLineId = this.dialogContext.getResources().getIdentifier("dividerLine", "id", MIUIXPACKAGE);
            View dividerLine = customView.findViewById(dividerLineId);
            if (dividerLine != null) {
                noCustomizedListViewTitle.setVisibility(8);
                dividerLine.setVisibility(8);
            }
        }
    }

    public void setDialogImmersive(AlertDialog dialog) {
        if (isPad()) {
            return;
        }
        Window w = dialog.getWindow();
        w.addFlags(-2147481344);
        w.getAttributes().setFitInsetsSides(0);
        final View decorView = w.getDecorView();
        clearFitSystemWindow(decorView);
        decorView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.3
            @Override // android.view.View.OnApplyWindowInsetsListener
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                Insets gestureInsets = insets.getInsets(WindowInsets.Type.mandatorySystemGestures());
                int mHeight = gestureInsets.bottom;
                if (mHeight > 0) {
                    View view = decorView;
                    view.setPaddingRelative(view.getPaddingStart(), decorView.getPaddingTop(), decorView.getPaddingEnd(), decorView.getPaddingBottom() + mHeight);
                }
                decorView.setOnApplyWindowInsetsListener(null);
                return WindowInsets.CONSUMED;
            }
        });
    }

    private static void clearFitSystemWindow(View view) {
        if (view != null) {
            view.setFitsSystemWindows(false);
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    clearFitSystemWindow(((ViewGroup) view).getChildAt(i));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void setNoCustomizedInputMethodListViewHeight(ListView noCustomListView, InputMethodMenuController.ImeSubtypeListAdapter adapter) {
        int height;
        int childrenCount = adapter.getCount();
        View childView = adapter.getView(0, (View) null, noCustomListView);
        childView.measure(0, 0);
        int childHeight = childView.getMeasuredHeight();
        if (childrenCount < 3) {
            height = childHeight * childrenCount;
        } else {
            height = childHeight * 3;
        }
        ViewGroup.LayoutParams layoutParams = noCustomListView.getLayoutParams();
        layoutParams.height = height;
        noCustomListView.setLayoutParams(layoutParams);
    }

    public void updateItemView(View view, CharSequence imeName, CharSequence subtypeName, int position, int checkedItem, ViewGroup parent) {
        int firstTextViewId = this.dialogContext.getResources().getIdentifier("title", "id", MIUIXPACKAGE);
        int secondTextViewId = this.dialogContext.getResources().getIdentifier("summary", "id", MIUIXPACKAGE);
        TextView firstTextView = (TextView) view.findViewById(firstTextViewId);
        TextView secondTextView = (TextView) view.findViewById(secondTextViewId);
        if (TextUtils.isEmpty(subtypeName)) {
            firstTextView.setText(imeName);
            secondTextView.setVisibility(8);
        } else {
            firstTextView.setText(imeName);
            secondTextView.setText(subtypeName);
            secondTextView.setVisibility(0);
        }
        int radioButtonId = this.dialogContext.getResources().getIdentifier("radio", "id", MIUIXPACKAGE);
        RadioButton radioButton = (RadioButton) view.findViewById(radioButtonId);
        radioButton.setChecked(position == checkedItem);
        ((ListView) parent).setChoiceMode(0);
        if (position == checkedItem) {
            view.setActivated(true);
        } else {
            view.setActivated(false);
        }
    }

    public int getNoCustomizedCheckedItem() {
        return this.noCustomizedCheckedItem;
    }

    public InputMethodInfo[] getNoCustomizedIms() {
        return this.mNoCustomizedIms;
    }

    public int[] getNoCustomizedSubtypeIds() {
        return this.mNoCustomizedSubtypeIds;
    }

    public void removeCustomTitle(AlertDialog.Builder dialogBuilder, View switchingDialogTitleView) {
        dialogBuilder.setCustomTitle((View) null);
        dialogBuilder.setTitle(17041675);
    }

    public void dismissWithoutAnimation(AlertDialog alertDialog) {
        try {
            Method method = alertDialog.getClass().getDeclaredMethod("dismissWithoutAnimation", new Class[0]);
            method.invoke(alertDialog, new Object[0]);
        } catch (Exception e) {
            Slog.e(TAG, "Reflect dismissWithoutAnimation exception!");
        }
    }

    public Notification buildImeNotification(Notification.Builder imeSwitcherNotification, Context context) {
        Notification notification = imeSwitcherNotification.build();
        notification.extras.putParcelable("miui.appIcon", Icon.createWithResource(context, 285737498));
        return notification;
    }

    public boolean shouldClearShowForcedFlag(Context context, int uid) {
        String[] packages;
        boolean result = false;
        if (context == null || (packages = context.getPackageManager().getPackagesForUid(uid)) == null || packages.length == 0) {
            return false;
        }
        for (String packageName : packages) {
            result |= "com.miui.home".equals(packageName);
        }
        return result;
    }

    public boolean isCallingBetweenCustomIME(Context context, int uid, String targetPkgName) {
        String[] packages;
        if (!customizedInputMethodList.contains(targetPkgName) || (packages = context.getPackageManager().getPackagesForUid(uid)) == null || packages.length == 0) {
            return false;
        }
        for (String str : packages) {
            if (customizedInputMethodList.contains(targetPkgName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInputMethodWindowInvisibleByRecentTask(int imeWindowVis) {
        if (!this.callStartInputOrWindowGainedFocusFlag) {
            return false;
        }
        this.callStartInputOrWindowGainedFocusFlag = false;
        return imeWindowVis == 0;
    }

    public void setWindowGainedbyRecentTask(boolean inputMethodWindowDisplayed) {
        this.callStartInputOrWindowGainedFocusFlag = inputMethodWindowDisplayed;
    }

    public boolean isInputMethodWindowInvisibleByScreenOn(int imeWindowVis) {
        boolean z = this.mScreenStatus;
        if (z && this.mScreenOffLastTime && imeWindowVis == 0) {
            this.mScreenOffLastTime = false;
            return true;
        }
        if (z) {
            this.mScreenOffLastTime = false;
        }
        return false;
    }

    public void setScreenOff(boolean inputMethodWindowDisplayed) {
        this.mScreenOffLastTime = inputMethodWindowDisplayed;
    }

    public void setScreenStatus(boolean screenStatus) {
        this.mScreenStatus = screenStatus;
    }

    public void updateSessionId(int sessionId) {
        if (synergyOperate()) {
            this.mSessionId = sessionId;
        }
    }

    public void commitTextForSynergy(IRemoteInputConnection inputContext, String text, int newCursorPosition) {
        try {
            Class<?> clazz = inputContext.getClass();
            Method declaredMethod = clazz.getDeclaredMethod("commitTextForSynergy", String.class, Integer.TYPE);
            declaredMethod.invoke(inputContext, text, Integer.valueOf(newCursorPosition));
        } catch (Exception e) {
            Slog.e(TAG, "Reflect commitTextForSynergy exception!");
        }
    }

    public boolean isSupportedCustomizedDialog() {
        return true;
    }

    public void registerObserver(final Context context) {
        this.mObserver = new ContentObserver(Handler.getMain()) { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                Context context2;
                try {
                    if (Settings.Secure.getUriFor(InputMethodManagerServiceImpl.MIRROR_INPUT_STATE).equals(uri) && (context2 = context) != null) {
                        int mirrorInputState = Settings.Secure.getInt(context2.getContentResolver(), InputMethodManagerServiceImpl.MIRROR_INPUT_STATE);
                        InputMethodManagerServiceImpl.this.mMirrorInputState = mirrorInputState != 1;
                    }
                } catch (Exception e) {
                    InputMethodManagerServiceImpl.this.mMirrorInputState = true;
                    Slog.e(InputMethodManagerServiceImpl.TAG, "Exception caused:" + e + ", set mMirrorInputState to true");
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MIRROR_INPUT_STATE), false, this.mObserver, -1);
    }

    public boolean getMirrorInputState() {
        return this.mMirrorInputState;
    }

    public void enableInputMethodMonitor(int highWatermark) {
        if (Build.IS_DEBUGGABLE) {
            InputMethodMonitor.getInstance().enable(highWatermark);
        }
    }

    public void createSession(int userId, IInputMethodSessionCallback.Stub callback) {
        Intent intent;
        ComponentName component;
        if (Build.IS_DEBUGGABLE && (intent = this.mBindingController.getCurIntent()) != null && (component = intent.getComponent()) != null) {
            InputMethodMonitor.getInstance().startCreateSession(userId, component, callback);
        }
    }

    public void onSessionCreated(IInputMethodSessionCallback.Stub callback) {
        if (Build.IS_DEBUGGABLE) {
            InputMethodMonitor.getInstance().finishCreateSession(callback);
        }
    }

    private void registerPointerEventListener() {
        if (this.mContext == null) {
            return;
        }
        if (this.mMiuiInputMethodEventListener == null) {
            this.mMiuiInputMethodEventListener = new MiuiInputMethodEventListener();
        }
        MiuiGestureMonitor.getInstance(this.mContext).registerPointerEventListener(this.mMiuiInputMethodEventListener);
    }

    private void unregisterPointerEventListener() {
        Context context = this.mContext;
        if (context != null && this.mMiuiInputMethodEventListener != null) {
            MiuiGestureMonitor.getInstance(context).unregisterPointerEventListener(this.mMiuiInputMethodEventListener);
            this.mMiuiInputMethodEventListener = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MiuiInputMethodEventListener implements MiuiGestureListener {
        private MiuiInputMethodEventListener() {
        }

        @Override // com.miui.server.input.gesture.MiuiGestureListener
        public void onPointerEvent(MotionEvent motionEvent) {
            if (InputMethodManagerServiceImpl.this.mHandler == null) {
                return;
            }
            final MotionEvent event = motionEvent.copy();
            InputMethodManagerServiceImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl$MiuiInputMethodEventListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InputMethodManagerServiceImpl.MiuiInputMethodEventListener.this.lambda$onPointerEvent$0(event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onPointerEvent$0(MotionEvent event) {
            if (event.getDeviceId() >= 0) {
                if (InputMethodManagerServiceImpl.this.mIsFromRemoteDevice) {
                    Slog.i(InputMethodManagerServiceImpl.TAG, "set mIsFromRemoteDevice as:" + InputMethodManagerServiceImpl.this.mIsFromRemoteDevice + " via Input event");
                }
                InputMethodManagerServiceImpl.this.mIsFromRemoteDevice = false;
                InputMethodManagerServiceImpl.this.mMirrorDisplayId = 0;
            }
            event.recycle();
        }
    }
}
