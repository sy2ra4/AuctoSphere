package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.Auction;
import com.university.auctionsystem.shared.model.Item;
import com.university.auctionsystem.shared.model.Role;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import com.university.auctionsystem.shared.protocol.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Callback;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Comparator;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class SellerDashboardController {

    @FXML private ListView<Item> myItemsListView;
    @FXML private ComboBox<Item> itemToAuctionComboBox;
    @FXML private TextField startPriceField;
    @FXML private TextField reservePriceField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private Label auctionStatusLabel;

    @FXML private TabPane sellerTabPane;
    @FXML private TableView<Auction> myAuctionsTableView;
    @FXML private TableColumn<Auction, Item> myItemNameCol;
    @FXML private TableColumn<Auction, Auction.AuctionStatus> myAuctionStatusCol;
    @FXML private TableColumn<Auction, BigDecimal> myAuctionCurrentBidCol;
    @FXML private TableColumn<Auction, Integer> myAuctionWinnerCol;
    @FXML private TableColumn<Auction, Auction.PaymentStatus> myAuctionPaymentStatusCol;
    @FXML private TableColumn<Auction, Timestamp> myAuctionEndTimeCol;

    private ClientNetworkHandler networkHandler;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm");


    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        startDatePicker.setValue(LocalDate.now());
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        startTimeField.setText(LocalTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));
        endTimeField.setText(LocalTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));

        auctionStatusLabel.setText("");

        networkHandler = ClientApp.getInstance().getNetworkHandler();

        myItemsListView.setCellFactory(lv -> new ListCell<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (ID: " + item.getItemId() + ")");
            }
        });

        itemToAuctionComboBox.setCellFactory(lv -> new ListCell<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (ID: " + item.getItemId() + ")");
            }
        });

        itemToAuctionComboBox.setButtonCell(new ListCell<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (ID: " + item.getItemId() + ")");
            }
        });

        setupMyAuctionsTable();
        setupMyItemsListViewActions();
        setupMyAuctionsTableActions();
    }

    private void setupMyItemsListViewActions() {

        myItemsListView.setCellFactory(lv -> new ListCell<Item>() {
            private final HBox hbox = new HBox(10);
            private final Label itemNameLabel = new Label();
            private final Button editButton = new Button("Edit");

            {
                HBox.setHgrow(itemNameLabel, Priority.ALWAYS);
                editButton.setOnAction(event -> {
                    Item item = getItem();
                    if (item != null) {
                        handleEditItemAction(item);
                    }
                });
                hbox.getChildren().addAll(itemNameLabel, editButton );
                hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    itemNameLabel.setText(item.getName() + " (ID: " + item.getItemId() + ")");
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupMyAuctionsTableActions() {
        if (myAuctionsTableView == null) return;
        TableColumn<Auction, Void> actionCol = new TableColumn<>("Actions");


        Callback<TableColumn<Auction, Void>, TableCell<Auction, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Auction, Void> call(final TableColumn<Auction, Void> param) {
                final TableCell<Auction, Void> cell = new TableCell<Auction, Void>() {
                    private final Button btnCancel = new Button("Cancel");

                    {
                        btnCancel.setOnAction(event -> {
                            Auction auction = getTableView().getItems().get(getIndex());
                            handleCancelUpcomingAuction(auction);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Auction auction = getTableView().getItems().get(getIndex());
                            if (auction.getStatus() == Auction.AuctionStatus.UPCOMING) {
                                btnCancel.setDisable(false);
                                setGraphic(btnCancel);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
                return cell;
            }
        };
        actionCol.setCellFactory(cellFactory);
        if (myAuctionsTableView.getColumns().stream().noneMatch(col -> col.getText().equals("Actions"))) {
            myAuctionsTableView.getColumns().add(actionCol);
        }
    }

    private void handleEditItemAction(Item itemToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/university/auctionsystem/client/views/ListItemView.fxml"));
            Parent root = loader.load();

            ListItemController controller = loader.getController();
            controller.setItemToEdit(itemToEdit);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Item Details");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadMyItems();
            itemToAuctionComboBox.setItems(FXCollections.observableArrayList(myItemsListView.getItems()));

        } catch (IOException e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not open item editing form.");
        }
    }

    private void handleCancelUpcomingAuction(Auction auctionToCancel) {
        if (auctionToCancel.getStatus() != Auction.AuctionStatus.UPCOMING) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Cannot Cancel", "Only UPCOMING auctions can be cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel the auction for '" + auctionToCancel.getItem().getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Cancellation");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Request request = new Request(RequestType.CANCEL_UPCOMING_AUCTION, auctionToCancel.getAuctionId());
                networkHandler.sendRequestAsync(request).thenAccept(res -> Platform.runLater(() -> {
                    if (res != null && res.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.INFORMATION, "Auction Cancelled", "The auction has been cancelled.");
                        loadMyCreatedAuctions();
                        loadMyItems();
                    } else {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Cancellation Failed", res != null ? res.getMessage() : "Could not cancel auction.");
                    }
                }));
            }
        });
    }

    public void loadSellerData() {
        if (ClientApp.getInstance().getCurrentUser() == null ||
                ClientApp.getInstance().getCurrentUser().getRole() != Role.SELLER) {
            myItemsListView.getItems().clear();
            itemToAuctionComboBox.getItems().clear();
            if (myAuctionsTableView != null) myAuctionsTableView.getItems().clear();
            return;
        }
        loadMyItems();
        loadMyCreatedAuctions();
    }

    private void loadMyCreatedAuctions() {
        if (myAuctionsTableView == null) return;

        Request request = new Request(RequestType.GET_MY_CREATED_AUCTIONS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<Auction> auctions = (List<Auction>) response.getData();
                    myAuctionsTableView.setItems(FXCollections.observableArrayList(auctions));
                } else {
                    myAuctionsTableView.getItems().clear();
                    UIUtils.showAlert(Alert.AlertType.ERROR, "My Auctions", "Failed to load your created auctions: " +
                            (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                myAuctionsTableView.getItems().clear();
                UIUtils.showAlert(Alert.AlertType.ERROR, "My Auctions", "Exception loading your auctions: " + ex.getMessage());
            });
            return null;
        });
    }

    @FXML
    private void handleRefreshMyCreatedAuctions() {
        loadMyCreatedAuctions();
    }

    private void setupMyAuctionsTable() {
        if (myAuctionsTableView == null) return;
        myItemNameCol.setCellValueFactory(new PropertyValueFactory<>("item"));
        myItemNameCol.setCellFactory(column -> new TableCell<Auction, Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        myAuctionStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        myAuctionStatusCol.setCellFactory(column -> new TableCell<Auction, Auction.AuctionStatus>() {
            @Override
            protected void updateItem(Auction.AuctionStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status.toString());
                }
            }
        });
        myAuctionCurrentBidCol.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));
        myAuctionCurrentBidCol.setCellFactory(column -> new TableCell<Auction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                }
            }
        });
        myAuctionWinnerCol.setCellValueFactory(new PropertyValueFactory<>("winningBidderId"));
        myAuctionWinnerCol.setCellFactory(column -> new TableCell<Auction, Integer>() {
            @Override
            protected void updateItem(Integer winnerId, boolean empty) {
                super.updateItem(winnerId, empty);
                if (empty) {
                    setText(null);
                } else {
                    if (winnerId == null || winnerId == 0) {
                        setText("N/A");
                    } else {
                        setText(String.valueOf(winnerId));
                    }
                }
            }
        });
        myAuctionPaymentStatusCol.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        myAuctionPaymentStatusCol.setCellFactory(column -> new TableCell<Auction, Auction.PaymentStatus>() {
            @Override
            protected void updateItem(Auction.PaymentStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status.toString());
                }
            }
        });
        myAuctionEndTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        myAuctionEndTimeCol.setCellFactory(column -> new TableCell<Auction, Timestamp>() {
            @Override
            protected void updateItem(Timestamp timestamp, boolean empty) {
                super.updateItem(timestamp, empty);
                if (empty || timestamp == null) {
                    setText(null);
                } else {
                    setText(dateTimeFormatter.format(timestamp));
                }
            }
        });
    }

    private void loadMyItems() {
        if (ClientApp.getInstance().getCurrentUser() == null) {
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Not logged in. Cannot load seller items.");
            return;
        }
        Request request = new Request(RequestType.GET_SELLER_ITEMS, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<Item> items = (List<Item>) response.getData();
                    if (items != null) {
                        items.sort(Comparator.comparing(Item::getName));
                        myItemsListView.setItems(FXCollections.observableArrayList(items));
                        itemToAuctionComboBox.setItems(FXCollections.observableArrayList(items));
                    } else {
                        myItemsListView.getItems().clear();
                        itemToAuctionComboBox.getItems().clear();
                    }
                    if (items != null) {
                        items.sort(Comparator.comparing(Item::getName));
                        myItemsListView.setItems(FXCollections.observableArrayList(items));
                        itemToAuctionComboBox.setItems(FXCollections.observableArrayList(items));
                    } else {
                        myItemsListView.getItems().clear();
                        itemToAuctionComboBox.getItems().clear();
                        UIUtils.showAlert(Alert.AlertType.INFORMATION, "My Items", "No items found.");}
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load your items.");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> UIUtils.showAlert(Alert.AlertType.ERROR, "Error Loading Items", "Exception: " + ex.getMessage()));
            return null;
        });
    }

    @FXML
    private void handleListItemAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/university/auctionsystem/client/views/ListItemView.fxml"));
            Parent root = loader.load();

            ListItemController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("List New Item");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadMyItems();

        } catch (IOException e) {
            e.printStackTrace();
            UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Could not open item listing form.");
        }
    }

    @FXML
    private void handleCreateAuctionAction() {
        Item selectedItem = itemToAuctionComboBox.getValue();
        if (selectedItem == null) {
            auctionStatusLabel.setText("Please select an item to auction.");
            return;
        }

        try {
            BigDecimal startPrice = new BigDecimal(startPriceField.getText());
            BigDecimal reservePrice = reservePriceField.getText().isEmpty() ? null : new BigDecimal(reservePriceField.getText());

            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = LocalTime.parse(startTimeField.getText(), DateTimeFormatter.ofPattern("HH:mm"));
            Timestamp startTimestamp = Timestamp.valueOf(LocalDateTime.of(startDate, startTime));

            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = LocalTime.parse(endTimeField.getText(), DateTimeFormatter.ofPattern("HH:mm"));
            Timestamp endTimestamp = Timestamp.valueOf(LocalDateTime.of(endDate, endTime));

            if (startTimestamp.after(endTimestamp)) {
                auctionStatusLabel.setText("Start time must be before end time.");
                return;
            }
            if (startTimestamp.before(new Timestamp(System.currentTimeMillis()))) {
                auctionStatusLabel.setText("Start time cannot be in the past.");
                return;
            }


            Auction auction = new Auction();
            auction.setItemId(selectedItem.getItemId());
            auction.setStartPrice(startPrice);
            auction.setReservePrice(reservePrice);
            auction.setStartTime(startTimestamp);
            auction.setEndTime(endTimestamp);

            Request request = new Request(RequestType.CREATE_AUCTION, auction);
            auctionStatusLabel.setText("Creating auction...");

            networkHandler.sendRequestAsync(request).thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        auctionStatusLabel.setText("Auction created successfully!");
                        itemToAuctionComboBox.getSelectionModel().clearSelection();
                        startPriceField.clear();
                        reservePriceField.clear();
                        loadMyItems();
                    } else {
                        auctionStatusLabel.setText("Failed to create auction: " + (response != null ? response.getMessage() : "No response"));
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    auctionStatusLabel.setText("Error creating auction: " + ex.getMessage());
                    ex.printStackTrace();
                });
                return null;
            });

        } catch (NumberFormatException e) {
            auctionStatusLabel.setText("Invalid price format.");
        } catch (DateTimeParseException e) {
            auctionStatusLabel.setText("Invalid time format. Use HH:MM.");
        }catch (Exception e) {
            auctionStatusLabel.setText("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}