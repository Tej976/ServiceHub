package com.example.servicehub.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicehub.R;
import com.example.servicehub.model.SendRequest;
import com.example.servicehub.model.UserReq;
import com.example.servicehub.util.FirebaseManager;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public static final int VIEW_TYPE_USERS = 1;
    public static final int VIEW_TYPE_REQUESTS = 2;

    private List<UserReq> userList;
    private List<SendRequest> requestList;
    private FirebaseManager firebaseManager;
    private int viewType = VIEW_TYPE_USERS;

    public UserAdapter(List<UserReq> userList, FirebaseManager firebaseManager) {
        this.userList = userList;
        this.firebaseManager = firebaseManager;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public void setRequestList(List<SendRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.userreq_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserReq user = userList.get(position);
        // Display username and role
        holder.textViewUsername.setText(user.getUsername() + " (" +
                (user.getRole().equals("customer") ? "Customer" : "Service Provider") + ")");

        if (viewType == VIEW_TYPE_USERS) {
            holder.buttonAction.setText("Send Request");
            holder.buttonReject.setVisibility(View.GONE);

            // First check if the current user can send request to this user based on roles
            firebaseManager.canSendFriendRequest(user.getUserId(), new FirebaseManager.FirebaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean canSend) {
                    if (!canSend) {
                        holder.buttonAction.setText("Can't Send Request");
                        holder.buttonAction.setEnabled(false);
                        return;
                    }

                    // Now check if a friend request already exists
                    firebaseManager.checkFriendRequestStatus(user.getUserId(), new FirebaseManager.FirebaseCallback<String>() {
                        @Override
                        public void onSuccess(String status) {
                            if (status.equals("pending")) {
                                holder.buttonAction.setText("Request Sent");
                                holder.buttonAction.setEnabled(false);
                            } else if (status.equals("accepted")) {
                                holder.buttonAction.setText("Friends");
                                holder.buttonAction.setEnabled(false);
                            } else if (status.equals("pending_received")) {
                                holder.buttonAction.setText("Accept");
                                holder.buttonAction.setEnabled(true);
                                holder.buttonReject.setVisibility(View.VISIBLE);
                            } else if (status.equals("none")) {
                                holder.buttonAction.setText("Send Request");
                                holder.buttonAction.setEnabled(true);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            // Handle error
                            Toast.makeText(holder.itemView.getContext(),
                                    "Error checking friend status: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(holder.itemView.getContext(),
                            "Error checking if can send request: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

            holder.buttonAction.setOnClickListener(v -> {
                if (holder.buttonAction.getText().equals("Send Request")) {
                    firebaseManager.sendFriendRequest(user.getUserId(), new FirebaseManager.FirebaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            holder.buttonAction.setText("Request Sent");
                            holder.buttonAction.setEnabled(false);
                            Toast.makeText(v.getContext(), "Friend request sent", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(v.getContext(), "Error sending request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if (holder.buttonAction.getText().equals("Accept")) {
                    // Find the request
                    for (SendRequest request : requestList) {
                        if (request.getSenderId().equals(user.getUserId())) {
                            firebaseManager.acceptFriendRequest(request, new FirebaseManager.FirebaseCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    holder.buttonAction.setText("Friends");
                                    holder.buttonAction.setEnabled(false);
                                    holder.buttonReject.setVisibility(View.GONE);
                                    Toast.makeText(v.getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(v.getContext(), "Error accepting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        }
                    }
                }
            });
        } else if (viewType == VIEW_TYPE_REQUESTS) {
            holder.buttonAction.setText("Accept");
            holder.buttonReject.setVisibility(View.VISIBLE);

            // Find the current request
            SendRequest currentRequest = null;
            for (SendRequest request : requestList) {
                if (request.getSenderId().equals(user.getUserId())) {
                    currentRequest = request;
                    break;
                }
            }

            final SendRequest finalRequest = currentRequest;

            holder.buttonAction.setOnClickListener(v -> {
                if (finalRequest != null) {
                    firebaseManager.acceptFriendRequest(finalRequest, new FirebaseManager.FirebaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            userList.remove(holder.getAdapterPosition());
                            requestList.remove(finalRequest);
                            notifyItemRemoved(holder.getAdapterPosition());
                            Toast.makeText(v.getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(v.getContext(), "Error accepting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            holder.buttonReject.setOnClickListener(v -> {
                if (finalRequest != null) {
                    firebaseManager.rejectFriendRequest(finalRequest, new FirebaseManager.FirebaseCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            userList.remove(holder.getAdapterPosition());
                            requestList.remove(finalRequest);
                            notifyItemRemoved(holder.getAdapterPosition());
                            Toast.makeText(v.getContext(), "Friend request rejected", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(v.getContext(), "Error rejecting request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewUsername;
        Button buttonAction, buttonReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            buttonAction = itemView.findViewById(R.id.buttonAction);
            buttonReject = itemView.findViewById(R.id.buttonReject);
        }
    }
}