package com.example.servicehub;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import android.view.MenuItem;

import com.example.servicehub.booking.MyBookingsActivity;
import com.example.servicehub.notifications.NotificationsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * A class to handle bottom navigation functionality across activities
 */
public class BottomNavigationHandler {

    private final Activity activity;
    private final String userId;
    private final String userType;
    private final BottomNavigationView bottomNav;

    /**
     * Constructor for BottomNavigationHandler
     *
     * @param activity The activity where the navigation is used
     * @param bottomNav The BottomNavigationView instance
     * @param userId The user ID to pass between activities
     * @param userType The user type to pass between activities
     */
    public BottomNavigationHandler(Activity activity, BottomNavigationView bottomNav,
                                   String userId, String userType) {
        this.activity = activity;
        this.bottomNav = bottomNav;
        this.userId = userId;
        this.userType = userType;

        // Setup the bottom navigation
        setup();
    }

    /**
     * Sets up the bottom navigation with listeners and visibility
     */
    private void setup() {
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
    }

    /**
     * Sets the currently selected item in the bottom navigation
     *
     * @param itemId The ID of the menu item to select
     */
    public void setSelectedItem(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    /**
     * The navigation item selection listener
     */
    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();

                    // If we're already on the selected screen, just return true
                    if ((activity instanceof MainActivity && id == R.id.nav_bottom_home) ||
                            (activity instanceof AddActivity && id == R.id.nav_bottom_add) ||
                            (activity instanceof MyBookingsActivity && id == R.id.nav_bottom_bookings) ||
                            (activity instanceof NotificationsActivity && id == R.id.nav_bottom_notifications)) {
                        return true;
                    }

                    // Start the appropriate activity based on menu selection
                    Intent intent = null;

                    if (id == R.id.nav_bottom_home) {
                        intent = new Intent(activity, MainActivity.class);
                    }
                    else if (id == R.id.nav_bottom_add) {
                        intent = new Intent(activity, AddActivity.class);
                    }
                    else if (id == R.id.nav_bottom_bookings) {
                        intent = new Intent(activity, MyBookingsActivity.class);
                    }
                    else if (id == R.id.nav_bottom_notifications) {
                        intent = new Intent(activity, NotificationsActivity.class);
                    }

                    if (intent != null) {
                        // Pass user data to the next activity
                        intent.putExtra("userId", userId);
                        intent.putExtra("userType", userType);
                        activity.startActivity(intent);
                        return true;
                    }

                    return false;
                }
            };
}