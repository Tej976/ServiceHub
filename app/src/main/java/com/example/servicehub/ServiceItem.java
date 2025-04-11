package com.example.servicehub;

public class ServiceItem {
    private String title;
    private int imageResource;

    public ServiceItem(String title, int imageResource) {
        this.title = title;
        this.imageResource = imageResource;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResource() {
        return imageResource;
    }
}