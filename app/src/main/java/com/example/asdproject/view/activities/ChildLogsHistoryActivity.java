package com.example.asdproject.view.activities;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.controller.FirebaseManager;
import com.example.asdproject.util.LocaleManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChildLogsHistoryActivity extends BaseActivity {

    private String childIdOrField;
    private String childDocId;
    private String childName;

    private RecyclerView recycler;
    private LogsAdapter adapter;

    private final List<LogItem> logs = new ArrayList<>();
    private final List<LogItem> allLogs = new ArrayList<>();

    private ListenerRegistration historyReg;

    // ===================== FILTERS =====================
    private enum TimeMode { ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS }
    private TimeMode timeMode = TimeMode.ALL;

    // Emotion filter
    private String selectedEmotionKey = "";   // happy/sad/angry/afraid/disgusted/surprised/unsure/other
    private String otherEmotionText = "";     // only if selectedEmotionKey == "other"

    // Situation filter
    private String selectedSituationKey = ""; // gift/fight/test/other
    private String otherSituationText = "";

    // Location filter
    private String selectedLocationKey = "";  // school/home/dontknow/other
    private String otherLocationText = "";

    private SimpleDateFormat sdf;

    // Emotion keys
    private static final String[] EMOTION_KEYS = new String[] {
            "happy", "sad", "angry", "afraid", "disgusted", "surprised", "unsure", "other"
    };

    // Situation keys (mom-friendly)
    private static final String[] SITUATION_KEYS = new String[] {
            "gift", "fight", "test", "other"
    };

    // Location keys
    private static final String[] LOCATION_KEYS = new String[] {
            "school", "home", "dontknow", "other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleManager.setLocale(this);
        Locale current = getResources().getConfiguration().getLocales().get(0);
        sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", current);
        setContentView(R.layout.activity_child_logs_history);

        childIdOrField = firstNonEmpty(
                getIntent().getStringExtra("CHILD_ID"),
                getIntent().getStringExtra("childId"),
                getIntent().getStringExtra("focusId")
        );

        childName = firstNonEmpty(
                getIntent().getStringExtra("CHILD_NAME"),
                getIntent().getStringExtra("childName")
        );

        // Title (optional)
        setTitle(!isEmpty(childName)
                ? getString(R.string.child_logs_title_with_name, childName)
                : getString(R.string.child_logs_title));

        recycler = findViewById(R.id.recyclerChildLogs);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogsAdapter(logs);
        recycler.setAdapter(adapter);

        Button btnGoBack = findViewById(R.id.btnGoBackChildLogs);
        Button btnFilterBy = findViewById(R.id.btnFilterBy);
        TextView btnLanguage = findViewById(R.id.btnLanguage);

        btnGoBack.setOnClickListener(v -> finish());
        btnFilterBy.setOnClickListener(v -> showPrettyFilterBottomSheet());

        btnLanguage.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate();
        });

        if (isEmpty(childIdOrField)) {
            Toast.makeText(this, getString(R.string.missing_child_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseManager.init(this);
        resolveChildDocIdAndListen();
    }

    // ==========================================================
    // Resolve doc id (supports both docId and childID field)
    // ==========================================================
    private void resolveChildDocIdAndListen() {
        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("children")
                .document(childIdOrField)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        childDocId = doc.getId();
                        listenToLogs();
                    } else {
                        db.collection("children")
                                .whereEqualTo("childID", childIdOrField)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    if (qs != null && !qs.isEmpty()) {
                                        childDocId = qs.getDocuments().get(0).getId();
                                        listenToLogs();
                                    } else {
                                        Toast.makeText(this, getString(R.string.child_not_found), Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_prefix) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // ==========================================================
    // Live listener
    // ==========================================================
    private void listenToLogs() {
        FirebaseFirestore db = FirebaseManager.getDb();

        if (historyReg != null) historyReg.remove();

        historyReg = db.collection("children")
                .document(childDocId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, getString(R.string.error_prefix) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap == null) return;

                    allLogs.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (isTaskLog(doc)) continue;

                        String logId = safe(doc.getString("id"));

                        // ✅ FILTER: skip TASK logs
                        if ("TASK".equals(logId)) continue;

                        String situation = safe(doc.getString("situation"));
                        String location  = safe(doc.getString("location"));
                        String emotion   = safe(doc.getString("feeling"));
                        String note      = safe(doc.getString("note"));

                        Long intensityLong = doc.getLong("intensity");
                        int intensity = (intensityLong == null) ? 0 : intensityLong.intValue();

                        Timestamp ts = doc.getTimestamp("timestamp");
                        long tsMillis = (ts == null) ? 0L : ts.toDate().getTime();
                        String tsText = (tsMillis == 0L) ? "" : sdf.format(ts.toDate());


                        allLogs.add(new LogItem(tsText, situation, location, emotion, intensity, note, tsMillis));
                    }

                    applyFilters();
                });
    }

    private boolean isTaskLog(DocumentSnapshot doc) {
        if (doc == null) return false;

        String logType = doc.getString("logType");
        if (logType != null && logType.trim().equalsIgnoreCase("TASK")) return true;

        String id = doc.getString("id");
        return id != null && id.trim().equalsIgnoreCase("TASK");
    }




    // ==========================================================
    // ✅ Pretty Filter Bottom Sheet (Time + Emotion + Situation + Location)
    // ==========================================================
    private void showPrettyFilterBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        // Title
        TextView title = new TextView(this);
        title.setText(getString(R.string.filter_title)); // "Filter"
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        // ----- TIME -----
        root.addView(sectionLabel(getString(R.string.filter_time_section))); // "Time"
        RadioGroup rgTime = new RadioGroup(this);
        rgTime.setOrientation(LinearLayout.VERTICAL);

        RadioButton rbAll   = timeRadio(getString(R.string.filter_time_all),  TimeMode.ALL);
        RadioButton rbToday = timeRadio(getString(R.string.filter_time_today), TimeMode.TODAY);
        RadioButton rb7     = timeRadio(getString(R.string.filter_time_7),    TimeMode.LAST_7_DAYS);
        RadioButton rb30    = timeRadio(getString(R.string.filter_time_30),   TimeMode.LAST_30_DAYS);

        rgTime.addView(rbAll);
        rgTime.addView(rbToday);
        rgTime.addView(rb7);
        rgTime.addView(rb30);

        if (timeMode == TimeMode.ALL) rbAll.setChecked(true);
        else if (timeMode == TimeMode.TODAY) rbToday.setChecked(true);
        else if (timeMode == TimeMode.LAST_7_DAYS) rb7.setChecked(true);
        else if (timeMode == TimeMode.LAST_30_DAYS) rb30.setChecked(true);

        root.addView(rgTime);

        // ----- EMOTION -----
        root.addView(sectionLabel(getString(R.string.filter_emotion_section))); // "Emotion"

        Button btnPickEmotion = nicePickerButton(getEmotionPickerButtonText());
        btnPickEmotion.setOnClickListener(v -> {
            sheet.dismiss();
            showEmotionPickerDialog();
        });
        root.addView(btnPickEmotion);

        // ----- SITUATION -----
        root.addView(sectionLabel(getString(R.string.filter_situation_search))); // reuse label if you have it
        Button btnPickSituation = nicePickerButton(getSituationPickerButtonText());
        btnPickSituation.setOnClickListener(v -> {
            sheet.dismiss();
            showSituationPickerDialog();
        });
        root.addView(btnPickSituation);

        // ----- LOCATION -----
        root.addView(sectionLabel(getString(R.string.filter_location_search))); // reuse label if you have it
        Button btnPickLocation = nicePickerButton(getLocationPickerButtonText());
        btnPickLocation.setOnClickListener(v -> {
            sheet.dismiss();
            showLocationPickerDialog();
        });
        root.addView(btnPickLocation);

        // ----- ACTIONS -----
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(14), 0, 0);

        Button btnClear = new Button(this);
        btnClear.setAllCaps(false);
        btnClear.setText(getString(R.string.filter_clear));
        btnClear.setOnClickListener(v -> {
            timeMode = TimeMode.ALL;

            selectedEmotionKey = "";
            otherEmotionText = "";

            selectedSituationKey = "";
            otherSituationText = "";

            selectedLocationKey = "";
            otherLocationText = "";

            applyFilters();
            sheet.dismiss();
        });

        Button btnApply = new Button(this);
        btnApply.setAllCaps(false);
        btnApply.setText(getString(R.string.apply));
        btnApply.setOnClickListener(v -> {
            int checkedId = rgTime.getCheckedRadioButtonId();
            View checked = rgTime.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof TimeMode) {
                timeMode = (TimeMode) checked.getTag();
            }
            applyFilters();
            sheet.dismiss();
        });

        actions.addView(btnClear);
        Space sp = new Space(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        actions.addView(sp);
        actions.addView(btnApply);

        root.addView(actions);

        sheet.setContentView(root);
        sheet.show();
    }

    private Button nicePickerButton(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setText(text);
        b.setBackgroundTintList(ColorStateList.valueOf(0xFFFFFFFF));
        b.setTextColor(0xFF085F63);
        return b;
    }

    private TextView sectionLabel(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextSize(14);
        tv.setPadding(0, dp(12), 0, dp(6));
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private RadioButton timeRadio(String text, TimeMode tag) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        rb.setTag(tag);
        rb.setTextSize(14);
        return rb;
    }

    // ==========================================================
    // ✅ Emotion picker dialog (NO giant images + correct selection)
    // ==========================================================
    private void showEmotionPickerDialog() {

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.filter_choose_emotion));

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(root);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(LinearLayout.VERTICAL);

        // IMPORTANT: children of RadioGroup must be RadioButtons
        for (String key : EMOTION_KEYS) {
            RadioButton rb = new RadioButton(this);
            rb.setTag(key);
            rb.setText(getEmotionLabel(key)); // text appears (not just emoji)
            rb.setCompoundDrawablePadding(dp(10));

            int icon = getEmotionIconRes(key);
            if (icon != 0) {
                android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, icon);
                if (d != null) {
                    int s = dp(26); // ✅ emoji size (try 22-28)
                    d.setBounds(0, 0, s, s);
                    rb.setCompoundDrawables(d, null, null, null);
                }
            }
            rb.setCompoundDrawablePadding(dp(10));

            rg.addView(rb);
        }

        // Other input (bug fix: only appears if "other" selected)
        EditText otherInput = new EditText(this);
        otherInput.setHint(getString(R.string.filter_other_hint));
        otherInput.setText(otherEmotionText);
        otherInput.setVisibility("other".equals(selectedEmotionKey) ? View.VISIBLE : View.GONE);

        boolean isRtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        otherInput.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        otherInput.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        otherInput.setGravity(isRtl ? (Gravity.END | Gravity.CENTER_VERTICAL) : (Gravity.START | Gravity.CENTER_VERTICAL));

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked instanceof RadioButton && checked.getTag() instanceof String) {
                selectedEmotionKey = (String) checked.getTag();
                otherInput.setVisibility("other".equals(selectedEmotionKey) ? View.VISIBLE : View.GONE);
            }
        });

        // preselect
        if (!isEmpty(selectedEmotionKey)) {
            for (int i = 0; i < rg.getChildCount(); i++) {
                View v = rg.getChildAt(i);
                if (v instanceof RadioButton) {
                    Object tag = v.getTag();
                    if (tag != null && tag.toString().equals(selectedEmotionKey)) {
                        ((RadioButton) v).setChecked(true);
                        break;
                    }
                }
            }
        }

        root.addView(rg);
        root.addView(otherInput);

        b.setView(scroll);

        b.setPositiveButton(getString(R.string.apply), (d, w) -> {
            if ("other".equals(selectedEmotionKey)) {
                otherEmotionText = safe(otherInput.getText() == null ? "" : otherInput.getText().toString());
            } else {
                otherEmotionText = "";
            }
            applyFilters();
        });

        b.setNegativeButton(getString(R.string.cancel), null);
        b.show();
    }

    // ==========================================================
    // ✅ Situation picker dialog (buttons list + Other textbox)
    // ==========================================================
    private void showSituationPickerDialog() {

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.filter_choose_situation));

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(root);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(LinearLayout.VERTICAL);

        for (String key : SITUATION_KEYS) {
            RadioButton rb = new RadioButton(this);
            rb.setTag(key);
            rb.setText(getSituationLabel(key));
            rg.addView(rb);
        }

        EditText otherInput = new EditText(this);
        otherInput.setHint(getString(R.string.filter_other_hint));
        otherInput.setText(otherSituationText);
        otherInput.setVisibility("other".equals(selectedSituationKey) ? View.VISIBLE : View.GONE);

        boolean isRtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        otherInput.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        otherInput.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        otherInput.setGravity(isRtl ? (Gravity.END | Gravity.CENTER_VERTICAL) : (Gravity.START | Gravity.CENTER_VERTICAL));

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked instanceof RadioButton && checked.getTag() instanceof String) {
                selectedSituationKey = (String) checked.getTag();
                otherInput.setVisibility("other".equals(selectedSituationKey) ? View.VISIBLE : View.GONE);
            }
        });

        // preselect
        if (!isEmpty(selectedSituationKey)) {
            for (int i = 0; i < rg.getChildCount(); i++) {
                View v = rg.getChildAt(i);
                if (v instanceof RadioButton) {
                    Object tag = v.getTag();
                    if (tag != null && tag.toString().equals(selectedSituationKey)) {
                        ((RadioButton) v).setChecked(true);
                        break;
                    }
                }
            }
        }

        root.addView(rg);
        root.addView(otherInput);

        b.setView(scroll);

        b.setPositiveButton(getString(R.string.apply), (d, w) -> {
            if ("other".equals(selectedSituationKey)) {
                otherSituationText = safe(otherInput.getText() == null ? "" : otherInput.getText().toString());
            } else {
                otherSituationText = "";
            }
            applyFilters();
        });

        b.setNegativeButton(getString(R.string.cancel), null);
        b.show();
    }

    // ==========================================================
    // ✅ Location picker dialog (buttons list + Other textbox)
    // ==========================================================
    private void showLocationPickerDialog() {

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(getString(R.string.filter_choose_location));

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(root);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(LinearLayout.VERTICAL);

        for (String key : LOCATION_KEYS) {
            RadioButton rb = new RadioButton(this);
            rb.setTag(key);
            rb.setText(getLocationLabel(key));
            rg.addView(rb);
        }

        EditText otherInput = new EditText(this);
        otherInput.setHint(getString(R.string.filter_other_hint));
        otherInput.setText(otherLocationText);
        otherInput.setVisibility("other".equals(selectedLocationKey) ? View.VISIBLE : View.GONE);

        boolean isRtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        otherInput.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        otherInput.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        otherInput.setGravity(isRtl ? (Gravity.END | Gravity.CENTER_VERTICAL) : (Gravity.START | Gravity.CENTER_VERTICAL));

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked instanceof RadioButton && checked.getTag() instanceof String) {
                selectedLocationKey = (String) checked.getTag();
                otherInput.setVisibility("other".equals(selectedLocationKey) ? View.VISIBLE : View.GONE);
            }
        });

        // preselect
        if (!isEmpty(selectedLocationKey)) {
            for (int i = 0; i < rg.getChildCount(); i++) {
                View v = rg.getChildAt(i);
                if (v instanceof RadioButton) {
                    Object tag = v.getTag();
                    if (tag != null && tag.toString().equals(selectedLocationKey)) {
                        ((RadioButton) v).setChecked(true);
                        break;
                    }
                }
            }
        }

        root.addView(rg);
        root.addView(otherInput);

        b.setView(scroll);

        b.setPositiveButton(getString(R.string.apply), (d, w) -> {
            if ("other".equals(selectedLocationKey)) {
                otherLocationText = safe(otherInput.getText() == null ? "" : otherInput.getText().toString());
            } else {
                otherLocationText = "";
            }
            applyFilters();
        });

        b.setNegativeButton(getString(R.string.cancel), null);
        b.show();
    }

    // ==========================================================
    // ✅ Apply filters (FIXED emotion bug: EXACT match by normalized key)
    // ==========================================================
    private void applyFilters() {
        logs.clear();

        long now = System.currentTimeMillis();
        long minTs = 0L;

        if (timeMode == TimeMode.TODAY) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            minTs = c.getTimeInMillis();
        } else if (timeMode == TimeMode.LAST_7_DAYS) {
            minTs = now - 7L * 24L * 60L * 60L * 1000L;
        } else if (timeMode == TimeMode.LAST_30_DAYS) {
            minTs = now - 30L * 24L * 60L * 60L * 1000L;
        }

        for (LogItem item : allLogs) {

            // Time
            if (minTs > 0L && item.tsMillis < minTs) continue;

            // Emotion (EXACT)
            if (!isEmpty(selectedEmotionKey)) {
                String itemKey = normalizeEmotionKey(item.emotion);

                if ("other".equals(selectedEmotionKey)) {
                    String typed = lc(otherEmotionText);
                    if (!typed.isEmpty() && !lc(item.emotion).contains(typed)) continue;
                } else {
                    if (isEmpty(itemKey) || !itemKey.equals(selectedEmotionKey)) continue;
                }
            }

            // Situation
            if (!isEmpty(selectedSituationKey)) {
                String sit = lc(item.situation);

                if ("other".equals(selectedSituationKey)) {
                    String typed = lc(otherSituationText);
                    if (!typed.isEmpty() && !sit.contains(typed)) continue;
                } else if ("gift".equals(selectedSituationKey)) {
                    if (!sit.contains("gift")) continue;
                } else if ("fight".equals(selectedSituationKey)) {
                    if (!sit.contains("fight")) continue;
                } else if ("test".equals(selectedSituationKey)) {
                    if (!sit.contains("test")) continue;
                }
            }

            // Location
            if (!isEmpty(selectedLocationKey)) {
                String loc = lc(item.location);

                if ("other".equals(selectedLocationKey)) {
                    String typed = lc(otherLocationText);
                    if (!typed.isEmpty() && !loc.contains(typed)) continue;
                } else if ("school".equals(selectedLocationKey)) {
                    if (!loc.contains("school")) continue;
                } else if ("home".equals(selectedLocationKey)) {
                    if (!loc.contains("home")) continue;
                } else if ("dontknow".equals(selectedLocationKey)) {
                    if (!loc.contains("don't") && !loc.contains("dont") && !loc.contains("know")) continue;
                }
            }

            logs.add(item);
        }

        // Newest first
        Collections.sort(logs, (a, b) -> Long.compare(b.tsMillis, a.tsMillis));
        adapter.notifyDataSetChanged();
    }

    // ===================== BUTTON TEXTS =====================
    private String getEmotionPickerButtonText() {
        if (isEmpty(selectedEmotionKey)) return getString(R.string.filter_emotion_any);

        if ("other".equals(selectedEmotionKey)) {
            String t = safe(otherEmotionText);
            return t.isEmpty() ? getString(R.string.filter_emotion_other) : t;
        }
        return getEmotionLabel(selectedEmotionKey);
    }

    private String getSituationPickerButtonText() {
        if (isEmpty(selectedSituationKey)) return getString(R.string.filter_situation_any);

        if ("other".equals(selectedSituationKey)) {
            return isEmpty(otherSituationText)
                    ? getString(R.string.situation_other)
                    : otherSituationText;
        }
        return getSituationLabel(selectedSituationKey);
    }

    private String getLocationPickerButtonText() {
        if (isEmpty(selectedLocationKey)) return getString(R.string.filter_location_any);

        if ("other".equals(selectedLocationKey)) {
            return isEmpty(otherLocationText)
                    ? getString(R.string.location_other)
                    : otherLocationText;
        }
        return getLocationLabel(selectedLocationKey);
    }

    // ===================== LABELS =====================
    private String getEmotionLabel(String key) {
        if ("happy".equals(key)) return getString(R.string.emotion_happy);
        if ("sad".equals(key)) return getString(R.string.emotion_sad);
        if ("angry".equals(key)) return getString(R.string.emotion_angry);
        if ("afraid".equals(key)) return getString(R.string.emotion_afraid);
        if ("disgusted".equals(key)) return getString(R.string.emotion_disgusted);
        if ("surprised".equals(key)) return getString(R.string.emotion_surprised);
        if ("unsure".equals(key)) return getString(R.string.emotion_unsure);
        return getString(R.string.emotion_other);
    }

    private String getSituationLabel(String key) {
        if ("gift".equals(key)) return getString(R.string.situation_gift);
        if ("fight".equals(key)) return getString(R.string.situation_fight);
        if ("test".equals(key)) return getString(R.string.situation_test);
        return getString(R.string.situation_other); // "Other / Something else"
    }

    private String getLocationLabel(String key) {
        if ("school".equals(key)) return getString(R.string.location_school);
        if ("home".equals(key)) return getString(R.string.location_home);
        if ("dontknow".equals(key)) return getString(R.string.location_dontknow);
        return getString(R.string.location_other);
    }
    // ===================== ICONS =====================
    private int getEmotionIconRes(String key) {
        if ("afraid".equals(key)) return R.drawable.emoji_afraid;
        if ("angry".equals(key)) return R.drawable.emoji_angry;
        if ("disgusted".equals(key)) return R.drawable.emoji_disgusted;
        if ("happy".equals(key)) return R.drawable.emoji_happy;
        if ("sad".equals(key)) return R.drawable.emoji_sad;
        if ("surprised".equals(key)) return R.drawable.emoji_surprised;
        if ("unsure".equals(key)) return R.drawable.emoji_unsure;
        return 0; // other/unknown
    }

    // Normalize Firestore emotion text to one key
    private String normalizeEmotionKey(String raw) {
        String e = lc(raw);
        // IMPORTANT: keep these EXACT and simple
        if (e.equals("happy") || e.contains("happy")) return "happy";
        if (e.equals("sad") || e.contains("sad")) return "sad";
        if (e.equals("angry") || e.contains("angry")) return "angry";
        if (e.equals("afraid") || e.contains("afraid") || e.contains("fear")) return "afraid";
        if (e.equals("disgusted") || e.contains("disgust")) return "disgusted";
        if (e.equals("surprised") || e.contains("surpris")) return "surprised";
        if (e.equals("unsure") || e.contains("unsure") || e.contains("confus")) return "unsure";
        return "";
    }

    // ==========================================================
    // Model
    // ==========================================================
    private static class LogItem {
        final String timestamp;
        final String situation;
        final String location;
        final String emotion;
        final int intensity;
        final String note;
        final long tsMillis;

        LogItem(String timestamp, String situation, String location, String emotion,
                int intensity, String note, long tsMillis) {
            this.timestamp = timestamp;
            this.situation = situation;
            this.location = location;
            this.emotion = emotion;
            this.intensity = intensity;
            this.note = note;
            this.tsMillis = tsMillis;
        }
    }

    // ==========================================================
    // Adapter (shows BOTH: emoji + emotion text inside the report card)
    // ==========================================================
    private class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.VH> {

        private final List<LogItem> list;

        LogsAdapter(List<LogItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            int pad18 = dp(18);
            int rad24 = dp(24);
            int mb14 = dp(14);

            CardView card = new CardView(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = mb14;
            card.setLayoutParams(lp);
            card.setRadius(rad24);
            card.setCardElevation(0f);
            card.setCardBackgroundColor(0xFFFBF6E9);

            LinearLayout root = new LinearLayout(parent.getContext());
            root.setLayoutParams(new CardView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(pad18, pad18, pad18, pad18);

            // Row 1: bullet + situation
            LinearLayout titleRow = new LinearLayout(parent.getContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView bullet = new TextView(parent.getContext());
            bullet.setText("• ");
            bullet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            bullet.setTextColor(0xFF000000);
            bullet.setPadding(0, 0, dp(6), 0);

            TextView tvSituation = new TextView(parent.getContext());
            tvSituation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tvSituation.setTextColor(0xFF000000);
            tvSituation.setTypeface(tvSituation.getTypeface(), android.graphics.Typeface.BOLD);
            tvSituation.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ));

            titleRow.addView(bullet);
            titleRow.addView(tvSituation);

            // Row 2: timestamp
            TextView tvTimestamp = new TextView(parent.getContext());
            LinearLayout.LayoutParams tsLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tsLp.topMargin = dp(6);
            tvTimestamp.setLayoutParams(tsLp);
            tvTimestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTimestamp.setTextColor(0xFF000000);

            // Row 3: location + emotion(icon+text) + intensity
            LinearLayout infoRow = new LinearLayout(parent.getContext());
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            infoLp.topMargin = dp(8);
            infoRow.setLayoutParams(infoLp);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            infoRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvLocation = smallLabel(parent);
            TextView tvIntensity = smallLabel(parent);

            LinearLayout emoWrap = new LinearLayout(parent.getContext());
            emoWrap.setOrientation(LinearLayout.HORIZONTAL);
            emoWrap.setGravity(Gravity.CENTER_VERTICAL);

            ImageView emoIcon = new ImageView(parent.getContext());
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
            iconLp.rightMargin = dp(6);
            emoIcon.setLayoutParams(iconLp);

            TextView tvEmotion = smallLabel(parent);

            emoWrap.addView(emoIcon);
            emoWrap.addView(tvEmotion);

            Space sp1 = new Space(parent.getContext());
            sp1.setLayoutParams(new LinearLayout.LayoutParams(dp(12), 1));
            Space sp2 = new Space(parent.getContext());
            sp2.setLayoutParams(new LinearLayout.LayoutParams(dp(12), 1));

            infoRow.addView(tvLocation);
            infoRow.addView(sp1);
            infoRow.addView(emoWrap);
            infoRow.addView(sp2);
            infoRow.addView(tvIntensity);

            // Row 4: note
            TextView tvNote = new TextView(parent.getContext());
            LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            noteLp.topMargin = dp(10);
            tvNote.setLayoutParams(noteLp);
            tvNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvNote.setGravity(Gravity.CENTER);
            tvNote.setTextColor(0xFF000000);

            root.addView(titleRow);
            root.addView(tvTimestamp);
            root.addView(infoRow);
            root.addView(tvNote);

            card.addView(root);

            return new VH(card, tvSituation, tvTimestamp, tvLocation, tvEmotion, emoIcon, tvIntensity, tvNote);
        }

        private TextView smallLabel(ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTextColor(0xFF000000);
            return tv;
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            LogItem item = list.get(position);

            h.tvSituation.setText(safe(item.situation));
            h.tvTimestamp.setText(getString(R.string.label_date) + " " + safe(item.timestamp));
            h.tvLocation.setText(getString(R.string.label_location) + " " + safe(item.location));

            // ✅ emoji + TEXT (this is what you asked)
            String key = normalizeEmotionKey(item.emotion);
            String label = isEmpty(key) ? safe(item.emotion) : getEmotionLabel(key);

            h.tvEmotion.setText(getString(R.string.label_emotion) + " " + label);

            int icon = getEmotionIconRes(key);
            if (icon != 0) {
                h.emoIcon.setVisibility(View.VISIBLE);
                h.emoIcon.setImageResource(icon);
            } else {
                h.emoIcon.setVisibility(View.GONE);
            }

            h.tvIntensity.setText(getString(R.string.label_intensity) + " " + item.intensity);

            String note = safe(item.note);
            h.tvNote.setText(note.isEmpty() ? getString(R.string.note_empty) : ("“" + note + "”"));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvSituation, tvTimestamp, tvLocation, tvEmotion, tvIntensity, tvNote;
            final ImageView emoIcon;

            VH(@NonNull View itemView,
               TextView tvSituation,
               TextView tvTimestamp,
               TextView tvLocation,
               TextView tvEmotion,
               ImageView emoIcon,
               TextView tvIntensity,
               TextView tvNote) {
                super(itemView);
                this.tvSituation = tvSituation;
                this.tvTimestamp = tvTimestamp;
                this.tvLocation = tvLocation;
                this.tvEmotion = tvEmotion;
                this.emoIcon = emoIcon;
                this.tvIntensity = tvIntensity;
                this.tvNote = tvNote;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyReg != null) historyReg.remove();
    }

    // ===================== utils =====================
    private String safe(String s) { return (s == null) ? "" : s.trim(); }
    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (!isEmpty(v)) return v.trim();
        return null;
    }

    private String lc(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.getDefault());
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }
}