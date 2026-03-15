package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.asdproject.R;
import com.example.asdproject.model.Task;
import com.example.asdproject.view.adapters.TaskAdapters;
import com.example.asdproject.view.fragments.ChildTaskFilterBottomSheetFragment;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChildTasksActivity extends AppCompatActivity {

    private RecyclerView recyclerTasks;
    private TaskAdapters taskAdapter;

    private final List<Task> taskList = new ArrayList<>();     // shown in UI (after filter)
    private final List<Task> allTasks  = new ArrayList<>();    // master list (before filter)

    private CollectionReference tasksRef;
    private String childId;

    private TextView txtTasksWaiting;

    // FILTER STATE
    private String selectedCreatorType = null; // null = ALL, "PARENT", "THERAPIST"

    // bayan added here - two listeners to support both field names in Firestore
    private ListenerRegistration tasksRegChildId;
    private ListenerRegistration tasksRegChildID;

    // bayan added here - hold latest results from both listeners
    private final List<Task> liveTasksChildId = new ArrayList<>();
    private final List<Task> liveTasksChildID = new ArrayList<>();

    private static final String TASK_DBG = "TASK_DBG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_tasks);

        View header = findViewById(R.id.header);
        TextView headerTitle = header.findViewById(R.id.txtHeaderTitle);
        ImageView btnFilter  = header.findViewById(R.id.btnFilter);
        ImageView btnBack    = header.findViewById(R.id.btnBack);

        headerTitle.setText(getString(R.string.child_tasks_header_title));

        txtTasksWaiting = findViewById(R.id.txtTaskCount);

        recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapters(taskList);
        recyclerTasks.setAdapter(taskAdapter);

        btnBack.setOnClickListener(v -> finish());

        btnFilter.setOnClickListener(v -> {
            ChildTaskFilterBottomSheetFragment sheet =
                    new ChildTaskFilterBottomSheetFragment(type -> {
                        selectedCreatorType = type;
                        applyFilter();
                        updateTaskCountPill();
                        animateTaskPill(txtTasksWaiting);
                    });

            sheet.show(getSupportFragmentManager(), "TASK_FILTER");
        });

        childId = getIntent().getStringExtra("childId");

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.child_tasks_missing_child_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("tasks");
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLiveTasksListener(childId); // bayan added here - start listener in onStart
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLiveTasksListener(); // bayan added here - stop listener in onStop
    }

    // ==============================
    // LIVE TASK LISTENER (updates UI instantly) - supports childId + childID
    // IMPORTANT CHANGE:
    // We DO NOT query by status in Firestore to avoid:
    // 1) case mismatch (ASSIGNED vs assigned)
    // 2) composite index issues
    // Instead, we filter status locally with equalsIgnoreCase.
    // ==============================
    private void startLiveTasksListener(String childId) {

        stopLiveTasksListener(); // bayan added here - remove old listeners

        Log.d(TASK_DBG, "==== startLiveTasksListener START ====");
        Log.d(TASK_DBG, "childId = " + childId);

        // ✅ Listener #1: field "childId"
        tasksRegChildId = tasksRef
                .whereEqualTo("childId", childId)
                .addSnapshotListener((snapshot, e) -> {

                    if (e != null) {
                        Log.e(TASK_DBG, "[LIVE childId ERROR] " + e.getMessage(), e);
                        return;
                    }
                    if (snapshot == null) return;

                    Log.d(TASK_DBG, "[LIVE childId] docs=" + snapshot.size());

                    liveTasksChildId.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String status = doc.getString("status");
                        if (status == null || !status.equalsIgnoreCase("ASSIGNED")) continue;

                        Task task = doc.toObject(Task.class);
                        if (task == null) continue;

                        task.setId(doc.getId());
                        liveTasksChildId.add(task);
                    }

                    mergeLiveTasksAndRender();
                });

        // ✅ Listener #2: field "childID" (fallback)
        tasksRegChildID = tasksRef
                .whereEqualTo("childID", childId)
                .addSnapshotListener((snapshot, e) -> {

                    if (e != null) {
                        Log.e(TASK_DBG, "[LIVE childID ERROR] " + e.getMessage(), e);
                        return;
                    }
                    if (snapshot == null) return;

                    Log.d(TASK_DBG, "[LIVE childID] docs=" + snapshot.size());

                    liveTasksChildID.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String status = doc.getString("status");
                        if (status == null || !status.equalsIgnoreCase("ASSIGNED")) continue;

                        Task task = doc.toObject(Task.class);
                        if (task == null) continue;

                        task.setId(doc.getId());
                        liveTasksChildID.add(task);
                    }

                    mergeLiveTasksAndRender();
                });
    }

    private void stopLiveTasksListener() {
        // bayan added here - prevent memory leaks + duplicated listeners
        if (tasksRegChildId != null) {
            tasksRegChildId.remove();
            tasksRegChildId = null;
        }
        if (tasksRegChildID != null) {
            tasksRegChildID.remove();
            tasksRegChildID = null;
        }
    }

    // bayan added here - merges both live lists + applies your time filter + updates UI
    private void mergeLiveTasksAndRender() {

        allTasks.clear();

        // merge list #1
        for (Task t : liveTasksChildId) {
            if (t == null || t.getId() == null) continue;
            if (!containsTask(t.getId())) {
                if (isTaskReadyToDisplay(t.getDisplayWhen())) allTasks.add(t);
            }
        }

        // merge list #2
        for (Task t : liveTasksChildID) {
            if (t == null || t.getId() == null) continue;
            if (!containsTask(t.getId())) {
                if (isTaskReadyToDisplay(t.getDisplayWhen())) allTasks.add(t);
            }
        }

        finalizeTaskList();
    }

    // ==============================
    // TIME CHECK LOGIC
    // IMPORTANT CHANGE:
    // If parsing fails OR displayWhen is missing, we SHOW the task (demo-safe).
    // ==============================
    private boolean isTaskReadyToDisplay(String displayWhen) {
        try {
            if (displayWhen == null || displayWhen.trim().isEmpty()) {
                return true; // bayan added here - missing time? show it
            }

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("d/M/yyyy, h:mma", Locale.ENGLISH);

            LocalDateTime taskTime =
                    LocalDateTime.parse(displayWhen.toUpperCase(), formatter);

            return !taskTime.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            Log.e("TASK_TIME", "Invalid displayWhen format: " + displayWhen, e);
            return true; // bayan added here - don't hide tasks on parse error
        }
    }

    // ==============================
    // UTILITIES
    // ==============================
    private boolean containsTask(String taskId) {
        for (Task t : allTasks) {
            if (t.getId() != null && t.getId().equals(taskId)) {
                return true;
            }
        }
        return false;
    }

    private void finalizeTaskList() {

        Log.d(TASK_DBG, "==== finalizeTaskList ====");
        Log.d(TASK_DBG, "allTasks size = " + allTasks.size());
        Log.d(TASK_DBG, "selectedCreatorType = " + selectedCreatorType);

        applyFilter();

        Log.d(TASK_DBG, "taskList after filter = " + taskList.size());

        updateTaskCountPill();

        if (taskList.isEmpty()) {
            Log.w(TASK_DBG, "⚠ NO TASKS DISPLAYED IN UI");
        }
    }

    private void applyFilter() {
        taskList.clear();

        for (Task task : allTasks) {
            if (selectedCreatorType == null ||
                    (task.getCreatorType() != null && selectedCreatorType.equals(task.getCreatorType()))) {
                taskList.add(task);
            }
        }

        taskAdapter.notifyDataSetChanged();
    }

    private void updateTaskCountPill() {
        int count = taskList.size();

        if (selectedCreatorType == null) {
            txtTasksWaiting.setText(getString(R.string.child_tasks_count_all, count));
        } else if ("PARENT".equals(selectedCreatorType)) {
            txtTasksWaiting.setText(getString(R.string.child_tasks_count_parent, count));
        } else if ("THERAPIST".equals(selectedCreatorType)) {
            txtTasksWaiting.setText(getString(R.string.child_tasks_count_therapist, count));
        }

        txtTasksWaiting.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    private void animateTaskPill(View pill) {
        pill.animate()
                .rotation(5f)
                .setDuration(150)
                .withEndAction(() ->
                        pill.animate()
                                .rotation(0f)
                                .setDuration(150)
                                .start()
                ).start();
    }
}