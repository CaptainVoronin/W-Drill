package org.sc.w_drill;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.CircularArrayList;
import org.sc.w_drill.utils.DBPair;
import java.util.ArrayList;
import java.util.Calendar;

public class ActLearnWords extends ActionBarActivity
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
    TextView wordTranscription;
    TextView wordExample;
    WDdb database;
    ArrayList<WordTmpStats> wordStats;
    private boolean confirmed;
    private Button btnIKnow;
    private Button btnIDontKnow;
    long deltaTime = 0;
    Calendar start;
    CircularArrayList<IWord> words = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_learn_words);

        int dictId = getIntent().getIntExtra( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1 );
        database = new WDdb( getApplicationContext() );
        activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );

        int wordId = getIntent().getIntExtra( ActDictionaryEntry.UPDATED_WORD_ID_PARAM_NAME, -1 );

        wordPlace = ( TextView ) findViewById( R.id.word );
        wordMeaning = ( TextView ) findViewById( R.id.meaning );
        btnIDontKnow = ( Button ) findViewById( R.id.dont_know );
        wordTranscription = ( TextView ) findViewById( R.id.transcription );
        wordExample = ( TextView ) findViewById( R.id.examples );

        btnIDontKnow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processButtonPush(false);
            }
        });

        btnIKnow = ( Button ) findViewById( R.id.i_know );
        btnIKnow.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processButtonPush(true);
            }
        });

        if( wordId != -1 )
        {

        }

        getWordsSet();
        if( words != null )
        {
            IWord word = words.next();
            if( word != null )
                bringWordToScreen( word );
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_learn_words_menu, menu);
        boolean res = super.onCreateOptionsMenu( menu );
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
        } else if( id == R.id.action_edit )
        {
            Intent intent = new Intent( ActLearnWords.this, ActDictionaryEntry.class );
            intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
            intent.putExtra( DBWordFactory.WORD_ID_VALUE_NAME, activeWord.getId() );
            intent.putExtra( ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.ADD_WORDS );
            startActivityForResult(intent, MainActivity.CODE_ActDictionaryEntry);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getWordsSet()
    {
        WordTmpStats stat;

        // TODO: There should be a limit of rows. Now it's the constant value - 10
        ArrayList<IWord> wrd = DBWordFactory.getInstance( database, activeDict ).getWordsToLearn( 10 );

        if( wrd == null )
        {
            words = null;
            showMessageAndExit(getString(R.string.txt_no_words_to_learn));
        }
        else {
            words = new CircularArrayList( wrd);

            wordStats = new ArrayList<WordTmpStats>();
            for (IWord w : wrd)
            {
                stat = new WordTmpStats(w.getId());
                stat.avgTime = w.getAvgTime();
                wordStats.add(stat);
            }
        }
    }

    private void showMessageAndExit(String string)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( string ).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    private void processButtonPush( boolean success )
    {

        if (confirmed)
        {
            // A user has confirmed his action
            // So we must write new percent into database
            WordTmpStats stat = null;

            for( WordTmpStats s : wordStats )
                if( s.id == activeWord.getId() )
                {
                    stat = s;
                    break;
                }

            int percent = activeWord.getLearnPercent();
            stat.attempts++;

            if( success )
            {
                if( percent + 20 > 100 )
                    percent = 100;
                else
                    percent += 20;
            }
            else
            {
                if( percent - 20 < 0 )
                    percent = 0;
                else
                    percent -= 20;
                stat.attempts--;
            }

            int time = ( int ) ( Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis() );

            time = Math.round( ( activeWord.getAvgTime() + time ) / ( activeWord.getAccessCount() + 1 ) );
            activeWord.setLearnPercent( percent );
            DBWordFactory.getInstance( database, activeDict )
                    .updatePercentAndTime( activeWord.getId(), activeWord.getLearnPercent(), time );

            // TODO: there should be some rule to remove word from list
            // 'cause it's impossible repeate learning five time
            if ( stat.attempts >= 1 )
            {
                if( words.remove( activeWord ) )
                    Log.d("[ActLearnWords::getNextWord]", "Word id " + activeWord.getId() + " was removed"  );
                else
                    Log.e("[ActLearnWords::getNextWord]", "Word id " + activeWord.getId() + " wasn't found in list!"  );

                if( wordStats.remove( stat ) )
                    Log.d("[ActLearnWords::getNextWord]", "Stat id " + activeWord.getId() + " was removed"  );
                else
                    Log.e("[ActLearnWords::getNextWord]", "Stat id " + activeWord.getId() + " wasn't found in list!");
            }

            // ...and get a new word for learning

            if ( words.size() != 0 )
            {
                IWord word = words.next();
                // There is another one word
                // Set buttons to default state
                btnIKnow.setText(getString(R.string.i_know));
                btnIDontKnow.setText(getString(R.string.dont_know));

                // Bring the new word to the screen
                bringWordToScreen(word);
                confirmed = false;
            }
            else
            {
                // The current word set is empty
                // Take the next set from DB
                getWordsSet();

                if( words == null || words.size() == 0 ) {
                    // If there are words for learning
                    // we'll make a transition.
                    if( DBDictionaryFactory.getInstance( database ).getWordsTo( activeDict.getId(), DBDictionaryFactory.STAGE_CHECK ) != 0 )
                        showWhatToDoDialog();
                    else
                    {
                        showNothingToDoDialog();
                    }
                }
                else
                    processButtonPush( success );
            }
        } else {
            showMeaning();
            if( success )
                btnIKnow.setText( getString( R.string.go_next));
            else
                btnIDontKnow.setText( getString( R.string.go_next));
            confirmed = true;
        }
    }

    private void showNothingToDoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.nothing_to_do ).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                finish();
            }
        });

        builder.setCancelable(true);
        builder.create();
        builder.show();

    }

    private void showWhatToDoDialog()
    {
        // TODO: This dialog can be shown if there are a words for checking

        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.no_more_words ).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                finish();
            }
        }).setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                Intent intent = new Intent( ActLearnWords.this, ActCheckWords.class );
                intent.putExtra( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId() );
                startActivity( intent );
            }
        });

        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    void bringWordToScreen( IWord word )
    {
        clearMeaning();
        activeWord = word;
        wordPlace.setText( activeWord.getWord() );

        if( activeWord.getTranscription() != null && activeWord.getTranscription().length() != 0 )
            wordTranscription.setTag( activeWord.getTranscription() );

        StringBuilder buffer = new StringBuilder();

        if( activeWord.meanings() != null )
        {
            for (IMeaning m : activeWord.meanings())
            {
                if (m.examples() != null)
                    for (DBPair pair : m.examples())
                        buffer.append(pair.getValue()).append("\n");
            }
            wordExample.setText(buffer.toString());
        }

        start = Calendar.getInstance();
    }

    private void clearMeaning()
    {
        wordMeaning.setText( "[...]" );
        wordTranscription.setText("");
        wordExample.setText("");
    }

    void showMeaning()
    {
        wordMeaning.setText( activeWord.meanings().get(0).meaning() );
    }

    public IWord getActiveWord( )
    {
        return activeWord;
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        switch( requestCode )
        {
            case MainActivity.CODE_ActDictionaryEntry:
                if ( resultCode == ActDictionaryEntry.RESULT_WORD_UPDATED  )
                {
                    int id = data.getIntExtra( DBWordFactory.WORD_ID_VALUE_NAME, -1 );
                    if( id == activeWord.getId() );
                        updateWord( );
                }
                break;
            default:
                break;
        }
    }

    private void updateWord()
    {
        if( words!= null )
        {
            int index = words.indexOf(activeWord);

            activeWord = DBWordFactory.getInstance(database, activeDict).getWordEx(activeWord.getId());
            words.set( index, activeWord );

            bringWordToScreen(activeWord);
        }
    }

    class WordTmpStats
    {
        public int id;

        public WordTmpStats( int _id )
        {
            id = _id;
            avgTime = 0;
            attempts = 0;
            faults = 0;
        }

        public int avgTime;
        public int attempts;
        public int faults;
    }
}