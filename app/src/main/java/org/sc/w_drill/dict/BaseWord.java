package org.sc.w_drill.dict;

/**
 * Created by Max on 10/4/2014.
 */
public class BaseWord implements IBaseWord
{
    int id;
    String word;
    int learnPercent;
    LearnState state;
    int avgTime;
    private int accessCount;

    public BaseWord( int _id, String _word, int _learnPercent, LearnState _state, int _avgTime, int _accessCount )
    {
        id = _id;
        word = _word;
        learnPercent = _learnPercent;
        state = _state;
        avgTime = _avgTime;
        accessCount = _accessCount;
    }

    @Override
    public void setWord(String _word) {
        word = _word;
    }

    @Override
    public int getId()
    {
        return id;
    }

    @Override
    public void setId(int _id)
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
        return learnPercent;
    }

    @Override
    public void setLearnPercent(int percent)
    {
        learnPercent = percent;
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
    }

    @Override
    public void setAvgTime(int time)
    {
        avgTime = time;
    }

    @Override
    public int getAvgTime()
    {
        return avgTime;
    }

    @Override
    public int getAccessCount()
    {
        return accessCount;
    }

    @Override
    public boolean equals( Object obj )
    {
        if( obj == null )
            return false;

        if( !(obj instanceof IBaseWord) )
            return false;

        IBaseWord d = ( IBaseWord ) obj;

        return ((d.getId() == id ) && ( (d.getWord().equalsIgnoreCase( word ))));
    }
}