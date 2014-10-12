package org.sc.w_drill.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Created by Max on 10/12/2014.
 */
public class ArrayListRandomizer<T>
{
    int index = 0;

    public ArrayList<T> stir( ArrayList<T> array )
    {
        Random random = new Random();
        Collections.shuffle( array );
        return array;
    }
}
