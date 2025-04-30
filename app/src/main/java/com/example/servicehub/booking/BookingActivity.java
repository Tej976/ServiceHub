package com.example.servicehub.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.servicehub.MainActivity;
import com.example.servicehub.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BookingActivity extends AppCompatActivity {

    private TextView tvProviderName, tvProviderMobile, tvProviderAddress, tvServiceType;
    private EditText etRequirements;
    private Button btnSelectDate, btnSelectTime, btnConfirmBooking;
    private String selectedDate = "";
    private String selectedTime = "";
    private Calendar calendar;
    private String providerId, serviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Book Service");

        // Initialize calendar
        calendar = Calendar.getInstance();

        // Initialize UI components
        tvProviderName = findViewById(R.id.tvProviderName);
        tvProviderMobile = findViewById(R.id.tvProviderMobile);
        tvProviderAddress = findViewById(R.id.tvProviderAddress);
        tvServiceType = findViewById(R.id.tvServiceType);
        etRequirements = findViewById(R.id.etRequirements);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // Get data from intent
        String providerName = getIntent().getStringExtra("providerName");
        String providerMobile = getIntent().getStringExtra("providerMobile");
        String providerAddress = getIntent().getStringExtra("providerAddress");
        providerId = getIntent().getStringExtra("providerId");
        serviceName = getIntent().getStringExtra("serviceName");

        // Set text views with data
        tvProviderName.setText(providerName);
        tvProviderMobile.setText("Phone: " + providerMobile);
        tvProviderAddress.setText("Address: " + providerAddress);
        tvServiceType.setText("Service: " + serviceName);

        // Set up date picker
        btnSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Set up time picker
        btnSelectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        // Set up confirm booking button
        btnConfirmBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmBooking();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                selectedDate = dateFormat.format(calendar.getTime());
                btnSelectDate.setText("Date: " + selectedDate);
            }
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                selectedTime = timeFormat.format(calendar.getTime());
                btnSelectTime.setText("Time: " + selectedTime);
            }
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), false
        );
        timePickerDialog.show();
    }

    private void confirmBooking() {
        String requirements = etRequirements.getText().toString().trim();

        // Validate inputs
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user ID (if authenticated)
        String userId = "guest";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // First fetch customer details to include in booking
        fetchCustomerDetails(userId, requirements);
    }

    private void fetchCustomerDetails(final String userId, final String requirements) {
        // Only proceed if we have a valid user ID
        if ("guest".equals(userId)) {
            Toast.makeText(this, "You must be logged in to book a service", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference("customers").child(userId);

        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get customer name and phone
                    String customerName = dataSnapshot.child("name").getValue(String.class);
                    String customerPhone = dataSnapshot.child("phone").getValue(String.class);
                    String customerAddress = dataSnapshot.child("address").getValue(String.class);

                    // Create booking with customer details
                    createBooking(userId, requirements, customerName, customerPhone, customerAddress);
                } else {
                    // No customer profile found - create booking without customer details
                    createBooking(userId, requirements, "", "", "");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(BookingActivity.this, "Failed to load customer profile: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createBooking(String userId, String requirements, String customerName, String customerPhone, String customerAddress) {
        // Create booking object
        Map<String, Object> booking = new HashMap<>();
        booking.put("customerId", userId);
        booking.put("customerName", customerName); // Add customer name
        booking.put("customerPhone", customerPhone); // Add customer phone
        booking.put("customerAddress", customerAddress); // Add customer address
        booking.put("requirements", requirements);
        booking.put("providerId", providerId);
        booking.put("providerName", tvProviderName.getText().toString());
        booking.put("serviceType", serviceName);
        booking.put("date", selectedDate);
        booking.put("time", selectedTime);
        booking.put("status", "pending");
        booking.put("bookingTime", System.currentTimeMillis());

        // Generate a unique booking ID
        String bookingId = UUID.randomUUID().toString();

        // Save to Firebase
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("bookings");

        bookingsRef.child(bookingId).setValue(booking).addOnSuccessListener(aVoid -> {
            Toast.makeText(BookingActivity.this, "Booking confirmed successfully!", Toast.LENGTH_LONG).show();

            // Create notification for service provider
            createNotification(providerId, "New Booking Request",
                    "You have a new booking request for " + serviceName, bookingId);

            // Navigate to MainActivity
            Intent intent = new Intent(BookingActivity.this, MainActivity.class);
            // Add these lines to pass the necessary user information
            String Id = FirebaseAuth.getInstance().getCurrentUser().getUid();
            intent.putExtra("userId", Id);
            // You'll need to determine the userType - this could be stored in SharedPreferences
            // or retrieved from Firebase. For now, I'm assuming "customer" since they're booking
            intent.putExtra("userType", "customer");
            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {
            Toast.makeText(BookingActivity.this, "Failed to book: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    // Method to create notification for service provider
    private void createNotification(String userId, String title, String message, String bookingId) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications");

        String notificationId = notificationsRef.push().getKey();

        if (notificationId != null) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("bookingId", bookingId);
            notification.put("read", false);

            notificationsRef.child(notificationId).setValue(notification);

            // Also send popup notification
            sendPopupNotification(userId, title, message, bookingId);
        }
    }

    // Method to send popup notification
    private void sendPopupNotification(String userId, String title, String message, String bookingId) {
        DatabaseReference popupNotificationsRef = FirebaseDatabase.getInstance()
                .getReference("popup_notifications").child(userId);

        String notificationId = popupNotificationsRef.push().getKey();

        if (notificationId != null) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("bookingId", bookingId);
            notification.put("read", false);

            popupNotificationsRef.child(notificationId).setValue(notification);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}