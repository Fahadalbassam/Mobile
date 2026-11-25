package com.group5.studentplanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Turns each ClassSession into one row (item_class.xml) of the weekly schedule.
 * The list is already sorted by day, so a day header is shown above the first class of each day.
 */
public class ClassAdapter extends ArrayAdapter<ClassSession> {

    public ClassAdapter(Context context, List<ClassSession> classes) {
        super(context, 0, classes);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext()).inflate(R.layout.item_class, parent, false);
        }
        ClassSession s = getItem(position);
        TextView textDayHeader = row.findViewById(R.id.textDayHeader);
        TextView textTime = row.findViewById(R.id.textTime);
        TextView textCourse = row.findViewById(R.id.textCourse);
        TextView textLocation = row.findViewById(R.id.textLocation);

        boolean firstOfDay = position == 0 || getItem(position - 1).dayOfWeek != s.dayOfWeek;
        textDayHeader.setVisibility(firstOfDay ? View.VISIBLE : View.GONE);
        textDayHeader.setText(FormatUtils.dayName(getContext(), s.dayOfWeek));

        textTime.setText(FormatUtils.formatMinutes(s.startMinutes) + "\n"
                + FormatUtils.formatMinutes(s.endMinutes));
        textCourse.setText(s.courseName);

        StringBuilder details = new StringBuilder();
        if (s.location != null && !s.location.isEmpty()) {
            details.append(s.location);
        }
        if (s.instructor != null && !s.instructor.isEmpty()) {
            if (details.length() > 0) {
                details.append(" • ");
            }
            details.append(s.instructor);
        }
        textLocation.setText(details.toString());
        textLocation.setVisibility(details.length() > 0 ? View.VISIBLE : View.GONE);

        return row;
    }
}
