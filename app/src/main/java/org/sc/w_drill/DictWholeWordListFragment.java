package org.sc.w_drill;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.BaseWord;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.utils.AMutableFilter;
import org.sc.w_drill.utils.FilterableList;
import org.sc.w_drill.utils.IFilteredListChangeListener;

import java.util.ArrayList;


public class DictWholeWordListFragment extends Fragment
{

    Dictionary activeDict;

    private DictWholeListListener mListener;
    private WDdb database;
    ListView listWords;
    EditText edSearchPattern;
    boolean isVisible;
    boolean isViewCreated = false;
    private boolean needRefresh;
    private TextWatcher searchTextWatcher;

    ArrayList<Integer> selectedWords;
    private CompoundButton.OnCheckedChangeListener checkBoxClickListener;
    private boolean operationButtonsVisible;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DictWholeWordListFragment.
     */

    public static DictWholeWordListFragment newInstance(  )
    {
        DictWholeWordListFragment fragment = new DictWholeWordListFragment();
        return fragment;
    }

    public DictWholeWordListFragment()
    {
        // Required empty public constructor
    }

    /**
     * This function must be called iff an instance
     * of the class has already created and it needs
     * change active dictionary
     * @param _dictId
     */
    public void setDict( Dictionary dict )
    {
        if( dict == null )
            throw new IllegalArgumentException( this.getClass().getName() + " Dictionary can't be 0" );

        Bundle b1 = new Bundle();
        b1.putInt( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, dict.getId() );
        setArguments( b1 );

        if( ( activeDict == null ) || ( ( activeDict != null ) && !activeDict.equals( dict ) ) )
        {
            activeDict = dict;

            setNeedRefresh();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        database = new WDdb( getActivity().getApplicationContext() );
        Bundle args = getArguments();
        if( args != null )
        {
            int dictId = args.getInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME);
            activeDict = DBDictionaryFactory.getInstance(database).getDictionaryById(dictId);
        }
        selectedWords = new ArrayList<Integer>();
        checkBoxClickListener = new CheckBoxClickListener();
        needRefresh = true;
    }

    private void fatalError()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dict_whole_word_list, container, false);

        listWords = (ListView) view.findViewById(R.id.word_list);

        edSearchPattern = (EditText) view.findViewById(R.id.search_pattern);
        isViewCreated = true;
        Log.d("[DictWholeListListener::onCreateView]", "Create view. Sincerely yours C.O.");
        if (needRefresh)
            refreshList();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DictWholeListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DictWholeListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void refreshList()
    {
        ArrayList<BaseWord> list;
        FilterableList<BaseWord> wordList;

        list = DBWordFactory.getInstance(database, activeDict).getBriefList();
        wordList = new FilterableList<BaseWord>();
        wordList.addAll( list );

        if( searchTextWatcher != null )
            edSearchPattern.removeTextChangedListener( searchTextWatcher  );
        searchTextWatcher = new SearchTextWatcher( wordList );
        edSearchPattern.addTextChangedListener( searchTextWatcher );

        WordListAdapter adapter = new WordListAdapter(getActivity().getApplicationContext(), wordList);
        wordList.addListener( adapter );
        listWords.setAdapter( adapter );
        needRefresh = false;
    }

    public void setNeedRefresh()
    {
        if( isVisible )
            refreshList();
        else
            needRefresh = true;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface DictWholeListListener
    {
        public void onWordSelected( int id );
        public void onStatActionModeForWordList();
        public void onFinishActionModeForWordList();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);

        // Visibility not changes, exit
        if (isVisibleToUser == isVisible )
            return;

        // If is becomes visible, refresh list
        if( ( isVisible = isVisibleToUser ) && isViewCreated && needRefresh )
            refreshList();
    }

    private class WordListAdapter extends ArrayAdapter<BaseWord> implements IFilteredListChangeListener
    {
        ArrayList<BaseWord> words;
        Context context;
        OnWordClickListener onClick;
        public WordListAdapter(Context _context, FilterableList<BaseWord> _words)
        {
            super(_context, R.layout.word_list_row, _words);
            context = _context;
            words = _words;
            onClick = new OnWordClickListener();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.word_list_row, parent, false);
            BaseWord word = words.get(position);
            rowView.setTag( Integer.valueOf( word.getId() ) );
            rowView.setOnClickListener( onClick );

            CheckBox chb = (CheckBox) rowView.findViewById(R.id.cbSelectWord);
            chb.setText( word.getWord());
            chb.setTag( Integer.valueOf( word.getId() ) );
            chb.setOnCheckedChangeListener( checkBoxClickListener );

            TextView text = ( TextView ) rowView.findViewById( R.id.word_percent );

            String txt;

            if( word.getLearnState() == IBaseWord.LearnState.learn )
                txt = getActivity().getApplicationContext().getString( R.string.word_learn_percent, word.getLearnPercent() );
            else
                txt = getActivity().getApplicationContext().getString( R.string.word_is_learned, word.getLearnPercent() );

            text.setText( txt );
            return rowView;
        }

        @Override
        public void onSizeChanged()
        {
            super.notifyDataSetChanged();
        }
    }

    class OnWordClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            Object tag = view.getTag();
            mListener.onWordSelected( ((Integer) tag).intValue() );
        }
    }

    class SearchTextWatcher implements TextWatcher
    {
        FilterableList<BaseWord> list;
        WordListFilter filter = null;

        public SearchTextWatcher( FilterableList<BaseWord> _list )
        {
            list = _list;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3)
        {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3)
        {
            if( charSequence.length() == 0 ) {
                if (filter == null) {
                    filter = new WordListFilter("");
                    list.setFilter(filter);
                    filter.addListener(list);
                }
                else
                    filter.setNewPattern("");
            }
        }

        @Override
        public void afterTextChanged(Editable editable)
        {
            String text = editable.toString();
            if( filter == null )
            {
                filter = new WordListFilter(text);
                list.setFilter( filter );
                filter.addListener( list );
            }
            else
                filter.setNewPattern( text );
        }
    }

    class WordListFilter extends AMutableFilter<String, BaseWord>
    {

        public WordListFilter( String _pattern )
        {
            super( _pattern );
        }

        @Override
        public boolean check(BaseWord value)
        {
            if( value == null )
                return false;

            return value.getWord().startsWith(pattern);
        }
    }

    class CheckBoxClickListener implements CheckBox.OnCheckedChangeListener
    {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b)
        {
            Integer val = ( Integer ) compoundButton.getTag();
            if( val != null )
                if ( b )
                {
                    if( selectedWords.size() == 0 )
                        showOperationButtons();
                    selectedWords.add(val);

                }
                else
                {
                    selectedWords.remove(val);
                    if( selectedWords.size() == 0 )
                        hideOperationButtons();
                }
        }
    }

    private void hideOperationButtons()
    {
        if( !operationButtonsVisible )
            return;
        operationButtonsVisible = false;
        mListener.onFinishActionModeForWordList();
    }

    private void showOperationButtons()
    {
        if( operationButtonsVisible )
            return;
        operationButtonsVisible = true;
        mListener.onStatActionModeForWordList();
    }

    public void deleteSelected()
    {
        DBWordFactory.getInstance( database, activeDict ).deleteWords( selectedWords );
        selectedWords.clear();
        refreshList();
    }

    public void onDestroyView ()
    {
        super.onDestroyView();
        needRefresh = true;
        isViewCreated = false;
    }
}