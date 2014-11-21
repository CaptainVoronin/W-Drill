package org.sc.w_drill.utils;

/**
 * Created by Max on 10/8/2014.
 */
public class DBPair
{
    int id;
    String value;

    public DBPair(int _id, String _value)
    {
        id = _id;
        value = _value;
    }

    public int getId()
    {
        return id;
    }

    public String getValue()
    {
        return value;
    }

}
