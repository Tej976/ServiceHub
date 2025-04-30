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
    private String userType; // Add userType field to determine display mode

    public BookingsAdapter(Context context,
                           List<? extends Map<String, ?>> data,
                           int resource,
                           String[] from,
                           int[] to,
                           String userType) { // Add userType parameter
        super(context, data, resource, from, to);
        this.context = context;
        this.data = data;
        this.userType = userType;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        // Get the TextViews
        TextView nameView = view.findViewById(R.id.textViewProviderName);
        TextView serviceView = view.findViewById(R.id.textViewServiceType);
        TextView statusView = view.findViewById(R.id.textViewStatus);

        // Get the data for this booking
        Map<String, ?> bookingData = data.get(position);
        String status = (String) bookingData.get("status");

        // Customize display based on user type
        if ("service_provider".equals(userType)) {
            // If user is a service provider, show customer information
            String customerName = (String) bookingData.get("customerName");
            if (customerName != null && !customerName.isEmpty()) {
                nameView.setText(customerName);
            } else {
                nameView.setText("Customer");
            }
        } else {
            // If user is a customer, show provider information (default behavior)
            String providerName = (String) bookingData.get("serviceProvider");
            nameView.setText(providerName);
        }

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
                case "canceled":
                case "rejected":
                    statusView.setTextColor(ContextCompat.getColor(context, R.color.cancelled_color));
                    break;
                default:
                    statusView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
            }
            // Capitalize first letter of status
            statusView.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
        }

        return view;
    }
}