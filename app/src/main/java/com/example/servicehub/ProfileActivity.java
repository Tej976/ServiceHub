package com.example.servicehub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.servicehub.constructors.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private String userId;
    private String userType;
    private TextView nameTextView;
    private TextView mobileTextView;
    private TextView addressTextView;
    private TextView emailTextView;
    private TextView accountTypeTextView;
    private TextView servicesTextView;
    private LinearLayout servicesSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        nameTextView = findViewById(R.id.textViewNameValue);
        mobileTextView = findViewById(R.id.textViewMobileValue);
        addressTextView = findViewById(R.id.textViewAddressValue);
        emailTextView = findViewById(R.id.textViewEmailValue);
        accountTypeTextView = findViewById(R.id.textViewAccountTypeValue);
        servicesTextView = findViewById(R.id.textViewServicesValue);
        servicesSection = findViewById(R.id.servicesSection);

        // Get userId and userType from intent
        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        if (userId == null || userType == null) {
            Toast.makeText(this, "User information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set account type
        String displayType = userType.equals("customer") ? "Customer" : "Service Provider";
        accountTypeTextView.setText(displayType);

        // Show services section only for service providers
        if (userType.equals("service_provider")) {
            servicesSection.setVisibility(View.VISIBLE);
        } else {
            servicesSection.setVisibility(View.GONE);
        }

        // Get user data from Firebase based on user type
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef;

        // Reference the correct node based on user type
        if (userType.equals("customer")) {
            userRef = database.getReference("customers").child(userId);
        } else {
            userRef = database.getReference("service_providers").child(userId);
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Raw data: " + dataSnapshot.toString());

                    // Display basic user info
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String mobile = dataSnapshot.child("mobile").getValue(String.class);
                    String address = dataSnapshot.child("address").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);

                    // Set text views
                    nameTextView.setText(name);
                    mobileTextView.setText(mobile);
                    addressTextView.setText(address);
                    emailTextView.setText(email);

                    // Display services for service providers
                    if (userType.equals("service_provider")) {
                        Log.d(TAG, "Service provider detected");

                        // Check if services node exists
                        if (dataSnapshot.hasChild("services")) {
                            try {
                                // Get services as list
                                List<String> servicesList = new ArrayList<>();

                                // Iterate through all children of the "services" node
                                for (DataSnapshot serviceSnapshot : dataSnapshot.child("services").getChildren()) {
                                    String service = serviceSnapshot.getValue(String.class);
                                    Log.d(TAG, "Found service: " + service);
                                    servicesList.add(service);
                                }

                                Log.d(TAG, "Services count: " + servicesList.size());

                                if (!servicesList.isEmpty()) {
                                    // Convert the services list to a comma-separated string
                                    StringBuilder servicesBuilder = new StringBuilder();
                                    for (int i = 0; i < servicesList.size(); i++) {
                                        servicesBuilder.append(servicesList.get(i));
                                        if (i < servicesList.size() - 1) {
                                            servicesBuilder.append(", ");
                                        }
                                    }

                                    // Set the text to the services
                                    servicesTextView.setText(servicesBuilder.toString());
                                } else {
                                    servicesTextView.setText("No services listed");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing services: ", e);
                                servicesTextView.setText("Error loading services");
                            }
                        } else {
                            Log.d(TAG, "No 'services' node found in the data");
                            servicesTextView.setText("No services listed");
                        }
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}