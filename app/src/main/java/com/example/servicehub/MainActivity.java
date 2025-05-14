package com.example.servicehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicehub.notifications.NotificationsActivity;
import com.example.servicehub.booking.MyBookingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

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
    private List<ServiceItem> filteredList;
    private BottomNavigationView bottomNav;
    private BottomNavigationHandler navigationHandler;

    private FirebaseAuth mAuth;
    private static final String PREF_NAME = "AppPrefs";
    private static final String FIRST_RUN_KEY = "isFirstRun";

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        themeManager = new ThemeManager(this);
        themeManager.init();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        servicesRecyclerView = findViewById(R.id.services_recycler_view);
        searchView = findViewById(R.id.searchView);

        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        createServiceList();
        filteredList = new ArrayList<>(serviceList);

        serviceAdapter = new ServiceAdapter(this, filteredList);
        servicesRecyclerView.setAdapter(serviceAdapter);

        serviceAdapter.setOnItemClickListener(position -> {
            String serviceName = filteredList.get(position).getTitle();
            Intent intent = new Intent(MainActivity.this, ServiceProvidersActivity.class);
            intent.putExtra("serviceName", serviceName);
            intent.putExtra("userId", userId);
            intent.putExtra("userType", userType);
            startActivity(intent);
        });

        userId = getIntent().getStringExtra("userId");
        userType = getIntent().getStringExtra("userType");

        if (userId == null || userType == null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                mAuth.signOut();
            }
            redirectToLogin();
            return;
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        navigationHandler = new BottomNavigationHandler(this, bottomNav, userId, userType);
        navigationHandler.setSelectedItem(R.id.nav_bottom_home);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        loadUserData();

        Intent serviceIntent = new Intent(this, com.example.servicehub.notifications.NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 🔍 Setup SearchView filtering
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterServices(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterServices(newText);
                return true;
            }
        });
    }

    private void filterServices(String query) {
        filteredList.clear();
        for (ServiceItem item : serviceList) {
            if (item.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }
        serviceAdapter.notifyDataSetChanged();
    }

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
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserData() {
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
                    View headerView = navigationView.getHeaderView(0);
                    TextView navUsername = headerView.findViewById(R.id.nav_header_name);
                    navUsername.setText(name);
                    TextView navUserType = headerView.findViewById(R.id.nav_header_userType);
                    String formattedUsertype = userType.substring(0, 1).toUpperCase() + userType.substring(1);
                    navUserType.setText(formattedUsertype);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navigationHandler != null) {
            navigationHandler.setSelectedItem(R.id.nav_bottom_home);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userType", userType);
            startActivity(intent);
        } else if (id == R.id.nav_toggle_theme) {
            themeManager.toggleTheme(this);
            item.setTitle(themeManager.isDarkMode() ? "Switch to Light Theme" : "Switch to Dark Theme");
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            preferences.edit().clear().apply();
            redirectToLogin();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}