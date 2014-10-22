package org.sc.w_drill.utils;

import android.content.Context;

import org.sc.w_drill.R;

/**
 * Created by MaxSh on 22.10.2014.
 */
public class Langs
{
    Context context;

    static Langs instance;

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
        String[] array = context.getResources().getStringArray(R.array.languages );
    }

    public static final Langs getInstance( Context _context )
    {
        if( instance == null )
            instance = new Langs( _context );
        return instance;
    }
}
