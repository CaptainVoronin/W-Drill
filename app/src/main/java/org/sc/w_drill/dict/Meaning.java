package org.sc.w_drill.dict;

import org.sc.w_drill.utils.DBPair;

import java.util.ArrayList;

/**
 * Created by Max on 10/8/2014.
 */
public class Meaning implements IMeaning
{
    int id;
    String meaning;
    boolean isFormal;
    boolean isDisapproving;
    boolean isRude;
    ArrayList<DBPair> examples;

    public Meaning( int _id, String _meaning  )
    {
        id = _id;
        meaning = _meaning;
    }

    public int getId()
    {
        return id;
    }

    @Override
    public String meaning()
    {
        return meaning;
    }

    @Override
    public boolean isFormal()
    {
        return false;
    }

    @Override
    public boolean isDisapproving()
    {
        return false;
    }

    @Override
    public boolean isRude()
    {
        return false;
    }

    @Override
    public ArrayList<DBPair> examples()
    {
        return examples;
    }

    public void addExample( DBPair example )
    {
        if( examples == null )
            examples = new ArrayList<DBPair>();
        examples.add( example );
    }

    public void addExample( int id, String value )
    {
        if( examples == null )
            examples = new ArrayList<DBPair>();
        examples.add( new DBPair( id, value ) );
    }
}
