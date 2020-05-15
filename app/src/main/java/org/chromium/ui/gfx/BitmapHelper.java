//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.chromium.ui.gfx;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;

@JNINamespace("gfx")
public class BitmapHelper {
    public BitmapHelper() {
    }

    @CalledByNative
    private static Bitmap createBitmap(int width, int height, int bitmapFormatValue) {
        Config bitmapConfig = getBitmapConfigForFormat(bitmapFormatValue);
        if (width == 0 || height == 0) {
            return Bitmap.createBitmap(1, 1, bitmapConfig);
        }
        return Bitmap.createBitmap(width, height, bitmapConfig);
    }

    @CalledByNative
    private static int getBitmapFormatForConfig(Config bitmapConfig) {
        switch(bitmapConfig) {
            case ALPHA_8:
                return 1;
            case ARGB_4444:
                return 2;
            case ARGB_8888:
                return 3;
            case RGB_565:
                return 4;
            default:
                return 0;
        }
    }

    private static Config getBitmapConfigForFormat(int bitmapFormatValue) {
        switch(bitmapFormatValue) {
            case 1:
                return Config.ALPHA_8;
            case 2:
                return Config.ARGB_4444;
            case 3:
            default:
                return Config.ARGB_8888;
            case 4:
                return Config.RGB_565;
        }
    }

    @CalledByNative
    private static int getByteCount(Bitmap bitmap) {
        return bitmap.getByteCount();
    }
}
