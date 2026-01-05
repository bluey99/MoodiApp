package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ParentHomeActivity extends AppCompatActivity {

    private String parentId;

    private FirebaseFirestore db;

    // child bar views
    private HorizontalScrollView childScroll;
    private LinearLayout childContainer;
    private TextView txtCurrentChild;

    // current selected child
    private String selectedChildId = null;
    private String selectedChildName = null;
    private View selectedChildView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        // get parent id from login
        Intent intent = getIntent();
        parentId = intent.getStringExtra("PARENT_ID");

        db = FirebaseFirestore.getInstance();

        // child bar
        childScroll = findViewById(R.id.childScroll);
        childContainer = findViewById(R.id.childContainer);
        txtCurrentChild = findViewById(R.id.txtCurrentChild);

        // default state
        childScroll.setVisibility(View.GONE);
        if (txtCurrentChild != null) {
            txtCurrentChild.setText("No child selected");
        }

        // Buttons
        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        Button btnAddReport = findViewById(R.id.btnAddReport);
        Button btnAddTask = findViewById(R.id.btnAddTask);
        Button btnTherapistNotes = findViewById(R.id.btnTherapistNotes);
        Button btnLogout = findViewById(R.id.btnLogout);

        // 👉 All these screens will work for the CURRENT selected child
        btnViewHistory.setOnClickListener(v -> {
            Intent i = new Intent(ParentHomeActivity.this, EmotionHistoryActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        btnAddReport.setOnClickListener(v -> {
            Intent i = new Intent(ParentHomeActivity.this, NewReportActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        btnAddTask.setOnClickListener(v -> {
            Intent i = new Intent(ParentHomeActivity.this, NewTaskActivity.class);
            i.putExtra("PARENT_ID", parentId);
            putChildExtras(i);
            startActivity(i);
        });

        btnTherapistNotes.setOnClickListener(v -> {
            Intent i = new Intent(ParentHomeActivity.this, TherapistNotesActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> finish());

        // Load children for this parent
        loadChildrenForParent();
    }

    // helper: attach selected child info to any Intent
    private void putChildExtras(Intent i) {
        if (selectedChildId != null) {
            i.putExtra("CHILD_ID", selectedChildId);
            i.putExtra("CHILD_NAME", selectedChildName);
        }
    }

    private void loadChildrenForParent() {
        if (parentId == null || parentId.isEmpty()) {
            return;
        }

        db.collection("children")
                .whereEqualTo("parentID", parentId)
                .get()
                .addOnSuccessListener(this::buildChildBar)
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            ParentHomeActivity.this,
                            "Failed to load children: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                    childScroll.setVisibility(View.GONE);
                    if (txtCurrentChild != null) {
                        txtCurrentChild.setText("No children linked yet");
                    }
                });
    }

    private void buildChildBar(QuerySnapshot qs) {
        childContainer.removeAllViews();
        selectedChildId = null;
        selectedChildName = null;
        selectedChildView = null;

        if (qs == null || qs.isEmpty()) {
            childScroll.setVisibility(View.GONE);
            if (txtCurrentChild != null) {
                txtCurrentChild.setText("No children linked yet");
            }
            return;
        }

        childScroll.setVisibility(View.VISIBLE);

        float density = getResources().getDisplayMetrics().density;
        int itemWidth = (int) (90 * density);
        int iconSize = (int) (70 * density);
        int padding = (int) (4 * density);
        int marginH = (int) (8 * density);

        int index = 0;

        for (DocumentSnapshot doc : qs.getDocuments()) {
            String childId = doc.getId();
            String childName = doc.getString("name");
            if (childName == null || childName.trim().isEmpty()) {
                childName = "Child";
            }

            // Outer vertical layout for one child
            LinearLayout childItem = new LinearLayout(this);
            childItem.setOrientation(LinearLayout.VERTICAL);
            childItem.setGravity(Gravity.CENTER_HORIZONTAL);
            childItem.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams itemParams =
                    new LinearLayout.LayoutParams(itemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            itemParams.setMargins(marginH, 0, marginH, 0);
            childItem.setLayoutParams(itemParams);

            // Icon
            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.ic_child);

            // Name
            TextView nameView = new TextView(this);
            nameView.setText(childName);
            nameView.setTextSize(14);
            nameView.setGravity(Gravity.CENTER);
            nameView.setTextColor(getResources().getColor(R.color.black));
            nameView.setPadding(0, padding, 0, 0);

            // Add views
            childItem.addView(icon);
            childItem.addView(nameView);

            final String finalChildId = childId;
            final String finalChildName = childName;

            // ONLY change the current child context, do NOT open any screen
            childItem.setOnClickListener(v -> {
                setSelectedChild(finalChildId, finalChildName, childItem);
                // no startActivity here – home stays open, but now focused on this child
            });

            // Add this child item to the container
            childContainer.addView(childItem);

            // Auto-select the first child (no navigation)
            if (index == 0) {
                setSelectedChild(finalChildId, finalChildName, childItem);
            }
            index++;
        }
    }

    // highlight selected child + store id/name + update label
    private void setSelectedChild(String childId, String childName, View childView) {
        selectedChildId = childId;
        selectedChildName = childName;

        if (selectedChildView != null) {
            selectedChildView.setBackground(null);
        }

        childView.setBackground(getResources().getDrawable(R.drawable.white_card_pg));
        selectedChildView = childView;

        if (txtCurrentChild != null) {
            txtCurrentChild.setText("Now viewing: " + selectedChildName);
        }

        Toast.makeText(
                this,
                "Viewing: " + selectedChildName,
                Toast.LENGTH_SHORT
        ).show();
    }
}