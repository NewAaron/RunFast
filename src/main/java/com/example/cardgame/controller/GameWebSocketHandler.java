package com.example.cardgame.controller;

import com.example.cardgame.model.*;
import com.example.cardgame.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private RoomService roomService;
    
    private Map<String, Player> sessionPlayerMap = new ConcurrentHashMap<>();
    private Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // 记录房间准备状态
    private Map<String, Set<String>> roomReadyMap = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessionMap.put(session.getId(), session);
        // 连接建立后等待前端指令
    }
    
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) msg.get("action");
            String inputPlayerId = (String) msg.get("playerId");
            
            switch(action) {
                case "createRoom":
                    handleCreateRoom(session, inputPlayerId);
                    break;
                case "joinRoom":
                    handleJoinRoom(session, msg);
                    break;
                case "play":
                    handlePlayCards(session, msg);
                    break;
                case "pass":
                    handlePass(session);
                    break;
                case "ready":
                    handleReady(session);
                    break;
                case "getRooms":
                    handleGetRooms(session);
                    break;
                case "leaveRoom":
                    handleLeaveRoom(session);
                    break;
                default:
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "error");
                    response.put("msg", "未知操作");
                    sendToSession(session, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Map<String, Object> errorMsg = new HashMap<>();
                errorMsg.put("type", "error");
                errorMsg.put("msg", "操作失败: " + e.getMessage());
                sendToSession(session, errorMsg);
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            }
        }
    }
    
    private void handleCreateRoom(WebSocketSession session, String inputPlayerId) throws IOException {
        Room room = roomService.createRoom();
        Player player = inputPlayerId != null && !inputPlayerId.isEmpty() ? new Player(inputPlayerId, inputPlayerId) : new Player(session.getId());
        
        room.addPlayer(player);
        sessionPlayerMap.put(session.getId(), player);
        sessionRoomMap.put(session.getId(), room.getRoomId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "room");
        response.put("roomId", room.getRoomId());
        response.put("playerId", player.getPlayerId());
        sendToSession(session, response);
    }
    
    private void handleJoinRoom(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = (String) msg.get("roomId");
        String inputPlayerId = (String) msg.get("playerId");
        Room room = roomService.getRoom(roomId);
        
        if (room != null && room.getPlayers().size() < 3) {
            Player player = inputPlayerId != null && !inputPlayerId.isEmpty() ? 
                new Player(inputPlayerId, inputPlayerId) : new Player(session.getId());
            
            room.addPlayer(player);
            sessionPlayerMap.put(session.getId(), player);
            sessionRoomMap.put(session.getId(), roomId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "room");
            response.put("roomId", roomId);
            response.put("playerId", player.getPlayerId());
            sendToSession(session, response);
            
            // 发送玩家列表更新
            broadcastPlayerList(roomId);
            
            // 检查房间是否已满，满则开始游戏
            checkRoomAndStartGameIfFull(roomId);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "status");
            response.put("msg", "房间不存在或已满");
            sendToSession(session, response);
        }
    }
    
    private void checkRoomAndStartGameIfFull(String roomId) throws IOException {
        Room room = roomService.getRoom(roomId);
        if (room != null && room.getPlayers().size() == 3 && room.getGame() == null) {
            Game game = new Game(room.getPlayers());
            room.setGame(game);
            room.setStarted(true);
            
            // 给每个玩家推送手牌
            for (Player p : room.getPlayers()) {
                for (Map.Entry<String, Player> entry : sessionPlayerMap.entrySet()) {
                    if (entry.getValue().getPlayerId().equals(p.getPlayerId())) {
                        WebSocketSession s = sessionMap.get(entry.getKey());
                        if (s != null && s.isOpen()) {
                            List<Card> hand = game.getHand(p.getPlayerId());
                            Map<String, Object> handMsg = new HashMap<>();
                            handMsg.put("type", "hand");
                            handMsg.put("cards", hand.stream().map(Card::toString).toArray());
                            sendToSession(s, handMsg);
                        }
                    }
                }
            }
            
            // 广播游戏开始
            Map<String, Object> startMsg = new HashMap<>();
            startMsg.put("type", "gameStart");
            startMsg.put("msg", "游戏开始!");
            broadcastToRoom(roomId, startMsg);
            
            // 广播当前出牌者
            broadcastCurrentPlayer(roomId, game.getCurrentPlayerId());
            
            // 通知首位玩家出牌
            String firstPlayerId = game.getCurrentPlayerId();
            for (Map.Entry<String, Player> entry : sessionPlayerMap.entrySet()) {
                if (entry.getValue().getPlayerId().equals(firstPlayerId)) {
                    WebSocketSession s = sessionMap.get(entry.getKey());
                    if (s != null && s.isOpen()) {
                        Map<String, Object> statusMsg = new HashMap<>();
                        statusMsg.put("type", "status");
                        statusMsg.put("msg", "请出包含♠3的首牌");
                        sendToSession(s, statusMsg);
                    }
                }
            }
        }
    }
    
    private void handlePlayCards(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String roomId = sessionRoomMap.get(session.getId());
        Room room = roomService.getRoom(roomId);
        
        if (room == null || room.getGame() == null) {
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("type", "error");
            errorMsg.put("msg", "游戏未开始");
            sendToSession(session, errorMsg);
            return;
        }
        
        Game game = room.getGame();
        Player player = sessionPlayerMap.get(session.getId());
        
        if (!game.getCurrentPlayerId().equals(player.getPlayerId())) {
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "status");
            statusMsg.put("msg", "还没轮到你出牌");
            sendToSession(session, statusMsg);
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<String> cardStrs = (List<String>) msg.get("cards");
        List<Card> hand = game.getHand(player.getPlayerId());
        List<Card> played = new ArrayList<>();
        
        // 打印收到的卡牌和手牌信息
        System.out.println("收到前端出牌请求，卡牌数量: " + cardStrs.size());
        System.out.println("前端发送的卡牌:");
        for (String s : cardStrs) {
            System.out.println(" - " + s);
        }
        
        System.out.println("后端记录的玩家手牌:");
        for (Card c : hand) {
            System.out.println(" - " + c.toString() + " [花色:" + c.getSuit() + ", 点数:" + c.getRank() + "]");
        }
        
        // 更精确的卡牌匹配算法
        for (String s : cardStrs) {
            System.out.println("处理前端卡牌: " + s);
            
            // 1. 尝试直接匹配
            boolean found = false;
            for (Card c : hand) {
                if (c.toString().equals(s) && !played.contains(c)) {
                    played.add(c);
                    System.out.println("直接匹配到手牌: " + c.toString());
                    found = true;
                    break;
                }
            }
            
            // 2. 如果直接匹配失败，尝试使用正则表达式解析
            if (!found) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([♠♥♣♦])([2-9JQKA]|10)");
                java.util.regex.Matcher matcher = pattern.matcher(s);
                
                if (matcher.find()) {
                    String suit = matcher.group(1);  // 花色
                    String rank = matcher.group(2);  // 点数
                    
                    System.out.println("正则解析结果 - 花色: " + suit + ", 点数: " + rank);
                    
                    // 尝试在手牌中找到匹配的卡牌
                    for (Card c : hand) {
                        if (c.getSuit().equals(suit) && c.getRank().equals(rank) && !played.contains(c)) {
                            played.add(c);
                            System.out.println("匹配到手牌: " + c.toString() + " [花色:" + c.getSuit() + ", 点数:" + c.getRank() + "]");
                            found = true;
                            break;
                        }
                    }
                }
            }
            
            if (!found) {
                System.out.println("警告：无法匹配到卡牌 " + s);
            }
        }
        
        // 如果匹配到的牌数量与前端发送的不一致，返回错误
        if (played.size() != cardStrs.size()) {
            System.out.println("错误：匹配到的牌数量(" + played.size() + ")与前端发送的牌数量(" + cardStrs.size() + ")不一致");
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "status");
            statusMsg.put("msg", "选择的牌不符合要求，请重新选择");
            sendToSession(session, statusMsg);
            return;
        }
        
        // 打印最终结果
        System.out.println("最终匹配到的牌数量: " + played.size());
        for (Card c : played) {
            System.out.println("最终牌: " + c.toString() + " [花色:" + c.getSuit() + ", 点数:" + c.getRank() + "]");
        }
        
        // 校验首轮首出必须包含♠3
        if (game.isFirstTrick()) {
            // 检查是否包含黑桃3，同时输出日志方便调试
            boolean hasS3 = false;
            System.out.println("首轮出牌检查，出牌数量: " + played.size());
            
            // 检查并打印玩家手牌中是否有黑桃3
            boolean playerHasS3 = false;
            for (Card c : hand) {
                if (c.getSuit().equals("♠") && c.getRank().equals("3")) {
                    playerHasS3 = true;
                    System.out.println("玩家手牌中有黑桃3");
                    break;
                }
            }
            
            // 检查准备出的牌中是否包含黑桃3
            for (Card c : played) {
                System.out.println("检查牌: " + c.toString() + ", 花色: " + c.getSuit() + ", 点数: " + c.getRank());
                // 精确比较花色和点数
                if ("♠".equals(c.getSuit()) && "3".equals(c.getRank())) {
                    hasS3 = true;
                    System.out.println("找到黑桃3!");
                    break;
                }
            }
            
            if (!hasS3 && playerHasS3) {
                Map<String, Object> statusMsg = new HashMap<>();
                statusMsg.put("type", "status");
                statusMsg.put("msg", "首轮首出必须包含♠3");
                sendToSession(session, statusMsg);
                return;
            }
        }
        
        // 牌型与规则校验
        Game.CardType playedType = game.judgeType(played);
        if (playedType == Game.CardType.INVALID) {
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "status");
            statusMsg.put("msg", "不支持的牌型");
            sendToSession(session, statusMsg);
            return;
        }
        
        // 检查是否合法出牌
        List<Card> lastCards = game.getLastPlayedCards();
        String lastPlayerId = game.getLastPlayedPlayerId();
        
        if (lastPlayerId != null && !lastCards.isEmpty()) {
            Game.CardType lastType = game.judgeType(lastCards);
            
            if (playedType != lastType && playedType != Game.CardType.BOMB) {
                Map<String, Object> statusMsg = new HashMap<>();
                statusMsg.put("type", "status");
                statusMsg.put("msg", "必须出相同牌型或炸弹");
                sendToSession(session, statusMsg);
                return;
            }
            
            if (playedType == lastType && !isBigger(played, lastCards, playedType)) {
                Map<String, Object> statusMsg = new HashMap<>();
                statusMsg.put("type", "status");
                statusMsg.put("msg", "牌型不够大");
                sendToSession(session, statusMsg);
                return;
            }
        }
        
        // 移除手牌
        if (!hand.containsAll(played)) {
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "status");
            statusMsg.put("msg", "手牌不合法");
            sendToSession(session, statusMsg);
            return;
        }
        
        hand.removeAll(played);
        game.setLastPlayedCards(played);
        game.setLastPlayedPlayerId(player.getPlayerId());
        
        // 广播桌面牌
        Map<String, Object> tableMsg = new HashMap<>();
        tableMsg.put("type", "table");
        tableMsg.put("playerId", player.getPlayerId());
        tableMsg.put("cards", played.stream().map(Card::toString).toArray());
        broadcastToRoom(roomId, tableMsg);
        
        // 检查胜负
        if (hand.isEmpty()) {
            // 游戏结束，广播结果
            broadcastGameOver(roomId, player.getPlayerId());
            return;
        }
        
        // 轮到下家
        game.nextPlayer();
        
        // 广播当前出牌人ID
        broadcastCurrentPlayer(roomId, game.getCurrentPlayerId());
    }
    
    private void handlePass(WebSocketSession session) throws IOException {
        String roomId = sessionRoomMap.get(session.getId());
        Room room = roomService.getRoom(roomId);
        
        if (room == null || room.getGame() == null) {
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("type", "error");
            errorMsg.put("msg", "游戏未开始");
            sendToSession(session, errorMsg);
            return;
        }
        
        Game game = room.getGame();
        Player player = sessionPlayerMap.get(session.getId());
        
        if (!game.getCurrentPlayerId().equals(player.getPlayerId())) {
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "status");
            statusMsg.put("msg", "还没轮到你");
            sendToSession(session, statusMsg);
            return;
        }
        
        // Pass限制：如果当前是新一轮，不能pass
        if (game.getLastPlayedCards().isEmpty() && game.getLastPlayedPlayerId() == null) {
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "status");
            statusMsg.put("msg", "新一轮不能PASS，必须先出牌");
            sendToSession(session, statusMsg);
            return;
        }
        
        game.pass();
        
        // 广播Pass信息
        Map<String, Object> passMsg = new HashMap<>();
        passMsg.put("type", "pass");
        passMsg.put("playerId", player.getPlayerId());
        broadcastToRoom(roomId, passMsg);
        
        // 轮到下家
        game.nextPlayer();
        
        // 广播当前出牌人
        broadcastCurrentPlayer(roomId, game.getCurrentPlayerId());
    }
    
    private void handleReady(WebSocketSession session) throws IOException {
        String roomId = sessionRoomMap.get(session.getId());
        if (roomId == null) {
            return;
        }
        
        Player player = sessionPlayerMap.get(session.getId());
        if (player == null) {
            return;
        }
        
        roomReadyMap.putIfAbsent(roomId, new HashSet<>());
        roomReadyMap.get(roomId).add(player.getPlayerId());
        
        // 广播当前准备人数
        int readyCount = roomReadyMap.get(roomId).size();
        Map<String, Object> readyMsg = new HashMap<>();
        readyMsg.put("type", "ready");
        readyMsg.put("count", readyCount);
        readyMsg.put("playerId", player.getPlayerId());
        broadcastToRoom(roomId, readyMsg);
        
        // 三人都准备，重开一局
        Room room = roomService.getRoom(roomId);
        if (room != null && readyCount == 3) {
            // 重置游戏
            room.setGame(new Game(room.getPlayers()));
            room.setStarted(true);
            
            // 清空准备状态
            roomReadyMap.put(roomId, new HashSet<>());
            
            // 广播游戏重新开始
            Map<String, Object> restartMsg = new HashMap<>();
            restartMsg.put("type", "gameRestart");
            restartMsg.put("msg", "新一局游戏开始!");
            broadcastToRoom(roomId, restartMsg);
            
            // 给每个玩家推送新手牌
            for (Player p : room.getPlayers()) {
                for (Map.Entry<String, Player> entry : sessionPlayerMap.entrySet()) {
                    if (entry.getValue().getPlayerId().equals(p.getPlayerId())) {
                        WebSocketSession s = sessionMap.get(entry.getKey());
                        if (s != null && s.isOpen()) {
                            List<Card> hand = room.getGame().getHand(p.getPlayerId());
                            Map<String, Object> handMsg = new HashMap<>();
                            handMsg.put("type", "hand");
                            handMsg.put("cards", hand.stream().map(Card::toString).toArray());
                            sendToSession(s, handMsg);
                        }
                    }
                }
            }
            
            // 广播当前出牌者
            broadcastCurrentPlayer(roomId, room.getGame().getCurrentPlayerId());
            
            // 通知首位玩家出牌
            String firstPlayerId = room.getGame().getCurrentPlayerId();
            for (Map.Entry<String, Player> entry : sessionPlayerMap.entrySet()) {
                if (entry.getValue().getPlayerId().equals(firstPlayerId) && roomId.equals(sessionRoomMap.get(entry.getKey()))) {
                    WebSocketSession s = sessionMap.get(entry.getKey());
                    if (s != null && s.isOpen()) {
                        Map<String, Object> statusMsg = new HashMap<>();
                        statusMsg.put("type", "status");
                        statusMsg.put("msg", "请出包含♠3的首牌");
                        sendToSession(s, statusMsg);
                    }
                }
            }
        }
    }
    
    private void handleGetRooms(WebSocketSession session) throws IOException {
        Collection<Room> allRooms = roomService.getAllRooms();
        List<Map<String, Object>> roomsInfo = new ArrayList<>();
        
        for (Room room : allRooms) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", room.getRoomId());
            roomInfo.put("playerCount", room.getPlayers().size());
            roomInfo.put("started", room.isStarted());
            roomsInfo.add(roomInfo);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "roomList");
        response.put("rooms", roomsInfo);
        sendToSession(session, response);
    }
    
    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        String roomId = sessionRoomMap.get(session.getId());
        if (roomId == null) {
            return;
        }
        
        Player player = sessionPlayerMap.get(session.getId());
        if (player == null) {
            return;
        }
        
        Room room = roomService.getRoom(roomId);
        if (room != null) {
            // 移除玩家
            room.getPlayers().removeIf(p -> p.getPlayerId().equals(player.getPlayerId()));
            
            // 移除准备状态
            if (roomReadyMap.containsKey(roomId)) {
                roomReadyMap.get(roomId).remove(player.getPlayerId());
            }
            
            // 通知其他玩家
            Map<String, Object> leftMsg = new HashMap<>();
            leftMsg.put("type", "playerLeft");
            leftMsg.put("playerId", player.getPlayerId());
            broadcastToRoom(roomId, leftMsg);
            
            // 如果房间为空，删除房间
            if (room.getPlayers().isEmpty()) {
                roomService.getRoom(roomId);
                roomReadyMap.remove(roomId);
            }
        }
        
        // 清理映射
        sessionRoomMap.remove(session.getId());
        sessionPlayerMap.remove(session.getId());
        
        // 通知离开成功
        Map<String, Object> leaveMsg = new HashMap<>();
        leaveMsg.put("type", "leaveRoom");
        leaveMsg.put("success", true);
        sendToSession(session, leaveMsg);
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String roomId = sessionRoomMap.get(session.getId());
        Player player = sessionPlayerMap.get(session.getId());
        
        if (roomId != null && player != null) {
            Room room = roomService.getRoom(roomId);
            if (room != null) {
                // 移除玩家
                room.getPlayers().removeIf(p -> p.getPlayerId().equals(player.getPlayerId()));
                
                // 如果有准备状态，移除
                if (roomReadyMap.containsKey(roomId)) {
                    roomReadyMap.get(roomId).remove(player.getPlayerId());
                }
                
                try {
                    // 通知其他玩家
                    Map<String, Object> disconnectMsg = new HashMap<>();
                    disconnectMsg.put("type", "playerDisconnected");
                    disconnectMsg.put("playerId", player.getPlayerId());
                    broadcastToRoom(roomId, disconnectMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                // 如果房间为空，删除房间
                if (room.getPlayers().isEmpty()) {
                    roomReadyMap.remove(roomId);
                }
            }
        }
        
        // 清理映射
        sessionPlayerMap.remove(session.getId());
        sessionRoomMap.remove(session.getId());
        sessionMap.remove(session.getId());
    }
    
    // 发送消息到单个会话
    private void sendToSession(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        }
    }
    
    // 广播消息到整个房间
    private void broadcastToRoom(String roomId, Map<String, Object> data) throws IOException {
        for (Map.Entry<String, String> entry : sessionRoomMap.entrySet()) {
            if (roomId.equals(entry.getValue())) {
                WebSocketSession s = sessionMap.get(entry.getKey());
                if (s != null && s.isOpen()) {
                    sendToSession(s, data);
                }
            }
        }
    }
    
    // 广播当前出牌玩家
    private void broadcastCurrentPlayer(String roomId, String currentPlayerId) throws IOException {
        Map<String, Object> currentPlayerMsg = new HashMap<>();
        currentPlayerMsg.put("type", "currentPlayer");
        currentPlayerMsg.put("playerId", currentPlayerId);
        broadcastToRoom(roomId, currentPlayerMsg);
    }
    
    // 广播游戏结束
    private void broadcastGameOver(String roomId, String winnerId) throws IOException {
        Room room = roomService.getRoom(roomId);
        if (room == null || room.getGame() == null) {
            return;
        }
        
        Game game = room.getGame();
        
        for (Player p : room.getPlayers()) {
            for (Map.Entry<String, Player> entry : sessionPlayerMap.entrySet()) {
                if (entry.getValue().getPlayerId().equals(p.getPlayerId())) {
                    WebSocketSession s = sessionMap.get(entry.getKey());
                    if (s != null && s.isOpen()) {
                        Map<String, Object> statusMsg = new HashMap<>();
                        statusMsg.put("type", "status");
                        
                        if (p.getPlayerId().equals(winnerId)) {
                            statusMsg.put("msg", "你赢了！");
                        } else {
                            List<Card> loserHand = game.getHand(p.getPlayerId());
                            int left = loserHand != null ? loserHand.size() : 0;
                            statusMsg.put("msg", "你输了！剩余" + left + "张牌");
                        }
                        
                        sendToSession(s, statusMsg);
                    }
                }
            }
        }
        
        // 显示准备区域
        Map<String, Object> gameOverMsg = new HashMap<>();
        gameOverMsg.put("type", "gameOver");
        gameOverMsg.put("winnerId", winnerId);
        broadcastToRoom(roomId, gameOverMsg);
        
        // 游戏结束，重置游戏但保留玩家
        room.setStarted(false);
        room.setGame(null);
    }
    
    // 广播玩家列表
    private void broadcastPlayerList(String roomId) throws IOException {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            return;
        }
        
        List<Map<String, String>> playerInfoList = new ArrayList<>();
        for (Player p : room.getPlayers()) {
            Map<String, String> playerInfo = new HashMap<>();
            playerInfo.put("id", p.getPlayerId());
            playerInfo.put("name", p.getName());
            playerInfoList.add(playerInfo);
        }
        
        Map<String, Object> playerListMsg = new HashMap<>();
        playerListMsg.put("type", "playerList");
        playerListMsg.put("players", playerInfoList);
        broadcastToRoom(roomId, playerListMsg);
    }
    
    // 牌型大小比较
    private boolean isBigger(List<Card> played, List<Card> last, Game.CardType type) {
        if (type == Game.CardType.BOMB) {
            return rankValue(played.get(0).getRank()) > rankValue(last.get(0).getRank());
        }
        if (type == Game.CardType.STRAIGHT_FLUSH || type == Game.CardType.STRAIGHT) {
            return played.size() == last.size() &&
                rankValue(maxRank(played)) > rankValue(maxRank(last));
        }
        if (type == Game.CardType.PAIR_SEQUENCE) {
            return played.size() == last.size() &&
                rankValue(maxRank(played)) > rankValue(maxRank(last));
        }
        if (type == Game.CardType.FULL_HOUSE || type == Game.CardType.TRIPLE) {
            return rankValue(maxTripleRank(played)) > rankValue(maxTripleRank(last));
        }
        if (type == Game.CardType.PAIR) {
            return rankValue(played.get(0).getRank()) > rankValue(last.get(0).getRank());
        }
        if (type == Game.CardType.SINGLE) {
            return rankValue(played.get(0).getRank()) > rankValue(last.get(0).getRank());
        }
        return false;
    }
    
    private int rankValue(String rank) {
        switch(rank) {
            case "3": return 3; case "4": return 4; case "5": return 5; case "6": return 6;
            case "7": return 7; case "8": return 8; case "9": return 9; case "10": return 10;
            case "J": return 11; case "Q": return 12; case "K": return 13; case "A": return 14; case "2": return 15;
            default: return 0;
        }
    }
    
    private String maxRank(List<Card> cards) {
        return cards.stream().max(Comparator.comparingInt(c -> rankValue(c.getRank()))).get().getRank();
    }
    
    private String maxTripleRank(List<Card> cards) {
        Map<String, Integer> map = new HashMap<>();
        for (Card c : cards) map.put(c.getRank(), map.getOrDefault(c.getRank(),0)+1);
        return map.entrySet().stream().filter(e -> e.getValue() >= 3).max(Comparator.comparingInt(e -> rankValue(e.getKey()))).get().getKey();
    }
}
