package com.example.servicehub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private String userId;
    private String userType;

    // View mode UI elements
    private TextView nameTextView;
    private TextView mobileTextView;
    private TextView addressTextView;
    private TextView emailTextView;
    private TextView accountTypeTextView;
    private TextView servicesTextView;
    private LinearLayout servicesSection;
    private LinearLayout viewModeLayout;

    // Edit mode UI elements
    private EditText nameEditText;
    private EditText mobileEditText;
    private EditText addressEditText;
    private LinearLayout editModeLayout;
    private Spinner servicesSpinner;
    private Button addServiceButton;
    private TextView selectedServicesTextView;
    private LinearLayout serviceEditSection;

    // Buttons
    private Button editButton;
    private Button saveButton;
    private Button cancelButton;

    // Service options
    private String[] serviceOptions = {"Select Service", "Room Cleaning", "Fridge Repairs", "Chef", "Pest Control", "Plumbing", "Electrical", "Window Cleaning", "Painting", "Gardening", "Moving", "AC Repairs", "Security"};
    private List<String> selectedServices = new ArrayList<>();

    // Mode flag
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        initializeUIElements();

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
            serviceEditSection.setVisibility(View.VISIBLE);
            setupServiceSpinner();
        } else {
            servicesSection.setVisibility(View.GONE);
            serviceEditSection.setVisibility(View.GONE);
        }

        // Set button click listeners
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToEditMode();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileChanges();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToViewMode();
                loadUserProfile(); // Reload the original data
            }
        });

        // Get user data from Firebase based on user type
        loadUserProfile();
    }

    private void initializeUIElements() {
        // View mode elements
        nameTextView = findViewById(R.id.textViewNameValue);
        mobileTextView = findViewById(R.id.textViewMobileValue);
        addressTextView = findViewById(R.id.textViewAddressValue);
        emailTextView = findViewById(R.id.textViewEmailValue);
        accountTypeTextView = findViewById(R.id.textViewAccountTypeValue);
        servicesTextView = findViewById(R.id.textViewServicesValue);
        servicesSection = findViewById(R.id.servicesSection);
        viewModeLayout = findViewById(R.id.viewModeLayout);

        // Edit mode elements
        nameEditText = findViewById(R.id.editTextName);
        mobileEditText = findViewById(R.id.editTextMobile);
        addressEditText = findViewById(R.id.editTextAddress);
        editModeLayout = findViewById(R.id.editModeLayout);
        servicesSpinner = findViewById(R.id.spinnerServices);
        addServiceButton = findViewById(R.id.buttonAddService);
        selectedServicesTextView = findViewById(R.id.textViewSelectedServices);
        serviceEditSection = findViewById(R.id.serviceEditSection);

        // Buttons
        editButton = findViewById(R.id.buttonEdit);
        saveButton = findViewById(R.id.buttonSave);
        cancelButton = findViewById(R.id.buttonCancel);

        // Initial mode is view mode
        viewModeLayout.setVisibility(View.VISIBLE);
        editModeLayout.setVisibility(View.GONE);
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
                    Toast.makeText(ProfileActivity.this,
                            "Please select a valid service", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this,
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

    private void switchToEditMode() {
        isEditMode = true;

        // Populate edit fields with current values
        nameEditText.setText(nameTextView.getText());
        mobileEditText.setText(mobileTextView.getText());
        addressEditText.setText(addressTextView.getText());

        // Switch layouts
        viewModeLayout.setVisibility(View.GONE);
        editModeLayout.setVisibility(View.VISIBLE);
    }

    private void switchToViewMode() {
        isEditMode = false;

        // Switch layouts
        editModeLayout.setVisibility(View.GONE);
        viewModeLayout.setVisibility(View.VISIBLE);
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

                        // Clear previous services
                        selectedServices.clear();

                        // Check if services node exists
                        if (dataSnapshot.hasChild("services")) {
                            try {
                                // Iterate through all children of the "services" node
                                for (DataSnapshot serviceSnapshot : dataSnapshot.child("services").getChildren()) {
                                    String service = serviceSnapshot.getValue(String.class);
                                    Log.d(TAG, "Found service: " + service);
                                    if (service != null) {
                                        selectedServices.add(service);
                                    }
                                }

                                Log.d(TAG, "Services count: " + selectedServices.size());

                                if (!selectedServices.isEmpty()) {
                                    // Convert the services list to a comma-separated string
                                    StringBuilder servicesBuilder = new StringBuilder();
                                    for (int i = 0; i < selectedServices.size(); i++) {
                                        servicesBuilder.append(selectedServices.get(i));
                                        if (i < selectedServices.size() - 1) {
                                            servicesBuilder.append(", ");
                                        }
                                    }

                                    // Set the text to the services
                                    servicesTextView.setText(servicesBuilder.toString());
                                    updateSelectedServicesText();
                                } else {
                                    servicesTextView.setText("No services listed");
                                    selectedServicesTextView.setText("Selected Services: None");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing services: ", e);
                                servicesTextView.setText("Error loading services");
                            }
                        } else {
                            Log.d(TAG, "No 'services' node found in the data");
                            servicesTextView.setText("No services listed");
                            selectedServicesTextView.setText("Selected Services: None");
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

    private void saveProfileChanges() {
        // Validate input
        String name = nameEditText.getText().toString().trim();
        String mobile = mobileEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();

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

        // For service providers, validate that at least one service is selected
        if (userType.equals("service_provider") && selectedServices.isEmpty()) {
            Toast.makeText(ProfileActivity.this,
                    "Please select at least one service", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map of updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("mobile", mobile);
        updates.put("address", address);

        // Reference the correct node based on user type
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef;

        if (userType.equals("customer")) {
            userRef = database.getReference("customers").child(userId);
        } else {
            userRef = database.getReference("service_providers").child(userId);
        }

        // Update the basic info
        userRef.updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // If service provider, update services
                    if (userType.equals("service_provider")) {
                        // First remove all existing services
                        userRef.child("services").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Then add the new services
                                    DatabaseReference servicesRef = userRef.child("services");
                                    for (int i = 0; i < selectedServices.size(); i++) {
                                        servicesRef.child(String.valueOf(i)).setValue(selectedServices.get(i));
                                    }

                                    // Also update the services in the service categories
                                    updateServiceCategories(database, name, mobile, address);
                                }
                            }
                        });
                    } else {
                        finalizeSave();
                    }
                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Failed to update profile: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateServiceCategories(FirebaseDatabase database, String name, String mobile, String address) {
        // Get user information
        FirebaseDatabase.getInstance().getReference("service_providers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Create updated user map for service categories
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("mobile", mobile);
                            userMap.put("address", address);
                            userMap.put("email", dataSnapshot.child("email").getValue(String.class));
                            userMap.put("authUid", dataSnapshot.child("authUid").getValue(String.class));
                            userMap.put("userType", "service_provider");
                            userMap.put("userId", userId);

                            // First, remove user from all service categories
                            DatabaseReference serviceCategories = database.getReference("service_categories");
                            for (String service : serviceOptions) {
                                if (!service.equals("Select Service")) {
                                    serviceCategories.child(service.toLowerCase().replace(" ", "_"))
                                            .child(userId).removeValue();
                                }
                            }

                            // Then add user to selected service categories
                            for (String service : selectedServices) {
                                serviceCategories.child(service.toLowerCase().replace(" ", "_"))
                                        .child(userId).setValue(userMap);
                            }

                            finalizeSave();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ProfileActivity.this,
                                "Failed to update service categories: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void finalizeSave() {
        // Switch back to view mode
        switchToViewMode();

        // Reload the profile to show updated data
        loadUserProfile();

        Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}