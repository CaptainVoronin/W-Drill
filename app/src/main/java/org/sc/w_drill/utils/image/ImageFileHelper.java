package org.sc.w_drill.utils.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;

import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IWord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.DataFormatException;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class ImageFileHelper {
    Context context;

    static ImageFileHelper instance = null;

    public static ImageFileHelper getInstance(Context ctx) {
        if (instance == null)
            instance = new ImageFileHelper(ctx);
        return instance;
    }

    protected ImageFileHelper(Context ctx) {
        context = ctx;
    };

    public final void putBitmapInInternalStorage(DictionaryImageFileManager manager, IBaseWord word, Bitmap bitmap) throws IOException
    {
        if( manager.getImageFile( word ) != null )
            manager.deleteImage( word );

        String filename = manager.mkPath( word );

        FileOutputStream fous = new FileOutputStream( new File( filename ) );
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fous);
        fous.flush();
        fous.close();
        Log.d("[ImageFileHelper::putBitmapInInternalStorage", "The file " + filename + " has been put in the storage " );
    }

    public final String putBitmapInCache(Bitmap bitmap) throws IOException
    {
        File cachDir = context.getCacheDir();
        String name = cachDir.getPath() + File.separator + UUID.randomUUID().toString() + ".png";
        File file = new File( name );
        FileOutputStream fous = new FileOutputStream( file );
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fous);
        fous.flush();
        fous.close();
        Log.d("[ImageFileHelper::putBitmapInCache", "The TMP file has been put in the cache " + name );
        return name;
    }

    public Bitmap pickImage(Uri uri) throws FileNotFoundException {
        final InputStream imageStream = context.getContentResolver().openInputStream(uri);
        return BitmapFactory.decodeStream(imageStream);
    }
}