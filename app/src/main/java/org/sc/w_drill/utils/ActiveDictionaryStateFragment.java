package org.sc.w_drill.utils;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sc.w_drill.ActDictionaryEntry;
import org.sc.w_drill.MainActivity;
import org.sc.w_drill.R;
import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.dict.Dictionary;

import java.util.ArrayList;

/**
 * Created by Max on 10/6/2014.
 */
public class ActiveDictionaryStateFragment extends Fragment
{

    MainActivity mListener;

    ActiveDictionaryStateFragment instance;

    Dictionary activeDict;

    WDdb database;

    public static ActiveDictionaryStateFragment getInstance( Dictionary dict )
    {
        Bundle args = new Bundle();
        ActiveDictionaryStateFragment fragment = new ActiveDictionaryStateFragment();
        args.putInt( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, dict.getId() );
        fragment.setArguments( args );
        return fragment;
    }

    public ActiveDictionaryStateFragment()
    {

    }

    public void setActiveDict( Dictionary dict )
    {
        activeDict = dict;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        int dictId = getArguments().getInt( DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME );
        database = new WDdb( getActivity().getApplicationContext() );
        activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );
    }

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
            text.setText( getActivity().getResources().getString( R.string.txt_words_total, activeDict.getWordCount() ) );
            text.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Log.d("[MainActivity]", "Go to a full list of words");
                }
            });

            int cnt = DBDictionaryFactory.getInstance( database )
                    .getWordsTo(activeDict.getId(), DBDictionaryFactory.STAGE_LEARN);

            text = (TextView) view.findViewById(R.id.words_for_learn);
            text.setText(getActivity().getResources().getString(R.string.txt_words_for_learn, cnt));

            text.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Log.d( "[MainActivity]", "Go to learn new words");
                    mListener.goToDictionaryEntry(activeDict.getId(), ActDictionaryEntry.WORDS_TO_LEARN);
                }
            });

            cnt = DBDictionaryFactory.getInstance(database)
                    .getWordsTo(activeDict.getId(), DBDictionaryFactory.STAGE_CHECK);

            text = (TextView) view.findViewById(R.id.words_for_check);
            text.setText(getActivity().getResources().getString(R.string.txt_words_for_check, cnt));

            text.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Log.d( "[MainActivity]", "Go to check words");
                    mListener.goToDictionaryEntry(activeDict.getId(), ActDictionaryEntry.WORDS_TO_STUDY);
                }
            });

            text = (TextView) view.findViewById(R.id.add_words);
            text.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Log.d( "[MainActivity]", "Go to add words");
                    mListener.goToDictionaryEntry(activeDict.getId(), ActDictionaryEntry.ADD_WORDS);
                }
            });

            text = (TextView) view.findViewById(R.id.edit_dict);
            text.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Log.d( "[MainActivity]", "Go to dictionary entry");
                    mListener.goToDictionaryEntry(activeDict.getId(), ActDictionaryEntry.WHOLE_LIST_ENTRY);
                }
            });


        }
        else
        {
            text = (TextView) view.findViewById(R.id.dict_has_to_be_supplemented_with_words);
            text.setText(getActivity().getResources().getString(R.string.dict_has_to_be_supplemented_with_words));
            text.setOnClickListener( new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    Log.d( "[MainActivity]", "The dictionary has to be supplemented with words");
                    mListener.goToDictionaryEntry(activeDict.getId(), ActDictionaryEntry.ADD_WORDS);
                }
            });
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (MainActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainActivity");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


}