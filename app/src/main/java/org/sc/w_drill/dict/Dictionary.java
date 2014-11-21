package org.sc.w_drill.dict;

import org.joda.time.DateTime;

/**
 * Created by maxsh on 29.09.2014.
 */
public class Dictionary
{

    private int id;
    private String name;
    int word_count;
    int words_to_learn;
    int words_to_check;
    private String lang;
    String uuid;
    private DateTime lastAccess;
    private int imagesCount;
    private long imagesSize;

    public Dictionary(int _id, String _name, String _uuid, String _lang, int _word_count)
    {
        id = _id;
        name = _name;
        lang = _lang;
        word_count = _word_count;
        uuid = _uuid;
        imagesCount = -1;
        imagesSize = -1;
    }

    public Dictionary(int _id, String _name, String _uuid, String _lang, int _word_count, int _words_to_learn, int _words_to_check)
    {
        id = _id;
        name = _name;
        lang = _lang;
        word_count = _word_count;
        words_to_learn = _words_to_learn;
        words_to_check = _words_to_check;
        uuid = _uuid;
        imagesCount = -1;
        imagesSize = -1;

    }

    public Dictionary(int _id, String _name, String _uuid, String _lang)
    {
        id = _id;
        name = _name;
        lang = _lang;
        word_count = 0;
        uuid = _uuid;
        imagesCount = -1;
        imagesSize = -1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;

        if (!(obj instanceof Dictionary))
            return false;

        Dictionary d = (Dictionary) obj;

        return ((d.getId() == id) && ((d.getName().equalsIgnoreCase(name))));
    }

    public String getUUID()
    {
        return uuid;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getLang()
    {
        return lang;
    }

    public int getWordCount()
    {
        return word_count;
    }

    public int getWordsToLearn()
    {
        return words_to_learn;
    }

    public int getWordsToCheck()
    {
        return words_to_check;
    }

    public void setWordsToLear(int val)
    {
        words_to_learn = val;
    }

    public void setWordsToCheck(int val)
    {
        words_to_check = val;
    }


    public DateTime getLastAccess()
    {
        return lastAccess;
    }

    public void setLastAccess(DateTime lastAccess)
    {
        this.lastAccess = lastAccess;
    }

    public int getImagesCount()
    {
        return imagesCount;
    }

    public void setImagesCount(int imagesCount)
    {
        this.imagesCount = imagesCount;
    }

    public long getImagesSize()
    {
        return imagesSize;
    }

    public void setImagesSize(long imagesSize)
    {
        this.imagesSize = imagesSize;
    }
}
