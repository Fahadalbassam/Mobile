package com.group5.studentplanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Shows all tasks. Tap a task to edit it, tick the box to mark it as completed.
 */
public class TaskListActivity extends AppCompatActivity implements TaskAdapter.OnTaskCheckedListener {

    private ListView listTasks;
    private TextView textEmpty;
    private CheckBox checkShowCompleted;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);
        InsetsHelper.apply(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);
        listTasks = findViewById(R.id.listTasks);
        textEmpty = findViewById(R.id.textEmpty);
        checkShowCompleted = findViewById(R.id.checkShowCompleted);

        checkShowCompleted.setOnCheckedChangeListener((button, checked) -> loadTasks());

        findViewById(R.id.buttonAddTask).setOnClickListener(
                v -> startActivity(new Intent(this, TaskEditActivity.class)));

        listTasks.setOnItemClickListener((parent, view, position, id) -> {
            Task task = (Task) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, TaskEditActivity.class);
            intent.putExtra(TaskEditActivity.EXTRA_TASK_ID, task.id); // pass the id to the next screen
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        List<Task> tasks = db.getAllTasks(checkShowCompleted.isChecked());
        listTasks.setAdapter(new TaskAdapter(this, tasks, this));
        boolean empty = tasks.isEmpty();
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onTaskChecked(Task task, boolean completed) {
        db.setTaskCompleted(task.id, completed);
        task.completed = completed;
        // A completed task does not need a reminder any more; re-schedule if un-ticked.
        ReminderScheduler.scheduleTask(this, task);
        loadTasks();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
