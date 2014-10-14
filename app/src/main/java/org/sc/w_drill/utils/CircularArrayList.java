package org.sc.w_drill.utils;

import java.util.ArrayList;

/**
 * Created by MaxSh on 14.10.2014.
 */
public class CircularArrayList<T>
{
    ArrayList<T> elements;
    int pos;


    public CircularArrayList( ArrayList<T> _elements )
    {
        elements = _elements;
        pos = 0;
    }

    public boolean remove( T obj )
    {
        int index = elements.indexOf( obj );
        if( index == -1 )
            return false;

        if ( index < pos )
            pos--;

        elements.remove( obj );

        if( elements.size() >= pos )
            pos = 0;

        return true;
    }

    public T next()
    {
        if( elements.size() == 0 )
            return null;

        T obj = elements.get( pos );
        if( pos + 1 >= elements.size())
            pos = 0;
        else
            pos++;

        return obj;
    }

    public int size()
    {
        return elements.size();
    }

    public int indexOf( T obj )
    {
        return elements.indexOf( obj );
    }

    public void set( int index, T obj )
    {
        elements.set( index, obj );
    }
}
