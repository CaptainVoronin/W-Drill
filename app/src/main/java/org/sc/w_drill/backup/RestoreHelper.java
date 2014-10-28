package org.sc.w_drill.backup;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.EPartOfSpeech;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.Meaning;
import org.sc.w_drill.dict.Word;
import org.sc.w_drill.utils.DBPair;
import org.sc.w_drill.utils.PartsOfSpeech;
import org.sc.w_drill.utils.image.DictionaryImageFileManager;
import org.sc.w_drill.utils.image.ImageFileHelper;
import org.w3c.dom.CDATASection;
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
    DictionaryImageFileManager dictManager;
    Context context;
    public RestoreHelper(WDdb _database)
    {
        database = _database;
    }

    /**
     * The function accepts ZIP file.
     * @param file
     */
    public int load( Context _context, File file  ) throws Exception
    {
        context = _context;

        String dictFile = unzipEntry( file, context.getCacheDir() );

        if( dictFile == null )
            throw new DataFormatException();

        StringBuilder buff = internalLoad( dictFile );
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

        if( content == null  )
            throw new DataFormatException( "Structure is incorrect" );

        SQLiteDatabase db = this.database.getWritableDatabase();

        db.beginTransaction();

        Dictionary dict = DBDictionaryFactory.getInstance(this.database).createNewSpec(dictname, lang, db, uuid);

        Exception ex = null;
        String w_uuid = null;
        try {

            dictManager = new DictionaryImageFileManager( context,dict );
            dictManager.checkDir();

            NodeList nList = doc.getElementsByTagName("word");
            DBWordFactory instance = DBWordFactory.getInstance( database, dict );
            Word word = null;
            for (int j = 0; j < nList.getLength(); j++)
            {
                String transcr = null;
                Node n = nList.item(j);

                int percent = Integer.parseInt( n.getAttributes().getNamedItem( "percent" ).getNodeValue() );
                w_uuid = n.getAttributes().getNamedItem( "uuid" ).getNodeValue();
                int state = Integer.parseInt( n.getAttributes().getNamedItem( "state" ).getNodeValue() );

                NodeList nodes = n.getChildNodes();
                for( int i = 0; i < nodes.getLength(); i++  ) {
                    Node node = nodes.item(i);
                    if (node.getNodeName().equals("value")) {
                        word = new Word(getTextContent(node));

                    } else if (node.getNodeName().equals("transcription"))
                        transcr = getTextContent(node);
                    else if( node.getNodeName().equals("image") )
                        writeImage( node, w_uuid );
                }

                if( word == null )
                    throw new DataFormatException( "Dictionary file is corrupted" );

                word.setUUID( w_uuid );
                word.setTranscription( transcr == null ? "" : transcr );
                word.setLearnState( state == 0 ?
                        IBaseWord.LearnState.learn : IBaseWord.LearnState.check );
                word.setLearnPercent( percent );

                // An instance of the Word class creates with
                // the one empty meaning so it should be removed
                word.meanings().clear();

                for( int i = 0; i < nodes.getLength(); i++  )
                {
                    Node node = nodes.item( i );
                    if( node.getNodeType() != Node.ELEMENT_NODE )
                        continue;

                    if( node.getNodeName().equals( "meaning" ) )
                    {
                        IMeaning mean = extractMeaning( node );
                        word.meanings().add( mean );
                    }
                }

                if( word != null && word.getWord().length() != 0 )
                {
                    instance.technicalInsert(db, dict.getId(), word );
                    count++;
                }
                else
                    Log.w( "[DictionaryLoader::putInDB]", "Word is empty, skipped" );
            }
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException e) {

            int dict_id = 2;
            int id;
            String name;

            Cursor crs = db.rawQuery( "select id, word from words where uuid = '" + w_uuid + "'", null );
            if( crs.getCount() != 0 ) {
                while( crs.moveToNext() )
                {
                    id = crs.getInt( 0 );
                    name = crs.getString( 1 );
                    Log.e("[DictionaryLoader::putInDB]", "name : " + name );
                }
                crs.close();
            }

        } catch (Exception e){
            ex = e;
            Log.e("[DictionaryLoader::putInDB]", "Exception: " + e.getMessage());
            dictManager.deleteDictDir();
        } finally {
            db.endTransaction();
            if (ex != null)
                throw ex;
        }
        return count;
    }

    private void writeImage(Node node, String uuid ) throws IOException
    {
        NodeList list = node.getChildNodes();
        CDATASection cdata;

        for( int i = 0; i < list.getLength(); i++ )
        {
            Node child = list.item( i );
            if( child.getNodeType() == Node.CDATA_SECTION_NODE )
            {
                cdata = ( CDATASection ) child;
                String value = cdata.getData();
                String path = dictManager.mkPath(uuid);
                ImageFileHelper.imageFileFromBASE64( path, value );
                break;
            }
        }
    }

    private IMeaning extractMeaning(Node node) throws DataFormatException
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
                value = n.getTextContent() ;
            else
            {
                String ex = n.getTextContent().trim();
                if( ex.length() != 0 )
                    examples.add(new DBPair(-1, ex ));
            }
        }

        if( value == null )
            throw new DataFormatException( "Meaning can't be empty" );

        Meaning m = new Meaning( value );
        m.setFormal( isFormal );
        m.setDisapproving( isDisapp );
        m.setRude( isRude );
        m.setPartOfSpeech(pos);
        m.examples().addAll( examples );

        return m;
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