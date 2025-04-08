package com.example.realtimedb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class UserTypeSelectionActivity extends AppCompatActivity {

    private Button customerButton;
    private Button serviceProviderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_type_selection);

        // Initialize UI elements
        customerButton = findViewById(R.id.buttonCustomer);
        serviceProviderButton = findViewById(R.id.buttonServiceProvider);

        // Set up customer button
        customerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToRegistration("customer");
            }
        });

        // Set up service provider button
        serviceProviderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToRegistration("service_provider");
            }
        });
    }

    private void navigateToRegistration(String userType) {
        Intent intent = new Intent(UserTypeSelectionActivity.this, RegistrationActivity.class);
        intent.putExtra("userType", userType);
        startActivity(intent);
    }
}