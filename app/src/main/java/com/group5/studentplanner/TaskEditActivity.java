package com.group5.studentplanner;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

/**
 * Screen for adding a new task or editing / deleting / sharing an existing one.
 */
public class TaskEditActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private EditText editTitle;
    private EditText editDescription;
    private Button buttonDate;
    private Button buttonTime;
    private Spinner spinnerPriority;
    private Spinner spinnerReminder;

    private DatabaseHelper db;
    private Task task;
    private final Calendar due = Calendar.getInstance();
    private int[] reminderMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_edit);
        InsetsHelper.apply(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);
        reminderMinutes = getResources().getIntArray(R.array.reminder_minutes);

        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        buttonDate = findViewById(R.id.buttonDate);
        buttonTime = findViewById(R.id.buttonTime);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerReminder = findViewById(R.id.spinnerReminder);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonDelete = findViewById(R.id.buttonDelete);
        Button buttonShare = findViewById(R.id.buttonShare);

        spinnerPriority.setAdapter(spinnerAdapter(R.array.priorities));
        spinnerReminder.setAdapter(spinnerAdapter(R.array.reminder_options));

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        task = taskId == -1 ? null : db.getTask(taskId);

        if (task == null) {
            // New task: default deadline is tomorrow at 11:59 PM, reminder 1 hour before.
            task = new Task();
            due.add(Calendar.DAY_OF_YEAR, 1);
            due.set(Calendar.HOUR_OF_DAY, 23);
            due.set(Calendar.MINUTE, 59);
            spinnerPriority.setSelection(Task.PRIORITY_MEDIUM);
            spinnerReminder.setSelection(indexOfReminder(60));
            setTitle(R.string.add_task);
        } else {
            editTitle.setText(task.title);
            editDescription.setText(task.description);
            due.setTimeInMillis(task.dueDate);
            spinnerPriority.setSelection(task.priority);
            spinnerReminder.setSelection(indexOfReminder(task.reminderMinutes));
            buttonDelete.setVisibility(View.VISIBLE);
            buttonShare.setVisibility(View.VISIBLE);
        }
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        updateDateTimeButtons();

        buttonDate.setOnClickListener(v -> pickDate());
        buttonTime.setOnClickListener(v -> pickTime());
        buttonSave.setOnClickListener(v -> save());
        buttonDelete.setOnClickListener(v -> confirmDelete());
        buttonShare.setOnClickListener(v -> share());
    }

    private ArrayAdapter<CharSequence> spinnerAdapter(int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayRes,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private int indexOfReminder(int minutes) {
        for (int i = 0; i < reminderMinutes.length; i++) {
            if (reminderMinutes[i] == minutes) {
                return i;
            }
        }
        return 0;
    }

    private void updateDateTimeButtons() {
        buttonDate.setText(FormatUtils.formatDate(due.getTimeInMillis()));
        buttonTime.setText(FormatUtils.formatTime(due.getTimeInMillis()));
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            due.set(year, month, day);
            updateDateTimeButtons();
        }, due.get(Calendar.YEAR), due.get(Calendar.MONTH), due.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            due.set(Calendar.HOUR_OF_DAY, hour);
            due.set(Calendar.MINUTE, minute);
            updateDateTimeButtons();
        }, due.get(Calendar.HOUR_OF_DAY), due.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this)).show();
    }

    private void save() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            editTitle.setError(getString(R.string.error_title));
            return;
        }
        task.title = title;
        task.description = editDescription.getText().toString().trim();
        task.dueDate = due.getTimeInMillis();
        task.priority = spinnerPriority.getSelectedItemPosition();
        task.reminderMinutes = reminderMinutes[spinnerReminder.getSelectedItemPosition()];

        if (task.id == -1) {
            db.insertTask(task);
        } else {
            db.updateTask(task);
        }
        ReminderScheduler.scheduleTask(this, task);
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    ReminderScheduler.cancelTask(this, task.id);
                    db.deleteTask(task.id);
                    Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Implicit intent: lets the user send the task with any app (WhatsApp, e-mail, ...). */
    private void share() {
        String text = task.title + "\nDue: " + FormatUtils.formatDateTime(task.dueDate);
        if (task.description != null && !task.description.isEmpty()) {
            text += "\n" + task.description;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
