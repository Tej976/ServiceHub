package com.example.servicehub.Book;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servicehub.R;

import java.util.ArrayList;

public class BookingActivity extends AppCompatActivity {

    private ListView bookingsListView;
    private TextView noBookingsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        bookingsListView = findViewById(R.id.bookingsListView);
        noBookingsTextView = findViewById(R.id.noBookingsTextView);

        loadBookings();
    }

    private void loadBookings() {
        ArrayList<Booking> bookings = BookingDB.getInstance().getBookings();

        if (bookings.isEmpty()) {
            noBookingsTextView.setVisibility(View.VISIBLE);
            bookingsListView.setVisibility(View.GONE);
        } else {
            noBookingsTextView.setVisibility(View.GONE);
            bookingsListView.setVisibility(View.VISIBLE);

            BookingAdapter adapter = new BookingAdapter(this, bookings);
            bookingsListView.setAdapter(adapter);
        }
    }
}
