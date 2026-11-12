package com.routepulse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class Main{
    public static void main(String[] args){
        try(Connection conn=DatabaseConnection.getConnection()){
            seedSampleData(conn);

            HubGraphRouter router=new HubGraphRouter();
            router.loadGraph(conn);

            List<Integer> route=router.findShortestPath(1,4);
            System.out.println("Optimal Hub Path: "+route);

            ShipmentStateEngine.updateStatus(conn,"SHP-1001",2,"IN_TRANSIT");
            ShipmentStateEngine.updateStatus(conn,"SHP-1001",3,"HUB_SCAN");
            ShipmentStateEngine.updateStatus(conn,"SHP-1001",4,"OUT_FOR_DELIVERY");
            ShipmentStateEngine.updateStatus(conn,"SHP-1001",4,"DELIVERED");

            ShipmentStateEngine.printShipmentAuditLog(conn,"SHP-1001");
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    private static void seedSampleData(Connection conn) throws SQLException{
        String insertHub="INSERT IGNORE INTO hubs(hub_id,hub_code,city,is_congested) VALUES(?,?,?,?)";
        try(PreparedStatement ps=conn.prepareStatement(insertHub)){
            Object[][] hubs={
                {1,"DEL-GW1","Gurgaon",false},
                {2,"JAI-HB1","Jaipur",false},
                {3,"AMD-HB1","Ahmedabad",true},
                {4,"BOM-GW1","Mumbai",false}
            };
            for(Object[] h:hubs){
                ps.setInt(1,(int)h[0]);
                ps.setString(2,(String)h[1]);
                ps.setString(3,(String)h[2]);
                ps.setBoolean(4,(boolean)h[3]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        String insertRoute="INSERT IGNORE INTO routes(route_id,source_hub_id,dest_hub_id,distance_km,base_cost) VALUES(?,?,?,?,?)";
        try(PreparedStatement ps=conn.prepareStatement(insertRoute)){
            Object[][] routes={
                {1,1,2,270.0,1200.0},
                {2,2,3,660.0,2800.0},
                {3,3,4,530.0,2300.0},
                {4,2,4,1150.0,4500.0}
            };
            for(Object[] r:routes){
                ps.setInt(1,(int)r[0]);
                ps.setInt(2,(int)r[1]);
                ps.setInt(3,(int)r[2]);
                ps.setDouble(4,(double)r[3]);
                ps.setDouble(5,(double)r[4]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        String insertShipment="INSERT IGNORE INTO shipments(shipment_id,tracking_number,current_hub_id,status,weight_kg) VALUES(?,?,?,?,?)";
        try(PreparedStatement ps=conn.prepareStatement(insertShipment)){
            ps.setString(1,"SHP-1001");
            ps.setString(2,"DLV99887766");
            ps.setInt(3,1);
            ps.setString(4,"MANIFESTED");
            ps.setDouble(5,2.4);
            ps.executeUpdate();
        }
    }
}