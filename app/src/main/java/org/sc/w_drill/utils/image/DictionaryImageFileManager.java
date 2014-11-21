package org.sc.w_drill.utils.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IWord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by MaxSh on 27.10.2014.
 */
public class DictionaryImageFileManager
{
    Context context;
    Dictionary dict;

    public DictionaryImageFileManager(Context ctx, Dictionary _dict)
    {
        context = ctx;
        dict = _dict;
    }

    public File getDir()
    {
        return mkDictPath(dict);
    }

    public void checkDir() throws FileNotFoundException
    {
        File dir = getDir();
        if (!dir.exists())
            if (!dir.mkdirs())
                throw new FileNotFoundException(dir.getPath());
    }

    public File[] getFiles()
    {
        return getDir().listFiles();
    }

    public void deleteImage(IBaseWord word) throws FileNotFoundException
    {
        File imageFile = getImageFile(word);

        if (imageFile == null)
            return;

        if (!imageFile.delete())
            throw new FileNotFoundException(imageFile.getPath());
        else
            Log.d("[DictionaryImageFileManager::deleteImage", "File " + imageFile.getPath() + " has been deleted");

    }

    public String mkPath(IBaseWord word)
    {
        return getDir().getPath() + File.separator + mkFilename(word);
    }

    public File getImageFile(IBaseWord word)
    {
        File imageFile = new File(mkPath(word));
        if (!imageFile.exists())
            return null;
        return imageFile;
    }

    public Bitmap getImageBitmap(IBaseWord word) throws FileNotFoundException
    {
        File file = getImageFile(word);
        return BitmapFactory.decodeFile(file.getPath());
    }

    public void deleteDictDir() throws FileNotFoundException
    {
        File dir = getDir();
        File[] files = getFiles();

        for (int i = 0; i < files.length; i++)
            if (!files[i].delete())
                throw new FileNotFoundException(files[i].getPath());

        if (!dir.delete())
            throw new FileNotFoundException(dir.getPath());
    }

    public void moveImageFromCache(IWord activeWord, String cachedImageFilename) throws IOException
    {
        byte[] buff = new byte[1024];
        String path = mkPath(activeWord);
        FileOutputStream fous = new FileOutputStream(path);
        FileInputStream fin = new FileInputStream(cachedImageFilename);

        while (fin.read(buff) > 0)
            fous.write(buff);

        fous.flush();
        fous.close();
        fin.close();

        File f = new File(cachedImageFilename);
        if (f.delete())
            Log.d("[DictionaryImageFileManager::moveImageFromCache", "The cache file " + cachedImageFilename + " has been deleted");
        else
            Log.d("[DictionaryImageFileManager::moveImageFromCache", "The cache file " + cachedImageFilename + " has not been deleted");
    }

    public void moveImageToDict(IBaseWord word, Dictionary dstDict) throws IOException
    {
        String imagePath = mkPath(word);
        String targetPath = mkDictPath(dstDict) + File.separator + mkFilename(word);
        moveFile(imagePath, targetPath);
    }

    private String mkFilename(IBaseWord word)
    {
        return word.getUUID() + ".png";
    }

    private File mkDictPath(Dictionary dictionary)
    {
        return context.getDir(dictionary.getUUID(), Context.MODE_PRIVATE);
    }

    private void moveFile(String srcFile, String dstFile) throws IOException
    {
        byte[] buff = new byte[1024];
        FileOutputStream fous = new FileOutputStream(dstFile);
        FileInputStream fin = new FileInputStream(srcFile);

        while (fin.read(buff) > 0)
            fous.write(buff);

        fous.flush();
        fous.close();
        fin.close();

        File f = new File(srcFile);
        if (f.delete())
            Log.d("[DictionaryImageFileManager::moveFile", "The src file " + srcFile + " has been deleted");
        else
            Log.d("[DictionaryImageFileManager::moveFile", "The src file " + srcFile + " has not been deleted");

    }

    public long getTotalFileSize()
    {
        long size = 0;
        File[] files = getFiles();

        for (int i = 0; i < files.length; i++)
        {
            size += files[i].length();
        }

        return size;
    }

}