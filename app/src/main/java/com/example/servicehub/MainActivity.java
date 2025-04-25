package com.example.servicehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicehub.booking.MyBookingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ThemeManager themeManager;

    private String userId;
    private String userType;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView servicesRecyclerView;
    private ServiceAdapter serviceAdapter;
    private List<ServiceItem> serviceList;

    private FirebaseAuth mAuth;
    private static final String PREF_NAME = "AppPrefs";
    private static final String FIRST_RUN_KEY = "isFirstRun";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        themeManager = new ThemeManager(this);
        themeManager.init();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI elements
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Initialize services RecyclerView
        servicesRecyclerView = findViewById(R.id.services_recycler_view);
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // Create sample service items
        createServiceList();

        // Setup adapter
        serviceAdapter = new ServiceAdapter(this, serviceList);
        servicesRecyclerView.setAdapter(serviceAdapter);

        // Set click listener for service items
        serviceAdapter.setOnItemClickListener(position -> {
            String serviceName = serviceList.get(position).getTitle();

            Intent intent = new Intent(MainActivity.this, ServiceProvidersActivity.class);
            intent.putExtra("serviceName", serviceName);
            intent.putExtra("userId",userId);
            intent.putExtra("userType",userType);
            startActivity(intent);

        });

        // Set up bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Set up Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Get userId and userType from intent
        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        // If userId or userType is null, redirect to login
        if (userId == null || userType == null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is authenticated but we don't have userId or userType - log out and redirect
                mAuth.signOut();
            }
            redirectToLogin();
            return;
        }

        // Load user data based on user type
        loadUserData();

        // Update the header view in navigation drawer
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.nav_header_name);
        TextView navUserType = headerView.findViewById(R.id.nav_header_userType);

    }

    // Create list of service items
    private void createServiceList() {
        serviceList = new ArrayList<>();
        serviceList.add(new ServiceItem("Room Cleaning", R.drawable.i_roomcleaning));
        serviceList.add(new ServiceItem("Fridge Repairs", R.drawable.i_fridgerepairs));
        serviceList.add(new ServiceItem("Chef", R.drawable.i_chef));
        serviceList.add(new ServiceItem("Pest Control", R.drawable.i_pest_control));
        serviceList.add(new ServiceItem("Plumbing", R.drawable.i_plumbing));
        serviceList.add(new ServiceItem("Electrical", R.drawable.i_electrical));
        serviceList.add(new ServiceItem("Window Cleaning", R.drawable.i_windowcleaning));
        serviceList.add(new ServiceItem("Painting", R.drawable.i_painting));
        serviceList.add(new ServiceItem("Gardening", R.drawable.i_gardening));
        serviceList.add(new ServiceItem("Moving", R.drawable.i_security));
        serviceList.add(new ServiceItem("AC Repair", R.drawable.i_acrepairs));
        serviceList.add(new ServiceItem("Security", R.drawable.i_security));

        // Add more services as needed
    }



    // Update the bottom navigation listener in MainActivity.java

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    int id = item.getItemId();
                    if (id == R.id.nav_bottom_home) {
                        return true;
                    }
                    else if (id == R.id.nav_bottom_add) {
                        Intent intent = new Intent(MainActivity.this, AddActivity.class);
                        startActivity(intent);
                        return true;
                    }
                    else if (id == R.id.nav_bottom_bookings) {
                        Intent intent = new Intent(MainActivity.this, MyBookingsActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }
                    else if (id == R.id.nav_bottom_notifications) {
                        Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        startActivity(intent);
                        return true;
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                selectedFragment).commit();
                    }
                    return true;
                }
            };

    /* Check if this is the first run after installation. If it is, clear any system-stored back stack information       */

    private void checkIfFirstRun() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isFirstRun = preferences.getBoolean(FIRST_RUN_KEY, true);

        if (isFirstRun) {
            // This is the first run after installation
            // Clear any potential activity stack by setting appropriate flags
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Save that the app has been run
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(FIRST_RUN_KEY, false);
            editor.apply();

            // Finish this instance of MainActivity
            finish();
            return;
        }
    }

    private void loadUserData() {
        // Get user's name from database based on user type
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef;

        if (userType.equals("customer")) {
            userRef = database.getReference("customers").child(userId);
        } else {
            userRef = database.getReference("service_providers").child(userId);
        }

        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.getValue(String.class);

                    // Also update the name in the navigation header
                    View headerView = navigationView.getHeaderView(0);
                    TextView navUsername = headerView.findViewById(R.id.nav_header_name);
                    navUsername.setText(name);

                    // set the userType in the navigation header
                    TextView navUserType = headerView.findViewById(R.id.nav_header_userType);
                    //format the userType i.e. capitalize the first letter
                    String formattedUsertype = userType.substring(0,1).toUpperCase()+ userType.substring(1);
                    navUserType.setText(formattedUsertype);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        // Clear the existing activity stack to ensure a clean start
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is still authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            // Handle the view profile action
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userType", userType);
            startActivity(intent);
        }
        else if (id == R.id.nav_toggle_theme) {
            // Toggle the theme when user clicks this menu item
            themeManager.toggleTheme(this);
            // Update the menu item text based on current theme
            item.setTitle(themeManager.isDarkMode() ? "Switch to Light Theme" : "Switch to Dark Theme");
        }
        else if (id == R.id.nav_logout) {
            // Handle the logout action
            mAuth.signOut();

            // Clear app preferences on logout to ensure a clean state
            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            redirectToLogin();
        }

        // Close the drawer after handling the action
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        // Close drawer on back press if it's open, otherwise perform normal back action
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}