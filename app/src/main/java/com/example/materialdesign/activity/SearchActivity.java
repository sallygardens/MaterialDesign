package com.example.materialdesign.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.materialdesign.R;

import butterknife.BindView;

public class SearchActivity extends AppCompatActivity {
    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";
    public static void startSearchFromLocation(int[] startingLocation, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, SearchActivity.class);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        startingActivity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
    }
}
