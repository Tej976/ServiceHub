package com.example.realtimedb;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {
    private List<CardItem> cardList;
    private Context context;

    public CardAdapter(List<CardItem> cardList, Context context) {
        this.cardList = cardList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardItem item = cardList.get(position);
        holder.cardTitle.setText(item.getTitle());

        // Set click listener for button inside CardView
        holder.cardButton.setOnClickListener(v -> performAction(item.getActionType()));
    }

    private void performAction(int actionType) {
        switch (actionType) {
            case 1: // Open Camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                context.startActivity(cameraIntent);
                break;
            case 2: // Open Web Page
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
                context.startActivity(webIntent);
                break;
            case 3: // Show Toast
                Toast.makeText(context, "Card Clicked!", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(context, "No action assigned!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cardTitle;
        Button cardButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTitle = itemView.findViewById(R.id.cardTitle);
            cardButton = itemView.findViewById(R.id.cardButton);
        }
    }
}
