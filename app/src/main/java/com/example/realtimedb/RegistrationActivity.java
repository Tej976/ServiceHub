package com.example.realtimedb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegistrationActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText mobileEditText;
    private EditText addressEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        nameEditText = findViewById(R.id.editTextName);
        mobileEditText = findViewById(R.id.editTextMobile);
        addressEditText = findViewById(R.id.editTextAddress);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get input values
                String name = nameEditText.getText().toString().trim();
                String mobile = mobileEditText.getText().toString().trim();
                String address = addressEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Validate input
                if (name.isEmpty()) {
                    nameEditText.setError("Name is required");
                    return;
                }
                if (mobile.isEmpty()) {
                    mobileEditText.setError("Mobile number is required");
                    return;
                }
                if (address.isEmpty()) {
                    addressEditText.setError("Address is required");
                    return;
                }
                if (email.isEmpty()) {
                    emailEditText.setError("Email is required");
                    return;
                }
                if (password.isEmpty()) {
                    passwordEditText.setError("Password is required");
                    return;
                }

                // Password strength check
                if (password.length() < 6) {
                    passwordEditText.setError("Password must be at least 6 characters long");
                    return;
                }

                // Register user with Firebase Authentication
                createUserWithFirebaseAuth(name, mobile, address, email, password);
            }
        });
    }

    private void createUserWithFirebaseAuth(final String name, final String mobile,
                                            final String address, final String email, final String password) {
        // Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, get the newly created user
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();

                            if (firebaseUser != null) {
                                // Now save additional user info to Realtime Database
                                saveUserToDatabase(firebaseUser.getUid(), name, mobile, address, email);
                            }
                        } else {
                            // If sign in fails, display a message to the user
                            Toast.makeText(RegistrationActivity.this, "Authentication failed: "
                                    + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserToDatabase(String authUid, String name, String mobile, String address, String email) {
        // Create user object
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("mobile", mobile);
        userMap.put("address", address);
        userMap.put("email", email);
        userMap.put("authUid", authUid);  // Store Firebase Auth UID to link accounts

        // Get Firebase reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

        // Generate unique database ID
        String userId = usersRef.push().getKey();
        userMap.put("userId", userId);

        // Save user to database
        usersRef.child(userId).setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Clear fields
                    nameEditText.getText().clear();
                    mobileEditText.getText().clear();
                    addressEditText.getText().clear();
                    emailEditText.getText().clear();
                    passwordEditText.getText().clear();

                    // Start MainActivity with userId
                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegistrationActivity.this, "Database registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}