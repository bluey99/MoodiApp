package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.model.Task;
import com.example.asdproject.view.adapters.TaskAdapters;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChildTasksActivity extends AppCompatActivity {

    private RecyclerView recyclerTasks;
    private TaskAdapters taskAdapter;
    private final List<Task> taskList = new ArrayList<>();
    private CollectionReference tasksRef;
    private String childId;
    private TextView txtTasksWaiting;
    private int lastTaskCount = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_tasks);

        View header = findViewById(R.id.header);
        TextView headerTitle = header.findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("My Tasks");
        txtTasksWaiting = findViewById(R.id.txtTaskCount);


        // Recycler
        recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapters(taskList);
        recyclerTasks.setAdapter(taskAdapter);

        // Back button (ImageView, NOT ImageButton)
        ImageView btnBack = header.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Child id
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

        Log.d("TASK_DEBUG", "Loading tasks for childId = " + childId);

        taskList.clear();
        taskAdapter.notifyDataSetChanged();

        // 1) Try new field name: "childID"
        tasksRef.whereEqualTo("childID", childId)
                .whereEqualTo("status", "ASSIGNED")
                .get()
                .addOnSuccessListener(snap1 -> {

                    for (QueryDocumentSnapshot doc : snap1) {
                        Log.d("TASK_DEBUG", "MATCH childID: " + doc.getId() + " -> " + doc.getData());
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        taskList.add(task);
                    }

                    // 2) Also try old field name: "childId"
                    tasksRef.whereEqualTo("childId", childId)
                            .whereEqualTo("status", "ASSIGNED")
                            .get()
                            .addOnSuccessListener(snap2 -> {

                                for (QueryDocumentSnapshot doc : snap2) {
                                    Log.d("TASK_DEBUG", "MATCH childId: " + doc.getId() + " -> " + doc.getData());

                                    // avoid duplicates if same task appears
                                    boolean exists = false;
                                    for (Task t : taskList) {
                                        if (t.getId() != null && t.getId().equals(doc.getId())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        Task task = doc.toObject(Task.class);
                                        task.setId(doc.getId());
                                        taskList.add(task);
                                    }
                                }

                                taskAdapter.notifyDataSetChanged();

                                int newCount = taskList.size();
                                txtTasksWaiting.setText("Tasks waiting: " + newCount);
                                if (newCount > lastTaskCount) animateTaskPill(txtTasksWaiting);
                                lastTaskCount = newCount;

                                Log.d("TASK_DEBUG", "FINAL COUNT = " + newCount);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed (childId): " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed (childID): " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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
