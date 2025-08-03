package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.ClientStateManager;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.*;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.collections.ListChangeListener;


import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AuctionViewController {

    @FXML private TilePane auctionsTilePane;
    @FXML private VBox auctionDetailsPane;
    @FXML private Text detailItemName;
    @FXML private Label detailItemDescription;
    @FXML private Label detailCurrentBid;
    @FXML private Label detailTimeRemaining;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Button contactSellerButton;
    @FXML private Label bidStatusLabel;
    @FXML private ListView<String> bidHistoryListView;



    private ClientNetworkHandler networkHandler;
    private Auction selectedAuction;
    private Timeline countdownTimeline;
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private ClientStateManager stateManager;
    private final SimpleDateFormat displayFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    private final SimpleDateFormat timerFormatter = new SimpleDateFormat("HH:mm:ss");


    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        stateManager = ClientStateManager.getInstance();
        TimeZone dhakaTimeZone = TimeZone.getTimeZone("Asia/Dhaka");
        displayFormatter.setTimeZone(dhakaTimeZone);
        timerFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        stateManager.getActiveAuctions().addListener((ListChangeListener<Auction>) c -> {

            Platform.runLater(() -> displayAuctions(stateManager.getActiveAuctions()));
        });

        auctionDetailsPane.setVisible(false);
        auctionDetailsPane.setManaged(false);

        bidHistoryListView.setPlaceholder(new Label("No bids have been placed yet."));
    }

    public void loadActiveAuctions() {
        Request request = new Request(RequestType.GET_ACTIVE_AUCTIONS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            if (response != null && response.isSuccess()) {
                List<Auction> auctions = (List<Auction>) response.getData();
                stateManager.setActiveAuctions(auctions);
            } else {
                Platform.runLater(() -> {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load auctions: " + (response != null ? response.getMessage() : "No response"));
                    stateManager.getActiveAuctions().clear();
                });
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error Loading Auctions", "Exception: " + ex.getMessage());
                stateManager.getActiveAuctions().clear();
            });
            return null;
        });
    }

    private void updateAuctionCardInTilePane(Auction updatedAuction) {
        if (updatedAuction == null || updatedAuction.getItem() == null) return;
        for (javafx.scene.Node node : auctionsTilePane.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                Button viewButton = (Button) card.getChildren().stream()
                        .filter(child -> child instanceof Button && "View Details".equals(((Button) child).getText()))
                        .findFirst().orElse(null);
                if (viewButton != null && viewButton.getUserData() instanceof Integer) {
                    int cardAuctionId = (Integer) viewButton.getUserData();
                    if (cardAuctionId == updatedAuction.getAuctionId()) {
                        for(javafx.scene.Node childNode : card.getChildren()){
                            if(childNode instanceof Label){
                                Label label = (Label) childNode;
                                if(label.getText().startsWith("Current Bid:")){
                                    label.setText("Current Bid: " + currencyFormatter.format(updatedAuction.getCurrentHighestBid() != null ? updatedAuction.getCurrentHighestBid() : updatedAuction.getStartPrice()));
                                }
                                if(label.getText().startsWith("Ends:") || label.getText().startsWith("Status:")){
                                    if(updatedAuction.getStatus() == Auction.AuctionStatus.ACTIVE) {
                                        label.setText("Ends: " + (updatedAuction.getEndTime() != null ? new SimpleDateFormat("MMM dd, HH:mm").format(updatedAuction.getEndTime()) : "N/A"));
                                    } else {
                                        label.setText("Status: " + updatedAuction.getStatus().toString());
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void displayAuctions(List<Auction> auctions) {
        auctionsTilePane.getChildren().clear();
        if (auctions == null || auctions.isEmpty()) {
            auctionsTilePane.getChildren().add(new Label("No active auctions found."));
            return;
        }
        for (Auction auction : auctions) {
            if (auction.getItem() == null) continue;
            VBox auctionCard = new VBox(5);
            auctionCard.getStyleClass().add("auction-item");
            auctionCard.setPrefWidth(200);
            ImageView itemImage = new ImageView();
            try {
                String imagePath = auction.getItem().getImagePath();
                Image img = new Image( (imagePath != null && !imagePath.isEmpty() ? imagePath : "/images/placeholder_item.png"), true);
                if(img.isError()){
                    img = new Image(getClass().getResourceAsStream("/images/placeholder_item.png"));
                }
                itemImage.setImage(img);
            } catch (Exception e) {
                itemImage.setImage(new Image(getClass().getResourceAsStream("/images/placeholder_item.png")));
            }
            itemImage.setFitHeight(100); itemImage.setFitWidth(180); itemImage.setPreserveRatio(true);
            Text itemName = new Text(auction.getItem().getName());
            itemName.getStyleClass().add("item-name");
            Label currentBidLabel = new Label("Current Bid: " + currencyFormatter.format(auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartPrice()));
            Label endsLabel;
            if (auction.getStatus() == Auction.AuctionStatus.ACTIVE) {
                endsLabel = new Label("Ends: " + (auction.getEndTime() != null ? displayFormatter.format(auction.getEndTime()) : "N/A"));
            } else {
                endsLabel = new Label("Status: " + auction.getStatus().toString());
            }
            Button viewButton = new Button("View Details");
            viewButton.setOnAction(e -> loadAndShowAuctionDetails(auction.getAuctionId()));
            viewButton.setUserData(auction.getAuctionId());
            auctionCard.getChildren().addAll(itemImage, itemName, currentBidLabel, endsLabel, viewButton);
            auctionsTilePane.getChildren().add(auctionCard);
        }
    }

    private void loadAndShowAuctionDetails(int auctionId) {

        Request request = new Request(RequestType.GET_AUCTION_DETAILS, auctionId);
        bidStatusLabel.setText("Loading details...");
        auctionDetailsPane.setVisible(false);
        auctionDetailsPane.setManaged(false);

        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess() && response.getData() instanceof Auction) {
                    this.selectedAuction = (Auction) response.getData();
                    populateAuctionDetailsPane(this.selectedAuction);
                } else {
                    bidStatusLabel.setText("Failed to load auction details.");
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not fetch details for auction ID " + auctionId);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                bidStatusLabel.setText("Error loading details: " + ex.getMessage());
                UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Exception fetching details for auction ID " + auctionId + ": " + ex.getMessage());
            });
            return null;
        });
    }


    private void populateAuctionDetailsPane(Auction auction) {
        if (auction == null || auction.getItem() == null) {
            System.err.println("populateAuctionDetailsPane called with null auction or item.");
            auctionDetailsPane.setVisible(false);
            auctionDetailsPane.setManaged(false);
            return;
        }

        this.selectedAuction = auction;

        detailItemName.setText(auction.getItem().getName());
        detailItemDescription.setText(auction.getItem().getDescription());
        bidStatusLabel.setText("");

        updateAuctionDetailsUIDisplay();
        loadBidHistory(auction.getAuctionId());

        auctionDetailsPane.setVisible(true);
        auctionDetailsPane.setManaged(true);
    }

    private void listenForDetailUpdates() {
        if (selectedAuction == null) return;
        stateManager.getActiveAuctions().addListener((ListChangeListener<Auction>) c -> {
            if (selectedAuction == null) return;
            Platform.runLater(() -> {
                stateManager.getActiveAuctions().stream()
                        .filter(a -> a.getAuctionId() == selectedAuction.getAuctionId())
                        .findFirst()
                        .ifPresentOrElse(
                                updatedAuction -> populateAuctionDetailsPane(updatedAuction),
                                () -> {
                                    auctionDetailsPane.setVisible(false);
                                    auctionDetailsPane.setManaged(false);
                                    if (countdownTimeline != null) countdownTimeline.stop();
                                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Auction Update", "The auction you were viewing is no longer active.");
                                }
                        );
            });
        });
    }

    private void updateAuctionDetailsUIDisplay() {
        if (selectedAuction == null) return;

        detailCurrentBid.setText("Current Bid: " + currencyFormatter.format(selectedAuction.getCurrentHighestBid() != null ? selectedAuction.getCurrentHighestBid() : selectedAuction.getStartPrice()));

        boolean canBid = ClientApp.getInstance().getCurrentUser() != null &&
                ClientApp.getInstance().getCurrentUser().getRole() == Role.BUYER &&
                selectedAuction.getStatus() == Auction.AuctionStatus.ACTIVE &&
                (selectedAuction.getItem() != null && ClientApp.getInstance().getCurrentUser().getUserId() != selectedAuction.getItem().getSellerId());

        bidAmountField.setDisable(!canBid);
        if (placeBidButton != null) placeBidButton.setDisable(!canBid);

        if (contactSellerButton != null) {
            boolean canContact = ClientApp.getInstance().getCurrentUser() != null &&
                    selectedAuction.getItem() != null &&
                    ClientApp.getInstance().getCurrentUser().getUserId() != selectedAuction.getItem().getSellerId();
            contactSellerButton.setDisable(!canContact);
            contactSellerButton.setVisible(true);
            contactSellerButton.setManaged(true);
            contactSellerButton.setOnAction(e -> handleContactSeller(selectedAuction));
        }


        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (selectedAuction.getStatus() == Auction.AuctionStatus.ACTIVE) {
            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateAuctionDetailsUIDisplay()));
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();

            long remainingMillis = selectedAuction.getTimeRemainingMillis();
            if (remainingMillis > 0) {
                detailTimeRemaining.setText("Time Remaining: " + timerFormatter.format(remainingMillis));
            } else {
                detailTimeRemaining.setText("Ending soon...");
            }
        } else {
            detailTimeRemaining.setText("Status: " + selectedAuction.getStatus().toString());
        }
    }


    private void handleContactSeller(Auction auctionContext) {
        if (ClientApp.getInstance().getCurrentUser() == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Login Required", "Please login to contact the seller.");
            return;
        }
        if (auctionContext == null || auctionContext.getItem() == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Auction context is missing.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Contact Seller");
        dialog.setHeaderText("Send a message about: " + auctionContext.getItem().getName());
        dialog.setContentText("Your Message:");

        dialog.showAndWait().ifPresent(messageText -> {
            if (messageText.trim().isEmpty()) {
                UIUtils.showAlert(Alert.AlertType.WARNING, "Empty Message", "Message cannot be empty.");
                return;
            }
            Message newMessage = new Message();
            newMessage.setAuctionId(auctionContext.getAuctionId());
            newMessage.setMessageText(messageText.trim());

            Request request = new Request(RequestType.SEND_MESSAGE, newMessage);
            networkHandler.sendRequestAsync(request).thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.INFORMATION, "Message Sent", "Your message has been sent to the seller.");
                    } else {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Send Failed", "Could not send message: " + (response != null ? response.getMessage() : "No response"));
                    }
                });
            });
        });
    }

    @FXML
    private void handlePlaceBid() {
        System.out.println("handlePlaceBid() method entered.");

        if (selectedAuction == null) {
            bidStatusLabel.setText("No auction selected.");
            UIUtils.showAlert(Alert.AlertType.WARNING, "Error", "No auction selected. Please select an auction first.");
            System.out.println("handlePlaceBid exit: selectedAuction is null.");
            return;
        }

        User currentUser = ClientApp.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getRole() != com.university.auctionsystem.shared.model.Role.BUYER) {
            bidStatusLabel.setText("Only logged-in buyers can bid.");
            UIUtils.showAlert(Alert.AlertType.ERROR, "Access Denied", "You must be logged in as a Buyer to place a bid.");
            System.out.println("handlePlaceBid exit: User is not a logged-in buyer.");
            return;
        }

        try {
            String bidAmountText = bidAmountField.getText();
            if (bidAmountText == null || bidAmountText.trim().isEmpty()) {
                bidStatusLabel.setText("Bid amount cannot be empty.");
                UIUtils.showAlert(Alert.AlertType.WARNING, "Invalid Bid", "Please enter a bid amount.");
                System.out.println("handlePlaceBid exit: Bid amount field is empty.");
                return;
            }

            BigDecimal bidAmount = new BigDecimal(bidAmountText.trim());
            BigDecimal minNextBid = selectedAuction.getCurrentHighestBid() != null ? selectedAuction.getCurrentHighestBid() : selectedAuction.getStartPrice();
            if (bidAmount.compareTo(minNextBid) <= 0) {
                bidStatusLabel.setText("Your bid must be higher than the current bid.");
                UIUtils.showAlert(Alert.AlertType.WARNING, "Invalid Bid", "Your bid must be higher than the current bid of " + currencyFormatter.format(minNextBid));
                System.out.println("handlePlaceBid exit: Bid amount is not higher than current bid.");
                return;
            }


            Bid bid = new Bid(selectedAuction.getAuctionId(), currentUser.getUserId(), bidAmount);
            Request request = new Request(RequestType.PLACE_BID, bid);

            bidStatusLabel.setText("Placing bid...");
            System.out.println("Sending PLACE_BID request to server...");

            networkHandler.sendRequestAsync(request).thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        bidStatusLabel.setText("Bid placed successfully! Awaiting confirmation...");
                        bidAmountField.clear();
                    } else {
                        bidStatusLabel.setText("Bid failed: " + (response != null ? response.getMessage() : "No response"));
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    bidStatusLabel.setText("Error placing bid: " + ex.getMessage());
                    ex.printStackTrace();
                });
                return null;
            });

        } catch (NumberFormatException e) {
            bidStatusLabel.setText("Invalid bid amount. Please enter a number.");
            UIUtils.showAlert(Alert.AlertType.ERROR, "Invalid Input", "The bid amount must be a valid number (e.g., 125.50).");
            System.out.println("handlePlaceBid exit: NumberFormatException.");
        }
    }

    private void loadBidHistory(int auctionId) {
        if (auctionId <= 0) {
            bidHistoryListView.getItems().clear();
            return;
        }

        Request request = new Request(RequestType.GET_BIDS_FOR_AUCTION, auctionId);

        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                bidHistoryListView.getItems().clear();

                if (response != null && response.isSuccess()) {
                    if (response.getData() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Bid> bids = (List<Bid>) response.getData();

                        if (bids.isEmpty()) {
                        } else {
                            for (Bid bid : bids) {
                                String bidText = String.format("%s by %s at %s",
                                        currencyFormatter.format(bid.getBidAmount()),
                                        bid.getBidderUsername() != null ? bid.getBidderUsername() : "Unknown",
                                        bid.getBidTime() != null ? displayFormatter.format(bid.getBidTime()) : "N/A"
                                );
                                bidHistoryListView.getItems().add(bidText);
                            }
                        }
                    } else {
                        System.err.println("Error: Expected List<Bid> for bid history, but received " + response.getData().getClass().getName());
                        bidHistoryListView.setPlaceholder(new Label("Error loading bid history."));
                    }
                } else {
                    System.err.println("Failed to load bid history: " + (response != null ? response.getMessage() : "No response from server"));
                    bidHistoryListView.setPlaceholder(new Label("Could not load bid history."));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                bidHistoryListView.getItems().clear();
                bidHistoryListView.setPlaceholder(new Label("An error occurred while fetching bid history."));
                System.err.println("Exception fetching bid history: " + ex.getMessage());
                ex.printStackTrace();
            });
            return null;
        });
    }

    @FXML
    private void handleRefreshAuctions() {
        loadActiveAuctions();
        auctionDetailsPane.setVisible(false);
        auctionDetailsPane.setManaged(false);
        if (contactSellerButton != null) {
            contactSellerButton.setVisible(false);
            contactSellerButton.setManaged(false);
        }
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        this.selectedAuction = null;
    }
}