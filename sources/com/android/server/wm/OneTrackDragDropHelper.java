package com.android.server.wm;

import android.app.ActivityThread;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Slog;
import android.webkit.MimeTypeMap;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import java.io.File;
import java.util.ArrayList;
import miui.os.Build;

/* loaded from: classes.dex */
public class OneTrackDragDropHelper {
    private static final String APP_ID = "31000000538";
    private static final String EVENT_NAME = "content_drag";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final int MSG_DRAG_DROP = 0;
    private static final String ONETRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String PACKAGE = "android";
    private static final String TAG = OneTrackDragDropHelper.class.getSimpleName();
    private static OneTrackDragDropHelper sInstance;
    private final Context mContext = ActivityThread.currentApplication();
    private final Handler mHandler = new Handler(MiuiBgThread.get().getLooper()) { // from class: com.android.server.wm.OneTrackDragDropHelper.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    DragDropData dragDropData = ((DragDropData.Builder) msg.obj).build();
                    OneTrackDragDropHelper.this.reportOneTrack(dragDropData);
                    return;
                default:
                    return;
            }
        }
    };

    public static synchronized OneTrackDragDropHelper getInstance() {
        OneTrackDragDropHelper oneTrackDragDropHelper;
        synchronized (OneTrackDragDropHelper.class) {
            if (sInstance == null) {
                sInstance = new OneTrackDragDropHelper();
            }
            oneTrackDragDropHelper = sInstance;
        }
        return oneTrackDragDropHelper;
    }

    private OneTrackDragDropHelper() {
    }

    public void notifyDragDropResult(WindowState dragWindow, WindowState dropWindow, boolean result, ClipData clipData) {
        DragDropData.Builder dragDropDataBuilder = new DragDropData.Builder(dragWindow, dropWindow, result, clipData, this.mContext.getContentResolver());
        this.mHandler.obtainMessage(0, dragDropDataBuilder).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportOneTrack(DragDropData dragDropData) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
        intent.putExtra("send_package_name", dragDropData.mDragPackage);
        intent.putExtra("target_package_name", dragDropData.mDropPackage);
        intent.putExtra("drag_result", dragDropData.mResult);
        intent.putExtra("content_style", dragDropData.mContentStyle);
        if (!Build.IS_INTERNATIONAL_BUILD) {
            intent.setFlags(3);
        }
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "Upload DragDropData exception! " + dragDropData, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DragDropData {
        public final String mContentStyle;
        public final String mDragPackage;
        public final String mDropPackage;
        public final String mResult;

        public DragDropData(String dragPackage, String dropPackage, String result, String contentStyle) {
            this.mDragPackage = dragPackage;
            this.mDropPackage = dropPackage;
            this.mResult = result;
            this.mContentStyle = contentStyle;
        }

        public String toString() {
            return "DragDropData{dragPackage='" + this.mDragPackage + "', dropPackage='" + this.mDropPackage + "', result='" + this.mResult + "', contentStyle='" + this.mContentStyle + "'}";
        }

        /* loaded from: classes.dex */
        public static class Builder {
            private final ClipData mClipData;
            private final ContentResolver mContentResolver;
            private final WindowState mDragWindow;
            private final WindowState mDropWindow;
            private final boolean mResult;

            Builder(WindowState dragWindow, WindowState dropWindow, boolean result, ClipData clipData, ContentResolver contentResolver) {
                this.mDragWindow = dragWindow;
                this.mDropWindow = dropWindow;
                this.mResult = result;
                this.mClipData = clipData;
                this.mContentResolver = contentResolver;
            }

            public DragDropData build() {
                String dropPackage = "";
                String dragPackage = this.mDragWindow.getOwningPackage() == null ? "" : this.mDragWindow.getOwningPackage();
                WindowState windowState = this.mDropWindow;
                if (windowState != null && windowState.getOwningPackage() != null) {
                    dropPackage = this.mDropWindow.getOwningPackage();
                }
                String result = this.mResult ? "成功" : "失败";
                boolean hasText = false;
                boolean hasImage = false;
                boolean hasFile = false;
                ClipData clipData = this.mClipData;
                int count = clipData == null ? 0 : clipData.getItemCount();
                for (int i = 0; i < count && (!hasText || !hasImage || !hasFile); i++) {
                    ClipData.Item item = this.mClipData.getItemAt(i);
                    if (item.getText() != null || item.getHtmlText() != null) {
                        hasText = true;
                    }
                    Uri uri = item.getUri();
                    if (uri != null) {
                        String mimeType = null;
                        if ("content".equals(uri.getScheme())) {
                            mimeType = this.mContentResolver.getType(uri);
                        } else if ("file".equals(uri.getScheme())) {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString()));
                        }
                        if (mimeType != null) {
                            if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
                                hasImage = true;
                            } else {
                                hasFile = true;
                            }
                        }
                    }
                }
                ClipData clipData2 = this.mClipData;
                int count2 = clipData2 == null ? 0 : clipData2.getDescription().getMimeTypeCount();
                for (int i2 = 0; i2 < count2 && (!hasText || !hasImage); i2++) {
                    String mimeType2 = this.mClipData.getDescription().getMimeType(i2);
                    if (ClipDescription.compareMimeTypes(mimeType2, "text/*")) {
                        hasText = true;
                    } else if (ClipDescription.compareMimeTypes(mimeType2, "image/*")) {
                        hasImage = true;
                    }
                }
                ArrayList<String> contentStyles = new ArrayList<>();
                if (hasText) {
                    contentStyles.add("文字");
                }
                if (hasImage) {
                    contentStyles.add("图片");
                }
                if (hasFile) {
                    contentStyles.add("文件");
                }
                String contentStyle = String.join(",", contentStyles);
                return new DragDropData(dragPackage, dropPackage, result, contentStyle);
            }
        }
    }
}
