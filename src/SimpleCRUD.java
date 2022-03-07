import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimpleCRUD {
    // vars
    String id;
    String type;
    String title;
    String tags;
    String path;

    // file to save
    private File db;
    private final String tmpSaveFileName = "generated/tmp_CRUD.txt";

    // backup file
    private boolean didCreateBackup = false;
    private File backupFile;

    // might want to enter the path of the txt file (the one stocked in prefs)
    public SimpleCRUD(String txtSaveFileName, boolean createDatabase) {
        File saveFile = new File("generated/" + txtSaveFileName);
        if (!createDatabase) {
            db = saveFile;
            return;
        }

        // if the file exists, we don't want to append the same records, but only the new ones
        if (saveFile.exists()) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss");
            Date d = new Date();
            File otherFile = new File("generated/backup_" + formatter.format(d) + "_" + txtSaveFileName);
            try {
                System.out.println("backup file created: " + otherFile.createNewFile());
                copyFile(saveFile, otherFile);
                didCreateBackup = true;
                backupFile = otherFile;
            } catch (IOException e) {
                e.printStackTrace();
            }

//            backupFile = saveFile;
//            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss");
//            Date d = new Date();
//            File fileDest = new File("generated/backup_" + formatter.format(d) + "_" + txtSaveFileName);
//            /*try {
//                System.out.println("dest file created : " + fileDest.createNewFile());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }*/
//            System.out.println(saveFile.getPath() + " write auth: " + saveFile.canWrite() + " read auth: " + saveFile.canRead());
//            System.out.println("file dest already exists ? " + fileDest.exists() + " write auth: " + fileDest.canWrite() + " read auth: " + fileDest.canRead());
//            didCreateBackup =  saveFile.renameTo(fileDest);
//            System.out.println("creation of backup file: " + txtSaveFileName + " " + didCreateBackup + "   date: " + formatter.format(d) + "    path: " + fileDest.getAbsolutePath() + "    origin: " + saveFile.getAbsolutePath());

        }
        try {
            db = saveFile;
            BufferedWriter bw = new BufferedWriter(new FileWriter(db));
            bw.write("");
            bw.flush();
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

           /* db = new File("generated/" + txtSaveFileName);
        try {
            System.out.println("db file created : " + db.createNewFile());
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void createNewDb(String txtSaveFileName) {

    }

    private void copyFile(File fileToCopy, File fileToPaste) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileToCopy));
        BufferedWriter bw = new BufferedWriter(new FileWriter(fileToPaste));

        String record;

        while ((record = br.readLine()) != null) {
            bw.write(record);
            bw.flush();
            bw.newLine();
        }
        br.close();
        bw.close();
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

    public void addRecord(int id, String type, String title, String tags, String path) throws IOException {
        // prevent the user from entering a comma which would mess up the txt file
        if (tags.contains(",")) {
            System.out.println("\u001B[41m" + "Erreur : les mots clefs ne doivent pas contenir de virgule" + "\u001B[0m");
            return;
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(db, true));
        String tagsToWrite = tags;

        // if a backup file has been set, we copy the existing data to the new file
        // ex: old file (before) -> image1, customTag, path1...
        //                          image2, tag2, path2...
        //     new file (after)  -> image1, customTag, path1...
        //                          image2, tag2, path2...
        //                          image3, tag3, path3...     <- a line was added, but the others remain the same
        if (didCreateBackup) {
            // loop inside backup file
            String[] record = searchRecordByPathName(path, backupFile);
            System.out.println(Arrays.toString(record));
            if (record != null && record.length != 0) {
                tagsToWrite = record[3];
            }

        }

        bw.write(id + "," + type + "," + title + "," + tagsToWrite + "," + path);
        bw.flush();
        bw.newLine();
        bw.close();
    }

    public List<String[]> viewAllRecords(int fileTypeFlag) throws IOException {
        List<String[]> recordList = new ArrayList<>();
        System.out.println("flag : " + fileTypeFlag);

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

            // we filter the results according to their type
            if (fileTypeFlag == 0) {
                recordList.add(recordArray);
            } else if (fileTypeFlag == 1 && stType.equalsIgnoreCase("video")) {
                recordList.add(recordArray);
            } else if (fileTypeFlag == 2 && stType.equalsIgnoreCase("image")) {
                recordList.add(recordArray);
            } else if (fileTypeFlag == -1) {
                System.out.println("Erreur: type de fichier non supporté");
            }
           /* switch (fileTypeFlag) {
                case -1: System.out.println("Erreur lors du visionnage des fichiers: flag inconnu (-1)");
                case 0: recordList.add(recordArray);
                case 1:
                    if (stType.equalsIgnoreCase("video")) {
                    recordList.add(recordArray);
                }
                case 2:
                    if (stType.equalsIgnoreCase("image")) {
                    recordList.add(recordArray);
                }
                //default: System.out.println("Erreur lors du filtrage des fichiers: flag > 2");
            }*/
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

    public String[] searchRecordByPathName(String pathName, File fileToRead) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileToRead));
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

            if (stPath.equals(pathName)) {
                recordArray = new String[]{stId, stType, stTitle, stTags, pathName};

            }
        }
        return  recordArray;
    }

    public void updateRecordById(String id, String newType, String newTitle, String newTags, String newPath) throws IOException {
        // prevent the user from entering a comma which would mess up the txt file
        if (newTags.contains(",")) {
            System.out.println("\u001B[43m" + "ATTENTION : les mots clefs ne doivent pas contenir de virgule" + "\u001B[0m");
            String tmpStr = newTags;
            newTags = tmpStr.replace(",", " ");
        }

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
        // prevent the user from entering a comma which would mess up the txt file
        if (newTags.contains(",")) {
            System.out.println("\u001B[43m" + "ATTENTION : les mots clefs ne doivent pas contenir de virgule" + "\u001B[0m");
            String tmpStr = newTags;
            newTags = tmpStr.replace(",", " ");
        }

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
