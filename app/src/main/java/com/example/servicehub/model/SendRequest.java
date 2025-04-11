package com.example.servicehub.model;

import java.util.Date;

public class SendRequest {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String status; // "pending", "accepted", "rejected"
    private long timestamp;

    // Empty constructor needed for Firebase
    public SendRequest() {}

    public SendRequest(String senderId, String receiverId) {
        this.requestId = senderId + "_" + receiverId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = "pending";
        this.timestamp = new Date().getTime();
    }

    // Getters and setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
