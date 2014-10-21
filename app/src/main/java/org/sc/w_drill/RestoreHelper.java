package org.sc.w_drill;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.zip.ZipInputStream;

/**
 * Created by MaxSh on 21.10.2014.
 */
public class RestoreHelper
{

    protected RestoreHelper(){};

    public static final void restore(Context context, String srcFile ) throws FileNotFoundException {
        File zipSrc = new File( srcFile );

        if( !zipSrc.exists())
            throw new FileNotFoundException( srcFile );

        // Clear the cache directory

        File cacheDir = context.getCacheDir();

        File[] files = cacheDir.listFiles();

        for( int i = 0; i < files.length; i++ )
            files[i].delete();

        unzipInCacheDir( zipSrc );

    }

    private static void unzipInCacheDir(File zipSrc) throws FileNotFoundException
    {
        FileInputStream is = new FileInputStream( zipSrc );

        FileOutputStream out = new FileOutputStream(cacheFile);

        byte[] buffer = new byte[1024];
        int read;
        while((read = is.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }

        is.close();
        is = null;
        out.flush();
        out.close();
        out = null;
    }


}
