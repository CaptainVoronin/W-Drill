package org.sc.w_drill;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IWord;

import java.util.ArrayList;
import java.util.Calendar;

public class LearnWordsFragment extends Fragment
{

    /*public static final int LEARN_WORDS = 0;
    public static final int CHECK_WORDS_WORDS_FROM_SET = 1;
    public static final int CHECK_WORDS_WORDS_FROM_DICT = 2; */
    //public static final String LEARN_KIND = "LEARN_KIND";
    /**
     * It's the learning mode indicator.
     * 0 - indicates that there are a limited set of words.
     * 1 - check a whole entire if a dictionary.
     */
    //public static final int LearnMode = 0;

    Dictionary activeDict;
    IBaseWord.LearnState learnStage;
    IWord activeWord;
    TextView wordPlace;
    TextView wordMeaning;
    WDdb database;

    private OnLearnWordsFragmentListener mListener;

    ArrayList<IWord> words;
    int position = 0;
    Dictionary dict;
    private boolean confirmed;
    private Button btnIKnow;
    private Button btnIDontKnow;
    long deltaTime = 0;
    Calendar start;

    public static LearnWordsFragment newInstance( int dictId )
    {
        LearnWordsFragment fragment = new LearnWordsFragment();
        Bundle args = new Bundle();
        args.putInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, dictId );
        fragment.setArguments( args );
        return fragment;
    }

    public LearnWordsFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {
        int dictId = getArguments().getInt( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME );
        database = new WDdb( getActivity().getApplicationContext() );
        dict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_learn_words, container, false);
        wordPlace = ( TextView ) view.findViewById( R.id.word );
        wordMeaning = ( TextView ) view.findViewById( R.id.meaning );
        btnIDontKnow = ( Button ) view.findViewById( R.id.dont_know );
        btnIDontKnow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processButtonPush(false);
            }
        });

        btnIKnow = ( Button ) view.findViewById( R.id.i_know );
        btnIKnow.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processButtonPush(true);
            }
        });

        getWordsSet();
        if( words != null )
        {
            IWord word = getNextWord();
            if( word != null )
                bringWordToScreen( word );
        }
        return view;
    }

    private void getWordsSet()
    {
        // TODO: There should be a limit of rows. Now it's the constant value - 10
        words = DBWordFactory.getInstance( database, dict ).getWordsToLearn( 10 );
        position = 0;
    }

    private void processButtonPush( boolean success )
    {

        if (confirmed)
        {
            // A user has confirmed his action
            // So we must write new percent into database
            int percent = activeWord.getLearnPercent();
            if( success )
            {
                if( percent + 20 > 100 )
                    percent = 100;
                else
                    percent += 20;
            }
            else
            {
                if( percent - 20 > 0 )
                    percent = 0;
                else
                    percent -= 20;
            }

            int time = ( int ) ( Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis() );

            time = Math.round( ( activeWord.getAvgTime() + time ) / ( activeWord.getAccessCount() + 1 ) );
            activeWord.setLearnPercent( percent );
            DBWordFactory.getInstance( database, dict )
                    .updatePercentAndTime( activeWord.getId(), activeWord.getLearnPercent(), time );

            // ...and get a new word for learning
            IWord word = getNextWord();
            if (word != null)
            {
                // There is another one word
                // Set buttons to default state
                btnIKnow.setText(getActivity().getApplicationContext().getString(R.string.i_know));
                btnIDontKnow.setText(getActivity().getApplicationContext().getString(R.string.dont_know));

                // Bring the new word to the screen
                bringWordToScreen(word);
                confirmed = false;
            }
            else
            {
                // The current word set is empty
                // Take the next set from DB
                getWordsSet();

                if( words == null && words.size() == 0 )
                    showWhatToToDialog();
                else
                    processButtonPush( success );
            }
        } else {
            showMeaning();
            if( success )
                btnIKnow.setText( getActivity().getApplicationContext().getString( R.string.go_next));
            else
                btnIDontKnow.setText( getActivity().getApplicationContext().getString( R.string.go_next));
            confirmed = true;
        }
    }

    private void showWhatToToDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setMessage( R.string.no_more_words ).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        }).setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {

            }
        });

        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLearnWordsFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnLearnWordsFragmentListener
    {
        public void wordsStageChanged();
    }

    void bringWordToScreen( IWord word )
    {
        clearMeaning();
        activeWord = word;
        wordPlace.setText( activeWord.getWord() );
        start = Calendar.getInstance();
    }

    private void clearMeaning()
    {
        wordMeaning.setText( "[...]" );
    }

    IWord getNextWord()
    {
        if( position + 1 > words.size() )
            return null;
        else
            return words.get( position++ );
    }

    void showMeaning()
    {
        wordMeaning.setText( activeWord.meanings().get(0).meaning() );
    }

    public IWord getActiveWord( )
    {
        return activeWord;
    }
}