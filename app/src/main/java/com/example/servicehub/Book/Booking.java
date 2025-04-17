package com.example.servicehub.Book;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Booking {
    private String serviceInfo;
    private String dateTime;
    private long id;

    public Booking(String serviceInfo, String dateTime, long id) {
        this.serviceInfo = serviceInfo;
        this.dateTime = dateTime;
        this.id = id;
    }

    public String getServiceInfo() {
        return serviceInfo;
    }

    public String getDateTime() {
        return dateTime;
    }

    public long getId() {
        return id;
    }
}
