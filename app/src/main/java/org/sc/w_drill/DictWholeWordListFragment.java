package org.sc.w_drill;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.BaseWord;
import org.sc.w_drill.dict.Dictionary;

import java.util.ArrayList;


public class DictWholeWordListFragment extends Fragment
{

    Dictionary activeDict;

    private DictWholeListListener mListener;
    private WDdb database;
    ListView listWords;
    boolean isVisible;
    private boolean needRefresh;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DictWholeWordListFragment.
     */

    public static DictWholeWordListFragment newInstance( int _dictId )
    {
        DictWholeWordListFragment fragment = new DictWholeWordListFragment();
        Bundle args = new Bundle();
        args.putInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, _dictId );
        fragment.setArguments(args);
        return fragment;
    }

    public DictWholeWordListFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            int dictId = getArguments().getInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME);
            database = new WDdb( getActivity().getApplicationContext() );
            if( dictId != -1 )
                activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );
            else
                fatalError();
        }
        else
            fatalError();
    }

    private void fatalError()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_dict_whole_word_list, container, false);

        listWords = ( ListView ) view.findViewById( R.id.word_list );

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
        ArrayList<BaseWord> wordList;

        wordList = DBWordFactory.getInstance(database, activeDict).getBriefList();
        WordListAdapter adapter = new WordListAdapter(getActivity().getApplicationContext(), wordList);
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
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);

        // Visibility not changes, exit
        if (isVisibleToUser == isVisible )
            return;

        // If is becomes visible, refresh list
        if( ( isVisible = isVisibleToUser ) && needRefresh )
            refreshList();
    }



    private class WordListAdapter extends ArrayAdapter<BaseWord>
    {
        ArrayList<BaseWord> words;
        Context context;
        OnWordClickListener onClick;
        public WordListAdapter(Context _context, ArrayList<BaseWord> _words)
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
            TextView text = ( TextView ) rowView.findViewById( R.id.word_percent );
            text.setText( "5%" );
            return rowView;
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
}
