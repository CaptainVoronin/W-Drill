package org.sc.w_drill.utils;

import android.content.Context;

import org.sc.w_drill.R;
import org.sc.w_drill.dict.IWord;

import java.util.HashMap;

/**
 * Created by Max on 10/17/2014.
 */
public class LearnColors
{

    HashMap< Integer, Integer > colors;

    Context context;

    static LearnColors instance;

    static
    {
        instance = null;
    }


    protected LearnColors(Context _context)
    {
        context = _context;
        colors = new HashMap< Integer, Integer >();
        init();
    }

    protected void init()
    {
        colors.put(R.color.lp0, context.getResources().getColor(R.color.lp0));
        colors.put( R.color.lp20, context.getResources().getColor( R.color.lp20 ) );
        colors.put( R.color.lp40, context.getResources().getColor( R.color.lp40 ) );
        colors.put( R.color.lp60, context.getResources().getColor( R.color.lp60 ) );
        colors.put( R.color.lp80, context.getResources().getColor( R.color.lp80 ) );
        colors.put( R.color.lp100, context.getResources().getColor( R.color.lp100 ) );

        colors.put( R.color.cp0, context.getResources().getColor( R.color.cp0 ) );
        colors.put( R.color.cp20, context.getResources().getColor( R.color.cp20 ) );
        colors.put( R.color.cp40, context.getResources().getColor( R.color.cp40 ) );
        colors.put( R.color.cp60, context.getResources().getColor( R.color.cp60 ) );
        colors.put( R.color.cp80, context.getResources().getColor( R.color.cp80 ) );
        colors.put( R.color.cp100, context.getResources().getColor( R.color.cp100 ) );
    }

    protected int getColor( int color_id )
    {
        return colors.get( color_id );
    }

    public static final LearnColors getInstance( Context _context )
    {
        if( instance == null )
            instance = new LearnColors( _context );

        return instance;
    }

    public int getColor( IWord word )
    {
        if ( word.getLearnState() == IWord.LearnState.learn )
            return getLeanColor( word.getLearnPercent() );
        else
            return getCheckColor(word.getLearnPercent());
    }

    public int getColor( int stage, int percent )
    {
        if ( stage == 0 )
            return getLeanColor( percent );
        else
            return getCheckColor(percent);
    }

    public int getColor( IWord.LearnState state, int percent )
    {
        if ( state == IWord.LearnState.learn  )
            return getLeanColor( percent );
        else
            return getCheckColor(percent);
    }

    private int getCheckColor(int percent)
    {
        int color = colors.get( R.color.cp0 );

        if( percent >= 100 && percent < 120  )
            color = colors.get( R.color.cp20 );
        else if( percent >= 120 && percent < 140   )
            color = colors.get( R.color.cp40 );
        else if( percent >= 140 && percent < 160   )
            color = colors.get( R.color.cp60 );
        else if( percent >= 160 && percent < 180   )
            color = colors.get( R.color.cp80 );
        else if( percent >= 180 )
            color = colors.get( R.color.cp100 );

        return color;
    }

    private int getLeanColor(int percent)
    {
        int color = colors.get( R.color.lp0 );

        if( percent >= 20 && percent < 40  )
            color = colors.get( R.color.lp20 );
        else if( percent >= 40 && percent < 60   )
            color = colors.get( R.color.lp40 );
        else if( percent >= 60 && percent < 80   )
            color = colors.get( R.color.lp60 );
        else if( percent >= 80 && percent < 100   )
            color = colors.get( R.color.lp80 );
        else if( percent >= 100 )
            color = colors.get( R.color.lp100 );

        return color;
    }
}
