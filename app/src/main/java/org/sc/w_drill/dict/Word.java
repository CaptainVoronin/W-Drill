package org.sc.w_drill.dict;

import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * Created by Max on 10/4/2014.
 */
public class Word extends BaseWord implements IWord
{
    ArrayList<IMeaning> meanings;
    String transcription;
    private DateTime lastUpdate;
    private DateTime lastAccess;

    public Word(int _id, String _word, int _learnPercent, LearnState _state, int _avgTime, int _accessCount, String uuid)
    {
        super(_id, _word, _learnPercent, _state, _avgTime, _accessCount, uuid);
        word = _word;
    }

    public Word(String _word)
    {
        super(-1, _word, 0, LearnState.learn, 0, 0, "");
        word = _word;
        addMeaning(new Meaning(""));
    }

    @Override
    public ArrayList<IMeaning> meanings()
    {
        return getMeanings();
    }

    public void addMeaning(IMeaning meaning)
    {
        getMeanings().add(meaning);
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
    public void setWord(String _word)
    {
        word = _word;
    }

    public static Word getDummy()
    {
        return new Word("");
    }

    private final ArrayList<IMeaning> getMeanings()
    {
        if (meanings == null)
            meanings = new ArrayList<IMeaning>();

        return meanings;
    }

    public DateTime getLastUpdate()
    {
        return lastUpdate;
    }

    public void setLastUpdate(DateTime lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public DateTime getLastAccess()
    {
        return lastAccess;
    }

    @Override
    public void setLastAccess(DateTime lastAccess)
    {
        this.lastAccess = lastAccess;
    }
}
