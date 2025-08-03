package com.university.auctionsystem.server;

import com.university.auctionsystem.server.services.AuctionService;
import com.university.auctionsystem.server.services.ItemService;
import com.university.auctionsystem.server.services.UserService;
import com.university.auctionsystem.server.services.MessageService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionServer {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private DatabaseManager dbManager;
    private UserService userService;
    private ItemService itemService;
    private AuctionService auctionService;
    private boolean running = true;
    private MessageService messageService;

    private final Set<ClientHandler> activeClientHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());


    public AuctionServer() {
        dbManager = new DatabaseManager();
        userService = new UserService(dbManager);
        itemService = new ItemService(dbManager);
        messageService = new MessageService(dbManager, userService);
        auctionService = new AuctionService(dbManager, itemService, activeClientHandlers);
        itemService.setAuctionService(auctionService);
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Auction Server started on port " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));


            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket, userService, itemService, auctionService, messageService, activeClientHandlers);
                    clientHandler.start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + PORT + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        running = false;
        System.out.println("Shutting down server...");
        for (ClientHandler handler : activeClientHandlers) {
            handler.closeConnectionGracefully();
        }
        activeClientHandlers.clear();
        if (auctionService != null) {
            auctionService.shutdownScheduler();
        }
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        System.out.println("Server shutdown complete.");
    }


    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        server.startServer();
    }


}