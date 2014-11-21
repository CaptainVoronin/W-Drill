package org.sc.w_drill;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sc.w_drill.db.WDdb;
import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.db_wrapper.RandomizerEmptyException;
import org.sc.w_drill.db_wrapper.RandomizerException;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.ArrayListRandomizer;
import org.sc.w_drill.utils.CircularArrayList;
import org.sc.w_drill.utils.MessageDialog;
import org.sc.w_drill.utils.TextHelper;
import org.sc.w_drill.utils.image.DictionaryImageFileManager;
import org.sc.w_drill.utils.image.ImageConstraints;
import org.sc.w_drill.utils.image.ImageHelper;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class ActCheckWords extends ActionBarActivity
{

    private boolean userGaveUp;

    enum Mode
    {
        CHOISE, COMPARE
    }

    ;

    Mode mode;

    LinearLayout rootView;
    RelativeLayout chooseOptionView = null;
    LinearLayout enterWordView = null;
    View activeView = null;
    IWord activeWord;
    WDdb database;
    Dictionary activeDict;
    CircularArrayList<IWord> words;
    ArrayListRandomizer<IWord> arrayRandomizer;
    ArrayListRandomizer<IMeaning> meaningRandomizer;
    TextView tv1, tv2, tv3, tv4;
    boolean missed = false;
    EditText edWordAnswer = null;
    Button btnIDontKnow;
    DictionaryImageFileManager dManager;
    InputChecker checker;
    InputTextChecker inputChecker;
    OptionChecker optionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_check_words);
        rootView = (LinearLayout) findViewById(R.id.root_view);

        Intent args = getIntent();
        int dictId = args.getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
        if (dictId == -1)
            fatalError();

        activeDict = DBDictionaryFactory.getInstance(this).getDictionaryById(dictId);

        try
        {
            prepareWordList();
        }
        catch (RandomizerEmptyException ex)
        {
            showErrorEndExit(getString(R.string.txt_no_words_to_check));
            return;
        }

        arrayRandomizer = new ArrayListRandomizer<IWord>();
        meaningRandomizer = new ArrayListRandomizer<IMeaning>();
        dManager = new DictionaryImageFileManager(this, activeDict);

        inputChecker = null;
        optionChecker = null;

        changeWord();
    }

    void prepareWordList() throws RandomizerEmptyException
    {
        ArrayList<IWord> list = DBWordFactory.getInstance(this, activeDict).getWordsToCheck(110);

        if (list == null && list.size() == 0)
            throw new RandomizerEmptyException();

        Collections.shuffle(list, new Random());

        words = new CircularArrayList<IWord>(list);
    }

    private void fatalError()
    {

    }

    void changeWord()
    {
        try
        {
            if (words.size() == 0)
                prepareWordList();
            activeWord = words.next();
            if (activeView != null)
                rootView.removeView(activeView);
            activeView = getView();
            rootView.addView(activeView);
        }
        catch (RandomizerException ex)
        {
            ex.printStackTrace();
            showErrorEndExit(getString(R.string.msg_not_enough_words_for_check));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            showErrorEndExit(ex.getMessage());
        }
    }

    private void showErrorEndExit(String message)
    {
        MessageDialog.showError(this, message, new MessageDialog.Handler()
        {
            @Override
            public void doAction()
            {
                finish();
            }
        }, null);
    }

    private View getView() throws RandomizerException, RandomizerEmptyException
    {
        if (activeWord.getLearnPercent() >= 200)
        {
            mode = Mode.COMPARE;
            return getWriteWordView();
        }
        else
        {
            mode = Mode.CHOISE;
            return getChooseOptionView();
        }
    }

    View getChooseOptionView() throws RandomizerException, RandomizerEmptyException
    {
        chooseOptionView = (RelativeLayout) getLayoutInflater()
                .inflate(R.layout.fragment_act_check_words_choose_option, null);

        tv1 = (TextView) chooseOptionView.findViewById(R.id.version1);
        tv1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                checkChoise(view);
            }
        });

        tv2 = (TextView) chooseOptionView.findViewById(R.id.version2);
        tv2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                checkChoise(view);
            }
        });

        tv3 = (TextView) chooseOptionView.findViewById(R.id.version3);
        tv3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                checkChoise(view);
            }
        });

        tv4 = (TextView) chooseOptionView.findViewById(R.id.version4);
        tv4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                checkChoise(view);
            }
        });

        if (optionChecker == null)
            optionChecker = new OptionChecker();

        checker = optionChecker;

        ArrayList<IWord> subset = makeSubset(activeDict, activeWord, 4);

        TextView tv = (TextView) chooseOptionView.findViewById(R.id.word_for_check);
        int color = getResources().getColor(R.color.TextPartSelection);

        tv.setText(Html.fromHtml(TextHelper.decorate(activeWord.getWord(), Integer.valueOf(color).toString())));

        setText(subset.get(0), tv1);
        setText(subset.get(1), tv2);
        setText(subset.get(2), tv3);
        setText(subset.get(3), tv4);

        if (dManager.getImageFile(activeWord) != null)
        {
            ImageView iv = (ImageView) chooseOptionView.findViewById(R.id.imgIllustration);
            Bitmap bmp;
            try
            {
                bmp = dManager.getImageBitmap(activeWord);
                ImageConstraints constraints = ImageConstraints.getInstance(this);
                iv.setImageBitmap(ImageHelper.resizeBitmapForShow(constraints, bmp));
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
                Toast.makeText(this, "Error: can't load image", Toast.LENGTH_LONG);
            }
        }

        return chooseOptionView;
    }

    void setText(IWord word, TextView tv)
    {
        if (word.meanings().size() > 1)
            tv.setText(meaningRandomizer.getRandomItem(word.meanings()).meaning());
        else
            tv.setText(word.meanings().get(0).meaning());
        tv.setTag(word);
    }

    View getWriteWordView()
    {
        enterWordView = (LinearLayout) getLayoutInflater().inflate(R.layout.fragment_act_check_words_enter_word, null);
        TextView tv = (TextView) enterWordView.findViewById(R.id.idMeaning);
        tv.setText(activeWord.meanings().get(0).meaning());
        tv = (TextView) enterWordView.findViewById(R.id.tvWord);
        tv.setText("");

        if (inputChecker == null)
            inputChecker = new InputTextChecker();

        checker = inputChecker;

        btnIDontKnow = (Button) enterWordView.findViewById(R.id.dont_know);
        btnIDontKnow.setText("?");
        btnIDontKnow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                compareWords("", null);
            }
        });

        edWordAnswer = (EditText) enterWordView.findViewById(R.id.word_answer);
        edWordAnswer.requestFocus();
        edWordAnswer.setTag(activeWord.getWord());
        edWordAnswer.setText("");

        edWordAnswer.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent)
            {
                if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                    return true;

                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                {
                    String word = edWordAnswer.getText().toString();
                    compareWords(word, null);
                    return true;
                }
                return false;
            }
        });
        return enterWordView;
    }

    void checkChoise(View view)
    {
        Object tag = view.getTag();
        if (tag != null)
            compareWords(((IWord) tag).getWord(), view);
    }

    private void increasePercent(IWord activeWord)
    {
        int percent = activeWord.getLearnPercent();
        if (percent + 20 > 200)
            percent = 200;
        else
            percent += 20;

        // TODO: time is skipped
        DBWordFactory.getInstance(this, activeDict)
                .updatePercentAndTime(activeWord.getId(), percent, 0);

    }

    private void decreasePercent(IWord activeWord)
    {
        int percent = activeWord.getLearnPercent();
        boolean reinit = false;

        if (percent - 20 < 0)
        {
            // In this case the state must be
            // changed and the word must be removed from current list
            // So WordRandomizer must be reinitialized
            percent = 0;
            reinit = true;
        }
        else
            percent -= 20;

        // TODO: time is skipped
        DBWordFactory.getInstance(this, activeDict)
                .updatePercentAndTime(activeWord.getId(), percent, 0);

        try
        {
            if (reinit)
                prepareWordList();
        }
        catch (RandomizerEmptyException ex)
        {
            showErrorEndExit(getString(R.string.txt_no_words_to_check));
        }
    }

    void compareWords(String word, View view)
    {
        if (checker.check(word))
        {
            if (!missed)
                increasePercent(activeWord);
            else
                missed = false;
            words.remove(activeWord);
            changeWord();
        }
        else
        {
            missed = true;
            decreasePercent(activeWord);
            if (checker instanceof OptionChecker)
                ((OptionChecker) checker).setIncorrectView(view);

            checker.onError();
        }
    }

    /**
     * @param dict       dictionary where are words
     * @param word       the word to check. One of it's meanings must be included
     * @param upperLimit how many words must be in subset
     * @return a list which is populated by words
     * @throws RandomizerException if dictionary hasn't enough words this exception will be throw
     */
    private ArrayList<IWord> makeSubset(Dictionary dict, IWord word, int upperLimit)
            throws RandomizerException, RandomizerEmptyException
    {

        ArrayList<IWord> subset = new ArrayList<IWord>();

        ArrayList<Integer> ids = DBWordFactory.getInstance(this, dict).getIdsListWithExclusion(word.getId());

        Collections.shuffle(ids, new Random());

        if (ids.size() < upperLimit - 1)
            throw new RandomizerEmptyException();

        subset.add(word);

        for (int i = 0; i < upperLimit - 1; i++)
            subset.add(DBWordFactory.getInstance(this, dict).getWordEx(ids.get(i).intValue()));

        Collections.shuffle(subset, new Random());

        return subset;
    }


    abstract class InputChecker
    {
        public boolean check(String word)
        {
            return TextHelper.compare(activeWord.getWord(), word);
        }

        public abstract void onError();
    }

    class OptionChecker extends InputChecker
    {

        View incorrectView;

        public void setIncorrectView(View view)
        {
            incorrectView = view;
        }

        @Override
        public void onError()
        {
            incorrectView.setBackgroundColor(Color.RED);

            if (((IWord) tv1.getTag()).getId() == activeWord.getId())
                tv1.setBackgroundColor(Color.LTGRAY);
            else if (((IWord) tv2.getTag()).getId() == activeWord.getId())
                tv2.setBackgroundColor(Color.LTGRAY);
            else if (((IWord) tv3.getTag()).getId() == activeWord.getId())
                tv3.setBackgroundColor(Color.LTGRAY);
            else if (((IWord) tv4.getTag()).getId() == activeWord.getId())
                tv4.setBackgroundColor(Color.LTGRAY);
        }
    }

    class InputTextChecker extends InputChecker
    {

        @Override
        public void onError()
        {
            TextView tv = (TextView) enterWordView.findViewById(R.id.tvWord);
            tv.setText(activeWord.getWord());
        }
    }
}