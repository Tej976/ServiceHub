package com.example.servicehub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ImageView splashLogo;
    private TextView splashText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        splashLogo = findViewById(R.id.splashLogo);
        splashText = findViewById(R.id.splashText);

        // Load animations
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.top_bounce);
        Animation textAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_bounce);

        // Start animations
        splashLogo.startAnimation(logoAnimation);
        splashText.startAnimation(textAnimation);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in, check their type and redirect
            checkUserTypeAndRedirect(currentUser.getUid());
        } else {
            // No user is logged in, go to login screen
            // Add a delay so users can see the splash screen animations
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            goToLoginActivity();
                        }
                    }, 3500); // 3.5 seconds delay to allow slower animations to complete
        }
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
                Toast.makeText(SplashActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                goToLoginActivity(); // Fallback to login on error
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
                    Toast.makeText(SplashActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    goToLoginActivity(); // Fallback to login
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SplashActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                goToLoginActivity(); // Fallback to login on error
            }
        });
    }

    private void redirectToMainActivity(String userId, String userType) {
        // Add a delay so users can see the splash screen and animations
        new android.os.Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }, 3500); // 3.5 seconds delay
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}