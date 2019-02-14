import com.ugos.jiprolog.engine.*;


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

    // set knowledge base filename
    public KnowledgeBaseCreator(String file){
        filename = file;
    }

    // add basic rules and axioms to base
    public void addRulesToBase() {

        // create new file
        try {
            file = new FileWriter(filename, false);
            printWriter = new PrintWriter(file);
        } catch (IOException we) {
            System.out.println("cant create : ");
        }

        // valid values of highways
        String[] highways = {"motorway", "trunk", "primary", "secondary", "tertiary", "unclassified", "residential",
                "motorway_link", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "living_street"};
        for(String highwayType : highways) {
            printWriter.println("validHighway(" + highwayType + ").");
        }
        printWriter.println();

        // valid values of access
        String[] access = {"yes", "permissive", "destination", "null", "allowed"};
        for(String accessType : access) {
            printWriter.println("validAccess(" + accessType + ").");
        }

        // rule for drivable pairs of Highways - Access
        printWriter.println("drivable(Highway, Access) :- validAccess(Access), validHighway(Highway).");
        printWriter.println();

        // rule for taxi - client pairing. checks if ClientLang is a member of TaxiLangs, if taxi is available and if capacity >= persons
        printWriter.println("validPairing(X) :- taxi(_, _, X, yes, MaxN, TaxiLangs, _), client(_, _, _, _, _, Person, ClientLang), Person =< MaxN, member(ClientLang, TaxiLangs).");

        // rule to convert direction to a readable value
        printWriter.println("direction(Oneway, Res) :- Oneway = yes -> Res = 1 ; (Oneway = -1 -> Res = -1 ; Res = 0).");

        // usable rule traffic(LId,Time,Traffic Intensity)
        printWriter.println("(traffic(Line,Time,Traf) :- traffics(Line, Low, High, Traf), Time > Low, Time =< High).");
        printWriter.close();

        // at the end the engine should consult our base
        try {
            engine.consultFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // add taxi data
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

    // add client data
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

    // add traffic data
    public void addTrafficToBase(String trafficFilename){
        LinkedList<String[]> fields;
        csvReader = new CSVReader(trafficFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;

        for (String[] trafficFields : fields) {
            // traffics in prolog in form (LineId, LowTime, HighTime, Intensity)
            String trafficString;

            // if >= 3 fields, then traffic time zones are provided
            if (trafficFields.length >= 3 && !trafficFields[2].isEmpty()) {
                String[] zones = trafficFields[2].split("\\|");
                for (String z : zones) {
                    String low = z.substring(0, 2);
                    String high = z.substring(6, 8);
                    String intensity = z.substring(12);

                    // traffics stores traffic data in form (lineId, LowTime, HighTime, TrafficIntensity)
                    trafficString = "traffics(" + trafficFields[0] + "," + low + "," + high + "," + intensity + ")";
                    try {
                        engineQuery = engine.openSynchronousQuery(parser.parseTerm("assert(" + trafficString + ")."));
                        if (engineQuery.nextSolution() == null)
                            throw new JIPEvaluationException("addTraffic: assertion failed");
                    } catch (JIPSyntaxErrorException e) {
                        System.out.println(e.getTerm().toString());
                    }

                }
            }
        }

    }

    // add line data
    public void addLinesToBase(String linesFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(linesFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;

        for (String[] lineFields : fields) {

            // line in prolog in form (id, highway, oneway, maxspeed, access, toll). in absence of data add null
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

    // add node data
    public void addNodes(String nodesFilename){
        LinkedList<String[]> fields;
        csvReader = new CSVReader(nodesFilename);
        fields = csvReader.readCSV();

        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPQuery nodeQuery;

        JIPTerm term;

        // at the same time with node insertion we insert canMoveFromTo clauses to create neighbouring relations
        long lastLineId = -1;
        long lastNodeId = -1;
        for (String[] nodesFields : fields) {

            engineQuery = engine.openSynchronousQuery(parser.parseTerm("line(" + nodesFields[2] + ", _, Oneway, _, _, _)."));
            if ((term = engineQuery.nextSolution()) != null) {

                // if nodes are part of the same line
                if ((long) Double.parseDouble(nodesFields[2]) == lastLineId) {
                    engineQuery = engine.openSynchronousQuery(parser.parseTerm("direction(" + term.getVariablesTable().get("Oneway").toString() + ", Res)."));
                    switch ((term = engineQuery.nextSolution()).getVariablesTable().get("Res").toString()) {

                        // line oneway in the same order as it is given
                        case "1":
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    Long.toString(lastNodeId) + ',' + nodesFields[3] + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            break;

                        // line oneway in the opposite order as it is given
                        case "-1":
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    nodesFields[3] + ',' + Long.toString(lastNodeId) + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            break;

                        // in every other occasion, assume two way road
                        default:
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    nodesFields[3] + ',' + Long.toString(lastNodeId) + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(canMoveFromTo(" +
                                    Long.toString(lastNodeId) + ',' + nodesFields[3] + ',' + Long.toString(lastLineId) + "))."));
                            if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
                            break;
                    }
                }

                // update node id and line id for next iteration
                lastLineId = (long) Double.parseDouble(nodesFields[2]);
                lastNodeId = (long) Double.parseDouble(nodesFields[3]);

                // nodes in prolog in form (node_id, line_id X, Y)
                String nodeString = "node(" + nodesFields[3] + ',' + nodesFields[2] + ',' + nodesFields[0] + ',' + nodesFields[1] + ')';
                nodeQuery = engine.openSynchronousQuery(parser.parseTerm("assert(" + nodeString + ")."));
                if (nodeQuery.nextSolution() == null) throw new JIPEvaluationException("addLine: assertion failed");
            }
        }
    }
}

