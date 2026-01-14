package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.asdproject.view.fragments.ChildTaskFilterBottomSheetFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.model.Task;
import com.example.asdproject.view.adapters.TaskAdapters;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChildTasksActivity extends AppCompatActivity {

    private RecyclerView recyclerTasks;
    private TaskAdapters taskAdapter;
    private final List<Task> taskList = new ArrayList<>();
    private CollectionReference tasksRef;
    private String childId;
    private TextView txtTasksWaiting;
    private int lastTaskCount = 0;
    // FILTER STATE
    private String selectedCreatorType = null;
// null = ALL, "PARENT", "THERAPIST"

    // Keep a master list (important)
    private final List<Task> allTasks = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_tasks);

        View header = findViewById(R.id.header);
        TextView headerTitle = header.findViewById(R.id.txtHeaderTitle);
        ImageView btnFilter = header.findViewById(R.id.btnFilter);

        headerTitle.setText("My Tasks");

        txtTasksWaiting = findViewById(R.id.txtTaskCount);

        recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapters(taskList);
        recyclerTasks.setAdapter(taskAdapter);

        ImageView btnBack = header.findViewById(R.id.btnBack);
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

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child id is missing – cannot load tasks", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("tasks");

        loadTasksForChild(childId);
    }

    // ==============================
    // TASK LOADING WITH TIME FILTER
    // ==============================
    private void loadTasksForChild(String childId) {

        Log.d("TASK_DEBUG", "Loading tasks for childId = " + childId);
        Log.d("TASK_TIME", "Now = " + LocalDateTime.now());

        taskList.clear();
        allTasks.clear();
        taskAdapter.notifyDataSetChanged();

        // -------- QUERY 1: childID --------
        tasksRef.whereEqualTo("childID", childId)
                .whereEqualTo("status", "ASSIGNED")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (QueryDocumentSnapshot doc : snapshot) {
                        handleTaskDocument(doc);
                    }

                    // -------- QUERY 2: childId --------
                    tasksRef.whereEqualTo("childId", childId)
                            .whereEqualTo("status", "ASSIGNED")
                            .get()
                            .addOnSuccessListener(snapshot2 -> {

                                for (QueryDocumentSnapshot doc : snapshot2) {
                                    if (!containsTask(doc.getId())) {
                                        handleTaskDocument(doc);
                                    }
                                }

                                finalizeTaskList();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed (childId): " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed (childID): " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ==============================
    // HANDLE SINGLE TASK DOCUMENT
    // ==============================
    private void handleTaskDocument(QueryDocumentSnapshot doc) {
        Task task = doc.toObject(Task.class);
        task.setId(doc.getId());

        if (isTaskReadyToDisplay(task.getDisplayWhen())) {
            allTasks.add(task);
            Log.d("TASK_TIME", "VISIBLE: " + task.getTaskName());
        } else {
            Log.d("TASK_TIME", "HIDDEN (future): " + task.getDisplayWhen());
        }
    }

    // ==============================
    // TIME CHECK LOGIC
    // ==============================
    private boolean isTaskReadyToDisplay(String displayWhen) {
        try {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("d/M/yyyy, h:mma", Locale.ENGLISH);

            LocalDateTime taskTime =
                    LocalDateTime.parse(displayWhen.toUpperCase(), formatter);

            return !taskTime.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            Log.e("TASK_TIME", "Invalid displayWhen format: " + displayWhen, e);
            return false;
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
        applyFilter();
        updateTaskCountPill();

        int newCount = taskList.size();

        if (newCount > lastTaskCount) {
            animateTaskPill(txtTasksWaiting);
        }

        lastTaskCount = newCount;
        Log.d("TASK_DEBUG", "FINAL COUNT = " + newCount);
    }


    private void applyFilter() {
        taskList.clear();

        for (Task task : allTasks) {
            if (selectedCreatorType == null ||
                    selectedCreatorType.equals(task.getCreatorType())) {
                taskList.add(task);
            }
        }

        taskAdapter.notifyDataSetChanged();
    }
    private void updateTaskCountPill() {
        int count = taskList.size();

        if (selectedCreatorType == null) {
            txtTasksWaiting.setText("Tasks waiting: " + count);
        } else if ("PARENT".equals(selectedCreatorType)) {
            txtTasksWaiting.setText("Mom tasks: " + count);
        } else if ("THERAPIST".equals(selectedCreatorType)) {
            txtTasksWaiting.setText("Therapist tasks: " + count);
        }
        if (count == 0) {
            txtTasksWaiting.setVisibility(View.GONE);
        } else {
            txtTasksWaiting.setVisibility(View.VISIBLE);
        }

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
