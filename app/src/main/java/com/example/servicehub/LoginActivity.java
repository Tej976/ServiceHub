package com.example.servicehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

        // Check if this is a fresh installation (first launch)
        SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstRun = preferences.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            // This is a fresh installation, mark it as no longer first run
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstRun", false);
            editor.apply();
        }

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

        // Set up register text view - Direct to UserTypeSelectionActivity
        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to user type selection screen
                Intent intent = new Intent(LoginActivity.this, UserTypeSelectionActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            // Check if user is already signed in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // Check user type and redirect accordingly
                checkUserTypeAndRedirect(currentUser.getUid());
            }
        } catch (Exception e) {
            // Log the error
            Log.e("LoginActivity", "Error in onStart: " + e.getMessage());
            Toast.makeText(this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            if (user != null) {
                                checkUserTypeAndRedirect(user.getUid());
                            }
                        } else {
                            // If sign in fails, display a message to the user
                            Toast.makeText(LoginActivity.this, "Authentication failed: "
                                    + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkUserTypeAndRedirect(String authUid) {
        // First check customers node
        DatabaseReference customersRef = FirebaseDatabase.getInstance().getReference("customers");
        customersRef.orderByChild("authUid").equalTo(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Found in customers
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        redirectToMainActivity(userId, "customer");
                        return;
                    }
                } else {
                    // Not found in customers, check service providers
                    checkServiceProviderAndRedirect(authUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkServiceProviderAndRedirect(String authUid) {
        DatabaseReference serviceProvidersRef = FirebaseDatabase.getInstance().getReference("service_providers");
        serviceProvidersRef.orderByChild("authUid").equalTo(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Found in service providers
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        redirectToMainActivity(userId, "service_provider");
                        return;
                    }
                } else {
                    // User not found in either node
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

    private void redirectToMainActivity(String userId, String userType) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userType", userType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}