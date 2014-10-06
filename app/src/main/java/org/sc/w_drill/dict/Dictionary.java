package org.sc.w_drill.dict;

/**
 * Created by maxsh on 29.09.2014.
 */
public class Dictionary {

    private int id;
    private String name;
    int word_count;
    private String lang;

    public Dictionary( int _id, String _name, String _lang, int _word_count )
    {
        id = _id;
        name = _name;
        lang = _lang;
        word_count = _word_count;
    }

    public Dictionary( int _id, String _name, String _lang )
    {
        id = _id;
        name = _name;
        lang = _lang;
        word_count = 0;
    }

    @Override
    public boolean equals( Object obj )
    {
        if( obj == null )
            return false;

        if( !(obj instanceof Dictionary) )
            return false;

        Dictionary d = ( Dictionary ) obj;

        return ((d.getId() == id ) && ( (d.getName().equalsIgnoreCase( name ))));
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLang() {
        return lang;
    }

    public int getWordCount() {
        return word_count;
    }
}
