package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.Auction;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class AdminDashboardController {
    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, Integer> userIdCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> createdAtCol;

    @FXML private TableView<Auction> auctionsTableView;
    @FXML private TableColumn<Auction, Integer> auctionIdCol;
    @FXML private TableColumn<Auction, String> auctionItemNameCol;
    @FXML private TableColumn<Auction, String> auctionStatusCol;
    @FXML private TableColumn<Auction, String> auctionStartTimeCol;
    @FXML private TableColumn<Auction, String> auctionEndTimeCol;
    @FXML private TableColumn<Auction, BigDecimal> auctionCurrentBidCol;
    @FXML private TableColumn<Auction, String> auctionPaymentStatusCol;
    @FXML private TableColumn<User, Void> userActionCol;
    @FXML private TableColumn<Auction, Void> auctionActionCol;

    private ClientNetworkHandler networkHandler;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        setupUserTableColumns();
        setupAuctionTableColumns();
    }

    public void loadAdminData() {
        if (ClientApp.getInstance().getCurrentUser() == null ||
                ClientApp.getInstance().getCurrentUser().getRole() != com.university.auctionsystem.shared.model.Role.ADMIN) {
            return;
        }
        loadAllUsers();
        loadAllAuctions();
    }

    @FXML
    private void handleRefreshAdminData() {
        loadAdminData();
    }

    private void setupUserTableColumns() {
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdAtCol.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(ts != null ? dateTimeFormatter.format(ts) : "N/A");
        });
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdAtCol.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(ts != null ? dateTimeFormatter.format(ts) : "N/A");
        });

        Callback<TableColumn<User, Void>, TableCell<User, Void>> userCellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<User, Void>() {
                    private final Button btn = new Button("Delete");
                    {
                        btn.getStyleClass().add("button-danger");
                        btn.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            handleDeleteUser(user);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            User user = getTableView().getItems().get(getIndex());
                            if (user.getUserId() == ClientApp.getInstance().getCurrentUser().getUserId() ||
                                    user.getRole() == com.university.auctionsystem.shared.model.Role.ADMIN) {
                                btn.setDisable(true);
                            } else {
                                btn.setDisable(false);
                            }
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        userActionCol.setCellFactory(userCellFactory);
    }

    private void setupAuctionTableColumns() {
        auctionIdCol.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        auctionItemNameCol.setCellValueFactory(cellData -> {
            Auction auction = cellData.getValue();
            String itemName = (auction.getItem() != null) ? auction.getItem().getName() : "N/A";
            return new javafx.beans.property.SimpleStringProperty(itemName);
        });
        auctionStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        auctionStartTimeCol.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getStartTime();
            return new javafx.beans.property.SimpleStringProperty(ts != null ? dateTimeFormatter.format(ts) : "N/A");
        });
        auctionEndTimeCol.setCellValueFactory(cellData -> {
            Timestamp ts = cellData.getValue().getEndTime();
            return new javafx.beans.property.SimpleStringProperty(ts != null ? dateTimeFormatter.format(ts) : "N/A");
        });
        auctionCurrentBidCol.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));

        auctionPaymentStatusCol.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        Callback<TableColumn<Auction, Void>, TableCell<Auction, Void>> auctionCellFactory = new Callback<>() {
            @Override
            public TableCell<Auction, Void> call(final TableColumn<Auction, Void> param) {
                return new TableCell<Auction, Void>() {
                    private final Button btn = new Button("Delete");
                    {
                        btn.setOnAction(event -> {
                            Auction auction = getTableView().getItems().get(getIndex());
                            handleDeleteAuction(auction);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        auctionActionCol.setCellFactory(auctionCellFactory);
    }

    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete user '" + user.getUsername() + "'? This is irreversible.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Request request = new Request(RequestType.DELETE_USER, user.getUserId());
                networkHandler.sendRequestAsync(request).thenAccept(res -> Platform.runLater(() -> {
                    if (res != null && res.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.INFORMATION, "User Deleted", "User " + user.getUsername() + " deleted.");
                        loadAllUsers();
                    } else {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Delete Failed", res != null ? res.getMessage() : "Could not delete user.");
                    }
                }));
            }
        });
    }

    private void handleDeleteAuction(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete auction for '" + (auction.getItem()!=null?auction.getItem().getName():"ID "+auction.getAuctionId()) + "'? This also deletes related bids/messages.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Request request = new Request(RequestType.DELETE_AUCTION, auction.getAuctionId());
                networkHandler.sendRequestAsync(request).thenAccept(res -> Platform.runLater(() -> {
                    if (res != null && res.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.INFORMATION, "Auction Deleted", "Auction deleted.");
                        loadAllAuctions();
                    } else {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Delete Failed", res != null ? res.getMessage() : "Could not delete auction.");
                    }
                }));
            }
        });
    }

    private void loadAllUsers() {
        Request request = new Request(RequestType.GET_ALL_USERS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<User> users = (List<User>) response.getData();
                    usersTableView.setItems(FXCollections.observableArrayList(users));
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Admin Error", "Failed to load users: " + (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Admin Error", "Exception loading users: " + ex.getMessage()));
            return null;
        });
    }

    private void loadAllAuctions() {
        Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) response.getData();
                    auctionsTableView.setItems(FXCollections.observableArrayList(auctions));
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Admin Error", "Failed to load auctions: " + (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Admin Error", "Exception loading auctions: " + ex.getMessage()));
            return null;
        });
    }
}