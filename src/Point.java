import com.ugos.jiprolog.engine.*;

import java.util.HashSet;
import java.util.LinkedList;

public class Point {

    private double x;
    private double y;
    private long node_id;
    private double heuristic;
    private double pathCost;
    public HashSet<Point> previous;
    public LinkedList<Integer> startingIds;
    public LinkedList<Integer> taxiIds;
    public static JIPEngine engine = null;

    public Point(long nid) {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("node(X,Y, _," + Long.toString(nid) + ",_)."));
        term = engineQuery.nextSolution();
        if (term != null) {
            this.x = Double.parseDouble(term.getVariablesTable().get("X").toString());
            this.y = Double.parseDouble(term.getVariablesTable().get("Y").toString());
        }
        this.pathCost = -1;
        this.node_id = nid;
        previous = new HashSet<>();
        startingIds = new LinkedList<>();
        taxiIds = new LinkedList<>();
    }

    public Point(long tId,double x, double y){
        this.x = x;
        this.y = y;
        this.node_id = tId;
    }

    public LinkedList<Long> getNeighbours() {
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;
        LinkedList<Long> neighbouringIds = new LinkedList<>();
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("canMoveFromTo(" + node_id + ", Neighbour, _)."));
        while ((term = engineQuery.nextSolution()) != null) {
            neighbouringIds.add((long) Double.parseDouble(term.getVariablesTable().get("Neighbour").toString()));
        }
        return neighbouringIds;
    }

    public double calculateDistance(Point target) {
        return Math.sqrt(Math.pow((target.getX() - this.x), 2) + Math.pow((target.getY() - this.y), 2));
    }

    public void calculateHeuristic(Point target) {
        heuristic = calculateDistance(target);
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

}
