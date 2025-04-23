package com.example.servicehub.booking;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.servicehub.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyBookingsActivity extends AppCompatActivity {

    private ListView bookingsListView;
    private List<Map<String, String>> bookingsList;
    private BookingsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyBookingsText;
    private FirebaseUser currentUser;
    private DatabaseReference bookingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Bookings");

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        // Initialize UI components
        bookingsListView = findViewById(R.id.listViewBookings);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyBookingsText = findViewById(R.id.emptyBookingsText);

        // Check if user is logged in
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view your bookings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize the bookings list and adapter
        bookingsList = new ArrayList<>();
        adapter = new BookingsAdapter(
                this,
                bookingsList,
                R.layout.booking_item,
                new String[]{"serviceType", "providerName", "date", "time", "status"},
                new int[]{R.id.textViewServiceType, R.id.textViewProviderName,
                        R.id.textViewBookingDate, R.id.textViewBookingTime, R.id.textViewStatus}
        );

        bookingsListView.setAdapter(adapter);

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadBookings();
            }
        });

        // Load bookings initially
        loadBookings();

        // Set up a daily check for expired bookings (in a real app, this would be done with a WorkManager or Service)
        checkExpiredBookings();
    }

    private void loadBookings() {
        swipeRefreshLayout.setRefreshing(true);
        String userId = currentUser.getUid();

        // Query bookings for current user
        Query userBookingsQuery = bookingsRef.orderByChild("customerId").equalTo(userId);

        userBookingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookingsList.clear();

                for (DataSnapshot bookingSnapshot : dataSnapshot.getChildren()) {
                    String bookingId = bookingSnapshot.getKey();
                    String serviceType = bookingSnapshot.child("serviceType").getValue(String.class);
                    String providerName = bookingSnapshot.child("providerName").getValue(String.class);
                    String date = bookingSnapshot.child("date").getValue(String.class);
                    String time = bookingSnapshot.child("time").getValue(String.class);
                    String status = bookingSnapshot.child("status").getValue(String.class);

                    // Check if this booking is expired
                    boolean isExpired = isBookingExpired(date, time);

                    // If booking is expired, update its status or delete it
                    if (isExpired) {
                        // Option 1: Delete expired booking
                        bookingsRef.child(bookingId).removeValue();

                        // Option 2: Mark as expired instead of deleting
                        // bookingsRef.child(bookingId).child("status").setValue("expired");

                        // Skip adding this booking to the list
                        continue;
                    }

                    // If booking is not expired, add it to the list
                    if (serviceType != null && providerName != null && date != null && time != null) {
                        Map<String, String> bookingMap = new HashMap<>();
                        bookingMap.put("id", bookingId);
                        bookingMap.put("serviceType", serviceType);
                        bookingMap.put("providerName", providerName);
                        bookingMap.put("date", date);
                        bookingMap.put("time", time);
                        bookingMap.put("status", status != null ? status : "pending");

                        bookingsList.add(bookingMap);
                    }
                }

                // Update UI
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);

                // Show/hide empty state message
                if (bookingsList.isEmpty()) {
                    emptyBookingsText.setVisibility(View.VISIBLE);
                    bookingsListView.setVisibility(View.GONE);
                } else {
                    emptyBookingsText.setVisibility(View.GONE);
                    bookingsListView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MyBookingsActivity.this,
                        "Failed to load bookings: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isBookingExpired(String date, String time) {
        try {
            // Parse the booking date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            Date bookingDate = dateFormat.parse(date);
            Date bookingTime = timeFormat.parse(time);

            // Combine date and time
            Calendar bookingCalendar = Calendar.getInstance();
            Calendar timeCalendar = Calendar.getInstance();

            bookingCalendar.setTime(bookingDate);
            timeCalendar.setTime(bookingTime);

            bookingCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            bookingCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));

            // Get current time
            Calendar now = Calendar.getInstance();

            // Check if booking date is in the past
            return bookingCalendar.before(now);

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void checkExpiredBookings() {
        // In production, this would be done with a WorkManager or a Service
        // For this example, we're just doing it when the activity opens
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        Query userBookingsQuery = bookingsRef.orderByChild("customerId").equalTo(userId);

        userBookingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot bookingSnapshot : dataSnapshot.getChildren()) {
                    String bookingId = bookingSnapshot.getKey();
                    String date = bookingSnapshot.child("date").getValue(String.class);
                    String time = bookingSnapshot.child("time").getValue(String.class);

                    if (date != null && time != null && isBookingExpired(date, time)) {
                        // Delete expired booking
                        bookingsRef.child(bookingId).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}