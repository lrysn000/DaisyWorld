import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileService {
    // Method to write the CSV header
    public void writeCSVHeader() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DaisySimulationGUI.CSV_FILE_PATH, false))) {
            writer.write("Step,GlobalTemperature,NumBlacks,NumWhites,Luminosity,AlbedoBlack,AlbedoWhite,AlbedoSurface");
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing CSV header: " + e.getMessage());
        }
    }

    // CSV Logging Method
    public void writeToCSV(int stepNumber, double globalTemp, int num_blacks, int num_whites,
                            double luminosity, double albedo_black, double albedo_white,double albedo_surface ) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DaisySimulationGUI.CSV_FILE_PATH, true))) {
            writer.write(stepNumber + "," + globalTemp+","+ num_blacks+","+num_whites+","+
                    luminosity+","+albedo_black+","+albedo_white+","+albedo_surface);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to CSV: " + e.getMessage());
        }
    }

}
