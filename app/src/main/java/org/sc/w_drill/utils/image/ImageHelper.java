package org.sc.w_drill.utils.image;

import android.graphics.Bitmap;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class ImageHelper
{

    static ImageHelper instance = null;

    static
    {
        instance = new ImageHelper();
    }

    public static final ImageHelper getInstance()
    {
        return instance;
    }

    protected ImageHelper()
    {

    }

    public static final Bitmap resizeBitmapForShow(ImageConstraints constraints, Bitmap bitmap)
    {
        return resizeBitmap(constraints.maxDisplaySize(), bitmap);
    }

    public static final Bitmap resizeBitmapForStorage(ImageConstraints constraints, Bitmap bitmap)
    {
        return resizeBitmap(constraints.storageSize(), bitmap);
    }

    protected static final Bitmap resizeBitmap(int limit, Bitmap bitmap)
    {
        Bitmap bmp = null;
        int width, height;

        if (bitmap.getWidth() <= limit && bitmap.getHeight() <= limit)
            return bitmap;

        boolean isHorizontalImage = bitmap.getWidth() >= bitmap.getHeight();
        float scale = 1;

        if (isHorizontalImage && (bitmap.getWidth() > limit))
            scale = ((float) bitmap.getWidth()) / limit;
        else
            scale = ((float) bitmap.getHeight()) / limit;

        width = Math.round(bitmap.getWidth() / scale);
        height = Math.round(bitmap.getHeight() / scale);

        bmp = Bitmap.createScaledBitmap(bitmap, width, height, false);
        return bmp;
    }
}
