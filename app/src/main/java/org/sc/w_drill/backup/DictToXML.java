package org.sc.w_drill.backup;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.DBPair;

import java.util.ArrayList;

/**
 * Created by MaxSh on 20.10.2014.
 */
public class DictToXML
{
    public static void toXML( WDdb database, StringBuilder buff, Dictionary dict )
    {

        buff.setLength(0);

        buff.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        buff.append( "<dictionary uuid=\"")
                .append( dict.getUUID() )
                .append( "\" lang=\"")
                .append( dict.getLang() )
                .append("\">\n");
        buff.append("\t<name>").append(dict.getName()).append("</name>\n");
        ArrayList<DBPair> ids = DBWordFactory.getInstance( database, dict ).technicalGetWordUnique();

        buff.append( "\t\t<content count=\"" ).append( ids.size() ).append( "\">\n" );

        for( DBPair pair : ids )
        {
            IWord word = DBWordFactory.getInstance( database, dict ).getWordEx( pair.getId() );
            WordToXML.toXML( buff, word, pair.getValue() );
        }

        buff.append( "\t\t</content>\n" );
        buff.append( "</dictionary>");
    }
}
