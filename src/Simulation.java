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

        engineQuery = engine.openSynchronousQuery(parser.parseTerm("client(X, Y, _, _,Time, _, _)."));
        double clientX = 0;
        double clientY = 0;
        String time = "25";
        if ((term = engineQuery.nextSolution()) != null) {
            clientX = Double.parseDouble(term.getVariablesTable().get("X").toString());
            clientY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            String s = term.getVariablesTable().get("Time").toString();
            time = s.substring(2, 4);
        }
        double minDistance = 10000.0;
        long targetId = 0;

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
            tempdistance = Math.sqrt(Math.pow((clientX - tempX), 2) + Math.pow((clientY - tempY), 2));
            if (tempdistance < minDistance) {
                targetId = tempid;
                minDistance = tempdistance;
            }
            for (i = 0; i < taxiList.length; i++) {
                tempdistance = Math.sqrt(Math.pow((taxiList[i].getX() - tempX), 2) + Math.pow((taxiList[i].getY() - tempY), 2));
                if (tempdistance < tempdist[i]) {
                    tempdist[i] = tempdistance;
                    taxis[i] = tempid;
                }
            }
        }
        Point target = new Point(targetId);
        for (i = 0; i < taxiList.length; i++) {
            Point tempPoint = new Point(taxis[i]);
            tempPoint.startingIds.add((int) taxiList[i].getNode_id());
            tempPoint.setTaxiId((int) taxiList[i].getNode_id());
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


        AStar solver = new AStar();
        for (Point taxi : taxiList) {
            Point result = solver.solve(taxi, target, time);
            if (result != null) {
                taxiPaths.add(new Point(result));
            }
        }
        System.out.println("Taxis by time of arrival to the client: ");
        for(i = 0; i < 6; i++) {
            Point currentTaxi = taxiPaths.poll();
            if(currentTaxi != null) {
                System.out.println(currentTaxi.getTaxiId() + " with time " + currentTaxi.getPathCost() / 130 + " and distance " + currentTaxi.getPathDist() + '.');
                KmlWriter outFile = new KmlWriter("Data\\routes" + Integer.toString(i) + ".kml");
                outFile.printIntroKml();
                LinkedList<Point> pathSoFar = new LinkedList<>();
                pathSoFar.addFirst(currentTaxi);
                target.printPaths(pathSoFar, outFile);
                outFile.endKml();
            }
        }
    }
}
