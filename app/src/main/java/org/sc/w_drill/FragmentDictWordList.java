package org.sc.w_drill;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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

import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.db_wrapper.DefaultDictionary;
import org.sc.w_drill.dict.BaseWord;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;
import org.sc.w_drill.utils.AMutableFilter;
import org.sc.w_drill.utils.FilterableList;
import org.sc.w_drill.utils.IFilteredListChangeListener;
import org.sc.w_drill.utils.LearnColors;
import org.sc.w_drill.utils.Triangle;
import org.sc.w_drill.utils.image.DictionaryImageFileManager;

import java.util.ArrayList;

public class FragmentDictWordList extends Fragment implements DialogSelectDict.DictionaryDialogListener
{

    Dictionary activeDict;
    private DictWholeListListener mListener;
    ListView listWords;
    EditText edSearchPattern;
    boolean isVisible;
    boolean isViewCreated = false;
    private boolean needRefresh;
    private TextWatcher searchTextWatcher;

    ArrayList<IBaseWord> selectedWords;
    private CompoundButton.OnCheckedChangeListener checkBoxClickListener;
    private boolean operationButtonsVisible;
    FilterDialog dlgFilter = null;
    WordListFilter wordFilter = null;
    private OrderDialog orderDialog;
    boolean defaultDictionary;


    enum FilterType
    {
        ALL, FOR_LEARN, FOR_CHECK
    }

    ;

    enum OrderProperty
    {
        ALPHABET, PERCENT, ACCESS_TIME
    }

    ;

    boolean orderAscending = true;
    OrderProperty orderProperty = OrderProperty.ALPHABET;

    FilterType filterType;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DictWholeWordListFragment.
     */

    public static FragmentDictWordList newInstance()
    {
            FragmentDictWordList fragment = new FragmentDictWordList();
        return fragment;
    }

    public FragmentDictWordList()
    {
    }

    public void onFilterClick(View view)
    {
        if (dlgFilter == null)
            dlgFilter = new FilterDialog(getActivity());
        dlgFilter.show();
    }

    /**
     * This function must be called iff an instance
     * of the class has already created and it needs
     * change active dictionary
     *
     * @param dict
     */
    public void setDict(Dictionary dict)
    {
        if (dict == null)
            throw new IllegalArgumentException(this.getClass().getName() + " Dictionary can't be 0");

        Bundle b1 = new Bundle();
        b1.putInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, dict.getId());
        setArguments(b1);

        if ((activeDict == null) || ((activeDict != null) && !activeDict.equals(dict)))
        {
            activeDict = dict;
            defaultDictionary = DefaultDictionary.isDefault(activeDict);
            setNeedRefresh();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null)
        {
            int dictId = args.getInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME);
            activeDict = DBDictionaryFactory.getInstance(getActivity()).getDictionaryById(dictId);
        }
        selectedWords = new ArrayList<IBaseWord>();
        checkBoxClickListener = new CheckBoxClickListener();
        filterType = FilterType.ALL;
        needRefresh = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_dict_whole_word_list, container, false);

        listWords = (ListView) view.findViewById(R.id.word_list);
        edSearchPattern = (EditText) view.findViewById(R.id.search_pattern);
        isViewCreated = true;
        final Button btnFilter = (Button) view.findViewById(R.id.btnFilter);
        final Button btnOrder = (Button) view.findViewById(R.id.btnSortOrder);
        if (!defaultDictionary)
        {

            btnFilter.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    onFilterClick(view);
                }
            });

            btnOrder.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    onOrderClick();
                }
            });
        }
        else
        {
            btnFilter.setEnabled(false);
            btnOrder.setEnabled(false);
        }

        if (needRefresh)
            refreshList();
        return view;
    }

    private void onOrderClick()
    {
        if (orderDialog == null)
            orderDialog = new OrderDialog(getActivity());
        orderDialog.show();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mListener = (DictWholeListListener) activity;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException(activity.toString()
                    + " must implement DictWholeListListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    private void refreshList()
    {
        final ArrayList<BaseWord> list = DBWordFactory.getInstance(getActivity(), activeDict)
                .getBriefList(getDBFilterClause(filterType), getOrderClause(orderAscending, orderProperty));

        final FilterableList<BaseWord> wordList = new FilterableList<BaseWord>();
        wordList.addAll(list);

        if (searchTextWatcher != null)
            edSearchPattern.removeTextChangedListener(searchTextWatcher);
        searchTextWatcher = new SearchTextWatcher(wordList);

        if (wordFilter != null)
        {
            wordList.setFilter(wordFilter);
            wordFilter.addListener(wordList);
        }

        edSearchPattern.addTextChangedListener(searchTextWatcher);

        WordListAdapter adapter = new WordListAdapter(getActivity(), wordList);
        wordList.addListener(adapter);
        listWords.setAdapter(adapter);

        needRefresh = false;
    }

    public void setNeedRefresh()
    {
        if (isVisible)
            refreshList();
        else
            needRefresh = true;
    }

    public interface DictWholeListListener
    {
        public void onWordSelected(int id);

        public void onWordsDeleted();

        public void onStartActionModeForWordList();

        public void onFinishActionModeForWordList();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);

        // Visibility not changes, exit
        if (isVisibleToUser == isVisible)
            return;

        // If is becomes visible, refresh list
        if ((isVisible = isVisibleToUser) && isViewCreated && needRefresh)
            refreshList();
    }

    private class WordListAdapter extends ArrayAdapter<BaseWord> implements IFilteredListChangeListener
    {
        ArrayList<BaseWord> words;
        Context context;
        OnWordClickListener onClick;

        public WordListAdapter(Context _context, FilterableList<BaseWord> _words)
        {
            super(_context, R.layout.row_word_list, _words);
            context = _context;
            words = _words;
            onClick = new OnWordClickListener();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View rowView;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (!defaultDictionary)
                rowView = inflater.inflate(R.layout.row_word_list, parent, false);
            else
                rowView = inflater.inflate(R.layout.row_notebook_word_list, parent, false);


            BaseWord word = words.get(position);

            //TODO: What for?
            rowView.setTag(Integer.valueOf(word.getId()));
            rowView.setOnClickListener(onClick);

            CheckBox chb = (CheckBox) rowView.findViewById(R.id.cbSelectWord);
            chb.setTag(word);
            chb.setOnCheckedChangeListener(checkBoxClickListener);

            TextView text = (TextView) rowView.findViewById(R.id.tvWord);
            text.setText(word.getWord());

            if (!defaultDictionary)
            {
                int color = LearnColors.getInstance(getActivity()).getColor(word.getLearnState(), word.getLearnPercent());
                rowView.setBackgroundColor(color);


                text = (TextView) rowView.findViewById(R.id.word_percent);

                Triangle tri = (Triangle) rowView.findViewById(R.id.maxIndicator);

                if (word.getLearnState() == IBaseWord.LearnState.learn)
                    tri.setColor(LearnColors.getInstance(getActivity()).getColor(0, 100));
                else
                    tri.setColor(LearnColors.getInstance(getActivity()).getColor(1, 200));

                String txt;

                if (word.getLearnState() == IBaseWord.LearnState.learn)
                    txt = getActivity().getApplicationContext().getString(R.string.word_learn_percent, word.getLearnPercent());
                else
                    txt = getActivity().getApplicationContext().getString(R.string.word_is_learned, word.getLearnPercent());

                text.setText(txt);
            }
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
            mListener.onWordSelected(((Integer) tag).intValue());
        }
    }

    class SearchTextWatcher implements TextWatcher
    {
        FilterableList<BaseWord> list;

        public SearchTextWatcher(FilterableList<BaseWord> _list)
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
            if (charSequence.length() == 0)
            {
                if (wordFilter == null)
                {
                    wordFilter = new WordListFilter("");
                    list.setFilter(wordFilter);
                    wordFilter.addListener(list);
                }
                else
                    wordFilter.setNewPattern("");
            }
        }

        @Override
        public void afterTextChanged(Editable editable)
        {
            String text = editable.toString();
            if (wordFilter == null)
            {
                wordFilter = new WordListFilter(text);
                list.setFilter(wordFilter);
                wordFilter.addListener(list);
            }
            else
                wordFilter.setNewPattern(text);
        }
    }

    class WordListFilter extends AMutableFilter<String, BaseWord>
    {

        public WordListFilter(String _pattern)
        {
            super(_pattern);
        }

        @Override
        public boolean check(BaseWord value)
        {
            if (value == null)
                return false;

            return value.getWord().startsWith(pattern);
        }
    }

    class CheckBoxClickListener implements CheckBox.OnCheckedChangeListener
    {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b)
        {
            IBaseWord val = (IBaseWord) compoundButton.getTag();
            if (val != null)
                if (b)
                {
                    if (selectedWords.size() == 0)
                        showOperationButtons();
                    selectedWords.add(val);

                }
                else
                {
                    selectedWords.remove(val);
                    if (selectedWords.size() == 0)
                        hideOperationButtons();
                }
        }
    }

    private void hideOperationButtons()
    {
        if (!operationButtonsVisible)
            return;
        operationButtonsVisible = false;
        mListener.onFinishActionModeForWordList();
    }

    private void showOperationButtons()
    {
        if (operationButtonsVisible)
            return;
        operationButtonsVisible = true;
        mListener.onStartActionModeForWordList();
    }

    public void deleteSelected()
    {
        DictionaryImageFileManager manager = new DictionaryImageFileManager(getActivity(), activeDict);

        int cnt = DBWordFactory.getInstance(getActivity(), activeDict).deleteWords(selectedWords);

        for (IBaseWord word : selectedWords)
        {
            try
            {
                manager.deleteImage(word);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        selectedWords.clear();

        if (cnt != 0)
        {
            if (mListener != null)
                mListener.onWordsDeleted();
            refreshList();
        }
    }

    public void onDestroyView()
    {
        super.onDestroyView();
        needRefresh = true;
        isViewCreated = false;
    }

    public String getDBFilterClause(FilterType type)
    {
        switch (type)
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

        public FilterDialog(Activity _activity)
        {
            super(_activity);
            activity = _activity;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setTitle(R.string.dlg_filter_title);
            setContentView(R.layout.dlg_words_filter);
            rbAll = (RadioButton) findViewById(R.id.rbAll);
            rbForLearn = (RadioButton) findViewById(R.id.rbLearned);
            rbForCheck = (RadioButton) findViewById(R.id.rbForCheck);
            setRadio();
            btnOk = (Button) findViewById(R.id.btnOk);
            btnCancel = (Button) findViewById(R.id.btnCancel);
            btnOk.setOnClickListener(this);
            btnCancel.setOnClickListener(this);
        }

        private void setRadio()
        {
            rbAll.setChecked(false);
            rbForCheck.setChecked(false);
            rbForLearn.setChecked(false);

            switch (filterType)
            {
                case ALL:
                    rbAll.setChecked(true);
                    break;
                case FOR_LEARN:
                    rbForLearn.setChecked(true);
                    break;
                case FOR_CHECK:
                    rbForCheck.setChecked(true);
                    break;
            }
        }

        @Override
        public void onClick(View view)
        {
            dismiss();
            FragmentDictWordList.FilterType newFilterType;
            switch (view.getId())
            {
                case R.id.btnOk:
                {
                    if (rbAll.isChecked())
                    {
                        newFilterType = FilterType.ALL;
                    }
                    else if (rbForLearn.isChecked())
                    {
                        newFilterType = FilterType.FOR_LEARN;
                    }
                    else if (rbForCheck.isChecked())
                    {
                        newFilterType = FilterType.FOR_CHECK;
                    }
                    else
                    {
                        newFilterType = FilterType.ALL;
                    }
                    if (newFilterType != filterType)
                    {
                        filterType = newFilterType;
                        setNeedRefresh();
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
            setTitle(R.string.dlg_order_title);
            setContentView(R.layout.dlg_word_sort_order);
            rbAlphabet = (RadioButton) findViewById(R.id.rbAlphabet);
            rbPercent = (RadioButton) findViewById(R.id.rbPercent);
            rbLastAccess = (RadioButton) findViewById(R.id.rbLastAccess);
            Button btn = (Button) findViewById(R.id.btnAsc);
            btn.setOnClickListener(this);
            btn = (Button) findViewById(R.id.btnDesc);
            btn.setOnClickListener(this);
            setRadio();
        }

        private void setRadio()
        {
            rbAlphabet.setChecked(false);
            rbPercent.setChecked(false);
            rbLastAccess.setChecked(false);

            if (orderProperty == OrderProperty.ALPHABET)
                rbAlphabet.setChecked(true);
            else if (orderProperty == OrderProperty.PERCENT)
                rbPercent.setChecked(true);
            if (orderProperty == OrderProperty.ACCESS_TIME)
                rbLastAccess.setChecked(true);
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

            switch (view.getId())
            {
                case R.id.btnAsc:
                    directionAsc = true;
                    break;
                case R.id.btnDesc:
                    directionAsc = false;
                    break;
            }

            if (rbAlphabet.isChecked())
                order = OrderProperty.ALPHABET;
            else if (rbPercent.isChecked())
                order = OrderProperty.PERCENT;
            else if (rbLastAccess.isChecked())
                order = OrderProperty.ACCESS_TIME;
            else
                order = OrderProperty.ALPHABET;

            if (order != orderProperty || orderAscending != directionAsc)
            {
                orderProperty = order;
                orderAscending = directionAsc;
                setNeedRefresh();
            }
        }
    }

    String getOrderClause(boolean ascOrder, OrderProperty orderProperty)
    {
        String buff = "";

        switch (orderProperty)
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

        if (ascOrder)
            buff += " asc ";
        else
            buff += " desc ";

        return buff;
    }

    public void moveSelected()
    {
        DialogSelectDict dlg = new DialogSelectDict(getActivity(), activeDict, this);
        dlg.show();
    }

    @Override
    public void onDictSelected(Dictionary dict)
    {
        moveWords(dict);
    }

    private void moveWords(Dictionary dict)
    {
        try
        {
            int cnt = DBWordFactory.getInstance(getActivity(), activeDict).moveWords(dict, selectedWords);

            DictionaryImageFileManager manager = new DictionaryImageFileManager(getActivity(), activeDict);

            for (IBaseWord word : selectedWords)
            {
                if (manager.getImageFile(word) != null)
                    manager.moveImageToDict(word, dict);
            }

            selectedWords.clear();

            if (cnt != 0)
            {
                if (mListener != null)
                    mListener.onWordsDeleted();
                refreshList();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //TODO: There must be a handler
        }
    }

    @Override
    public void onCanceled()
    {
        hideOperationButtons();
    }

    public void operationModeDestroyed()
    {
        operationButtonsVisible = false;
    }

    public void clearStats()
    {
        DBWordFactory.getInstance(getActivity(), activeDict).clearLearnStatistic();
        refreshList();
    }

    public void setAllLearned()
    {
        DBWordFactory.getInstance(getActivity(), activeDict).setAllLearned();
        refreshList();
    }

}