package com.university.auctionsystem.server.services;

import com.university.auctionsystem.server.DatabaseManager;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.model.Role;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private DatabaseManager dbManager;

    public UserService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public User registerUser(User user) {

        String pseudoHashedPassword = new StringBuilder(user.getPasswordHash()).reverse().toString();

        String sql = "INSERT INTO users (username, password_hash, email, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, pseudoHashedPassword);
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getRole().name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                    user.setPasswordHash(null);
                    return user;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public User loginUser(String username, String password) {
        String pseudoHashedPassword = new StringBuilder(password).reverse().toString();
        String sql = "SELECT user_id, username, email, role, created_at FROM users WHERE username = ? AND password_hash = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, pseudoHashedPassword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(Role.valueOf(rs.getString("role")));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserById(int userId) {
        String sql = "SELECT user_id, username, email, role, created_at FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(Role.valueOf(rs.getString("role")));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserProfile(int userId) {
        return getUserById(userId);
    }

    public boolean updateUserProfile(int userId, String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return false;
        }
        String sql = "UPDATE users SET email = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setString(1, newEmail.trim());
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("duplicate entry") && e.getMessage().toLowerCase().contains("email")) {
                System.err.println("UserService: Attempt to update to an already existing email.");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, email, role, created_at FROM users ORDER BY username ASC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(Role.valueOf(rs.getString("role")));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public boolean deleteUserAsAdmin(int userIdToDelete) {
        User user = getUserById(userIdToDelete);
        if (user == null) {
            System.err.println("UserService: Cannot delete. User ID " + userIdToDelete + " not found.");
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            System.err.println("UserService: Deleting Admin users is restricted through this method.");
            return false;
        }
        String deleteMessagesSql = "DELETE FROM messages WHERE sender_id = ? OR receiver_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(deleteMessagesSql)) {
            pstmt.setInt(1, userIdToDelete);
            pstmt.setInt(2, userIdToDelete);
            pstmt.executeUpdate();
            System.out.println("UserService: Deleted messages for user ID " + userIdToDelete);
        } catch (SQLException e) {
            System.err.println("UserService: Error deleting messages for user " + userIdToDelete + ": " + e.getMessage());
        }
        String deleteBidsSql = "DELETE FROM bids WHERE bidder_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(deleteBidsSql)) {
            pstmt.setInt(1, userIdToDelete);
            pstmt.executeUpdate();
            System.out.println("UserService: Deleted bids for user ID " + userIdToDelete);
        } catch (SQLException e) {
            System.err.println("UserService: Error deleting bids for user " + userIdToDelete + ": " + e.getMessage());
        }

        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, userIdToDelete);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("UserService: User ID " + userIdToDelete + " deleted successfully by admin.");
                return true;
            } else {
                System.err.println("UserService: Deleting user ID " + userIdToDelete + " failed, no rows affected.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("UserService: SQL Error deleting user " + userIdToDelete + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}