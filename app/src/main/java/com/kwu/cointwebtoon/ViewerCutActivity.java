package com.kwu.cointwebtoon;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.kwu.cointwebtoon.DataStructure.Episode;
import com.kwu.cointwebtoon.DataStructure.Webtoon;
import com.kwu.cointwebtoon.Views.Smart_Cut_ImageView;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class ViewerCutActivity extends TypeKitActivity implements Observer {
    public static enum Action {
        PREVIOUS,
        NEXT,
        None // when no action was detected
    }

    private ArrayList<String> imageURLs;//imageURL 담을 ArrayList
    private Webtoon webtoon_instance;
    private Episode episode_instance;
    private ViewFlipper flipper;//이미지를 넘기면서 볼 수 있는 뷰
    private int imageIndex = 0;//현재 보고 있는 컷이 몇 번째 컷인가?
    private int count = 0; // 좋아요 기능을 위한 변수
    private GetServerData getServerData;
    private int toonId, episodeId;
    private ImageButton good;
    private Toolbar topToolbar, bottomToolbar;
    private COINT_SQLiteManager manager;
    private TextView episodeTitleTextView, episodeIdTextView, good_count;
    private boolean runMode, showtoolbar;
    private int toolbarheight = 0;
    private Thread autoThread;
    private int cut_like;
    private int sleepTime = -1;
    private RatingBar ratingbar;
    private TextView mention, starTV, artistTV;
    private LayoutInflater inflater = null;
    private Button givingStar;
    private Smart_Cut_ImageView newImageView;
    public static final int REQUEST_CODE_RATING = 1001;
    Application_UserInfo userInfo;
    private SharedPreferences likePreference;
    public float myStar = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewer_cut_activity);
        good = (ImageButton) findViewById(R.id.good);
        topToolbar = (Toolbar) findViewById(R.id.toptoolbar);
        bottomToolbar = (Toolbar) findViewById(R.id.bottomtoolbar);
        episodeTitleTextView = (TextView) findViewById(R.id.CutToonTitle);
        episodeTitleTextView.setSelected(true);
        episodeIdTextView = (TextView) findViewById(R.id.current_pos);
        toolbarheight = dpToPixel(42);
        manager = COINT_SQLiteManager.getInstance(this);
        good_count = (TextView) findViewById(R.id.count_txt);
        runMode = false;
        showtoolbar = true;
        setSupportActionBar(topToolbar);
        flipper = (ViewFlipper) findViewById(R.id.viewflipper);
        inflater = LayoutInflater.from(this);
        ///Flipper 리스너 추가 + SwipeDetector Class를 추가하여 Swipe과 touch 동작을 구분
        final SwipeDetector swipeDetector = new SwipeDetector();
        flipper.setOnTouchListener(swipeDetector);

        flipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (swipeDetector.swipeDetected()) {
                    switch (swipeDetector.getAction()) {
                        case PREVIOUS:
                            MovewPreviousView();
                            break;
                        case NEXT:
                            MoveNextView();
                            break;
                    }
                } else {
                    //클릭 처리
                    showToolbars(!showtoolbar);
                }
            }

        });
        userInfo = (Application_UserInfo) getApplication();
        flipper.setBackgroundColor(Color.WHITE);
        Intent getIntent = getIntent();
        toonId = getIntent.getIntExtra("id", -1);
        episodeId = getIntent.getIntExtra("ep_id", -1);

        if (toonId == -1 || episodeId == -1) {
            Toast.makeText(this, "이미지 로드를 실패하였습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        likePreference = getSharedPreferences("episode_like", MODE_PRIVATE);
        getServerData = new GetServerData(this);
        getServerData.registerObserver(this);
        getServerData.getImagesFromServer(toonId, episodeId);
        good_count.setText(String.valueOf(manager.getWebtoonInstance(toonId).getLikes()));
    }

    @Override
    public void update(Observable observable, Object o) {
        View view;
        try {
            episodeTitleTextView.setText(manager.getEpisodeTitle(toonId, episodeId));
            episodeIdTextView.setText(String.valueOf(episodeId));
        } catch (Exception ex) {
        }
        this.imageURLs = (ArrayList<String>) o;
        imageIndex = 0;
        if (imageURLs != null) {
            for (int i = 0; i < imageURLs.size(); i++) {
                newImageView = new Smart_Cut_ImageView(this);
                if (i == 0) {
                    Glide.with(this)
                            .load(imageURLs.get(i))
                            .asBitmap()
                            .placeholder(R.drawable.viewer_sc_placeholder)
                            .into(newImageView);
                } else {
                    Glide.with(this)
                            .load(imageURLs.get(i))
                            .asBitmap()
                            .skipMemoryCache(true)
                            .placeholder(R.drawable.view_placeholder_testing)
                            .into(newImageView);
                }
                flipper.addView(newImageView);
            }
            view = inflater.inflate(R.layout.viewer_rating_item, null);
            try {
                flipper.addView(view);
                artistTV = (TextView) view.findViewById(R.id.cut_artist);
                ratingbar = (RatingBar) view.findViewById(R.id.cut_rating_bar);
                mention = (TextView) view.findViewById(R.id.cut_mention);
                starTV = (TextView) view.findViewById(R.id.cut_starScore);
                givingStar = (Button) view.findViewById(R.id.cut_giving_star);
                new ViewerCutActivity.GetCurrentToonInfo().execute();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }
        getServerData.plusHit(toonId);
    }

    private void MoveNextView() {
        if (imageIndex < imageURLs.size()) {
            flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.appear_from_right));
            flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.disappear_to_left));
            flipper.showNext();
            imageIndex++;
        } else {
            if (runMode && (episodeId < manager.maxEpisodeId(toonId))) {
                //정주행 모드일 때, 마지막 컷에서 Flipper 를 클릭했을 경우 다음 회차가 존재할 경우에 다음 회차로 넘어감
                flipper.removeAllViews();
                imageURLs.clear();
                episodeId += 1;
                getServerData.getImagesFromServer(toonId, episodeId);
                manager.updateEpisodeRead(toonId, episodeId);
            }
        }
    }

    private void MovewPreviousView() {
        if (imageIndex > 0) {
            flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.appear_from_left));
            flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.disappear_to_right));
            flipper.showPrevious();
            imageIndex--;
        } else {
            if (runMode && episodeId > 1) {   //정주행 모드 일 때, 첫 컷에서 이전 버튼을 클릭하면 이전 회차로 넘어감
                flipper.removeAllViews();
                imageURLs.clear();
                episodeId -= 1;
                getServerData.getImagesFromServer(toonId, episodeId);
                manager.updateEpisodeRead(toonId, episodeId);
            }
        }
    }

    public int dpToPixel(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void showToolbars(boolean show) {
        showtoolbar = show;
        if (show) {
            topToolbar.animate().translationY(0).withLayer();
            bottomToolbar.animate().translationY(0).withLayer();
        } else {
            topToolbar.animate().translationY(-1 * toolbarheight).withLayer();
            bottomToolbar.animate().translationY(toolbarheight).withLayer();
        }
    }

    public void runButtonClick(View v) {
        if (runMode) {
            runMode = false;
            ImageButton target = (ImageButton) v;
            target.setImageDrawable(getDrawable(R.drawable.run_inactive));
        } else {
            runMode = true;
            ImageButton target = (ImageButton) v;
            target.setImageDrawable(getDrawable(R.drawable.run_active));
        }
    }

    public void BackBtn(View v) {
        this.finish();
    }

    public void HeartBtn(View v) {
        if (!userInfo.isLogin()) {
            new AlertDialog.Builder(this)
                    .setTitle("로그인")
                    .setMessage("로그인이 필요한 서비스 입니다. 로그인 하시겠습니까?")
                    .setPositiveButton("예", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(ViewerCutActivity.this, LoginActivity.class));
                        }
                    }).setNegativeButton("아니요", null).show();
            return;
        }
        SharedPreferences.Editor editor = likePreference.edit();
        count++;
        if (count % 2 != 0) {
            getServerData.likeWebtoon(toonId, "plus");
            editor.putBoolean(String.valueOf(toonId), true);
            good.setImageDrawable(getDrawable(R.drawable.episode_heart_active));
            good_count.setText(String.valueOf(cut_like + 1));
        } else if (count % 2 == 0 && likePreference.getBoolean(String.valueOf(toonId), false)) {
            getServerData.likeWebtoon(toonId, "minus");
            editor.putBoolean(String.valueOf(toonId), false);
            good.setImageDrawable(getDrawable(R.drawable.episode_heart_inactive));
            if (cut_like >= 0) {
                good_count.setText(String.valueOf(cut_like));
            }
        }
        editor.commit();
    }

    public void Dat(View v) {
        try {
            Intent comment_intent = new Intent(this, ViewerCommentActivity.class);
            comment_intent.putExtra("id", toonId);
            comment_intent.putExtra("ep_id", episodeId);
            comment_intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (!(imageIndex == imageURLs.size())) {
                comment_intent.putExtra("cutnum", imageIndex + 1);
            }
            startActivity(comment_intent);
        } catch (NullPointerException ex) {
        }
    }

    public void Previous(View v) {
        try {
            if (episodeId > 1) {
                flipper.removeAllViews();
                imageURLs.clear();
                episodeId -= 1;
                getServerData.getImagesFromServer(toonId, episodeId);
                manager.updateEpisodeRead(toonId, episodeId);
            }
        } catch (NullPointerException ex) {
        }
    }

    public void Next(View v) {
        try {
            if (episodeId > 0 && (episodeId < manager.maxEpisodeId(toonId))) {
                flipper.removeAllViews();
                imageURLs.clear();
                episodeId += 1;
                getServerData.getImagesFromServer(toonId, episodeId);
                manager.updateEpisodeRead(toonId, episodeId);
            }
        } catch (NullPointerException ex) {
        }
    }

    public void timerClick(View v) {
        ImageButton my = (ImageButton) v;
        if (sleepTime == -1) {
            my.setImageDrawable(getDrawable(R.drawable.viewer_2sec));
            sleepTime = 2000;
        } else if (sleepTime == 2000) {
            my.setImageDrawable(getDrawable(R.drawable.viewer_3sec));
            sleepTime = 3000;
        } else if (sleepTime == 3000) {
            my.setImageDrawable(getDrawable(R.drawable.viewer_4sec));
            sleepTime = 4000;
        } else if (sleepTime == 4000) {
            my.setImageDrawable(getDrawable(R.drawable.viewer_defaultset));
            sleepTime = -1;
            try {
                autoThread.interrupt();
            } catch (Exception e) {
            }
            return;
        }
        try {
            autoThread.interrupt();
        } catch (Exception e) {
        }
        autoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(sleepTime);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MoveNextView();
                            }
                        });
                    } catch (InterruptedException intex) {
                        break;
                    }
                }
            }
        });
        autoThread.start();
    }

    private class SwipeDetector implements View.OnTouchListener {
        public final int HORIZONTAL_MIN_DISTANCE = 40;
        private float downX, upX;
        private Action mSwipeDetected = Action.None;

        public boolean swipeDetected() {
            return mSwipeDetected != Action.None;
        }

        public Action getAction() {
            return mSwipeDetected;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (imageURLs == null) {
                return true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    mSwipeDetected = Action.None;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    upX = event.getX();
                    float deltaX = downX - upX;
                    if (Math.abs(deltaX) > HORIZONTAL_MIN_DISTANCE) {
                        if (deltaX < 0) {
                            mSwipeDetected = Action.PREVIOUS;
                            return true;
                        }
                        if (deltaX > 0) {
                            mSwipeDetected = Action.NEXT;
                            return true;
                        }
                    }
            }
            return false;
        }
    }

    public void cut_givingStarBtnClick(View v) {
        try {
            if (!userInfo.isLogin()) {
                new AlertDialog.Builder(this)
                        .setTitle("로그인")
                        .setMessage("로그인이 필요한 서비스 입니다. 로그인 하시겠습니까?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(ViewerCutActivity.this, LoginActivity.class));
                            }
                        }).setNegativeButton("아니요", null).show();
                return;
            }
            Intent intent = new Intent(this, ViewerStarScoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(intent, REQUEST_CODE_RATING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RATING) {
            if (resultCode == RESULT_OK) {
                try {
                    float SCORE = data.getExtras().getFloat("SCORE");
                    if (starTV != null && ratingbar != null) {
                        starTV.setText(String.valueOf(SCORE));
                        ratingbar.setMax(10);
                        ratingbar.setRating((SCORE) / 2);
                        myStar = SCORE;
                        manager.updateMyStarScore(toonId, episodeId, SCORE);
                        Log.i("coint", String.valueOf(manager.getMyStar(toonId, episodeId)));
                        givingStar.setEnabled(false);
                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        getServerData.removeObserver(this);
        super.onDestroy();
    }

    private class GetCurrentToonInfo extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            episode_instance = manager.getEpisodeInstance(toonId, episodeId);
            webtoon_instance = manager.getWebtoonInstance(toonId);
            myStar = manager.getMyStar(toonId, episodeId);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                cut_like = webtoon_instance.getLikes();
                good_count.setText(String.valueOf(cut_like));
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
            super.onPostExecute(aVoid);
            if (!userInfo.isLogin()) {
                likePreference.edit().putBoolean(String.valueOf(toonId), false).commit();
            }
            if (likePreference.getBoolean(String.valueOf(toonId), false)) {
                good.setImageDrawable(getDrawable(R.drawable.episode_heart_active));
            }
            try {
                if (myStar != -1) {
                    ratingbar.setMax(10);
                    ratingbar.setRating(myStar / 2);
                    givingStar.setEnabled(false);
                    starTV.setText(String.valueOf(myStar));
                } else {
                    ratingbar.setRating(0);
                    givingStar.setEnabled(true);
                    starTV.setText("0.0");
                }
                mention.setText(episode_instance.getMention());
                artistTV.setText("작가의 말 (" + webtoon_instance.getArtist() + ")");
                artistTV.setPaintFlags(artistTV.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
            } catch (NullPointerException ex) {
            }
        }
    }
}
