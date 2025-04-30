package com.example.servicehub.booking;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.servicehub.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerBookingDetailsActivity extends AppCompatActivity {

    private TextView tvProviderName, tvProviderPhone, tvServiceType, tvDate, tvTime, tvRequirements, tvStatus;
    private Button btnCancel;
    private String bookingId;
    private DatabaseReference bookingRef, notificationsRef;
    private String providerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_booking_details);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Booking Details");

        // Initialize UI components
        tvProviderName = findViewById(R.id.tvProviderName);
        tvProviderPhone = findViewById(R.id.tvProviderPhone);
        tvServiceType = findViewById(R.id.tvServiceType);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvRequirements = findViewById(R.id.tvRequirements);
        tvStatus = findViewById(R.id.tvStatus);
        btnCancel = findViewById(R.id.btnCancel);

        // Get booking ID from intent
        bookingId = getIntent().getStringExtra("bookingId");

        if (bookingId == null) {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database references
        bookingRef = FirebaseDatabase.getInstance().getReference("bookings").child(bookingId);
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");

        // Load booking details
        loadBookingDetails();

        // Set up cancel button
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelBooking();
            }
        });
    }

    private void loadBookingDetails() {
        bookingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String serviceType = dataSnapshot.child("serviceType").getValue(String.class);
                    String date = dataSnapshot.child("date").getValue(String.class);
                    String time = dataSnapshot.child("time").getValue(String.class);
                    String requirements = dataSnapshot.child("requirements").getValue(String.class);
                    String status = dataSnapshot.child("status").getValue(String.class);
                    providerId = dataSnapshot.child("providerId").getValue(String.class);

                    // Try to get provider name from booking data
                    String providerName = dataSnapshot.child("providerName").getValue(String.class);

                    // Update UI with booking details
                    if (providerName != null && !providerName.isEmpty()) {
                        tvProviderName.setText(providerName);
                    } else {
                        tvProviderName.setText("Loading provider info...");
                    }

                    tvServiceType.setText(serviceType != null ? serviceType : "N/A");
                    tvDate.setText(date != null ? date : "N/A");
                    tvTime.setText(time != null ? time : "N/A");
                    tvRequirements.setText(requirements != null ? requirements : "N/A");
                    tvStatus.setText(status != null ?
                            status.substring(0, 1).toUpperCase() + status.substring(1) : "N/A");

                    // Show cancel button only for pending or confirmed bookings
                    if ("pending".equals(status) || "confirmed".equals(status)) {
                        btnCancel.setVisibility(View.VISIBLE);
                    } else {
                        btnCancel.setVisibility(View.GONE);
                    }

                    // Get provider details from database
                    if (providerId != null) {
                        loadProviderDetails(providerId);
                    }
                } else {
                    Toast.makeText(CustomerBookingDetailsActivity.this, "Booking no longer exists", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CustomerBookingDetailsActivity.this, "Failed to load booking details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProviderDetails(String providerId) {
        DatabaseReference providerRef = FirebaseDatabase.getInstance()
                .getReference("service_providers").child(providerId);

        providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("mobile").getValue(String.class);

                    // Update provider name if it exists
                    if (name != null && !name.isEmpty()) {
                        tvProviderName.setText(name);
                    }

                    // Set phone number
                    if (phone != null && !phone.isEmpty()) {
                        tvProviderPhone.setText(phone);
                    } else {
                        tvProviderPhone.setText("N/A");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerBookingDetailsActivity.this,
                        "Failed to load provider details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelBooking() {
        // Update booking status to cancelled
        bookingRef.child("status").setValue("cancelled")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(CustomerBookingDetailsActivity.this,
                                    "Booking cancelled", Toast.LENGTH_SHORT).show();

                            // Create notification for service provider
                            createNotification(providerId);

                            // Send popup notification
                            sendPopupNotification(providerId, "Booking Cancelled",
                                    "A customer has cancelled their booking.");

                            // Hide cancel button
                            btnCancel.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(CustomerBookingDetailsActivity.this,
                                    "Failed to cancel booking", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Method to send popup notification to specific user
    private void sendPopupNotification(String userId, String title, String message) {
        // First store notification data in Firebase
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("popup_notifications").child(userId);

        String notificationId = notificationsRef.push().getKey();

        if (notificationId != null) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("bookingId", bookingId);
            notification.put("read", false);

            notificationsRef.child(notificationId).setValue(notification);
        }
    }

    private void createNotification(String userId) {
        String notificationId = notificationsRef.push().getKey();

        if (notificationId != null) {
            String title = "Booking Cancelled";
            String message = "A customer has cancelled their booking.";

            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("bookingId", bookingId);
            notification.put("read", false);

            notificationsRef.child(notificationId).setValue(notification);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}