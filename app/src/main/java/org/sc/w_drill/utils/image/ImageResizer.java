package org.sc.w_drill.utils.image;

import android.graphics.Bitmap;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class ImageResizer
{

    static ImageResizer instance = null;

    static
    {
        instance = new ImageResizer();
    }

    public static final ImageResizer getInstance()
    {
        return instance;
    }

    protected ImageResizer()
    {

    }


    public static final Bitmap resizeBitmap( ImageConstraints constraints, Bitmap bitmap )
    {
        Bitmap bmp = null;
        int width, height;

        if( bitmap.getWidth() <= constraints.maxSize() && bitmap.getHeight() > constraints.maxSize() )
            return bitmap;

        boolean isHorizontalImage = bitmap.getWidth() >= bitmap.getHeight();
        float scale = 1;

        if( isHorizontalImage && ( bitmap.getWidth() > constraints.maxSize() ) )
            scale = ( ( float ) bitmap.getWidth() ) / constraints.maxSize() ;
        else
            scale  = ( ( float ) bitmap.getHeight() ) / constraints.maxSize() ;

        width  = Math.round( bitmap.getWidth()  / scale );
        height = Math.round( bitmap.getHeight() / scale );

        bmp = Bitmap.createScaledBitmap( bitmap, width, height, false );
        return bmp;
    }
}
