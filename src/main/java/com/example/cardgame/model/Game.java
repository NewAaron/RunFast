package com.example.cardgame.model;

import java.util.*;

public class Game {
    private List<Card> deck = new ArrayList<>();
    private Map<String, List<Card>> playerHands = new HashMap<>();
    private String currentPlayerId;
    private List<Card> lastPlayedCards = new ArrayList<>();
    private String lastPlayedPlayerId;
    private int passCount = 0;
    private boolean started = false;
    private boolean firstTrick = true; // 只在游戏最开始为true
    public Game(List<Player> players) {
        initDeck();
        deal(players);
        started = true;
    }
    private void initDeck() {
        String[] suits = {"♠", "♥", "♣", "♦"};
        String[] ranks = {"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"};
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(new Card(suit, rank));
            }
        }
        deck.removeIf(card -> card.getSuit().equals("JOKER")); // 无大小王
        deck.removeIf(card -> card.toString().equals("♠2")); // 去除♠2
        Collections.shuffle(deck);
    }
    private void deal(List<Player> players) {
        for (int i = 0; i < 3; i++) {
            List<Card> hand = new ArrayList<>();
            for (int j = 0; j < 17; j++) {
                hand.add(deck.get(i * 17 + j));
            }
            playerHands.put(players.get(i).getPlayerId(), hand);
        }
        // 找到有♠3的玩家先手
        for (Player p : players) {
            for (Card c : playerHands.get(p.getPlayerId())) {
                if (c.toString().equals("♠3")) {
                    currentPlayerId = p.getPlayerId();
                    return; // 修正：找到后立即返回，确保只设置一次
                }
            }
        }
        // 如果没人有♠3，默认第一个玩家先手（兜底）
        if (currentPlayerId == null && !players.isEmpty()) {
            currentPlayerId = players.get(0).getPlayerId();
        }
    }
    public List<Card> getHand(String playerId) {
        return playerHands.get(playerId);
    }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public boolean isStarted() { return started; }
    public String getLastPlayedPlayerId() { return lastPlayedPlayerId; }
    public void setLastPlayedPlayerId(String id) { this.lastPlayedPlayerId = id; }
    public void setLastPlayedCards(List<Card> cards) {
        this.lastPlayedCards = new ArrayList<>(cards);
        passCount = 0;
        if (firstTrick) firstTrick = false; // 第一手出牌后关闭
    }
    public boolean isFirstTrick() { return firstTrick; }
    public List<Card> getLastPlayedCards() {
        return new ArrayList<>(lastPlayedCards);
    }
    public void nextPlayer() {
        List<String> ids = new ArrayList<>(playerHands.keySet());
        int idx = ids.indexOf(currentPlayerId);
        currentPlayerId = ids.get((idx + 1) % 3);
    }
    public void pass() { passCount++; if (passCount >= 2) { lastPlayedCards.clear(); lastPlayedPlayerId = null; passCount = 0; } }
    // 牌型枚举
    public enum CardType {
        BOMB, STRAIGHT_FLUSH, TRIPLE_SEQUENCE_WITH_PAIRS, TRIPLE_SEQUENCE, STRAIGHT, FULL_HOUSE, TRIPLE, PAIR_SEQUENCE, PAIR, SINGLE, INVALID
    }
    // 牌型判定入口
    public CardType judgeType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return CardType.INVALID;
        // 炸弹（四张同点 或 四带一）
        if ((cards.size() == 4 && sameRank(cards)) || isFourWithOne(cards)) return CardType.BOMB;
        // 同花顺（同花色连续3张及以上）
        if (cards.size() >= 3 && sameSuit(cards) && isStraight(cards, false)) return CardType.STRAIGHT_FLUSH;
        // 连三带对（如555-666+77-88）
        if (isTripleSequenceWithPairs(cards)) return CardType.TRIPLE_SEQUENCE_WITH_PAIRS;
        // 连三（如333-444-555）
        if (isTripleSequence(cards)) return CardType.TRIPLE_SEQUENCE;
        // 普通顺子（5张及以上，不能有2）
        if (cards.size() >= 5 && isStraight(cards, true)) return CardType.STRAIGHT;
        // 三带二（如999+55）
        if (cards.size() == 5 && isFullHouse(cards)) return CardType.FULL_HOUSE;
        // 三张
        if (cards.size() == 3 && sameRank(cards)) return CardType.TRIPLE;
        // 连对（2组及以上连续对子）
        if (cards.size() >= 4 && isPairSequence(cards)) return CardType.PAIR_SEQUENCE;
        // 对子
        if (cards.size() == 2 && sameRank(cards)) return CardType.PAIR;
        // 单张
        if (cards.size() == 1) return CardType.SINGLE;
        return CardType.INVALID;
    }
    // 辅助方法
    private boolean sameRank(List<Card> cards) {
        String r = cards.get(0).getRank();
        for (Card c : cards) if (!c.getRank().equals(r)) return false;
        return true;
    }
    private boolean sameSuit(List<Card> cards) {
        String s = cards.get(0).getSuit();
        for (Card c : cards) if (!c.getSuit().equals(s)) return false;
        return true;
    }
    private boolean isStraight(List<Card> cards, boolean exclude2) {
        List<Integer> vals = new ArrayList<>();
        for (Card c : cards) {
            if (exclude2 && c.getRank().equals("2")) return false;
            vals.add(rankValue(c.getRank()));
        }
        Collections.sort(vals);
        for (int i = 1; i < vals.size(); i++) if (vals.get(i) != vals.get(i-1)+1) return false;
        return true;
    }
    private boolean isFullHouse(List<Card> cards) {
        Map<String, Integer> map = new HashMap<>();
        for (Card c : cards) map.put(c.getRank(), map.getOrDefault(c.getRank(),0)+1);
        return map.size() == 2 && map.containsValue(3) && map.containsValue(2);
    }
    private boolean isPairSequence(List<Card> cards) {
        if (cards.size() % 2 != 0) return false;
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparingInt(c -> rankValue(c.getRank())));
        for (int i = 0; i < sorted.size(); i += 2) {
            if (!sorted.get(i).getRank().equals(sorted.get(i+1).getRank())) return false;
            if (i > 0 && rankValue(sorted.get(i).getRank()) != rankValue(sorted.get(i-2).getRank())+1) return false;
        }
        return true;
    }
    private boolean isTripleSequenceWithPairs(List<Card> cards) {
        // 连三带对：n组连续的三张 + n组对子
        // 最少需要10张牌（2组三张6张 + 2组对子4张）
        if (cards.size() < 10 || cards.size() % 5 != 0) return false;
        
        Map<String, Integer> rankCount = new HashMap<>();
        for (Card c : cards) {
            rankCount.put(c.getRank(), rankCount.getOrDefault(c.getRank(), 0) + 1);
        }
        
        // 统计各种牌型数量
        List<String> triples = new ArrayList<>();
        List<String> pairs = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                triples.add(entry.getKey());
            } else if (entry.getValue() == 2) {
                pairs.add(entry.getKey());
            } else if (entry.getValue() != 0) {
                // 如果有其他数量的牌，则不是连三带对
                return false;
            }
        }
        
        // 必须有相同数量的三张和对子，且至少2组
        if (triples.size() < 2 || triples.size() != pairs.size()) {
            return false;
        }
        
        // 检查三张是否连续
        triples.sort(Comparator.comparingInt(this::rankValue));
        for (int i = 1; i < triples.size(); i++) {
            if (rankValue(triples.get(i)) != rankValue(triples.get(i-1)) + 1) {
                return false;
            }
        }
        
        return true;
    }
    private boolean isTripleSequence(List<Card> cards) {
        // 连三：n组连续的三张，最少需要6张牌（2组三张）
        if (cards.size() < 6 || cards.size() % 3 != 0) return false;
        
        Map<String, Integer> rankCount = new HashMap<>();
        for (Card c : cards) {
            rankCount.put(c.getRank(), rankCount.getOrDefault(c.getRank(), 0) + 1);
        }
        
        // 统计三张的数量
        List<String> triples = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                triples.add(entry.getKey());
            } else if (entry.getValue() != 0) {
                // 如果有其他数量的牌，则不是连三
                return false;
            }
        }
        
        // 必须至少有2组三张
        if (triples.size() < 2) {
            return false;
        }
        
        // 检查三张是否连续
        triples.sort(Comparator.comparingInt(this::rankValue));
        for (int i = 1; i < triples.size(); i++) {
            if (rankValue(triples.get(i)) != rankValue(triples.get(i-1)) + 1) {
                return false;
            }
        }
        
        return true;
    }
    private boolean isFourWithOne(List<Card> cards) {
        if (cards.size() != 5) return false;
        Map<String, Integer> map = new HashMap<>();
        for (Card c : cards) map.put(c.getRank(), map.getOrDefault(c.getRank(),0)+1);
        return map.size() == 2 && (map.containsValue(4) && map.containsValue(1));
    }
    private int rankValue(String rank) {
        switch(rank) {
            case "3": return 3; case "4": return 4; case "5": return 5; case "6": return 6;
            case "7": return 7; case "8": return 8; case "9": return 9; case "10": return 10;
            case "J": return 11; case "Q": return 12; case "K": return 13; case "A": return 14; case "2": return 15;
            default: return 0;
        }
    }
    
    // 添加游戏结束检查方法
    public boolean isGameOver() {
        for (String playerId : playerHands.keySet()) {
            if (playerHands.get(playerId).isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    // 获取赢家ID
    public String getWinnerPlayerId() {
        for (String playerId : playerHands.keySet()) {
            if (playerHands.get(playerId).isEmpty()) {
                return playerId;
            }
        }
        return null;
    }
    
    // 比较牌型大小的公共方法
    public boolean compareCards(List<Card> newCards, List<Card> oldCards) {
        CardType newType = judgeType(newCards);
        CardType oldType = judgeType(oldCards);
        
        // 炸弹可以压任何牌
        if (newType == CardType.BOMB && oldType != CardType.BOMB) {
            return true;
        }
        
        // 同类型比较
        if (newType == oldType) {
            return compareCardsOfSameType(newCards, oldCards, newType);
        }
        
        return false;
    }
    
    private boolean compareCardsOfSameType(List<Card> newCards, List<Card> oldCards, CardType type) {
        switch (type) {
            case BOMB:
                // 炸弹比较点数
                return rankValue(newCards.get(0).getRank()) > rankValue(oldCards.get(0).getRank());
            case STRAIGHT_FLUSH:
            case STRAIGHT:
                // 顺子比较最大牌
                if (newCards.size() != oldCards.size()) return false;
                return getMaxRankValue(newCards) > getMaxRankValue(oldCards);
            case TRIPLE_SEQUENCE_WITH_PAIRS:
                // 连三带对比较三张的最大点，且张数必须相同
                if (newCards.size() != oldCards.size()) return false;
                return getMaxTripleRankValue(newCards) > getMaxTripleRankValue(oldCards);
            case TRIPLE_SEQUENCE:
                // 连三比较三张的最大点，且张数必须相同
                if (newCards.size() != oldCards.size()) return false;
                return getMaxTripleRankValue(newCards) > getMaxTripleRankValue(oldCards);
            case FULL_HOUSE:
                // 三带二比较三张的点数
                return getTripleRankValue(newCards) > getTripleRankValue(oldCards);
            case TRIPLE:
                // 三张比较点数
                return rankValue(newCards.get(0).getRank()) > rankValue(oldCards.get(0).getRank());
            case PAIR_SEQUENCE:
                // 连对比较最大对子
                if (newCards.size() != oldCards.size()) return false;
                return getMaxPairRankValue(newCards) > getMaxPairRankValue(oldCards);
            case PAIR:
                // 对子比较点数
                return rankValue(newCards.get(0).getRank()) > rankValue(oldCards.get(0).getRank());
            case SINGLE:
                // 单张比较点数
                return rankValue(newCards.get(0).getRank()) > rankValue(oldCards.get(0).getRank());
            default:
                return false;
        }
    }
    
    // 获取最大点数的辅助方法
    private int getMaxRankValue(List<Card> cards) {
        return cards.stream().mapToInt(c -> rankValue(c.getRank())).max().orElse(0);
    }
    
    // 获取三张相同点数的辅助方法
    private int getTripleRankValue(List<Card> cards) {
        Map<String, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                return rankValue(entry.getKey());
            }
        }
        
        return 0;
    }
    
    // 获取最大三张的点数
    private int getMaxTripleRankValue(List<Card> cards) {
        Map<String, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        
        int maxRank = 0;
        for (Map.Entry<String, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 3) {
                maxRank = Math.max(maxRank, rankValue(entry.getKey()));
            }
        }
        
        return maxRank;
    }
    
    // 获取最大对子的点数
    private int getMaxPairRankValue(List<Card> cards) {
        Map<String, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.put(card.getRank(), rankCount.getOrDefault(card.getRank(), 0) + 1);
        }
        
        int maxRank = 0;
        for (Map.Entry<String, Integer> entry : rankCount.entrySet()) {
            if (entry.getValue() == 2) {
                maxRank = Math.max(maxRank, rankValue(entry.getKey()));
            }
        }
        
        return maxRank;
    }
}
