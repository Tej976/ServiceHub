package com.example.servicehub;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private List<Map<String, String>> notificationsList;
    private SimpleAdapter adapter;
    private String userId;
    private String userType;

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

        // Initialize notifications list
        notificationsList = new ArrayList<>();

        // Set up adapter
        adapter = new SimpleAdapter(
                this,
                notificationsList,
                R.layout.notification_item,
                new String[]{"title", "message", "time"},
                new int[]{R.id.textViewNotificationTitle, R.id.textViewNotificationMessage, R.id.textViewNotificationTime}
        );

        notificationsListView.setAdapter(adapter);
        notificationsListView.setEmptyView(emptyView);

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

                    if (title != null && message != null && timestamp != null) {
                        // Format timestamp
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                        String timeString = sdf.format(new Date(timestamp));

                        // Create notification map
                        Map<String, String> notification = new HashMap<>();
                        notification.put("title", title);
                        notification.put("message", message);
                        notification.put("time", timeString);

                        notificationsList.add(notification);
                    }
                }

                // Sort notifications by timestamp (newest first)
                Collections.sort(notificationsList, new Comparator<Map<String, String>>() {
                    @Override
                    public int compare(Map<String, String> o1, Map<String, String> o2) {
                        return o2.get("time").compareTo(o1.get("time"));
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