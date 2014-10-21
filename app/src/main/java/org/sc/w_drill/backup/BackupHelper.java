package org.sc.w_drill.backup;

import android.content.Context;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by Max on 10/20/2014.
 */
public class BackupHelper
{

    static final String innerFileName = "dictionary.xml";

    protected BackupHelper()
    {}

    public static final void backup(Context context, String destdir, Dictionary dict) throws IOException
    {
        File dir = new File( destdir );

        if( !dir.exists())
            dir.mkdirs();

        File cacheDir = context.getCacheDir();

        WDdb database = new WDdb( context );
        StringBuilder buff = new StringBuilder();

        DictToXML.toXML( database, buff, dict );

        File tmpFile = new File ( cacheDir.getPath() + File.separator + innerFileName);

        writeToTmpFile( tmpFile, buff );

        String destFilename = dict.getName() + ".zip";

        zipTmpFileAndCopyToDest( tmpFile, new File( destdir + File.separator + destFilename ), innerFileName  );

        tmpFile.delete();
    }

    private static void zipTmpFileAndCopyToDest(File tmpFile,  File destFile, String zipEntryName ) throws IOException
    {
        byte[] buffer = new byte[1024];

        FileOutputStream fos = new FileOutputStream( destFile );

        ZipOutputStream zos = new ZipOutputStream(fos);

        ZipEntry ze= new ZipEntry( zipEntryName );

        zos.putNextEntry(ze);

        FileInputStream in = new FileInputStream( tmpFile );

        int len;
        while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }


        zos.closeEntry();
        in.close();

        //remember close it
        zos.close();
    }

    private static void writeToTmpFile(File tmpFile, StringBuilder buff) throws IOException
    {
        FileWriter fw = new FileWriter( tmpFile );
        fw.write( buff.toString() );
        fw.close();
        fw = null;
    }
}
