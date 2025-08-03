-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: auction_system_db
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `auctions`
--

DROP TABLE IF EXISTS `auctions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auctions` (
  `auction_id` int NOT NULL AUTO_INCREMENT,
  `item_id` int NOT NULL,
  `start_time` timestamp NOT NULL,
  `end_time` timestamp NOT NULL,
  `start_price` decimal(10,2) NOT NULL,
  `reserve_price` decimal(10,2) DEFAULT NULL,
  `current_highest_bid` decimal(10,2) DEFAULT '0.00',
  `winning_bidder_id` int DEFAULT NULL,
  `status` enum('UPCOMING','ACTIVE','ENDED','CANCELLED') DEFAULT 'UPCOMING',
  `payment_status` enum('PENDING','PAID','FAILED','REFUNDED') DEFAULT 'PENDING',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`auction_id`),
  UNIQUE KEY `item_id` (`item_id`),
  KEY `winning_bidder_id` (`winning_bidder_id`),
  CONSTRAINT `auctions_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`item_id`),
  CONSTRAINT `auctions_ibfk_2` FOREIGN KEY (`winning_bidder_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auctions`
--

LOCK TABLES `auctions` WRITE;
/*!40000 ALTER TABLE `auctions` DISABLE KEYS */;
INSERT INTO `auctions` VALUES (1,1,'2025-06-25 11:15:00','2025-06-25 11:16:00',50.00,NULL,0.00,NULL,'ENDED','PENDING','2025-06-25 17:13:11'),(2,2,'2025-06-25 11:30:00','2025-06-25 12:28:00',5.00,5.00,0.00,NULL,'ENDED','PENDING','2025-06-25 17:29:51'),(3,3,'2025-06-25 12:19:00','2025-06-26 13:16:00',100.00,100.00,250.00,5,'ENDED','PAID','2025-06-25 18:18:59'),(4,4,'2025-06-25 13:30:00','2025-06-25 13:33:00',1000.00,NULL,1000.00,NULL,'ENDED','PENDING','2025-06-25 19:29:06'),(5,5,'2025-06-25 13:37:00','2025-06-25 13:47:00',2000.00,NULL,2000.00,NULL,'ENDED','PENDING','2025-06-25 19:36:07'),(6,6,'2025-06-25 13:40:00','2025-06-25 14:00:00',20.00,NULL,20.00,NULL,'ENDED','PENDING','2025-06-25 19:38:25'),(7,7,'2025-06-25 14:45:00','2025-06-25 14:48:00',40.00,NULL,40.00,NULL,'ENDED','PENDING','2025-06-25 19:43:44'),(9,8,'2025-06-25 14:46:00','2025-06-26 14:49:00',28.00,NULL,28.00,NULL,'ENDED','PENDING','2025-06-25 19:45:31'),(10,9,'2025-06-25 14:46:00','2025-06-26 07:55:00',10.00,NULL,54.00,5,'ENDED','PAID','2025-06-25 19:49:56'),(11,10,'2025-06-25 19:57:00','2025-06-25 20:05:00',60.00,NULL,80.00,4,'ENDED','PAID','2025-06-25 20:02:07'),(19,13,'2025-06-27 04:38:00','2025-06-28 04:38:00',56.00,NULL,56.00,NULL,'CANCELLED','PENDING','2025-06-26 09:38:48'),(20,14,'2025-06-29 01:49:00','2025-06-29 02:02:00',150.00,NULL,150.00,NULL,'ENDED','PENDING','2025-06-29 06:55:23'),(21,15,'2025-06-29 07:15:00','2025-06-30 07:20:00',120.00,NULL,300.00,4,'ENDED','PENDING','2025-06-29 07:14:16'),(22,16,'2025-07-02 03:55:00','2025-07-03 03:59:00',120.00,NULL,230.00,4,'ENDED','PENDING','2025-07-02 03:54:50'),(23,17,'2025-07-02 04:05:00','2025-07-03 04:06:00',120.00,NULL,130.00,5,'ENDED','PENDING','2025-07-02 04:04:14'),(24,18,'2025-07-02 04:09:00','2025-07-02 04:11:00',10.00,NULL,20.00,5,'ENDED','PAID','2025-07-02 04:08:27');
/*!40000 ALTER TABLE `auctions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bids`
--

DROP TABLE IF EXISTS `bids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bids` (
  `bid_id` int NOT NULL AUTO_INCREMENT,
  `auction_id` int NOT NULL,
  `bidder_id` int NOT NULL,
  `bid_amount` decimal(10,2) NOT NULL,
  `bid_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`bid_id`),
  KEY `auction_id` (`auction_id`),
  KEY `bidder_id` (`bidder_id`),
  CONSTRAINT `bids_ibfk_1` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`auction_id`),
  CONSTRAINT `bids_ibfk_2` FOREIGN KEY (`bidder_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bids`
--

LOCK TABLES `bids` WRITE;
/*!40000 ALTER TABLE `bids` DISABLE KEYS */;
INSERT INTO `bids` VALUES (1,3,4,150.00,'2025-06-25 19:26:54'),(2,3,5,250.00,'2025-06-25 19:35:02'),(3,10,4,20.00,'2025-06-25 19:50:36'),(4,10,5,30.00,'2025-06-25 19:51:14'),(5,10,4,31.00,'2025-06-25 19:52:17'),(6,10,4,32.00,'2025-06-25 19:52:21'),(7,10,4,33.00,'2025-06-25 19:55:56'),(8,11,5,70.00,'2025-06-25 20:02:53'),(9,11,4,80.00,'2025-06-25 20:03:39'),(10,10,5,54.00,'2025-06-25 20:52:39'),(11,21,4,140.00,'2025-06-29 07:39:08'),(12,21,5,160.00,'2025-06-29 07:39:48'),(13,21,5,180.00,'2025-06-29 07:53:58'),(14,21,4,190.00,'2025-06-29 07:54:14'),(15,21,4,200.00,'2025-06-29 07:56:04'),(16,21,5,210.00,'2025-06-29 07:56:25'),(17,21,5,220.00,'2025-06-29 16:05:27'),(18,21,4,240.00,'2025-06-29 16:18:40'),(19,21,5,250.00,'2025-06-30 06:21:36'),(20,21,4,300.00,'2025-06-30 06:33:22'),(21,22,4,150.00,'2025-07-02 03:56:02'),(22,22,5,190.00,'2025-07-02 03:56:17'),(23,23,5,130.00,'2025-07-02 04:05:12'),(24,24,5,20.00,'2025-07-02 04:09:41'),(25,22,5,200.00,'2025-07-02 04:40:53'),(26,22,4,210.00,'2025-07-02 04:42:14'),(27,22,5,220.00,'2025-07-02 04:42:49'),(28,22,4,230.00,'2025-07-02 04:43:39');
/*!40000 ALTER TABLE `bids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `items`
--

DROP TABLE IF EXISTS `items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `items` (
  `item_id` int NOT NULL AUTO_INCREMENT,
  `seller_id` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `image_path` varchar(255) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`item_id`),
  KEY `seller_id` (`seller_id`),
  CONSTRAINT `items_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `items`
--

LOCK TABLES `items` WRITE;
/*!40000 ALTER TABLE `items` DISABLE KEYS */;
INSERT INTO `items` VALUES (1,2,'ring','ring from 1970','','ornament','ring,ornament,asif','2025-06-25 17:10:04'),(2,2,'shirt','shirt of a famous person','','dress','dress,shirt,asif','2025-06-25 17:29:00'),(3,2,'guitar','guitar of 1990','','instruments','music,instruments,guitar','2025-06-25 18:18:18'),(4,2,'Car','old car','','vehicle','car,vehicle','2025-06-25 19:28:27'),(5,2,'car2','car2 edit',NULL,'vehicle','car,vehicle','2025-06-25 19:31:13'),(6,2,'toy','toy','','toy','toy','2025-06-25 19:37:21'),(7,2,'toy2','toy2','','toy','toy','2025-06-25 19:43:22'),(8,2,'toy3','toy3','','toy','toy','2025-06-25 19:45:20'),(9,2,'toy4','toy4','','toy','toy','2025-06-25 19:48:26'),(10,2,'pop','pop','','pop','pop','2025-06-25 20:01:07'),(11,2,'pop2','pop2','','pop','pop','2025-06-26 07:42:09'),(12,2,'lol','lol test',NULL,'lol','lol','2025-06-26 09:29:20'),(13,2,'io','io test 1','','io','io','2025-06-26 09:37:03'),(14,2,'book','book','','book','book','2025-06-29 06:51:07'),(15,2,'book2','book2','','book','book','2025-06-29 07:13:46'),(16,2,'final','final','','final','final','2025-07-02 03:53:19'),(17,2,'final 2','final2','','final','','2025-07-02 04:03:45'),(18,2,'final 3','ooo','','ooo','','2025-07-02 04:07:09');
/*!40000 ALTER TABLE `items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `message_id` int NOT NULL AUTO_INCREMENT,
  `auction_id` int NOT NULL,
  `sender_id` int NOT NULL,
  `receiver_id` int NOT NULL,
  `message_text` text NOT NULL,
  `timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`message_id`),
  KEY `auction_id` (`auction_id`),
  KEY `sender_id` (`sender_id`),
  KEY `receiver_id` (`receiver_id`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`auction_id`),
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `messages_ibfk_3` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` VALUES (4,10,5,2,'condition?','2025-06-25 23:48:58',1),(5,10,2,5,'mint','2025-06-26 06:34:00',1),(6,10,5,2,'ok','2025-06-26 06:34:22',1),(7,10,1,2,'is it a car?','2025-06-26 07:00:44',1),(8,10,2,1,'no','2025-06-26 07:01:13',1),(10,3,4,2,'??','2025-06-26 10:00:07',1),(11,21,4,2,'lol','2025-06-29 07:15:29',1),(12,21,5,2,'hi','2025-06-30 04:49:41',1),(13,21,2,5,'kol','2025-06-30 05:08:48',1),(14,21,1,2,'hi','2025-06-30 06:29:53',1),(15,22,1,2,'hi','2025-07-02 03:57:47',1),(16,22,2,1,'lol','2025-07-02 03:58:03',1);
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `email` varchar(100) NOT NULL,
  `role` enum('BUYER','SELLER','ADMIN') NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','adminpass','admin2@auction.com','ADMIN','2025-06-25 13:36:52'),(2,'asif','321fisa','asif@gmail.com','SELLER','2025-06-25 16:25:56'),(4,'rafi','321ifar','rafi@gmail.com','BUYER','2025-06-25 16:45:55'),(5,'joy','321yoj','joy@yahoo.com','BUYER','2025-06-25 19:34:39');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-08-03 12:41:51
