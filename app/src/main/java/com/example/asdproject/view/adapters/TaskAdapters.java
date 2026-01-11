package com.example.asdproject.view.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asdproject.R;
import com.example.asdproject.model.Task;
import com.example.asdproject.view.activities.MomTasksActivity;

import java.util.List;

public class TaskAdapters extends RecyclerView.Adapter<TaskAdapters.TaskViewHolder> {

    private List<Task> taskList;

    public TaskAdapters(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void setTasks(List<Task> tasks) {
        this.taskList = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.txtTaskTitle.setText("New Task from Mom");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), MomTasksActivity.class);

            intent.putExtra("taskId", task.getId());
            intent.putExtra("taskName", task.getTaskName());
            intent.putExtra("displayWhen", task.getDisplayWhen());
            intent.putExtra("discussionPrompts", task.getDiscussionPrompts());

            // ✅ MAIN FIX: pass childId forward
            intent.putExtra("childId", task.getChildId());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return (taskList == null) ? 0 : taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView txtTaskTitle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTaskTitle = itemView.findViewById(R.id.txtTaskTitle);
        }
    }
}
