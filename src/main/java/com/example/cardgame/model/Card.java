package com.example.cardgame.model;

/**
 * 卡牌类
 * 表示一张扑克牌，包含花色和点数
 */
public class Card {
    private String suit; // 花色: ♠, ♥, ♣, ♦
    private String rank; // 点数: 3, 4, 5, 6, 7, 8, 9, 10, J, Q, K, A, 2

    /**
     * 构造一张卡牌
     * @param suit 花色
     * @param rank 点数
     */
    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }
    
    /**
     * 获取花色
     * @return 花色符号
     */
    public String getSuit() { 
        return suit; 
    }
    
    /**
     * 获取点数
     * @return 点数值
     */
    public String getRank() { 
        return rank; 
    }
    
    /**
     * 将卡牌转换为字符串形式
     * @return 花色+点数的字符串表示
     */
    public String toString() { 
        return suit + rank; 
    }
    
    /**
     * 判断两张卡牌是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Card card = (Card) obj;
        return suit.equals(card.suit) && rank.equals(card.rank);
    }
    
    /**
     * 生成哈希码
     */
    @Override
    public int hashCode() {
        return 31 * suit.hashCode() + rank.hashCode();
    }
}
