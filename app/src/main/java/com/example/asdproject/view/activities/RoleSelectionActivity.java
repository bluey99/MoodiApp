package com.example.asdproject.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.util.LocaleManager;

public class RoleSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseManager.init(this);
        setContentView(R.layout.activity_role_selection);

        LinearLayout btnChild = findViewById(R.id.btnChild);
        LinearLayout btnParent = findViewById(R.id.btnParent);

        // ✅ language button (TextView or ImageButton both are View)
        View btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> {
                LocaleManager.toggleLanguage(RoleSelectionActivity.this);
                recreate(); // ✅ refresh UI strings
            });
        }

        btnChild.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnParent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, ParentLoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
}