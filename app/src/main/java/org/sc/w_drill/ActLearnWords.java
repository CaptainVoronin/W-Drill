package org.sc.w_drill;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sc.w_drill.db_wrapper.DBDictionaryFactory;
import org.sc.w_drill.db_wrapper.DBWordFactory;
import org.sc.w_drill.dict.Dictionary;
import org.sc.w_drill.dict.IMeaning;
import org.sc.w_drill.dict.IWord;
import org.sc.w_drill.utils.CircularArrayList;
import org.sc.w_drill.utils.DBPair;
import org.sc.w_drill.utils.LearnColors;
import org.sc.w_drill.utils.MessageDialog;
import org.sc.w_drill.utils.PartsOfSpeech;
import org.sc.w_drill.utils.TextHelper;
import org.sc.w_drill.utils.Triangle;
import org.sc.w_drill.utils.image.DictionaryImageFileManager;
import org.sc.w_drill.utils.image.ImageConstraints;
import org.sc.w_drill.utils.image.ImageHelper;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;

public class ActLearnWords extends ActionBarActivity
{

    Dictionary activeDict;
    IWord activeWord;
    TextView wordPlace;
    TextView wordTranscription;
    TextView wordExample;
    ArrayList<WordTmpStats> wordStats;
    private boolean confirmed;

    Calendar start;
    CircularArrayList<IWord> words = null;
    Triangle learnIndicator;
    LearnColors learnColors;
    PartsOfSpeech partsOS;
    private GestureDetectorCompat mDetector;
    boolean wordsLearned = false;
    LinearLayout viewContainer;
    ImageView imgUp, imgDown;
    private ImageView imgIllustration;
    DictionaryImageFileManager dictManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_learn_words);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        int dictId = getIntent().getIntExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, -1);
        activeDict = DBDictionaryFactory.getInstance(this).getDictionaryById(dictId);

        int wordId = getIntent().getIntExtra(ActDictionaryEntry.UPDATED_WORD_ID_PARAM_NAME, -1);

        wordPlace = (TextView) findViewById(R.id.the_word);

        wordTranscription = (TextView) findViewById(R.id.transcription);
        wordExample = (TextView) findViewById(R.id.examples);

        learnIndicator = (Triangle) findViewById(R.id.learnIndicator);

        learnColors = LearnColors.getInstance(getApplicationContext());

        partsOS = PartsOfSpeech.getInstance(getApplicationContext());

        viewContainer = (LinearLayout) findViewById(R.id.viewContainer);

        imgUp = (ImageView) findViewById(R.id.imgUp);
        imgDown = (ImageView) findViewById(R.id.imgDown);

        dictManager = new DictionaryImageFileManager(this, activeDict);

        imgIllustration = (ImageView) findViewById(R.id.wordIllustration);

        getWordsSet();
        if (words != null)
        {
            IWord word = words.next();
            if (word != null)
                bringWordToScreen(word);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.act_learn_words_menu, menu);
        boolean res = super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if (id == R.id.action_edit)
        {
            Intent intent = new Intent(ActLearnWords.this, ActDictionaryEntry.class);
            intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
            intent.putExtra(DBWordFactory.WORD_ID_VALUE_NAME, activeWord.getId());
            intent.putExtra(ActDictionaryEntry.ENTRY_KIND_PARAM_NAME, ActDictionaryEntry.ADD_WORDS);
            intent.putExtra(ActDictionaryEntry.EDIT_AND_RETURN, Boolean.valueOf(true));
            startActivityForResult(intent, MainActivity.CODE_ActDictionaryEntry);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getWordsSet()
    {
        WordTmpStats stat;

        // TODO: There should be a limit of rows. Now it's the constant value - 10
        ArrayList<IWord> wrd = DBWordFactory.getInstance(this, activeDict).getWordsToLearn(10);

        if (wrd == null)
        {
            words = null;
            showMessageAndExit(getString(R.string.txt_no_words_to_learn));
        }
        else
        {
            words = new CircularArrayList(wrd);

            wordStats = new ArrayList<WordTmpStats>();
            for (IWord w : wrd)
            {
                stat = new WordTmpStats(w.getId());
                stat.avgTime = w.getAvgTime();
                wordStats.add(stat);
            }
        }
    }

    private void showMessageAndExit(String string)
    {
        String title = getString(R.string.txt_learning);
        MessageDialog.showInfo(this, string, new MessageDialog.Handler()
        {
            @Override
            public void doAction()
            {
                onBackPressed();
            }
        }, title);
    }

    private void showNothingToDoDialog()
    {
        String title = getString(R.string.txt_learning);
        MessageDialog.showInfo(this, R.string.nothing_to_do, new MessageDialog.Handler()
        {
            @Override
            public void doAction()
            {
                onBackPressed();
            }
        }, title);
    }

    private void showWhatToDoDialog()
    {
        // TODO: This dialog can be shown if there are a words for checking
        MessageDialog.showQuestion(this, R.string.no_more_words, new MessageDialog.Handler()
                {
                    @Override
                    public void doAction()
                    {
                        Intent intent = new Intent(ActLearnWords.this, ActCheckWords.class);
                        intent.putExtra(DBDictionaryFactory.DICTIONARY_ID_VALUE_NAME, activeDict.getId());
                        startActivity(intent);

                    }
                },
                new MessageDialog.Handler()
                {
                    @Override
                    public void doAction()
                    {
                        onBackPressed();
                    }
                }, getString(R.string.txt_learning));
    }

    void bringWordToScreen(IWord word)
    {
        clearScreen();
        activeWord = word;
        int color = getResources().getColor(R.color.TextPartSelection);

        wordPlace.setText(Html.fromHtml(TextHelper.decorate(activeWord.getWord(), Integer.valueOf(color).toString())));
        learnIndicator.setColor(learnColors.getColor(activeWord));

        if (activeWord.getTranscription() != null && activeWord.getTranscription().length() != 0)
            wordTranscription.setText(activeWord.getTranscription());

        StringBuilder buffer = new StringBuilder();

        if (activeWord.meanings() != null)
        {
            for (IMeaning m : activeWord.meanings())
            {
                if (m.examples() != null)
                    for (DBPair pair : m.examples())
                        buffer.append(pair.getValue()).append("\n");
            }
            wordExample.setText(buffer.toString());
        }

        if (dictManager.getImageFile(activeWord) != null)
        {
            try
            {
                Bitmap bmp = dictManager.getImageBitmap(activeWord);
                ImageConstraints constraints = ImageConstraints.getInstance(this);
                imgIllustration.setImageBitmap(ImageHelper.resizeBitmapForShow(constraints, bmp));
            }
            catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
                // TODO: exception handler
            }
        }

        start = Calendar.getInstance();

    }

    private void clearScreen()
    {
        wordExample.setText("");
        wordTranscription.setText("");
        viewContainer.removeAllViews();
        imgIllustration.setImageBitmap(null);
    }

    void showMeaning()
    {
        state = State.WAIT_CONFIRMATION;
        for (IMeaning m : activeWord.meanings())
        {
            MeaningRow row = new MeaningRow(m);
            View view = row.getView();
            viewContainer.addView(view);
        }
    }

    /*public IWord getActiveWord( )
    {
        return activeWord;
    } */

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case MainActivity.CODE_ActDictionaryEntry:
                if (resultCode == ActDictionaryEntry.RESULT_WORD_UPDATED)
                {
                    int id = data.getIntExtra(DBWordFactory.WORD_ID_VALUE_NAME, -1);
                    if (id == activeWord.getId()) ;
                    updateWord();
                }
                break;
            default:
                break;
        }
    }

    private void updateWord()
    {
        if (words != null)
        {
            int index = words.indexOf(activeWord);

            activeWord = DBWordFactory.getInstance(this, activeDict).getWordEx(activeWord.getId());
            words.set(index, activeWord);

            bringWordToScreen(activeWord);
            if (state == State.WAIT_CONFIRMATION)
                showMeaning();
        }
    }

    class WordTmpStats
    {
        public int id;

        public WordTmpStats(int _id)
        {
            id = _id;
            avgTime = 0;
            attempts = 0;
            faults = 0;
        }

        public int avgTime;
        public int attempts;
        public int faults;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    enum Direction
    {
        UP, DOWN, LEFT, RIGHT, NONE
    }

    ;

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY)
        {
            float x1 = event1.getRawX();
            float y1 = event1.getRawY();
            float x2 = event2.getRawX();
            float y2 = event2.getRawY();
            Direction d = getDirection(x1, y1, x2, y2);

            processAction(d);

            return true;
        }

        Direction getDirection(float x1, float y1, float x2, float y2)
        {
            Direction d = Direction.NONE;
            float delta = 50;
            float tg;
            double ang;

            if (Math.abs(Math.abs(x1) - Math.abs(x2)) < 0.0001)
                ang = 90;
            else
            {
                tg = -1 * (y2 - y1) / (x2 - x1);
                ang = Math.atan(tg) * 57.295;
            }

            // Firstly, it needs to detect horizontal or
            // vertical movement. It depends on angle

            if (Math.abs(ang) > 50)
            {
                // This is an area of vertical movements
                if (y1 > y2)
                    d = Direction.UP;
                else
                    d = Direction.DOWN;

            }
            else if (Math.abs(ang) < 30)
            {
                // This is an area of horizontal movements
                if (x1 > x2)
                    d = Direction.LEFT;
                else
                    d = Direction.RIGHT;
            }

            return d;
        }
    }

    @Override
    public void onBackPressed()
    {
        if (wordsLearned)
            setResult(Activity.RESULT_OK);
        else
            setResult(Activity.RESULT_CANCELED);

        super.onBackPressed();
    }

    class MeaningRow
    {
        LinearLayout view;

        public MeaningRow(IMeaning meaning)
        {
            LayoutInflater lf = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            view = (LinearLayout) lf.inflate(R.layout.row_show_meaning, null);
            TextView tv = (TextView) view.findViewById(R.id.tvPartOfSpeech);
            tv.setText(PartsOfSpeech.getInstance(ActLearnWords.this).getName(meaning.partOFSpeech()));
            tv = (TextView) view.findViewById(R.id.tvMeaning);
            tv.setText(meaning.meaning());
        }

        public View getView()
        {
            return view;
        }
    }

    enum State
    {
        WAIT_ANSWER, WAIT_CONFIRMATION
    }

    ;

    enum Answer
    {
        I_KNOW, I_DONT_KNOW
    }

    ;
    State state = State.WAIT_ANSWER;
    Answer answer;

    void processAction(Direction direction)
    {
        boolean accepted = false;

        if (state == State.WAIT_ANSWER)
        {
            if (direction == Direction.UP)
            {
                answer = Answer.I_KNOW;
                setIconsToIKnowState();
                accepted = true;
            }
            else if (direction == Direction.DOWN)
            {
                answer = Answer.I_DONT_KNOW;
                setIconsToIDontKnowState();
                accepted = true;
            }

            if (accepted)
                showMeaning();
        }
        else
        {
            state = State.WAIT_ANSWER;

            if (direction == Direction.RIGHT)
            {
                if (answer == Answer.I_DONT_KNOW)
                    processFail();
                else
                    processSuccess();
                accepted = true;
            }
            else if (direction == Direction.UP)
            {
                if (answer == Answer.I_KNOW)
                    processSuccess();
                accepted = true;
            }
            else if (direction == Direction.DOWN)
            {
                if (answer == Answer.I_KNOW)
                    processFail();
                accepted = true;
            }

            if (accepted)
            {
                setIconsToDefaultState();
                nextWord();
            }
        }
    }

    private void nextWord()
    {
        if (words.size() != 0)
        {
            IWord word = words.next();
            // There is another one word
            // Set buttons to default state

            imgUp.setImageDrawable(getResources().getDrawable(R.drawable.hand_up));
            imgDown.setImageDrawable(getResources().getDrawable(R.drawable.hand_down));

            // Bring the new word to the screen
            bringWordToScreen(word);
            confirmed = false;
        }
        else
        {
            // The current word set is empty
            // Take the next set from DB
            getWordsSet();

            if (words == null || words.size() == 0)
            {
                // If there are words for learning
                // we'll make a transition.
                if (DBDictionaryFactory.getInstance(this).getWordsTo(activeDict.getId(), DBDictionaryFactory.STAGE_CHECK) >= 4)
                    showWhatToDoDialog();
                else
                {
                    showNothingToDoDialog();
                }
            }
            else
            {
                confirmed = false;
                bringWordToScreen(words.next());
            }
        }
    }

    private void processFail()
    {
        WordTmpStats stat = getStat();
        int percent = activeWord.getLearnPercent();

        stat.attempts++;

        if (percent - 20 < 0)
            percent = 0;
        else
            percent -= 20;

        wordsLearned = true;
        saveResult(stat, percent);
    }

    private void processSuccess()
    {
        WordTmpStats stat = getStat();
        int percent = activeWord.getLearnPercent();

        stat.attempts++;

        if (percent + 20 > 100)
            percent = 100;
        else
            percent += 20;

        wordsLearned = true;
        saveResult(stat, percent);
        words.remove(activeWord);
        wordStats.remove(stat);
    }

    private void setIconsToIDontKnowState()
    {
        imgUp.setImageDrawable(getResources().getDrawable(R.drawable.hand_up));
        imgDown.setImageDrawable(getResources().getDrawable(R.drawable.next));
    }

    private void setIconsToDefaultState()
    {
        imgUp.setImageDrawable(getResources().getDrawable(R.drawable.hand_up));
        imgDown.setImageDrawable(getResources().getDrawable(R.drawable.hand_down));
    }

    private void setIconsToIKnowState()
    {
        imgUp.setImageDrawable(getResources().getDrawable(R.drawable.next));
        imgDown.setImageDrawable(getResources().getDrawable(R.drawable.hand_down));
    }

    WordTmpStats getStat()
    {
        WordTmpStats stat = null;

        for (WordTmpStats s : wordStats)
            if (s.id == activeWord.getId())
            {
                stat = s;
                break;
            }
        return stat;
    }

    void saveResult(WordTmpStats stat, int percent)
    {
        int time = (int) (Calendar.getInstance().getTimeInMillis() - start.getTimeInMillis());

        time = Math.round((activeWord.getAvgTime() + time) / (activeWord.getAccessCount() + 1));
        activeWord.setLearnPercent(percent);
        DBWordFactory.getInstance(this, activeDict)
                .updatePercentAndTime(activeWord.getId(), activeWord.getLearnPercent(), time);

    }
}