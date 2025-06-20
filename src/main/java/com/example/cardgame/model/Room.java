package com.example.cardgame.model;

import java.util.*;

public class Room {
    private String roomId;
    private List<Player> players = new ArrayList<>();
    private boolean started = false;
    private Game game;
    public Room(String roomId) {
        this.roomId = roomId;
    }
    public String getRoomId() { return roomId; }
    public List<Player> getPlayers() { return players; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    public boolean addPlayer(Player player) {
        if (players.size() < 3) {
            players.add(player);
            return true;
        }
        return false;
    }
}
