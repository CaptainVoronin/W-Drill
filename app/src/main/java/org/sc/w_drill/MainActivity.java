package org.sc.w_drill;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.ActiveDictionaryStateFragment;
import org.sc.w_drill.utils.TextHelper;
import org.sc.w_drill.utils.datetime.DateTimeUtils;
import org.sc.w_drill.utils.Langs;
import org.sc.w_drill.utils.image.ImageConstraints;

public class MainActivity extends ActionBarActivity implements DlgDictionary.OnDictionaryOkClickListener
{
    public static final int CODE_ActDictionaryList = 1;
    public static final int CODE_ActDictionaryEntry = 2;
    public static final int CODE_ActSettings = 3;
    private static final int CODE_ActLearnWords = 4;

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

    ActiveDictionaryStateFragment activeDictionaryStateFragment;
    LinearLayout rootView;
    View currentView;
    ImageConstraints imageConstraints;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set uncough exception handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread thread, Throwable e)
            {
                handleUncaughtException(thread, e);
            }
        });

        rootView = (LinearLayout) findViewById(R.id.rootLayout);

        int activeDictID = -1;

        sharedPrefs = getPreferences(Activity.MODE_PRIVATE);

        if (sharedPrefs != null)
            activeDictID = sharedPrefs.getInt(ST_ACTIVE_DICTIONARY_ID, -1);

        imageConstraints = ImageConstraints.getInstance(this);

        /*******************************************
         *
         * Плохой кусок
         *
         ********************************************/
        database = new WDdb(getApplicationContext());

        SQLiteDatabase db = database.getWritableDatabase();
        db.close();
        detectState(activeDictID);
    }

    private void handleUncaughtException(Thread thread, Throwable e)
    {
        try
        {
            e.printStackTrace();
            File dir = new File(Environment.getExternalStorageDirectory() +
                    File.separator + "Scholar");

            if (!dir.exists())
                dir.mkdirs();

            File f = new File(dir.getPath() + File.separator + DateTimeUtils.mkFilenameForTime()
                    + ".stacktrace");
            f.createNewFile();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            FileWriter fw = new FileWriter(f);
            fw.write(sw.toString());
            fw.close();

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.exit(1);
    }

    private void detectState(int activeDictID)
    {
        // Are there
        dictionaryFactory = DBDictionaryFactory.getInstance(database);
        ArrayList<Dictionary> dictList = dictionaryFactory.getList();

        if (currentView != null)
            rootView.removeView(currentView);

        // It's possible when no dictionaries in database
        if (dictList == null || dictList.size() == 0)
            currentView = createNoDictionaryInterface();
        else
        {
            // In case of a mistake when the active dictionary
            // has been deleted from DB
            boolean activeDictExistsInDB = false;
            if (activeDictID != -1)
            {
                for (Dictionary dict : dictList)
                    if (dict.getId() == activeDictID)
                    {
                        activeDictExistsInDB = true;
                        activeDict = dict;
                        break;
                    }
                if (activeDictExistsInDB)
                    currentView = restoreState(); // yes, it was deleted
                else
                    currentView = createChooseDictionaryInterface(); // no, it wasn't
            }
            else
                currentView = createChooseDictionaryInterface(); // no active dictionary
        }

        rootView.addView(currentView);
    }

    /**
     * Эта функция создает инфтерфейс и восстанавливаетс
     * состояние программы
     */
    View restoreState()
    {
        // Set up the action bar.
        actionBar = getSupportActionBar();
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        return setupActiveDict();
    }

    private ArrayList<Dictionary> getDictList()
    {
        return dictionaryFactory.getList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        switch (id)
        {
            case R.id.action_settings:
                intent = new Intent(this, ActSettings.class);
                startActivityForResult(intent, CODE_ActSettings);
                return true;
            case R.id.action_dict_list:
            {
                intent = new Intent(this, ActDictionaryList.class);
                startActivityForResult(intent, CODE_ActDictionaryList);
            }
            break;
            case R.id.action_about:
            {
                intent = new Intent(this, ActAbout.class);
                startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNewDictionaryDialog()
    {
        DlgDictionary dlg = new DlgDictionary(this);
        dlg.setOkListsner(this);
        dlg.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        int id;
        switch (requestCode)
        {
            case CODE_ActDictionaryList:
                if (data != null)
                    id = data.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
                else
                {
                    if (activeDict == null)
                        id = -1;
                    else
                        id = activeDict.getId();
                }
                detectState(id);
                break;
            case CODE_ActDictionaryEntry:
                if (data != null)
                {
                    // Here we are interested in changing of a dictionary
                    if (resultCode == ActDictionaryEntry.DICTIONARY_CHANGED)
                    {
                        id = data.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
                        detectState(id);
                    }
                }
                break;
            case CODE_ActLearnWords:
                if (resultCode == Activity.RESULT_OK)
                    detectState(activeDict.getId());
                break;
            default:
                break;
        }
    }

    View setupActiveDict()
    {

        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.active_dict_state_fragment, rootView, false);

        DBDictionaryFactory.getInstance(database).getAdditionalInfo(activeDict);

        // Set the active dictionary name
        TextView text = (TextView) view.findViewById(R.id.dict_name);
        text.setText(activeDict.getName());

        // Set the dictionary lang
        text = (TextView) view.findViewById(R.id.tvDictLang);
        text.setText(Langs.getInstance(this).get(activeDict.getLang()));

        // Set last access time
        text = ( TextView ) view.findViewById( R.id.tvLastAccess );

        if( activeDict.getLastAccess() == null )
            text.setText( getString( R.string.txt_last_access_date, getString( R.string.txt_never )));
        else
        {
            String message = DateTimeUtils.timeIntervalToString( this, activeDict.getLastAccess() );
            text.setText( getString(R.string.txt_last_access_date,message));
        }

        /**************************************
         *
         * Set the word count
         *
         * ************************************/

        text = (TextView) view.findViewById(R.id.word_count);
        text.setText(Integer.valueOf(activeDict.getWordCount()).toString());

        if (activeDict.getWordCount() != 0)
        {
            OnTotalWordsClick handler = new OnTotalWordsClick();
            text.setOnClickListener(handler);
            text = (TextView) view.findViewById(R.id.total_words_label);
            text.setOnClickListener(handler);
        }

        /**************************************
         *
         * Set the words-to-learn count
         *
         * ************************************/

        TextView label = (TextView) view.findViewById(R.id.words_to_learn_label);
        TextView counter = (TextView) view.findViewById(R.id.words_for_learn);

        if (activeDict.getWordsToLearn() != 0)
        {
            label.setText(R.string.txt_words_for_learn);
            counter.setText(Integer.valueOf(activeDict.getWordsToLearn()).toString());

            label.setOnClickListener(new OnLearnWordsClick());
            counter.setOnClickListener(new OnLearnWordsClick());
        }
        else
        {
            label.setText(R.string.txt_no_words_to_learn);
            counter.setText("");
        }
        //text.setPaintFlags(text.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        /**************************************
         *
         * Set the words-to-check count
         *
         * ************************************/

        counter = (TextView) view.findViewById(R.id.words_for_check);
        label = (TextView) view.findViewById(R.id.words_to_check_label);

        if (activeDict.getWordsToCheck() != 0)
        {
            label.setText(R.string.txt_words_for_check);
            counter.setText(Integer.valueOf(activeDict.getWordsToCheck()).toString());
            label.setOnClickListener(new OnCheckClick());
            counter.setOnClickListener(new OnCheckClick());

        }
        else
        {
            label.setText(R.string.txt_no_words_for_check);
            counter.setText("");
        }

        /**************************************
         *
         * Add the "add words" link
         *
         * ************************************/

        text = (TextView) view.findViewById(R.id.add_words);
        text.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(MainActivity.this, ActDictionaryEntry.class);
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
                intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.ADD_WORDS);
                startActivityForResult(intent, CODE_ActDictionaryEntry);
            }
        });

        return view;

    }

    /**
     * This realises a transition to an entry of the dictionary
     * which is pointed by the first parameter.
     *
     * @param dictId
     * @param entryPart - a kind of entry. 0 - a whole word list, 1 - words to learn
     *                  2 - words to study, 3 - add new words
     */
    public void goToDictionaryEntry(int dictId, int entryPart)
    {
        Intent intent = new Intent(this, ActDictionaryEntry.class);
        intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, dictId);
        Log.d("[MainActivity::goToDictionaryEntry]", "Start ActDictionaryEntry for part " + entryPart + ", dict ID " + dictId);
        intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, entryPart);
        startActivity(intent);
    }

    protected void showAlert(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // User cancelled the dialog
            }
        });

        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    public static final String ST_ACTIVE_DICTIONARY_ID = "ST_ACTIVE_DICTIONARY_ID";

    @Override
    protected void onPause()
    {
        super.onPause();

        SharedPreferences.Editor ed = sharedPrefs.edit();

        if (activeDict != null)
            ed.putInt(ST_ACTIVE_DICTIONARY_ID, activeDict.getId());
        else
            ed.putInt(ST_ACTIVE_DICTIONARY_ID, -1);
        ed.commit();
    }

    /**
     * It crates an interface in the case when there aren't
     * a dictionaries in DB
     *
     * @return
     */
    View createNoDictionaryInterface()
    {
        // Inflate the layout for this fragment
        View view = getLayoutInflater().inflate(R.layout.fragment_no_dicts, rootView, false);
        TextView text = (TextView) view.findViewById(R.id.tvPressToCreateDictionary);
        text.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showNewDictionaryDialog();
            }
        });
        return view;
    }

    /**
     * It crates an interface in the case when there isn't
     * a choosen dictionary
     *
     * @return
     */
    View createChooseDictionaryInterface()
    {
        LayoutInflater inflater = getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_choose_dicts, rootView, false);
        TextView text = (TextView) view.findViewById(R.id.txt_press_to_choose_dict);
        text.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(MainActivity.this, ActDictionaryList.class);
                if (activeDict != null)
                    intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
                else
                    intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
                startActivityForResult(intent, CODE_ActDictionaryList);
            }
        });

        return view;
    }

    @Override
    public void onNewDictOkClick(int dictId)
    {
        detectState(dictId);
    }

    class OnLearnWordsClick implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            Intent intent = new Intent(MainActivity.this, ActLearnWords.class);
            if (activeDict != null)
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
            else
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);

            intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.WORDS_TO_STUDY);

            startActivityForResult(intent, CODE_ActLearnWords);
        }
    }

    private class OnCheckClick implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            Intent intent = new Intent(MainActivity.this, ActCheckWords.class);
            if (activeDict != null)
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
            else
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);

            intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.WORDS_TO_LEARN);

            startActivity(intent);
        }
    }

    private class OnTotalWordsClick implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            Intent intent = new Intent(MainActivity.this, ActDictionaryEntry.class);
            if (activeDict != null)
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
            else
                intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);

            intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.WHOLE_LIST_ENTRY);

            startActivityForResult(intent, CODE_ActDictionaryEntry);
        }
    }
}