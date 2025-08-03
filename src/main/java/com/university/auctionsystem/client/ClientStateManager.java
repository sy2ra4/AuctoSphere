package com.university.auctionsystem.client;

import com.university.auctionsystem.shared.model.Auction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


public class ClientStateManager {
    private static ClientStateManager instance;


    private final ObservableList<Auction> activeAuctions = FXCollections.observableArrayList();

    private ClientStateManager() {}

    public static synchronized ClientStateManager getInstance() {
        if (instance == null) {
            instance = new ClientStateManager();
        }
        return instance;
    }

    public ObservableList<Auction> getActiveAuctions() {
        return activeAuctions;
    }

    public void setActiveAuctions(List<Auction> newAuctions) {
        Platform.runLater(() -> {
            activeAuctions.sort(Comparator.comparing(Auction::getEndTime));
            activeAuctions.setAll(newAuctions);
        });
    }

    public void handleAuctionUpdate(Auction updatedAuction) {
        Platform.runLater(() -> {
            int index = -1;
            for (int i = 0; i < activeAuctions.size(); i++) {
                if (activeAuctions.get(i).getAuctionId() == updatedAuction.getAuctionId()) {
                    index = i;
                    break;
                }
            }

            if (updatedAuction.getStatus() == Auction.AuctionStatus.ACTIVE) {
                if (index != -1) {
                    activeAuctions.set(index, updatedAuction);
                } else {
                    activeAuctions.add(updatedAuction);
                }
            } else {
                if (index != -1) {
                    activeAuctions.remove(index);
                }
            }
        });
    }
}