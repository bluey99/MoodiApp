package com.example.asdproject.view.activities;



import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.model.EmotionLog;
import com.example.asdproject.notifications.ChildFirebaseMessagingService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import com.example.asdproject.view.fragments.NotificationsBottomSheetFragment;



/**
 * ChildHomeActivity
 * Displays the main home screen for the child user after login.
 * Acts as a central navigation hub and provides gentle, non-intrusive
 * feedback about recent emotional check-ins.
 * Design principles:
 * - No pressure or negative feedback
 * - No notifications or reminders
 * - Positive reflection only
 */
public class ChildHomeActivity extends AppCompatActivity {

    // Greeting and navigation UI elements
    private TextView txtGreeting;
    private LinearLayout btnLogEmotion;
    private LinearLayout btnTasks;
    private LinearLayout btnHistory;
    private LinearLayout btnCalmingTools;
    private ImageView btnNotifications;
    private ImageView btnSettings;
    private View viewNotificationDot;



    // Gentle check-in indicator (streak-style feedback)
    private LinearLayout layoutStreak;
    private TextView txtStreak;

    // Firestore document ID representing the logged-in child
    private String childId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        //ChildFirebaseMessagingService.testLocalNotification(this);//This creates/triggers a system-level notification.we couldnt implement it becuase of firebase plan limitations so we are keeping it off.

        //Bind header UI elements
        btnNotifications = findViewById(R.id.btnNotifications);
        viewNotificationDot = findViewById(R.id.viewNotificationDot);

        btnSettings = findViewById(R.id.btnSettings);

        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            NotificationsBottomSheetFragment sheet =
                    NotificationsBottomSheetFragment.newInstance(childId);

            sheet.setOnNotificationsDismissed(() -> {
                checkForNewTasks(); // refresh red dot after notifications are seen
            });

            sheet.show(getSupportFragmentManager(), "NotificationsBottomSheet");
        });




        // Bind streak UI elements
        layoutStreak = findViewById(R.id.layoutStreak);
        txtStreak = findViewById(R.id.txtStreak);

        // Retrieve child information passed from LoginActivity
        String childName = getIntent().getStringExtra("childName");
        childId = getIntent().getStringExtra("childId");



        // Bind navigation UI elements
        txtGreeting = findViewById(R.id.txtGreeting);
        btnLogEmotion = findViewById(R.id.btnLogEmotion);
        btnTasks = findViewById(R.id.btnTasks);
        btnHistory = findViewById(R.id.btnHistory);
        btnCalmingTools = findViewById(R.id.btnCalmingTools);

        // Display personalized greeting
        txtGreeting.setText(getTimeBasedGreeting() + ", " + childName + "!");


        // Navigate to emotion logging flow
        btnLogEmotion.setOnClickListener(v -> {
            Intent intent = new Intent(this, EmotionLogActivity.class);
            intent.putExtra("childId", childId);
            startActivity(intent);
        });

        // Navigate to emotion history screen
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChildHistoryActivity.class);
            intent.putExtra("childId", childId);
            startActivity(intent);
        });

        //Navigate to Task screen
        btnTasks.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, ChildTasksActivity.class);
            intent.putExtra("childId", childId);// can be null, ChildTasksActivity will handle
            intent.putExtra("childName", childName);
            startActivity(intent);
        });

        //Navigate to Calming tools screen
        btnCalmingTools.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, ChildCalmingToolsActivity.class);
            intent.putExtra("childId", childId); // optional, but consistent
            startActivity(intent);
        });


        // Initial load of check-in feedback
        loadCheckInStreak();
    }

    /**
     * Loads recent emotion logs from Firestore and computes
     * the number of consecutive days with at least one emotional check-in.
     *
     * This method does not create pressure:
     * - If no recent activity exists, the indicator is hidden
     * - No reminders or negative messages are shown
     */
    private void loadCheckInStreak() {

        if (childId == null || childId.isEmpty()) {
            layoutStreak.setVisibility(LinearLayout.GONE);
            return;
        }

        FirebaseFirestore db = FirebaseManager.getDb();

        // 1) Resolve the Firestore document id by searching using the REAL childID
        db.collection("children")
                .whereEqualTo("childID", childId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        layoutStreak.setVisibility(LinearLayout.GONE);
                        return;
                    }

                    String resolvedDocId = query.getDocuments().get(0).getId();

                    // 2) Use the resolved doc id to reach the history subcollection
                    db.collection("children")
                            .document(resolvedDocId)
                            .collection("history")
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                if (snapshot.isEmpty()) {
                                    layoutStreak.setVisibility(LinearLayout.GONE);
                                    return;
                                }

                                Set<String> loggedDays = new HashSet<>();
                                Calendar cal = Calendar.getInstance();

                                snapshot.forEach(doc -> {
                                    EmotionLog log = doc.toObject(EmotionLog.class);
                                    if (log.getTimestamp() == null) return;

                                    cal.setTime(log.getTimestamp().toDate());

                                    String dayKey =
                                            cal.get(Calendar.YEAR) + "-" +
                                                    cal.get(Calendar.DAY_OF_YEAR);

                                    loggedDays.add(dayKey);
                                });

                                int consecutiveDays = calculateConsecutiveDays(loggedDays);
                                applyGentleStreakMessage(consecutiveDays);
                            })
                            .addOnFailureListener(e ->
                                    layoutStreak.setVisibility(LinearLayout.GONE)
                            );
                })
                .addOnFailureListener(e ->
                        layoutStreak.setVisibility(LinearLayout.GONE)
                );
    }


    /**
     * Calculates the number of consecutive calendar days,
     * starting from today and going backwards,
     * in which at least one emotion log exists.
     *
     * @param loggedDays Set of unique day identifiers containing logs
     * @return number of consecutive days with activity
     */
    private int calculateConsecutiveDays(Set<String> loggedDays) {

        Calendar cal = Calendar.getInstance();
        int count = 0;

        while (true) {
            String key =
                    cal.get(Calendar.YEAR) + "-" +
                            cal.get(Calendar.DAY_OF_YEAR);

            if (loggedDays.contains(key)) {
                count++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }

        return count;
    }

    /**
     * Displays a gentle, supportive message based on recent check-in activity.
     *
     * This feedback is intentionally:
     * - Non-competitive
     * - Non-punitive
     * - Optional (hidden when not applicable)
     *
     * @param days number of consecutive check-in days
     */
    private void applyGentleStreakMessage(int days) {

        if (days <= 0) {
            layoutStreak.setVisibility(LinearLayout.GONE);
            return;
        }

        String message;

        if (days == 1) {
            message = "\uD83C\uDF31 You checked in today";
        } else if (days == 2) {
            message = "\uD83C\uDF31 You’ve been checking in";
        } else if (days <= 4) {
            message = "\uD83C\uDF31 You’ve been checking in for " + days + " days";
        } else {
            message = "\uD83C\uDF31 You’ve been checking in regularly (" + days + " days)";
        }

        txtStreak.setText(message);
        layoutStreak.setVisibility(LinearLayout.VISIBLE);
    }

    /**
     * Ensures that the check-in feedback is refreshed whenever
     * the activity becomes visible again (e.g., after logging a feeling).
     */
    @Override
    protected void onResume() {
        super.onResume();
        txtGreeting.setText(getTimeBasedGreeting() + ", " + getIntent().getStringExtra("childName") + "!");
        loadCheckInStreak();
        checkForNewTasks(); //  update notification dot
    }


    /**
     * Returns a greeting message based on the current time of day.
     *
     * Used to display a friendly, context-aware greeting
     * when the child opens the home screen.
     *
     * @return a time-appropriate greeting string
     */
    private String getTimeBasedGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour <= 11) {
            return "Good morning";
        } else if (hour >= 12 && hour <= 16) {
            return "Good afternoon";
        } else if (hour >= 17 && hour <= 21) {
            return "Good evening";
        } else {
            return "Hello";
        }
    }
    private void checkForNewTasks() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks")
                .whereEqualTo("childId", childId)
                .whereEqualTo("status", "ASSIGNED")
                .get()
                .addOnSuccessListener(snapshot1 -> {

                    boolean hasUnseen = false;

                    for (var doc : snapshot1.getDocuments()) {
                        // unseen = seenByChild != true (false OR missing)
                        if (!Boolean.TRUE.equals(doc.getBoolean("seenByChild"))) {
                            hasUnseen = true;
                            break;
                        }
                    }

                    if (hasUnseen) {
                        viewNotificationDot.setVisibility(View.VISIBLE);
                        return;
                    }

                    // ---------- fallback for childID ----------
                    db.collection("tasks")
                            .whereEqualTo("childID", childId)
                            .whereEqualTo("status", "ASSIGNED")
                            .get()
                            .addOnSuccessListener(snapshot2 -> {

                                boolean hasUnseenFallback = false;

                                for (var doc : snapshot2.getDocuments()) {
                                    if (!Boolean.TRUE.equals(doc.getBoolean("seenByChild"))) {
                                        hasUnseenFallback = true;
                                        break;
                                    }
                                }

                                viewNotificationDot.setVisibility(
                                        hasUnseenFallback ? View.VISIBLE : View.GONE
                                );
                            });
                });
    }






}
