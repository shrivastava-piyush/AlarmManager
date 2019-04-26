package com.bytezap.wobble.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class Blur {

    private static final int DEFAULT_BLUR_RADIUS = 20;

    @TargetApi(17)
    public static Bitmap apply(Context context, Bitmap sentBitmap) {
        return apply(context, sentBitmap, DEFAULT_BLUR_RADIUS);
    }

    @TargetApi(17)
    public static Bitmap apply(Context context, Bitmap sentBitmap, int radius) {
        radius = Math.round(radius/4);
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        final RenderScript rs = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(rs, sentBitmap, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);

        // clean up renderscript resources
        rs.destroy();
        input.destroy();
        output.destroy();
        script.destroy();

        return bitmap;
    }
}