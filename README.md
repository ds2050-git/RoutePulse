# RoutePulse

RoutePulse is a backend shipment tracking and dynamic routing engine designed to model high-volume hub-and-spoke logistics networks. It features graph-based path optimization with congestion weighting and strict transactional state validation.

## Tech Stack
* **Language:** Java 17+
* **Database:** MySQL (Relational Schema, Indexes, ACID Transactions)
* **Connectivity:** JDBC (MySQL Connector/J)
* **Algorithms:** Dijkstra's Shortest Path with Dynamic Edge Weighting

---

## Core Features
1. **Graph-Based Hub Routing:** Models city hubs and transit corridors as a weighted directed graph, dynamically adjusting edge weights based on real-time hub congestion flags.
2. **Transactional State Machine:** Enforces strict shipment lifecycle transitions (`MANIFESTED` -> `IN_TRANSIT` -> `HUB_SCAN` -> `OUT_FOR_DELIVERY` -> `DELIVERED / NDR`) using database locks (`FOR UPDATE`) and ACID commits.
3. **Audit Logging & Analytics:** Tracks scan events per hub and includes SQL window functions/CTEs for dwell-time analysis and active load monitoring.

---
