package org.sc.w_drill;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.db_wrapper.RandomArrayUniqWords;
import org.sc.w_drill.db_wrapper.RandomizerEmptyException;
import org.sc.w_drill.db_wrapper.RandomizerException;
import org.sc.w_drill.db_wrapper.WordRandomizer;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.ArrayListRandomizer;

import java.util.ArrayList;


public class ActCheckWords extends ActionBarActivity {

    private boolean userGaveUp;

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
    ArrayListRandomizer<IMeaning> meaningRandomizer;
    TextView tv1, tv2, tv3, tv4;
    boolean missed = false;
    EditText edWordAnswer = null;
    Button btnIDontKnow;

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
        try {
            randomizer.init(whereSTMT);
        }
        catch( RandomizerEmptyException ex )
        {
            showErrorEndExit( "Нет слов для проверки по графику" );
            return;
        }
        subset = new ArrayList<IWord>();
        arrayRandomizer = new ArrayListRandomizer<IWord>();
        meaningRandomizer = new ArrayListRandomizer<IMeaning>();
        changeWord();
    }

    private void fatalError()
    {

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
        try {
            activeWord = getWord();
            if (activeView != null)
                rootView.removeView(activeView);
            activeView = getView();
            rootView.addView(activeView);
        } catch( RandomizerException ex )
        {
            showErrorEndExit(getString(R.string.msg_not_enough_words_for_check));
        }
        catch ( Exception ex )
        {
            showErrorEndExit(ex.getMessage());
        }
    }

    private void showErrorEndExit(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage(message).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        builder.setCancelable( true );
        builder.create();
        builder.show();

    }

    private View getView() throws RandomizerException {
        if( activeWord.getLearnPercent() >= 200 )
        {
            mode = Mode.COMPARE;
            return getWriteWordView();
        }
        else
        {
            mode = Mode.CHOISE;
            return getChooseOptionView();
        }
    }

    private IWord getWord()
    {
        if( activeWord != null )
            return randomizer.getRandomWord( activeWord );
        else
            return randomizer.getRandomWord( );
    }

    View getChooseOptionView() throws RandomizerException {

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

        subset = RandomArrayUniqWords.make(subset, randomizer, 4);

        TextView tv = ( TextView ) chooseOptionView.findViewById( R.id.word_for_check );
        tv.setText( activeWord.getWord() );

        setText( subset.get(0), tv1 );
        setText( subset.get(1), tv2 );
        setText( subset.get(2), tv3 );
        setText( subset.get(3), tv4 );

        return chooseOptionView;
    }

    void setText( IWord word, TextView tv )
    {
        if( word.meanings().size() > 1 )
            tv.setText( meaningRandomizer.getRandomItem( word.meanings()).meaning() );
        else
            tv.setText( word.meanings().get(0).meaning() );
        tv.setTag( word );
    }

    View getWriteWordView()
    {
        enterWordView = (LinearLayout) getLayoutInflater().inflate(R.layout.fragment_act_check_words_enter_word, null);
        TextView tv = ( TextView ) enterWordView.findViewById( R.id.idMeaning );
        tv.setText( activeWord.meanings().get(0).meaning());
        tv = ( TextView ) enterWordView.findViewById( R.id.tvWord );
        tv.setText( "" );

        btnIDontKnow = ( Button ) enterWordView.findViewById( R.id.dont_know );
        btnIDontKnow.setText( "?" );
        btnIDontKnow.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if( !userGaveUp)
                {
                    userGaveUp = true;
                    btnIDontKnow.setText( ">>" );
                    showCorrectMeaning();
                }
                else
                {
                    userGaveUp = false;
                    btnIDontKnow.setText( "?" );
                    decreasePercent( activeWord );
                    changeWord();
                }
            }
        });

        edWordAnswer = ( EditText ) enterWordView.findViewById( R.id.word_answer );

        edWordAnswer.setTag( activeWord.getWord() );
        edWordAnswer.setText( "" );

        edWordAnswer.setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if( keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER )
                {
                    String word = edWordAnswer.getText().toString();
                    compareWords( word );
                    return true;
                }
                return false;
            }
        });
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

        try {
            if (reinit)
                randomizer.init(whereSTMT);
        } catch( RandomizerEmptyException ex )
        {
            showErrorEndExit( getString( R.string.txt_no_words_to_check ) );
        }
    }

    void compareWords( String word )
    {
        if( activeWord.getWord().equalsIgnoreCase(word) )
            correct();
        else
            incorrect();
    }

    private void incorrect()
    {
        decreasePercent( activeWord );
        showCorrectMeaning();
    }

    private void showCorrectMeaning()
    {
        TextView tv = ( TextView ) enterWordView.findViewById( R.id.tvWord );
        tv.setText( activeWord.getWord() );
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