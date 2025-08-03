package com.university.auctionsystem.shared.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum AuctionStatus { UPCOMING, ACTIVE, ENDED, CANCELLED }
    public enum PaymentStatus { PENDING, PAID, FAILED, REFUNDED }

    private int auctionId;
    private int itemId;
    private Item item;
    private Timestamp startTime;
    private Timestamp endTime;
    private BigDecimal startPrice;
    private BigDecimal reservePrice;
    private BigDecimal currentHighestBid;
    private int winningBidderId;
    private AuctionStatus status;
    private Timestamp createdAt;
    private PaymentStatus paymentStatus;

    public Auction() {
        this.currentHighestBid = BigDecimal.ZERO;
        this.status = AuctionStatus.UPCOMING;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public BigDecimal getStartPrice() { return startPrice; }
    public void setStartPrice(BigDecimal startPrice) { this.startPrice = startPrice; }
    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }
    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    public int getWinningBidderId() { return winningBidderId; }
    public void setWinningBidderId(int winningBidderId) { this.winningBidderId = winningBidderId; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus;}

    public long getTimeRemainingMillis() {
        if (status == AuctionStatus.ACTIVE && endTime != null) {
            long remaining = endTime.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
        return 0;
    }
}