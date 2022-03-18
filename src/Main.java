import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.awt.*;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class Main extends Application {

    Stage primaryStage;
    private Stage editStage;
    private Scene editScene;

    // Width and height of the whole application
    private double window_height;
    private double window_width;

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
    private VBox researchesVBox;
    private List<Button> editButtonList;
    private RadioButton filterAll;
    private RadioButton filterVideos;
    private RadioButton filterImages;
    private HBox scrollAndEditorBox;
    private VBox embedEditorBox;
    private Text resultsCountText;

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
    private boolean isEmbedEditorOpened;
    private Image image;
    private boolean isVideoPlaying = false;

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
    final String[] SUPPORTED_VIDEO_EXTENSIONS = {"mp4", "mpg", "wmv", "fxm", "flv"};
    final String[] SUPPORTED_IMAGES_EXTENSIONS = {"jpg", "png", "gif", "mpo"};

    // the class who manages all io operations on the txt files
    private SimpleCRUD crud;

    @Override
    public void start(Stage primaryStage) {

        nodeList = new ArrayList<>();

        // the dimensions for the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        window_width = screenSize.width * 0.6;
        window_height = screenSize.height * 0.75;

        group = new Group();
        group.getChildren().addAll(populateGroup());
        //group.setAutoSizeChildren(true);

        listOfResults = new ArrayList<>();
        matchingResultsList = new ArrayList<>();

        prefs = Preferences.userRoot().node(this.getClass().getName());

        Scene scene = new Scene(group, window_width, window_height);

        //todo: bug: the image/video size does not properly resize itself ONLY WHEN ENTERING the fullscreen, but works after a refresh
        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollAndEditorBox.setPrefHeight(newValue.doubleValue() * 0.7);

            if (editButtonList != null) {
                for (Button b : editButtonList) {
                    //b.setPrefHeight(scrollPane.getPrefHeight() * 0.1);
                    b.setPrefHeight(scrollAndEditorBox.getPrefHeight() * 0.1);
                }
            }
        });

        // if the window is resized, we also resize all nodes affected by this resize
        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            scrollAndEditorBox.setPrefWidth(newValue.doubleValue() - 10);

            // if the embed editor is opened, we reduce the with of the scroll pane
            if (isEmbedEditorOpened) {
                embedEditorBox.setPrefWidth(newValue.doubleValue() * 0.7);
                scrollPane.setPrefWidth(scrollAndEditorBox.getPrefWidth() * 0.3);
                //System.out.println(scrollPane.getPrefWidth());
                allResultsBox.setPrefWidth(scrollPane.getPrefWidth() - 22);
                if (editButtonList != null) {
                    for (Button b : editButtonList) {
                        b.setPrefWidth(allResultsBox.getPrefWidth());
                        //b.setPrefWidth(scrollAndEditorBox.getPrefWidth() - 15);
                    }
                }
                editNextButton.setPrefHeight(embedEditorBox.getPrefHeight() * 0.1);
                editNextButton.setPrefWidth(embedEditorBox.getPrefWidth() * 0.3);
                editPreviousButton.setPrefHeight(embedEditorBox.getPrefHeight() * 0.1);
                editPreviousButton.setPrefWidth(embedEditorBox.getPrefWidth() * 0.3);

                if (imageView != null) {
                    imageView.setFitHeight(embedEditorBox.getHeight() * 0.7);
                    imageView.setFitWidth(embedEditorBox.getPrefWidth() * 0.9);
                }
                if (mediaView != null) {
                    mediaView.setFitHeight(embedEditorBox.getHeight() * 0.7);
                    mediaView.setFitWidth(embedEditorBox.getPrefWidth() * 0.9);
                }
            } else {
                scrollPane.setPrefWidth(scrollAndEditorBox.getPrefWidth());
                allResultsBox.setPrefWidth(scrollPane.getPrefWidth() - 22);
                if (editButtonList != null) {
                    for (Button b : editButtonList) {

                        b.setPrefWidth(allResultsBox.getPrefWidth());
                        //b.setPrefWidth(scrollAndEditorBox.getPrefWidth() - 15);
                    }
                }

            }

            menuBar.setPrefWidth(newValue.doubleValue());
            //searchBox.setPrefWidth(newValue.doubleValue() - 10);
            searchBox.setPrefWidth(newValue.doubleValue());
            //searchBox.setMaxWidth(newValue.doubleValue() - 10);
            //searchBox.setMinWidth(newValue.doubleValue() - 10);
            researchTextField.setPrefWidth(newValue.doubleValue() - 100);
            //researchTextField.setLayoutX(newValue.doubleValue() - 100);
            //t.setLayoutX(newValue.doubleValue() * 0.3);
            researchesVBox.setPrefWidth(newValue.doubleValue());
            //resultBox.setPrefWidth(scrollPane.getPrefWidth() - 15); // 1583

            //resultBox.setPrefHeight(window_height * 0.1); // 100
        });


        primaryStage.setTitle("Recherche de contenu");
        primaryStage.setMinWidth(window_width * 0.45);
        primaryStage.setMinHeight(window_height * 0.67); // 0.62
        primaryStage.setScene(scene);
        scene.getRoot().requestFocus();
        this.primaryStage = primaryStage;

        // forward/backward feature
        scene.setOnKeyReleased(event -> {
            if (mediaPlayer != null && !mediaPlayer.getStatus().equals(MediaPlayer.Status.DISPOSED) && mediaView != null && mediaView.isFocused()) {
               // mediaView.requestFocus();
            if (event.getCode() == KeyCode.RIGHT) {
                    mediaPlayer.seek(mediaPlayer.getCurrentTime().add(Duration.seconds(10)));
                System.out.println("forward : media time: " + (int) mediaPlayer.getCurrentTime().toSeconds() + "/" + (int) mediaPlayer.getTotalDuration().toSeconds());
            }
                if (event.getCode() == KeyCode.LEFT) {
                    mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(Duration.seconds(10)));
                    System.out.println("backward : media time: " + (int) mediaPlayer.getCurrentTime().toSeconds() + "/" + (int) mediaPlayer.getTotalDuration().toSeconds());
                }
                if (event.getCode() == KeyCode.SPACE) {
                    if (isVideoPlaying) {
                        System.out.println("paused");
                        mediaPlayer.pause();
                        isVideoPlaying = false;
                    } else {
                        System.out.println("resumed");
                        mediaPlayer.play();
                        isVideoPlaying = true;
                    }
                }
            }
        });

        primaryStage.show();
    }

    List<Node> populateGroup() {
        // list of all elements that will be added

        // container for all results
        allResultsBox = new VBox();
        System.out.println(window_width);
        allResultsBox.setPrefWidth(window_width - 32);

        // scrollbar for the results list
        scrollPane = new ScrollPane(allResultsBox);
        scrollPane.setPrefWidth(window_width - 10);
        scrollPane.setPrefHeight(window_height - 180);
        scrollPane.setLayoutX(5);
        scrollPane.setLayoutY(window_height * 0.20);
        // scrollPane.setStyle("-fx-border-color: blue;");

        // main vertical container
        researchesVBox = new VBox();
        researchesVBox.setAlignment(Pos.TOP_CENTER);
        researchesVBox.setPrefWidth(window_width);
        researchesVBox.setPrefHeight(window_height * 0.2); // 200
        //researchesVBox.setFillWidth(true);
        researchesVBox.setLayoutY(50); //50

        // horizontal search container
        searchBox = new HBox();
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setPrefWidth(window_width);
        searchBox.setPrefHeight(window_height * 0.1); // 100
        //searchBox.setMinWidth(window_width * 0.9); // 1500

        // title text
        Text t = new Text("Recherche de photos ou vidéos");
        t.setTextAlignment(TextAlignment.CENTER);
        t.setStyle("-fx-font-size: 32");
        t.setLayoutX(window_width * 0.3);

        // text input
        researchTextField = new TextField();
        researchTextField.setPromptText("Rechercher par titre ou mot-clef");
        researchTextField.setPrefWidth(window_width * 0.5);
        researchTextField.setMaxWidth(window_width * 0.5);
        researchTextField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                closeEmbedEditor();
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
            closeEmbedEditor();

            // we reset the id number
            _idIncrNumber = 0;
            generateTxtFile();
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

                // we close the editor and clear the text input field
                closeEmbedEditor();
                researchTextField.setText("");

                // - if there is no txt file associated to the src folder, we create one in generated
                //   we give it the name of the src folder
                // - if there is a txt file with the name of the src, we store the tags previously modified, then create a new txt file
                //   we add to it the tags associated with their id, and delete the original file
                _idIncrNumber = 0;
                generateTxtFile();
            }
        });

        MenuItem changeCurrentXmlFile = new MenuItem("Changer de fichier de sauvegarde (sélectionner une autre base de données)");
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
                closeEmbedEditor();
            }
        });

        // refresh txt if an item was added
        // we create a copy of the existing txt file then we append the new data
        MenuItem helpItem = new MenuItem("Aide et préférences");
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
            closeEmbedEditor();
            startSearching();
        });

        // radio buttons to filter the searched content by video, images or all
        ToggleGroup radioToggleGroup = new ToggleGroup();
        filterAll = new RadioButton("Tous");
        filterVideos = new RadioButton("Vidéos");
        filterImages = new RadioButton("Photos");
        filterAll.setToggleGroup(radioToggleGroup);
        filterAll.setSelected(true);
        filterAll.setPadding(new Insets(0, 20, 0, 0));
        filterAll.setFocusTraversable(false);
        filterVideos.setToggleGroup(radioToggleGroup);
        filterVideos.setPadding(new Insets(0, 20, 0, 0));
        filterVideos.setFocusTraversable(false);
        filterImages.setToggleGroup(radioToggleGroup);
        filterImages.setPadding(new Insets(0, 0, 0, 0));
        filterImages.setFocusTraversable(false);

        // if the user hits one of the filter buttons, it will directly make a research
        filterAll.setOnAction(event -> {
            closeEmbedEditor();
            startSearching();
        });
        filterImages.setOnAction(event -> {
            closeEmbedEditor();
            startSearching();
        });
        filterVideos.setOnAction(event -> {
            closeEmbedEditor();
            startSearching();
        });

        HBox radioButtonBox = new HBox();
        radioButtonBox.setAlignment(Pos.TOP_CENTER);
        radioButtonBox.getChildren().addAll(filterAll, filterVideos, filterImages);

        // populate the container with all others elements
        // research bar with its search button
        searchBox.getChildren().addAll(researchTextField, okBtn);

        resultsCountText = new Text();
        resultsCountText.setStyle("-fx-font-size: 18");

        // container for the title, search bar & his button and the radio buttons
        researchesVBox.getChildren().addAll(t, searchBox, radioButtonBox, resultsCountText);

        // container for the embed editor
        embedEditorBox = new VBox();
        //embedEditorBox.setStyle("-fx-border-color: red;");
        embedEditorBox.setAlignment(Pos.BOTTOM_CENTER);

        // Hbox containing the scroll pane and the embed editor
        scrollAndEditorBox = new HBox();
        scrollAndEditorBox.setPrefWidth(window_width - 10);
        scrollAndEditorBox.setPrefHeight(window_height * 0.7);
        scrollAndEditorBox.setLayoutX(5);
        scrollAndEditorBox.setLayoutY(researchesVBox.getPrefHeight() + 50);
        //scrollAndEditorBox.setStyle("-fx-border-color: black;");

        scrollAndEditorBox.getChildren().addAll(scrollPane, embedEditorBox);

        // add the container(s) to the node list
        nodeList.add(menuBar);
        nodeList.add(researchesVBox);
        //nodeList.add(scrollPane);
        nodeList.add(scrollAndEditorBox);

        return nodeList;
    }

    private void closeEmbedEditor() {
        if (mediaView != null && isVideoPlaying) {
            mediaPlayer.dispose();
            isVideoPlaying = false;
        }
        if (embedEditorBox != null && isEmbedEditorOpened) {
            // close the embed editor (leave it blank without getting rid of it)
            embedEditorBox.getChildren().clear();
            embedEditorBox.setPrefWidth(0);
            scrollPane.setPrefWidth(scrollAndEditorBox.getPrefWidth());
            allResultsBox.setPrefWidth(scrollPane.getPrefWidth() - 22);
            for (Button b : editButtonList) {
                b.setPrefWidth(allResultsBox.getPrefWidth());
            }
            isEmbedEditorOpened = false;
        }
    }

    private void openHelpWindow() {
        System.out.println("help window opened");

        if (helpNodeList != null) {
            helpNodeList.clear();
        } else {
            helpNodeList = new ArrayList<>();
        }

        Text searchUseTitle = new Text("Utilisation : barre de recherche\n");
        searchUseTitle.setStyle("-fx-font-size: 24;-fx-font-weight: bold;");
        searchUseTitle.setTextAlignment(TextAlignment.CENTER);

        Text searchUseDesc1 = new Text("Dans la barre de recherche, entrez le texte contenu dans le titre du média recherché ");
        searchUseDesc1.setStyle("-fx-font-size: 16;");

        Text searchUseDesc2 = new Text("OU ");
        searchUseDesc2.setStyle("-fx-font-size: 16;-fx-font-weight: bold;");

        Text searchUseDesc3 = new Text("dans les mots-clefs (la description) préalablement reseignés. " +
                "Si la barre de recherche ne contient rien, tous les résultats seront affichés." +
                "\n\nLes résultats affichés sont ceux qui contiennent ");
        searchUseDesc3.setStyle("-fx-font-size: 16;");

        Text searchUseDesc4 = new Text("TOUS ");
        searchUseDesc4.setStyle("-fx-font-size: 16;-fx-font-weight: bold;");

        Text searchUseDesc5 = new Text("les mots entrés.\nExemple : si on recherche '");
        searchUseDesc5.setStyle("-fx-font-size: 16");

        Text searchUseDesc6 = new Text("anniversaire mariage");
        searchUseDesc6.setStyle("-fx-font-size: 16;-fx-font-style: italic;");

        Text searchUseDesc7 = new Text("', le résultat '");
        searchUseDesc7.setStyle("-fx-font-size: 16");

        Text searchUseDesc8 = new Text("anniversaire de mariage d'Elisabeth et Patrice");
        searchUseDesc8.setStyle("-fx-font-size: 16;-fx-font-style: italic;");

        Text searchUseDesc9 = new Text("' apparaîtra comme résultat, mais pas '");
        searchUseDesc9.setStyle("-fx-font-size: 16");

        Text searchUseDesc10 = new Text("anniversaire de Hubert");
        searchUseDesc10.setStyle("-fx-font-size: 16;-fx-font-style: italic;");

        Text searchUseDesc11 = new Text("' ou bien '");
        searchUseDesc11.setStyle("-fx-font-size: 16");

        Text searchUseDesc12 = new Text("mariage de Cathel et Laurent");
        searchUseDesc12.setStyle("-fx-font-size: 16;-fx-font-style: italic;");

        Text searchUseDesc13 = new Text("'.\n\nLes résultats peuvent être filtrés par type : tous (aucun filtrage), vidéos uniquement ou photos uniquement.\n\n");
        searchUseDesc13.setStyle("-fx-font-size: 16");

        TextFlow searchUseTextFlow = new TextFlow();
        searchUseTextFlow.setTextAlignment(TextAlignment.JUSTIFY);
        searchUseTextFlow.getChildren().addAll(searchUseDesc1, searchUseDesc2, searchUseDesc3, searchUseDesc4, searchUseDesc5,
                searchUseDesc6, searchUseDesc7, searchUseDesc8, searchUseDesc9, searchUseDesc10, searchUseDesc11, searchUseDesc12, searchUseDesc13);

        Text editorUseTitle = new Text("Utilisation : éditeur\n");
        editorUseTitle.setStyle("-fx-font-size: 24;-fx-font-weight: bold;");
        editorUseTitle.setTextAlignment(TextAlignment.CENTER);

        Text editorUseDesc = new Text("Une fois avoir cliqué sur un résultat, l'éditeur de contenu s'ouvre. Il est composé de plusieurs éléments :\n\n" +
                "- le lecteur, qui affiche les photos et les vidéos. Si le contenu est une vidéo, elle peux être lancée ou mise en pause en cliquant sur la zone de lecture. " +
                "Il est possible d'avancer ou de reculer de 10 secondes dans la vidéo en utilisant les flèches droite et gauche, et de mettre en pause/reprendre la vidéo " +
                "en appuyant sur espace\n\n" +
                "- le titre du média\n\n" +
                "- les flèches suivant et précedent pour passer au résultat suivant ou précédent dans la liste des résultats\n\n" +
                "- le champ de texte dans lequel des mots-clefs peuvent être écrits, et qui pourront être recherchés par la suite pour trouver le média correspondant\n\n" +
                "- le bouton 'ouvrir avec le lecteur' pour lancer le lecteur par défaut de l'ordinateur, utile pour afficher l'image ou la vidéo en plein écran\n\n" +
                "Lors de la saisie des mots-clefs, il est conseillé d'entrer le maximum d'informations pouvant identifier le média.\n" +
                "Le lieu, les personnes présentes, le sujet ou évènement de la photo/vidéo sont les informations les plus importantes.");
        editorUseDesc.setStyle("-fx-font-size: 16");

        TextFlow editorUseTextFlow = new TextFlow();
        editorUseTextFlow.setTextAlignment(TextAlignment.JUSTIFY);
        editorUseTextFlow.getChildren().addAll(editorUseDesc);

        helpVbox = new VBox();
        helpVbox.setAlignment(Pos.CENTER);
        helpVbox.setPrefWidth(window_width * 0.8 - 30);
        helpVbox.setLayoutX(10);
        helpVbox.getChildren().addAll(searchUseTitle, searchUseTextFlow, editorUseTitle, editorUseTextFlow);

        ScrollBar sb = new ScrollBar();
        sb.setLayoutX(window_width * 0.8 - 15);
        sb.setPrefHeight(window_height * 0.8);
        sb.setVisibleAmount(30);

        helpNodeList.add(helpVbox);
        helpNodeList.add(sb);

        Group helpGroup = new Group();
       /* helpGroup.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                System.out.println(helpVbox.getLayoutY());
                if (helpVbox.getLayoutY() >= 0) {
                    helpVbox.setTranslateY(helpVbox.getTranslateY() + event.getDeltaY());
                }
            }
        });*/

        sb.setOrientation(Orientation.VERTICAL);
        sb.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                helpVbox.setLayoutY(-newValue.doubleValue());
            }
        });
        helpGroup.getChildren().addAll(helpNodeList);

        Scene helpScene = new Scene(helpGroup, window_width * 0.8, window_height * 0.8);

        Stage helpStage = new Stage();
        helpStage.setScene(helpScene);
        helpStage.setResizable(false);
        helpStage.setTitle("Aide et préférences");
        helpStage.initOwner(primaryStage);
        helpStage.initModality(Modality.WINDOW_MODAL);
        helpStage.show();
    }

    private void startSearching() {
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
                generateTxtFile();
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
            // if (crud == null) {

            crud = new SimpleCRUD(prefs.get(PATH_TO_XML_FILE, "nope"), false);
            //   }

            // we add a flag to filter the results
            int fileTypeFlag = -1;
            if (filterAll.isSelected()) {
                fileTypeFlag = 0;
            } else if (filterVideos.isSelected()) {
                fileTypeFlag = 1;
            } else if (filterImages.isSelected()) {
                fileTypeFlag = 2;
            }


            List<String[]> allResultsList = crud.viewAllRecords(fileTypeFlag);
            int countResults = 0;

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

                    countResults++;
                }

            }
            resultsCountText.setText(countResults + " résultats");
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
        if (researchTextField.getText() == null || researchTextField.getText().equalsIgnoreCase("") || containsOnlySpaces(researchTextField.getText())) {
            return true;
        }

        String[] wordsResearched = researchTextField.getText().split(" ");
        if (wordsResearched.length == 0) {
            System.out.println("search nothing");
            return false;
        }

        boolean includedWord = false;

        for (String word : wordsResearched) {
            // if a word has this pattern : w1+w2
            // we make a search for content that has both w1 and w2
            // we need to make another splitter
            //todo: the "+" splitter also works with " ", maybe change that ?
            String[] containsWords = word.split("\\+");
            if (containsWords.length > 1) {
                System.out.println(Arrays.toString(containsWords));
                boolean matchingWord = true;

            for (String wrd : containsWords) {
                    if (!removeAccents(normalize(title.toLowerCase(Locale.ROOT))).contains(removeAccents(normalize(wrd.toLowerCase(Locale.ROOT))))) {
                        if (!removeAccents(normalize(tags.toLowerCase(Locale.ROOT))).contains(removeAccents(normalize(wrd.toLowerCase(Locale.ROOT)))))
                        matchingWord = false;
                    }
                }
                if (matchingWord) {
                    return true;
                }
            }


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

    // checks if a string contains only spaces
    private boolean containsOnlySpaces(String researchTextField) {
        char[] chars = researchTextField.toCharArray();
        // we loop through the chars
        for (char c : chars) {
            if (c != ' ') {
                return false;
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

    //generate a new xml file
    //this fetches all files from the data folder specified and gather all known data (title, year)
    private void generateTxtFile() {
        // we need to fetch every file
        // we loop our way to the files
        String mainPath = prefs.get(PATH_TO_DATA_DIR_PREF_KEY, System.getProperty("user.home") + "\\pictures");
        File fileToSearch = new File(mainPath);
        System.out.println(mainPath);

        // we generate a txt name based on the src folder
        // ex: screenshots.txt
        String absolutePathOfXMLFile = new File(mainPath).getAbsolutePath() + ".txt";

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
        // we need to escape comma characters, or the txt file will be messed up

        // we split the file name to obtain first the name itself and second the file extension (.mp4, .png ,...)
        String[] splitStrings = fileName.split("\\.");

        if (splitStrings.length == 0) {
            // message in yellow (warning)
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
            if (splitStrings[splitStrings.length - 1].equalsIgnoreCase(vidExt)) {
                // the tags must be manually added after
                try {
                    crud.addRecord(_idIncrNumber, "video", fileName, "", absolutePathName);
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
                try {
                    crud.addRecord(_idIncrNumber, "image", fileName, "", absolutePathName);
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
        Button editButton = new Button(title);
        editButton.setPrefHeight(scrollAndEditorBox.getPrefHeight() * 0.1);
        editButton.setPrefWidth(allResultsBox.getPrefWidth());
        editButton.setFocusTraversable(false);
        editButton.setOnAction(event -> {
            // if a video is playing, we stop and dispose it
            if (mediaPlayer != null && isVideoPlaying) {
                mediaPlayer.dispose();
            }
            openEmbedEditor(resultRow);
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

    private void openEmbedEditor(String[] resultRow) {
        String id = resultRow[0];
        String type = resultRow[1];
        String title = resultRow[2];
        String tags = resultRow[3];
        String path = resultRow[4];

        // checks if the editor container is empty, and clears it if not
        if (!embedEditorBox.getChildren().isEmpty()) {
            embedEditorBox.getChildren().clear();
        }

        // sets the size of the container
        // also reduces the width of the scroll pane
        isEmbedEditorOpened = true;
        //scrollAndEditorBox.setPrefWidth(primaryStage.getWidth() - 25);
        embedEditorBox.setPrefWidth(scrollAndEditorBox.getPrefWidth() * 0.7);
        scrollPane.setPrefWidth(scrollAndEditorBox.getPrefWidth() * 0.3);
        allResultsBox.setPrefWidth(scrollPane.getPrefWidth() - 22);
        if (editButtonList != null) {
            for (Button b : editButtonList) {
                b.setPrefWidth(allResultsBox.getPrefWidth());
            }
        }

        boolean isMediaImage = true;
        isVideoPlaying = false;

        editHbox = new HBox();
        editHbox.setAlignment(Pos.BOTTOM_CENTER);

        // previous button
        editPreviousButton = new Button("<---");
        //editPreviousButton.setStyle("-fx-font-size: 32");
        editPreviousButton.setPrefHeight(embedEditorBox.getPrefHeight() * 0.1); //40
        editPreviousButton.setPrefWidth(embedEditorBox.getPrefWidth() * 0.3); // 500
        editPreviousButton.setFocusTraversable(false);

        // next button
        editNextButton = new Button("--->");
        //editNextButton.setStyle("-fx-font-size: 32");
        editNextButton.setPrefHeight(embedEditorBox.getPrefHeight() * 0.1);
        editNextButton.setPrefWidth(embedEditorBox.getPrefWidth() * 0.3);
        editNextButton.setFocusTraversable(false);

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
        openWithSystemButton.setOnAction(event -> {
            if (mediaPlayer != null && isVideoPlaying) {
                mediaPlayer.pause();
                isVideoPlaying = false;
            }
            openContentWithDefaultSoftware(path);
        });


        editPreviousButton.setOnAction(event -> {
            for (int i = 0; i < matchingResultsList.size(); i++) {
                if (matchingResultsList.get(i) == resultRow) {
                    if (i > 0) {
                        if (isVideoPlaying) {
                            mediaPlayer.dispose();
                            isVideoPlaying = false;
                        }
                        String tagText = tagsTextArea.getText().replace("\n", " ");
                        try {
                            crud.updateTagsOfRecordById(id, tagText);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // we modify the view
                        matchingResultsList.get(i)[3] = tagText;
                        openEmbedEditor(matchingResultsList.get(i - 1));
                    }
                }
            }
        });

        editNextButton.setOnAction(event -> {
            for (int i = 0; i < matchingResultsList.size(); i++) {
                if (matchingResultsList.get(i) == resultRow) {
                    if (i < matchingResultsList.size() - 1) {
                        if (isVideoPlaying) {
                            mediaPlayer.dispose();
                            isVideoPlaying = false;
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
                        openEmbedEditor(matchingResultsList.get(i + 1));
                    }
                }
            }
        });


        if (type != null && type.equals("image")) {
            try {
                image = new Image(new FileInputStream(path));
                imageView = new ImageView(image);
                imageView.setFitWidth(embedEditorBox.getPrefWidth() * 0.9);
                imageView.setFitHeight(embedEditorBox.getHeight() * 0.7);
                imageView.setPreserveRatio(true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (type != null && type.equals("video")) {
            isMediaImage = false;
            Media media = new Media(new File(path).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView = new MediaView(mediaPlayer);


            mediaView.setFitWidth(embedEditorBox.getPrefWidth() * 0.9); // 1400
            mediaView.setFitHeight(embedEditorBox.getPrefHeight() * 0.7);
            mediaView.setPreserveRatio(true);
            mediaView.setOnMouseClicked(event -> {
                mediaView.requestFocus();
                if (!isVideoPlaying) {
                    mediaPlayer.play();
                    isVideoPlaying = true;
                } else {
                    mediaPlayer.pause();
                    isVideoPlaying = false;
                }
            });
        }

        if (!embedEditorBox.getChildren().isEmpty()) {
            embedEditorBox.getChildren().remove(0);
            if (isMediaImage) {
                embedEditorBox.getChildren().add(0, imageView);
            } else {
                embedEditorBox.getChildren().add(0, mediaView);
            }
            contentTitle.setText(title);
            tagsTextArea.setText(tags);
            openWithSystemButton.setOnAction(event ->
                    openContentWithDefaultSoftware(path)
            );
        } else {
            if (isMediaImage) {
                embedEditorBox.getChildren().add(imageView);
            } else {
                embedEditorBox.getChildren().add(0, mediaView);
            }
            embedEditorBox.getChildren().add(contentTitle);
            embedEditorBox.getChildren().add(editHbox);
            embedEditorBox.getChildren().add(tagsTextArea);
            embedEditorBox.getChildren().add(openWithSystemButton);
        }
       /* if (!editVbox.getChildren().isEmpty()) {
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
        }*/

       /* editNodeList.add(editVbox);

        if (editGroup == null) {
            editGroup = new Group();
        } else {
            editGroup.getChildren().clear();
        }

        editGroup.getChildren().addAll(editNodeList);

        if (editScene == null) {
            editScene = new Scene(editGroup, 1400, 800);
        }
        editStage.setScene(editScene);*/

        /*// when the editor is closed, we stop the video if it is playing
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
        editStage.show();*/
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
