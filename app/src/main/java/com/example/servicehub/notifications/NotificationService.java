package com.example.servicehub.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.servicehub.R;
import com.example.servicehub.booking.BookingDetailsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationService extends Service {

    private static final String CHANNEL_ID = "service_hub_channel";
    private DatabaseReference notificationsRef;
    private ChildEventListener notificationListener;

    private static final int NOTIFICATION_ID = 1001; // Add this line

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel for Android Oreo and above
        createNotificationChannel();

        // Set up Firebase listener
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId != null) {
            notificationsRef = FirebaseDatabase.getInstance()
                    .getReference("popup_notifications").child(userId);

            notificationListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if (snapshot.exists()) {
                        String title = snapshot.child("title").getValue(String.class);
                        String message = snapshot.child("message").getValue(String.class);
                        String bookingId = snapshot.child("bookingId").getValue(String.class);
                        Boolean isRead = snapshot.child("read").getValue(Boolean.class);

                        if (title != null && message != null && (isRead == null || !isRead)) {
                            // Display notification
                            displayNotification(title, message, bookingId);

                            // Mark notification as read
                            snapshot.getRef().child("read").setValue(true);
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // Not needed for pop-up notifications
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    // Not needed for pop-up notifications
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // Not needed for pop-up notifications
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            };

            notificationsRef.addChildEventListener(notificationListener);
        }
    }


    private void displayNotification(String title, String message, String bookingId) {
        // Create intent for notification click action
        Intent intent;

        if (bookingId != null) {
            // If it's related to a booking, open booking details
            intent = new Intent(this, BookingDetailsActivity.class);
            intent.putExtra("bookingId", bookingId);
        } else {
            // If no booking ID, open main activity
            intent = new Intent(this, com.example.servicehub.MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create notification channel first (for Android 8.0+)
        createNotificationChannel();

        // Create a notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service Running")
                .setContentText("Synchronizing data...")
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, notification);

        // Your service logic here

        return START_STICKY;
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }



    private void setupFirebaseListeners() {
        // Only set up listeners if they're not already set up
        if (notificationListener == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            notificationsRef = FirebaseDatabase.getInstance()
                    .getReference("popup_notifications").child(userId);

            // Add your existing listener code here if it's not already in onCreate
            // or just leave this empty if you're already setting it up in onCreate
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove Firebase listener
        if (notificationsRef != null && notificationListener != null) {
            notificationsRef.removeEventListener(notificationListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}