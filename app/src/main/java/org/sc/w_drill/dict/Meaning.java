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
    private boolean isFormal;
    private boolean isDisapproving;
    private boolean isRude;
    ArrayList<DBPair> examples;
    private String partOS;

    public Meaning( int _id, String _meaning  )
    {
        id = _id;
        meaning = _meaning;
        partOS = EPartOfSpeech.noun.toString();
    }

    public Meaning( String _meaning  )
    {
        id = -1;
        meaning = _meaning;
        partOS = EPartOfSpeech.noun.toString();
    }

    public Meaning( int _id, String _meaning, boolean _isFormal, boolean _isDisapproving, boolean _isRude  )
    {
        id = _id;
        meaning = _meaning;
        partOS = EPartOfSpeech.noun.toString();
        setFormal(_isFormal);
        setDisapproving(_isDisapproving);
        setRude(_isRude);
    }


    public int getId()
    {
        return id;
    }

    @Override
    public String partOFSpeech()
    {
        return partOS;
    }

    @Override
    public void setPartOfSpeech(String part)
    {
        partOS = part;
    }

    @Override
    public String meaning()
    {
        return meaning;
    }

    @Override
    public boolean isFormal()
    {
        return isFormal;
    }

    @Override
    public boolean isDisapproving()
    {
        return isDisapproving;
    }

    @Override
    public boolean isRude()
    {
        return isRude;
    }

    @Override
    public ArrayList<DBPair> examples()
    {
        return getExamples();
    }

    public void addExample( DBPair example )
    {
        getExamples().add( example );
    }

    public void addExample( int id, String value )
    {
        getExamples().add( new DBPair( id, value ) );
    }

    public void addExample( String value )
    {
        addExample( -1, value );
    }

    private final ArrayList<DBPair> getExamples()
    {
        if( examples == null )
            examples = new ArrayList<DBPair>();
        return examples;
    }

    public void setFormal(boolean isFormal) {
        this.isFormal = isFormal;
    }

    public void setDisapproving(boolean isDisapproving) {
        this.isDisapproving = isDisapproving;
    }

    public void setRude(boolean isRude) {
        this.isRude = isRude;
    }
}