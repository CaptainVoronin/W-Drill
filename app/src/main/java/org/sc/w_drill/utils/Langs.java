package org.sc.w_drill.utils;

import android.content.Context;
import android.util.Pair;

import org.sc.w_drill.R;

import java.util.ArrayList;
import java.util.HashMap;
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
        list.addAll( langs.values() );
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
}
