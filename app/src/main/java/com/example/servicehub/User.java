package com.example.servicehub;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String name;
    private String mobile;
    private String address;
    private String email;
    private String authUid;
    private String userType;
    private List<String> services;

    // Empty constructor needed for Firebase
    public User() {
        // Default constructor required for Firebase
    }

    public User(String userId, String name, String mobile, String address, String email, String authUid, String userType) {
        this.userId = userId;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.email = email;
        this.authUid = authUid;
        this.userType = userType;
        this.services = new ArrayList<>();
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
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

    public String getAuthUid() {
        return authUid;
    }

    public void setAuthUid(String authUid) {
        this.authUid = authUid;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}