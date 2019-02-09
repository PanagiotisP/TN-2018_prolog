import java.util.HashSet;
import java.util.LinkedList;

public class Point {

    private double x;
    private double y;
    private double heuristic;
    private double pathCost;
    public LinkedList<Point> neighboring;
    public HashSet<Point> previous;
    public LinkedList<Integer> startingIds;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.pathCost = -1;
        neighboring = new LinkedList<>();
        previous = new HashSet<>();
        startingIds = new LinkedList<>();
    }

    public void newNeighbor(Point neo) {
        neighboring.add(neo);
    }

    public double calculateDistance(Point target) {
        return Math.sqrt(Math.pow((target.getX() - this.x), 2) + Math.pow((target.getY() - this.y), 2));
    }

    public void calculateHeuristic(Point target) {
        heuristic = calculateDistance(target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (x != point.x) return false;
        if (y != point.y) return false;

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

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }




}
