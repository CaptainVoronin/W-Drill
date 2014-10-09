package org.sc.w_drill.db_wrapper;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.dict.Dictionary;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.AttributedCharacterIterator;
import java.util.Enumeration;
import java.util.zip.DataFormatException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by MaxSh on 09.10.2014.
 */
public class DictionaryLoader
{
    WDdb database;

    public DictionaryLoader( WDdb _database )
    {
        database = _database;
    }

    /**
     * The function accepts ZIP file.
     * @param file
     */
    public int load( Context context, File file, File internalStorage  ) throws Exception {
        String dictFile = unzipEntry( file, internalStorage );

        if( dictFile == null )
            throw new DataFormatException();

        StringBuilder buff = internalLoad( dictFile );
        WDdb database = new WDdb( context );
        return putInDB( database, buff );
    }

    private int putInDB(WDdb database, StringBuilder buff) throws Exception {
        int count = 0;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docb = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(buff.toString()));

        Document doc = docb.parse(is);

        Node root = doc.getFirstChild();

        if (!root.getNodeName().equals("dictionary"))
            throw new DataFormatException();

        String dictname = getAttribute(root, "name", true);

        String lang = getAttribute(root, "lang", true);

        SQLiteDatabase db = this.database.getWritableDatabase();
        db.beginTransaction();

        Exception ex = null;
        try {
            Dictionary dict = DBDictionaryFactory.getInstance(this.database).createNewSpec(dictname, lang, db);
            String word = null;
            String meaning = null;
            String example = null;

            NodeList nList = doc.getElementsByTagName("row");
            DBWordFactory instance = DBWordFactory.getInstance( database, dict );
            for (int j = 0; j < nList.getLength(); j++) {
                Node n = nList.item(j);

                NodeList nodes = n.getChildNodes();
                for( int i = 0; i < nodes.getLength(); i++  )
                {
                    Node node = nodes.item( i );
                    if( node.getNodeName().equals( "word" ) )
                        word = getTextContent(node);
                    else if( node.getNodeName().equals( "translation" ) )
                        meaning = getTextContent(node);
                    else if( node.getNodeName().equals( "description" ) )
                        example = getTextContent(node);
                }
                Log.d( "[Word]", word + " insert" );
                if( word != null && word.length() != 0 ) {
                    instance.technicalInsert(db, dict.getId(), word, meaning, example);
                    count++;
                }
                else
                    Log.d( "[DictionaryLoader::putInDB]", "Word is empty, skipped" );
            }
            db.setTransactionSuccessful();

        } catch (Exception e) {
            ex = e;
            Log.e("[DictionaryLoader::putInDB]", "Exception: " + e.getMessage());
        } finally {
            db.endTransaction();
            if (ex != null)
                throw ex;
        }
        return count;
    }

    private void insertWordRow(Node n)
    {
    }

    String getTextContent( Node node )
    {
        NodeList nodes;

        if( ( ( nodes = node.getChildNodes() ) == null ) || nodes.getLength() == 0 )
            return "";

        return node.getFirstChild().getTextContent();
    }

    String getAttribute( Node node, String attName, boolean must_be ) throws DataFormatException
    {

        NamedNodeMap atts = null;

        if( (atts = node.getAttributes() ) == null )
            if( must_be )
                throw new DataFormatException( );
            else
                return "";

        Node att = atts.getNamedItem( attName );

        if( att == null  )
            if( must_be )
                throw new DataFormatException();
            else
                return "";

        String value = att.getNodeValue();

        if( value == null && value.length() == 0 )
            if( must_be )
                throw new DataFormatException();
            else
                return "";
        return value;
    }

    private StringBuilder internalLoad(String dictFile) throws FileNotFoundException, IOException
    {
        StringBuilder buff = new StringBuilder();
        String line = null;

        BufferedReader reader = new BufferedReader( new FileReader( dictFile ) );

        while( ( line = reader.readLine() ) != null )
        {
            buff.append( line );
        }

        reader.close();
        return buff;
    }

    private String unzipEntry( File file, File internalStorage ) throws IOException
    {
        byte[] buffer = new byte[1024];

        ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
        ZipEntry entry = (ZipEntry) zip.getNextEntry();

        File tmpFile = null;

        tmpFile = new File(internalStorage + File.separator + entry.getName());

        FileOutputStream fos = new FileOutputStream(tmpFile);

        int len;
        while ((len = zip.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }

        fos.close();
        zip.closeEntry();
        zip.close();

        if( tmpFile != null )
            return tmpFile.getPath();
        else
            return null;
    }


}
