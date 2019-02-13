import java.util.*;

public class AStar {
    private HashSet<Point> closedSet;
    private HashMap<Long, Point> graph;
    private PriorityQueue<Point> openSet;

    public AStar() {
        graph = new HashMap<Long, Point>(50000);
        openSet = new PriorityQueue<>(1, new Comparator<>() {
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
        closedSet = new HashSet<>();
    }

    public Point solve(Point start, Point target, String time) {
        closedSet.clear();
        graph.clear();
        openSet.clear();
        openSet.add(start);
        graph.put(start.getNode_id(), start);
        boolean found = false;
        while (!openSet.isEmpty()) {
            System.out.println(openSet.size());
            Point top = openSet.peek();
            openSet.remove(top);
            if (!closedSet.contains(top)) {
                closedSet.add(top);
                if (found && top.getPathCost() + top.getHeuristic() > target.getPathCost()) {
                    break;
                }

                for (LinkedList<Long> neighbourInfo : top.getNeighbours()) {
                    long neighbourId = neighbourInfo.getFirst();
                    long lineId = neighbourInfo.getLast();
                    Point neighbour = graph.get(neighbourId);
                    if (neighbour == null || top.getPathCost() + top.calculateCost(neighbour, lineId, time) == neighbour.getPathCost()) {
                        neighbour = new Point(neighbourId);
                        neighbour.setTaxiId(top.getTaxiId());
                        neighbour.setPathCost(top.getPathCost() + top.calculateCost(neighbour, lineId, time));
                        neighbour.setPathDist(top.getPathDist() + top.calculateDistance(neighbour));
                        neighbour.previous.add(top);
                        neighbour.calculateHeuristic(target);
                        openSet.add(neighbour);
                        graph.put(neighbourId, neighbour);
                    } else if (top.getPathCost() + top.calculateCost(neighbour, lineId, time) < neighbour.getPathCost()) {
                        neighbour.setPathCost(top.getPathCost() + top.calculateCost(neighbour, lineId, time));
                        neighbour.setPathDist(top.getPathDist() + top.calculateDistance(neighbour));
                        neighbour.previous.clear();
                        neighbour.previous.add(top);
                    }
                    if (neighbour.equals(target)) {
                        target.previous = neighbour.previous;
                        found = true;
                        target.setPathCost(neighbour.getPathCost());
                        target.setPathDist(neighbour.getPathDist());
                        target.setTaxiId(neighbour.getTaxiId());
                    }
                }
            }
        }
        if(found == true) {
            return target;
        }
         return null;
    }
}
