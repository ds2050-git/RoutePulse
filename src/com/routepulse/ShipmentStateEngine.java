package com.routepulse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class ShipmentStateEngine{
    private static final Map<String,Set<String>> VALID_TRANSITIONS=Map.of(
        "MANIFESTED",Set.of("IN_TRANSIT"),
        "IN_TRANSIT",Set.of("HUB_SCAN"),
        "HUB_SCAN",Set.of("IN_TRANSIT","OUT_FOR_DELIVERY"),
        "OUT_FOR_DELIVERY",Set.of("DELIVERED","NDR"),
        "NDR",Set.of("OUT_FOR_DELIVERY","HUB_SCAN")
    );

    public static void updateStatus(Connection conn,String shipmentId,int newHubId,String newStatus) throws SQLException{
        conn.setAutoCommit(false);
        try{
            String lockQuery="SELECT status FROM shipments WHERE shipment_id=? FOR UPDATE";
            String currentStatus=null;

            try(PreparedStatement lockStmt=conn.prepareStatement(lockQuery)){
                lockStmt.setString(1,shipmentId);
                try(ResultSet rs=lockStmt.executeQuery()){
                    if(!rs.next()){
                        throw new SQLException("Shipment not found: "+shipmentId);
                    }
                    currentStatus=rs.getString("status");
                }
            }

            Set<String> allowed=VALID_TRANSITIONS.getOrDefault(currentStatus,Set.of());
            if(!allowed.contains(newStatus)){
                throw new IllegalStateException("Invalid transition from "+currentStatus+" to "+newStatus);
            }

            String updateQuery="UPDATE shipments SET current_hub_id=?,status=? WHERE shipment_id=?";
            try(PreparedStatement pstmt=conn.prepareStatement(updateQuery)){
                pstmt.setInt(1,newHubId);
                pstmt.setString(2,newStatus);
                pstmt.setString(3,shipmentId);
                pstmt.executeUpdate();
            }

            String insertScan="INSERT INTO scan_events(shipment_id,hub_id,scan_type) VALUES(?,?,?)";
            try(PreparedStatement pstmtScan=conn.prepareStatement(insertScan)){
                pstmtScan.setString(1,shipmentId);
                pstmtScan.setInt(2,newHubId);
                pstmtScan.setString(3,newStatus);
                pstmtScan.executeUpdate();
            }

            conn.commit();
        }catch(Exception e){
            conn.rollback();
            if(e instanceof SQLException){
                throw (SQLException)e;
            }
            throw new SQLException(e.getMessage(),e);
        }finally{
            conn.setAutoCommit(true);
        }
    }

    public static void printShipmentAuditLog(Connection conn,String shipmentId) throws SQLException{
        String query="SELECT s.scan_id,s.scan_type,s.scanned_at,h.hub_code,h.city FROM scan_events s JOIN hubs h ON s.hub_id=h.hub_id WHERE s.shipment_id=? ORDER BY s.scanned_at ASC";
        try(PreparedStatement pstmt=conn.prepareStatement(query)){
            pstmt.setString(1,shipmentId);
            try(ResultSet rs=pstmt.executeQuery()){
                while(rs.next()){
                    System.out.println(
                        rs.getInt("scan_id")+" | "+
                        rs.getString("scan_type")+" | "+
                        rs.getString("hub_code")+" ("+rs.getString("city")+") | "+
                        rs.getTimestamp("scanned_at")
                    );
                }
            }
        }
    }
}