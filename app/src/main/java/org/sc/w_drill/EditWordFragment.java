package org.sc.w_drill;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.BaseWord;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.dict.Word;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EditWordFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EditWordFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class EditWordFragment extends Fragment
{

    private Dictionary activeDict;
    private IWord activeWord;
    WDdb database;
    EditText edWord;

    private OnFragmentInteractionListener mListener;
    private boolean isVisible;
    private boolean needBringWord;

    public static EditWordFragment newInstance(  )
    {
        EditWordFragment fragment = new EditWordFragment();
        return fragment;
    }

    public EditWordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new WDdb( getActivity().getApplicationContext() );

        /*int dictId;
        int wordId;
        if (getArguments() != null)
        {
            dictId = getArguments().getInt(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME);
            wordId = getArguments().getInt(DBWordFactory.WORD_ID_VALUE_NAME);



            activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );

            if( wordId != -1 )
            {
                activeWord = DBWordFactory.getInstance( database, activeDict  ).getWord( wordId );
                bringWordToScreen();
            }
            else
                prepareForNewWord();
        }
        else
        {
            fatalError();
        } */
    }

    private void prepareForNewWord()
    {
        edWord.setText("");
    }

    public void bringWordToScreen()
    {
        edWord.setText(activeWord.getWord());
        needBringWord = false;
    }

    private void fatalError()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_word, container, false);
        Button btnOk = ( Button ) view.findViewById( R.id.btnOk );
        btnOk.setOnClickListener(  new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startSaveWord();
            }
        });
        edWord = ( EditText ) view.findViewById( R.id.the_word );
        return view;
    }

    private void startSaveWord()
    {
        /**
         * Gather information
         */
        String word = String.valueOf(edWord.getText());
        // It's a dummy, obviously
        if( activeWord == null )
            activeWord = new Word( word );
        else
            activeWord.setWord( word );

        saveWord();
    }

    private void saveWord()
    {
        int id;
        if( ( id = activeWord.getId() ) != -1 ) {
            DBWordFactory.getInstance(database, activeDict).updateWord(activeWord);
        }
        else
        {
            id = DBWordFactory.getInstance(database, activeDict).insertWord(activeWord);
            activeWord.setId( id );
        }

        /**
         * We should update the word list
         * in any case due to word or a new word was added
         * or an old word has been changed.
         */
        mListener.onWordAdded( id );

        activeWord = new Word("");

        bringWordToScreen();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    public interface OnFragmentInteractionListener {

        public void onWordAdded( int id );
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        super.setUserVisibleHint(isVisibleToUser);

        // Visibility not changes, exit
        if (isVisibleToUser == isVisible )
            return;

        // If is becomes visible, refresh list
        if( isVisibleToUser && needBringWord )
            bringWordToScreen();

        isVisible = isVisibleToUser;

    }

    public void setActiveWord( int wordId )
    {
        activeWord = DBWordFactory.getInstance( database, activeDict  ).getWord( wordId );
        needBringWord = true;
    }

    public void setParams( int dictId, int wordId)
    {
        activeDict = DBDictionaryFactory.getInstance( database ).getDictionaryById( dictId );

        if( wordId != -1 )
            activeWord = DBWordFactory.getInstance( database, activeDict  ).getWord( wordId );
        else
            activeWord = Word.getDummy();
    }
}