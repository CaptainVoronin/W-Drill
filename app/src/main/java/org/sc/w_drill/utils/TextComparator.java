package org.sc.w_drill.utils;

import java.util.StringTokenizer;

/**
 * Created by MaxSh on 10.11.2014.
 */
public class TextComparator {

    protected TextComparator()
    {}


    public static final boolean compare( String pattern, String text )
    {
        StringTokenizer st = new StringTokenizer( " " );
        String[] tokens = text.split( " " );

        StringBuilder buff = new StringBuilder();

        boolean flag = false;

        for( int i = 0; i < tokens.length; i++ ) {
            if ( tokens[i].startsWith( "[" ) && !flag )
            {
                buff.append(tokens[i].trim().toLowerCase());
            }
            else if( tokens[i].startsWith( "]" ) )
                buff.append(tokens[i].trim().toLowerCase());

        }

        return true;
    }

    public static final String[] splitWithBrackets( String text )
    {
        return text.split( "[]]");
    }
}
