package org.sc.w_drill.utils;

/**
 * Created by Max on 10/5/2014.
 */
public interface IFilter<T>
{
    public boolean check( T value );
}
