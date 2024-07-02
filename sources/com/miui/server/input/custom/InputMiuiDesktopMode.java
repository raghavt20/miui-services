package com.miui.server.input.custom;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.view.KeyEvent;
import android.view.KeyboardShortcutInfo;
import com.android.server.policy.MiuiInputLog;
import com.android.server.policy.MiuiKeyInterceptExtend;
import com.android.server.wm.MiuiDesktopModeUtils;
import com.miui.server.input.InputConstants;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* loaded from: classes.dex */
public class InputMiuiDesktopMode {
    public static final String SHORTCUT_ALT_ON_TAB = "2+61";
    public static final String SHORTCUT_META_ON_D = "65536+32";
    public static final String SHORTCUT_META_ON_TAB = "65536+61";

    public static boolean launchRecents(Context context, String shortcut) {
        if (MiuiDesktopModeUtils.isActive(context)) {
            if (SHORTCUT_META_ON_TAB.equals(shortcut)) {
                Intent intent = new Intent(InputConstants.IntentAction.MIUI_HOME_SHORTCUT);
                intent.putExtra(InputConstants.KEYEVENT1, 65536);
                intent.putExtra(InputConstants.KEYEVENT2, 61);
                intent.setPackage("com.miui.home");
                context.sendBroadcastAsUser(intent, UserHandle.CURRENT, InputConstants.PERMISSION_INTERNAL_GENERAL_API);
                MiuiInputLog.major("miuidesktopmode launchRecents");
                return true;
            }
            if (SHORTCUT_ALT_ON_TAB.equals(shortcut)) {
                Intent intent2 = new Intent(InputConstants.IntentAction.MIUI_HOME_SHORTCUT);
                intent2.putExtra(InputConstants.KEYEVENT1, 2);
                intent2.putExtra(InputConstants.KEYEVENT2, 61);
                intent2.setPackage("com.miui.home");
                context.sendBroadcastAsUser(intent2, UserHandle.CURRENT, InputConstants.PERMISSION_INTERNAL_GENERAL_API);
                MiuiInputLog.major("miuidesktopmode launchRecents");
                return false;
            }
            return false;
        }
        return false;
    }

    public static boolean launchHome(Context context, String shortcut) {
        if (MiuiDesktopModeUtils.isActive(context) && SHORTCUT_META_ON_D.equals(shortcut)) {
            Intent intent = new Intent(InputConstants.IntentAction.MIUI_HOME_SHORTCUT);
            intent.putExtra(InputConstants.KEYEVENT1, 65536);
            intent.putExtra(InputConstants.KEYEVENT2, 32);
            intent.setPackage("com.miui.home");
            context.sendBroadcastAsUser(intent, UserHandle.CURRENT, InputConstants.PERMISSION_INTERNAL_GENERAL_API);
            MiuiInputLog.major("miuidesktopmode launchHome");
            return true;
        }
        return false;
    }

    public static boolean shouldInterceptKeyboardCombinationRule(Context context, int metaState, int keyCode) {
        if (!MiuiDesktopModeUtils.isActive(context) || metaState != 65536) {
            return false;
        }
        if (keyCode == 22 || keyCode == 21) {
            return true;
        }
        return false;
    }

    public static int getKeyInterceptType(MiuiKeyInterceptExtend.INTERCEPT_STAGE interceptStage, Context context, KeyEvent event) {
        if (MiuiDesktopModeUtils.isActive(context) && event.getKeyCode() == 61 && event.getAction() == 0 && event.getRepeatCount() == 0) {
            int shiftlessModifiers = event.getModifiers() & (-194);
            if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2)) {
                if (interceptStage == MiuiKeyInterceptExtend.INTERCEPT_STAGE.BEFORE_QUEUEING) {
                    launchRecents(context, SHORTCUT_ALT_ON_TAB);
                    return 4;
                }
                return 4;
            }
            return 0;
        }
        return 0;
    }

    public static List<KeyboardShortcutInfo> getMuiDeskModeKeyboardShortcutInfo(Context context, List<KeyboardShortcutInfo> keyboardShortcutInfos) {
        if (MiuiDesktopModeUtils.isActive(context)) {
            return (List) keyboardShortcutInfos.stream().filter(new Predicate() { // from class: com.miui.server.input.custom.InputMiuiDesktopMode$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return InputMiuiDesktopMode.lambda$getMuiDeskModeKeyboardShortcutInfo$0((KeyboardShortcutInfo) obj);
                }
            }).collect(Collectors.toList());
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getMuiDeskModeKeyboardShortcutInfo$0(KeyboardShortcutInfo keyboardShortcutInfo) {
        long keyCode = keyboardShortcutInfo.getShortcutKeyCode();
        if ((281474976710656L & keyCode) != 0) {
            long keyCode2 = keyCode & (-281474976710657L);
            if (keyCode2 == 22 || keyCode2 == 21) {
                return false;
            }
            return true;
        }
        return true;
    }
}
