import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SimpleCRUD {
    // vars
    String id;
    String type;
    String title;
    String tags;
    String path;

    // file to save
    private File db;
    private final String tmpSaveFileName = "generated/tmp_CRUD test.txt";

    // might want to enter the path of the txt file (the one stocked in prefs)
    public SimpleCRUD(String txtSaveFileName) {
        db = new File(txtSaveFileName);
    }

    public SimpleCRUD(String id, String type, String title, String tags, String path) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.tags = tags;
        this.path = path;
    }

    public SimpleCRUD(){
        db = new File("generated/CRUD test.txt");
    }

    public void addRecord(int id, String type, String title, String tags, String path, File testXmlFile) throws IOException {
        // do something like "if id already exists then print error"
        if (tags.contains(",")) {
            System.out.println("\u001B[41m" + "Erreur : les mots clefs ne doivent pas contenir de virgule" + "\u001B[0m");
            return;
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(testXmlFile, true));
        bw.write(id + "," + type + "," + title + "," + tags + "," + path);
        bw.flush();
        bw.newLine();
        bw.close();
    }

    public List<String[]> viewAllRecords() throws IOException {
        List<String[]> recordList = new ArrayList<>();
        if (!db.exists()) {
            return recordList;
        }
        BufferedReader br = new BufferedReader(new FileReader(db));
        String record;
        String[] recordArray;

        // we start reading
        while ((record = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(record, ",");

            String stId = "";
            String stType = "";
            String stTitle = "";
            String stTags = "";
            String stPath = "";

            // if the tags field is null or equals "", we do not modify it
            // because the string tokenizer doesn't count it as a value
            // so there would be 4 token instead of 5
            if (st.countTokens() == 4) {
                stId = st.nextToken();
                stType = st.nextToken();
                stTitle = st.nextToken();
                stPath = st.nextToken();
            } else {
                stId = st.nextToken();
                stType = st.nextToken();
                stTitle = st.nextToken();
                stTags = st.nextToken();
                stPath = st.nextToken();
            }

            recordArray = new String[]{stId, stType, stTitle, stTags, stPath};
            recordList.add(recordArray);
        }
        br.close();
        return recordList;
    }

    public void deleteRecordById(String id) throws IOException {
        String record;
        File tmpDB = new File(tmpSaveFileName);
        BufferedReader br = new BufferedReader( new FileReader(db));
        BufferedWriter bw = new BufferedWriter( new FileWriter(tmpDB));

        // we rewrite the content of the db except for the chosen id
        while ((record = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(record, ",");
            String stId = st.nextToken();

            if (stId.equals(id)) {
                continue;
            }
            bw.write(record);
            bw.flush();
            bw.newLine();
        }

        br.close();
        bw.close();

        db.delete();
        tmpDB.renameTo(db);
    }

    public String[] searchRecordById(String id) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(db));
        String record;
        String[] recordArray = new String[0];

        // search occurrence of id in file
        while ((record = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(record, ",");

            String stId = st.nextToken();
            String stType = st.nextToken();
            String stTitle = st.nextToken();
            String stTags = st.nextToken();
            String stPath = st.nextToken();

            if (stId.equals(id)) {
                recordArray = new String[]{stId, stType, stTitle, stTags, stPath};

            }
        }
        return  recordArray;
    }

    public void updateRecordById(String id, String newType, String newTitle, String newTags, String newPath) throws IOException {
        String record;
        File tmpDB = new File(tmpSaveFileName);
        BufferedReader br = new BufferedReader(new FileReader(db));
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpDB));

        // we search the record with the corresponding id, then update it
        // equivalent of delete and add
        while ((record = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(record, ",");
            String stId = st.nextToken();

            if (stId.equals(id)) {
                bw.write(id + "," + newType + "," + newTitle + "," + newTags + "," + newPath);
            } else {
                bw.write(record);
            }
            bw.flush();
            bw.newLine();
        }
        br.close();
        bw.close();

        db.delete();
        tmpDB.renameTo(db);
    }

    // used to update just the tags
    public void updateTagsOfRecordById(String id, String newTags) throws IOException {
        String record;
        File tmpDB = new File(tmpSaveFileName);
        BufferedReader br = new BufferedReader(new FileReader(db));
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpDB));

        // we search the record with the corresponding id, then we update ONLY THE TAGS
        // equivalent of delete and add
        while ((record = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(record, ",");

            String stId = "";
            String stType = "";
            String stTitle = "";
            String stTags = "";
            String stPath = "";

            // if the tags field is null or equals "", we do not modify it
            // because the string tokenizer doesn't count it as a value
            // so there would be 4 token instead of 5
            if (st.countTokens() == 4) {
                stId = st.nextToken();
                stType = st.nextToken();
                stTitle = st.nextToken();
                stPath = st.nextToken();
            } else {
                stId = st.nextToken();
                stType = st.nextToken();
                stTitle = st.nextToken();
                stTags = st.nextToken();
                stPath = st.nextToken();
            }


            if (stId.equals(id)) {
                bw.write(id + "," + stType + "," + stTitle + "," + newTags + "," + stPath);
            } else {
                bw.write(record);
            }
            bw.flush();
            bw.newLine();
        }
        br.close();
        bw.close();

        db.delete();
        tmpDB.renameTo(db);
    }
}
