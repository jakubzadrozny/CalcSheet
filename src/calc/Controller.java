package calc;

import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class Controller {

    private Main app;
    private File filePath;

    public void setApp (Main app) { this.app = app; }

    private final int ROWS = 40;
    private final int COLS = 26;
    private final int HEADER_WIDTH = 30;
    private final int CELL_WIDTH = 100;
    private final int CELL_HEIGHT = 25;

    private HBox active;

    private Environment env = new Environment();
    private Map<String, HBox> cells = new HashMap<>();

    @FXML
    private VBox box;

    @FXML
    private JFXTextField input;

    @FXML
    private GridPane grid;

    @FXML
    private void initialize () {
        box.setPrefSize(500, 500);
        box.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        grid.getColumnConstraints().add(new ColumnConstraints(HEADER_WIDTH));
        grid.getRowConstraints().add(new RowConstraints(CELL_HEIGHT));

        for(int i = 1; i <= ROWS; i++) {
            grid.getRowConstraints().add(new RowConstraints(CELL_HEIGHT));
            HBox header = new HBox();
            resetClassAtPos(header, 0, i);
            header.getChildren().add(new Label(String.valueOf(i)));
            grid.add(header, 0, i);
        }

        for(int i = 1; i <= COLS; i++) {
            grid.getColumnConstraints().add(new ColumnConstraints(CELL_WIDTH));
            HBox header = new HBox();
            resetClassAtPos(header, i, 0);
            header.getChildren().add(new Label(Character.toString((char) (i + 64))));
            grid.add(header, i, 0);
        }

        for(int i = 1; i <= ROWS; i++) {
            for(int j = 1; j <= COLS; j++) {

                HBox cell = new HBox();
                cells.put(GenerateName(j, i), cell);

                resetClassAtPos(cell, j, i);
                cell.getChildren().add(new Label());
                cell.getProperties().put("cellY", i);
                cell.getProperties().put("cellX", j);

                cell.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                    if(active != null) active.getStyleClass().remove("active");
                    active = (HBox) ev.getSource();
                    active.getStyleClass().add("active");

                    int x = (int) active.getProperties().get("cellX");
                    int y = (int) active.getProperties().get("cellY");
                    String inp = env.getInput(GenerateName(x, y));
                    if(inp != null) input.setText(inp);
                    else input.setText("");
                });

                cell.setOnDragDetected(event -> {
                    Dragboard db = cell.startDragAndDrop(TransferMode.ANY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("string");
                    db.setContent(content);

                    event.consume();
                });

                cell.setOnDragOver(event -> {
                    if (event.getGestureSource() != cell)
                        event.acceptTransferModes(TransferMode.ANY);

                    event.consume();
                });

                cell.setOnDragEntered(event -> {
                    if (event.getGestureSource() == cell) return;

                    HBox source = (HBox) event.getGestureSource();
                    int sourceX = (int) source.getProperties().get("cellX");
                    int sourceY = (int) source.getProperties().get("cellY");
                    int targetX = (int) cell.getProperties().get("cellX");
                    int targetY = (int) cell.getProperties().get("cellY");

                    for(int ii = sourceX; ii <= targetX; ii++) {
                        for(int jj = sourceY; jj <= targetY; jj++) {
                            String name = Controller.GenerateName(ii, jj);
                            cells.get(name).getStyleClass().add("active");
                        }
                    }

                    event.consume();
                });

                cell.setOnDragExited(event -> {
                    if(event.getGestureSource() == cell) return;

                    HBox source = (HBox) event.getGestureSource();
                    int sourceX = (int) source.getProperties().get("cellX");
                    int sourceY = (int) source.getProperties().get("cellY");
                    int targetX = (int) cell.getProperties().get("cellX");
                    int targetY = (int) cell.getProperties().get("cellY");

                    for(int ii = sourceX; ii <= targetX; ii++) {
                        for(int jj = sourceY; jj <= targetY; jj++) {
                            String name = Controller.GenerateName(ii, jj);
                            cells.get(name).getStyleClass().remove("active");
                        }
                    }

                    event.consume();
                });

                cell.setOnDragDropped(event -> {
                    if(event.getGestureSource() == cell) return;

                    HBox source = (HBox) event.getGestureSource();
                    int sourceX = (int) source.getProperties().get("cellX");
                    int sourceY = (int) source.getProperties().get("cellY");
                    int targetX = (int) cell.getProperties().get("cellX");
                    int targetY = (int) cell.getProperties().get("cellY");
                    String sourceName = Controller.GenerateName(sourceX, sourceY);

                    for(int ii = sourceX; ii <= targetX; ii++) {
                        for(int jj = sourceY; jj <= targetY; jj++) {
                            if(ii == sourceX && jj == sourceY) continue;

                            String name = Controller.GenerateName(ii, jj);
                            String input =  env.copyInput(sourceName, name, ii - sourceX, jj - sourceY);
                            List<String> changed = env.evaluate(name, input);
                            updateFromList(changed);
                        }
                    }

                    event.setDropCompleted(true);
                    event.consume();
                });

                grid.add(cell, j, i);
            }
        }

        File f = getFilePath();
        if(f != null) {
            openFile(f);
            setFilePath(f);
        }
    }

    private void updateFromList (List<String> l) {
        for (String cell : l) {
            Label lbl = (Label) cells.get(cell).getChildren().get(0);
            lbl.setText(env.getValue(cell).toString());
        }
    }

    private void resetClassAtPos(HBox box, int x, int y) {
        box.getStyleClass().clear();
        box.getStyleClass().add("cell");
        if(x == 0) {
            box.getStyleClass().add("headerV");
            if(y == 1) box.getStyleClass().add("headerVF");
        }
        if(y == 0) {
            box.getStyleClass().add("headerH");
            if(x == 1) box.getStyleClass().add("headerHF");
        }
    }

    private void openFile (File f) {
        if(f == null) return;
        clearAll();
        List<String> changed = env.loadAll(f);
        updateFromList(changed);
        if(active != null) {
            int activeX = (int) active.getProperties().get("cellX");
            int activeY = (int) active.getProperties().get("cellY");
            input.setText(env.getInput(GenerateName(activeX, activeY)));
        }
    }

    private void clearAll () {
        input.clear();
        env.clearAll();
        for (Map.Entry<String, HBox> entry : cells.entrySet()) {
            Label lbl = (Label) entry.getValue().getChildren().get(0);
            lbl.setText("");
        }
    }

    @FXML
    private void handleSubmit (ActionEvent event) {
        if (active == null) return;

        int x = (int) active.getProperties().get("cellX");
        int y = (int) active.getProperties().get("cellY");
        String name = GenerateName(x, y);

        List<String> changed = env.evaluate(name, input.getText());
        updateFromList(changed);
    }

    @FXML
    private void handleNew () {
        setFilePath(null);
        clearAll();
    }

    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "SER files (*.ser)", "*.ser");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(app.primaryStage);

        if (file != null) {
            setFilePath(file);
            openFile(file);
        }
    }

    @FXML
    private void handleSave () {
        if(filePath != null)
            env.saveAll(filePath);
        else
            handleSaveAs();
    }

    @FXML
    private void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "SER files (*.ser)", "*.ser");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(app.primaryStage);

        if (file != null) {
            if (!file.getPath().endsWith(".ser")) {
                file = new File(file.getPath() + ".ser");
            }
            setFilePath(file);
            env.saveAll(file);
        }
    }

    @FXML
    private void handleClose () {
        System.exit(0);
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Calculator");
        alert.setHeaderText("About");
        alert.setContentText("by Jakub Zadrozny @ II UWr");

        alert.showAndWait();
    }

    private File getFilePath () {
        Preferences prefs = Preferences.userNodeForPackage(Controller.class);
        String filePath = prefs.get("filePath", null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }

    private void setFilePath (File f) {
        Preferences prefs = Preferences.userNodeForPackage(Controller.class);
        if (f != null) {
            prefs.put("filePath", f.getPath());
            if(app != null)
                app.primaryStage.setTitle("Calculator - " + f.getName());
        } else {
            prefs.remove("filePath");
            if(app != null)
                app.primaryStage.setTitle("Calculator");
        }
        filePath = f;
    }

    public static String GenerateName(int x, int y) {
        return Character.toString((char) (x + 64)) + String.valueOf(y);
    }

}
