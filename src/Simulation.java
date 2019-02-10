import java.io.*;
import java.util.*;
import java.lang.String;

import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPSyntaxErrorException;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

public class Simulation {

    public static void main(String[] args) throws IOException {
        String kb = "knowledgeBase.pl";
        KnowledgeBaseCreator creatorKB = new KnowledgeBaseCreator(kb);
//        creatorKB.addTaxisToBase("taxis.csv");
//        creatorKB.addClientToBase("client.csv");
//        creatorKB.addLinesToBase("lines.csv");
//        creatorKB.addRulesToBase();
//        creatorKB.addNodes("nodes.csv");
        try {
            creatorKB.engine.consultFile(kb);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Point.engine = creatorKB.engine;

        // find valid taxis
        JIPEngine engine = Point.engine;
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        LinkedList<Integer> taxiIdList = new LinkedList<>();
        LinkedList<Point> taxiList = new LinkedList<>();


        // add client to our graph
        Point target = null;
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("client(X, Y, _, _, _, _, _)."));
        while ((term = engineQuery.nextSolution()) != null) {
            double clientX = Double.parseDouble(term.getVariablesTable().get("X").toString());
            double clientY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            double minDistance = 10000.0;
            long candidateId = 0;
            JIPQuery engineNodeQuery = engine.openSynchronousQuery(parser.parseTerm("node(X, Y, _, Id, _)."));
            while ((term = engineNodeQuery.nextSolution()) != null) {
                double candidateX = Double.parseDouble(term.getVariablesTable().get("X").toString());
                double candidateY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
                if (Math.sqrt(Math.pow((clientX - candidateX), 2) + Math.pow((clientY - candidateY), 2)) < minDistance) {
                    minDistance = Math.sqrt(Math.pow((clientX - candidateX), 2) + Math.pow((clientY - candidateY), 2));
                    candidateId = (long) Double.parseDouble(term.getVariablesTable().get("Id").toString());
                }
            }
            target = new Point(candidateId);
        }

        // find valid taxis IDs
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("validPairing(ValidTaxi)."));
        while ((term = engineQuery.nextSolution()) != null) {
            taxiIdList.add(Integer.parseInt(term.getVariablesTable().get("ValidTaxi").toString()));
        }
        // add taxi points to our graph.
        for (int taxiId : taxiIdList) {
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("taxi(X, Y,+ " + taxiId + ", _, _, _, _)."));
            double taxiX = 0;
            double taxiY = 0;
            long taxiNodeId = 0;
            double minDistance = 10000.0;
            if ((term = engineQuery.nextSolution()) != null) {
                taxiX = Double.parseDouble(term.getVariablesTable().get("X").toString());
                taxiY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
            }
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("node(X, Y, _, Id, _)."));
            while ((term = engineQuery.nextSolution()) != null) {
                double candidateX = Double.parseDouble(term.getVariablesTable().get("X").toString());
                double candidateY = Double.parseDouble(term.getVariablesTable().get("Y").toString());
                if (Math.sqrt(Math.pow((taxiX - candidateX), 2) + Math.pow((taxiY - candidateY), 2)) < minDistance) {
                    minDistance = Math.sqrt(Math.pow((taxiX - candidateX), 2) + Math.pow((taxiY - candidateY), 2));
                    taxiNodeId = (long) Double.parseDouble(term.getVariablesTable().get("Id").toString());
                }
            }
            Point temp = new Point(taxiNodeId);
            temp.setPathCost(minDistance);
            temp.startingIds.add(taxiId);
            taxiList.add(temp);
        }
        HashMap<long,Point> graph = new HashMap<long, Point>(500);
        PriorityQueue<Point> openSet = new PriorityQueue<>(taxiList.size(), new Comparator<>() {
            @Override
            public int compare(Point taxi1, Point taxi2) {
                double res = taxi1.getHeuristic() + taxi1.getPathCost() - taxi2.getHeuristic() - taxi2.getPathCost();
                if (res > 0)
                    return 1;
                else if (res == 0)
                    return 0;
                else
                    return -1;
            }
        });


        // TODO:: Trexei, bgazei swsto apotelesma, alla trexei poly arga. Mia ekshghsh pou dinw einai oti o A* ginetai
        //  lathos, giati otan ftiaxnoume neo Point (neighbour), auto den elegxetai an yparxei sto openSet
        //  (para mono sto closedSet) ki etsi den pairnei apo to yparxon ta stoixeia pathCost,
        //  opote den exoume swste enhmerwseis sto openSet. Mia lysh se auto einai na exoume domh grhgorou search
        //  gia to openSet (search ginetai ki sthn priority queue se grammiko xrono, den prepei na einai argo kathws
        //  ftanei gyrw sta 150 stoixeia) kaimia domh grhgorou get - retrieve opws array. Kai auta na exoun idia
        //  references profanws. To index sto array twra mporei na einai pedio sto Point (me to pou bazw Point sto array na tou bazw kai to index)
        HashSet<Point> closedSet = new HashSet<>();
        for (Point taxi : taxiList) {
            openSet.clear();
            closedSet.clear();
            graph.clear();
            openSet.add(taxi);
            graph.put(taxi.getNode_id(),taxi);
            boolean found = false;
            while(!openSet.isEmpty()) {
                System.out.println(openSet.size());
                Point top = openSet.peek();
                closedSet.add(top);
                openSet.remove(top);
                if (found && top.getPathCost() + top.getHeuristic() > target.getPathCost()) {
                    break;
                }

                for(long neighbourId : top.getNeighbours()) {
                    Point neighbour = graph.get(neighbourId);
                    if(neighbour == null || top.getPathCost() + top.calculateDistance(neighbour) == neighbour.getPathCost()){
                        neighbour =  new Point(neighbourId);
                        neighbour.setPathCost(top.getPathCost() + top.calculateDistance(neighbour));
                        neighbour.previous.add(top);
                        neighbour.calculateHeuristic(target);
                        openSet.add(neighbour);
                        graph.put(neighbourId,neighbour);
                    }
                    else if (top.getPathCost() + top.calculateDistance(neighbour) < neighbour.getPathCost()) {
                        neighbour.setPathCost(top.getPathCost() + top.calculateDistance(neighbour));
                        neighbour.previous.clear();
                        neighbour.previous.add(top);
                        openSet.add(neighbour);
                    }
                    if (neighbour.equals(target)) {
                        target.previous = neighbour.previous;
                        found = true;
                    }
                }
            }
            KmlWriter outFile = new KmlWriter("Data\\routes" + taxi.getNode_id() + ".kml");
            outFile.printIntroKml();
            LinkedList<Point> pathSoFar = new LinkedList<>();
            pathSoFar.addFirst(target);
            target.printPaths(pathSoFar, outFile);
            outFile.endKml();
        }
    }
}
