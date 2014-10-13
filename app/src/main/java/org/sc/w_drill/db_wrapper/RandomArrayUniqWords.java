package org.sc.w_drill.db_wrapper;

import org.sc.w_drill.dict.IWord;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by MaxSh on 13.10.2014.
 */
public class RandomArrayUniqWords
{

    /**
     * The function fills an initial array, which is passed as the first parameter
     * with random words. The initial array can contains elements.
     * The parameter upperLimit is a maximum length of the result array.
     *
     * @param initialArray
     * @param randomiser
     * @param upperLimit
     * @return
     */
    public static ArrayList<IWord> make(ArrayList<IWord> initialArray, WordRandomizer randomiser, int upperLimit)
            throws RandomizerException {

        IWord word;

        int initalSize = upperLimit - initialArray.size();

        if( initalSize <= 0 )
            throw new IllegalArgumentException( "Incorrect value for upper limit" );

        if( upperLimit > randomiser.getAvalableElementsSize() )
            throw new RandomizerException( initalSize, randomiser.getAvalableElementsSize() );
        else if( upperLimit == randomiser.getAvalableElementsSize() )
        {
            ArrayList<IWord> words = new ArrayList<IWord>();
            words.addAll( randomiser.getAllInOrder() );
            for( IWord w : initialArray )
                words.remove( w );
            initialArray.addAll( words );
        }
        else {
            ArrayList<IWord> words = new ArrayList<IWord>();

            boolean exists = false;

            while (initalSize > 0) {
                word = randomiser.getRandomWord();
                exists = false;

                for (IWord w : initialArray)
                    if (w.getId() == word.getId()) {
                        exists = true;
                        break;
                    }

                for (IWord w : words)
                    if (w.getId() == word.getId()) {
                        exists = true;
                        break;
                    }

                if (!exists) {
                    words.add(word);
                    initalSize--;
                }
            }

            initialArray.addAll(words);
        }
        Collections.shuffle( initialArray );

        return initialArray;
    }
}
