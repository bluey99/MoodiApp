package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.util.LocaleManager;

public class SelectionHistoryActivity extends BaseActivity {

    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply language
        LocaleManager.setLocale(this);

        setContentView(R.layout.activity_selection_history);


        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");


        TextView btnLanguage = findViewById(R.id.btnLanguage);

        btnLanguage.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate();
        });


        Button btnGoBack = findViewById(R.id.btnGoBack);

        btnGoBack.setOnClickListener(v -> finish());


        androidx.appcompat.widget.AppCompatButton btnChildHistory =
                findViewById(R.id.btnChildHistory);

        btnChildHistory.setOnClickListener(v -> {

            Intent i = new Intent(
                    SelectionHistoryActivity.this,
                    ChildLogsHistoryActivity.class);

            putChildExtras(i);
            startActivity(i);

        });


        androidx.appcompat.widget.AppCompatButton btnTaskHistory =
                findViewById(R.id.btnTaskHistory);

        btnTaskHistory.setOnClickListener(v -> {

            Intent i = new Intent(
                    SelectionHistoryActivity.this,
                    EmotionHistoryActivity.class);

            putChildExtras(i);
            startActivity(i);

        });

    }


    private void putChildExtras(Intent i) {

        if(childId != null){

            i.putExtra("CHILD_ID", childId);
            i.putExtra("CHILD_NAME", childName);

        }

    }
}