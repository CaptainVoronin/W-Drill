package org.sc.w_drill;

import android.app.Activity;
import android.app.Dialog;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
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
    FilterDialog dlgFilter = null;
    private OrderDialog orderDialog;

    enum FilterType { ALL, FOR_LEARN, FOR_CHECK };
    enum OrderProperty{ ALPHABET, PERCENT, ACCESS_TIME };


    boolean orderAscending = true;
    OrderProperty orderProperty = OrderProperty.ALPHABET;

    FilterType filterType;

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

    public void onFilterClick(View view)
    {
        if( dlgFilter == null )
            dlgFilter = new FilterDialog( getActivity() );
        dlgFilter.show();
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
        filterType = FilterType.ALL;
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
        Button btn = ( Button ) view.findViewById( R.id.btnFilter );
        btn.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onFilterClick(view);
            }
        });
        btn = ( Button ) view.findViewById( R.id.btnSortOrder );
        btn.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onOrderClick();
            }
        });

        if (needRefresh)
            refreshList();
        return view;
    }

    private void onOrderClick()
    {
        if( orderDialog == null )
            orderDialog = new OrderDialog( getActivity() );
        orderDialog.show();
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

        list = DBWordFactory.getInstance(database, activeDict)
                .getBriefList(getDBFilterClause(filterType), getOrderClause(orderAscending, orderProperty));

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

    public interface DictWholeListListener
    {
        public void onWordSelected( int id );
        public void onWordsDeleted( );
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
        // TODO: It doesn't work
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
        int cnt = DBWordFactory.getInstance( database, activeDict ).deleteWords( selectedWords );
        selectedWords.clear();

        if( cnt != 0 )
        {
            if( mListener != null )
                mListener.onWordsDeleted( );
            refreshList();
        }
    }

    public void onDestroyView ()
    {
        super.onDestroyView();
        needRefresh = true;
        isViewCreated = false;
    }

    public String getDBFilterClause( FilterType type  )
    {
        switch( type )
        {
            case ALL:
                return null;
            case FOR_LEARN:
                return " stage = 0";
            case FOR_CHECK:
                return " stage = 1";
            default:
                return null;
        }
    }

    class FilterDialog extends Dialog implements android.view.View.OnClickListener
    {
        public Activity activity;
        public Button btnOk, btnCancel;
        RadioButton rbAll, rbForLearn, rbForCheck;

        public FilterDialog( Activity _activity )
        {
            super(_activity );
            activity = _activity;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setTitle( R.string.dlg_filter_title );
            setContentView(R.layout.dlg_words_filter);
            rbAll = ( RadioButton ) findViewById( R.id.rbAll );
            rbForLearn = ( RadioButton ) findViewById( R.id.rbLearned );
            rbForCheck = ( RadioButton ) findViewById( R.id.rbForCheck );
            setRadio();
            btnOk = (Button) findViewById(R.id.btnOk);
            btnCancel = (Button) findViewById(R.id.btnCancel);
            btnOk.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
        }

        private void setRadio()
        {
            rbAll.setChecked( false );
            rbForCheck.setChecked( false );
            rbForLearn.setChecked( false );

            switch( filterType )
            {
                case ALL:
                    rbAll.setChecked( true );
                    break;
                case FOR_LEARN:
                    rbForLearn.setChecked( true );
                    break;
                case FOR_CHECK:
                    rbForCheck.setChecked( true );
                    break;
            }
        }

        @Override
        public void onClick(View view)
        {
            dismiss();
            DictWholeWordListFragment.FilterType newFilterType;
            switch(view.getId() )
            {
                case R.id.btnOk:
                {
                    if( rbAll.isChecked() )
                    {
                        newFilterType = FilterType.ALL;
                    } else if( rbForLearn.isChecked() )
                    {
                        newFilterType = FilterType.FOR_LEARN;
                    } else if( rbForCheck.isChecked() )
                    {
                        newFilterType = FilterType.FOR_CHECK;
                    }
                    else
                    {
                        newFilterType = FilterType.ALL;
                    }
                    if( newFilterType != filterType )
                    {
                        filterType = newFilterType;
                        setNeedRefresh();
                        //refreshList();
                    }
                }
                case R.id.btnCancel:
                    break;
            }
        }
    }

    class OrderDialog extends Dialog implements android.view.View.OnClickListener
    {

        RadioButton rbAlphabet, rbPercent, rbLastAccess;

        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            //requestWindowFeature(Window.FEATURE_NO_TITLE);
            setTitle( R.string.dlg_order_title );
            setContentView(R.layout.dlg_word_sort_order);
            rbAlphabet = ( RadioButton ) findViewById( R.id.rbAlphabet );
            rbPercent = ( RadioButton ) findViewById( R.id.rbPercent );
            rbLastAccess = ( RadioButton ) findViewById( R.id.rbLastAccess );
            Button btn = ( Button ) findViewById( R.id.btnAsc );
            btn.setOnClickListener( this );
            btn = ( Button ) findViewById( R.id.btnDesc );
            btn.setOnClickListener( this );
            setRadio();
        }

        private void setRadio()
        {
            rbAlphabet.setChecked( false );
            rbPercent.setChecked( false );
            rbLastAccess.setChecked( false );

            if( orderProperty == OrderProperty.ALPHABET )
                rbAlphabet.setChecked( true );
            else if( orderProperty == OrderProperty.PERCENT )
                rbPercent.setChecked( true );
            if( orderProperty == OrderProperty.ACCESS_TIME )
                rbLastAccess.setChecked( true );
        }

        public OrderDialog(Context context)
        {
            super(context);
        }

        @Override
        public void onClick(View view)
        {
            dismiss();
            boolean directionAsc = view.getId() == R.id.btnAsc;
            OrderProperty order;

            switch( view.getId() )
            {
                case R.id.btnAsc:
                    directionAsc = true;
                    break;
                case R.id.btnDesc:
                    directionAsc = false;
                    break;
            }

            if( rbAlphabet.isChecked( )  )
                order = OrderProperty.ALPHABET;
            else if( rbPercent.isChecked() )
                order = OrderProperty.PERCENT;
            else if( rbLastAccess.isChecked( ) )
                order = OrderProperty.ACCESS_TIME;
            else
                order = OrderProperty.ALPHABET;

            if( order != orderProperty || orderAscending != directionAsc )
            {
                orderProperty = order;
                orderAscending = directionAsc;
                setNeedRefresh();
            }
        }
    }

    String getOrderClause( boolean ascOrder, OrderProperty orderProperty )
    {
        String buff = "";

        switch( orderProperty )
        {
            case ALPHABET:
                buff += " word ";
                break;
            case PERCENT:
                buff += " percent ";
                break;
            case ACCESS_TIME:
                buff += " last_access ";
                break;
            default:
                buff += " word ";
                break;
        }

        if( ascOrder )
            buff += " asc ";
        else
            buff += " desc ";

        return buff;
    }
}