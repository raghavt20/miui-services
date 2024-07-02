package com.android.server.inputmethod;

import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.InputMethodHelper;
import java.util.List;
import miui.os.Build;

/* loaded from: classes.dex */
public class MiuiInputMethodStubImpl implements MiuiInputMethodStub {
    private static final String SECURE_INPUT_METHOD_PACKAGE_NAME = "com.miui.securityinputmethod";
    public static final String TAG = "StylusInputMethodSwitcher";
    private int mCallingUid;
    private boolean mInputMethodChangedBySelf;
    private InputMethodManagerService mService;
    private BaseInputMethodSwitcher securityInputMethodSwitcher;
    private BaseInputMethodSwitcher sogouInputMethodSwitcher;
    private BaseInputMethodSwitcher stylusInputMethodSwitcher;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiInputMethodStubImpl> {

        /* compiled from: MiuiInputMethodStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiInputMethodStubImpl INSTANCE = new MiuiInputMethodStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public MiuiInputMethodStubImpl m1648provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public MiuiInputMethodStubImpl m1647provideNewInstance() {
            return new MiuiInputMethodStubImpl();
        }
    }

    public void init(InputMethodManagerService service) {
        this.stylusInputMethodSwitcher = new StylusInputMethodSwitcher();
        this.securityInputMethodSwitcher = new SecurityInputMethodSwitcher();
        this.sogouInputMethodSwitcher = new SogouInputMethodSwitcher();
        this.stylusInputMethodSwitcher.init(service);
        this.securityInputMethodSwitcher.init(service);
        this.sogouInputMethodSwitcher.init(service);
        this.mService = service;
    }

    public void onSystemRunningLocked() {
        this.stylusInputMethodSwitcher.onSystemRunningLocked();
        this.securityInputMethodSwitcher.onSystemRunningLocked();
        this.sogouInputMethodSwitcher.onSystemRunningLocked();
        try {
            this.mCallingUid = this.mService.mContext.getPackageManager().getPackageUid("com.miui.securityinputmethod", 0);
        } catch (Exception e) {
            this.mCallingUid = -1;
        }
    }

    public void onSwitchUserLocked(int newUserId) {
        this.stylusInputMethodSwitcher.onSwitchUserLocked(newUserId);
        this.securityInputMethodSwitcher.onSwitchUserLocked(newUserId);
        this.sogouInputMethodSwitcher.onSwitchUserLocked(newUserId);
    }

    public boolean mayChangeInputMethodLocked(EditorInfo attribute, int startInputReason) {
        return this.securityInputMethodSwitcher.mayChangeInputMethodLocked(attribute, startInputReason) || this.sogouInputMethodSwitcher.mayChangeInputMethodLocked(attribute) || this.stylusInputMethodSwitcher.mayChangeInputMethodLocked(attribute);
    }

    public boolean shouldHideImeSwitcherLocked() {
        return this.stylusInputMethodSwitcher.shouldHideImeSwitcherLocked() || this.sogouInputMethodSwitcher.shouldHideImeSwitcherLocked() || this.securityInputMethodSwitcher.shouldHideImeSwitcherLocked();
    }

    public void removeMethod(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        this.stylusInputMethodSwitcher.removeMethod(imList);
        this.securityInputMethodSwitcher.removeMethod(imList);
        this.sogouInputMethodSwitcher.removeMethod(imList);
    }

    public List<InputMethodInfo> filterMethodLocked(List<InputMethodInfo> methodInfos) {
        return this.sogouInputMethodSwitcher.filterMethodLocked(this.securityInputMethodSwitcher.filterMethodLocked(this.stylusInputMethodSwitcher.filterMethodLocked(methodInfos)));
    }

    public void bootPhase(int phase, Context mContext) {
        if (phase == 1000 && !Build.IS_INTERNATIONAL_BUILD) {
            InputMethodHelper.init(mContext);
        }
    }

    public void setInputMethodChangedBySelf(int callingUid) {
        if (callingUid != -1 && callingUid == this.mCallingUid) {
            this.mInputMethodChangedBySelf = true;
        }
    }

    public void clearInputMethodChangedBySelf() {
        this.mInputMethodChangedBySelf = false;
    }

    public boolean isInputMethodChangedBySelf() {
        return this.mInputMethodChangedBySelf;
    }
}
