package com.university.auctionsystem.shared.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private int bidId;
    private int auctionId;
    private int bidderId;
    private String bidderUsername;
    private BigDecimal bidAmount;
    private Timestamp bidTime;
    private String auctionItemName;
    private String auctionStatus;

    public Bid() {
    }

    public Bid(int auctionId, int bidderId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    public int getBidId() { return bidId; }
    public void setBidId(int bidId) { this.bidId = bidId; }
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }
    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
    public Timestamp getBidTime() { return bidTime; }
    public void setBidTime(Timestamp bidTime) { this.bidTime = bidTime; }
    public String getAuctionItemName() { return auctionItemName; }
    public void setAuctionItemName(String auctionItemName) { this.auctionItemName = auctionItemName; }
    public String getAuctionStatus() { return auctionStatus; }
    public void setAuctionStatus(String auctionStatus) { this.auctionStatus = auctionStatus; }
}
