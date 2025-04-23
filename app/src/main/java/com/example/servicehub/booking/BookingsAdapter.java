package com.example.servicehub.booking;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.servicehub.R;

import java.util.List;
import java.util.Map;

public class BookingsAdapter extends SimpleAdapter {

    private Context context;
    private List<? extends Map<String, ?>> data;

    public BookingsAdapter(Context context,
                           List<? extends Map<String, ?>> data,
                           int resource,
                           String[] from,
                           int[] to) {
        super(context, data, resource, from, to);
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        // Get the status TextView
        TextView statusView = view.findViewById(R.id.textViewStatus);

        // Get the status for this booking
        String status = (String) data.get(position).get("status");

        // Set the color based on status
        if (status != null) {
            switch (status.toLowerCase()) {
                case "pending":
                    statusView.setTextColor(ContextCompat.getColor(context, R.color.pending_color));
                    break;
                case "confirmed":
                    statusView.setTextColor(ContextCompat.getColor(context, R.color.confirmed_color));
                    break;
                case "completed":
                    statusView.setTextColor(ContextCompat.getColor(context, R.color.completed_color));
                    break;
                case "cancelled":
                    statusView.setTextColor(ContextCompat.getColor(context, R.color.cancelled_color));
                    break;
                default:
                    statusView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
            }
        }

        return view;
    }
}