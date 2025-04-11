package com.example.servicehub;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

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
                    // Get user data
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        // Display user info
                        nameTextView.setText(user.getName());
                        mobileTextView.setText(user.getMobile());
                        addressTextView.setText(user.getAddress());
                        emailTextView.setText(user.getEmail());

                        // Display services for service providers
                        if (userType.equals("service_provider") && user.getServices() != null) {
                            List<String> services = user.getServices();
                            if (services.isEmpty()) {
                                servicesTextView.setText("No services selected");
                            } else {
                                StringBuilder servicesText = new StringBuilder();
                                for (int i = 0; i < services.size(); i++) {
                                    servicesText.append(services.get(i));
                                    if (i < services.size() - 1) {
                                        servicesText.append(", ");
                                    }
                                }
                                servicesTextView.setText(servicesText.toString());
                            }
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