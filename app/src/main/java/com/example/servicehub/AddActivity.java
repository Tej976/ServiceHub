package com.example.servicehub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CardAdapter adapter;
    private List<CardItem> cardList = new ArrayList<>();                       // List to hold CardItem objects
    private int cardCount = 1;                                                 // Counter to keep track of the number of cards added
    private final String DEFAULT_EMAIL_ADDRESS = "tejaswininikam642004@gmail.com";     // Default email address
    private EditText serviceNameInput;
    private EditText serviceDescription;

    private FirebaseAuth mAuth;
    private String userId;
    private String userType;
    private String providerEmail;
    private String providerName;

    // onCreate method is called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                                    // Call the superclass's onCreate method
        setContentView(R.layout.activity_add);                                 // Set the content view to the activity_main layout

        serviceNameInput = findViewById(R.id.serviceNameInput);
        serviceDescription = findViewById(R.id.serviceDescription);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get current user information
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            providerEmail = currentUser.getEmail();
        }

        // Get userId and userType from intent or SharedPreferences
        // If passing from MainActivity, you can get them like this:
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        userType = intent.getStringExtra("userType");

        // If userId is available, fetch additional provider info
        if (userId != null && "service_provider".equals(userType)) {
            fetchProviderInfo();
        }

        // Initialize the RecyclerView
     //   recyclerView = findViewById(R.id.recyclerView);                        // Find the RecyclerView by its ID
      //  recyclerView.setLayoutManager(new LinearLayoutManager(this));          // Set a LinearLayoutManager for vertical scrolling

        // Initialize the adapter with the card list and the current context
   //     adapter = new CardAdapter(cardList, this);
     //   recyclerView.setAdapter(adapter);                                      // Set the adapter to the RecyclerView

        // Find the button that adds new cards
        Button addCardButton = findViewById(R.id.addCardButton);

        // Set an OnClickListener on the button to add a new card and send an email when clicked
        addCardButton.setOnClickListener(v -> {
  //          addNewCard();
            sendEmail(serviceNameInput, serviceDescription);
        });
    }

    // Fetch service provider information from Firebase
    private void fetchProviderInfo() {
        DatabaseReference providerRef = FirebaseDatabase.getInstance()
                .getReference("service_providers")
                .child(userId);

        providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get provider name
                    if (dataSnapshot.child("name").exists()) {
                        providerName = dataSnapshot.child("name").getValue(String.class);
                    }

                    // Get provider email if not already available from Firebase Auth
                    if (providerEmail == null && dataSnapshot.child("email").exists()) {
                        providerEmail = dataSnapshot.child("email").getValue(String.class);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddActivity.this,
                        "Failed to load provider data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to add a new card to the list
   /* private void addNewCard() {
        // Determine the action type based on the current card count (cycling through 1, 2, 3)
        int actionType = cardCount % 3 + 1; // This will give values 1, 2, or 3

        // Create a new CardItem with a title and action type, and add it to the list
        cardList.add(new CardItem("Card " + cardCount, actionType));

        // Notify the adapter that a new item has been inserted at the end of the list
        adapter.notifyItemInserted(cardList.size() - 1);

        // Increment the card count for the next card
        cardCount++;
    }*/

    // Method to send an email using an implicit intent
    private void sendEmail(EditText serviceNameInput, EditText serviceDescription) {
        try {
            if (providerEmail == null) {
                Toast.makeText(this, "Service provider email not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Try a more universal approach to email intent
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");                                                // Standard email MIME type
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{DEFAULT_EMAIL_ADDRESS});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Request to add a New Service from ServiceHub");

            String name = serviceNameInput.getText().toString();
            String sname = serviceDescription.getText().toString();

            String messageBody = "A new request to add a new service "+name+".\n\n";
            messageBody +=  "The new service will provide "+sname+",\n\n";
            if (providerName != null) {
                messageBody += "Service Provider: " + providerName + "\n";
            }
            messageBody += "From: " + providerEmail;

            emailIntent.putExtra(Intent.EXTRA_TEXT, messageBody);

            // Try to show a chooser of email apps
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email using..."));
                Toast.makeText(this, "Please select an email client", Toast.LENGTH_SHORT).show();
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email clients installed on this device", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sending email: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}