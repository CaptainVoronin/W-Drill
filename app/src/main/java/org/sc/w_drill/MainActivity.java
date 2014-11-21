package org.sc.w_drill;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DefaultDictionary;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.utils.Langs;
import org.sc.w_drill.utils.MessageDialog;
import org.sc.w_drill.utils.datetime.DateTimeUtils;
import org.sc.w_drill.utils.image.ImageConstraints;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

public class MainActivity extends ActionBarActivity implements DlgDictionary.OnDictionaryOkClickListener
{
    public static final int CODE_ActDictionaryList = 1;
    public static final int CODE_ActDictionaryEntry = 2;
    public static final int CODE_ActSettings = 3;
    private static final int CODE_ActLearnWords = 4;

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
    LinearLayout rootView, gridNest;
    View currentView;
    ImageConstraints imageConstraints;
    CheckDictStateTask checkStateTask;
    DictStateHandler stateHandler;
    private View statView;

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

        rootView = (LinearLayout) findViewById(R.id.theBigNest);
        gridNest = (LinearLayout) findViewById(R.id.gridNest);

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
        SQLiteDatabase db = WDdb.getInstance(getApplicationContext()).getWritableDatabase();

        stateHandler = new DictStateHandler();
        checkStateTask = null;

        checkDefaultDictionary();

        try
        {
            detectState(activeDictID);
        }
        catch (Exception e)
        {
            showFatalError();
            exit(-1);
        }
    }

    private void showFatalError()
    {

    }

    private void checkDefaultDictionary()
    {
        DefaultDictionary defDict = DefaultDictionary.getInstance(this);
        defDict.init();
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
            Log.e("MainActivity:handleUncaughtException", sw.toString());
            fw.write(sw.toString());
            fw.close();

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        exit(1);
    }

    private void detectState(int activeDictID)
    {
        // Firstly I kill the state task if it exists
        if (checkStateTask != null)
            if (!checkStateTask.isCancelled())
                checkStateTask.cancel(true);

        checkStateTask = null;

        // Are there
        dictionaryFactory = DBDictionaryFactory.getInstance(this);

        ArrayList<Dictionary> dictList = null;
        try
        {
            dictList = dictionaryFactory.getList();
        }
        catch (SQLDataException e)
        {
            e.printStackTrace();
            showFatalError();
            exit(-1);
        }

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
        fillGridNest();
    }

    /**
     * Эта функция создает инфтерфейс и восстанавливаетс
     * состояние программы
     */
    View restoreState()
    {
        //TODO: Do I need this function?
        // Set up the action bar.
        actionBar = getSupportActionBar();
        return setupActiveDict();
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
            break;
            case R.id.action_add_word_quick:
                addWordQuick();
                break;
            case R.id.action_goto_quick_words:
                gotoQuickWords();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoQuickWords()
    {
        Intent intent = new Intent(this, ActDictionaryEntry.class);
        try
        {
            intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, DefaultDictionary.getInstance(this).getId());
            intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.WHOLE_LIST_ENTRY);
            startActivity(intent);
        }
        catch (SQLDataException e)
        {
            e.printStackTrace();
            showError(getString(R.string.txt_error_open_default_dictionary, e.getMessage()));
        }
    }

    private void showError(String message)
    {
        MessageDialog.showError(this, message, null, null);
    }

    private void addWordQuick()
    {
        DialogAddWordQuick dlg = new DialogAddWordQuick(this);
        dlg.show();
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

        statView = inflater.inflate(R.layout.active_dict_state_fragment, rootView, false);

        // Set the active dictionary name
        TextView text = (TextView) statView.findViewById(R.id.dict_name);
        text.setText(activeDict.getName());

        // Set the dictionary lang
        text = (TextView) statView.findViewById(R.id.tvDictLang);
        text.setText(Langs.getInstance(this).get(activeDict.getLang()));

        /**************************************
         *
         * Add the "add words" link
         *
         * ************************************/

        text = (TextView) statView.findViewById(R.id.add_words);
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

        CheckDictStateTaskParams params = new CheckDictStateTaskParams();
        params.context = this;
        params.dictId = activeDict.getId();

        checkStateTask = new CheckDictStateTask();
        checkStateTask.execute(new CheckDictStateTaskParams[]{params});

        return statView;

    }

    void setupStats(DictStats stats)
    {

        // Set last access time
        TextView text = (TextView) statView.findViewById(R.id.tvLastAccess);

        if (stats == null)
            text.setText(getString(R.string.txt_last_access_date, getString(R.string.txt_never)));
        else
        {
            String message = DateTimeUtils.timeIntervalToString(this, stats.lastAccess);
            text.setText(getString(R.string.txt_last_access_date, message));
        }

        /**************************************
         *
         * Set the word count
         *
         * ************************************/

        text = (TextView) statView.findViewById(R.id.word_count);
        text.setText(Integer.valueOf(stats.wordsTotal).toString());

        if (stats.wordsTotal != 0)
        {
            OnTotalWordsClick handler = new OnTotalWordsClick();
            text.setOnClickListener(handler);
            text = (TextView) statView.findViewById(R.id.total_words_label);
            text.setOnClickListener(handler);
        }

        /**************************************
         *
         * Set the words-to-learn count
         *
         * ************************************/

        TextView label = (TextView) statView.findViewById(R.id.words_to_learn_label);
        TextView counter = (TextView) statView.findViewById(R.id.words_for_learn);

        if (stats.wordsForLearn != 0)
        {
            label.setText(R.string.txt_words_for_learn);
            counter.setText(Integer.valueOf(stats.wordsForLearn).toString());

            label.setOnClickListener(new OnLearnWordsClick());
            counter.setOnClickListener(new OnLearnWordsClick());
        }
        else
        {
            label.setText(R.string.txt_no_words_to_learn);
            counter.setText("");
        }

        /**************************************
         *
         * Set the words-to-check count
         *
         * ************************************/

        counter = (TextView) statView.findViewById(R.id.words_for_check);
        label = (TextView) statView.findViewById(R.id.words_to_check_label);

        if (stats.wordsForCheck != 0)
        {
            label.setText(R.string.txt_words_for_check);
            counter.setText(Integer.valueOf(stats.wordsForCheck).toString());
            label.setOnClickListener(new OnCheckClick());
            counter.setOnClickListener(new OnCheckClick());

        }
        else
        {
            label.setText(R.string.txt_no_words_for_check);
            counter.setText("");
        }

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

    public static final String ST_ACTIVE_DICTIONARY_ID = "ST_ACTIVE_DICTIONARY_ID";

    @Override
    protected void onResume()
    {
        super.onResume();
     /*   int id;

        if (activeDict == null)
            id = -1;
        else
            id = activeDict.getId();
        detectState(id); */

        if (checkStateTask == null)
        {
            checkStateTask = new CheckDictStateTask();
            CheckDictStateTaskParams pars = new CheckDictStateTaskParams();
            pars.context = this;
            pars.dictId = activeDict.getId();
            checkStateTask.execute(pars, null, null);
        }
    }

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

        if (checkStateTask != null)
            if (!checkStateTask.isCancelled())
                checkStateTask.cancel(true);
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

    /**
     * This function populates the grid view with information
     * about other dictionaries
     */
    void fillGridNest()
    {
        try
        {
            ArrayList<Dictionary> dicts = DBDictionaryFactory.getInstance(this).getList();
            GridView view = new GridView(this);
            view.setAdapter(new DictionaryGridAdapter(this, dicts));
            gridNest.removeAllViews();
            gridNest.addView(view);
        }
        catch (SQLDataException e)
        {
            e.printStackTrace();
        }
    }

    class DictionaryGridAdapter extends ArrayAdapter<Dictionary>
    {
        ArrayList<Dictionary> dicts;

        public DictionaryGridAdapter(Context _context, List objects)
        {
            super(_context, R.layout.row_dict_list, objects);
            dicts = (ArrayList<Dictionary>) objects;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.view_small_dictionary,
                    parent, false);
            TextView tv = (TextView) ll.findViewById(R.id.tvDictName);

            tv.setText(dicts.get(i).getName());
            return null;
        }
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

    class DictStateHandler extends android.os.Handler
    {
        @Override
        public void handleMessage(android.os.Message msg)
        {
            if (msg.obj != null && msg.obj instanceof DictStats)
            {
                DictStats stats = (DictStats) msg.obj;
                setupStats(stats);
            }
        }
    }

    class CheckDictStateTaskParams
    {
        Context context;
        int dictId;
    }

    class DictStats
    {
        int wordsForLearn;
        int wordsForCheck;
        int wordsTotal;
        DateTime lastAccess;
    }

    class CheckDictStateTask extends AsyncTask<CheckDictStateTaskParams, Void, Void>
    {
        CheckDictStateTaskParams param;

        @Override
        protected Void doInBackground(CheckDictStateTaskParams... params)
        {
            boolean killed = false;

            param = params[0];

            DBDictionaryFactory instance = DBDictionaryFactory.getInstance(param.context);

            Dictionary dict = instance.getDictionaryById(param.dictId);

            do
            {
                instance.getAdditionalInfo(dict);

                DictStats stats = new DictStats();

                stats.wordsForLearn = dict.getWordsToLearn();
                stats.wordsForCheck = dict.getWordsToCheck();
                stats.lastAccess = dict.getLastAccess();
                stats.wordsTotal = dict.getWordCount();

                Message msg = new Message();

                msg.obj = stats;

                stateHandler.sendMessage(msg);

                try
                {
                    TimeUnit.SECONDS.sleep(900);
                }
                catch (InterruptedException ex)
                {
                    killed = true;
                }

                if (killed)
                    break;

            } while (!isCancelled());

            return null;
        }
    }
}