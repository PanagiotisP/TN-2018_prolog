import java.io.*;
import java.util.*;
import java.lang.String;

import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

public class Simulation {

    public static void main(String[] args) throws IOException {
        String kb = "knowledgeBase.pl";
        KnowledgeBaseCreator creatorKB = new KnowledgeBaseCreator(kb);
        creatorKB.addRulesToBase();
        creatorKB.addTaxisToBase("taxis.csv");
        creatorKB.addClientToBase("client.csv");
        creatorKB.addLinesToBase("lines.csv");
        creatorKB.addNodes("nodes.csv");

        creatorKB.addTrafficToBase("traffic.csv");
        Point.engine = creatorKB.engine;

        // find valid taxis
        JIPEngine engine = Point.engine;
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        LinkedList<Integer> taxiIdList = new LinkedList<>();
        Point[] taxiList;


        engineQuery = engine.openSynchronousQuery(parser.parseTerm("validPairing(ValidTaxi)."));
        while ((term = engineQuery.nextSolution()) != null) {
            int tempTaxid = Integer.parseInt(term.getVariablesTable().get("ValidTaxi").toString());
            taxiIdList.add(tempTaxid);

        }
        int i = 0;
        taxiList = new Point[taxiIdList.size()];
        for (int taxiId : taxiIdList) {
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("taxi(X, Y,+ " + taxiId + ", _, _, _, _)."));
            double taxiX = 0;
            double taxiY = 0;
            if ((term = engineQuery.nextSolution()) != null) {
                taxiX = Double.parseDouble(term.getVariablesTable().get("X").toString());
                taxiY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            }
            taxiList[i++] = (new Point(taxiId, taxiX, taxiY));
        }

        engineQuery = engine.openSynchronousQuery(parser.parseTerm("client(X, Y, DestX,DestY,Time, _, _)."));
        double clientX = 0;
        double clientY = 0;
        double destinationX = 0;
        double destinationY = 0;
        String time = "25";
        if ((term = engineQuery.nextSolution()) != null) {
            clientX = Double.parseDouble(term.getVariablesTable().get("X").toString());
            clientY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            String s = term.getVariablesTable().get("Time").toString();
            time = s.substring(2, 4);
            destinationX = Double.parseDouble(term.getVariablesTable().get("DestX").toString());
            destinationY = Double.parseDouble(term.getVariablesTable().get("DestY").toString());
        }
        double minDistance = 10000.0;
        long targetId = 0;
        double destDist = 10000.0;
        long destId = 0;

        JIPQuery engineNodeQuery = engine.openSynchronousQuery(parser.parseTerm("node(Id, _, X, Y)."));
        double[] tempdist = new double[taxiIdList.size()];
        for (i = 0; i < tempdist.length; i++) {
            tempdist[i] = 10000.0;
        }
        Long[] taxis = new Long[taxiIdList.size()];
        while ((term = engineNodeQuery.nextSolution()) != null) {
            long tempid = (long) Double.parseDouble(term.getVariablesTable().get("Id").toString());
            double tempX = Double.parseDouble(term.getVariablesTable().get("X").toString());
            double tempY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            double tempdistance;
//            if(!graph.containsKey(tempid)) {
//                Point tempPoint = new Point(tempid);
//                graph.put(tempid, tempPoint);
            tempdistance = Haversine.distance(clientX,clientY,tempX,tempY);
            if (tempdistance < minDistance) {
                targetId = tempid;
                minDistance = tempdistance;
            }
            tempdistance = Haversine.distance(destinationX,destinationY,tempX,tempY);
            if (tempdistance < destDist) {
                destId = tempid;
                destDist = tempdistance;
            }
            for (i = 0; i < taxiList.length; i++) {
                tempdistance = Haversine.distance(taxiList[i].getX(),taxiList[i].getY(),tempX,tempY);
                if (tempdistance < tempdist[i]) {
                    tempdist[i] = tempdistance;
                    taxis[i] = tempid;
                }
            }
        }
        Point target = new Point(targetId);
        Point destination = new Point(destId);
        for (i = 0; i < taxiList.length; i++) {
            Point tempPoint = new Point(taxis[i]);
            tempPoint.startingIds.add((int) taxiList[i].getNode_id());
            tempPoint.setTaxiId((int) taxiList[i].getNode_id());
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("taxi(_, _,+ " + tempPoint.getTaxiId() + ", _, _, _, Rating)."));
            tempPoint.setRating(Double.parseDouble(engineQuery.nextSolution().getVariablesTable().get("Rating").toString()));
            taxiList[i] = tempPoint;
            taxiList[i].setPathCost(tempdist[i]);
        }
        // add client to our graph


        PriorityQueue<Point> taxiPaths = new PriorityQueue<>(taxis.length, new Comparator<>() {
            @Override
            public int compare(Point taxi1, Point taxi2) {
                double res = taxi1.getPathCost() - taxi2.getPathCost();
                if (res > 0)
                    return 1;
                else if (res == 0)
                    return 0;
                else
                    return -1;
            }
        });

        PriorityQueue<Point> taxiPathsRating = new PriorityQueue<>(taxis.length, new Comparator<>() {
            @Override
            public int compare(Point taxi1, Point taxi2) {
                double res = taxi2.getRating() - taxi1.getRating();
                if (res > 0)
                    return 1;
                else if (res == 0)
                    return 0;
                else
                    return -1;
            }
        });

        AStar solver = new AStar();
        for (Point taxi : taxiList) {
            Point result = solver.solve(taxi, target, time);
            if (result != null) {
                taxiPaths.add(new Point(result));
                taxiPathsRating.add(new Point(result));
            }
        }


        System.out.println("Taxis by time of arrival to the client: ");
        for (i = 0; i < 5; i++) {
            Point currentTaxi = taxiPaths.poll();
            if (currentTaxi != null) {
                System.out.println("Taxi " + currentTaxi.getTaxiId() + " with approx. time " + Math.round(currentTaxi.getPathCost() / 13 * 6) + " min and distance " + Math.round(currentTaxi.getPathDist()*100)/100.0 + " km.");
                KmlWriter outFile = new KmlWriter("Data\\routes" + Integer.toString(i) + ".kml");
                outFile.printIntroKml();
                LinkedList<Point> pathSoFar = new LinkedList<>();
                pathSoFar.addFirst(currentTaxi);
                target.printPaths(pathSoFar, outFile);
                outFile.endKml();
            }
        }
        System.out.println("Taxis by rating: ");
        for (i = 0; i < 5; i++) {
            Point currentTaxi = taxiPathsRating.poll();
            if (currentTaxi != null) {
                System.out.println("Taxi " + currentTaxi.getTaxiId() + " with rating " + currentTaxi.getRating() + '.');
            }
        }

        Point startRoute = new Point(target.getNode_id());
        startRoute.startingIds.add(0);
        Point finalDestination = solver.solve(startRoute, destination,Integer.toString(Integer.parseInt(time) + 1));

        if(finalDestination != null){
            double duration = (finalDestination.getPathCost() * 60) / 130 ;
            System.out.println("Destination is "+ Math.round(finalDestination.getPathDist()*100)/100.0 + "km away from client. Approximate taxi ride duration: "+ Math.round(duration)+" min.");
            KmlWriter outFile = new KmlWriter("Data\\route_to_destination.kml");
            outFile.printIntroKml();
            LinkedList<Point> pathSoFar = new LinkedList<>();
            pathSoFar.addFirst(finalDestination);
            destination.printPaths(pathSoFar, outFile);
            outFile.endKml();
        }
    }
}
