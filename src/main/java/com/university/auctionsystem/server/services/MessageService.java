package com.university.auctionsystem.server.services;

import com.university.auctionsystem.server.DatabaseManager;
import com.university.auctionsystem.shared.model.Message;
import com.university.auctionsystem.shared.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private DatabaseManager dbManager;
    private UserService userService;

    public MessageService(DatabaseManager dbManager, UserService userService) {
        this.dbManager = dbManager;
        this.userService = userService;
    }

    public Message sendMessage(Message message) {
        System.out.println("MessageService: Attempting to send message. AuctionID: " + message.getAuctionId() +
                ", SenderID: " + message.getSenderId() + ", ReceiverID: " + message.getReceiverId() +
                ", Text: " + message.getMessageText());

        if (message.getAuctionId() <= 0 || message.getSenderId() <= 0 || message.getReceiverId() <= 0) {
            System.err.println("MessageService: Invalid IDs provided. AuctionID: " + message.getAuctionId() +
                    ", SenderID: " + message.getSenderId() + ", ReceiverID: " + message.getReceiverId());
            return null;
        }

        String sql = "INSERT INTO messages (auction_id, sender_id, receiver_id, message_text) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, message.getAuctionId());
            pstmt.setInt(2, message.getSenderId());
            pstmt.setInt(3, message.getReceiverId());
            pstmt.setString(4, message.getMessageText());

            System.out.println("MessageService: Executing insert: " + pstmt.toString());
            int affectedRows = pstmt.executeUpdate();
            System.out.println("MessageService: Affected rows: " + affectedRows);

            if (affectedRows == 0) {
                System.err.println("MessageService: Creating message failed, no rows affected.");
                return null;
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newMessageId = generatedKeys.getInt(1);
                    message.setMessageId(newMessageId);
                    System.out.println("MessageService: Message created with ID: " + newMessageId);

                    User sender = userService.getUserById(message.getSenderId());
                    if (sender != null) {
                        message.setSenderUsername(sender.getUsername());
                    } else {
                        System.err.println("MessageService: Could not find sender username for ID: " + message.getSenderId());
                    }
                    User receiver = userService.getUserById(message.getReceiverId());
                    if (receiver != null) {
                        message.setReceiverUsername(receiver.getUsername());
                    } else {
                        System.err.println("MessageService: Could not find receiver username for ID: " + message.getReceiverId());
                    }


                    return message;
                } else {
                    System.err.println("MessageService: Creating message failed, no ID obtained after insert.");
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("MessageService SQL Error sending message: " + e.getMessage() + " SQLState: " + e.getSQLState() + " ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
        System.err.println("MessageService: sendMessage returning null due to an issue.");
        return null;
    }

    public List<Message> getMessagesForUser(int userId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, s.username as sender_username, r.username as receiver_username, i.name as item_name " +
                "FROM messages m " +
                "JOIN users s ON m.sender_id = s.user_id " +
                "JOIN users r ON m.receiver_id = r.user_id " +
                "JOIN auctions a ON m.auction_id = a.auction_id " +
                "JOIN items i ON a.item_id = i.item_id " +
                "WHERE m.sender_id = ? OR m.receiver_id = ? " +
                "ORDER BY m.timestamp DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setMessageId(rs.getInt("message_id"));
                msg.setAuctionId(rs.getInt("auction_id"));
                msg.setSenderId(rs.getInt("sender_id"));
                msg.setSenderUsername(rs.getString("sender_username"));
                msg.setReceiverId(rs.getInt("receiver_id"));
                msg.setReceiverUsername(rs.getString("receiver_username"));
                msg.setMessageText(rs.getString("message_text"));
                msg.setTimestamp(rs.getTimestamp("timestamp"));
                msg.setRead(rs.getBoolean("is_read"));
                msg.setAuctionItemName(rs.getString("item_name"));
                messages.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public boolean markMessageAsRead(int messageId, int userId) {
        String sql = "UPDATE messages SET is_read = TRUE WHERE message_id = ? AND receiver_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}