package com.group5.studentplanner;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Turns each Task into one row (item_task.xml) of the task list.
 */
public class TaskAdapter extends ArrayAdapter<Task> {

    public interface OnTaskCheckedListener {
        void onTaskChecked(Task task, boolean completed);
    }

    private final OnTaskCheckedListener listener;

    public TaskAdapter(Context context, List<Task> tasks, OnTaskCheckedListener listener) {
        super(context, 0, tasks);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext()).inflate(R.layout.item_task, parent, false);
        }
        Task task = getItem(position);
        CheckBox checkDone = row.findViewById(R.id.checkDone);
        TextView textTitle = row.findViewById(R.id.textTitle);
        TextView textDue = row.findViewById(R.id.textDue);
        TextView textPriority = row.findViewById(R.id.textPriority);

        textTitle.setText(task.title);
        String due = "Due: " + FormatUtils.formatDateTime(task.dueDate);
        if (!task.completed && task.dueDate < System.currentTimeMillis()) {
            due += " (overdue)";
        }
        textDue.setText(due);

        // Strike through finished tasks
        if (task.completed) {
            textTitle.setPaintFlags(textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textTitle.setPaintFlags(textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        textPriority.setText(FormatUtils.priorityName(getContext(), task.priority));
        textPriority.setTextColor(ContextCompat.getColor(getContext(),
                FormatUtils.priorityColor(task.priority)));

        // Remove the old listener before setting the state, so recycling rows does not trigger it.
        checkDone.setOnCheckedChangeListener(null);
        checkDone.setChecked(task.completed);
        checkDone.setOnCheckedChangeListener((button, checked) -> listener.onTaskChecked(task, checked));

        return row;
    }
}
