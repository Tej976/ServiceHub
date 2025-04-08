package com.example.realtimedb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser ;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RegistrationActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText mobileEditText;
    private EditText addressEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private TextView userTypeTextView;
    private LinearLayout servicesLayout;

    // Service provider specific UI elements
    private Spinner servicesSpinner;
    private Button addServiceButton;
    private TextView selectedServicesTextView;

    // Service options
    private String[] serviceOptions = {"Select Service", "Cleaning", "Plumbing", "Electrical", "Carpentry", "Gardening", "Painting"};
    private List<String> selectedServices = new ArrayList<>();

    private FirebaseAuth mAuth;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // Get user type from intent
        userType = getIntent().getStringExtra("userType");
        if (userType == null) {
            Toast.makeText(this, "User  type not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        nameEditText = findViewById(R.id.editTextName);
        mobileEditText = findViewById(R.id.editTextMobile);
        addressEditText = findViewById(R.id.editTextAddress);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        registerButton = findViewById(R.id.registerButton);
        userTypeTextView = findViewById(R.id.textViewUserType);
        servicesLayout = findViewById(R.id.servicesLayout);

        // Initialize service provider specific UI elements
        servicesSpinner = findViewById(R.id.spinnerServices);
        addServiceButton = findViewById(R.id.buttonAddService);
        selectedServicesTextView = findViewById(R.id.textViewSelectedServices);

        // Set user type text
        String displayType = userType.equals("customer") ? "Customer" : "Service Provider";
        userTypeTextView.setText("Register as " + displayType);

        // Setup spinner and service selection (only for service providers)
        if (userType.equals("service_provider")) {
            servicesLayout.setVisibility(View.VISIBLE);
            setupServiceSpinner();
        } else {
            servicesLayout.setVisibility(View.GONE);
        }

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

                // For service providers, validate that at least one service is selected
                if (userType.equals("service_provider") && selectedServices.isEmpty()) {
                    Toast.makeText(RegistrationActivity.this,
                            "Please select at least one service", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Register user with Firebase Authentication
                createUserWithFirebaseAuth(name, mobile, address, email, password);
            }
        });
    }

    private void setupServiceSpinner() {
        // Create adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, serviceOptions);

        // Set the adapter to the spinner
        servicesSpinner.setAdapter(adapter);

        // Add button click listener to add selected service
        addServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedService = servicesSpinner.getSelectedItem().toString();

                // Check if the selected service is valid (not the placeholder)
                if (!selectedService.equals("Select Service") && !selectedServices.contains(selectedService)) {
                    selectedServices.add(selectedService);
                    updateSelectedServicesText();
                } else if (selectedService.equals("Select Service")) {
                    Toast.makeText(RegistrationActivity.this,
                            "Please select a valid service", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegistrationActivity.this,
                            "Service already selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateSelectedServicesText() {
        if (selectedServices.isEmpty()) {
            selectedServicesTextView.setText("Selected Services: None");
        } else {
            StringBuilder servicesText = new StringBuilder("Selected Services: ");
            for (int i = 0; i < selectedServices.size(); i++) {
                servicesText.append(selectedServices.get(i));
                if (i < selectedServices.size() - 1) {
                    servicesText.append(", ");
                }
            }
            selectedServicesTextView.setText(servicesText.toString());
        }
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
                                // Now save additional user info to Realtime Database based on user type
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
        userMap.put("userType", userType);

        // Add services list for service providers
        if (userType.equals("service_provider")) {
            userMap.put("services", selectedServices);
        }

        // Get Firebase reference based on user type
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef;

        if (userType.equals("customer")) {
            usersRef = database.getReference("customers");
        } else {
            usersRef = database.getReference("service_providers");
        }

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
                    selectedServices.clear();
                    updateSelectedServicesText();

                    // Start MainActivity with userId and userType
                    Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("userType", userType);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegistrationActivity.this, "Database registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}