package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.Auction;
import com.university.auctionsystem.shared.model.Bid;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import com.university.auctionsystem.shared.protocol.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class BuyerDashboardController {
    @FXML private ListView<Bid> myBidsListView;
    @FXML private ListView<Auction> wonAuctionsListView;

    private ClientNetworkHandler networkHandler;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm");

    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        setupMyBidsListViewCellFactory();
        setupWonAuctionsListViewCellFactory();

        myBidsListView.setPlaceholder(new Label("You haven't placed any bids yet."));
        wonAuctionsListView.setPlaceholder(new Label("You haven't won any auctions yet."));
    }

    private void setupMyBidsListViewCellFactory() {
        myBidsListView.setCellFactory(lv -> new ListCell<Bid>() {
            @Override
            protected void updateItem(Bid bid, boolean empty) {
                super.updateItem(bid, empty);
                if (empty || bid == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String itemName = bid.getAuctionItemName() != null ? bid.getAuctionItemName() : "Auction ID: " + bid.getAuctionId();
                    String status = bid.getAuctionStatus() != null ? bid.getAuctionStatus() : "N/A";
                    setText(String.format("Item: %s (Status: %s) - Your Bid: %s on %s",
                            itemName,
                            status,
                            currencyFormatter.format(bid.getBidAmount()),
                            dateTimeFormatter.format(bid.getBidTime())
                    ));
                }
            }
        });
    }

    private void setupWonAuctionsListViewCellFactory() {
        wonAuctionsListView.setCellFactory(lv -> new ListCell<Auction>() {
            private final HBox hbox = new HBox(10);
            private final Label label = new Label();
            private final Button payButton = new Button();

            {
                HBox.setHgrow(label, Priority.ALWAYS);
                hbox.getChildren().addAll(label, payButton);
                hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                payButton.setOnAction(event -> {
                    Auction auctionToPay = getItem();
                    if (auctionToPay != null) {
                        handleMakePayment(auctionToPay);
                    }
                });
            }

            @Override
            protected void updateItem(Auction auction, boolean empty) {
                super.updateItem(auction, empty);
                if (empty || auction == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    label.setText(formatWonAuctionForDisplay(auction));

                    if (auction.getStatus() != Auction.AuctionStatus.ENDED) {
                        payButton.setText("Awaiting End");
                        payButton.setDisable(true);
                    } else if (auction.getPaymentStatus() == Auction.PaymentStatus.PAID) {
                        payButton.setText("Paid");
                        payButton.setDisable(true);
                    } else {
                        payButton.setText("Make Payment");
                        payButton.setDisable(false);
                    }
                    setGraphic(hbox);
                }
            }
        });
    }

    private String formatWonAuctionForDisplay(Auction auction) {
        return String.format("Item: %s - Won Price: %s - Payment: %s",
                auction.getItem() != null ? auction.getItem().getName() : "N/A",
                currencyFormatter.format(auction.getCurrentHighestBid()),
                auction.getPaymentStatus());
    }

    public void loadBuyerData() {
        if (ClientApp.getInstance().getCurrentUser() == null ||
                ClientApp.getInstance().getCurrentUser().getRole() != com.university.auctionsystem.shared.model.Role.BUYER) {
            myBidsListView.getItems().clear();
            wonAuctionsListView.getItems().clear();
            return;
        }
        loadMyBids();
        loadWonAuctions();
    }

    @FXML
    private void handleRefreshData() {
        loadBuyerData();
    }

    private void loadMyBids() {
        Request request = new Request(RequestType.GET_MY_BIDS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<Bid> bids = (List<Bid>) response.getData();
                    if (bids != null) {
                        myBidsListView.setItems(FXCollections.observableArrayList(bids));
                    } else {
                        myBidsListView.getItems().clear();
                        UIUtils.showAlert(Alert.AlertType.WARNING, "My Bids", "Received null data for bids.");
                    }
                } else {
                    myBidsListView.getItems().clear();
                    if (response != null && !response.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load your bids: " + response.getMessage());
                    } else if (response == null) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load your bids: No response from server.");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                myBidsListView.getItems().clear();
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error Loading Bids", "Exception: " + ex.getMessage());
            });
            return null;
        });
    }

    private void loadWonAuctions() {
        Request request = new Request(RequestType.GET_MY_WON_AUCTIONS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) response.getData();
                    if (auctions != null) {
                        wonAuctionsListView.setItems(FXCollections.observableArrayList(auctions));
                    } else {
                        wonAuctionsListView.getItems().clear();
                        UIUtils.showAlert(Alert.AlertType.WARNING, "Won Auctions", "Received null data for won auctions.");
                    }
                } else {
                    wonAuctionsListView.getItems().clear();
                    if (response != null && !response.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load won auctions: " + response.getMessage());
                    } else if (response == null) {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load won auctions: No response from server.");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                wonAuctionsListView.getItems().clear();
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error Loading Won Auctions", "Exception: " + ex.getMessage());
            });
            return null;
        });
    }

    private void handleMakePayment(Auction auctionToPay) {
        if (auctionToPay == null) return;

        if (auctionToPay.getPaymentStatus() == Auction.PaymentStatus.PAID) {
            UIUtils.showAlert(Alert.AlertType.INFORMATION, "Payment", "This auction has already been paid for.");
            return;
        }
        if (auctionToPay.getStatus() != Auction.AuctionStatus.ENDED) {
            UIUtils.showAlert(Alert.AlertType.INFORMATION, "Payment", "Auction has not ended yet. Cannot make payment.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION,
                "Pay " + currencyFormatter.format(auctionToPay.getCurrentHighestBid()) + " for " + auctionToPay.getItem().getName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirmDialog.setTitle("Confirm Payment");
        confirmDialog.setHeaderText(null);

        confirmDialog.showAndWait().ifPresent(responseType -> {
            if (responseType == ButtonType.YES) {
                Request request = new Request(RequestType.PROCESS_PAYMENT, auctionToPay.getAuctionId());
                networkHandler.sendRequestAsync(request).thenAccept(res -> {
                    Platform.runLater(() -> {
                        if (res != null && res.isSuccess()) {
                            UIUtils.showAlert(Alert.AlertType.INFORMATION, "Payment Success", "Payment processed successfully!");

                            auctionToPay.setPaymentStatus(Auction.PaymentStatus.PAID);
                            wonAuctionsListView.refresh();
                        } else {
                            UIUtils.showAlert(Alert.AlertType.ERROR, "Payment Failed", res != null ? res.getMessage() : "Could not process payment.");
                        }
                    });
                });
            }
        });
    }
}