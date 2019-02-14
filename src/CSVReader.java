import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.lang.String;

// this class reads a CSV file and returns its data in form of tokenized strings
public class CSVReader {

    private String csvFile;
    private BufferedReader br = null;
    private String line = "";
    private String cvsSplitBy = ",";
    private LinkedList<String[]> fields = new LinkedList<>();

    public CSVReader(String filename) {
        csvFile = "Data/" + filename;
    }

    public LinkedList<String[]> readCSV() {
        try {
            br = new BufferedReader(new FileReader(csvFile));
            br.readLine();
            while ((line = br.readLine()) != null) {
                // use comma as separator
                fields.add(line.split(cvsSplitBy, -1));
            }

        } catch (
                FileNotFoundException e) {
            e.printStackTrace();
        } catch (
                IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fields;
    }
}