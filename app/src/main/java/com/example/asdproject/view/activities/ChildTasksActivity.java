package com.example.asdproject.view.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.asdproject.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import com.example.asdproject.view.adapters.TaskAdapters;
import com.example.asdproject.model.Task;

public class ChildTasksActivity extends AppCompatActivity {

    private RecyclerView recyclerTasks;
    private TaskAdapters taskAdapter;
    private final List<Task> taskList = new ArrayList<>();

    private CollectionReference tasksRef;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_tasks);

        recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));

        taskAdapter = new TaskAdapters(taskList);
        recyclerTasks.setAdapter(taskAdapter);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 🔑 get childId from ChildHomeActivity
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

    private void loadTasksForChild(String childId) {
        tasksRef.whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);

                        // ✅ IMPORTANT: store Firestore doc id into Task.id
                        task.setId(doc.getId());

                        taskList.add(task);
                    }
                    taskAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ChildTasksActivity.this,
                                "Failed to load tasks: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
