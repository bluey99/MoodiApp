package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.asdproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NewReportActivity extends AppCompatActivity {

    private static final String PREFS = "reports_prefs";
    private static final String KEY_REPORTS_PREFIX = "reports_list_";

    private EditText edtSituation, edtDateTime, edtLocation,
            edtChildReaction, edtHowHandled, edtQuestionsTherapist;

    // chosen date & time
    private final Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    // 🔹 child context from ParentHomeActivity (NEW, but safe)
    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);

        // get child context from parent home (may be null if not sent)
        Intent intent = getIntent();
        childId = intent.getStringExtra("CHILD_ID");
        childName = intent.getStringExtra("CHILD_NAME");

        edtSituation          = findViewById(R.id.edtSituation);
        edtDateTime           = findViewById(R.id.edtDateTime);
        edtLocation           = findViewById(R.id.edtLocation);
        edtChildReaction      = findViewById(R.id.edtChildReaction);
        edtHowHandled         = findViewById(R.id.edtHowHandled);
        edtQuestionsTherapist = findViewById(R.id.edtQuestionsTherapist);

        Button btnViewHistory  = findViewById(R.id.btnViewHistory);
        Button btnSendReport   = findViewById(R.id.btnSendReport);
        Button btnGoBackReport = findViewById(R.id.btnGoBackReport);

        // Timestamp: only pick via dialog (no typing / no paste)
        edtDateTime.setFocusable(false);
        edtDateTime.setClickable(true);
        edtDateTime.setLongClickable(false);
        edtDateTime.setOnClickListener(v -> showDateTimePicker());

        btnViewHistory.setOnClickListener(v -> {
            Intent i = new Intent(NewReportActivity.this, ReportsHistoryActivity.class);
            // We pass child info forward (even if history still shows all reports)
            if (childId != null) {
                i.putExtra("CHILD_ID", childId);
                i.putExtra("CHILD_NAME", childName);
            }
            startActivity(i);
        });

        btnSendReport.setOnClickListener(v -> sendReport());

        btnGoBackReport.setOnClickListener(v -> finish());
    }

    // --------- DATE + TIME PICKERS (no past allowed) ----------

    private void showDateTimePicker() {
        int year  = selectedDateTime.get(Calendar.YEAR);
        int month = selectedDateTime.get(Calendar.MONTH);
        int day   = selectedDateTime.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {

                    // Build chosen date at midnight
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d, 0, 0, 0);

                    // Today at midnight
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    // Block past days
                    if (chosen.before(today)) {
                        Toast.makeText(this,
                                "You cannot choose a past date!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Accept date
                    selectedDateTime.set(Calendar.YEAR, y);
                    selectedDateTime.set(Calendar.MONTH, m);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, d);

                    // now pick time
                    showTimePicker();
                },
                year, month, day
        );

        // Extra protection: user cannot scroll before today
        dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        dp.show();
    }

    private void showTimePicker() {
        int hour   = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog tp = new TimePickerDialog(
                this,
                (view, h, m) -> {

                    Calendar chosen = (Calendar) selectedDateTime.clone();
                    chosen.set(Calendar.HOUR_OF_DAY, h);
                    chosen.set(Calendar.MINUTE, m);
                    chosen.set(Calendar.SECOND, 0);
                    chosen.set(Calendar.MILLISECOND, 0);

                    Calendar now = Calendar.getInstance();

                    // If date is today -> block past time
                    Calendar today = Calendar.getInstance();
                    if (chosen.get(Calendar.YEAR)  == today.get(Calendar.YEAR) &&
                            chosen.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            chosen.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {

                        if (chosen.before(now)) {
                            Toast.makeText(this,
                                    "You cannot choose a past time!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    // Accept time
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
                    selectedDateTime.set(Calendar.MINUTE, m);

                    String formatted = dateTimeFormat.format(selectedDateTime.getTime());
                    edtDateTime.setText(formatted);
                },
                hour, minute,
                true // 24h format; change to false for AM/PM
        );

        tp.show();
    }

    // --------- STORAGE HELPERS ----------

    // ✅ Back to ORIGINAL logic: key is per PARENT (FirebaseAuth UID)
    private String getReportsKeyForCurrentParent() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "guest";
        return KEY_REPORTS_PREFIX + uid;
    }

    private void sendReport() {
        String situation = edtSituation.getText().toString().trim();
        String timestamp = edtDateTime.getText().toString().trim();
        String location  = edtLocation.getText().toString().trim();

        String childReaction = edtChildReaction.getText().toString().trim();
        String howHandled    = edtHowHandled.getText().toString().trim();
        String questions     = edtQuestionsTherapist.getText().toString().trim(); // optional

        // Required fields
        if (situation.isEmpty() || timestamp.isEmpty()
                || location.isEmpty() || childReaction.isEmpty()
                || howHandled.isEmpty()) {

            Toast.makeText(this,
                    "Please fill all the required fields!!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        saveFullReport(situation, timestamp, location,
                childReaction, howHandled, questions);

        Toast.makeText(this, "Report sent", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveFullReport(String situation, String timestamp, String location,
                                String childReaction, String howHandled, String questions) {

        // same key as before → ReportsHistoryActivity sees the data again
        String key = getReportsKeyForCurrentParent();

        try {
            String json = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(key, "[]");

            JSONArray arr = new JSONArray(json);

            JSONObject obj = new JSONObject();
            obj.put("situation",     situation);
            obj.put("timestamp",     timestamp);
            obj.put("location",      location);
            obj.put("childReaction", childReaction);
            obj.put("howHandled",    howHandled);
            obj.put("questions",     questions);

            // 🔹 extra info – safe, won't break old code
            obj.put("childId",   childId);
            obj.put("childName", childName);

            arr.put(obj);

            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(key, arr.toString())
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}