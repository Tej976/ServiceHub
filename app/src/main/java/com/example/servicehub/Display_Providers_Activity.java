package com.example.friendrequestapp;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.friendrequestapp.adapter.UserAdapter;
import com.example.friendrequestapp.model.User;
import com.example.friendrequestapp.util.FirebaseManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private FirebaseManager firebaseManager;
    private List<User> userList;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar toolbar = findViewById(R.id.menu_main);
        setSupportActionBar(toolbar);

        firebaseManager = new FirebaseManager();
        userList = new ArrayList<>();

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(userList, firebaseManager);
        recyclerViewUsers.setAdapter(userAdapter);

        tabLayout = findViewById(R.id.tabLayout);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        loadAllUsers();
                        break;
                    case 1:
                        loadFriendRequests();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load all users by default
        loadAllUsers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAllUsers() {
        firebaseManager.getAllUsers(new FirebaseManager.FirebaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                userList.clear();
                userList.addAll(users);
                userAdapter.setViewType(UserAdapter.VIEW_TYPE_USERS);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                // Handle error
                // Add proper error handling here
                Log.e("UserListActivity", "Error loading users: " + e.getMessage());
                Toast.makeText(UserListActivity.this, "Error loading users", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void loadFriendRequests() {
        firebaseManager.getPendingFriendRequests(new FirebaseManager.FirebaseCallback<List<com.example.friendrequestapp.model.FriendRequest>>() {
            @Override
            public void onSuccess(List<com.example.friendrequestapp.model.FriendRequest> friendRequests) {
                userList.clear();
                for (com.example.friendrequestapp.model.FriendRequest request : friendRequests) {
                    firebaseManager.getUserInfo(request.getSenderId(), new FirebaseManager.FirebaseCallback<User>() {
                        @Override
                        public void onSuccess(User user) {
                            userList.add(user);
                            userAdapter.setRequestList(friendRequests);
                            userAdapter.setViewType(UserAdapter.VIEW_TYPE_REQUESTS);
                            userAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onError(Exception e) {
                            // Handle error
                        }
                    });
                }

                if (friendRequests.isEmpty()) {
                    userAdapter.setViewType(UserAdapter.VIEW_TYPE_REQUESTS);
                    userAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Exception e) {
                // Handle error
            }
        });
    }
}
