import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The FileService class is responsible for writing simulation data to a CSV file.
 * It logs step number, global temperature, daisy populations, luminosity, and albedo values.
 * The output path is defined by DaisySimulationGUI.CSV_FILE_PATH.
 */
public class FileService {

    /**
     * Writes the header row to the CSV file.
     */
    public void writeCSVHeader() {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(DaisySimulationGUI.CSV_FILE_PATH, false))) {
            // Write the column headers, separated by commas
            writer.write("Step,GlobalTemperature,NumBlacks,NumWhites,Luminosity," +
                    "AlbedoBlack,AlbedoWhite,AlbedoSurface");
            writer.newLine(); // Move to the next line
        } catch (IOException e) {
            // Print error message if writing fails
            System.err.println("Error writing CSV header: " + e.getMessage());
        }
    }

    /**
     * Appends a line of simulation data to the CSV file.
     * Uses append mode to preserve existing content (append = true).
     *
     * @param stepNumber     The current simulation step
     * @param globalTemp     The current global temperature
     * @param num_blacks     Number of black daisies
     * @param num_whites     Number of white daisies
     * @param luminosity     Current solar luminosity
     * @param albedo_black   Albedo of black daisies
     * @param albedo_white   Albedo of white daisies
     * @param albedo_surface Average surface albedo at this step
     */
    public void writeToCSV(int stepNumber, double globalTemp, int num_blacks, int num_whites,
                           double luminosity, double albedo_black, double albedo_white,
                           double albedo_surface) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(DaisySimulationGUI.CSV_FILE_PATH, true))) {
            // Concatenate data fields into a CSV line and write it
            writer.write(stepNumber + "," + globalTemp + "," + num_blacks + "," + num_whites + "," +
                    luminosity + "," + albedo_black + "," + albedo_white + "," + albedo_surface);
            writer.newLine(); // Move to the next line
        } catch (IOException e) {
            // Print error message if writing fails
            System.err.println("Error writing to CSV: " + e.getMessage());
        }
    }

}
