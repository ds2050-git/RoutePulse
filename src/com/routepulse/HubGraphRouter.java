package com.routepulse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class HubGraphRouter{
    private final Map<Integer,List<Edge>> adjList=new HashMap<>();

    public static class Edge{
        int targetHub;
        double weight;

        Edge(int targetHub,double weight){
            this.targetHub=targetHub;
            this.weight=weight;
        }
    }

    private static class NodeDistance{
        int node;
        double distance;

        NodeDistance(int node,double distance){
            this.node=node;
            this.distance=distance;
        }
    }

    public void loadGraph(Connection conn) throws SQLException{
        adjList.clear();
        String query="SELECT r.source_hub_id,r.dest_hub_id,r.distance_km,h1.is_congested AS src_congested,h2.is_congested AS dest_congested FROM routes r JOIN hubs h1 ON r.source_hub_id=h1.hub_id JOIN hubs h2 ON r.dest_hub_id=h2.hub_id";

        try(Statement stmt=conn.createStatement();ResultSet rs=stmt.executeQuery(query)){
            while(rs.next()){
                int src=rs.getInt("source_hub_id");
                int dest=rs.getInt("dest_hub_id");
                double dist=rs.getDouble("distance_km");
                boolean srcCongested=rs.getBoolean("src_congested");
                boolean destCongested=rs.getBoolean("dest_congested");

                double forwardWeight=destCongested?dist*1.5:dist;
                double reverseWeight=srcCongested?dist*1.5:dist;

                adjList.computeIfAbsent(src,k->new ArrayList<>()).add(new Edge(dest,forwardWeight));
                adjList.computeIfAbsent(dest,k->new ArrayList<>()).add(new Edge(src,reverseWeight));
            }
        }
    }

    public List<Integer> findShortestPath(int startHub,int endHub){
        Map<Integer,Double> distances=new HashMap<>();
        Map<Integer,Integer> previous=new HashMap<>();
        PriorityQueue<NodeDistance> pq=new PriorityQueue<>(Comparator.comparingDouble(nd->nd.distance));

        for(Integer hub:adjList.keySet()){
            distances.put(hub,Double.MAX_VALUE);
        }

        if(!distances.containsKey(startHub) || !distances.containsKey(endHub)){
            return Collections.emptyList();
        }

        distances.put(startHub,0.0);
        pq.add(new NodeDistance(startHub,0.0));

        while(!pq.isEmpty()){
            NodeDistance current=pq.poll();
            int u=current.node;

            if(u==endHub){
                break;
            }
            if(current.distance>distances.get(u)){
                continue;
            }

            for(Edge edge:adjList.getOrDefault(u,Collections.emptyList())){
                int v=edge.targetHub;
                double newDist=distances.get(u)+edge.weight;

                if(newDist<distances.get(v)){
                    distances.put(v,newDist);
                    previous.put(v,u);
                    pq.add(new NodeDistance(v,newDist));
                }
            }
        }

        List<Integer> path=new ArrayList<>();
        Integer curr=endHub;

        if(!previous.containsKey(curr) && !curr.equals(startHub)){
            return path;
        }

        while(curr!=null){
            path.add(curr);
            curr=previous.get(curr);
        }

        Collections.reverse(path);
        return path;
    }
}