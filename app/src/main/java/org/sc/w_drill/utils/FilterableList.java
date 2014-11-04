package org.sc.w_drill.utils;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Max on 10/5/2014.
 */
public class FilterableList<T> extends ArrayList<T> implements IFilterChangedListener
{
    IFilter<T> filter;
    ArrayList<T> filteredItems;
    ArrayList<IFilteredListChangeListener> listeners;

    public FilterableList()
    {
        super();
        listeners = new ArrayList<IFilteredListChangeListener>();

    }
    public void setFilter( IFilter<T> _filter )
    {
        filter = _filter;
        applyFilter();
    }

    private void applyFilter()
    {
        filteredItems = new ArrayList<T>();
        for( int i = 0; i < super.size(); i++ )
        {
            T value = super.get(i);
            if (filter.check( value ))
                filteredItems.add( value );
        }
        sendMessage();
    }

    public void clearFilter()
    {
        filter = null;
        sendMessage();
    }

    @Override
    public int size()
    {
        int count = 0;

        if( filter == null )
            return super.size();
        else
            return filteredItems.size();
    }

    @Override
    public T get( int index )
    {
        if( filter == null )
            return super.get(index);
        else
            return filteredItems.get(index);
    }

    @Override
    public boolean add(T value)
    {
        boolean result = super.add( value );
        if( result && filter != null)
        {
            if (filter.check(value))
                result = filteredItems.add(value);
        }

        if( result )
            sendMessage();
        return result;
    }

    @Override
    public void clear()
    {
        super.clear();
        if( filteredItems != null )
            filteredItems.clear();
        sendMessage();
    }

    private void sendMessage()
    {
        if( listeners != null )
        {
            for( IFilteredListChangeListener listener : listeners )
                listener.onSizeChanged();
        }
    }

    @Override
    public boolean addAll( Collection<? extends T> items )
    {
        if( super.addAll( items ) )
            if( filteredItems != null )
            {
                applyFilter();
                sendMessage();
                return true;
            }
            else
                return false;
         else
            return false;
    }

    public void addListener( IFilteredListChangeListener listener )
    {
        listeners.add( listener );
    }

    @Override
    public void onFilterChanged()
    {
        applyFilter();
        sendMessage();
    }
}