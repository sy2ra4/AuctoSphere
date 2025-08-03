package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.Message;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class MessagesViewController {
    @FXML
    private ListView<Message> messagesListView;
    @FXML
    private VBox messageDetailContainer;
    @FXML
    private TextArea messageDetailArea;
    @FXML
    private Button replyButton;

    private ClientNetworkHandler networkHandler;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    private User currentUser;
    private Message selectedMessageForReply;

    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        currentUser = ClientApp.getInstance().getCurrentUser();

        messageDetailContainer.setVisible(false);
        messageDetailContainer.setManaged(false);

        messagesListView.setCellFactory(lv -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null || currentUser == null) {
                    setText(null);
                    setFont(Font.getDefault());
                } else {
                    String prefix = message.getSenderId() == currentUser.getUserId() ? "To: " + message.getReceiverUsername() : "From: " + message.getSenderUsername();
                    setText(prefix + " (Item: " + message.getAuctionItemName() + ") - " +
                            message.getMessageText().substring(0, Math.min(message.getMessageText().length(), 40)) + "...");
                    if (!message.isRead() && message.getReceiverId() == currentUser.getUserId()) {
                        setFont(Font.font(getFont().getFamily(), FontWeight.BOLD, getFont().getSize()));
                    } else {
                        setFont(Font.font(getFont().getFamily(), FontWeight.NORMAL, getFont().getSize()));
                    }
                }
            }
        });

        messagesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldMsg, newMsg) -> {
            selectedMessageForReply = newMsg;
            if (newMsg != null) {
                displayMessageDetail(newMsg);
                replyButton.setDisable(false);
                if (!newMsg.isRead() && newMsg.getReceiverId() == currentUser.getUserId()) {
                    markMessageAsReadOnServer(newMsg);
                }
            } else {
                messageDetailArea.clear();
                messageDetailArea.setVisible(false);
                messageDetailArea.setManaged(false);
                replyButton.setDisable(true);

            }
        });


        messageDetailArea.setVisible(false);
        messageDetailArea.setManaged(false);

    }

    public void loadUserMessages() {
        if (currentUser == null) return;
        Request request = new Request(RequestType.GET_MY_MESSAGES, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    List<Message> messages = (List<Message>) response.getData();
                    messagesListView.setItems(FXCollections.observableArrayList(messages));
                    messagesListView.getSelectionModel().clearSelection();
                    messageDetailContainer.setVisible(false);
                    messageDetailContainer.setManaged(false);
                    replyButton.setDisable(true);
                } else {
                    UIUtils.showAlert(Alert.AlertType.ERROR, "Error", "Failed to load messages.");
                }
            });
        });
    }

    @FXML
    private void handleRefreshMessages() {
        loadUserMessages();
        messageDetailArea.setVisible(false);
        messageDetailArea.setManaged(false);
    }

    private void displayMessageDetail(Message message) {
        String detail = String.format("Item Context: %s\nSent: %s\nFrom: %s (%d)\nTo: %s (%d)\n\nMessage:\n%s",
                message.getAuctionItemName(),
                dateTimeFormatter.format(message.getTimestamp()),
                message.getSenderUsername(), message.getSenderId(),
                message.getReceiverUsername(), message.getReceiverId(),
                message.getMessageText());
        messageDetailArea.setText(detail);
        messageDetailContainer.setVisible(true);
        messageDetailContainer.setManaged(true);

    }

    private void markMessageAsReadOnServer(Message message) {
        Request request = new Request(RequestType.MARK_MESSAGE_AS_READ, message.getMessageId());
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            if (response != null && response.isSuccess()) {
                Platform.runLater(() -> {
                    message.setRead(true);
                    messagesListView.refresh();
                });
            }
        });
    }

    @FXML
    private void handleReplyAction() {
        if (selectedMessageForReply == null || currentUser == null) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "No Message Selected", "Please select a message to reply to.");
            return;
        }
        int replyToUserId = selectedMessageForReply.getSenderId();
        String replyToUsername = selectedMessageForReply.getSenderUsername();
        if (replyToUserId == currentUser.getUserId()) {
            UIUtils.showAlert(Alert.AlertType.WARNING, "Cannot Reply", "You cannot reply to your own sent message in this thread view.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to " + replyToUsername);
        dialog.setHeaderText("Replying about item: " + selectedMessageForReply.getAuctionItemName());
        dialog.setContentText("Your Reply:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(replyText -> {
            if (replyText.trim().isEmpty()) {
                UIUtils.showAlert(Alert.AlertType.WARNING, "Empty Reply", "Reply message cannot be empty.");
                return;
            }

            Message replyMessage = new Message();
            replyMessage.setAuctionId(selectedMessageForReply.getAuctionId());
            replyMessage.setReceiverId(replyToUserId);
            replyMessage.setMessageText(replyText.trim());

            Request request = new Request(RequestType.SEND_MESSAGE, replyMessage);
            networkHandler.sendRequestAsync(request).thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        UIUtils.showAlert(Alert.AlertType.INFORMATION, "Reply Sent", "Your reply has been sent.");
                        loadUserMessages();
                    } else {
                        UIUtils.showAlert(Alert.AlertType.ERROR, "Reply Failed", "Could not send reply: " + (response != null ? response.getMessage() : "No response"));
                    }
                });
            });
        });
    }
}