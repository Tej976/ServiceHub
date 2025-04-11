package com.example.servicehub.util;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.servicehub.model.SendRequest;
import com.example.servicehub.model.UserReq;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private UserReq currentUser;

    public FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Load current user info if available
        if (mAuth.getCurrentUser() != null) {
            loadCurrentUserInfo();
        }
    }

    private void loadCurrentUserInfo() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUser = dataSnapshot.getValue(UserReq.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading current user info", databaseError.toException());
            }
        });
    }



    // Register a new user in Firebase Authentication and store user data in Realtime Database

    public void registerUser(String email, String password, String username, String role, final FirebaseCallback<UserReq> callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            UserReq user = new UserReq(userId, username, email, role);

                            mDatabase.child("users").child(userId).setValue(user)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            currentUser = user;
                                            callback.onSuccess(user);
                                        } else {
                                            callback.onError(dbTask.getException());
                                        }
                                    });
                        }
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }



    // Get all registered users except the current user
    // For customers: Show only service providers
    // For service providers: Show only other service providers

    public void getAllUsers(final FirebaseCallback<List<UserReq>> callback) {
        String currentUserId = mAuth.getCurrentUser().getUid();

        // First get the current user to determine their role
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserReq currentUser = dataSnapshot.getValue(UserReq.class);
                if (currentUser == null) {
                    callback.onError(new Exception("Current user not found"));
                    return;
                }

                // Now get all users based on the current user's role
                mDatabase.child("users").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<UserReq> userList = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            UserReq user = snapshot.getValue(UserReq.class);
                            if (user != null && !user.getUserId().equals(currentUserId)) {
                                // Filter based on roles:
                                // If current user is customer, show only service providers
                                // If current user is service provider, show only other service providers
                                if (currentUser.getRole().equals("customer")) {
                                    if (user.getRole().equals("service_provider")) {
                                        userList.add(user);
                                    }
                                } else { // Current user is a service provider
                                    if (user.getRole().equals("service_provider")) {
                                        userList.add(user);
                                    }
                                }
                            }
                        }
                        callback.onSuccess(userList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }



    // Check if a friend request can be sent based on user roles

    public void canSendFriendRequest(String receiverId, final FirebaseCallback<Boolean> callback) {
        if (currentUser == null) {
            loadCurrentUserInfoAndCheckRules(receiverId, callback);
            return;
        }

        getUserInfo(receiverId, new FirebaseCallback<UserReq>() {
            @Override
            public void onSuccess(UserReq receiverUser) {
                String senderRole = currentUser.getRole();
                String receiverRole = receiverUser.getRole();

                boolean canSend = true;

                // Service Provider to Customer: Not allowed
                if (senderRole.equals("service_provider") && receiverRole.equals("customer")) {
                    canSend = false;
                }
                if (senderRole.equals("customer") && receiverRole.equals("customer")) {
                    canSend = false;
                }

                callback.onSuccess(canSend);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }


    private void loadCurrentUserInfoAndCheckRules(String receiverId, final FirebaseCallback<Boolean> callback) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUser = dataSnapshot.getValue(UserReq.class);

                // Now check rules
                getUserInfo(receiverId, new FirebaseCallback<UserReq>() {
                    @Override
                    public void onSuccess(UserReq receiverUser) {
                        String senderRole = currentUser.getRole();
                        String receiverRole = receiverUser.getRole();

                        boolean canSend = true;

                        // Service Provider to Customer: Not allowed
                        if (senderRole.equals("service_provider") && receiverRole.equals("customer")) {
                            canSend = false;
                        }

                        callback.onSuccess(canSend);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }


    // Send a friend request

    public void sendFriendRequest(String receiverId, final FirebaseCallback<Boolean> callback) {
        canSendFriendRequest(receiverId, new FirebaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean canSend) {
                if (canSend) {
                    String currentUserId = mAuth.getCurrentUser().getUid();
                    SendRequest request = new SendRequest(currentUserId, receiverId);

                    mDatabase.child("friend_requests").child(request.getRequestId()).setValue(request)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    callback.onSuccess(true);
                                } else {
                                    callback.onError(task.getException());
                                }
                            });
                } else {
                    callback.onError(new Exception("Cannot send friend request based on user roles"));
                }
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // Get pending friend requests for the current user based on role
    // For service providers, we'll only return requests from other service providers
    // For customers, we'll return all their pending requests
    public void getPendingFriendRequests(final FirebaseCallback<List<SendRequest>> callback) {
        String currentUserId = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserReq currentUser = dataSnapshot.getValue(UserReq.class);
                if (currentUser == null) {
                    callback.onError(new Exception("Current user not found"));
                    return;
                }

                mDatabase.child("friend_requests").orderByChild("receiverId").equalTo(currentUserId)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                List<SendRequest> requestList = new ArrayList<>();

                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    SendRequest request = snapshot.getValue(SendRequest.class);
                                    if (request != null && request.getStatus().equals("pending")) {
                                        // For service providers, check if request is from another service provider
                                        if (currentUser.getRole().equals("service_provider")) {
                                            mDatabase.child("users").child(request.getSenderId()).addListenerForSingleValueEvent(
                                                    new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                                                            UserReq sender = senderSnapshot.getValue(UserReq.class);
                                                            if (sender != null && sender.getRole().equals("service_provider")) {
                                                                requestList.add(request);
                                                                callback.onSuccess(requestList);
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            callback.onError(error.toException());
                                                        }
                                                    });
                                        } else {
                                            // For customers, add all requests
                                            requestList.add(request);
                                        }
                                    }
                                }

                                // For customers, call the callback here
                                if (currentUser.getRole().equals("customer")) {
                                    callback.onSuccess(requestList);
                                } else if (requestList.isEmpty()) {
                                    // For service providers with no pending requests
                                    callback.onSuccess(requestList);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                callback.onError(databaseError.toException());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.toException());
            }
        });
    }
    // Accept a friend request
    public void acceptFriendRequest(SendRequest request, final FirebaseCallback<Boolean> callback) {
        // Update request status to accepted
        request.setStatus("accepted");

        Map<String, Object> requestUpdates = new HashMap<>();
        requestUpdates.put("/friend_requests/" + request.getRequestId() + "/status", "accepted");

        // Add both users to each other's friends list
        requestUpdates.put("/friends/" + request.getSenderId() + "/" + request.getReceiverId(), true);
        requestUpdates.put("/friends/" + request.getReceiverId() + "/" + request.getSenderId(), true);

        mDatabase.updateChildren(requestUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    // Reject a friend request
    public void rejectFriendRequest(SendRequest request, final FirebaseCallback<Boolean> callback) {
        mDatabase.child("friend_requests").child(request.getRequestId()).child("status").setValue("rejected")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(true);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    // Check if a friend request already exists between two users
    public void checkFriendRequestStatus(String otherUserId, final FirebaseCallback<String> callback) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        String requestId1 = currentUserId + "_" + otherUserId;
        String requestId2 = otherUserId + "_" + currentUserId;

        mDatabase.child("friend_requests").child(requestId1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    SendRequest request = dataSnapshot.getValue(SendRequest.class);
                    callback.onSuccess(request.getStatus());
                } else {
                    // Check if the request exists in the other direction
                    mDatabase.child("friend_requests").child(requestId2).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot innerSnapshot) {
                            if (innerSnapshot.exists()) {
                                SendRequest request = innerSnapshot.getValue(SendRequest.class);
                                callback.onSuccess(request.getStatus() + "_received");
                            } else {
                                callback.onSuccess("none");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            callback.onError(databaseError.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    // Check if users are friends
    public void checkIfFriends(String otherUserId, final FirebaseCallback<Boolean> callback) {
        String currentUserId = mAuth.getCurrentUser().getUid();

        mDatabase.child("friends").child(currentUserId).child(otherUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        callback.onSuccess(dataSnapshot.exists());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.toException());
                    }
                });
    }

    // Get user info by user ID
    public void getUserInfo(String userId, final FirebaseCallback<UserReq> callback) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserReq user = dataSnapshot.getValue(UserReq.class);
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onError(new Exception("User not found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    // Callback interface for async operations
    public interface FirebaseCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}