package com.example.realtimedb;

public class User {
    private String name;
    private String mobile;
    private String address;
    private String email;
    private String userId;
    private String authUid;  // Added to link with Firebase Auth UID

    // Required empty constructor for Firebase
    public User() {
    }

    public User(String name, String mobile, String address, String email, String userId, String authUid) {
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.email = email;
        this.userId = userId;
        this.authUid = authUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthUid() {
        return authUid;
    }

    public void setAuthUid(String authUid) {
        this.authUid = authUid;
    }
}