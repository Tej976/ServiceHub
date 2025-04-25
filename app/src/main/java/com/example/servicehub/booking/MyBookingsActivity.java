package com.example.servicehub.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.servicehub.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBookingsActivity extends AppCompatActivity {

    private ListView bookingsListView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Map<String, String>> bookingsList;
    private BookingsAdapter adapter;
    private String userId;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Bookings");

        // Get user info from intent
        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        // If not provided in intent, try to get from Firebase Auth
        if (userId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Initialize UI components
        bookingsListView = findViewById(R.id.listViewBookings);
        emptyView = findViewById(R.id.emptyBookingsView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initialize bookings list
        bookingsList = new ArrayList<>();

        // Set up adapter
        adapter = new BookingsAdapter(
                this,
                bookingsList,
                R.layout.booking_item,
                new String[]{"serviceProvider", "serviceType", "dateTime", "status", "bookingId"},
                new int[]{R.id.textViewProviderName, R.id.textViewServiceType, R.id.textViewDateTime, R.id.textViewStatus, R.id.textViewBookingId}
        );

        bookingsListView.setAdapter(adapter);
        bookingsListView.setEmptyView(emptyView);

        // Set item click listener
        bookingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, String> booking = bookingsList.get(position);
                String bookingId = booking.get("bookingId");
                String providerId = booking.get("providerId");

                // Start different activity based on user type
                if ("service_provider".equals(userType)) {
                    Intent intent = new Intent(MyBookingsActivity.this, BookingDetailsActivity.class);
                    intent.putExtra("bookingId", bookingId);
                    intent.putExtra("providerId", providerId);
                    startActivity(intent);
                } else {
                    // For customers, show booking details without accept/reject buttons
                    Intent intent = new Intent(MyBookingsActivity.this, CustomerBookingDetailsActivity.class);
                    intent.putExtra("bookingId", bookingId);
                    startActivity(intent);
                }
            }
        });

        // Load bookings
        loadBookings();

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadBookings();
            }
        });
    }

    private void loadBookings() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        // Query bookings based on user type
        Query query;
        if ("service_provider".equals(userType)) {
            query = bookingsRef.orderByChild("providerId").equalTo(userId);
        } else {
            query = bookingsRef.orderByChild("customerId").equalTo(userId);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookingsList.clear();

                for (DataSnapshot bookingSnapshot : dataSnapshot.getChildren()) {
                    String bookingId = bookingSnapshot.getKey();
                    String providerName = bookingSnapshot.child("providerName").getValue(String.class);
                    String serviceType = bookingSnapshot.child("serviceType").getValue(String.class);
                    String date = bookingSnapshot.child("date").getValue(String.class);
                    String time = bookingSnapshot.child("time").getValue(String.class);
                    String status = bookingSnapshot.child("status").getValue(String.class);
                    String providerId = bookingSnapshot.child("providerId").getValue(String.class);

                    if (providerName != null && serviceType != null && date != null && time != null && status != null) {
                        // Create booking map
                        Map<String, String> booking = new HashMap<>();
                        booking.put("bookingId", bookingId);
                        booking.put("serviceProvider", providerName);
                        booking.put("serviceType", serviceType);
                        booking.put("dateTime", date + " at " + time);
                        booking.put("status", status);
                        booking.put("providerId", providerId);

                        bookingsList.add(booking);
                    }
                }

                // Sort bookings by date (most recent first)
                Collections.sort(bookingsList, new Comparator<Map<String, String>>() {
                    @Override
                    public int compare(Map<String, String> o1, Map<String, String> o2) {
                        return o2.get("dateTime").compareTo(o1.get("dateTime"));
                    }
                });

                // Update UI
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);

                // Update empty view message based on user type
                if (bookingsList.isEmpty()) {
                    if ("service_provider".equals(userType)) {
                        emptyView.setText("No service requests yet");
                    } else {
                        emptyView.setText("You haven't booked any services yet");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MyBookingsActivity.this,
                        "Failed to load bookings: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}