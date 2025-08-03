package com.university.auctionsystem.client.model;

import com.university.auctionsystem.shared.model.Auction;
import com.university.auctionsystem.shared.model.Item;
import javafx.beans.property.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ObservableAuction {

    private final ObjectProperty<Auction> sourceAuction;
    private final IntegerProperty auctionId;
    private final ObjectProperty<Item> item;
    private final ObjectProperty<BigDecimal> currentHighestBid;
    private final ObjectProperty<Auction.AuctionStatus> status;
    private final ObjectProperty<Auction.PaymentStatus> paymentStatus;
    private final ObjectProperty<Timestamp> endTime;

    public ObservableAuction(Auction auction) {
        this.sourceAuction = new SimpleObjectProperty<>(auction);
        this.auctionId = new SimpleIntegerProperty(auction.getAuctionId());
        this.item = new SimpleObjectProperty<>(auction.getItem());
        this.currentHighestBid = new SimpleObjectProperty<>(auction.getCurrentHighestBid());
        this.status = new SimpleObjectProperty<>(auction.getStatus());
        this.paymentStatus = new SimpleObjectProperty<>(auction.getPaymentStatus());
        this.endTime = new SimpleObjectProperty<>(auction.getEndTime());
    }

    public void update(Auction newAuctionData) {
        this.sourceAuction.set(newAuctionData);
        this.auctionId.set(newAuctionData.getAuctionId());
        this.item.set(newAuctionData.getItem());
        this.currentHighestBid.set(newAuctionData.getCurrentHighestBid());
        this.status.set(newAuctionData.getStatus());
        this.paymentStatus.set(newAuctionData.getPaymentStatus());
        this.endTime.set(newAuctionData.getEndTime());
    }

    public Auction getSourceAuction() {
        return sourceAuction.get();
    }

    public ObjectProperty<Auction> sourceAuctionProperty() {
        return sourceAuction;
    }

    public int getAuctionId() { return auctionId.get(); }
    public IntegerProperty auctionIdProperty() { return auctionId; }

    public Item getItem() { return item.get(); }
    public ObjectProperty<Item> itemProperty() { return item; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid.get(); }
    public ObjectProperty<BigDecimal> currentHighestBidProperty() { return currentHighestBid; }

    public Auction.AuctionStatus getStatus() { return status.get(); }
    public ObjectProperty<Auction.AuctionStatus> statusProperty() { return status; }

    public Auction.PaymentStatus getPaymentStatus() { return paymentStatus.get(); }
    public ObjectProperty<Auction.PaymentStatus> paymentStatusProperty() { return paymentStatus; }

    public Timestamp getEndTime() { return endTime.get(); }
    public ObjectProperty<Timestamp> endTimeProperty() { return endTime; }

    public long getTimeRemainingMillis() {
        if (getStatus() == Auction.AuctionStatus.ACTIVE && getEndTime() != null) {
            long remaining = getEndTime().getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
        return 0;
    }
}