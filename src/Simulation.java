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

        // add basic rules and axioms
        creatorKB.addRulesToBase();

        // add data
        creatorKB.addTaxisToBase("taxis.csv");
        creatorKB.addClientToBase("client.csv");
        creatorKB.addLinesToBase("lines.csv");
        creatorKB.addNodes("nodes.csv");
        creatorKB.addTrafficToBase("traffic.csv");

        // every point shares the same engine to avoid time consuming consults.
        Point.engine = creatorKB.engine;

        // initialize prolog engine
        JIPEngine engine = Point.engine;
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;

        // taxiIdList stores taxis' ids
        LinkedList<Integer> taxiIdList = new LinkedList<>();

        // taxiList stores taxis' Points
        Point[] taxiList;

        // find valid taxis' Ids
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

        // create client Point in the same way as above
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

        // tempDist iteratively updates distance to find the minimum distance node
        double[] tempDist = new double[taxiIdList.size()];

        // initialize with large ig value
        for (i = 0; i < tempDist.length; i++) {
            tempDist[i] = 10000.0;
        }

        // store the node_id values of minimun distance nodes
        Long[] taxis = new Long[taxiIdList.size()];

        // loop over every node to find minimum distance node for every taxi and for client
        JIPQuery engineNodeQuery = engine.openSynchronousQuery(parser.parseTerm("node(Id, _, X, Y)."));
        while ((term = engineNodeQuery.nextSolution()) != null) {
            long tempid = (long) Double.parseDouble(term.getVariablesTable().get("Id").toString());
            double tempX = Double.parseDouble(term.getVariablesTable().get("X").toString());
            double tempY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            double tempdistance;
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
                if (tempdistance < tempDist[i]) {
                    tempDist[i] = tempdistance;
                    taxis[i] = tempid;
                }
            }
        }
        // create client and destination points
        Point target = new Point(targetId);
        Point destination = new Point(destId);

        // create taxi Points and add startingIds (for KML file), TaxiId and Rating (for output)
        for (i = 0; i < taxiList.length; i++) {
            Point tempPoint = new Point(taxis[i]);
            tempPoint.startingIds.add((int) taxiList[i].getNode_id());
            tempPoint.setTaxiId((int) taxiList[i].getNode_id());
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("taxi(_, _,+ " + tempPoint.getTaxiId() + ", _, _, _, Rating)."));
            tempPoint.setRating(Double.parseDouble(engineQuery.nextSolution().getVariablesTable().get("Rating").toString()));
            taxiList[i] = tempPoint;
            taxiList[i].setPathCost(tempDist[i]);
        }

        // create two priorityQueues to sort end taxis' routes by rating and by cost
        PriorityQueue<Point> taxiPathsCost = new PriorityQueue<>(taxis.length, new Comparator<>() {
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

        // class to run the algorithm
        AStar solver = new AStar();
        for (Point taxi : taxiList) {
            Point result = solver.solve(taxi, target, time);
            if (result != null) {
                taxiPathsCost.add(new Point(result));
            }
        }

        // output and KML files
        System.out.println("Taxis by time of arrival at the client: ");
        for (i = 0; i < 5; i++) {
            Point currentTaxi = taxiPathsCost.poll();
            if (currentTaxi != null) {

                // sort top 5 taxis by rating
                taxiPathsRating.add(new Point(currentTaxi));
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

        // run A* one additional time to find the route between client and destination
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
