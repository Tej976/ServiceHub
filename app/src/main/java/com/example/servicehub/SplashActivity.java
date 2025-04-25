package com.example.servicehub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private LinearLayout splashContainer;
    private ImageView splashLogo;
    private TextView splashText;
    private CardView loginCardView;

    // Login components
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize splash views
        splashContainer = findViewById(R.id.splashContainer);
        splashLogo = findViewById(R.id.splashLogo);
        splashText = findViewById(R.id.splashText);
        loginCardView = findViewById(R.id.loginCardView);

        // Initialize login views
        emailEditText = findViewById(R.id.editTextLoginEmail);
        passwordEditText = findViewById(R.id.editTextLoginPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerTextView = findViewById(R.id.textViewRegister);

        // Load and start splash animations
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.top_bounce);
        Animation textAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_bounce);
        splashLogo.startAnimation(logoAnimation);
        splashText.startAnimation(textAnimation);

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in, check their type and redirect
            checkUserTypeAndRedirect(currentUser.getUid());
        } else {
            // No user is logged in, show login UI after delay
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            showLoginForm();
                        }
                    }, 2500); // 2.5 seconds delay
        }

        // Set up login button click listener
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
                // Navigate to user type selection screen
                Intent intent = new Intent(SplashActivity.this, UserTypeSelectionActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showLoginForm() {
        // Prepare animation for splash elements to move up and shrink
        Animation moveUpSplash = AnimationUtils.loadAnimation(this, R.anim.move_up);
        moveUpSplash.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Called when animation starts
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // After splash moves up, show login form with animation
                loginCardView.setVisibility(View.VISIBLE);
                Animation slideUp = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.slide_up);
                loginCardView.startAnimation(slideUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Called when animation repeats
            }
        });

        // Start animation for splash container to move up and shrink
        splashContainer.startAnimation(moveUpSplash);
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(SplashActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            if (user != null) {
                                checkUserTypeAndRedirect(user.getUid());
                            }
                        } else {
                            // If sign in fails, display a message to the user
                            Toast.makeText(SplashActivity.this, "Authentication failed: "
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
                Toast.makeText(SplashActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                showLoginForm(); // Show login form on error
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
                    showLoginForm(); // Show login form if profile not found
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SplashActivity.this, "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                showLoginForm(); // Show login form on error
            }
        });
    }

    private void redirectToMainActivity(String userId, String userType) {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userType", userType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}