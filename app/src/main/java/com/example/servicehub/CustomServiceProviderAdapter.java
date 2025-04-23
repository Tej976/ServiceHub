package com.example.servicehub;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SimpleAdapter;

import java.util.List;
import java.util.Map;

public class CustomServiceProviderAdapter extends SimpleAdapter {

    private Context context;
    private List<? extends Map<String, ?>> data;

    public CustomServiceProviderAdapter(Context context,
                                        List<? extends Map<String, ?>> data,
                                        int resource,
                                        String[] from,
                                        int[] to) {
        super(context, data, resource, from, to);
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // Get the view from the parent adapter
        View view = super.getView(position, convertView, parent);

        // Find the Book Now button
        Button bookButton = view.findViewById(R.id.btnBookNow);

        // Set click listener
        bookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the provider data for this position
                Map<String, ?> providerData = data.get(position);

                // Create intent to start the BookingActivity
                Intent intent = new Intent(context, BookingActivity.class);

                // Pass relevant data to the booking activity
                intent.putExtra("providerName", (String) providerData.get("name"));
                intent.putExtra("providerMobile", (String) providerData.get("mobile"));
                intent.putExtra("providerAddress", (String) providerData.get("address"));
                intent.putExtra("providerId", (String) providerData.get("id"));
                intent.putExtra("serviceName", ((ServiceProvidersActivity) context).getIntent().getStringExtra("serviceName"));

                // Start the activity
                context.startActivity(intent);
            }
        });

        return view;
    }
}