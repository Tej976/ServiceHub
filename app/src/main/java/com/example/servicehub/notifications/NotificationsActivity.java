package com.example.servicehub.notifications;

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
import com.example.servicehub.booking.BookingDetailsActivity;
import com.example.servicehub.BottomNavigationHandler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.widget.SimpleAdapter;

public class NotificationsActivity extends AppCompatActivity {

    private ListView notificationsListView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Map<String, Object>> notificationsList;
    private SimpleAdapter adapter;
    private String userId;
    private String userType;
    private BottomNavigationView bottomNavigationView;
    private BottomNavigationHandler navigationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        // Get user info from intent
        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        // If not provided in intent, try to get from Firebase Auth
        if (userId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Initialize UI components
        notificationsListView = findViewById(R.id.listViewNotifications);
        emptyView = findViewById(R.id.emptyNotificationsView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

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
            // Set the selected item to notifications when this activity starts
            navigationHandler.setSelectedItem(R.id.nav_bottom_notifications);
        }

        // Initialize notifications list
        notificationsList = new ArrayList<>();

        // Set up adapter with custom notification adapter
        adapter = new NotificationAdapter(
                this,
                notificationsList,
                R.layout.notification_item,
                new String[]{"title", "message", "time"},
                new int[]{R.id.textViewNotificationTitle, R.id.textViewNotificationMessage,
                        R.id.textViewNotificationTime}
        );

        notificationsListView.setAdapter(adapter);
        notificationsListView.setEmptyView(emptyView);

        // Set item click listener
        notificationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> notification = notificationsList.get(position);
                String bookingId = (String) notification.get("bookingId");
                String notificationId = (String) notification.get("id");

                // Mark notification as read
                if (notificationId != null) {
                    DatabaseReference notifRef = FirebaseDatabase.getInstance()
                            .getReference("notifications").child(notificationId);
                    notifRef.child("read").setValue(true);
                }

                // Navigate to booking details if there's a booking ID
                if (bookingId != null && !bookingId.isEmpty()) {
                    Intent intent = new Intent(NotificationsActivity.this, BookingDetailsActivity.class);
                    intent.putExtra("bookingId", bookingId);
                    startActivity(intent);
                }
            }
        });

        // Load notifications
        loadNotifications();

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadNotifications();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the notifications tab stays selected when returning to this activity
        if (navigationHandler != null) {
            navigationHandler.setSelectedItem(R.id.nav_bottom_notifications);
        }
    }

    private void loadNotifications() {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");

        // Query notifications for this user
        Query query = notificationsRef.orderByChild("userId").equalTo(userId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificationsList.clear();

                for (DataSnapshot notificationSnapshot : dataSnapshot.getChildren()) {
                    String title = notificationSnapshot.child("title").getValue(String.class);
                    String message = notificationSnapshot.child("message").getValue(String.class);
                    Long timestamp = notificationSnapshot.child("timestamp").getValue(Long.class);
                    String bookingId = notificationSnapshot.child("bookingId").getValue(String.class);
                    Boolean read = notificationSnapshot.child("read").getValue(Boolean.class);
                    if (read == null) read = false;

                    if (title != null && message != null && timestamp != null) {
                        // Format timestamp
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                        String timeString = sdf.format(new Date(timestamp));

                        // Create notification map
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("id", notificationSnapshot.getKey());
                        notification.put("title", title);
                        notification.put("message", message);
                        notification.put("time", timeString);
                        notification.put("bookingId", bookingId);
                        notification.put("read", read ? "read" : "unread");
                        notification.put("timestamp", timestamp);

                        notificationsList.add(notification);
                    }
                }

                // Sort notifications by timestamp (newest first)
                Collections.sort(notificationsList, new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        Long t1 = (Long) o1.get("timestamp");
                        Long t2 = (Long) o2.get("timestamp");
                        return t2.compareTo(t1); // Descending order
                    }
                });

                // Update UI
                adapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);

                // Show/hide empty view
                if (notificationsList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NotificationsActivity.this,
                        "Failed to load notifications: " + databaseError.getMessage(),
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