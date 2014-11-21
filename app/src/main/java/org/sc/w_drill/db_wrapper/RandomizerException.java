package org.sc.w_drill.db_wrapper;

/**
 * Created by MaxSh on 13.10.2014.
 */
public class RandomizerException extends Exception
{

    private int avalableCount;
    private int requiredCount;

    public RandomizerException(String _message)
    {
        super(_message);
    }

    public RandomizerException(int required, int available)
    {
        super("Not enough elements. There are " + available + ", required " + required);
        avalableCount = available;
        requiredCount = required;
    }


    public int getAvailableCount()
    {
        return avalableCount;
    }

    public int getRequiredCount()
    {
        return requiredCount;
    }
}
