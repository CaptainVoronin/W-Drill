package org.sc.w_drill;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.db_wrapper.WordRandomizer;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.ArrayListRandomizer;

import java.util.ArrayList;


public class ActCheckWords extends ActionBarActivity {

    enum Mode { CHOISE, COMPARE };

    Mode mode;

    LinearLayout rootView;
    RelativeLayout chooseOptionView = null;
    LinearLayout enterWordView = null;
    View activeView = null;
    IWord activeWord;
    WDdb database;
    Dictionary activeDict;
    WordRandomizer randomizer;
    String whereSTMT = " stage = 1 ";
    ArrayList<IWord> subset;
    ArrayListRandomizer<IWord> arrayRandomizer;
    TextView tv1, tv2, tv3, tv4;
    boolean missed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_check_words);
        rootView = ( LinearLayout ) findViewById( R.id.root_view );

        Intent args = getIntent();
        int dictId = args.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
        if( dictId == -1 )
            fatalError();
        database = new WDdb( getApplicationContext() );

        activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );
        randomizer = new WordRandomizer( database, activeDict );
        randomizer.init( whereSTMT );
        subset = new ArrayList<IWord>();
        arrayRandomizer = new ArrayListRandomizer<IWord>();
        changeWord();
    }

    private void fatalError()
    {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_check_words, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void changeWord()
    {
        activeWord = getWord();
        if( activeView != null )
            rootView.removeView( activeView );
        activeView = getView();
        rootView.addView( activeView );
        rootView.refreshDrawableState();
    }

    private View getView()
    {
        if( activeWord.getLearnPercent() > 200 )
        {
            mode = Mode.CHOISE;
            return getWriteWordView();
        }
        else
        {
            mode = Mode.COMPARE;
            return getChooseOptionView();
        }
    }

    private IWord getWord()
    {
        return randomizer.gerRandomWord();
    }

    View getChooseOptionView()
    {

        //if( chooseOptionView == null )
        //{
            chooseOptionView = (RelativeLayout) getLayoutInflater()
                    .inflate(R.layout.fragment_act_check_words_choose_option, null);

            tv1 = ( TextView ) chooseOptionView.findViewById( R.id.version1 );
            tv1.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    checkChoise( view );
                }
            });

            tv2 = ( TextView ) chooseOptionView.findViewById( R.id.version2 );
            tv2.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    checkChoise( view );
                }
            });

            tv3 = ( TextView ) chooseOptionView.findViewById( R.id.version3 );
            tv3.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    checkChoise( view );
                }
            });

            tv4 = ( TextView ) chooseOptionView.findViewById( R.id.version4 );
            tv4.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    checkChoise( view );
                }
            });
        //}

        tv1.setBackgroundColor( Color.WHITE );
        tv2.setBackgroundColor( Color.WHITE );
        tv3.setBackgroundColor( Color.WHITE );
        tv4.setBackgroundColor( Color.WHITE );

        subset.clear();
        subset.add( activeWord );
        IWord word;

        // TODO: it's wrong method there can be duplicates
        for( int i = 0; i < 3; i++ )
            subset.add( randomizer.gerRandomWord() );

        arrayRandomizer.stir( subset );

        TextView tv = ( TextView ) chooseOptionView.findViewById( R.id.word_for_check );
        tv.setText( activeWord.getWord() );

        word = subset.get(0);
        tv1.setText( word.meanings().get(0).meaning() );
        tv1.setTag( word );

        word = subset.get(1);
        tv2.setText( word.meanings().get(0).meaning() );
        tv2.setTag( word );

        word = subset.get(2);
        tv3.setText( word.meanings().get(0).meaning() );
        tv3.setTag( word );

        word = subset.get(3);
        tv4.setText( word.meanings().get(0).meaning() );
        tv4.setTag( word );

        return chooseOptionView;
    }

    View getWriteWordView()
    {
        if( enterWordView == null )
        {
            enterWordView = (LinearLayout) getLayoutInflater().inflate(R.layout.fragment_act_check_words_enter_word, rootView);
        }
        return enterWordView;
    }

    void checkChoise( View view )
    {
        Object tag = view.getTag();
        if( tag != null )
            if( ((IWord) tag ).getId() == activeWord.getId() )
            {
                if( !missed )
                    increasePercent(activeWord);
                else
                    missed = false;
                changeWord();
            }
            else
            {
                missed = true;
                decreasePercent( activeWord );
                showCorrectOption(view);
            }
    }

    private void increasePercent(IWord activeWord)
    {
        int percent = activeWord.getLearnPercent();
        if( percent + 20 > 200 )
            percent = 200;
        else
            percent += 20;

        // TODO: time is skipped
        DBWordFactory.getInstance( database, activeDict )
                .updatePercentAndTime( activeWord.getId(), percent, 0 );

    }

    private void decreasePercent(IWord activeWord)
    {
        int percent = activeWord.getLearnPercent();
        boolean reinit = false;

        if( percent - 20 < 0 )
        {
            // In this case the state must be
            // changed and the word must be removed from current list
            // So WordRandomizer must be reinitialized
            percent = 0;
            reinit = true;
        }
        else
            percent -= 20;

        // TODO: time is skipped
        DBWordFactory.getInstance( database, activeDict )
                .updatePercentAndTime( activeWord.getId(), percent, 0 );

        if( reinit )
            randomizer.init(  whereSTMT );
    }



    void compareWords( String word  )
    {
        if( activeWord.getWord().equalsIgnoreCase(word) )
            correct();
        else
            incorrect();
    }

    private void incorrect()
    {
    }

    private void showCorrectMeaning()
    {

    }

    private void showCorrectOption( View incorrectView )
    {
        incorrectView.setBackgroundColor(Color.RED);

        if( ((IWord)tv1.getTag()).getId() == activeWord.getId())
            tv1.setBackgroundColor( Color.LTGRAY );
        else if( ((IWord)tv2.getTag()).getId() == activeWord.getId())
            tv2.setBackgroundColor( Color.LTGRAY );
        else if( ((IWord)tv3.getTag()).getId() == activeWord.getId())
            tv3.setBackgroundColor( Color.LTGRAY );
        else if( ((IWord)tv4.getTag()).getId() == activeWord.getId())
            tv4.setBackgroundColor( Color.LTGRAY );
    }

    private void correct()
    {
        changeWord();
    }
}