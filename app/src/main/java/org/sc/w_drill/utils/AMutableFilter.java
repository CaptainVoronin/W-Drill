package org.sc.w_drill.utils;

import java.util.ArrayList;

/**
 * Created by Max on 10/5/2014.
 */
public abstract class AMutableFilter<E, T> implements IFilter<T>
{
    protected E pattern;

    ArrayList<IFilterChangedListener> listeners;

    public AMutableFilter(E _pattern)
    {
        pattern = _pattern;
        listeners = new ArrayList<IFilterChangedListener>();
    }

    public void setNewPattern(E _pattern)
    {
        pattern = _pattern;
        sendMessage();
    }

    public void addListener(IFilterChangedListener _listener)
    {
        listeners.add(_listener);
    }

    public void sendMessage()
    {
        for (IFilterChangedListener listener : listeners)
            listener.onFilterChanged();
    }

}
