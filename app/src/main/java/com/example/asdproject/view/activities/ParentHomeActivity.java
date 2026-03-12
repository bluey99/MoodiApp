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
import com.example.asdproject.util.LocaleManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class ParentHomeActivity extends BaseActivity {

    private String parentId;
    private FirebaseFirestore db;

    private HorizontalScrollView childScroll;
    private LinearLayout childContainer;
    private TextView txtCurrentChild;

    private String selectedChildId = null;
    private String selectedChildName = null;
    private View selectedChildView = null;

    private TextView txtTaskNotification;

    private ImageView btnBell;
    private View notifDot;

    private ListenerRegistration notifReg;

    private String latestNotifId = null;
    private String latestNotifMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Apply saved language BEFORE layout
        LocaleManager.setLocale(this);

        setContentView(R.layout.activity_parent_home);

        parentId = getIntent().getStringExtra("PARENT_ID");
        db = FirebaseFirestore.getInstance();

        childScroll = findViewById(R.id.childScroll);
        childContainer = findViewById(R.id.childContainer);
        txtCurrentChild = findViewById(R.id.txtCurrentChild);
        txtTaskNotification = findViewById(R.id.txtTaskNotification);
        btnBell = findViewById(R.id.btnBell);
        notifDot = findViewById(R.id.notifDot);

        if (txtTaskNotification != null)
            txtTaskNotification.setVisibility(View.GONE);

        if (notifDot != null)
            notifDot.setVisibility(View.GONE);

        // 🌐 Language toggle
        TextView btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> {
                LocaleManager.toggleLanguage(this);
                recreate();
            });
        }

        if (btnBell != null) {
            btnBell.setOnClickListener(v -> openNotificationPopup());
        }

        txtCurrentChild.setText(getString(R.string.parent_home_no_child_selected));

        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        Button btnAddReport = findViewById(R.id.btnAddReport);
        Button btnAddTask = findViewById(R.id.btnAddTask);
        Button btnTherapistNotes = findViewById(R.id.btnTherapistNotes);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnViewHistory.setOnClickListener(v -> {
            Intent i = new Intent(this, SelectionHistoryActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        btnAddReport.setOnClickListener(v -> {
            Intent i = new Intent(this, NewReportActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        btnAddTask.setOnClickListener(v -> {
            Intent i = new Intent(this, NewTaskActivity.class);
            i.putExtra("PARENT_ID", parentId);
            putChildExtras(i);
            startActivity(i);
        });

        btnTherapistNotes.setOnClickListener(v -> {
            Intent i = new Intent(this, TherapistNotesActivity.class);
            putChildExtras(i);
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());


        loadChildrenForParent();
        listenForUnreadNotifications();
    }
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_message))
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.parent_home_logout), (dialog, which) -> {
                    finish();
                })
                .show();
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

        if (parentId == null || parentId.isEmpty())
            return;

        db.collection("parents")
                .document(parentId)
                .get()
                .addOnSuccessListener(parentDoc -> {

                    if (!parentDoc.exists()) {
                        txtCurrentChild.setText(getString(R.string.parent_home_parent_not_found));
                        return;
                    }

                    List<String> linkedChildren =
                            (List<String>) parentDoc.get("linkedChildren");

                    if (linkedChildren == null || linkedChildren.isEmpty()) {
                        childScroll.setVisibility(View.GONE);
                        txtCurrentChild.setText(getString(R.string.parent_home_no_children_linked));
                        return;
                    }

                    db.collection("children")
                            .whereIn("childID", linkedChildren)
                            .get()
                            .addOnSuccessListener(this::buildChildBar)
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            getString(R.string.parent_home_failed_load_children) + " " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.parent_home_failed_load_parent) + " " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void buildChildBar(QuerySnapshot qs) {

        childContainer.removeAllViews();
        selectedChildId = null;
        selectedChildName = null;
        selectedChildView = null;

        if (qs == null || qs.isEmpty()) {
            childScroll.setVisibility(View.GONE);
            txtCurrentChild.setText(getString(R.string.parent_home_no_children_found));
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

            String childId = doc.getString("childID");
            String childName = doc.getString("name");

            if (childName == null || childName.trim().isEmpty())
                childName = getString(R.string.parent_home_default_child_name);

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

            childItem.setOnClickListener(v ->
                    setSelectedChild(finalChildId, finalChildName, childItem));

            childContainer.addView(childItem);

            if (index == 0)
                setSelectedChild(finalChildId, finalChildName, childItem);

            index++;
        }
    }

    private void setSelectedChild(String childId, String childName, View childView) {

        selectedChildId = childId;
        selectedChildName = childName;

        if (selectedChildView != null)
            selectedChildView.setBackground(null);

        childView.setBackground(getResources().getDrawable(R.drawable.white_card_pg));
        selectedChildView = childView;

        txtCurrentChild.setText(
                getString(R.string.parent_home_now_viewing, selectedChildName)
        );

        Toast.makeText(this,
                getString(R.string.parent_home_toast_viewing, selectedChildName),
                Toast.LENGTH_SHORT).show();
    }

    private void listenForUnreadNotifications() {

        if (parentId == null || parentId.trim().isEmpty())
            return;

        Query q = db.collection("notifications")
                .whereEqualTo("receiverType", "PARENT")
                .whereEqualTo("receiverId", parentId)
                .whereEqualTo("read", false)
                .limit(1);

        notifReg = q.addSnapshotListener((snap, err) -> {

            if (err != null) {
                Toast.makeText(this,
                        getString(R.string.parent_home_notif_error) + " " + err.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (snap == null || snap.isEmpty()) {
                latestNotifId = null;
                latestNotifMessage = null;
                if (notifDot != null)
                    notifDot.setVisibility(View.GONE);
                return;
            }

            DocumentSnapshot d = snap.getDocuments().get(0);
            latestNotifId = d.getId();

            String msg = d.getString("message");
            if (msg == null || msg.trim().isEmpty())
                msg = getString(R.string.parent_home_task_done_default);

            latestNotifMessage = msg;

            if (notifDot != null)
                notifDot.setVisibility(View.VISIBLE);
        });
    }

    private void openNotificationPopup() {

        String messageToShow = (latestNotifMessage == null)
                ? getString(R.string.parent_home_no_new_notifications)
                : latestNotifMessage;

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.parent_home_notification_title))
                .setMessage(messageToShow)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {

                    if (latestNotifId != null) {
                        db.collection("notifications")
                                .document(latestNotifId)
                                .update("read", true);
                    }

                    dialog.dismiss();
                })
                .show();
    }
}