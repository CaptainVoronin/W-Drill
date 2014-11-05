package org.sc.w_drill.backup;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
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
import org.xml.sax.SAXException;

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
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by MaxSh on 09.10.2014.
 */
public class RestoreHelper
{
    WDdb database;
    DictionaryImageFileManager dictManager;
    File file;
    Context context;
    ImportProgressListener listener;
    boolean bLoadImages;
    boolean bLoadStats;
    Handler handler;

    public RestoreHelper( Context _context, WDdb _database, File _file,
                         ImportProgressListener _listener,
                         boolean _bLoadImages, boolean _bLoadStats, Handler _handler )
    {
        database = _database;
        context = _context;
        file = _file;
        listener = _listener;
        bLoadImages = _bLoadImages;
        bLoadStats = _bLoadStats;
        handler = _handler;
    }

    public int load(  ) throws Exception
    {
        if( handler != null )
            handler.sendEmptyMessage( ImportProgressListener.STATE_BEFORE_UNZIP );

        String dictFile = unzipEntry( file, context.getCacheDir() );

        if( dictFile == null )
            throw new DataFormatException();

        if( handler != null )
            handler.sendEmptyMessage( ImportProgressListener.STATE_LOAD_TEXT );

        StringBuilder buff = internalLoad( dictFile );
        Document doc = buffToDOM( buff );
        int count = DBDictionaryFactory.toughRestoreIntegrity( database );
        if( count != 0 )
            Log.d( "RestoreHelper::load", "Lost words have been deleted. Total " + count );
        return putInDB( doc );
    }

    private Document buffToDOM( StringBuilder buff ) throws IOException, SAXException, ParserConfigurationException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docb = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(buff.toString()));

        // Create an XML document from buffer
        return docb.parse(is);
    }

    private int putInDB( Document doc ) throws Exception
    {

        int count = 0;
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
               dictname = children.item( i ).getTextContent();
            else if( children.item( i ).getNodeName().equals( "content" ) )
                content = children.item( i );

         if( dictname == null  )
             throw new DataFormatException( "Name of dictionary hasn't been found" );

        if( content == null  )
            throw new DataFormatException( "Structure is incorrect" );

        SQLiteDatabase db = database.getWritableDatabase();

        db.beginTransaction();

        Dictionary dict = DBDictionaryFactory.getInstance(database).createNewSpec(dictname, lang, db, uuid);

        Exception ex = null;

        if( handler != null )
            handler.sendEmptyMessage( ImportProgressListener.STATE_LOAD_DB );

        try {

            dictManager = new DictionaryImageFileManager( context,dict );
            dictManager.checkDir();

            NodeList nList = doc.getElementsByTagName("word");
            DBWordFactory instance = DBWordFactory.getInstance( database, dict );

            if( listener != null )
                listener.setMaxValue( nList.getLength() );

            for (int j = 0; j < nList.getLength(); j++)
            {
                processWordNode( instance, db, dict.getId(), nList.item(j) );
                count++;
                if( listener != null )
                    listener.setProgress( count );
            }

            db.setTransactionSuccessful();
        } catch (Exception e){
            ex = e;
            Log.e("[DictionaryLoader::putInDB]", "Exception: " + e.getMessage());
            dictManager.deleteDictDir();
        } finally {
            db.endTransaction();
            db.close();
            if (ex != null)
                throw ex;
        }
        return count;
    }

    private void processWordNode( DBWordFactory instance, SQLiteDatabase db, int dictId, Node word_node ) throws IOException, DataFormatException
    {
        Word word = null;
        String transcr = null;
        int percent = 0;
        int state = 0;

        // TODO: dangerous code - starts here
        Node att = word_node.getAttributes().getNamedItem( "uuid" );

        if( att == null )
            throw new DataFormatException( "A node word must has an attribute uuid" );

        String w_uuid = att.getNodeValue();

        att = word_node.getAttributes().getNamedItem( "percent" );
        if( att != null  )
            percent = Integer.parseInt( att.getNodeValue() );

        att = word_node.getAttributes().getNamedItem("state");
        if( att != null  )
            state = Integer.parseInt( att.getNodeValue() );

        NodeList nodes = word_node.getChildNodes();
        Node imageNode = null;
        for( int i = 0; i < nodes.getLength(); i++  ) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("value"))
                word = new Word(getTextContent(node));
            else if (node.getNodeName().equals("transcription"))
                transcr = getTextContent(node);
            else if( node.getNodeName().equals("image") && bLoadImages )
                imageNode = node;
        }

        if( word == null )
            throw new DataFormatException( "Dictionary file is corrupted" );

        if( imageNode != null )
            writeImage( imageNode, word );

        word.setUUID( w_uuid );
        word.setTranscription( transcr == null ? "" : transcr );

        if( bLoadStats )
        {
            word.setLearnState(state == 0 ?
                    IBaseWord.LearnState.learn : IBaseWord.LearnState.check);
            word.setLearnPercent(percent);
        }

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
            instance.technicalInsert(db, dictId, word );
        else
            Log.w( "[DictionaryLoader::putInDB]", "Word is empty, skipped" );
    }

    private void writeImage(Node node, IBaseWord word ) throws IOException
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
                String path = dictManager.mkPath(word);
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