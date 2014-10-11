package org.sc.w_drill.dict;

import java.util.ArrayList;

/**
 * Created by Max on 10/4/2014.
 */
public class Word extends BaseWord implements IWord
{
    ArrayList<IMeaning> meanings;
    String transcription;

    public Word ( int _id, String _word, int _learnPercent, LearnState _state, int _avgTime, int _accessCount  )
    {
        super( _id, _word, _learnPercent, _state, _avgTime, _accessCount );
        word = _word;
    }

    public Word ( String _word )
    {
        super( -1, _word, 0, LearnState.learn, 0, 0 );
        word = _word;
    }

    @Override
    public ArrayList<IMeaning> meanings()
    {
        return getMeanings();
    }

    public void addMeaning( IMeaning meaning )
    {
        getMeanings().add( meaning );
    }

    @Override
    public String getTranscription()
    {
        return transcription;
    }

    @Override
    public void setTranscription(String value)
    {
        transcription = value;
    }

    @Override
    public void setWord(String _word) {
        word = _word;
    }

/*    @Override
    public int getId()
    {
        return id;
    } */

    /*public void setId( int _id )
    {
        id = _id;
    }

    @Override
    public String getWord()
    {
        return word;
    }

    @Override
    public int getLearnPercent()
    {
        return percent;
    }

    public void setLearnPercent( int _percent )
    {
        percent = _percent;
    }

    @Override
    public LearnState getLearnState()
    {
        return state;
    }

    @Override
    public void setLearnState(LearnState _state)
    {
        state = _state;
    } */

    public static Word getDummy()
    {
        return new Word( "" );
    }

    private final ArrayList<IMeaning> getMeanings()
    {
        if( meanings == null )
            meanings = new ArrayList<IMeaning>();

        return meanings;
    }
}
