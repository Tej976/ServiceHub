package com.example.servicehub.Book;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.servicehub.R;

import java.util.ArrayList;

public class BookingAdapter extends ArrayAdapter<Booking> {

    private Context context;

    public BookingAdapter(Context context, ArrayList<Booking> bookings) {
        super(context, 0, bookings);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        }

        final Booking currentBooking = getItem(position);

        TextView serviceInfoTextView = listItem.findViewById(R.id.bookingServiceTextView);
        TextView dateTimeTextView = listItem.findViewById(R.id.bookingDateTimeTextView);
        Button cancelButton = listItem.findViewById(R.id.cancelBookingButton);

        serviceInfoTextView.setText(currentBooking.getServiceInfo());
        dateTimeTextView.setText(currentBooking.getDateTime());

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookingDB.getInstance().cancelBooking(currentBooking.getId());
                Toast.makeText(context, "Booking cancelled", Toast.LENGTH_SHORT).show();

                // Notify adapter that data has changed
                BookingAdapter.this.remove(currentBooking);
                BookingAdapter.this.notifyDataSetChanged();

                // Check if list is now empty
                if (BookingAdapter.this.getCount() == 0) {
                    ((BookingActivity)context).recreate();
                }
            }
        });

        return listItem;
    }
}
