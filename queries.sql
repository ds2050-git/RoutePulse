USE routepulse;

WITH dwell_times AS(
    SELECT
        shipment_id,
        hub_id,
        scan_type,
        scanned_at,
        TIMESTAMPDIFF(MINUTE,LAG(scanned_at) OVER(PARTITION BY shipment_id ORDER BY scanned_at),scanned_at) AS minutes_since_prev_scan
    FROM scan_events
)
SELECT
    d.shipment_id,
    h.hub_code,
    h.city,
    d.scan_type,
    d.minutes_since_prev_scan
FROM dwell_times d
JOIN hubs h ON d.hub_id=h.hub_id
WHERE d.minutes_since_prev_scan>120;

SELECT
    h.hub_code,
    h.city,
    COUNT(s.shipment_id) AS active_shipments,
    SUM(s.weight_kg) AS total_load_kg
FROM hubs h
LEFT JOIN shipments s ON h.hub_id=s.current_hub_id AND s.status!='DELIVERED'
GROUP BY h.hub_id,h.hub_code,h.city
ORDER BY total_load_kg DESC;