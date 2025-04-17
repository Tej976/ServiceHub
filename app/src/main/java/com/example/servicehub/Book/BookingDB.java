package com.example.servicehub.Book;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

// Simple singleton database for demo purposes
public class BookingDB {

    private static BookingDB instance;
    private ArrayList<Booking> bookings;

    private BookingDB() {
        bookings = new ArrayList<>();
    }

    public static synchronized BookingDB getInstance() {
        if (instance == null) {
            instance = new BookingDB();
        }
        return instance;
    }

    public void addBooking(Booking booking) {
        bookings.add(booking);
    }

    public ArrayList<Booking> getBookings() {
        return new ArrayList<>(bookings); // Return a copy to avoid modification
    }

    public void cancelBooking(long id) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getId() == id) {
                bookings.remove(i);
                break;
            }
        }
    }
}
