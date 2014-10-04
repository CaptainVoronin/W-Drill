package org.sc.w_drill;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;


public class MainActivity extends ActionBarActivity
{

    public static final int CODE_ActNewDictionary  = 0;
    public static final int CODE_ActDictionaryList = 1;

    /**
     * База данных приложения
     */
    private WDdb database;


    /**
     * Фабрика словарей
     */
    DBDictionaryFactory dictionaryFactory;

    /**
     * Активный словарь
     */
    private Dictionary activeDict;

    ActionBar actionBar = null;

    SharedPreferences sharedPrefs;

    NoDictionaryFragment noDictFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int activeDictID = -1;

        sharedPrefs = getPreferences(Activity.MODE_PRIVATE);

        if( sharedPrefs != null )
            activeDictID = sharedPrefs.getInt( ST_ACTIVE_DICTIONARY_ID, -1 );

        /*******************************************
         *
         * Плохой кусок
         *
         ********************************************/
        database = new WDdb( getApplicationContext()  );

        database.getWritableDatabase();
        detectState( activeDictID );
    }

    private void detectState( int activeDictID )
    {
        // Are there
        dictionaryFactory = DBDictionaryFactory.getInstance( database );
        ArrayList<Dictionary> dictList = dictionaryFactory.getList();

        // It's possible when no dictionaries in database
        if( dictList == null || dictList.size() == 0 )
            prepareClearInterface();
        else {
            // In case of a mistake when active dictionary
            // has been deleted from DB
            boolean activeDictExistsInDB = false;
            if( activeDictID != -1 ) {
                for( Dictionary dict : dictList )
                    if( dict.getId() ==  activeDictID  )
                    {
                        activeDictExistsInDB = true;
                        activeDict = dict;
                        break;
                    }
                if( activeDictExistsInDB )
                    restoreState(activeDictID); // yes, it was deleted
                else
                    goToDictionaryList(); // no, it wasn't
            }
            else
                goToDictionaryList(); // no active dictionary
        }
    }

    /**
     * Go to the dictionary list due to
     * dictionary isn't selected
     */
    private void goToDictionaryList()
    {

        FragmentManager fragmentManager = getFragmentManager();

        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();

        ChooseDictionaryFragment fragment = new ChooseDictionaryFragment();

        fragmentTransaction.replace( android.R.id.content, fragment );

        fragmentTransaction.commit();

    }

    /**
     * Эта функция создает инфтерфейс и восстанавливаетс
     * состояние программы
     * @param activeDictID
     */
    private void restoreState(int activeDictID)
    {
        // Set up the action bar.
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        if( activeDictID != -1 )
        {
            setupActiveDict( activeDictID );
        }
    }

    /**
     * Эта функция вызывается в том случае,
     * если в БД нет ни одного словаря.
     * Она сразу кидает на страницу создания словаря.
     */
    private void prepareClearInterface()
    {
        FragmentManager fragmentManager = getFragmentManager();

        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();

        noDictFragment = new NoDictionaryFragment();

        fragmentTransaction.replace( android.R.id.content, noDictFragment );

        fragmentTransaction.commit();

        /* */
    }

    private ArrayList<Dictionary> getDictList()
    {
        return dictionaryFactory.getList();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch ( id )
        {
            case R.id.action_settings:
                return true;
            case R.id.action_dict_list:
            {
                Intent intent = new Intent(this, ActDictionaryList.class);
                startActivityForResult(intent, CODE_ActDictionaryList);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        int id;
        switch( requestCode )
        {
            case CODE_ActNewDictionary:
            case CODE_ActDictionaryList:
                if( data != null )
                    id = data.getIntExtra( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1 );
                else
                    id = -1;
                    detectState( id );
                break;
            default:
                break;
        }
    }

    private void setupActiveDict(int dictID)
    {
        try
        {
            activeDict = dictionaryFactory.getDictionaryById(dictID);
            getSupportActionBar().setTitle(getString(R.string.current_dict_info, activeDict.getName(), activeDict.getLang()));

            FragmentManager fragmentManager = getFragmentManager();

            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();

            ActiveDictionaryStateFragment fragment = new ActiveDictionaryStateFragment();

            fragmentTransaction.replace( android.R.id.content, fragment );

            fragmentTransaction.commit();

        }
        catch( Exception e )
        {
            showAlert( e.getMessage() );
        }
    }


    /**
     * This realises a transition to an entry of the dictionary
     * which is pointed by the first parameter.
     * @param dictId
     * @param entryPart - a kind of entry. 0 - a whole word list, 1 - words to learn
     *                  2 - words to study, 3 - add new words
     */
    protected void goToDictionaryEntry( int dictId, int entryPart )
    {
        Intent intent = new Intent( this, ActDictionaryEntry.class );
        intent.putExtra( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, dictId );
        intent.putExtra( ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, entryPart );
        startActivity( intent );
    }

    protected void showAlert( String message )
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage(message).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        builder.setCancelable( true );
        builder.create();
        builder.show();
    }

    public static final String ST_ACTIVE_DICTIONARY_ID = "ST_ACTIVE_DICTIONARY_ID";

    @Override
    protected void onPause()
    {
        super.onPause();

        SharedPreferences.Editor ed = sharedPrefs.edit();

        if( activeDict != null )
            ed.putInt(ST_ACTIVE_DICTIONARY_ID, activeDict.getId() );
        else
            ed.putInt(ST_ACTIVE_DICTIONARY_ID, -1);
        ed.commit();
    }

    /**
     * This fragment is used when ni one dictionary in DB
     */
    class NoDictionaryFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {

            // Inflate the layout for this fragment
            View view =  inflater.inflate(R.layout.no_dicts_fragment, container, false);
            TextView text = ( TextView ) view.findViewById( R.id.msg_goto_new_dictionary );
            text.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent( MainActivity.this, ActNewDictionary.class);
                    intent.putExtra( ActNewDictionary.CANCELABLE_PAR_NAME, false );
                    startActivityForResult(intent, CODE_ActNewDictionary);
                }
            });
            return view;
        }

        public void onActivityResult (int requestCode, int resultCode, Intent data)
        {
            MainActivity.this.onActivityResult( requestCode, resultCode, data );
        }
    }

    /**
     * THis fragment is used when there are dictionaries but
     * no one active
     */
    class ChooseDictionaryFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {

            // Inflate the layout for this fragment
            View view =  inflater.inflate(R.layout.choose_dicts_fragment, container, false);
            TextView text = ( TextView ) view.findViewById( R.id.msg_have_to_choose_dict );
            text.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent( MainActivity.this, ActDictionaryList.class);
                    if( activeDict != null )
                        intent.putExtra( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId() );
                    else
                        intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
                    startActivityForResult(intent, CODE_ActDictionaryList);
                }
            });
            return view;
        }
        public void onActivityResult (int requestCode, int resultCode, Intent data)
        {
            MainActivity.this.onActivityResult( requestCode, resultCode, data );
        }
    }

    class ActiveDictionaryStateFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState)
        {
            View view;

            // Inflate the layout for this fragment
            if( activeDict.getWordCount() != 0 )
                view =  inflater.inflate(R.layout.active_dict_state_fragment, container, false);
            else
                view =  inflater.inflate(R.layout.active_dict_no_words_fragment, container, false);

            TextView text = ( TextView ) view.findViewById( R.id.dict_name );
            text.setText( activeDict.getName() );

            /**
             * Id the active dictionary has words at all
             */
            if( activeDict.getWordCount() != 0 )
            {
                text = ( TextView ) view.findViewById( R.id.word_count );
                text.setText( MainActivity.this.getResources().getString( R.string.words_total, activeDict.getWordCount() ) );
                text.setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d( "[MainActivity]", "Go to a full list of words");
                    }
                });

                int cnt = DBDictionaryFactory.getInstance(MainActivity.this.database)
                        .getWordsTo(activeDict.getId(), DBDictionaryFactory.STAGE_LEARN);

                text = (TextView) view.findViewById(R.id.words_to_learn);
                text.setText(MainActivity.this.getResources().getString(R.string.words_to_learn, cnt));

                text.setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d( "[MainActivity]", "Go to learn new words");
                        goToDictionaryEntry( activeDict.getId(), ActDictionaryEntry.WORDS_TO_LEARN );
                    }
                });

                cnt = DBDictionaryFactory.getInstance(MainActivity.this.database)
                        .getWordsTo(activeDict.getId(), DBDictionaryFactory.STAGE_CHECK);

                text = (TextView) view.findViewById(R.id.words_to_check);
                text.setText(MainActivity.this.getResources().getString(R.string.words_to_check, cnt));

                text.setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d( "[MainActivity]", "Go to check words");
                        goToDictionaryEntry( activeDict.getId(), ActDictionaryEntry.WORDS_TO_STUDY );


                    }
                });

                text = (TextView) view.findViewById(R.id.add_words);
                text.setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d( "[MainActivity]", "Go to add words");
                        goToDictionaryEntry( activeDict.getId(), ActDictionaryEntry.ADD_WORDS );
                    }
                });

                text = (TextView) view.findViewById(R.id.edit_dict);
                text.setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d( "[MainActivity]", "Go to dictionary entry");
                        goToDictionaryEntry( activeDict.getId(), ActDictionaryEntry.WHOLE_LIST_ENTRY );
                    }
                });


            }
            else
            {
                text = (TextView) view.findViewById(R.id.dict_has_to_be_supplemented_with_words);
                text.setText(MainActivity.this.getResources().getString(R.string.dict_has_to_be_supplemented_with_words));
                text.setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d( "[MainActivity]", "The dictionary has to be supplemented with words");
                        goToDictionaryEntry( activeDict.getId(), ActDictionaryEntry.ADD_WORDS );
                    }
                });
            }

            return view;
        }
    }
}