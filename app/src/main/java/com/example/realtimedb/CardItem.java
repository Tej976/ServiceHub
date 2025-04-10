package com.example.realtimedb;

public class CardItem {
    private String title;
    private int actionType; // Define different actions (1 = open camera, 2 = open web, etc.)

    public CardItem(String title, int actionType) {
        this.title = title;
        this.actionType = actionType;
    }

    public String getTitle() {
        return title;
    }

    public int getActionType() {
        return actionType;
    }
}
