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

public class BookingDetailsActivity extends AppCompatActivity {

    private TextView tvServiceType, tvDate, tvTime, tvRequirements, tvStatus;
    private TextView tvCustomerName, tvPhoneNo, tvAdd;
    private Button btnAccept, btnReject;
    private String bookingId, providerId, customerId;
    private DatabaseReference bookingRef, notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Booking Details");

        // Initialize UI components
        tvServiceType = findViewById(R.id.tvServiceType);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvRequirements = findViewById(R.id.tvRequirements);
        tvStatus = findViewById(R.id.tvStatus);

        // Initialize customer details TextViews
        tvCustomerName = findViewById(R.id.customerNameTV);
        tvPhoneNo = findViewById(R.id.customerPhoneTV);
        tvAdd = findViewById(R.id.customerAddTV);

        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);

        // Get booking ID from intent
        bookingId = getIntent().getStringExtra("bookingId");
        providerId = getIntent().getStringExtra("providerId");

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

        // Set up button click listeners
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBookingStatus("confirmed");
            }
        });

        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBookingStatus("rejected");
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
                    customerId = dataSnapshot.child("customerId").getValue(String.class);

                    // Get customer name from database
                    if (customerId != null) {
                        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                                .getReference("customers").child(customerId);

                        // Load customer name
                        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String name = snapshot.child("name").getValue(String.class);
                                    String phone = snapshot.child("phone").getValue(String.class);
                                    String address = snapshot.child("address").getValue(String.class);

                                    // Only update UI if TextViews are initialized
                                    if (tvCustomerName != null) {
                                        tvCustomerName.setText(name != null ? name : "N/A");
                                    }
                                    if (tvPhoneNo != null) {
                                        tvPhoneNo.setText(phone != null ? phone : "N/A");
                                    }
                                    if (tvAdd != null) {
                                        tvAdd.setText(address != null ? address : "N/A");
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(BookingDetailsActivity.this,
                                        "Failed to load customer details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Update UI
                    tvServiceType.setText(serviceType);
                    tvDate.setText(date);
                    tvTime.setText(time);
                    tvRequirements.setText(requirements);
                    tvStatus.setText(status);

                    // Update status text color based on status
                    updateStatusColor(status);

                    // Hide/show buttons based on status
                    if ("pending".equals(status)) {
                        btnAccept.setVisibility(View.VISIBLE);
                        btnReject.setVisibility(View.VISIBLE);
                    } else {
                        btnAccept.setVisibility(View.GONE);
                        btnReject.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(BookingDetailsActivity.this, "Booking no longer exists", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(BookingDetailsActivity.this, "Failed to load booking details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusColor(String status) {
        if (tvStatus != null) {
            int colorResId;
            switch (status) {
                case "confirmed":
                    colorResId = R.color.confirmed_color;
                    break;
                case "rejected":
                    colorResId = R.color.cancelled_color;
                    break;
                case "pending":
                default:
                    colorResId = R.color.pending_color;
                    break;
            }
            tvStatus.setTextColor(getResources().getColor(colorResId));
        }
    }

    private void updateBookingStatus(final String newStatus) {
        // Update booking status
        bookingRef.child("status").setValue(newStatus)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(BookingDetailsActivity.this,
                                    "Booking " + newStatus, Toast.LENGTH_SHORT).show();

                            // Create notification for customer
                            createNotification(customerId, newStatus);

                            // Hide buttons after action
                            btnAccept.setVisibility(View.GONE);
                            btnReject.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(BookingDetailsActivity.this,
                                    "Failed to update booking status", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createNotification(String userId, String status) {
        String notificationId = notificationsRef.push().getKey();

        if (notificationId != null) {
            String title = "Booking " + status.substring(0, 1).toUpperCase() + status.substring(1);
            String message = "Your booking request has been " + status + " by the service provider.";

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

    // Method to send popup notification
    private void sendPopupNotification(String userId, String title, String message) {
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
}