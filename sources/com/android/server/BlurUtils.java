package com.android.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import com.android.server.wm.MiuiMultiWindowRecommendController;
import java.lang.reflect.Array;

/* loaded from: classes.dex */
public class BlurUtils {
    public static Bitmap addBlackBoard(Bitmap bmp, int color) {
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        Bitmap newBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        canvas.drawBitmap(bmp, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, MiuiMultiWindowRecommendController.MULTI_WINDOW_RECOMMEND_SHADOW_V2_OFFSET_X, paint);
        canvas.drawColor(color);
        return newBitmap;
    }

    public static Bitmap blurImage(Context context, Bitmap input, float radius) {
        Bitmap tempInput = Bitmap.createScaledBitmap(input, input.getWidth() / 4, input.getHeight() / 4, false);
        Bitmap result = tempInput.copy(tempInput.getConfig(), true);
        RenderScript rsScript = RenderScript.create(context);
        if (rsScript == null) {
            return null;
        }
        Allocation alloc = Allocation.createFromBitmap(rsScript, tempInput, Allocation.MipmapControl.MIPMAP_NONE, 1);
        Allocation outAlloc = Allocation.createTyped(rsScript, alloc.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
        blur.setRadius(radius);
        blur.setInput(alloc);
        blur.forEach(outAlloc);
        outAlloc.copyTo(result);
        rsScript.destroy();
        return Bitmap.createScaledBitmap(result, input.getWidth(), input.getHeight(), false);
    }

    public static Bitmap stackBlur(Bitmap sentBitmap, int radius) {
        Bitmap bitmap;
        int i;
        int i2;
        int p = radius;
        Bitmap bitmap2 = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (p < 1) {
            return null;
        }
        int w = bitmap2.getWidth();
        int h = bitmap2.getHeight();
        int[] pix = new int[w * h];
        String str = " ";
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap2.getPixels(pix, 0, w, 0, 0, w, h);
        int wm = w - 1;
        int rbs = h - 1;
        int wh = w * h;
        int rbs2 = p + p + 1;
        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int[] vmin = new int[Math.max(w, h)];
        int divsum = (rbs2 + 1) >> 1;
        int wh2 = divsum * divsum;
        int[] dv = new int[wh2 * 256];
        int i3 = 0;
        while (true) {
            bitmap = bitmap2;
            if (i3 >= wh2 * 256) {
                break;
            }
            dv[i3] = i3 / wh2;
            i3++;
            bitmap2 = bitmap;
        }
        int yi = 0;
        int yw = 0;
        int[][] stack = (int[][]) Array.newInstance((Class<?>) Integer.TYPE, rbs2, 3);
        int r1 = p + 1;
        int divsum2 = 0;
        while (divsum2 < h) {
            int i4 = 0;
            int rsum = 0;
            int boutsum = 0;
            int goutsum = 0;
            int routsum = 0;
            int binsum = 0;
            int ginsum = 0;
            int rinsum = 0;
            String str2 = str;
            int i5 = 0;
            int gsum = h;
            int h2 = -p;
            int bsum = 0;
            while (h2 <= p) {
                int hm = rbs;
                int hm2 = i4;
                int[] vmin2 = vmin;
                int p2 = pix[yi + Math.min(wm, Math.max(h2, hm2))];
                int[] sir = stack[h2 + p];
                sir[hm2] = (p2 & 16711680) >> 16;
                sir[1] = (p2 & 65280) >> 8;
                sir[2] = p2 & 255;
                int rbs3 = r1 - Math.abs(h2);
                rsum += sir[0] * rbs3;
                i5 += sir[1] * rbs3;
                bsum += sir[2] * rbs3;
                if (h2 > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                h2++;
                vmin = vmin2;
                rbs = hm;
                i4 = 0;
            }
            int[] vmin3 = vmin;
            int hm3 = rbs;
            int stackpointer = radius;
            int x = 0;
            while (x < w) {
                r[yi] = dv[rsum];
                g[yi] = dv[i5];
                b[yi] = dv[bsum];
                int rsum2 = rsum - routsum;
                int gsum2 = i5 - goutsum;
                int bsum2 = bsum - boutsum;
                int stackstart = (stackpointer - p) + rbs2;
                int[] sir2 = stack[stackstart % rbs2];
                int routsum2 = routsum - sir2[0];
                int goutsum2 = goutsum - sir2[1];
                int boutsum2 = boutsum - sir2[2];
                if (divsum2 != 0) {
                    i2 = h2;
                } else {
                    i2 = h2;
                    int i6 = x + p + 1;
                    vmin3[x] = Math.min(i6, wm);
                }
                int i7 = vmin3[x];
                int p3 = pix[yw + i7];
                sir2[0] = (p3 & 16711680) >> 16;
                sir2[1] = (p3 & 65280) >> 8;
                int wm2 = wm;
                int wm3 = p3 & 255;
                sir2[2] = wm3;
                int rinsum2 = rinsum + sir2[0];
                int ginsum2 = ginsum + sir2[1];
                int binsum2 = binsum + sir2[2];
                rsum = rsum2 + rinsum2;
                i5 = gsum2 + ginsum2;
                bsum = bsum2 + binsum2;
                stackpointer = (stackpointer + 1) % rbs2;
                int[] sir3 = stack[stackpointer % rbs2];
                routsum = routsum2 + sir3[0];
                goutsum = goutsum2 + sir3[1];
                boutsum = boutsum2 + sir3[2];
                rinsum = rinsum2 - sir3[0];
                ginsum = ginsum2 - sir3[1];
                binsum = binsum2 - sir3[2];
                yi++;
                x++;
                wm = wm2;
                h2 = i2;
            }
            yw += w;
            divsum2++;
            vmin = vmin3;
            h = gsum;
            str = str2;
            rbs = hm3;
        }
        int[] vmin4 = vmin;
        int hm4 = rbs;
        int stackstart2 = h;
        String str3 = str;
        int x2 = 0;
        int h3 = divsum2;
        while (x2 < w) {
            int hm5 = 0;
            int gsum3 = 0;
            int rsum3 = 0;
            int yp = (-p) * w;
            int yp2 = -p;
            int i8 = 0;
            int y = yp2;
            int yp3 = yp;
            int rinsum3 = 0;
            int ginsum3 = 0;
            int binsum3 = 0;
            int binsum4 = 0;
            int routsum3 = 0;
            while (y <= p) {
                int div = rbs2;
                int yi2 = Math.max(0, yp3) + x2;
                int[] sir4 = stack[y + p];
                sir4[0] = r[yi2];
                sir4[1] = g[yi2];
                sir4[2] = b[yi2];
                int rbs4 = r1 - Math.abs(y);
                rsum3 += r[yi2] * rbs4;
                gsum3 += g[yi2] * rbs4;
                int bsum3 = hm5 + (b[yi2] * rbs4);
                if (y > 0) {
                    rinsum3 += sir4[0];
                    ginsum3 += sir4[1];
                    binsum3 += sir4[2];
                } else {
                    binsum4 += sir4[0];
                    routsum3 += sir4[1];
                    i8 += sir4[2];
                }
                int bsum4 = hm4;
                if (y < bsum4) {
                    yp3 += w;
                }
                y++;
                hm4 = bsum4;
                rbs2 = div;
                hm5 = bsum3;
            }
            int div2 = rbs2;
            int bsum5 = hm5;
            int bsum6 = hm4;
            int stackpointer2 = radius;
            int i9 = i8;
            int boutsum3 = x2;
            int yi3 = 0;
            int y2 = i9;
            while (true) {
                int i10 = y;
                i = stackstart2;
                if (yi3 < i) {
                    pix[boutsum3] = (pix[boutsum3] & (-16777216)) | (dv[rsum3] << 16) | (dv[gsum3] << 8) | dv[bsum5];
                    int rsum4 = rsum3 - binsum4;
                    int gsum4 = gsum3 - routsum3;
                    int bsum7 = bsum5 - y2;
                    int stackstart3 = (stackpointer2 - p) + div2;
                    int[] sir5 = stack[stackstart3 % div2];
                    int routsum4 = binsum4 - sir5[0];
                    int goutsum3 = routsum3 - sir5[1];
                    int boutsum4 = y2 - sir5[2];
                    if (x2 == 0) {
                        vmin4[yi3] = Math.min(yi3 + r1, bsum6) * w;
                    }
                    int p4 = vmin4[yi3] + x2;
                    sir5[0] = r[p4];
                    sir5[1] = g[p4];
                    sir5[2] = b[p4];
                    int rinsum4 = rinsum3 + sir5[0];
                    int ginsum4 = ginsum3 + sir5[1];
                    int binsum5 = binsum3 + sir5[2];
                    rsum3 = rsum4 + rinsum4;
                    gsum3 = gsum4 + ginsum4;
                    bsum5 = bsum7 + binsum5;
                    stackpointer2 = (stackpointer2 + 1) % div2;
                    int[] sir6 = stack[stackpointer2];
                    binsum4 = routsum4 + sir6[0];
                    routsum3 = goutsum3 + sir6[1];
                    y2 = boutsum4 + sir6[2];
                    rinsum3 = rinsum4 - sir6[0];
                    ginsum3 = ginsum4 - sir6[1];
                    binsum3 = binsum5 - sir6[2];
                    boutsum3 += w;
                    yi3++;
                    p = radius;
                    stackstart2 = i;
                    y = i10;
                }
            }
            x2++;
            p = radius;
            hm4 = bsum6;
            stackstart2 = i;
            h3 = yi3;
            rbs2 = div2;
        }
        int y3 = stackstart2;
        Log.e("pix", w + str3 + y3 + str3 + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, y3);
        return bitmap;
    }
}
