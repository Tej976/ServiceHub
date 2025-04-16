package com.example.servicehub;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceProvidersActivity extends AppCompatActivity {

    private ListView serviceProviderListView;
    private List<Map<String, String>> providersList;
    private SimpleAdapter adapter;
    private TextView titleTextView;
    private String serviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_providers);

        // Get service name from intent
        serviceName = getIntent().getStringExtra("serviceName");
        if (serviceName == null || serviceName.isEmpty()) {
            Toast.makeText(this, "Service not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        serviceProviderListView = findViewById(R.id.listViewServiceProviders);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(serviceName + " Services");
        }

        // Set activity title
        titleTextView.setText(serviceName + " Service Providers");

        // Initialize the providers list and adapter
        providersList = new ArrayList<>();
        adapter = new SimpleAdapter(
                this,
                providersList,
                R.layout.service_provider_item,
                new String[]{"name", "mobile", "address"},
                new int[]{R.id.textViewProviderName, R.id.textViewProviderMobile, R.id.textViewProviderAddress}
        );

        serviceProviderListView.setAdapter(adapter);

        // Load service providers for this service
        loadServiceProviders();
    }

    private void loadServiceProviders() {
        // Reference to the specific service category in Firebase
        // Convert service name to lowercase for consistent database references
        String serviceKey = serviceName.toLowerCase();

        DatabaseReference serviceRef = FirebaseDatabase.getInstance()
                .getReference("service_categories")
                .child(serviceKey);

        serviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                providersList.clear();

                // Iterate through all providers in this category
                for (DataSnapshot providerSnapshot : dataSnapshot.getChildren()) {
                    // Get provider details
                    String name = providerSnapshot.child("name").getValue(String.class);
                    String mobile = providerSnapshot.child("mobile").getValue(String.class);
                    String address = providerSnapshot.child("address").getValue(String.class);

                    if (name != null && mobile != null && address != null) {
                        // Create a map for this provider
                        Map<String, String> providerMap = new HashMap<>();
                        providerMap.put("name", name);
                        providerMap.put("mobile", mobile);
                        providerMap.put("address", address);

                        // Add to the list
                        providersList.add(providerMap);
                    }
                }

                // Update the adapter
                adapter.notifyDataSetChanged();

                // Show message if no providers found
                if (providersList.isEmpty()) {
                    Toast.makeText(ServiceProvidersActivity.this,
                            "No " + serviceName.toLowerCase() + " service providers available",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ServiceProvidersActivity.this,
                        "Failed to load service providers: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}