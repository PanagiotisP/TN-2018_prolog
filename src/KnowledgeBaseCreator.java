import com.ugos.jiprolog.engine.JIPEngine;
import com.ugos.jiprolog.engine.JIPQuery;
import com.ugos.jiprolog.engine.JIPTerm;
import com.ugos.jiprolog.engine.JIPTermParser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

public class KnowledgeBaseCreator {
    private String filename;
    private CSVReader csvReader;
    private PrintWriter printWriter = null;
    private FileWriter file = null;

    public KnowledgeBaseCreator(String file){
        filename = file;
    }

    public void addTaxisToBase(String taxisFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(taxisFilename);
        fields = csvReader.readCSV();

        try {
            file = new FileWriter(filename, false);
            printWriter = new PrintWriter(file);
        } catch (IOException we) {
            System.out.println("cant create : ");
        }

        for (String[] taxiFields : fields) {
            // taxis in prolog in form (X, Y, id, availabe, capacity, languages, rating)
            printWriter.print("taxi(");
            for (int i = 0; i < 4; i++) {
                printWriter.printf("%s,", taxiFields[i]);
            }
            if(taxiFields[4].length() > 1) {
                // get last character (max capacity)
                taxiFields[4] = taxiFields[4].substring(taxiFields[4].length() - 1);
            }
            printWriter.printf("%s,", taxiFields[4]);
            printWriter.printf("[%s],", taxiFields[5].replace('|', ','));
            printWriter.printf("%s).\n", taxiFields[6]);
        }
        printWriter.println();
        printWriter.close();
    }

    public void addClientToBase(String clientFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(clientFilename);
        fields = csvReader.readCSV();

        try {
            file = new FileWriter(filename, true);
            printWriter = new PrintWriter(file);
        } catch (IOException we) {
            System.out.println("cant create : ");
        }

        for (String[] clientFields : fields) {
            // client in prolog in form (X, Y, Xdest, Ydest, time, persons, language)
            printWriter.print("client(");
            for (int i = 0; i < 6; i++) {
                printWriter.printf("%s,", clientFields[i]);
            }
            printWriter.printf("%s).\n", clientFields[6]);
        }
        printWriter.close();
    }

    public void addLinesToBase(String linesFilename) {
        LinkedList<String[]> fields;
        csvReader = new CSVReader(linesFilename);
        fields = csvReader.readCSV();

        try {
            file = new FileWriter(filename, true);
            printWriter = new PrintWriter(file);
        } catch (IOException we) {
            System.out.println("cant create : ");
        }

        for (String[] lineFields : fields) {
            // line in prolog in form (id, highway, oneway, maxspeed, access, toll)
            printWriter.print("line(");
            printWriter.printf("%s,", lineFields[0]);
            printWriter.printf("%s,", lineFields[1].length() == 0 ? "null" : lineFields[1]);
            printWriter.printf("%s,", lineFields[3].length() == 0 ? "null" : lineFields[3]);
            printWriter.printf("%s,", lineFields[6].length() == 0 ? "null" : lineFields[6]);
            printWriter.printf("%s,", lineFields[9].length() == 0 ? "null" : lineFields[9]);
            printWriter.printf("%s).\n", lineFields[17].length() == 0 ? "null" : lineFields[17]);
        }
        printWriter.close();

    }

    public void addRulesToBase() {
        try {
            file = new FileWriter(filename, true);
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
        printWriter.println("drivable(X) :- line(X, Highway, _, _, Access, _), validAccessibility(Access), validHighway(Highway).");
        printWriter.println();
        printWriter.println("validPairing(X) :- taxi(_, _, X, yes, MaxN, TaxiLangs, _), client(_, _, _, _, _, Person, ClientLang), Person =< MaxN, member(ClientLang, TaxiLangs).");
        printWriter.println();
        printWriter.println("getTaxiRating(Id, Rating) :- taxi(_, _, Id, _, _, _, Rating).");
        printWriter.println("getTaxiCoord(Id, X, Y) :- taxi(X, Y, Id, _, _, _, _).");
        printWriter.println();
        printWriter.println("getClientPosCoord(X, Y) :- client(X, Y, _, _, _, _, _).");
        printWriter.println("getClientDesCoord(X, Y) :- client(_, _, X, Y, _, _, _).");
        printWriter.println("getClientTime(T) :- client(_, _, _, _, T, _, _).");
        printWriter.close();
    }

    public void addNodes(String nodesFilename){
        LinkedList<String[]> fields;
        csvReader = new CSVReader(nodesFilename);
        fields = csvReader.readCSV();

        int roadId=0;
        try {
            file = new FileWriter(filename, true);
            printWriter = new PrintWriter(file);
        } catch (IOException we) {
            System.out.println("cant create : ");
        }

        for (String[] nodesFields : fields) {
            // nodes in prolog in form (X, Y, line_id, node_id)
            printWriter.print("node(");
            for (int i = 0; i < 4; i++) {
                printWriter.printf("%s,", nodesFields[i]);
            }
            printWriter.printf("%d).\n", roadId++);
        }
        printWriter.println();
        printWriter.close();
    }


}

