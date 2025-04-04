package com.example.realtimedb;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private String userId;
    private TextView nameTextView;
    private TextView mobileTextView;
    private TextView addressTextView;
    private TextView emailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        nameTextView = findViewById(R.id.textViewNameValue);
        mobileTextView = findViewById(R.id.textViewMobileValue);
        addressTextView = findViewById(R.id.textViewAddressValue);
        emailTextView = findViewById(R.id.textViewEmailValue);

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get user data from Firebase
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef = database.getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get user data
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        // Display user info except password
                        nameTextView.setText(user.getName());
                        mobileTextView.setText(user.getMobile());
                        addressTextView.setText(user.getAddress());
                        emailTextView.setText(user.getEmail());
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