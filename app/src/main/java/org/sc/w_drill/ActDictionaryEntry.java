package org.sc.w_drill;

import java.util.Locale;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
//import android.support.v7.view.ActionMode;
import android.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IWord;

public class ActDictionaryEntry
        extends ActionBarActivity
        implements ActionBar.TabListener,
        EditWordFragment.OnFragmentInteractionListener,
        DictWholeWordListFragment.DictWholeListListener,
        LearnWordsFragment.OnLearnWordsFragmentListener
{

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    EditWordFragment editWordFragment;
    DictWholeWordListFragment dictWholeWordListFragment;
    LearnWordsFragment learnWordsFragment;

    public static final String ENTRY_KIND_PARAM_NAME = "ENTRY_KIND_PARAM_NAME";

    public static final int WHOLE_LIST_ENTRY = 0;
    public static final int WORDS_TO_LEARN = 1;
    public static final int WORDS_TO_STUDY = 2;
    public static final int ADD_WORDS = 3;

    MenuItem editItem;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    WDdb database;

    Dictionary activeDictionary;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_dictionaty_entry);

        Intent data = getIntent();

        int dictId;
        database = new WDdb( getApplicationContext() );

        if( data != null )
        {
            dictId = data.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
            if( dictId != -1  )
                activeDictionary = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );
            else
                fatalError();
        }
        else
            fatalError();

        int entryKind = data.getIntExtra( ENTRY_KIND_PARAM_NAME, ADD_WORDS );

        // Set up the action bar.
        //final ActionBar actionBar = getSupportActionBar();
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle( activeDictionary.getName() );
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        //actionBar.setDisplayHomeAsUpEnabled(true);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        switch( entryKind )
        {
            case ActDictionaryEntry.ADD_WORDS:
                getSupportActionBar().setSelectedNavigationItem( 1 );
                break;
            case ActDictionaryEntry.WORDS_TO_STUDY:
            case ActDictionaryEntry.WORDS_TO_LEARN:
                getSupportActionBar().setSelectedNavigationItem( 0 );
                break;
            case ActDictionaryEntry.WHOLE_LIST_ENTRY:
                getSupportActionBar().setSelectedNavigationItem( 2 );
                break;
        }
    }

    private void fatalError()
    {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_dictionaty_entry, menu);
        boolean res = super.onCreateOptionsMenu( menu );
        editItem = menu.findItem( R.id.action_edit );
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
            if( learnWordsFragment != null )
            {
                IWord word = learnWordsFragment.getActiveWord();
                if( word != null )
                {
                    onWordSelected( word.getId() );
                }
            }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        int position = tab.getPosition();
        if( editItem != null )
            if( position == 1 || position == 2 )
                editItem.setVisible( false );
            else
                editItem.setVisible( true );

        mViewPager.setCurrentItem( position );
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onWordAdded( int id )
    {
        if( dictWholeWordListFragment != null )
        {
            dictWholeWordListFragment.setNeedRefresh();
        }
    }

    @Override
    public void onWordSelected(int id)
    {
        editWordFragment.setActiveWord(id);
        getSupportActionBar().setSelectedNavigationItem( 1 );
    }

    @Override
    public void onStatActionModeForWordList()
    {
        actionMode = startActionMode( callback );
    }

    @Override
    public void onFinishActionModeForWordList()
    {
        if( actionMode != null)
            actionMode.finish();
    }

    @Override
    public void wordsStageChanged()
    {

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if( position == 1 )
            {
                if( editWordFragment == null )
                    editWordFragment = EditWordFragment.newInstance( );
                editWordFragment.setParams( activeDictionary.getId(), -1 );
                Log.d("[ActDictionaryEntry]", "Get EditWordFragment instance");
                return ( Fragment ) editWordFragment;
            }
            else if ( position == 2 )
            {
                if( dictWholeWordListFragment == null )
                    dictWholeWordListFragment = DictWholeWordListFragment.newInstance();

                dictWholeWordListFragment.setDict( activeDictionary );
                Log.d("[ActDictionaryEntry]", "Get DictWholeWordListFragment instance");
                return ( Fragment ) dictWholeWordListFragment;
            }
            else
            {
                if( learnWordsFragment == null )
                    learnWordsFragment = LearnWordsFragment.newInstance(  activeDictionary.getId() );
                Log.d("[ActDictionaryEntry]", "Get LearnWordsFragment instance");
                //learnWordsFragment.setLearnParams( activeDictionary, IBaseWord.LearnState.learn);
                return ( Fragment ) learnWordsFragment;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.learn_words).toUpperCase(l);
                case 1:
                    return getString(R.string.add_words).toUpperCase(l);
                case 2:
                    return getString(R.string.whole_word_list).toUpperCase(l);
            }
            return null;
        }
    }

    ActionMode.Callback callback = new ActionMode.Callback ()
    {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu)
        {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.action_bar_for_word_list, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu)
        {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem)
        {
            switch (menuItem.getItemId())
            {
                case R.id.bnt_delete_words:
                    dictWholeWordListFragment.deleteSelected();
                    break;
                default:
                    break;
            }

            actionMode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode)
        {

        }
    };
}
