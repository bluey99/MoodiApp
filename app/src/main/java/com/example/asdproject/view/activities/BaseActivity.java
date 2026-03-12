package com.example.asdproject.view.activities;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.util.LocaleManager;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }
}