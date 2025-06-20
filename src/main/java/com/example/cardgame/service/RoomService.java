package com.example.cardgame.service;

import com.example.cardgame.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private Map<String, Room> rooms = new ConcurrentHashMap<>();
    private Map<String, Set<String>> roomReadyPlayers = new ConcurrentHashMap<>();
    
    public Room createRoom() {
        String roomId = String.valueOf(new Random().nextInt(900000) + 100000);
        Room room = new Room(roomId);
        rooms.put(roomId, room);
        return room;
    }
    
    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }
    
    public Collection<Room> getAllRooms() {
        return rooms.values();
    }
    
    // 添加玩家到房间
    public boolean addPlayerToRoom(String roomId, Player player) {
        Room room = getRoom(roomId);
        if (room == null) {
            return false;
        }
        return room.addPlayer(player);
    }
    
    // 从房间移除玩家
    public boolean removePlayerFromRoom(String roomId, String playerId) {
        Room room = getRoom(roomId);
        if (room == null) {
            return false;
        }
        
        List<Player> players = room.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayerId().equals(playerId)) {
                players.remove(i);
                return true;
            }
        }
        
        return false;
    }
    
    // 检查房间是否已满
    public boolean isRoomFull(String roomId) {
        Room room = getRoom(roomId);
        return room != null && room.getPlayers().size() >= 3;
    }
    
    // 检查房间是否已开始游戏
    public boolean isGameStarted(String roomId) {
        Room room = getRoom(roomId);
        return room != null && room.isStarted();
    }
    
    // 开始新游戏
    public Game startNewGame(String roomId) {
        Room room = getRoom(roomId);
        if (room == null || room.getPlayers().size() < 3) {
            return null;
        }
        
        Game game = new Game(room.getPlayers());
        room.setGame(game);
        room.setStarted(true);
        return game;
    }
    
    // 重置房间游戏
    public void resetGame(String roomId) {
        Room room = getRoom(roomId);
        if (room != null) {
            room.setGame(null);
            room.setStarted(false);
        }
    }
    
    // 玩家准备
    public void playerReady(String roomId, String playerId) {
        roomReadyPlayers.putIfAbsent(roomId, new HashSet<>());
        roomReadyPlayers.get(roomId).add(playerId);
    }
    
    // 检查是否所有玩家都准备好
    public boolean areAllPlayersReady(String roomId) {
        Room room = getRoom(roomId);
        Set<String> readyPlayers = roomReadyPlayers.getOrDefault(roomId, new HashSet<>());
        
        return room != null && readyPlayers.size() == room.getPlayers().size();
    }
    
    // 获取已准备玩家数量
    public int getReadyPlayersCount(String roomId) {
        return roomReadyPlayers.getOrDefault(roomId, new HashSet<>()).size();
    }
    
    // 重置准备状态
    public void resetReadyStatus(String roomId) {
        roomReadyPlayers.put(roomId, new HashSet<>());
    }
}
