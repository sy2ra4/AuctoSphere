package com.university.auctionsystem.shared.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private int messageId;
    private int auctionId;
    private int senderId;
    private String senderUsername;
    private int receiverId;
    private String receiverUsername;
    private String messageText;
    private Timestamp timestamp;
    private boolean isRead;
    private String auctionItemName;

    public Message() {}

    public Message(int auctionId, int senderId, int receiverId, String messageText) {
        this.auctionId = auctionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
    }

    public int getMessageId() {
        return messageId;
    }
    public void setMessageId(int messageId) { this.messageId = messageId; }
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getAuctionItemName() { return auctionItemName; }
    public void setAuctionItemName(String auctionItemName) { this.auctionItemName = auctionItemName; }

    @Override
    public String toString() {
        return "Message from " + senderUsername + " about '" + auctionItemName + "': " + messageText.substring(0, Math.min(30, messageText.length())) + "...";
    }
}