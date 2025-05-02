package com.example.servicehub.booking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.servicehub.R;
import java.util.List;
import java.util.Map;

public class BookingsAdapter extends SimpleAdapter {
    private Context context;
    private List<? extends Map<String, ?>> data;
    private String userType;
    private LayoutInflater inflater;

    public BookingsAdapter(Context context,
                           List<? extends Map<String, ?>> data,
                           int resource,
                           String[] from,
                           int[] to,
                           String userType) {
        super(context, data, resource, from, to);
        this.context = context;
        this.data = data;
        this.userType = userType;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        // Get the TextViews
        TextView nameView = view.findViewById(R.id.textViewProviderName);
        TextView serviceView = view.findViewById(R.id.textViewServiceType);
        TextView statusView = view.findViewById(R.id.textViewStatus);
        TextView roleIndicator = view.findViewById(R.id.textViewRoleIndicator);
        ImageView iconView = view.findViewById(R.id.iconBookingType);

        // Get the data for this booking
        Map<String, ?> bookingData = data.get(position);
        String status = (String) bookingData.get("status");
        String providerId = (String) bookingData.get("providerId");
        String userId = (String) bookingData.get("userId"); // Current user ID
        String customerId = (String) bookingData.get("customerId");
        String bookingMode = (String) bookingData.get("bookingMode"); // "providing" or "receiving"

        // Determine if the service provider is viewing a booking they're providing or receiving
        if ("service_provider".equals(userType)) {
            if ("providing".equals(bookingMode)) {
                // Service provider is providing this service
                String customerName = (String) bookingData.get("customerName");
                nameView.setText(customerName != null && !customerName.isEmpty() ? customerName : "Customer");
                roleIndicator.setText("You are providing");
                roleIndicator.setVisibility(View.VISIBLE);
                iconView.setImageResource(R.drawable.ic_providing_service);
            } else {
                // Service provider is receiving this service (acting as a customer)
                String providerName = (String) bookingData.get("serviceProvider");
                nameView.setText(providerName != null && !providerName.isEmpty() ? providerName : "Provider");
                roleIndicator.setText("You are receiving");
                roleIndicator.setVisibility(View.VISIBLE);
                iconView.setImageResource(R.drawable.ic_receiving_service);
            }
        } else {
            // Regular customer - only shows services they're receiving
            String providerName = (String) bookingData.get("serviceProvider");
            nameView.setText(providerName != null && !providerName.isEmpty() ? providerName : "Provider");
            roleIndicator.setVisibility(View.GONE); // Hide role indicator for regular customers
            iconView.setVisibility(View.GONE); // Hide icon for regular customers
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