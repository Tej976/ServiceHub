package com.example.realtimedb;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class AddActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CardAdapter adapter;
    private List<CardItem> cardList = new ArrayList<>();                       // List to hold CardItem objects
    private int cardCount = 1;                                                 // Counter to keep track of the number of cards added

    // onCreate method is called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                                    // Call the superclass's onCreate method
        setContentView(R.layout.activity_add);                                // Set the content view to the activity_main layout

        // Initialize the RecyclerView
        recyclerView = findViewById(R.id.recyclerView);                        // Find the RecyclerView by its ID
        recyclerView.setLayoutManager(new LinearLayoutManager(this));   // Set a LinearLayoutManager for vertical scrolling

        // Initialize the adapter with the card list and the current context
        adapter = new CardAdapter(cardList, this);
        recyclerView.setAdapter(adapter); // Set the adapter to the RecyclerView

        // Find the button that adds new cards
        Button addCardButton = findViewById(R.id.addCardButton);
        // Set an OnClickListener on the button to add a new card when clicked
        addCardButton.setOnClickListener(v -> addNewCard());
    }

    // Method to add a new card to the list
    private void addNewCard() {
        // Determine the action type based on the current card count (cycling through 1, 2, 3)
        int actionType = cardCount % 3 + 1; // This will give values 1, 2, or 3
        // Create a new CardItem with a title and action type, and add it to the list
        cardList.add(new CardItem("Card " + cardCount, actionType));
        // Notify the adapter that a new item has been inserted at the end of the list
        adapter.notifyItemInserted(cardList.size() - 1);
        // Increment the card count for the next card
        cardCount++;
    }
}