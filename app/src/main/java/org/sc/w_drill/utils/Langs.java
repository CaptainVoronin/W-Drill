package org.sc.w_drill.utils;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.sc.w_drill.R;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Created by MaxSh on 22.10.2014.
 */
public class Langs
{
    Context context;

    static Langs instance;

    HashMap<String, String> langs;

    static
    {
        instance = null;
    }

    protected Langs( Context _context )
    {
        context = _context;
        init();
    }

    private void init()
    {
        String[] array = context.getResources().getStringArray( R.array.langs );
        langs = new HashMap<String, String>();

        for( int i = 0; i < array.length; i++ )
        {
            String[] parts = array[i].split( ";" );
            langs.put(parts[0], parts[1]);
        }
    }

    public static final Langs getInstance( Context _context )
    {
        if( instance == null )
            instance = new Langs( _context );
        return instance;
    }

    public ArrayList<String> getList()
    {
        ArrayList<String> list = new ArrayList<String>( );
        list.addAll(langs.values());
        return list;
    }

    public Object[] keysArray()
    {
        return langs.keySet().toArray();
    }

    public String get( String key )
    {
        return langs.get( key );
    }

    public HashMap<String,String> getSubset( String pattern )
    {
        HashMap<String,String> result = new HashMap<String,String>();
        if( pattern == null )
        {
            result = langs;
        }
        else {
            Set<String> set = langs.keySet();
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = langs.get(key);
                String str = value.toLowerCase();
                if (str.contains(pattern.toLowerCase())) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    public String getCode( String name )
    {

        Iterator<Map.Entry<String,String>> it = langs.entrySet().iterator();

        while( it.hasNext() )
        {
            Map.Entry<String,String> item = it.next();
            if( item.getValue().equals( name ))
                return item.getKey();
        }
        return null;
    }
}