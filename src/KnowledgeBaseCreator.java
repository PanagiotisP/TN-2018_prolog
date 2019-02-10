import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;
import com.ugos.jiprolog.engine.JIPEvaluationException;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

public class KnowledgeBaseCreator {
    private String filename;
    private CSVReader csvReader;
    private PrintWriter printWriter = null;
    private FileWriter file = null;
    public static  JIPEngine engine = new JIPEngine();

    public KnowledgeBaseCreator(String file){
        filename = file;
    }

    public void addTaxisToBase(String taxisFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(taxisFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;

        for (String[] taxiFields : fields) {
            // taxis in prolog in form (X, Y, id, availabe, capacity, languages, rating)

            String taxiString = "taxi(";
            for (int i = 0; i < 4; i++) {
                taxiString += taxiFields[i] + ',';
            }
            if(taxiFields[4].length() > 1) {
                // get last character (max capacity)
                taxiFields[4] = taxiFields[4].substring(taxiFields[4].length() - 1);
            }
            taxiString += taxiFields[4] + ',';
            taxiString += "[" + taxiFields[5].replace('|', ',') + "],";
            taxiString += taxiFields[6] + ")";
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("assert(" + taxiString + ")."));
            if(engineQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
        }
    }

    public void addClientToBase(String clientFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(clientFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;

        for (String[] clientFields : fields) {
            // client in prolog in form (X, Y, Xdest, Ydest, time, persons, language)
            String clientString = "client(";
            for (int i = 0; i < 6; i++) {
                clientString += clientFields[i] + ',';
            }
            clientString += clientFields[6] +")";
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("assert(" + clientString + ")."));
            if(engineQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
        }
    }

    public void addLinesToBase(String linesFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(linesFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;

        for (String[] lineFields : fields) {
            // line in prolog in form (id, highway, oneway, maxspeed, access, toll)
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("drivable(" + (lineFields[1].length() == 0 ? "null" : lineFields[1]) + ',' + (lineFields[9].length() == 0 ? "null" : lineFields[9]) + ")."));
            if (engineQuery.nextSolution() != null) {
                String lineString = "line(";
                lineString += lineFields[0] + ',';
                lineString += (lineFields[1].length() == 0 ? "null" : lineFields[1]) + ',';
                lineString += (lineFields[3].length() == 0 ? "null" : lineFields[3]) + ',';
                lineString += (lineFields[6].length() == 0 ? "null" : lineFields[6]) + ',';
                lineString += (lineFields[9].length() == 0 ? "null" : lineFields[9]) + ',';
                lineString += (lineFields[17].length() == 0 ? "null" : lineFields[17]) + ')';
                engineQuery = engine.openSynchronousQuery(parser.parseTerm("assert(" + lineString + ")."));
                if (engineQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
            }
        }
    }

    public void addRulesToBase() {
        try {
            file = new FileWriter(filename, false);
            printWriter = new PrintWriter(file);
        } catch (IOException we) {
            System.out.println("cant create : ");
        }
        // valid fields of highway
        String[] highways = {"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "living_street"};
        for(String highwayType : highways) {
            printWriter.println("validHighway(" + highwayType + ").");
        }
        printWriter.println();

        String[] accessibility = {"yes", "permissive", "destination", "null", "allowed"};
        for(String accessibilityType : accessibility) {
            printWriter.println("validAccessibility(" + accessibilityType + ").");
        }
        printWriter.println("drivable(Highway, Access) :- validAccessibility(Access), validHighway(Highway).");
        printWriter.println();
        printWriter.println("validPairing(X) :- taxi(_, _, X, yes, MaxN, TaxiLangs, _), client(_, _, _, _, _, Person, ClientLang), Person =< MaxN, member(ClientLang, TaxiLangs).");
        printWriter.println("direction(Oneway, Res) :- Oneway = yes -> Res = 1 ; (Oneway = -1 -> Res = -1 ; Res = 0).");
//        printWriter.println("canMoveFromTo(X,Y, ConnectingLine) :- (node(_,_,ConnectingLine, X,IndexX)," +
//                "line(ConnectingLine, _, Oneway, _, _, _), " +
//                "drivable(ConnectingLine), " +
//                "direction(Oneway, Res), " +
//                "(Res = 1 -> IndexY is IndexX + 1 " +
//                ";(Res = -1 -> IndexY is IndexX - 1" +
//                "; (IndexY is IndexX - 1 ; IndexY is IndexX + 1))), node(_,_,ConnectingLine, Y, IndexY))." );
        printWriter.close();
        try {
            engine.consultFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addNodes(String nodesFilename){
        LinkedList<String[]> fields;
        csvReader = new CSVReader(nodesFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPQuery nodeQuery;

        JIPTerm term;
        long lastLineId = -1;
        long lastNodeId = -1;
        int roadId = 0;
        for (String[] nodesFields : fields) {
            // nodes in prolog in form (X, Y, line_id, node_id)
            engineQuery = engine.openSynchronousQuery(parser.parseTerm("line(" + nodesFields[2] + ", _, Oneway, _, _, _)."));
            if ((term = engineQuery.nextSolution()) != null) {
                if ((long) Double.parseDouble(nodesFields[2]) == lastLineId) {
                    if ((long) Double.parseDouble(nodesFields[2]) == 31486421) {
                        int kaka = 0;
                    }
                    switch (term.getVariablesTable().get("Oneway").toString()) {
                        case "yes":
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    Long.toString(lastNodeId) + ',' + nodesFields[3] + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            break;
                        case "-1":
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    nodesFields[3] + ',' + Long.toString(lastNodeId) + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            break;
                        default:
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    nodesFields[3] + ',' + Long.toString(lastNodeId) + ',' + Long.toString(lastLineId) + "))."));
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    Long.toString(lastNodeId) + ',' + nodesFields[3] + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            break;
                    }
                }

                lastLineId = (long) Double.parseDouble(nodesFields[2]);
                lastNodeId = (long) Double.parseDouble(nodesFields[3]);

                String nodeString = "node(";
                for (int i = 0; i < 4; i++) {
                    nodeString += nodesFields[i] + ',';
                }
                nodeString += roadId++ + ")";
                nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(" + nodeString + ")."));
                if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
            }
        }
    }
}

