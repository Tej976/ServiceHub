package com.example.servicehub;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private String mobile;
    private String address;
    private String email;
    private String userId;
    private String authUid;
    private String userType;
    private List<String> services;

    // Empty constructor required for Firebase
    public User() {
        // Initialize services to avoid null pointer exceptions
        services = new ArrayList<>();
    }

    public User(String name, String mobile, String address, String email, String userId, String authUid, String userType) {
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.email = email;
        this.userId = userId;
        this.authUid = authUid;
        this.userType = userType;
        this.services = new ArrayList<>();
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