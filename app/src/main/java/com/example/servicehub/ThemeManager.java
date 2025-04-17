package com.example.servicehub;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String THEME_PREFS = "ThemePrefs";
    private static final String DARK_MODE_KEY = "isDarkMode";

    private Context context;
    private boolean isDarkMode;

    public ThemeManager(Context context) {
        this.context = context;

        // Load saved theme preference
        SharedPreferences sharedPreferences = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
    }

    public void init() {
        applyTheme();
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public void toggleTheme(AppCompatActivity activity) {
        // Toggle the theme state
        isDarkMode = !isDarkMode;

        // Save the theme preference
        SharedPreferences sharedPreferences = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DARK_MODE_KEY, isDarkMode);
        editor.apply();

        // Apply the theme
        applyTheme();

        // Recreate the activity to apply changes
        if (activity != null) {
            activity.recreate();
        }
    }

    public void applyTheme() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            context.setTheme(R.style.AppTheme);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            context.setTheme(R.style.AppTheme);
        }
    }
}