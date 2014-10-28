package org.sc.w_drill.utils.image;

import android.app.Application;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.sc.w_drill.dict.Dictionary;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class ImageConstraints
{

    Context context;
    int maxSize = 300;
    int storageSize = 800;
    float phisWidth, phisHeight;

    static ImageConstraints instance = null;


    public static ImageConstraints getInstance( Context _context )
    {
        if( instance == null ) {
            instance = new ImageConstraints(_context);
        }

        return instance;
    }

    private void init()
    {
        WindowManager wm = ( WindowManager ) context.getSystemService(Application.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        d.getMetrics( dm  );

        // Define size in inches
        phisWidth = dm.widthPixels / dm.xdpi;
        phisHeight = dm.heightPixels / dm.ydpi;
        float minDim = phisWidth > phisHeight ? phisHeight : phisWidth;

        if( minDim > 2.5 )
            minDim /= 2;
        else
            minDim = 1.6f;
        // TODO: I'm not certain about size for small screens
        maxSize = Math.round( minDim * dm.xdpi );
    }

    protected ImageConstraints( Context _context )
    {
        context = _context;
        init();
    }

    public final int maxDisplaySize()
    {
        return maxSize;
    }

    public final int storageSize()
    {
        return storageSize;
    }

    public static boolean isFileAcceptable( String src )
    {
        int pos = src.lastIndexOf( "." );
        String ext = src.substring( pos );

        if( ext.equalsIgnoreCase("jpg" ) ||
            ext.equalsIgnoreCase("jpeg" )  ||
            ext.equalsIgnoreCase("png" )  ||
            ext.equalsIgnoreCase("bmp" )  ||
            ext.equalsIgnoreCase("gif" ) )
        return true;
        else
            return false;
    }


}
