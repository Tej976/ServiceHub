package com.example.servicehub.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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
import com.example.servicehub.MainActivity;
import com.example.servicehub.AddActivity;
import com.example.servicehub.notifications.NotificationsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
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
    private List<Map<String, String>> filteredBookingsList;
    private BookingsAdapter adapter;
    private String userId;
    private String userType;
    private TabLayout tabLayout;
    private BottomNavigationView bottomNavigationView;

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

        // Set up bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_bottom_bookings);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        // Initialize TabLayout
        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Confirmed"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));
        tabLayout.addTab(tabLayout.newTab().setText("Cancelled"));

        // Initialize bookings lists
        bookingsList = new ArrayList<>();
        filteredBookingsList = new ArrayList<>();

        // Set up adapter with new parameter - pass userType
        adapter = new BookingsAdapter(
                this,
                filteredBookingsList,
                R.layout.booking_item,
                new String[]{"serviceProvider", "serviceType", "dateTime", "status", "bookingId"},
                new int[]{R.id.textViewProviderName, R.id.textViewServiceType, R.id.textViewDateTime, R.id.textViewStatus, R.id.textViewBookingId},
                userType  // Pass userType to adapter
        );

        bookingsListView.setAdapter(adapter);
        bookingsListView.setEmptyView(emptyView);

        // Set item click listener
        bookingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, String> booking = filteredBookingsList.get(position);
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

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterBookingsByStatus(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
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

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.nav_bottom_home) {
                        Intent intent = new Intent(MyBookingsActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    else if (id == R.id.nav_bottom_add) {
                        Intent intent = new Intent(MyBookingsActivity.this, AddActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    else if (id == R.id.nav_bottom_bookings) {
                        // Already on bookings screen, do nothing but keep it selected
                        return true;
                    }
                    else if (id == R.id.nav_bottom_notifications) {
                        Intent intent = new Intent(MyBookingsActivity.this, NotificationsActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            };

    private void filterBookingsByStatus(int tabPosition) {
        filteredBookingsList.clear();

        if (tabPosition == 0) {
            // Show all bookings
            filteredBookingsList.addAll(bookingsList);
            emptyView.setText(bookingsList.isEmpty() ?
                    ("service_provider".equals(userType) ? "No service requests yet" : "You haven't booked any services yet") :
                    "No bookings found");
        } else {
            // Filter bookings by status
            String status = "";
            boolean isCancelledTab = false;

            switch (tabPosition) {
                case 1:
                    status = "pending";
                    break;
                case 2:
                    status = "confirmed";
                    break;
                case 3:
                    status = "completed";
                    break;
                case 4:
                    status = "cancelled";
                    isCancelledTab = true;
                    break;
            }

            // Add bookings with matching status to filtered list
            for (Map<String, String> booking : bookingsList) {
                String bookingStatus = booking.get("status");

                if (bookingStatus != null) {
                    // For the Cancelled tab, include both "cancelled" and "rejected" statuses
                    if (isCancelledTab) {
                        if (bookingStatus.equalsIgnoreCase("cancelled") ||
                                bookingStatus.equalsIgnoreCase("canceled") ||
                                bookingStatus.equalsIgnoreCase("rejected")) {
                            filteredBookingsList.add(booking);
                        }
                    } else if (bookingStatus.equalsIgnoreCase(status)) {
                        // For other tabs, use exact status matching
                        filteredBookingsList.add(booking);
                    }
                }
            }

            // Update empty view message for Cancelled tab to include both statuses
            if (isCancelledTab) {
                emptyView.setText("No cancelled or rejected bookings");
            } else {
                emptyView.setText("No " + status + " bookings");
            }
        }

        // Notify adapter of data change
        adapter.notifyDataSetChanged();
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

                    // Get customer details
                    String customerId = bookingSnapshot.child("customerId").getValue(String.class);
                    String customerName = bookingSnapshot.child("customerName").getValue(String.class);

                    // If customerName is null or empty, fetch from database
                    if ((customerName == null || customerName.isEmpty()) && customerId != null) {
                        fetchCustomerDetails(bookingId, customerId);
                    }

                    if (serviceType != null && date != null && time != null && status != null) {
                        // Create booking map
                        Map<String, String> booking = new HashMap<>();
                        booking.put("bookingId", bookingId);
                        booking.put("serviceProvider", providerName != null ? providerName : "");
                        booking.put("serviceType", serviceType);
                        booking.put("dateTime", date + " at " + time);
                        booking.put("status", status);
                        booking.put("providerId", providerId != null ? providerId : "");
                        booking.put("customerId", customerId != null ? customerId : "");
                        booking.put("customerName", customerName != null ? customerName : "");

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

                // Apply current filter
                filterBookingsByStatus(tabLayout.getSelectedTabPosition());

                // Stop refresh animation
                swipeRefreshLayout.setRefreshing(false);
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

    // New method to fetch customer details if they're not already in the booking data
    private void fetchCustomerDetails(final String bookingId, String customerId) {
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference("customers").child(customerId);

        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    // Update booking in Firebase with customer details
                    if (name != null) {
                        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                                .getReference("bookings").child(bookingId);
                        bookingRef.child("customerName").setValue(name);

                        // Also update our local list
                        for (Map<String, String> booking : bookingsList) {
                            if (bookingId.equals(booking.get("bookingId"))) {
                                booking.put("customerName", name);
                                if (phone != null) {
                                    booking.put("customerPhone", phone);
                                }
                                break;
                            }
                        }

                        // Refresh the adapter to show the updated customer name
                        filterBookingsByStatus(tabLayout.getSelectedTabPosition());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently - this is just an enhancement
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}