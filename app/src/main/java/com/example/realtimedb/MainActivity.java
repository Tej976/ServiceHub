package com.example.realtimedb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private String userId;
    private String userType;
    private TextView welcomeTextView;
    private TextView userTypeTextView;
    private Button viewProfileButton;
    private Button logoutButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        welcomeTextView = findViewById(R.id.textViewWelcome);
        userTypeTextView = findViewById(R.id.textViewUserType);
        viewProfileButton = findViewById(R.id.buttonViewProfile);
        logoutButton = findViewById(R.id.buttonLogout);

        // Get userId and userType from intent
        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        // If userId or userType is null, redirect to login
        if (userId == null || userType == null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is authenticated but we don't have userId or userType - log out and redirect
                mAuth.signOut();
            }
            redirectToLogin();
            return;
        }

        // Display user type
        String displayType = userType.equals("customer") ? "Customer" : "Service Provider";
        userTypeTextView.setText("Account Type: " + displayType);

        // Load user data based on user type
        loadUserData();

        // Set up view profile button
        viewProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("userType", userType);
                startActivity(intent);
            }
        });

        // Set up logout button
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sign out from Firebase Auth
                mAuth.signOut();
                redirectToLogin();
            }
        });
    }

    private void loadUserData() {
        // Get user's name from database based on user type
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef;

        if (userType.equals("customer")) {
            userRef = database.getReference("customers").child(userId);
        } else {
            userRef = database.getReference("service_providers").child(userId);
        }

        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.getValue(String.class);
                    welcomeTextView.setText("Welcome, " + name + "!");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is still authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        }
    }
}