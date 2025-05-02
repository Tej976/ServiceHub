package com.example.servicehub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicehub.booking.MyBookingsActivity;
import com.example.servicehub.notifications.NotificationsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class AddActivity extends AppCompatActivity {
    private RecyclerView recyclerView;                                        // Counter to keep track of the number of cards added
    private final String DEFAULT_EMAIL_ADDRESS = "tejaswininikam642004@gmail.com";     // Default email address
    private EditText serviceNameInput;
    private EditText serviceDescription;

    private FirebaseAuth mAuth;
    private String userId;
    private String userType;
    private String providerEmail;
    private String providerName;
    private TabLayout tabLayout;
    private BottomNavigationView bottomNavigationView;

    // onCreate method is called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                                    // Call the superclass's onCreate method
        setContentView(R.layout.activity_add);                                 // Set the content view to the activity_main layout

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add New Service");

        serviceNameInput = findViewById(R.id.serviceNameInput);
        serviceDescription = findViewById(R.id.serviceDescription);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get current user information
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            providerEmail = currentUser.getEmail();
        }

        // Get userId and userType from intent or SharedPreferences
        // If passing from MainActivity, you can get them like this:
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        userType = intent.getStringExtra("userType");

        // If userId is available, fetch additional provider info
        if (userId != null && "service_provider".equals(userType)) {
            fetchProviderInfo();
        }

        // Find the button that adds new cards
        Button addCardButton = findViewById(R.id.addCardButton);

        // Set an OnClickListener on the button to add a new card and send an email when clicked
        addCardButton.setOnClickListener(v -> {
            sendEmail(serviceNameInput, serviceDescription);
        });


        // Initialize bottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Check if bottomNavigationView exists in layout
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
            bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

            // FIX: Set the selected item to the add navigation item when this activity starts
            bottomNavigationView.setSelectedItemId(R.id.nav_bottom_add);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX: Ensure the add tab stays selected when returning to this activity
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_bottom_add);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.nav_bottom_home) {
                        Intent intent = new Intent(AddActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    else if (id == R.id.nav_bottom_bookings) {
                        Intent intent = new Intent(AddActivity.this, MyBookingsActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    else if (id == R.id.nav_bottom_add) {
                        // Already on Add screen, do nothing but keep it selected
                        return true;
                    }
                    else if (id == R.id.nav_bottom_notifications) {
                        Intent intent = new Intent(AddActivity.this, NotificationsActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            };

    // Fetch service provider information from Firebase
    private void fetchProviderInfo() {
        DatabaseReference providerRef = FirebaseDatabase.getInstance()
                .getReference("service_providers")
                .child(userId);

        providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get provider name
                    if (dataSnapshot.child("name").exists()) {
                        providerName = dataSnapshot.child("name").getValue(String.class);
                    }

                    // Get provider email if not already available from Firebase Auth
                    if (providerEmail == null && dataSnapshot.child("email").exists()) {
                        providerEmail = dataSnapshot.child("email").getValue(String.class);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddActivity.this,
                        "Failed to load provider data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to send an email using an implicit intent
    private void sendEmail(EditText serviceNameInput, EditText serviceDescription) {
        try {
            if (providerEmail == null) {
                Toast.makeText(this, "Service provider email not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try a more universal approach to email intent
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");                                                // Standard email MIME type
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{DEFAULT_EMAIL_ADDRESS});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Request to add a New Service from ServiceHub");

            String name = serviceNameInput.getText().toString();
            String sname = serviceDescription.getText().toString();

            String messageBody = "A new request to add a new service "+name+".\n\n";
            messageBody +=  "The new service will provide "+sname+",\n\n";
            if (providerName != null) {
                messageBody += "Service Provider: " + providerName + "\n";
            }
            messageBody += "From: " + providerEmail;

            emailIntent.putExtra(Intent.EXTRA_TEXT, messageBody);

            // Try to show a chooser of email apps
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email using..."));
                Toast.makeText(this, "Please select an email client", Toast.LENGTH_SHORT).show();
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email clients installed on this device", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sending email: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}