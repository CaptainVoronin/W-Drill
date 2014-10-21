package org.sc.w_drill.backup;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.EPartOfSpeech;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.Meaning;
import org.sc.w_drill.dict.Word;
import org.sc.w_drill.utils.DBPair;
import org.sc.w_drill.utils.PartsOfSpeech;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.DataFormatException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by MaxSh on 09.10.2014.
 */
public class RestoreHelper
{
    WDdb database;

    public RestoreHelper(WDdb _database)
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

        // Create an XML document from buffer
        Document doc = docb.parse(is);

        // Take the root node, it must be "dictionary"
        Node root = doc.getFirstChild();

        if (!root.getNodeName().equals("dictionary"))
            throw new DataFormatException();

        String lang = getAttribute(root, "lang", true);
        String uuid = getAttribute( root, "uuid", true );

        if( DBDictionaryFactory.getInstance( database ).dictionaryExists( uuid ))
            throw new SQLiteConstraintException( "Dictionary with UUID {" + uuid + "} already exists." );

        NodeList children = root.getChildNodes();

        String dictname = null;
        Node content = null;

        for( int i = 0; i < children.getLength(); i++ )
            if( children.item( i ).getNodeName().equals( "name" ) )
            {
                dictname = children.item( i ).getTextContent();
            } else if( children.item( i ).getNodeName().equals( "content" ) )
                content = children.item( i );

         if( dictname == null  )
             throw new DataFormatException( "Name of dictionary hasn't been found" );

        if( dictname == null  )
            throw new DataFormatException( "Structure is incorrect" );

        SQLiteDatabase db = this.database.getWritableDatabase();
        db.beginTransaction();

        Exception ex = null;
        try {

            Dictionary dict = DBDictionaryFactory.getInstance(this.database).createNewSpec(dictname, lang, db, uuid);
            String meaning = null;
            String example = null;
            String value = null;

            NodeList nList = doc.getElementsByTagName("word");
            DBWordFactory instance = DBWordFactory.getInstance( database, dict );
            Word word = null;
            for (int j = 0; j < nList.getLength(); j++) {
                Node n = nList.item(j);

                int percent = Integer.parseInt( n.getAttributes().getNamedItem( "percent" ).getNodeValue() );
                String w_uuid = n.getAttributes().getNamedItem( "uuid" ).getNodeValue();
                int state = Integer.parseInt( n.getAttributes().getNamedItem( "state" ).getNodeValue() );

                NodeList nodes = n.getChildNodes();
                for( int i = 0; i < nodes.getLength(); i++  ) {
                    Node node = nodes.item(i);
                    if (node.getNodeName().equals("value")) {
                        word = new Word(getTextContent(node));
                        break;
                    }
                }

                for( int i = 0; i < nodes.getLength(); i++  )
                {
                    Node node = nodes.item( i );
                    if( node.getNodeName().equals( "meaning" ) )
                    {
                        IMeaning meaning = extractMeaning( node );
                        word.meanings().add( meaning );
                    }
                }
                if( word != null && word.getWord().length() != 0 ) {
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

    private IMeaning extractMeaning(Node node)
    {
        String value = null, example = null;
        ArrayList<DBPair> examples = new ArrayList<DBPair>();

        boolean isDisapp = Boolean.parseBoolean( node.getAttributes()
                                                .getNamedItem( "disapproving" ).getNodeValue() );

        boolean isFormal = Boolean.parseBoolean( node.getAttributes()
                .getNamedItem( "formal" ).getNodeValue() );

        boolean isRude = Boolean.parseBoolean( node.getAttributes()
                .getNamedItem( "rude" ).getNodeValue() );

        String pos = node.getAttributes().getNamedItem("pos").getNodeValue();

        if(EPartOfSpeech.check( pos ) )
            pos = EPartOfSpeech.noun.toString();

        NodeList nodes = node.getChildNodes();

        for( int i = 0; i < nodes.getLength(); i++ )
        {
            Node n = nodes.item( i );
            if( n.getNodeName().equals( "value" ) )
                value = n.getNodeValue();
            else
                examples.add( new DBPair(-1, n.getNodeValue()) );
        }

        // TODO: Attributes: rude, formal, disapp. are ignored

        Meaning m = new Meaning( value );
        m.setPartOfSpeech( pos );
        m.examples().addAll( examples );

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
