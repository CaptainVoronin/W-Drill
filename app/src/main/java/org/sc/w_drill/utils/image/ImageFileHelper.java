package org.sc.w_drill.utils.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;

import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IWord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.UUID;
import java.util.zip.DataFormatException;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class ImageFileHelper {
    Context context;
    static final Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
    static final int compressQuality = 80;

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
        bitmap.compress(format, compressQuality, fous);
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
        bitmap.compress( format, compressQuality, fous);
        fous.flush();
        fous.close();
        file = new File( name );

        Log.d("[ImageFileHelper::putBitmapInCache", "The TMP file (size= " + file.length() + " )has been put in the cache " + name );
        return name;
    }

    public Bitmap pickImage(Uri uri) throws FileNotFoundException {
        final InputStream imageStream = context.getContentResolver().openInputStream(uri);
        return BitmapFactory.decodeStream(imageStream);
    }

    /**
     * Thanks to Mr_and_Mrs_D from Stackoverflow
     * @param file
     * @return
     * @throws IOException
     */
    public static String imageFileToBASE64( File file ) throws IOException
    {
        String res = null;
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {

            int length = ( int ) f.length();
            byte[] data = new byte[length];
            f.readFully(data);
            res =  Base64.encodeToString( data, 0 );
        } finally {
            f.close();
            return res;
        }
    }

    public static void imageFileFromBASE64( String path, String base64 ) throws IOException
    {
        byte[] buff = Base64.decode( base64, 0 );
        File file = new File( path );

        if( file.exists() )
            if( !file.delete() )
                throw new IOException( "Can't delete file " + path );

        FileOutputStream fw = new FileOutputStream( file );
        fw.write( buff );
        fw.flush();
        fw.close();
    }
}