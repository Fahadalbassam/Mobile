package com.group5.studentplanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Weekly class schedule, grouped by day (Sunday to Saturday).
 */
public class ScheduleActivity extends AppCompatActivity {

    private ListView listClasses;
    private TextView textEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        InsetsHelper.apply(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listClasses = findViewById(R.id.listClasses);
        textEmpty = findViewById(R.id.textEmpty);

        findViewById(R.id.buttonAddClass).setOnClickListener(
                v -> startActivity(new Intent(this, ClassEditActivity.class)));

        listClasses.setOnItemClickListener((parent, view, position, id) -> {
            ClassSession session = (ClassSession) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, ClassEditActivity.class);
            intent.putExtra(ClassEditActivity.EXTRA_CLASS_ID, session.id);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<ClassSession> classes = DatabaseHelper.getInstance(this).getAllClasses();
        listClasses.setAdapter(new ClassAdapter(this, classes));
        boolean empty = classes.isEmpty();
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listClasses.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
