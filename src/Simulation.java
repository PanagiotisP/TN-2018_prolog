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
        creatorKB.addTaxisToBase("taxis.csv");
        creatorKB.addClientToBase("client.csv");
        creatorKB.addLinesToBase("lines.csv");
        creatorKB.addRulesToBase();

        JIPEngine engine = new JIPEngine();
        engine.consultFile(kb);
        JIPTermParser parser = engine.getTermParser();
        JIPQuery engineQuery;
        JIPTerm term;

        String x = "X";
        System.out.println("CASE 7");
        engineQuery = engine.openSynchronousQuery(parser.parseTerm("drivable(Z)."));
        term = engineQuery.nextSolution();
        while (term != null) {
            System.out.println(x + " prefers " + term.getVariablesTable().get("Z").toString());
            term = engineQuery.nextSolution();
        }
    }
}
