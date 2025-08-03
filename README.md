# AuctoSphere - Distributed Auction System

This project was developed by me as a part of evaluation on Advanced Object Oriented Programming Laboratoey course at United International University.

## Problem Statement & Purpose

Traditional auctions are limited by location and schedule constraints, hindering participation and market reach. AuctoSphere addresses these issues with a distributed auction system – a networked platform that connects buyers and sellers globally.

Our primary goals are to:

*   **Expand Global Access:** Enable users from anywhere in the world to participate in real-time auctions.
*   **Increase Market Efficiency:** Foster greater competition and more accurate pricing through increased participation.
*   **Enhance Transparency & Accessibility:** Provide a digitized bidding process, ensuring clear record-keeping, reducing errors, and accommodating users with mobility limitations or those in remote areas.
*   **Reduce Operational Costs:** Eliminate the need for physical venues, staff, and associated logistics, offering greater flexibility to participants.

AuctoSphere demonstrates core computer science principles including client-server networking, multithreading for concurrent user management, database design, and modern GUI development using JavaFX.


## Feature List

The system supports three user roles: Buyer, Seller, and Administrator, each with distinct capabilities. Key features include:

*   **User Management:** Secure registration/authentication, role-based access control (RBAC), and profile management.
*   **Item & Auction Management (Seller):** Listing items (with descriptions, images, categories, tags), editing listings, scheduling auctions (start/end times, starting price, optional reserve price), canceling auctions, and a dedicated seller dashboard for monitoring auction status and winner details.
*   **Bidding & Auction Participation (Buyer):** Auction browsing (search/category filtering), bidding (dynamic UI updates upon bid placement), automated notifications (outbid alerts, winning notification), simulated payment system, and a buyer dashboard to track bids and won auctions.
*   **Communication:** In-app messaging between buyers and sellers for auction item discussion.
*   **Administrator Features:** Centralized administrative panel, user management (viewing all registered users), auction monitoring (viewing active auctions), and the ability to delete users and auctions with confirmation prompts.


## Technology & Implementation Details

AuctoSphere is built using a robust Java stack:

1.  **Database:** MySQL (Relational)
    *   Schema: `users`, `items`, `auctions`, `bids`, `messages`. Database management handled by JDBC and the mysql-connector-j driver, managed through Maven. Centralized connection handling via `DatabaseManager`.

2.  **GUI:** JavaFX
    *   Architecture: Declarative UI definition using FXML for separation of design and logic. CSS styling provides a modern dark theme. Controller classes handle user interactions and backend communication. Apache Maven simplifies build management and dependency resolution (JavaFX, MySQL connector).

3.  **Networking:** Java Sockets
    *   Protocol: Custom object-based protocol for client-server communication using `ObjectOutputStream` and `ObjectInputStream`.
    *   Server Architecture: Listens on a specific port, utilizing `ClientHandler` threads to manage individual clients concurrently.
    *   Client Architecture:  Utilizes Java CompletableFutures with correlation IDs for robust request-response handling. Background listener thread handles asynchronous server pushes (e.g., Auction Updates) without blocking the UI.

4.  **Concurrency:** Multithreading
    *   Server: `ClientHandler` threads manage concurrent clients, synchronizing access to shared resources to prevent race conditions. A Java Timer automatically updates auction statuses in the background.
    *   Client:  JavaFX Application Thread remains free for UI rendering. Network operations are handled on background threads (CompletableFuture thread pool and dedicated listenerThread) to maintain a responsive user interface.

---

## License

This project is licensed under the [MIT License](LICENSE).
