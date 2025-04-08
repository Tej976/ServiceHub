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
    private TextView welcomeTextView;
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
        viewProfileButton = findViewById(R.id.buttonViewProfile);
        logoutButton = findViewById(R.id.buttonLogout);

        // Get userId from intent
        userId = getIntent().getStringExtra("userId");

        // If userId is null, check if user is authenticated, otherwise redirect to login
        if (userId == null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // Try to find user ID in database
                findUserIdByAuthUid(currentUser.getUid());
            } else {
                redirectToLogin();
                return;
            }
        } else {
            loadUserData();
        }

        // Set up view profile button
        viewProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userId != null) {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "User ID not available", Toast.LENGTH_SHORT).show();
                }
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

    private void findUserIdByAuthUid(String authUid) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("authUid").equalTo(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the first matching user
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        userId = userSnapshot.getKey();
                        loadUserData();
                        break;
                    }
                } else {
                    // User exists in Auth but not in Database - unusual case
                    Toast.makeText(MainActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        });
    }

    private void loadUserData() {
        // Get user's name from database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef = database.getReference("users").child(userId);

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