package com.example.cardgame.model;

import java.util.UUID;

public class Player {
    private String playerId;
    private String name;
    public Player(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
    }
    public Player(String name) {
        this.playerId = java.util.UUID.randomUUID().toString();
        this.name = name;
    }
    public String getPlayerId() { return playerId; }
    public String getName() { return name; }
}
