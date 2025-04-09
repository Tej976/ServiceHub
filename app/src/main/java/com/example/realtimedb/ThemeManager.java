package com.example.realtimedb;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    // Constants
    private static final String THEME_PREFS = "ThemePreferences";
    private static final String KEY_THEME_MODE = "ThemeMode";

    // Singleton instance
    private static ThemeManager instance;

    // Theme modes
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    // Member variables
    private final SharedPreferences preferences;

    // Private constructor for singleton pattern
    private ThemeManager(Context context) {
        preferences = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
    }

    // Get singleton instance
    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }

    // Apply saved theme mode or default to system
    public void applyTheme() {
        int themeMode = getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    // Get current theme mode
    public int getThemeMode() {
        // Default to system theme for API 29+ (Android 10+)
        int defaultMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                MODE_SYSTEM : MODE_LIGHT;
        return preferences.getInt(KEY_THEME_MODE, defaultMode);
    }

    // Save and apply theme mode
    public void setThemeMode(int themeMode) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_THEME_MODE, themeMode);
        editor.apply();

        // Apply the theme mode
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    // Toggle between light and dark theme
    public void toggleTheme() {
        int currentTheme = getThemeMode();
        int newTheme = (currentTheme == MODE_DARK) ? MODE_LIGHT : MODE_DARK;
        setThemeMode(newTheme);
    }

    // Check if dark theme is active
    public boolean isDarkTheme() {
        return getThemeMode() == MODE_DARK;
    }
}