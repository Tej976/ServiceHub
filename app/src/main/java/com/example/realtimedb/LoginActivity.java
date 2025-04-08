package com.example.realtimedb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        emailEditText = findViewById(R.id.editTextLoginEmail);
        passwordEditText = findViewById(R.id.editTextLoginPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerTextView = findViewById(R.id.textViewRegister);

        // Set up login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Validate input
                if (email.isEmpty()) {
                    emailEditText.setError("Email is required");
                    return;
                }
                if (password.isEmpty()) {
                    passwordEditText.setError("Password is required");
                    return;
                }

                // Perform login
                loginUser(email, password);
            }
        });

        // Set up register text view
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to registration screen
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            redirectToMainActivity(currentUser.getUid());
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Login successful!",
                                    Toast.LENGTH_SHORT).show();

                            if (user != null) {
                                redirectToMainActivity(user.getUid());
                            }
                        } else {
                            // If sign in fails, display a message to the user
                            Toast.makeText(LoginActivity.this, "Authentication failed: "
                                    + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void redirectToMainActivity(String uid) {
        // Get the userId from the Realtime Database using the Firebase Auth UID
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("authUid").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the first matching user
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        break;
                    }
                } else {
                    // User exists in Firebase Auth but not in Database - unusual case
                    Toast.makeText(LoginActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}