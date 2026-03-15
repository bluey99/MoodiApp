package com.example.asdproject.view.activities;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EmotionHistoryActivity extends BaseActivity {

    private String childIdOrField;
    private String childDocId;
    private String childName;

    private RecyclerView recycler;
    private HistoryAdapter adapter;

    private final List<Item> shown = new ArrayList<>();
    private final List<Item> all = new ArrayList<>();

    private ListenerRegistration historyReg;

    private enum TimeMode { ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS }
    private TimeMode timeMode = TimeMode.ALL;

    private enum SourceMode { ALL, MOM, THERAPIST }
    private SourceMode sourceMode = SourceMode.ALL;

    private String selectedEmotionKey = "";
    private String otherEmotionText = "";

    private SimpleDateFormat sdf;

    private static final String[] EMOTION_KEYS = new String[] {
            "happy", "sad", "angry", "afraid", "disgusted", "surprised", "unsure", "other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleManager.setLocale(this);
        Locale current = getResources().getConfiguration().getLocales().get(0);
        sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", current);

        setContentView(R.layout.activity_emotion_history);

        childIdOrField = firstNonEmpty(
                getIntent().getStringExtra("CHILD_ID"),
                getIntent().getStringExtra("childId"),
                getIntent().getStringExtra("focusId")
        );

        childName = firstNonEmpty(
                getIntent().getStringExtra("CHILD_NAME"),
                getIntent().getStringExtra("childName")
        );

        setTitle(!isEmpty(childName)
                ? getString(R.string.tasks_history_title_with_name, childName)
                : getString(R.string.tasks_history_title));

        recycler = findViewById(R.id.recyclerEmotionHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(shown);
        recycler.setAdapter(adapter);

        Button btnBack = findViewById(R.id.btnGoBackEmotion);
        Button btnFilter = findViewById(R.id.btnFilterBy);
        TextView btnLanguage = findViewById(R.id.btnLanguage);

        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showPrettyFilterBottomSheet());

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
        resolveChildDocIdThenListen();
    }

    private void resolveChildDocIdThenListen() {
        FirebaseFirestore db = FirebaseManager.getDb();

        db.collection("children")
                .document(childIdOrField)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        childDocId = doc.getId();
                        listenToTaskHistory(db);
                    } else {
                        findChildDocIdByField(db);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_prefix) + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void findChildDocIdByField(FirebaseFirestore db) {
        db.collection("children")
                .whereEqualTo("childID", childIdOrField)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs != null && !qs.isEmpty()) {
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        childDocId = doc.getId();
                        listenToTaskHistory(db);
                    } else {
                        db.collection("children")
                                .whereEqualTo("childId", childIdOrField)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    if (qs2 != null && !qs2.isEmpty()) {
                                        DocumentSnapshot doc2 = qs2.getDocuments().get(0);
                                        childDocId = doc2.getId();
                                        listenToTaskHistory(db);
                                    } else {
                                        Toast.makeText(this, getString(R.string.child_not_found), Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, getString(R.string.error_prefix) + " " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_prefix) + " " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void listenToTaskHistory(FirebaseFirestore db) {
        if (historyReg != null) historyReg.remove();

        historyReg = db.collection("children")
                .document(childDocId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this,
                                getString(R.string.error_prefix) + " " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    all.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        if (!isTaskLog(doc)) continue;

                        String emotion = firstNonEmpty(doc.getString("feeling"), doc.getString("emotion"));
                        Long intensityLong = doc.getLong("intensity");
                        int intensity = (intensityLong == null) ? 0 : intensityLong.intValue();

                        Timestamp ts = doc.getTimestamp("timestamp");
                        long tsMillis = (ts == null) ? 0L : ts.toDate().getTime();
                        String tsText = (ts == null) ? "" : sdf.format(ts.toDate());

                        String taskName = safe(doc.getString("taskName"));
                        String taskPrompt = safe(doc.getString("taskPrompt"));
                        String discussionPrompts = safe(doc.getString("discussionPrompts"));
                        String note = safe(doc.getString("note"));

                        SourceMode src = !isEmpty(taskPrompt) ? SourceMode.MOM : SourceMode.THERAPIST;

                        String displayPrompts = firstNonEmpty(discussionPrompts, taskPrompt, note);

                        String displayName = !taskName.isEmpty()
                                ? taskName
                                : (!displayPrompts.isEmpty() ? displayPrompts : getString(R.string.task_default));

                        all.add(new Item(tsText, displayName, emotion, intensity, displayPrompts, tsMillis, src));
                    }

                    applyFilters();
                });
    }

    private boolean isTaskLog(DocumentSnapshot doc) {
        if (doc == null) return false;
        String logType = safe(doc.getString("logType")).toUpperCase(Locale.getDefault());
        return logType.equals("TASK");
    }

    private void showPrettyFilterBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = new TextView(this);
        title.setText(getString(R.string.filter_title));
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        root.addView(sectionLabel(getString(R.string.filter_time_section)));
        RadioGroup rgTime = new RadioGroup(this);
        rgTime.setOrientation(LinearLayout.VERTICAL);

        RadioButton rbAll = timeRadio(getString(R.string.filter_time_all), TimeMode.ALL);
        RadioButton rbToday = timeRadio(getString(R.string.filter_time_today), TimeMode.TODAY);
        RadioButton rb7 = timeRadio(getString(R.string.filter_time_7), TimeMode.LAST_7_DAYS);
        RadioButton rb30 = timeRadio(getString(R.string.filter_time_30), TimeMode.LAST_30_DAYS);

        rgTime.addView(rbAll);
        rgTime.addView(rbToday);
        rgTime.addView(rb7);
        rgTime.addView(rb30);

        if (timeMode == TimeMode.ALL) rbAll.setChecked(true);
        else if (timeMode == TimeMode.TODAY) rbToday.setChecked(true);
        else if (timeMode == TimeMode.LAST_7_DAYS) rb7.setChecked(true);
        else if (timeMode == TimeMode.LAST_30_DAYS) rb30.setChecked(true);

        root.addView(rgTime);

        root.addView(sectionLabel(getString(R.string.filter_source_section)));
        RadioGroup rgSrc = new RadioGroup(this);
        rgSrc.setOrientation(LinearLayout.VERTICAL);

        RadioButton sAll = sourceRadio(getString(R.string.filter_source_all), SourceMode.ALL);
        RadioButton sMom = sourceRadio(getString(R.string.filter_source_mom), SourceMode.MOM);
        RadioButton sTher = sourceRadio(getString(R.string.filter_source_therapist), SourceMode.THERAPIST);

        rgSrc.addView(sAll);
        rgSrc.addView(sMom);
        rgSrc.addView(sTher);

        if (sourceMode == SourceMode.ALL) sAll.setChecked(true);
        else if (sourceMode == SourceMode.MOM) sMom.setChecked(true);
        else if (sourceMode == SourceMode.THERAPIST) sTher.setChecked(true);

        root.addView(rgSrc);

        root.addView(sectionLabel(getString(R.string.filter_emotion_section)));
        Button btnPickEmotion = nicePickerButton(getEmotionPickerButtonText());
        btnPickEmotion.setOnClickListener(v -> {
            sheet.dismiss();
            showEmotionPickerDialog();
        });
        root.addView(btnPickEmotion);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, dp(14), 0, 0);

        Button btnClear = new Button(this);
        btnClear.setAllCaps(false);
        btnClear.setText(getString(R.string.filter_clear));
        btnClear.setOnClickListener(v -> {
            timeMode = TimeMode.ALL;
            sourceMode = SourceMode.ALL;
            selectedEmotionKey = "";
            otherEmotionText = "";
            applyFilters();
            sheet.dismiss();
        });

        Button btnApply = new Button(this);
        btnApply.setAllCaps(false);
        btnApply.setText(getString(R.string.apply));
        btnApply.setOnClickListener(v -> {
            View tChecked = rgTime.findViewById(rgTime.getCheckedRadioButtonId());
            if (tChecked != null && tChecked.getTag() instanceof TimeMode) {
                timeMode = (TimeMode) tChecked.getTag();
            }

            View sChecked = rgSrc.findViewById(rgSrc.getCheckedRadioButtonId());
            if (sChecked != null && sChecked.getTag() instanceof SourceMode) {
                sourceMode = (SourceMode) sChecked.getTag();
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

    private RadioButton sourceRadio(String text, SourceMode tag) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        rb.setTag(tag);
        rb.setTextSize(14);
        return rb;
    }

    private RadioButton timeRadio(String text, TimeMode tag) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        rb.setTag(tag);
        rb.setTextSize(14);
        return rb;
    }

    private TextView sectionLabel(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextSize(14);
        tv.setPadding(0, dp(12), 0, dp(6));
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
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

        for (String key : EMOTION_KEYS) {
            RadioButton rb = new RadioButton(this);
            rb.setTag(key);
            rb.setText(getEmotionLabel(key));
            rb.setCompoundDrawablePadding(dp(10));

            int icon = getEmotionIconRes(key);
            if (icon != 0) {
                android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, icon);
                if (d != null) {
                    int s = dp(26);
                    d.setBounds(0, 0, s, s);
                    rb.setCompoundDrawables(d, null, null, null);
                }
            }
            rg.addView(rb);
        }

        EditText otherInput = new EditText(this);
        otherInput.setHint(getString(R.string.filter_other_hint));
        otherInput.setText(otherEmotionText);
        otherInput.setVisibility("other".equals(selectedEmotionKey) ? View.VISIBLE : View.GONE);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked instanceof RadioButton && checked.getTag() instanceof String) {
                selectedEmotionKey = (String) checked.getTag();
                otherInput.setVisibility("other".equals(selectedEmotionKey) ? View.VISIBLE : View.GONE);
            }
        });

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

    private String getEmotionPickerButtonText() {
        if (isEmpty(selectedEmotionKey)) return getString(R.string.filter_emotion_any);

        if ("other".equals(selectedEmotionKey)) {
            String t = safe(otherEmotionText);
            return t.isEmpty() ? getString(R.string.filter_emotion_other) : t;
        }
        return getEmotionLabel(selectedEmotionKey);
    }

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

    private int getEmotionIconRes(String key) {
        if ("afraid".equals(key)) return R.drawable.emoji_afraid;
        if ("angry".equals(key)) return R.drawable.emoji_angry;
        if ("disgusted".equals(key)) return R.drawable.emoji_disgusted;
        if ("happy".equals(key)) return R.drawable.emoji_happy;
        if ("sad".equals(key)) return R.drawable.emoji_sad;
        if ("surprised".equals(key)) return R.drawable.emoji_surprised;
        if ("unsure".equals(key)) return R.drawable.emoji_unsure;
        return 0;
    }

    private String normalizeEmotionKey(String raw) {
        String e = lc(raw);
        if (e.equals("happy") || e.contains("happy")) return "happy";
        if (e.equals("sad") || e.contains("sad")) return "sad";
        if (e.equals("angry") || e.contains("angry")) return "angry";
        if (e.equals("afraid") || e.contains("afraid") || e.contains("fear")) return "afraid";
        if (e.equals("disgusted") || e.contains("disgust")) return "disgusted";
        if (e.equals("surprised") || e.contains("surpris")) return "surprised";
        if (e.equals("unsure") || e.contains("unsure") || e.contains("confus")) return "unsure";
        return "";
    }

    private void applyFilters() {
        shown.clear();

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

        for (Item it : all) {

            if (minTs > 0L && it.tsMillis > 0L && it.tsMillis < minTs) continue;

            if (sourceMode != SourceMode.ALL && it.source != sourceMode) continue;

            if (!isEmpty(selectedEmotionKey)) {
                String itemKey = normalizeEmotionKey(it.emotion);

                if ("other".equals(selectedEmotionKey)) {
                    String typed = lc(otherEmotionText);
                    if (!typed.isEmpty() && !lc(it.emotion).contains(typed)) continue;
                } else {
                    if (isEmpty(itemKey) || !itemKey.equals(selectedEmotionKey)) continue;
                }
            }

            shown.add(it);
        }

        Collections.sort(shown, (a, b) -> Long.compare(b.tsMillis, a.tsMillis));
        adapter.notifyDataSetChanged();
    }

    private static class Item {
        final String timestamp;
        final String taskName;
        final String emotion;
        final int intensity;
        final String prompts;
        final long tsMillis;
        final SourceMode source;

        Item(String timestamp, String taskName, String emotion, int intensity,
             String prompts, long tsMillis, SourceMode source) {
            this.timestamp = timestamp;
            this.taskName = taskName;
            this.emotion = emotion;
            this.intensity = intensity;
            this.prompts = prompts;
            this.tsMillis = tsMillis;
            this.source = source;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

        private final List<Item> list;

        HistoryAdapter(List<Item> list) {
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

            LinearLayout titleRow = new LinearLayout(parent.getContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView bullet = new TextView(parent.getContext());
            bullet.setText("• ");
            bullet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            bullet.setTextColor(0xFF000000);
            bullet.setPadding(0, 0, dp(6), 0);

            TextView tvTask = new TextView(parent.getContext());
            tvTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tvTask.setTextColor(0xFF000000);
            tvTask.setTypeface(tvTask.getTypeface(), android.graphics.Typeface.BOLD);
            tvTask.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ));

            titleRow.addView(bullet);
            titleRow.addView(tvTask);

            TextView tvTimestamp = new TextView(parent.getContext());
            LinearLayout.LayoutParams tsLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tsLp.topMargin = dp(6);
            tvTimestamp.setLayoutParams(tsLp);
            tvTimestamp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTimestamp.setTextColor(0xFF000000);

            LinearLayout infoRow = new LinearLayout(parent.getContext());
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            infoLp.topMargin = dp(8);
            infoRow.setLayoutParams(infoLp);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            infoRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView emoIcon = new ImageView(parent.getContext());
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
            iconLp.rightMargin = dp(6);
            emoIcon.setLayoutParams(iconLp);

            TextView tvEmotion = smallLabel(parent);

            Space sp1 = new Space(parent.getContext());
            sp1.setLayoutParams(new LinearLayout.LayoutParams(dp(12), 1));

            TextView tvIntensity = smallLabel(parent);

            Space sp2 = new Space(parent.getContext());
            sp2.setLayoutParams(new LinearLayout.LayoutParams(dp(12), 1));

            TextView tvSource = smallLabel(parent);

            infoRow.addView(emoIcon);
            infoRow.addView(tvEmotion);
            infoRow.addView(sp1);
            infoRow.addView(tvIntensity);
            infoRow.addView(sp2);
            infoRow.addView(tvSource);

            TextView tvPrompts = new TextView(parent.getContext());
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            pLp.topMargin = dp(10);
            tvPrompts.setLayoutParams(pLp);
            tvPrompts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tvPrompts.setGravity(Gravity.CENTER);
            tvPrompts.setTextColor(0xFF000000);

            root.addView(titleRow);
            root.addView(tvTimestamp);
            root.addView(infoRow);
            root.addView(tvPrompts);
            card.addView(root);

            return new VH(card, tvTask, tvTimestamp, emoIcon, tvEmotion, tvIntensity, tvSource, tvPrompts);
        }

        private TextView smallLabel(ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTextColor(0xFF000000);
            return tv;
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Item it = list.get(position);

            h.tvTask.setText(safe(it.taskName));
            h.tvTimestamp.setText(getString(R.string.label_date) + " " + safe(it.timestamp));

            String key = normalizeEmotionKey(it.emotion);
            String label = isEmpty(key) ? safe(it.emotion) : getEmotionLabel(key);
            h.tvEmotion.setText(getString(R.string.label_emotion) + " " + (label.isEmpty() ? "—" : label));

            int icon = getEmotionIconRes(key);
            if (icon != 0) {
                h.emoIcon.setVisibility(View.VISIBLE);
                h.emoIcon.setImageResource(icon);
            } else {
                h.emoIcon.setVisibility(View.GONE);
            }

            h.tvIntensity.setText(getString(R.string.label_intensity) + " " + it.intensity);

            if (it.source == SourceMode.MOM) {
                h.tvSource.setText(getString(R.string.label_source) + " " + getString(R.string.source_mom));
            } else {
                h.tvSource.setText(getString(R.string.label_source) + " " + getString(R.string.source_therapist));
            }

            String text = safe(it.prompts);
            h.tvPrompts.setText(text.isEmpty() ? "“—”" : ("“" + text + "”"));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvTask, tvTimestamp, tvEmotion, tvIntensity, tvSource, tvPrompts;
            final ImageView emoIcon;

            VH(@NonNull View itemView,
               TextView tvTask,
               TextView tvTimestamp,
               ImageView emoIcon,
               TextView tvEmotion,
               TextView tvIntensity,
               TextView tvSource,
               TextView tvPrompts) {
                super(itemView);
                this.tvTask = tvTask;
                this.tvTimestamp = tvTimestamp;
                this.emoIcon = emoIcon;
                this.tvEmotion = tvEmotion;
                this.tvIntensity = tvIntensity;
                this.tvSource = tvSource;
                this.tvPrompts = tvPrompts;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyReg != null) historyReg.remove();
    }

    private String safe(String s) { return (s == null) ? "" : s.trim(); }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return "";
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "";
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