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
    Random random;

    public ArrayListRandomizer()
    {
        random = new Random();
    }

    public ArrayList<T> stir( ArrayList<T> array )
    {
        Collections.shuffle( array, random );
        return array;
    }

    public T getRandomItem( ArrayList<T> array )
    {
        Collections.shuffle( array, random );
        return array.get( random.nextInt( array.size() ) );
    }
}
