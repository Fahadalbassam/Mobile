package com.group5.studentplanner;

import android.app.TimePickerDialog;
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
 * Screen for adding a new class to the weekly schedule or editing / deleting an existing one.
 */
public class ClassEditActivity extends AppCompatActivity {

    public static final String EXTRA_CLASS_ID = "class_id";

    private EditText editCourse;
    private EditText editLocation;
    private EditText editInstructor;
    private Spinner spinnerDay;
    private Spinner spinnerReminder;
    private Button buttonStart;
    private Button buttonEnd;

    private DatabaseHelper db;
    private ClassSession session;
    private int startMinutes = 8 * 60;   // 8:00 AM
    private int endMinutes = 9 * 60;     // 9:00 AM
    private int[] reminderMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_edit);
        InsetsHelper.apply(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);
        reminderMinutes = getResources().getIntArray(R.array.reminder_minutes);

        editCourse = findViewById(R.id.editCourse);
        editLocation = findViewById(R.id.editLocation);
        editInstructor = findViewById(R.id.editInstructor);
        spinnerDay = findViewById(R.id.spinnerDay);
        spinnerReminder = findViewById(R.id.spinnerReminder);
        buttonStart = findViewById(R.id.buttonStart);
        buttonEnd = findViewById(R.id.buttonEnd);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonDelete = findViewById(R.id.buttonDelete);

        spinnerDay.setAdapter(spinnerAdapter(R.array.days));
        spinnerReminder.setAdapter(spinnerAdapter(R.array.reminder_options));

        long classId = getIntent().getLongExtra(EXTRA_CLASS_ID, -1);
        session = classId == -1 ? null : db.getClassSession(classId);

        if (session == null) {
            // New class: default to today and a 10-minute reminder.
            session = new ClassSession();
            spinnerDay.setSelection(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1);
            spinnerReminder.setSelection(indexOfReminder(10));
            setTitle(R.string.add_class);
        } else {
            editCourse.setText(session.courseName);
            editLocation.setText(session.location);
            editInstructor.setText(session.instructor);
            spinnerDay.setSelection(session.dayOfWeek - 1);
            spinnerReminder.setSelection(indexOfReminder(session.reminderMinutes));
            startMinutes = session.startMinutes;
            endMinutes = session.endMinutes;
            buttonDelete.setVisibility(View.VISIBLE);
        }
        updateTimeButtons();

        buttonStart.setOnClickListener(v -> pickTime(true));
        buttonEnd.setOnClickListener(v -> pickTime(false));
        buttonSave.setOnClickListener(v -> save());
        buttonDelete.setOnClickListener(v -> confirmDelete());
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

    private void updateTimeButtons() {
        buttonStart.setText(FormatUtils.formatMinutes(startMinutes));
        buttonEnd.setText(FormatUtils.formatMinutes(endMinutes));
    }

    private void pickTime(boolean isStart) {
        int current = isStart ? startMinutes : endMinutes;
        new TimePickerDialog(this, (view, hour, minute) -> {
            if (isStart) {
                startMinutes = hour * 60 + minute;
            } else {
                endMinutes = hour * 60 + minute;
            }
            updateTimeButtons();
        }, current / 60, current % 60, DateFormat.is24HourFormat(this)).show();
    }

    private void save() {
        String course = editCourse.getText().toString().trim();
        if (course.isEmpty()) {
            editCourse.setError(getString(R.string.error_course));
            return;
        }
        if (endMinutes <= startMinutes) {
            Toast.makeText(this, R.string.error_time, Toast.LENGTH_SHORT).show();
            return;
        }
        session.courseName = course;
        session.location = editLocation.getText().toString().trim();
        session.instructor = editInstructor.getText().toString().trim();
        session.dayOfWeek = spinnerDay.getSelectedItemPosition() + 1; // Calendar.SUNDAY = 1
        session.startMinutes = startMinutes;
        session.endMinutes = endMinutes;
        session.reminderMinutes = reminderMinutes[spinnerReminder.getSelectedItemPosition()];

        if (session.id == -1) {
            db.insertClass(session);
        } else {
            db.updateClass(session);
        }
        ReminderScheduler.scheduleClass(this, session);
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    ReminderScheduler.cancelClass(this, session.id);
                    db.deleteClass(session.id);
                    Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
