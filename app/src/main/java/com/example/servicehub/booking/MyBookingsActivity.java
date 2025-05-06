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
import com.example.servicehub.BottomNavigationHandler;
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
    private BottomNavigationHandler navigationHandler;

    // Add a new tab for service providers to switch between providing and receiving
    private TabLayout providerRoleTabLayout;
    private boolean isProvidingMode = true; // Default to showing services to provide

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

        // Initialize provider role tab layout
        providerRoleTabLayout = findViewById(R.id.providerRoleTabLayout);

        // First check if this user is a service provider regardless of their current role
        checkIfUserIsServiceProvider(userId);

        // Initialize bottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Setup bottom navigation handler
        if (bottomNavigationView != null) {
            navigationHandler = new BottomNavigationHandler(
                    this,
                    bottomNavigationView,
                    userId,
                    userType
            );
            // Set the selected item to bookings when this activity starts
            navigationHandler.setSelectedItem(R.id.nav_bottom_bookings);
        }

        // Initialize TabLayout for booking status
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

        // Fixed onItemClick listener to properly handle booking navigation
        bookingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, String> booking = filteredBookingsList.get(position);
                String bookingId = booking.get("bookingId");
                String providerId = booking.get("providerId");
                String bookingMode = booking.get("bookingMode");

                // Add logs to debug the navigation decision
                android.util.Log.e("CRITICAL_NAV", "************************************");
                android.util.Log.e("CRITICAL_NAV", "isProvidingMode flag: " + isProvidingMode);
                android.util.Log.e("CRITICAL_NAV", "bookingMode: " + bookingMode);
                android.util.Log.e("CRITICAL_NAV", "userType: " + userType);
                android.util.Log.e("CRITICAL_NAV", "Tab index: " + providerRoleTabLayout.getSelectedTabPosition());
                android.util.Log.e("CRITICAL_NAV", "************************************");

                // Use the bookingMode from the selected booking to determine which activity to navigate to
                if ("providing".equals(bookingMode)) {
                    // If this is a service we are providing, go to BookingDetailsActivity
                    android.util.Log.e("CRITICAL_NAV", "NAVIGATING TO: BookingDetailsActivity");
                    Intent intent = new Intent(MyBookingsActivity.this, BookingDetailsActivity.class);
                    intent.putExtra("bookingId", bookingId);
                    intent.putExtra("providerId", providerId);
                    startActivity(intent);
                } else {
                    // If this is a service we booked, go to CustomerBookingDetailsActivity
                    android.util.Log.e("CRITICAL_NAV", "NAVIGATING TO: CustomerBookingDetailsActivity");
                    Intent intent = new Intent(MyBookingsActivity.this, CustomerBookingDetailsActivity.class);
                    intent.putExtra("bookingId", bookingId);
                    startActivity(intent);
                }
            }
        });

        // Set up tab selection listener for status tabs
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

    // New method to check if the user is a service provider regardless of their current role
    private void checkIfUserIsServiceProvider(String userId) {
        DatabaseReference serviceProvidersRef = FirebaseDatabase.getInstance()
                .getReference("service_providers");

        serviceProvidersRef.orderByChild("authUid").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean isServiceProvider = dataSnapshot.exists();

                        if (isServiceProvider) {
                            // This user is a service provider, show the provider role tab layout
                            setupProviderRoleTabs();
                        } else {
                            // This user is only a customer, hide the provider role tab layout
                            providerRoleTabLayout.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // On error, default to hiding the provider role tabs
                        providerRoleTabLayout.setVisibility(View.GONE);
                    }
                });
    }


    private void setupProviderRoleTabs() {
        // Show the provider role tab layout
        providerRoleTabLayout.setVisibility(View.VISIBLE);

        // Clear existing tabs
        providerRoleTabLayout.removeAllTabs();

        // Add tabs
        providerRoleTabLayout.addTab(providerRoleTabLayout.newTab().setText("Services to Provide"));
        providerRoleTabLayout.addTab(providerRoleTabLayout.newTab().setText("Services I Booked"));

        // Set up tab selection listener for provider role tabs
        providerRoleTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isProvidingMode = tab.getPosition() == 0;

                android.util.Log.e("CRITICAL_TAB", "Tab selected: " + tab.getPosition());
                android.util.Log.e("CRITICAL_TAB", "isProvidingMode set to: " + isProvidingMode);

                // Clear lists
                bookingsList.clear();
                filteredBookingsList.clear();

                // Reload with new mode
                loadBookings();
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
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the bookings tab stays selected when returning to this activity
        if (navigationHandler != null) {
            navigationHandler.setSelectedItem(R.id.nav_bottom_bookings);
        }
        // Refresh bookings when returning to the activity
        loadBookings();
    }

    private void filterBookingsByStatus(int tabPosition) {
        filteredBookingsList.clear();

        if (tabPosition == 0) {
            // Show all bookings
            filteredBookingsList.addAll(bookingsList);

            // Update empty view message based on user type and mode
            if ("service_provider".equals(userType)) {
                if (isProvidingMode) {
                    emptyView.setText(bookingsList.isEmpty() ? "No service requests from customers yet" : "No bookings found");
                } else {
                    emptyView.setText(bookingsList.isEmpty() ? "You haven't booked any services yet" : "No bookings found");
                }
            } else {
                emptyView.setText(bookingsList.isEmpty() ? "You haven't booked any services yet" : "No bookings found");
            }
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
                            // Make a deep copy of the booking to ensure bookingMode is preserved
                            Map<String, String> bookingCopy = new HashMap<>(booking);
                            filteredBookingsList.add(bookingCopy);
                        }
                    } else if (bookingStatus.equalsIgnoreCase(status)) {
                        // For other tabs, use exact status matching
                        // Make a deep copy of the booking to ensure bookingMode is preserved
                        Map<String, String> bookingCopy = new HashMap<>(booking);
                        filteredBookingsList.add(bookingCopy);
                    }
                }
            }

            // Update empty view message
            if ("service_provider".equals(userType)) {
                String rolePrefix = isProvidingMode ? "to provide" : "booked";

                if (isCancelledTab) {
                    emptyView.setText("No cancelled or rejected bookings " + rolePrefix);
                } else {
                    emptyView.setText("No " + status + " bookings " + rolePrefix);
                }
            } else {
                if (isCancelledTab) {
                    emptyView.setText("No cancelled or rejected bookings");
                } else {
                    emptyView.setText("No " + status + " bookings");
                }
            }
        }

        // Debug the filtered list
        for (int i = 0; i < filteredBookingsList.size(); i++) {
            Map<String, String> booking = filteredBookingsList.get(i);
            android.util.Log.d("FilteredList", "Item " + i + " - BookingMode: " + booking.get("bookingMode"));
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

        bookingsList.clear();

        // Check if provider tabs are visible and use the current mode
        if (providerRoleTabLayout.getVisibility() == View.VISIBLE) {
            if (isProvidingMode) {
                // Load bookings where this user is the provider
                loadProviderBookings();
            } else {
                // Load bookings where this user is the customer
                loadCustomerBookings();
            }
        } else {
            // For regular customers, only load customer bookings
            loadCustomerBookings();
        }
    }

    private void loadProviderBookings() {
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        // Find the provider ID by auth UID
        DatabaseReference serviceProvidersRef = FirebaseDatabase.getInstance()
                .getReference("service_providers");

        serviceProvidersRef.orderByChild("authUid").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot providerSnapshot : dataSnapshot.getChildren()) {
                                String providerId = providerSnapshot.getKey();

                                // Now query for bookings with this provider ID
                                Query query = bookingsRef.orderByChild("providerId").equalTo(providerId);

                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot bookingSnapshot : dataSnapshot.getChildren()) {
                                            // Always set bookingMode to "providing" for provider bookings
                                            processBookingSnapshot(bookingSnapshot, "providing");
                                        }

                                        // Sort and display bookings
                                        sortAndDisplayBookings();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        handleDatabaseError(databaseError);
                                    }
                                });
                            }
                        } else {
                            // No provider record found, show empty list
                            sortAndDisplayBookings();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        handleDatabaseError(databaseError);
                    }
                });
    }


    private void loadCustomerBookings() {
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");
        Query query = bookingsRef.orderByChild("customerId").equalTo(userId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot bookingSnapshot : dataSnapshot.getChildren()) {
                    // Always set bookingMode to "receiving" for customer bookings
                    processBookingSnapshot(bookingSnapshot, "receiving");
                }

                // Sort and display bookings
                sortAndDisplayBookings();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void processBookingSnapshot(DataSnapshot bookingSnapshot, String bookingMode) {
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
            booking.put("userId", userId); // Current user ID

            // Explicitly set the bookingMode - this is critical for correct navigation
            booking.put("bookingMode", bookingMode);

            bookingsList.add(booking);
        }
    }

    private void sortAndDisplayBookings() {
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

    private void handleDatabaseError(DatabaseError databaseError) {
        Toast.makeText(MyBookingsActivity.this,
                "Failed to load bookings: " + databaseError.getMessage(),
                Toast.LENGTH_SHORT).show();
        swipeRefreshLayout.setRefreshing(false);
    }

    // Method to fetch customer details if they're not already in the booking data
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