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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        // Create booking object
        Map<String, Object> booking = new HashMap<>();
        booking.put("customerId", userId);
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}