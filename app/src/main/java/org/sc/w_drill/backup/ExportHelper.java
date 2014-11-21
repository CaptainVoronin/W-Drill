package org.sc.w_drill.backup;

import android.content.Context;
import android.os.Handler;

import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.DBPair;
import org.sc.w_drill.utils.datetime.DateTimeUtils;
import org.sc.w_drill.utils.image.DictionaryImageFileManager;
import org.sc.w_drill.utils.image.ImageFileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Max on 10/20/2014.
 */
public class ExportHelper
{

    public final static String PREF_EXPORT_IMAGES = "PREF_EXPORT_IMAGES";
    public final static String PREF_EXPORT_STATS = "PREF_EXPORT_STATS";
    public final static String PREF_IMPORT_STATS = "PREF_IMPORT_STATS";
    public final static String PREF_IMPORT_IMAGES = "PREF_IMPORT_IMAGES";

    static final String innerFileName = "dictionary.xml";

    boolean bExportImages;
    boolean bExportStats;
    Context context;
    String destdir;
    Dictionary dict;
    ExportProgressListener listener;
    Handler handler;

    public ExportHelper(Context _context, String _destdir,
                        Dictionary _dict, boolean _bExportImages,
                        boolean _bExportStats,
                        ExportProgressListener _listener, Handler _handler)
    {
        bExportImages = _bExportImages;
        bExportStats = _bExportStats;
        context = _context;
        destdir = _destdir;
        dict = _dict;
        listener = _listener;
        handler = _handler;
    }

    public final void backup() throws IOException
    {
        File dir = new File(destdir);

        if (!dir.exists())
            dir.mkdirs();

        File cacheDir = context.getCacheDir();

        StringBuilder buff = new StringBuilder();

        DictionaryImageFileManager dictManager = new DictionaryImageFileManager(context, dict);

        dictionaryToXML(dictManager, buff, dict);

        File tmpFile = new File(cacheDir.getPath() + File.separator + innerFileName);

        writeToTmpFile(tmpFile, buff);

        String destFilename = dict.getName() + ".zip";

        zipTmpFileAndCopyToDest(tmpFile, new File(destdir + File.separator + destFilename), innerFileName);

        tmpFile.delete();
    }

    private void zipTmpFileAndCopyToDest(File tmpFile, File destFile, String zipEntryName) throws IOException
    {
        byte[] buffer = new byte[1024];

        FileOutputStream fos = new FileOutputStream(destFile);

        ZipOutputStream zos = new ZipOutputStream(fos);

        ZipEntry ze = new ZipEntry(zipEntryName);

        zos.putNextEntry(ze);

        FileInputStream in = new FileInputStream(tmpFile);

        int len;
        while ((len = in.read(buffer)) > 0)
        {
            zos.write(buffer, 0, len);
        }

        zos.closeEntry();
        in.close();
        zos.close();
    }

    private void writeToTmpFile(File tmpFile, StringBuilder buff) throws IOException
    {
        FileWriter fw = new FileWriter(tmpFile);
        fw.write(buff.toString());
        fw.close();
        fw = null;
    }

    public void dictionaryToXML(DictionaryImageFileManager dictManager, StringBuilder buff, Dictionary dict)
    {
        buff.setLength(0);

        buff.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        buff.append("<dictionary uuid=\"")
                .append(dict.getUUID())
                .append("\" lang=\"")
                .append(dict.getLang())
                .append("\">\n");
        buff.append("\t<name>").append(dict.getName()).append("</name>\n");
        ArrayList<DBPair> ids = DBWordFactory.getInstance(context, dict).technicalGetWordUnique();

        if (listener != null)
            listener.setMaxValue(ids.size());

        buff.append("\t<content count=\"").append(ids.size()).append("\">\n");

        int count = 0;
        for (DBPair pair : ids)
        {
            IWord word = DBWordFactory.getInstance(context, dict).getWordEx(pair.getId());
            wordToXML(dictManager, buff, word, pair.getValue());
            if (listener != null)
            {
                count++;
                listener.setCurrentProgress(count);
            }
        }

        buff.append("\t</content>\n");
        buff.append("</dictionary>");
    }

    public final void wordToXML(DictionaryImageFileManager dictManager, StringBuilder buff, IWord word, String uuid)
    {
        buff.append("\t\t\t<word uuid=\"").append(uuid).append("\" ");

        if (bExportStats)
        {
            buff.append("state=\"").append(word.getLearnState() == IBaseWord.LearnState.learn ? 0 : 1).append("\" ");
            buff.append("percent=\"").append(word.getLearnPercent()).append("\"");
            buff.append(" updated=\"").append(DateTimeUtils.getDateTimeString(word.getLastUpdate(), true)).append("\"");
            if (word.getLastAccess() != null)
                buff.append(" accessed=\"").append(DateTimeUtils.getDateTimeString(word.getLastAccess(), true)).append("\"");
        }

        buff.append(">\n");

        buff.append("\t\t\t\t<value>").append(word.getWord()).append("</value>\n");

        if (word.getTranscription() != null && word.getTranscription().length() != 0)
            buff.append("\t\t\t\t<transcription>").append(word.getTranscription()).append("</transcription>\n");

        if (bExportImages)
        {
            File file = dictManager.getImageFile(word);

            if (file != null)
            {
                try
                {
                    String base64 = ImageFileHelper.imageFileToBASE64(file);
                    buff.append("\t\t\t\t<image>\n");
                    buff.append("\t\t\t\t\t<![CDATA[");
                    buff.append(base64);
                    buff.append("]]>\n");
                    buff.append("\t\t\t\t</image>\n");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    // TODO: What must to be here?
                }
            }
        }

        for (IMeaning m : word.meanings())
        {
            meaningToXML(buff, m);
        }
        buff.append("\t\t\t</word>\n");

    }

    final void meaningToXML(StringBuilder buff, IMeaning meaning)
    {
        buff.append("\t\t\t\t\t<meaning pos=\"").append(meaning.partOFSpeech()).append("\" ");
        buff.append("formal=\"").append(meaning.isFormal()).append("\" ");
        buff.append("rude=\"").append(meaning.isRude()).append("\" ");
        buff.append("disapproving=\"").append(meaning.isDisapproving()).append("\">\n");
        buff.append("\t\t\t\t\t\t<value>");
        buff.append(meaning.meaning());
        buff.append("</value>\n");
        if (meaning.examples() != null && meaning.examples().size() != 0)
        {
            for (DBPair ex : meaning.examples())
                exampleToXML(buff, ex.getValue());
        }
        buff.append("\t\t\t\t\t</meaning>\n");

    }

    final void exampleToXML(StringBuilder buff, String example)
    {
        buff.append("\t\t\t\t\t\t<example>");
        buff.append(example);
        buff.append("</example>\n");
    }
}