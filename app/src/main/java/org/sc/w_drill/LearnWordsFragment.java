package org.sc.w_drill;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IBaseWord;

public class LearnWordsFragment extends Fragment
{
    Dictionary activeDict;
    IBaseWord.LearnState learnStage;

    private OnLearnWordsFragmentListener mListener;

    public static LearnWordsFragment newInstance()
    {
        LearnWordsFragment fragment = new LearnWordsFragment();
        return fragment;
    }
    public LearnWordsFragment()
    {
        // Required empty public constructor
    }

    public void setLearnParams( Dictionary _dict, IBaseWord.LearnState _learnState )
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_leard_words, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLearnWordsFragmentListener) activity;
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

    public interface OnLearnWordsFragmentListener
    {
        public void wordsStageChanged();
    }

}
