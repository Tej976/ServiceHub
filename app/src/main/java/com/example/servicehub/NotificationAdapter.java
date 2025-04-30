package com.example.servicehub;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import java.util.List;
import java.util.Map;

/**
 * Custom adapter for handling notification read/unread states
 */
public class NotificationAdapter extends SimpleAdapter {

    public NotificationAdapter(Context context, List<? extends Map<String, ?>> data,
                               int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the base view from SimpleAdapter
        View view = super.getView(position, convertView, parent);

        // Get the notification item
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) getItem(position);

        // Get the read indicator view
        View indicator = view.findViewById(R.id.notificationReadIndicator);

        // Set the state based on read status - don't try to bind through SimpleAdapter
        if (indicator != null) {
            String readStatus = (String) item.get("read");
            if (readStatus != null && readStatus.equals("read")) {
                indicator.setBackgroundResource(R.drawable.notification_indicator_read);
            } else {
                indicator.setBackgroundResource(R.drawable.notification_indicator_unread);
            }
        }

        return view;
    }
}