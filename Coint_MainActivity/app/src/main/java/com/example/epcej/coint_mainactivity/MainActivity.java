package com.example.epcej.coint_mainactivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ViewPager pager;
    RecyclerView recyclerView;
    Top15Adapter top15Adapter;
    RecyclerView.LayoutManager layoutManager;
    MyWebtoonAdp myWebtoonAdapter;
    private ArrayList<SearchResult> arrayList;
    private COINT_SQLiteManager coint_sqLiteManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);            // 액션바에서 앱 이름 보이지 않게 함
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //안드로이드에 데이터를 넣음
        GetServerData getServerData = new GetServerData(this);

        pager = (ViewPager)findViewById(R.id.pager);                        //뷰페이저에 어댑터를 연결하는 부분
        top15Adapter = new Top15Adapter(this);
        pager.setAdapter(top15Adapter);

        //즐겨찾는 웹툰 추가하는 부분
        recyclerView = (RecyclerView)findViewById(R.id.my_recycler_view);

        recyclerView.setHasFixedSize(true);

        coint_sqLiteManager = COINT_SQLiteManager.getInstance(this);

        Cursor cursor = coint_sqLiteManager.isMyWebtoon();          //is_mine이 1인 웹툰을 불러옴 Id, Title, Artist, Thumburl, Starscore순.
/*
        layoutManager = new StaggeredGridLayoutManager(5,StaggeredGridLayoutManager.VERTICAL);*/
        layoutManager = new GridLayoutManager(this,5);
        recyclerView.setLayoutManager(layoutManager);

        myWebtoonAdapter = new MyWebtoonAdp(this);
        recyclerView.setAdapter(myWebtoonAdapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {     // 검색 메뉴 만들어주는 부분
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {       // 검색 누르면 실행
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        EditText search = (EditText)findViewById(R.id.searchbar);
        String searchString = search.getText().toString();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {

            if(searchString.equals("")){
                Toast.makeText(this,"검색어를 입력하세요",Toast.LENGTH_SHORT).show();
            }else{
                intent = new Intent(MainActivity.this, SearchList.class);
                intent.putExtra("Intent",search.getText().toString());
                startActivity(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {        // navigation drawer에 있는 메뉴 선택시 실행
        // Handle navigation view item clicks here.
        Intent intent;
        int id = item.getItemId();

        switch(id){
            case R.id.webtoonRanking:
                intent = new Intent(MainActivity.this, Top100RankList.class);
                intent.putExtra("Intent","조회수 Top 100");
                startActivity(intent);
            break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onClick(View view) {        // 베스트도전은 웹뷰로 띄우고, 나머지는 액티비티
        int id = view.getId();
        String result;
        Intent intent;
        int position = top15Adapter.returnPosition-1;
        Cursor c = coint_sqLiteManager.topHits(position);

        switch (id){
            case R.id.genreBtn:
                intent = new Intent(MainActivity.this, IntentTest.class);
                intent.putExtra("Intent","genre");
                startActivity(intent);
                break;
            case R.id.artistBtn:
                intent = new Intent(MainActivity.this, IntentTest.class);
                intent.putExtra("Intent","artist");
                startActivity(intent);
                break;
            case R.id.weekdayBtn:
                intent = new Intent(MainActivity.this, IntentTest.class);
                intent.putExtra("Intent","weekday");
                startActivity(intent);
                break;
            case R.id.bestBtn:
                //webview로 띄울 예정
                intent = new Intent(MainActivity.this, BestChallenge.class);
                intent.putExtra("Best","bestchallenge");
                startActivity(intent);
                break;
            case R.id.addTopBtn:
                position = (Integer)view.getTag();

                c = coint_sqLiteManager.topHits(position);
                c.moveToFirst();

                result = coint_sqLiteManager.updateMyWebtoon(c.getString(0).toString());
                c = coint_sqLiteManager.isMyWebtoon();
                myWebtoonAdapter.addRemoveItem(c);
                break;

            case R.id.addMidBtn:
                position = (Integer)view.getTag();

                c = coint_sqLiteManager.topHits(position);
                c.moveToPosition(1);

                result = coint_sqLiteManager.updateMyWebtoon(c.getString(0).toString());
                c = coint_sqLiteManager.isMyWebtoon();
                myWebtoonAdapter.addRemoveItem(c);
                break;
            case R.id.addBotBtn:
                position = (Integer)view.getTag();

                c = coint_sqLiteManager.topHits(position);
                c.moveToLast();

                result = coint_sqLiteManager.updateMyWebtoon(c.getString(0).toString());
                c = coint_sqLiteManager.isMyWebtoon();
                myWebtoonAdapter.addRemoveItem(c);
                break;
        }
    }

    protected void onResume() {
        super.onResume();
        Cursor c = coint_sqLiteManager.isMyWebtoon();
        myWebtoonAdapter.addRemoveItem(c);
    }

}