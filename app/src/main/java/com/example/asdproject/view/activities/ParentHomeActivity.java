package com.example.asdproject.view.activities;

import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
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

    // keep your existing TextView (won't be used as main notification UI)
    private TextView txtTaskNotification;

    // ✅ NEW: bell + red dot (from your XML)
    private ImageView btnBell;
    private View notifDot;

    private ListenerRegistration notifReg;

    // latest unread notification
    private String latestNotifId = null;
    private String latestNotifMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        Intent intent = getIntent();
        parentId = intent.getStringExtra("PARENT_ID");

        db = FirebaseFirestore.getInstance();

        // child bar
        childScroll = findViewById(R.id.childScroll);
        childContainer = findViewById(R.id.childContainer);
        txtCurrentChild = findViewById(R.id.txtCurrentChild);

        // existing message TextView (leave it as-is)
        txtTaskNotification = findViewById(R.id.txtTaskNotification);
        if (txtTaskNotification != null) {
            txtTaskNotification.setVisibility(View.GONE);
        }

        // ✅ Bell + Red dot
        btnBell = findViewById(R.id.btnBell);
        notifDot = findViewById(R.id.notifDot);
        if (notifDot != null) notifDot.setVisibility(View.GONE);

        if (btnBell != null) {
            btnBell.setOnClickListener(v -> openNotificationPopup());
        }

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

        btnViewHistory.setOnClickListener(v -> {
            Intent i = new Intent(ParentHomeActivity.this, SelectionHistoryActivity.class);
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

        loadChildrenForParent();

        // ✅ This is what makes the dot appear
        listenForUnreadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifReg != null) {
            notifReg.remove();
            notifReg = null;
        }
    }

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

            LinearLayout childItem = new LinearLayout(this);
            childItem.setOrientation(LinearLayout.VERTICAL);
            childItem.setGravity(Gravity.CENTER_HORIZONTAL);
            childItem.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams itemParams =
                    new LinearLayout.LayoutParams(itemWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
            itemParams.setMargins(marginH, 0, marginH, 0);
            childItem.setLayoutParams(itemParams);

            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.ic_child);

            TextView nameView = new TextView(this);
            nameView.setText(childName);
            nameView.setTextSize(14);
            nameView.setGravity(Gravity.CENTER);
            nameView.setTextColor(getResources().getColor(R.color.black));
            nameView.setPadding(0, padding, 0, 0);

            childItem.addView(icon);
            childItem.addView(nameView);

            final String finalChildId = childId;
            final String finalChildName = childName;

            childItem.setOnClickListener(v -> setSelectedChild(finalChildId, finalChildName, childItem));

            childContainer.addView(childItem);

            if (index == 0) {
                setSelectedChild(finalChildId, finalChildName, childItem);
            }
            index++;
        }
    }

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

        Toast.makeText(this, "Viewing: " + selectedChildName, Toast.LENGTH_SHORT).show();
    }

    // ==========================================================
    // ✅ NOTIFICATIONS (red dot + popup)
    // ==========================================================

    private void listenForUnreadNotifications() {
        if (parentId == null || parentId.trim().isEmpty()) return;

        Query q = db.collection("notifications")
                .whereEqualTo("receiverType", "PARENT")
                .whereEqualTo("receiverId", parentId)
                .whereEqualTo("read", false)
                .limit(1);

        notifReg = q.addSnapshotListener((snap, err) -> {

            // ✅ show errors instead of silent fail
            if (err != null) {
                Toast.makeText(this, "Notif error: " + err.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (snap == null || snap.isEmpty()) {
                latestNotifId = null;
                latestNotifMessage = null;
                if (notifDot != null) notifDot.setVisibility(View.GONE);
                return;
            }

            DocumentSnapshot d = snap.getDocuments().get(0);
            latestNotifId = d.getId();

            String msg = d.getString("message");
            if (msg == null || msg.trim().isEmpty()) msg = "Child has finished the task.";
            latestNotifMessage = msg;

            if (notifDot != null) notifDot.setVisibility(View.VISIBLE);
        });
    }

    private void openNotificationPopup() {
        // ✅ ALWAYS show something when clicking bell
        String messageToShow = (latestNotifMessage == null)
                ? "No new notifications"
                : latestNotifMessage;

        new AlertDialog.Builder(this)
                .setTitle("Notification")
                .setMessage(messageToShow)
                .setPositiveButton("OK", (dialog, which) -> {
                    // mark as read only if we have a real notification
                    if (latestNotifId != null) {
                        db.collection("notifications")
                                .document(latestNotifId)
                                .update("read", true)
                                .addOnSuccessListener(v -> {
                                    latestNotifId = null;
                                    latestNotifMessage = null;
                                    if (notifDot != null) notifDot.setVisibility(View.GONE);
                                });
                    }
                    dialog.dismiss();
                })
                .show();
    }
}
