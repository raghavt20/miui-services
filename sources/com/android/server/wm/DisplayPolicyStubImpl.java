package com.android.server.wm;

import android.graphics.Insets;
import android.os.Binder;
import android.view.DisplayInfo;
import android.view.InsetsFrameProvider;
import android.view.WindowInsets;
import android.view.WindowManager;
import com.android.server.wm.DisplayPolicyStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.whetstone.client.WhetstoneClientManager;

/* loaded from: classes.dex */
public class DisplayPolicyStubImpl implements DisplayPolicyStub {
    private static final String CARWITH_PACKAGE_NAME = "com.miui.carlink";
    private InsetsFrameProvider mInsetsFrameProvider;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayPolicyStubImpl> {

        /* compiled from: DisplayPolicyStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayPolicyStubImpl INSTANCE = new DisplayPolicyStubImpl();
        }

        /* renamed from: provideSingleton, reason: merged with bridge method [inline-methods] */
        public DisplayPolicyStubImpl m2468provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        /* renamed from: provideNewInstance, reason: merged with bridge method [inline-methods] */
        public DisplayPolicyStubImpl m2467provideNewInstance() {
            return new DisplayPolicyStubImpl();
        }
    }

    public int getExtraNavigationBarAppearance(WindowState winCandidate, WindowState navColorWin) {
        int appearance = 0;
        if (navColorWin != null && (navColorWin.getAttrs().extraFlags & 1048576) == 1048576) {
            appearance = 0 | 16;
        }
        if (winCandidate != null && (winCandidate.getAttrs().extraFlags & 32768) == 32768) {
            return appearance | 2048;
        }
        return appearance;
    }

    public boolean isMiuiVersion() {
        return true;
    }

    public void notifyOnScroll(boolean start) {
        WhetstoneClientManager.notifyOnScroll(start);
    }

    public boolean isCarWithDisplay(DisplayContent displayContent) {
        return CARWITH_PACKAGE_NAME.equals(getDisplayName(displayContent));
    }

    public void setCarWithWindowContainer(DisplayContent displayContent, WindowState win, DisplayPolicyStub.InsertActionInterface insertActionInterface) {
        if (this.mInsetsFrameProvider == null) {
            this.mInsetsFrameProvider = createCarwithInsetsFrameProvider(displayContent, win.mAttrs);
        }
        if (insertActionInterface != null) {
            insertActionInterface.insertAction(this.mInsetsFrameProvider.getId());
        }
    }

    public InsetsFrameProvider createCarwithInsetsFrameProvider(DisplayContent displayContent, WindowManager.LayoutParams attrs) {
        Binder mInsetsSourceOwner = new Binder();
        this.mInsetsFrameProvider = new InsetsFrameProvider(mInsetsSourceOwner, 0, WindowInsets.Type.navigationBars());
        DisplayInfo di = displayContent.getDisplayInfo();
        if (di.logicalWidth > di.logicalHeight) {
            this.mInsetsFrameProvider.setInsetsSize(Insets.of(attrs.width, 0, 0, 0));
        } else {
            this.mInsetsFrameProvider.setInsetsSize(Insets.of(0, 0, 0, attrs.height));
        }
        return this.mInsetsFrameProvider;
    }

    private String getDisplayName(DisplayContent displayContent) {
        return displayContent.mDisplayInfo.name;
    }
}
