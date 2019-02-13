import com.ugos.jiprolog.engine.*;

import java.util.HashSet;
import java.util.LinkedList;

public class Point {

    private double x;
    private double y;
    private long node_id;
    private double heuristic;
    private double pathCost;
    private double pathDist;
    private double rating;
    public HashSet<Point> previous;
    public LinkedList<Integer> startingIds;
    private int taxiId = 0;
    public static JIPEngine engine = null;

    public Point(long nid) {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("node(" + Long.toString(nid) + ", _, X, Y)."));
        term = engineQuery.nextSolution();
        if (term != null) {
            this.x = Double.parseDouble(term.getVariablesTable().get("X").toString());
            this.y = Double.parseDouble(term.getVariablesTable().get("Y").toString());
        }
        this.pathCost = -1;
        this.node_id = nid;
        previous = new HashSet<>();
        startingIds = new LinkedList<>();
    }

    public Point(Point toCopy){
        x = toCopy.getX();
        y = toCopy.getY();
        pathCost = toCopy.getPathCost();
        pathDist = toCopy.getPathDist();
        previous = toCopy.previous;
                //new HashSet<Point>(toCopy.previous);
        heuristic = toCopy.getHeuristic();
        node_id = toCopy.getNode_id();
        taxiId = toCopy.getTaxiId();
        rating = toCopy.getRating();
    }

    public Point(long tId,double x, double y){
        this.x = x;
        this.y = y;
        this.node_id = tId;
    }

    public LinkedList<LinkedList<Long>> getNeighbours() {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        LinkedList<LinkedList<Long>> toReturn = new LinkedList<>();
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("canMoveFromTo(" + node_id + ", Neighbour, Line)."));
        while ((term = engineQuery.nextSolution()) != null) {
            LinkedList<Long> neighbours = new LinkedList<>();
            neighbours.add((long) Double.parseDouble(term.getVariablesTable().get("Neighbour").toString()));
            neighbours.add((long) Double.parseDouble(term.getVariablesTable().get("Line").toString()));
            toReturn.add(neighbours);
        }
        return toReturn;
    }

    public double calculateCost(Point target, Long lineId, String time) {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("traffic(" + Long.toString(lineId)+ "," + time + ",Traffic)."));
        double traffic = 1.5;//default traffic when no traffic data are available
        String maxLimit;
        int limit = 40;
        boolean toll = false;
        double relativeCost = 1;
        double actualCost = calculateDistance(target);
        while ((term = engineQuery.nextSolution()) != null) {
            switch (term.getVariablesTable().get("Traffic").toString()) {
                case ("high"):
                    traffic = 3;
                    break;
                case ("medium"):
                    traffic = 2;
                    break;
                case ("low"):
                    traffic = 1.0;
                    break;
                default:
                    traffic = 2;
                    break;
            }
        }
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("line(" + lineId + ",Type,_,Limit,_,Toll)."));
        while ((term = engineQuery.nextSolution()) != null) {
            maxLimit = term.getVariablesTable().get("Limit").toString();
            if(maxLimit.equals("null")|| maxLimit.isEmpty()){
                switch (term.getVariablesTable().get("Type").toString()) {
                    case("motorway"):
                        limit = 130;
                        break;
                    case("motorway_link"):
                    case("trunk"):
                        limit = 110;
                        break;
                    case("trunk_link"):
                    case("primary"):
                        limit = 90;
                        break;
                    case("primary_link"):
                    case("secondary"):
                        limit = 70;
                        break;
                    case("secondary_link"):
                    case("tertiary"):
                        limit = 50;
                        break;
                    case("tertiary_link"):
                    case("living_street"):
                    case("residential"):
                        limit = 20;
                        break;
                    default:
                        limit = 30;
                        break;
                }
            }
            else
                limit = Integer.parseInt(maxLimit);
            if(!term.getVariablesTable().get("Toll").toString().equals("null") && !term.getVariablesTable().get("Toll").toString().equals("no"))
                toll = true;
        }
        relativeCost = (relativeCost + (toll ? 0.1 : 0))* traffic  * 130 / limit;
        return relativeCost*actualCost;
    }

    public void calculateHeuristic(Point target) {
        heuristic = calculateDistance(target);
    }

    public double calculateDistance(Point target){
        return Haversine.distance(target.getY(),target.getX(), this.y ,this.x);
    }

    @Override
    public int hashCode() {
        return (int) node_id - 2147483647;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (node_id != point.getNode_id()) return false;
        return true;
    }
    //DFS scan of target to previous to find all nearest paths
    public void printPaths(LinkedList<Point> pathSoFar, KmlWriter outFile){
        if(pathSoFar.getLast().previous.isEmpty()){         //if no previous Point print the path
            outFile.newTaxiRoute(pathSoFar);
        }
        else {
            for(Point it:pathSoFar.getLast().previous){      // scan previous and for each add to new path and run recursively
                pathSoFar.addLast(it);
                printPaths(pathSoFar,outFile);
                pathSoFar.removeLast();
            }
        }
    }

    public double getHeuristic() {
        return heuristic;
    }

    public double getPathCost() {
        return pathCost;
    }

    public void setPathCost(double pathCost) {
        this.pathCost = pathCost;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getNode_id() {
        return node_id;
    }

    public double getPathDist() {
        return pathDist;
    }

    public void setPathDist(double pathDist) {
        this.pathDist = pathDist;
    }

    public int getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(int taxiId) {
        this.taxiId = taxiId;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
