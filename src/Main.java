import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.awt.*;
import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class Main extends Application {

    Stage primaryStage;
    private Stage editStage;
    private Scene editScene;

    // Width and height of the whole application
    private double window_height = 900;
    private double window_width = 1600;

    // path to the file where all data is stored
    private static final String TEST_PATH_TO_TXT_PREFIX = "generated/CRUD_";
    File testXmlFile;

    // list of all result lines
    ArrayList<List<Pair>> listOfResults;
    VBox allResultsBox;
    private TextField researchTextField;
    Button resultButton;

    // other results list
    private ArrayList<String[]> matchingResultsList;

    // main window
    Group group;
    private Group editGroup;
    List<Node> nodeList;
    private HBox resultBox;
    DirectoryChooser directoryChooser;
    FileChooser fileChooser;
    Menu optionsMenu;
    private ScrollPane scrollPane;
    private MenuBar menuBar;
    private HBox searchBox;
    private Text t;
    private VBox researchesVBox;
    private List<Button> editButtonList;
    private RadioButton filterAll;
    private RadioButton filterVideos;
    private RadioButton filterImages;

    // edit window
    private List<Node> editNodeList;
    private VBox editVbox;
    private HBox editHbox;
    private ImageView imageView;
    private TextArea tagsTextArea;
    private Button editPreviousButton;
    private Button editNextButton;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Text contentTitle;
    private Button openWithSystemButton;

    // help window
    private List<Node> helpNodeList;
    private VBox helpVbox;

    // the preferences of the user are stored in the Vars file
    Preferences prefs;
    final String PATH_TO_DATA_DIR_PREF_KEY = "path_to_data_dir";
    final String HAS_PATH_TO_DATA_DIR_BEEN_SET_PREF_KEY = "hasPathToDataBeenSet";
    final String PATH_TO_XML_FILE = "path_to_xml_file";
    final String HAS_PATH_TO_XML_FILE_BEEN_SET = "hasPathToXmlFileBeenSet";

    // file writing and reading
    FileWriter fileWriter;
    BufferedWriter bw;

    // this is used to auto generate an id value for each data element
    int _idIncrNumber = 0;

    // the custom list of supported extensions
    // if a file doesn't have its extension in the lists, it will not be added in the xml file
    // case doesn't matter
    final String[] SUPPORTED_VIDEO_EXTENSIONS = {"mp4", "mpg", "mov", "wmv"};
    final String[] SUPPORTED_IMAGES_EXTENSIONS = {"jpg", "png", "jpeg", "gif"};

    // the class who manages all io operations on the txt files
    private SimpleCRUD crud;

    @Override
    public void start(Stage primaryStage) {

        nodeList = new ArrayList<>();

        group = new Group();
        group.getChildren().addAll(populateGroup());
        //group.setAutoSizeChildren(true);

        listOfResults = new ArrayList<>();
        matchingResultsList = new ArrayList<>();

        prefs = Preferences.userRoot().node(this.getClass().getName());

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        window_width = screenSize.width * 0.75;
        window_height = screenSize.height * 0.75;

        Scene scene = new Scene(group, window_width, window_height);

        // if the window is resized, we also resize all nodes affected by this resize
        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                System.out.println("width changed: " + newValue.intValue());

                scrollPane.setPrefWidth(newValue.doubleValue() - 10);
                allResultsBox.setPrefWidth(scrollPane.getPrefWidth() - 17);
                menuBar.setPrefWidth(newValue.doubleValue());
                //searchBox.setPrefWidth(newValue.doubleValue() - 10);
                searchBox.setPrefWidth(newValue.doubleValue());
                //searchBox.setMaxWidth(newValue.doubleValue() - 10);
                //searchBox.setMinWidth(newValue.doubleValue() - 10);
                researchTextField.setPrefWidth(newValue.doubleValue() - 100);
                //researchTextField.setLayoutX(newValue.doubleValue() - 100);
                //t.setLayoutX(newValue.doubleValue() * 0.3);
                researchesVBox.setPrefWidth(newValue.doubleValue());
                //todo: pour l'edit button, seul le dernier est modifié
                // il faut faire une liste de tous les buttons créés et les resizes ensembles (loop)
                // aussi penser a créer/reset la liste de buttons a chaque recherche
                //resultBox.setPrefWidth(scrollPane.getPrefWidth() - 15); // 1583
                if (editButtonList != null) {
                    for (Button b : editButtonList) {
                        b.setPrefWidth(scrollPane.getPrefWidth() - 15);

                    }
                }
                //resultBox.setPrefHeight(window_height * 0.1); // 100
            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                System.out.println("height changed: " + newValue.intValue());

                scrollPane.setPrefHeight(newValue.doubleValue() - 185);
                //resultBox.setPrefHeight(newValue.doubleValue() * 0.1);
                if (editButtonList != null) {
                    for (Button b : editButtonList) {
                        b.setPrefHeight(scrollPane.getPrefHeight() * 0.1);
                    }
                }
            }
        });

        primaryStage.setTitle("Recherche de contenu");
        primaryStage.setMinWidth(window_width * 0.4);
        primaryStage.setMinHeight(window_height * 0.5);
        primaryStage.setScene(scene);
        scene.getRoot().requestFocus();
        this.primaryStage = primaryStage;
        //primaryStage.setResizable(false);

        primaryStage.show();
    }

    List<Node> populateGroup() {
        // list of all elements that will be added

        // container for all results
        allResultsBox = new VBox();
        allResultsBox.setPrefWidth(window_width * 0.9 - 27);

        // scrollbar for the results list
        scrollPane = new ScrollPane(allResultsBox);
        scrollPane.setPrefWidth(window_width * 0.9 - 10);
        scrollPane.setPrefHeight(window_height * 0.8 - 120);
        //scrollPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        scrollPane.setLayoutX(5);
        scrollPane.setLayoutY(window_height * 0.20);

        // main vertical container
        researchesVBox = new VBox();
        researchesVBox.setAlignment(Pos.TOP_CENTER);
        researchesVBox.setPrefWidth(window_width * 0.9 - 10);
        researchesVBox.setPrefHeight(window_height * 0.2); // 200
        //researchesVBox.setFillWidth(true);
        researchesVBox.setLayoutY(50); //50

        // horizontal search container
        searchBox = new HBox();
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setPrefWidth(window_width * 0.9 - 10);
        searchBox.setPrefHeight(window_height * 0.1); // 100
        //searchBox.setMinWidth(window_width * 0.9); // 1500

        // title text
        t = new Text("Recherche de photos ou vidéos");
        t.setTextAlignment(TextAlignment.CENTER);
        t.setStyle("-fx-font-size: 32");
        t.setLayoutX(window_width * 0.3);

        // text input
        researchTextField = new TextField();
        researchTextField.setPromptText("Rechercher");
        researchTextField.setPrefWidth(window_width * 0.5);
        researchTextField.setMaxWidth(window_width * 0.5);
        researchTextField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                startSearching();
            }
        });

        // text input dialog
        TextInputDialog tid = new TextInputDialog();
        tid.getEditor().setPromptText("ex : D:\\");
        tid.setContentText("Chemin : ");
        tid.setHeaderText("Entrez le chemin vers le dossier parent des données");

        // directory chooser
        directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisissez un dossier");

        // file chooser for the xml file
        fileChooser = new FileChooser();
        fileChooser.setTitle("Choisissez un fichier .txt");

        // option menu
        MenuItem generateXmlFile = new MenuItem("Actualiser la base de données (un fichier de sauvegarde sera créé)");
        generateXmlFile.setOnAction(event -> {
            // we reset the id number
            _idIncrNumber = 0;
            generateXmlFile();
        });

        // when we change the source folder, we associate a txt file to it
        MenuItem changeMainDirectory = new MenuItem("Changer de dossier principal (sélectionner le dossier source du contenu)");
        changeMainDirectory.setOnAction(event -> {

            //directoryChooser.setInitialDirectory(new File(prefs.get(PATH_TO_DATA_DIR_PREF_KEY, System.getProperty("user.home") + "\\pictures")));

            // searches if the users has specified the folder to the data dir, and asks him if not
            // open a browser to choose a folder from
            File selectedFile = directoryChooser.showDialog(primaryStage);

            if (selectedFile != null && !selectedFile.getPath().equalsIgnoreCase("")) {
                prefs.put(PATH_TO_DATA_DIR_PREF_KEY, selectedFile.getAbsolutePath());
                prefs.putBoolean(HAS_PATH_TO_DATA_DIR_BEEN_SET_PREF_KEY, true);
                System.out.println("new dir: " + prefs.get(PATH_TO_DATA_DIR_PREF_KEY, System.getProperty("user.home") + "\\pictures"));

                // recreate the main box of all displayed results
                allResultsBox.getChildren().clear();

                // - if there is no txt file associated to the src folder, we create one in generated
                //   we give it the name of the src folder
                // - if there is a txt file with the name of the src, we store the tags previously modified, then create a new txt file
                //   we add to it the tags associated with their id, and delete the original file
                _idIncrNumber = 0;
                generateXmlFile();
            }
        });

        MenuItem changeCurrentXmlFile = new MenuItem("Changer de fichier xml (sélectionner une autre base de données)");
        changeCurrentXmlFile.setOnAction(event -> {
            File saveFile = new File("generated");

            // creates a new "generated" folder if it doesn't exist
            if (saveFile.exists()) {
                fileChooser.setInitialDirectory(new File("generated"));
            } else {
                if (saveFile.mkdir()) {
                    System.out.println("dossier 'generated' créé");
                } else
                    System.out.println("le dossier 'generated' n'a pas pu être créé");
            }

            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null && !selectedFile.getPath().equalsIgnoreCase("")) {
                prefs.put(PATH_TO_XML_FILE, selectedFile.getAbsolutePath());
                prefs.putBoolean(HAS_PATH_TO_XML_FILE_BEEN_SET, true);

                // recreate the main box of all displayed results
                allResultsBox.getChildren().clear();

                System.out.println("new xml file source : " + prefs.get(PATH_TO_XML_FILE, "aucun"));
            }
        });

        // refresh txt if an item was added
        // we create a copy of the existing txt file then we append the new data
        MenuItem helpItem = new MenuItem("Aide");
        helpItem.setOnAction(event -> {
            openHelpWindow();
        });

        optionsMenu = new Menu("Options");
        optionsMenu.getItems().addAll(changeMainDirectory, generateXmlFile, changeCurrentXmlFile, helpItem);

        // menu bar
        menuBar = new MenuBar();
        menuBar.setPrefWidth(window_width);

        menuBar.getMenus().add(optionsMenu);

        // search button
        Button okBtn = new Button("OK");
        okBtn.setOnAction(event -> {
            startSearching();
        });

        // radio buttons to filter the searched content by video, images or all
        ToggleGroup radioToggleGroup = new ToggleGroup();
        filterAll = new RadioButton("Tous");
        filterVideos = new RadioButton("Vidéos");
        filterImages = new RadioButton("Images");
        filterAll.setToggleGroup(radioToggleGroup);
        filterAll.setSelected(true);
        filterAll.setPadding(new Insets(0, 20, 70, 0));
        filterVideos.setToggleGroup(radioToggleGroup);
        filterVideos.setPadding(new Insets(0, 20, 70, 0));
        filterImages.setToggleGroup(radioToggleGroup);
        filterImages.setPadding(new Insets(0, 0, 70, 0));

        HBox radioButtonBox = new HBox();
        radioButtonBox.setAlignment(Pos.TOP_CENTER);
        radioButtonBox.getChildren().addAll(filterAll, filterVideos, filterImages);

        // populate the container with all others elements
        // research bar with its search button
        searchBox.getChildren().addAll(researchTextField, okBtn);

        // container for the title, search bar & his button and the radio buttons
        researchesVBox.getChildren().addAll(t, searchBox, radioButtonBox);

        // add the container(s) to the node list
        nodeList.add(menuBar);
        nodeList.add(researchesVBox);
        nodeList.add(scrollPane);

        return nodeList;
    }

    private void openHelpWindow() {
        System.out.println("help window opened");

        if (helpNodeList != null) {
            helpNodeList.clear();
        } else {
            helpNodeList = new ArrayList<>();
        }

        Text helpText = new Text();
        helpText.setText("Help text here");

        helpVbox = new VBox();
        helpVbox.getChildren().add(helpText);
        helpNodeList.add(helpVbox);

        Group helpGroup = new Group();
        helpGroup.getChildren().addAll(helpNodeList);

        Scene helpScene = new Scene(helpGroup, 800, 800);

        Stage helpStage = new Stage();
        helpStage.setScene(helpScene);
        helpStage.setResizable(false);
        helpStage.setTitle("Aide et informations");
        helpStage.initOwner(primaryStage);
        helpStage.initModality(Modality.WINDOW_MODAL);
        helpStage.show();
    }

    private void startSearching() {
        System.out.println(prefs.getBoolean(HAS_PATH_TO_DATA_DIR_BEEN_SET_PREF_KEY, false));
        if (!listOfResults.isEmpty()) {
            listOfResults.clear();
        }

        if (!matchingResultsList.isEmpty()) {
            matchingResultsList.clear();
        }

        // searches if the users has specified the folder to the data dir, and asks him if not
        if (!prefs.getBoolean(HAS_PATH_TO_DATA_DIR_BEEN_SET_PREF_KEY, false)) {
            System.out.println("not been set");

            Alert selectedFileAlert = new Alert(Alert.AlertType.INFORMATION, "Cliquez sur OK pour sélectionner un dossier source");
            selectedFileAlert.setHeaderText("Sélectionnez un dossier source");
            Optional<ButtonType> result = selectedFileAlert.showAndWait();

            // if the user clicks on OK, he may select a src directory for the content
            if (!result.isPresent() || result.get() != ButtonType.OK) {
                return;
            }
            // open a browser to choose a folder from
            File selectedFile = directoryChooser.showDialog(primaryStage);

            if (selectedFile != null && !selectedFile.getPath().equalsIgnoreCase("")) {
                System.out.println("no file selected");
                prefs.put(PATH_TO_DATA_DIR_PREF_KEY, selectedFile.getAbsolutePath());
                prefs.putBoolean(HAS_PATH_TO_DATA_DIR_BEEN_SET_PREF_KEY, true);
            } else {
                return;
            }
        }
        if (!prefs.getBoolean(HAS_PATH_TO_XML_FILE_BEEN_SET, false)) {
            System.out.println("c dla merde");
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cliquez sur OK pour générer une nouvelle base de données, ou séléctionnez en une dans les options");
            alert.setHeaderText("Aucune base de données spécifiée");
            Optional<ButtonType> result = alert.showAndWait();
            // if the user clicks on OK, a new txt file will be generated
            if (result.isPresent() && result.get() == ButtonType.OK) {
                _idIncrNumber = 0;
                generateXmlFile();
            }
        }

        // recreate the main box of all displayed results
        allResultsBox.getChildren().clear();

        // the results button list
        if (editButtonList == null) {
            editButtonList = new ArrayList<>();
        } else {
            editButtonList.clear();
        }

        try {
            if (crud == null) {
                System.out.println("null");
                crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"), false);
            }
            List<String[]> allResultsList = crud.viewAllRecords();

            for (String[] resultRow : allResultsList) {
                // items in resultRow :
                // 0 -> id
                // 1 -> type
                // 2 -> title
                // 3 -> tags
                // 4 -> path

                boolean resultMatch = matchSearchWords(resultRow);
                if (resultMatch) {
                  /*  System.out.println("+---------------------------------------------------+");
                    System.out.println("| " + resultRow[2]);
                    System.out.println("| " + resultRow[3]);
                    System.out.println("+---------------------------------------------------+" + "\n");*/

                    resultBox = new HBox();
                    //resultBox.setAlignment(Pos.CENTER);
                    resultBox.setPrefWidth(window_width - 25); // 1583
                    resultBox.setPrefHeight(window_height * 0.1); // 100
                    resultBox.setId("box" + resultRow[0]);

                    createResultButton(resultRow);

                    // we add the matching results in a list
                    matchingResultsList.add(resultRow);

                    // then we add this container to the main list of all results
                    //allResultsBox.getChildren().add(resultBox);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean matchSearchWords(String[] resultRow) {
        if (resultRow.length == 0) {
            System.out.println("\u001B[41m" + "Erreur : le fichier texte ne contient aucune données" + "\u001B[0m");
            return false;
        }

        String id = resultRow[0];
        String type = resultRow[1];
        String title = resultRow[2];
        String tags = resultRow[3];
        String path = resultRow[4];

        // if there is no research words, we display all results
        if (researchTextField.getText() == null || researchTextField.getText().equalsIgnoreCase("")) {
            return true;
        }

        String[] wordsResearched = researchTextField.getText().split(" ");
        if (wordsResearched.length == 0)
            return false;

        for (String word : wordsResearched) {
            // if the name OR the tags doesn't contains the searched words, we return false
            // is ok, but could be simplified...
            if (!removeAccents(normalize(title.toLowerCase(Locale.ROOT))).contains(removeAccents(normalize(word.toLowerCase(Locale.ROOT))))) {
                if (!removeAccents(normalize(tags.toLowerCase(Locale.ROOT))).contains(removeAccents(normalize(word.toLowerCase(Locale.ROOT))))) {
                    return false;
                }
            }
        }
        return true;
    }

    // replaces accents on strings and puts them after the concerned character
    // ex: à -> a`, ô -> o^
    String normalize(String input) {
        return input == null ? null : Normalizer.normalize(input, Normalizer.Form.NFKD);
    }

    // removes the accents from a string (e^ -> e...)
    String removeAccents(String input) {
        return normalize(input).replaceAll("\\p{M}", "");
    }

    // generate a new xml file
    // this fetches all files from the data folder specified and gather all known data (title, year)
    private void generateXmlFile() {
        // we need to fetch every file
        // we loop our way to the files
        String mainPath = prefs.get(PATH_TO_DATA_DIR_PREF_KEY, System.getProperty("user.home") + "\\pictures");
        File fileToSearch = new File(mainPath);
        System.out.println(mainPath);

        // we generate a txt name based on the src folder
        // ex: screenshots.txt
        String absolutePathOfXMLFile = new File(mainPath).getName() + ".txt";

        /*if (new File(absolutePathOfXMLFile).exists()) {
            testXmlFile = new File("generated/tmp_" + new File(mainPath).getName() + ".txt");
        } else */
        testXmlFile = new File("generated/" + absolutePathOfXMLFile);

        File generatedFolder = new File("generated");
        if (!generatedFolder.exists()) {
            System.out.println("generate txt folder : " + generatedFolder.mkdir());
        }

        prefs.put(PATH_TO_XML_FILE, absolutePathOfXMLFile);
        prefs.putBoolean(HAS_PATH_TO_XML_FILE_BEEN_SET, true);
        System.out.println(prefs.get(PATH_TO_XML_FILE, "NO!!!"));

        // recreate the main box of all displayed results
        allResultsBox.getChildren().clear();

        System.out.println("new xml file source : " + prefs.get(PATH_TO_XML_FILE, "none"));


        //    try {
          /*  fileWriter = new FileWriter(testXmlFile, true);
            bw = new BufferedWriter(fileWriter);
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<DATA>\n");*/

        crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"), true);
        searchFiles(fileToSearch);

//            bw.write("</DATA>\n");
//            bw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void searchFiles(File fileToSearch) {
        if (fileToSearch.isDirectory()) {
            File[] list = fileToSearch.listFiles();
            if (list != null) {
                for (File f : list) {
                    searchFiles(f.getAbsoluteFile());
                }
            }
        } else {
            // file is not directory
            // write the path name in the xml file
            writeInXMLFile(fileToSearch.getAbsolutePath(), fileToSearch.getName());
        }
    }

    // create a writer to put the path of a file inside the xml file
    private void writeInXMLFile(String absolutePathName, String fileName) {
        // we split the file name to obtain first the name itself and second the file extension (.mp4, .png ,...)
        String[] splitStrings = fileName.split("\\.");

        if (splitStrings.length == 0) {
            System.out.println((char) 27 + "[43m" + "Erreur : le fichier n'a aucun nom" + +(char) 27 + "[0m");
            return;
        }

        if (splitStrings.length == 1) {
            // message in yellow (warning)
            System.out.println((char) 27 + "[43m" + "Un fichier inconnu a été ignoré : " + splitStrings[0] + (char) 27 + "[0m");
            return;
        }

        // if the file is supported, this becomes true
        boolean wasFileAddedToList = false;

        for (String vidExt : SUPPORTED_VIDEO_EXTENSIONS) {
            if (splitStrings[1].equalsIgnoreCase(vidExt)) {
                // the tags must be manually added after
                try {
                    //todo: HERE IS THE TEST FOR THE TXT FILE
                    // there must be a tag (not empty) so we give the type

                    crud.addRecord(_idIncrNumber, "video", fileName, "video", absolutePathName);

                  /*  bw.write("<CONTENT>\n" +
                            "<TYPE>" + "VIDEO" + "</TYPE>\n" +
                            "<ID>" + _idIncrNumber + "</ID>\n" +
                            "<TITLE>" + fileName + "</TITLE>\n" +
                            "<TAGS></TAGS>\n" +
                            "<PATH_TO_CONTENT>" + absolutePathName + "</PATH_TO_CONTENT>\n" +
                            "</CONTENT>\n");*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
                wasFileAddedToList = true;
                _idIncrNumber++;
            }
        }
        for (String imgExt : SUPPORTED_IMAGES_EXTENSIONS) {
            if (!wasFileAddedToList && splitStrings[splitStrings.length - 1].equalsIgnoreCase(imgExt)) {
                // the tags must be manually added after
                //todo: for the images the title might be useless (ex: DSC00275.JPG)
                try {
                    //todo: HERE IS THE TEST FOR THE TXT FILE
                    // there must be a tag (not empty) so we give the type
                    //SimpleCRUD crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"));
                    crud.addRecord(_idIncrNumber, "image", fileName, "image", absolutePathName);

                    /*bw.write("<CONTENT>\n" +
                            "<TYPE>" + "IMAGE" + "</TYPE>\n" +
                            "<ID>" + _idIncrNumber + "</ID>\n" +
                            "<TITLE>" + fileName + "</TITLE>\n" +
                            "<TAGS></TAGS>\n" +
                            "<PATH_TO_CONTENT>" + absolutePathName + "</PATH_TO_CONTENT>\n" +
                            "</CONTENT>\n");*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
                wasFileAddedToList = true;
                _idIncrNumber++;
            }
        }

        if (!wasFileAddedToList) {
            System.out.println("A file was not added to the data list : format not supported : " + fileName);
        }
    }

    private void createResultButton(String[] resultRow) {
        String id = resultRow[0];
        String type = resultRow[1];
        String title = resultRow[2];
        String tags = resultRow[3];
        String path = resultRow[4];
/*
        resultButton = new Button(title + "\n" + tags + "    " + id);
        resultButton.setAlignment(Pos.CENTER);
        resultButton.setPrefHeight(100);
        resultButton.setPrefWidth(1400);
        resultButton.setOnAction(event ->
                openContentWithDefaultSoftware(path)
        );*/

        // the edit button
        Button editButton = new Button(title + "\n" + tags);
        editButton.setPrefHeight(scrollPane.getPrefHeight() * 0.1);
        editButton.setPrefWidth(scrollPane.getPrefWidth());
        editButton.setOnAction(event -> {
            openEditor(resultRow);
        });

        editButtonList.add(editButton);

        //HBox containerForResult = new HBox();
       // containerForResult.setPrefHeight(window_height * 0.1);
        //containerForResult.setPrefWidth(window_width);

        //containerForResult.getChildren().add(editButton);
        //resultBox.getChildren().add(editButton);
        allResultsBox.getChildren().add(editButton);
    }

    private void openEditor(String[] resultRow) {
        String id = resultRow[0];
        String type = resultRow[1];
        String title = resultRow[2];
        String tags = resultRow[3];
        String path = resultRow[4];

        boolean isMediaImage = true;
        AtomicBoolean isVideoPlaying = new AtomicBoolean(false);

        // need to make a new nodelist
        if (editNodeList == null) {
            editNodeList = new ArrayList<>();
        } else {
            editNodeList.clear();
        }

        // won't access if the window is opened again via the "edit" button
        // or if an arrow is pressed
        // or if the window is closed then reopened
        if (editStage == null || !editStage.isShowing()) {

            // containers
            editVbox = new VBox();
            editVbox.setAlignment(Pos.TOP_CENTER);
            editVbox.setPrefWidth(1400);
            editHbox = new HBox();
            editHbox.setAlignment(Pos.CENTER);

            // previous button
            editPreviousButton = new Button("<----");
            editPreviousButton.setStyle("-fx-font-size: 32");
            editPreviousButton.setPrefHeight(40);
            editPreviousButton.setPrefWidth(500);

            // nex button
            editNextButton = new Button("---->");
            editNextButton.setStyle("-fx-font-size: 32");
            editNextButton.setPrefHeight(40);
            editNextButton.setPrefWidth(500);

            editHbox.getChildren().addAll(editPreviousButton, editNextButton);

            // name of the file in the view
            contentTitle = new Text(title);

            // text area containing the tags (can be edited)
            tagsTextArea = new TextArea();
            tagsTextArea.setPromptText("Entrez des mots-clefs");
            tagsTextArea.setText(tags);
            tagsTextArea.setPrefHeight(60);

            // button to open the media with the system's reader
            openWithSystemButton = new Button("Ouvrir avec le lecteur");
            openWithSystemButton.setPrefHeight(40);
            openWithSystemButton.setOnAction(event ->
                    openContentWithDefaultSoftware(path)
            );

            // the stage
            editStage = new Stage();
            editStage.setResizable(false);
            editStage.setTitle("Éditer du contenu");
            editStage.initOwner(primaryStage);
            editStage.initModality(Modality.WINDOW_MODAL);
        }


        editPreviousButton.setOnAction(event -> {
            for (int i = 0; i < matchingResultsList.size(); i++) {
                if (matchingResultsList.get(i) == resultRow) {
                    if (i > 0) {
                        if (isVideoPlaying.get()) {
                            mediaPlayer.dispose();
                            isVideoPlaying.set(false);
                        }
                        String tagText = tagsTextArea.getText().replace("\n", " ");
                        //SimpleCRUD crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"));
                        try {
                            crud.updateTagsOfRecordById(id, tagText);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // we modify the view
                        matchingResultsList.get(i)[3] = tagText;
                        openEditor(matchingResultsList.get(i - 1));
                    }
                }
            }
        });

        editNextButton.setOnAction(event -> {
            for (int i = 0; i < matchingResultsList.size(); i++) {
                if (matchingResultsList.get(i) == resultRow) {
                    if (i < matchingResultsList.size() - 1) {
                        if (isVideoPlaying.get()) {
                            mediaPlayer.dispose();
                            isVideoPlaying.set(false);
                        }
                        String tagText = tagsTextArea.getText().replace("\n", " ");
                        //SimpleCRUD crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"));
                        try {
                            crud.updateTagsOfRecordById(id, tagText);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // we modify the view
                        matchingResultsList.get(i)[3] = tagText;
                        openEditor(matchingResultsList.get(i + 1));
                    }
                }
            }
        });


        if (type != null && type.equals("image")) {
            try {
                Image image = new Image(new FileInputStream(path));
                imageView = new ImageView(image);
                imageView.setFitHeight(600);
                imageView.setFitWidth(1400);
                imageView.setPreserveRatio(true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (type != null && type.equals("video")) {
            isMediaImage = false;
            Media media = new Media(new File(path).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView = new MediaView(mediaPlayer);
            mediaView.setFitHeight(600);
            mediaView.setFitWidth(1400);
            mediaView.setPreserveRatio(true);
            mediaView.setOnMouseClicked(event -> {
                if (!isVideoPlaying.get()) {
                    mediaPlayer.play();
                    isVideoPlaying.set(true);
                } else {
                    mediaPlayer.pause();
                    isVideoPlaying.set(false);
                }
            });
        }

        if (!editVbox.getChildren().isEmpty()) {
            editVbox.getChildren().remove(0);
            if (isMediaImage) {
                editVbox.getChildren().add(0, imageView);
            } else {
                editVbox.getChildren().add(0, mediaView);
            }
            contentTitle.setText(title);
            tagsTextArea.setText(tags);
            openWithSystemButton.setOnAction(event ->
                    openContentWithDefaultSoftware(path)
            );
        } else {
            if (isMediaImage) {
                editVbox.getChildren().add(imageView);
            } else {
                editVbox.getChildren().add(0, mediaView);
            }
            editVbox.getChildren().add(contentTitle);
            editVbox.getChildren().add(editHbox);
            editVbox.getChildren().add(tagsTextArea);
            editVbox.getChildren().add(openWithSystemButton);
        }

        editNodeList.add(editVbox);

        if (editGroup == null) {
            editGroup = new Group();
        } else {
            editGroup.getChildren().clear();
        }

        editGroup.getChildren().addAll(editNodeList);

        if (editScene == null) {
            editScene = new Scene(editGroup, 1400, 800);
        }
        editStage.setScene(editScene);

        // when the editor is closed, we stop the video if it is playing
        // then, we save the tags entered by the user into the xml file
        editStage.setOnCloseRequest(event -> {
            if (isVideoPlaying.get()) {
                mediaPlayer.dispose();
                isVideoPlaying.set(false);
            }
            String tagText = tagsTextArea.getText().replace("\n", " ");
            //SimpleCRUD crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"));
            try {
                crud.updateTagsOfRecordById(id, tagText);

            } catch (IOException e) {
                e.printStackTrace();
            }
            for (String[] strings : matchingResultsList) {
                if (strings == resultRow) {
                    // we modify the view
                    strings[3] = tagText;
                }
            }
        });
        editStage.show();
    }

    private void openContentWithDefaultSoftware(String uri) {
        // try to open the video with the provided uri
        System.out.println("File opened : " + uri);
        try {
            File file = new File(uri);

            // check if Desktop is supported by Platform or not
            if (!Desktop.isDesktopSupported()) {
                System.out.println("not supported");
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            //checks file exists or not
            if (file.exists()) {
                //opens the specified file
                desktop.open(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
