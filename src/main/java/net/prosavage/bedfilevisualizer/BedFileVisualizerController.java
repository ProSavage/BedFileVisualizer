package net.prosavage.bedfilevisualizer;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.prosavage.bedfilevisualizer.Util.Util;
import net.prosavage.bedfilevisualizer.bedfileclient.BedFileClient;
import net.prosavage.bedfilevisualizer.bedfileclient.BedFileReader;
import net.prosavage.bedfilevisualizer.bedfileclient.BedFileReaderWithIterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class BedFileVisualizerController {
    @FXML private AnchorPane anchor_pane;
    @FXML private Button open_bed_files_button, test_button;
    @FXML private TextField track_count_text_field, minimum_base_pair_overlap_text_field, overlap_count_text_field;
    @FXML private VBox plot_node;
    @FXML
    ScatterChart scatterChart;
    @FXML
    ScrollPane mary_graph;
    @FXML
    ListView<String> listView;

    private final int DEFAULT_MINIMUM_BASE_PAIR_OVERLAP_COUNT = 1000;
    private List<File> files;
    private String[] chromosomes = {"chr1","chr2","chr3","chr4","chr5","chr6","chr7","chr8","chr9","chr10",
                                    "chr11","chr12","chr13","chr14","chr15","chr16","chr17","chr18","chr19","chr20",
                                    "chr21","chr22","chrX","chrY","chrM"};
    private Color[] colors = {Color.valueOf("#1abc9c"), Color.valueOf("#2ecc71"), Color.valueOf("#3498db"), Color.valueOf("#9b59b6"), Color.valueOf("#bdc3c7"),
            Color.valueOf("#f1c40f"), Color.valueOf("#f39c12"), Color.valueOf("#d35400"), Color.valueOf("#e74c3c"), Color.MAGENTA,
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN,
            Color.AZURE, Color.BLUE, Color.INDIGO, Color.PURPLE, Color.MAGENTA,
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN};

    private Stage getStage() {
        return (Stage) anchor_pane.getScene().getWindow();
    }

    @FXML
    public void onClick() {
        mary_graph.setOpacity(0);
        mary_graph.setScaleZ(99);
        mary_graph.setDisable(true);
        scatterChart.setOpacity(1);
        scatterChart.setScaleZ(9999);
        scatterChart.setDisable(false);
        FileChooser file_chooser = new FileChooser();
        FileChooser.ExtensionFilter extension_filter = new FileChooser.ExtensionFilter("BED files (*.bed)", "*.bed");
        file_chooser.getExtensionFilters().add(extension_filter);
        File file = file_chooser.showOpenDialog(getStage());
        ArrayList<String> categories = new ArrayList<>();
        BEDCell[] bedCells = BedFileReader.ReadBedFile(file);
        HashMap<Integer, List<BEDCell>> bedCellMap = new HashMap<>();
        for (BEDCell bedCell : bedCells) {
            if (!categories.contains(bedCell.getChromosome())) {
                categories.add(bedCell.getChromosome());
            }
            switch (bedCell.getChromosome()) {
                case "chrX":
                    bedCell.setChromosome("chr23");
                    break;
                case "chrY":
                    bedCell.setChromosome("chr24");
                    break;
                case "chrM":
                    bedCell.setChromosome("chr25");
                    break;
                default:
                    break;
            }
            int chromosome = Integer.parseInt(bedCell.getChromosome().replace("chr", ""));
            if (!bedCellMap.containsKey(chromosome)) {
                bedCellMap.put(chromosome, new ArrayList<>(Arrays.asList(bedCell)));
            }
            bedCellMap.get(chromosome).add(bedCell);
        }

        ArrayList<Double> sizes = new ArrayList<>();

        for (Integer chromosome : bedCellMap.keySet()) {
            sizes.add((double) bedCellMap.get(chromosome).size());
            double interval = (double) 1 / bedCellMap.get(chromosome).size();
            int counter = 0;
            XYChart.Series series = new XYChart.Series();
            for (BEDCell bedCell : bedCellMap.get(chromosome)) {
                counter++;
                int range = bedCell.getEnd() - bedCell.getStart();
                if (range > 10000) {
                    range = (int) Math.log(range);
                }
                series.getData().add(new XYChart.Data(((double) chromosome + (counter * interval)), range));
            }

            scatterChart.getData().add(series);
        }

        double min = Collections.min(sizes);
        double max = Collections.max(sizes);
        ArrayList<Double> normalized = new ArrayList<>();
        sizes.forEach((size) -> {
                size = (size - min) / (max-min);
            normalized.add(size);
        });

        for (XYChart.Series series : (Iterable<XYChart.Series>) scatterChart.getData()) {
            for (XYChart.Data data : (Iterable<XYChart.Data>) series.getData()) {
                double chrIndex = (double) data.getXValue();

                for (int j = 0; j < 26; j++) {
                    if (chrIndex < j + 1) {
                        String colorRaw = normalized.get(j).toString();
                        data.getNode().setStyle("-fx-background-color: rgba(0, 255, 0," + colorRaw + ")");
                        break;
                    }
                }
            }
        }
 //        scatterChart.getData().forEach((series) -> series[1].setStyle("-fx-background-color: #00E5FF"));
    }



    @FXML
    private void onActionOpenBedFilesButton(ActionEvent action_event) {
        System.out.println("yooooo");
        FileChooser file_chooser = new FileChooser();
        FileChooser.ExtensionFilter extension_filter = new FileChooser.ExtensionFilter("BED files (*.bed)", "*.bed");
        file_chooser.getExtensionFilters().add(extension_filter);
        files = file_chooser.showOpenMultipleDialog(getStage());
        for (File file : files) {
            System.out.println(file.getName());
        }
        int number_of_tracks = files.size();
        track_count_text_field.setText(Integer.toString(number_of_tracks));
        overlap_count_text_field.setText(Integer.toString(number_of_tracks));
        init(); //Don't do this here plz. Run this when controller first loads
    }

    @FXML
    private void generateBeds() {
        FileChooser file_chooser = new FileChooser();
        FileChooser.ExtensionFilter extension_filter = new FileChooser.ExtensionFilter("BED files (*.bed)", "*.bed");
        file_chooser.getExtensionFilters().add(extension_filter);
        List<File> files = file_chooser.showOpenMultipleDialog(getStage());
        ArrayList<BedFileReaderWithIterator> file_readers = new ArrayList<>();
        if (files.size() == 0) {
            return;
        }
        track_count_text_field.setText(files.size() + "");
        int minimumBasePairOverLap;
        if (minimum_base_pair_overlap_text_field.getText().isEmpty()) {
            minimumBasePairOverLap = 1000;
        } else {
            minimumBasePairOverLap = Integer.parseInt(minimum_base_pair_overlap_text_field.getText());
        }
        int numOverLaps;
        if (overlap_count_text_field.getText().isEmpty()) {
            numOverLaps = files.size();
        } else {
            numOverLaps = Integer.parseInt(overlap_count_text_field.getText());
        }

        BedFileClient bedFileClient = new BedFileClient(files);
        File output = new File(files.get(0).getParent(), "output.bed");
		 bedFileClient.runIntersect(output);
        listView.getItems().add(output.getAbsolutePath());
        Util.setupContextMenu(listView);
    }



    private void makeTextFieldNumberic(TextField text_field) {
        text_field.textProperty().addListener((observable, old_value, new_value) -> {
            if (new_value.matches("\\d*") != true) {
                text_field.setText(new_value.replaceAll("[^\\d]", ""));
            }
        });
    }

    @FXML
    private void onActionTestButton(ActionEvent action_event) {
        // Okay so this part just bring mary's part to front.
        mary_graph.setOpacity(1);
        mary_graph.setScaleZ(9999);
        mary_graph.setDisable(false);
        scatterChart.setOpacity(0);
        scatterChart.setScaleZ(99);
        scatterChart.setDisable(true);
        plot_node.getChildren().clear();
        //This part will do file chooser shit.
        FileChooser file_chooser = new FileChooser();
        FileChooser.ExtensionFilter extension_filter = new FileChooser.ExtensionFilter("BED files (*.bed)", "*.bed");
        file_chooser.getExtensionFilters().add(extension_filter);
        List<File> files = file_chooser.showOpenMultipleDialog(getStage());
        ArrayList<BedFileReaderWithIterator> file_readers = new ArrayList<BedFileReaderWithIterator>();
        for (File file : files) {
            try {
                file_readers.add(new BedFileReaderWithIterator(file));
            } catch (FileNotFoundException exception){
                System.out.println("FILE NOT FOUND OH NOOOOOOO"); // Through popup or smth
                return;
            }
        }

        // This part loops the chormosomes in the actual file.
        int counter = 0;
        for (int i = 0; i < chromosomes.length - 1; i++) {
            String current_chromosome = chromosomes[i];
            System.out.println(current_chromosome);
            Color current_color = colors[i];
            int min = -1;
            int max = -1; int test = 0;
            counter = 0;
            HBox hbox = new HBox();
            VBox vbox = new VBox();
            for (BedFileReaderWithIterator file_reader : file_readers) {
                test++;
                System.out.println(test);
                ArrayList<BEDCell> chromosome_bed_cells = new ArrayList<>();
                BEDCell bed_cell;
                try {
                    while ((bed_cell = file_reader.next()) != null) {
                        if (!current_chromosome.equals(bed_cell.getChromosome())) {
                            System.out.println("End of " + current_chromosome);
                            file_readers.get(counter).holdBEDCell(bed_cell);
                            counter++;
                            break;
                        }
                        if (bed_cell.getStart() < min || min < 0) {
                            min = bed_cell.getStart();
                        }
                        if (bed_cell.getEnd() > max || min < 0) {
                            max = bed_cell.getEnd();
                        }
                        chromosome_bed_cells.add(bed_cell);
                        System.out.println(current_chromosome);
                        System.out.println(bed_cell.getStart() + " " + bed_cell.getEnd() + "  " + bed_cell.getChromosome());
                    }

                    ThePlot the_plot = new ThePlot(true);
                    Node row = the_plot.getRow(current_chromosome, file_reader.getName(), min, max, chromosome_bed_cells, current_color);
                    if (row != null) {
                        vbox.getChildren().add(row);
                    }

                } catch (IOException exception) {
                    System.out.println("IOException oh noooo"); //throw some popup
                    return;
                }
            }
            Label chromosome_label = new Label(current_chromosome);
            chromosome_label.setStyle("-fx-text-color: WHITE;");
            chromosome_label.setStyle("-fx-text-fill: white;");
            chromosome_label.setRotate(-90);
            hbox.getChildren().addAll(chromosome_label, vbox);
            plot_node.getChildren().add(hbox);
        }
    }



    private void init() {
        makeTextFieldNumberic(minimum_base_pair_overlap_text_field);
        minimum_base_pair_overlap_text_field.setText(Integer.toString(DEFAULT_MINIMUM_BASE_PAIR_OVERLAP_COUNT));
        makeTextFieldNumberic(overlap_count_text_field);
    }


}

//
// When your bed file shows intersections in every region
//
//%%%%%%%%%%**************,,,,,,,******************************************************//(((#######################((((((
//%%%%%%%%***************,,,,,,,,,*****************************************************//(((#############(((((((((((((///
//%%%%%(,****************,,,,,,,,,,****************************************************//(((###########((((((((((////////
//,,*********************,,,,,,,,,,****************************************************//(((##########(((((((/////////***
//***********************,,,,,,,,,,,***************************************************//(((##########((((((///////******
//************************,,,,,,,,,,,**************************************************//(((##########(((((///////*******
//************************,,,,,,,,,,,**************************************************//(((##########(((((///////******,
//*************************,,,,,,,,,,,*************************************************///(((#########(((((///////******,
//***********************************,*************************************************///(((#######((((((////////******,
//**************************************************************************************//(((((###((((((((/////////*****,
//**************,,,,..******************************************************************///((((((((((((((///////////*****
//***************,,,,,,.%*.//////********************************************************////(((((((((//////////////*****
//***************..,,,,.%%%%%*,////*******************************************************//////////////////////////*****
//**************,,,.,,,.%%%%%%%%%.************************************************************************/*///*********.
//**************/////,,.%%%%%%%%%%%%#.****************************************************,,,,,,,,,,,,,,,,,,,,,,,./%%%.,,
//*************///////,,%%%%%%%%%%%%%%%%.*************************************************,,,,,,,,,,,,,,,,,.,%%%%%%%%%.,,
//************/////////.,%%%%%%%%%%%%%%%%%%,**********************************************,,,,,,,,,,,,.*%%%%%%%%%%%%%#,,,
//************//////////.%%%%%%%%%%%%%%%%%%%%%.********************************************,,,,,,,.#%%%%%%%%%%%%%%%%%,,,.
//***********//////////**.%%%%%%%%%%%%%%%%%%%%%%,******************************************,,,.#%%%%%%%%%%%%%%%%%%%%%,,.,
//**********//////////****,%%%%%%%%%%%%%%%%%%%%%%%%,*************************************./%%%%%%%%%%%%%%%%%%%%%%%%%.,,,,
//**********/////////******,#%%%%%%%%%%%%%%%%%%%%%%%#,*****************,,,,,,,*******.*%%%%%%%%%%%%%%%%%%%%%%%%%%%%/.,,,,
//*********//////////*****,,,,%%%%%%%%%%%%%%%%%%%%%%%%*(#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%*.,,,,,
//********//////////*****,,,,,.#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.,,,,,,,
//***********************,,,,,,,.%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.******,,
//**********************,,,,,,,,,,*%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%,//*******,
//******,,,,,,,,,,,,,,,,,,,,,,,,,,,,/%%%%%,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.///////*****
//*****,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,*%.%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%./////////****,
//*****,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,(%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.%%%.*//////////*****,
//****,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,/%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.****////////******,
//****,,,,,,,,....,,,,,,,,,,,,,,,,,,,.%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.*****************,
//***,,,,,,..........,,,,,,,,,,,,,,,.%%%%%%%%%%%**(,,.%%%%%%%%%%%%%%%%%%%%%%%%%%%%.(,,,#%%%%%%%%%%%%%%%,****************,
//**,,,,,,...........,,,,,,,,,,,,**,%%%%%%%%%%%*%@@&,,.%%%%%%%%%%%%%%%%%%%%%%%%%%,@@@,,,#%%%%%%%%%%%%%%#,**************,,
//**,,,,,.............,,,,,,,,,****/%%%%%%%%%%%.,,,,,,.%%%%%%%%%%%%%%%%%%%%%%%%%%,,,,,,,(%%%%%%%%%%%%%%%/************,,,,
//*,,,,,..............,,,,,,,,,***.%%%%%%%%%%%%%.,,,,,(%%%%%%%%%%%%%%%%%%%%%%%%%%,,,,,,.%%%%%%%%%%%%%%%%%.,,***,,,,,,,,,,
//*,,,,,..............,,,,,,,,***,#%%%%%%%%%%%%%%%(/#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%#*#%%%%%%%%%%%%%%%%%%%/,,,,,,,,,,,,,,,
//,,,,,................,,,,,,,***,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%...,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.,,,,,,,,,,,,,,
//,,,,,...............,,,,,,,,**,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(..,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(,,,,,,,,,,,,,,
//,,,,,,,,..........,,,,,,,,****.%%(////////%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%/////////%%%%%%%%.,,,,,,,,,,,,,
//,,,,,,,,,,,,,,,,,,,,,,,,******(%///////////#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%////////////#%%%%%%%**********,,,
//,,,,,,,,,,,,,,,,,,,***********%%///////////(%%%%%%%%%%%%%%%%%%%%####%%%%%%%%%%%%%%%%%%/////////////%%%%%%%.**********,,
//,,,,**************************%%%//////////%%%%%%%%%%%%%%%%#,(########,#%%%%%%%%%%%%%%////////////%%%%%%%%#***********,
//,***********************//////#%%%%#////%%%%%%%%%%%%%%%%%%((###########,%%%%%%%%%%%%%%%%/////////%%%%%%%%%%.///*******,
//*********/////////////////////.%%%%%%%%%%%%%%%%%%%%%%%%%%%#/###########//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(/////*****,
//*******////////////////////////#%%%%%%%%%%%%%%%%%%%%%%%%%%%**##########,#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%./////*****
//******/////////////////////////*(%%%%%%%%%%%%%%%%%%%%%%%%%%%%/,/#####(,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(/////*****
//******//////////////////////////*%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%*////****,
//*******/////////////////////////**%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.///*****,
//*********//////////////////////****/%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%///******,
//************/////////////////*******#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%,*******,
//************************************/%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.******,,
//***********************************,%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%******,,,
