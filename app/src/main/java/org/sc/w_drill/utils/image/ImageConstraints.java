package org.sc.w_drill.utils.image;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class ImageConstraints
{

    public static final int maxSize()
    {
        return 300;
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
