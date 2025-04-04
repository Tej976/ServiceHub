package com.example.realtimedb;

public class User {
    private String name;
    private String mobile;
    private String address;
    private String email;
    private String password;
    private String userId;

    // Required empty constructor for Firebase
    public User() {
    }

    public User(String name, String mobile, String address, String email, String password, String userId) {
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.email = email;
        this.password = password;
        this.userId = userId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}