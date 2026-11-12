CREATE DATABASE IF NOT EXISTS routepulse;
USE routepulse;

CREATE TABLE hubs(
    hub_id INT PRIMARY KEY AUTO_INCREMENT,
    hub_code VARCHAR(50) NOT NULL UNIQUE,
    city VARCHAR(100) NOT NULL,
    is_congested BOOLEAN DEFAULT FALSE
);

CREATE TABLE routes(
    route_id INT PRIMARY KEY AUTO_INCREMENT,
    source_hub_id INT NOT NULL,
    dest_hub_id INT NOT NULL,
    distance_km DOUBLE NOT NULL,
    base_cost DOUBLE NOT NULL,
    FOREIGN KEY(source_hub_id) REFERENCES hubs(hub_id),
    FOREIGN KEY(dest_hub_id) REFERENCES hubs(hub_id)
);

CREATE TABLE shipments(
    shipment_id VARCHAR(64) PRIMARY KEY,
    tracking_number VARCHAR(64) NOT NULL UNIQUE,
    current_hub_id INT,
    status VARCHAR(32) NOT NULL,
    weight_kg DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(current_hub_id) REFERENCES hubs(hub_id)
);

CREATE TABLE scan_events(
    scan_id INT PRIMARY KEY AUTO_INCREMENT,
    shipment_id VARCHAR(64) NOT NULL,
    hub_id INT NOT NULL,
    scan_type VARCHAR(32) NOT NULL,
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(shipment_id) REFERENCES shipments(shipment_id),
    FOREIGN KEY(hub_id) REFERENCES hubs(hub_id)
);

CREATE INDEX idx_scan_shipment_time ON scan_events(shipment_id,scanned_at);
CREATE INDEX idx_shipments_status_hub ON shipments(status,current_hub_id);