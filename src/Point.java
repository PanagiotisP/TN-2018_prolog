import com.ugos.jiprolog.engine.*;

import java.util.HashSet;
import java.util.LinkedList;

public class Point {

    // point longtitude
    private double x;

    // point latitude
    private double y;

    // a number to distinguish points (apart from several points at the beginning which do not correlate with a certain
    // node, take node id)
    private long node_id;

    // heuristic value
    private double heuristic;
    private double pathCost;
    private double pathDist;
    private double rating;

    // taxi id. used to return sorted output
    private int taxiId = 0;

    // previous point(s). hashset because on the previous programs, multiple - same distance paths were asked
    public HashSet<Point> previous;

    // startingTaxisIds. list because maybe more than one taxi can start from the same point
    public LinkedList<Integer> startingIds;

    // prolog engine used for queries
    public static JIPEngine engine = null;

    // initialise a new Point with given node_id
    public Point(long nid) {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;

        // get coordinates from knowledge base
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

    // sort of copy constructor
    public Point(Point toCopy){
        x = toCopy.getX();
        y = toCopy.getY();
        pathCost = toCopy.getPathCost();
        pathDist = toCopy.getPathDist();
        previous = toCopy.previous;
        heuristic = toCopy.getHeuristic();
        node_id = toCopy.getNode_id();
        taxiId = toCopy.getTaxiId();
        rating = toCopy.getRating();
    }

    public Point(long tId, double x, double y){
        this.x = x;
        this.y = y;
        this.node_id = tId;
    }

    // get neighbours by asking knowledge base to return all statements canMoveFromTo with a given node_id
    // returned item is a LinkedList of LinkedLists (same number as neighbours) of two items each,
    // neighbour id and connecting lind_id (used as tuples/pairs)
    public LinkedList<LinkedList<Long>> getNeighbours() {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        LinkedList<LinkedList<Long>> toReturn = new LinkedList<>();
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("canMoveFromTo(" + node_id + ", Neighbour, Line)."));
        while ((term = engineQuery.nextSolution()) != null) {
            LinkedList<Long> neighbours = new LinkedList<>();

            // return neighbour id and line id
            neighbours.add((long) Double.parseDouble(term.getVariablesTable().get("Neighbour").toString()));
            neighbours.add((long) Double.parseDouble(term.getVariablesTable().get("Line").toString()));
            toReturn.add(neighbours);
        }
        return toReturn;
    }

    // calculate cost to a point using a certain line, on given time
    public double calculateCost(Point target, Long lineId, String time) {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;

        //default traffic when no traffic data are available
        double traffic = 2.5;
        String maxLimit;

        // base speed limit, in case the query returns null
        int limit = 40;

        // value for tolls
        boolean toll = false;
        double relativeCost = 1;

        // actual cost to multiply by a coefficient is distance between the 2 points
        double actualCost = calculateDistance(target);

        engineQuery = engine.openSynchronousQuery(parser.parseTerm("traffic(" + Long.toString(lineId)+ "," + time + ",Traffic)."));
        while ((term = engineQuery.nextSolution()) != null) {

            // according to traffic intensity, a value between 1 and 5 is set. For example, on high traffic we need 5
            // times more time to drive the same distance
            switch (term.getVariablesTable().get("Traffic").toString()) {
                case ("high"):
                    traffic = 5;
                    break;
                case ("medium"):
                    traffic = 3;
                    break;
                case ("low"):
                    traffic = 1.0;
                    break;
                default:
                    traffic = 2.5;
                    break;
            }
        }

        engineQuery = engine.openSynchronousQuery(parser.parseTerm("line(" + lineId + ",Type,_,Limit,_,Toll)."));
        while ((term = engineQuery.nextSolution()) != null) {
            maxLimit = term.getVariablesTable().get("Limit").toString();
            if(maxLimit.equals("null")|| maxLimit.isEmpty()){

                // set the speed limit according to road type, if max speed limit is not provided
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

        // relativeCost is measured in hours. the higher the traffic the higher the cost. the higher the speed_limit
        // the lower the cost
        relativeCost = (relativeCost + (toll ? 0.1 : 0))* traffic  * 130 / limit;
        return relativeCost*actualCost;
    }

    // heuristic value is just the distance
    public void calculateHeuristic(Point target) {
        heuristic = calculateDistance(target);
    }

    // haversine distance
    public double calculateDistance(Point target){
        return Haversine.distance(target.getY(),target.getX(), this.y ,this.x);
    }

    // hashcode used for hashsets. the value is unique node_if (- maxInt to be a valid signed int)
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
