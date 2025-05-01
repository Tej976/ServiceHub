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

        // Set default text for customer details
        tvCustomerName.setText("Loading customer details...");
        tvPhoneNo.setText("Loading...");
        tvAdd.setText("Loading...");

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

                    // FIX: Check for customerId first since that's the expected field name
                    customerId = dataSnapshot.child("customerId").getValue(String.class);

                    // If customerId is null, check for alternative field names
                    if (customerId == null) {
                        // Try userId as fallback
                        customerId = dataSnapshot.child("userId").getValue(String.class);

                        // Log the search for debugging
                        System.out.println("DEBUG: Looked for customerId first (null), then found userId: " + customerId);
                    } else {
                        System.out.println("DEBUG: Found customerId: " + customerId);
                    }

                    // Update booking details UI
                    tvServiceType.setText(serviceType != null ? serviceType : "N/A");
                    tvDate.setText(date != null ? date : "N/A");
                    tvTime.setText(time != null ? time : "N/A");
                    tvRequirements.setText(requirements != null ? requirements : "N/A");
                    tvStatus.setText(status != null ? status : "N/A");

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

                    // Get customer details from database
                    if (customerId != null) {
                        loadCustomerDetails(customerId);
                    } else {
                        // If customerId is null, display a message
                        tvCustomerName.setText("Customer info not available");
                        tvPhoneNo.setText("N/A");
                        tvAdd.setText("N/A");

                        // Log the error
                        System.out.println("ERROR: Customer ID is null in booking data");
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

    private void loadCustomerDetails(String customerId) {
        // Log the customerId we're trying to load
        System.out.println("DEBUG: Loading customer details for ID: " + customerId);

        // Get reference to the customers node in Firebase
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference("customers").child(customerId);

        // Debug logging
        System.out.println("DEBUG: Firebase path: " + customerRef.toString());

        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Debug log whether we found data
                System.out.println("DEBUG: Customer data exists: " + snapshot.exists());

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("mobile").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);

                    // Log data to verify what's being retrieved
                    System.out.println("Customer Data - Name: " + name + ", Phone: " + phone + ", Address: " + address);

                    // Update UI with customer details - checking for null before setting
                    tvCustomerName.setText(name != null && !name.isEmpty() ? name : "N/A");
                    tvPhoneNo.setText(phone != null && !phone.isEmpty() ? phone : "N/A");
                    tvAdd.setText(address != null && !address.isEmpty() ? address : "N/A");
                } else {
                    // Try to load customer details from auth ID as fallback
                    loadCustomerDetailsByAuthId(customerId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookingDetailsActivity.this,
                        "Failed to load customer details", Toast.LENGTH_SHORT).show();

                // Set error text in case of database error
                tvCustomerName.setText("Error loading customer");
                tvPhoneNo.setText("Error");
                tvAdd.setText("Error");

                // Log the error details
                System.out.println("ERROR: Database error while loading customer: " + error.getMessage());
            }
        });
    }

    // Fallback method - Try to get customer by authUid field
    private void loadCustomerDetailsByAuthId(String userId) {
        System.out.println("DEBUG: Trying to load customer by authUid reference");

        // Query the entire customers collection for any customer with this authUid
        DatabaseReference allCustomersRef = FirebaseDatabase.getInstance().getReference("customers");

        allCustomersRef.orderByChild("authUid").equalTo(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            // Get the first matching customer
                            for (DataSnapshot customerSnapshot : dataSnapshot.getChildren()) {
                                String name = customerSnapshot.child("name").getValue(String.class);
                                String phone = customerSnapshot.child("mobile").getValue(String.class);
                                String address = customerSnapshot.child("address").getValue(String.class);

                                // Log that we found the customer via this alternate method
                                System.out.println("Found customer via authUid query: " + name);

                                // Update UI with customer details
                                tvCustomerName.setText(name != null && !name.isEmpty() ? name : "N/A");
                                tvPhoneNo.setText(phone != null && !phone.isEmpty() ? phone : "N/A");
                                tvAdd.setText(address != null && !address.isEmpty() ? address : "N/A");

                                // Only process the first matching customer
                                break;
                            }
                        } else {
                            // If customer data doesn't exist with either method
                            tvCustomerName.setText("Customer not found");
                            tvPhoneNo.setText("N/A");
                            tvAdd.setText("N/A");

                            System.out.println("ERROR: Customer not found by either userId or authUid");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // On failure
                        tvCustomerName.setText("Error loading customer");
                        tvPhoneNo.setText("Error");
                        tvAdd.setText("Error");

                        System.out.println("ERROR: Failed authUid lookup: " + error.getMessage());
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