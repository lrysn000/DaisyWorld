import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DaisySimulationGUI is the main GUI class for simulating daisyworld.
 * It manages simulation logic, GUI elements, and handles user interactions.
 */
public class DaisySimulationGUI {
    static int NUM_STEPS = 0;

    // Path for the CSV output file
    public static final String CSV_FILE_PATH = "simulation_output.csv";

    // Constants for simulation
    public static final int MAX_AGE = 25;  // Maximum age a daisy can reach
    public static final int ROWS = 30;     // Number of rows in the simulation grid
    public static final int COLS = 30;     // Number of columns in the simulation grid

    // Albedo values for black and white daisies and the bare ground
    public static double ALBEDO_OF_BLACKS = 0.25;
    public static double ALBEDO_OF_WHITES = 0.75;
    private static int ALBEDO_OF_BLACKS_100 = 25; // Albedo value for black daisies as integer (for slider use)
    private static int ALBEDO_OF_WHITES_100 = 75; // Albedo value for white daisies as integer (for slider use)
    public static double ALBEDO_OF_SURFACE = 0.4;

    // Initial percentages for seeding daisies
    public static int PERCENTAGE_OF_BLACKS = 20;
    public static int PERCENTAGE_OF_WHITE = 20;

    // Constant for temperature diffusion
    public static final double DIFFUSE_FACTOR = 0.5;

    // Daisy counters
    public static int whiteDaisies = 0;
    public static int blackDaisies = 0;

    // Grid representing the simulation environment
    public static Patch[][] patches;

    // Simulation state variables
    private double globalTemperature = 0;
    private double solarLuminosity = 0.6;
    private List<Daisy> daisies;
    private ExecutorService executor;  // Thread pool for parallel processing
    private boolean startButtonEnabled = false;
    private boolean stopButtonEnabled = false;

    private volatile boolean running;  // Control flag for the simulation loop
    private FileService fileService = new FileService();  // Service for CSV file output

    // GUI components
    private JFrame frame;
    private JButton[][] buttons;
    private JLabel temperatureLabel;
    private JComboBox<String> luminosityComboBox;
    private JSlider blackDaisySlider;
    private JSlider whiteDaisySlider;
    private JLabel blackDaisyLabel;
    private JLabel whiteDaisyLabel;
    private JSlider blackDaisyAlbedoSlider;
    private JSlider whiteDaisyAlbedoSlider;
    private JLabel blackDaisyAlbedoLabel;
    private JLabel whiteDaisyAlbedoLabel;
    private JButton startButton;
    private JButton stopButton;
    private JButton setupButton;

    /**
     * Constructor that initializes the simulation and GUI.
     */
    public DaisySimulationGUI() {
        initialize();
    }

    /**
     * Initializes the simulation grid and daisies, creates the GUI window.
     */
    public void initialize() {
        if (daisies != null)
            daisies.clear();

        whiteDaisies = 0;
        blackDaisies = 0;
        NUM_STEPS = 0;

        this.daisies = new ArrayList<>();
        patches = new Patch[ROWS][COLS];

        // Initialize patches and randomize soil pollution
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                patches[row][col] = new Patch(row, col);
                if (Math.random() < 0.30) {
                    patches[row][col].setSoilPollution(0.4 + Math.random() * 0.6); // Random pollution between 0.4~1.0
                }
            }
        }

        // Use a thread pool to handle concurrent simulation tasks
        executor = Executors.newFixedThreadPool(ROWS * COLS);

        // Create and show the GUI
        createAndShowGUI();
    }

    /**
     * Sets up the initial simulation conditions including solar luminosity and daisy seeding.
     *
     * @param lumiOption the index of the luminosity option chosen by the user.
     */
    public void setup(int lumiOption) {
        luminosityComboBox.setSelectedIndex(lumiOption);
        switch (lumiOption) {
            case 0:
                solarLuminosity = 0.6;
                break;
            case 1:
                solarLuminosity = 1.0;
                break;
            case 2:
                solarLuminosity = 1.4;
                break;
            default:
                solarLuminosity = 0.6;
                break;
        }

        seedBlacksRandomly();  // Seed black daisies
        seedWhitesRandomly();  // Seed white daisies
        calculateTemperature();  // Initial temperature calculation

        // Output initial data to CSV
        System.out.println("Initial Global Temperature: " + globalTemperature);
        fileService.writeCSVHeader();
        fileService.writeToCSV(0, globalTemperature, blackDaisies, whiteDaisies,
                solarLuminosity, ALBEDO_OF_BLACKS, ALBEDO_OF_WHITES, ALBEDO_OF_SURFACE);
    }

    /**
     * Randomly seeds black daisies across the grid based on percentage setting.
     */
    private void seedBlacksRandomly() {
        int numBlackSeeds = (ROWS * COLS * DaisySimulationGUI.PERCENTAGE_OF_BLACKS) / 100;
        Random rand = new Random();
        while (blackDaisies < numBlackSeeds) {
            int row = rand.nextInt(ROWS);
            int col = rand.nextInt(COLS);
            if (!patches[row][col].hasDaisy()) {
                Daisy d = new Daisy(1, row, col, rand.nextInt(MAX_AGE));
                daisies.add(d);
                blackDaisies++;
                patches[row][col].setDaisy(d);
            }
        }
    }

    /**
     * Randomly seeds white daisies across the grid based on percentage setting.
     */
    private void seedWhitesRandomly() {
        int numWhiteSeeds = (ROWS * COLS * DaisySimulationGUI.PERCENTAGE_OF_WHITE) / 100;
        Random rand = new Random();
        while (whiteDaisies < numWhiteSeeds) {
            int row = rand.nextInt(ROWS);
            int col = rand.nextInt(COLS);
            if (!patches[row][col].hasDaisy()) {
                Daisy d = new Daisy(0, row, col, rand.nextInt(MAX_AGE));
                daisies.add(d);
                whiteDaisies++;
                patches[row][col].setDaisy(d);
            }
        }
    }

    /**
     * Calculates temperature for each patch based on current solar luminosity.
     */
    private void calculateTemperature() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                patches[row][col].calculateTemperature(solarLuminosity);
            }
        }
        updateGlobalTemperature();
    }

    /**
     * Updates the global temperature by averaging all patch temperatures.
     */
    private void updateGlobalTemperature() {
        double sum = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                sum += patches[row][col].getTemperature();
            }
        }
        globalTemperature = sum / (ROWS * COLS);
        temperatureLabel.setText(String.format("Global Temperature: %.2f", globalTemperature));
    }

    /**
     * Executes one simulation step: update temperature, diffuse heat, and handle daisy logic.
     */
    public void simulateStep() {
        // Step 1: Calculate patch temperature
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                patches[row][col].calculateTemperature(solarLuminosity);
            }
        }

        // Step 2: Diffuse temperature concurrently
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final int r = row;
                final int c = col;
                executor.submit(() -> patches[r][c].diffuse());
            }
        }

        // Step 3: Breeding and pollution logic concurrently
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final int r = row;
                final int c = col;
                executor.submit(() -> {
                    if (Math.random() < 0.3 && NUM_STEPS % 30 == 0) {
                        double pollutionValue = 0.4 + Math.random() * 0.6;
                        if (pollutionValue > patches[r][c].getSoilPollution()) {
                            patches[r][c].setSoilPollution(pollutionValue);  // Pollute the soil
                        }
                    }
                    if (patches[r][c].hasDaisy()) {
                        // Self-cleaning patch when daisy is present
                        patches[r][c].setSoilPollution(patches[r][c].getSoilPollution() - 0.005);
                        patches[r][c].getDaisy().checkSurvivability();  // Handle daisy survival logic
                    } else {
                        // Bare patch slowly cleans itself
                        patches[r][c].setSoilPollution(patches[r][c].getSoilPollution() - 0.0008);
                    }
                });
            }
        }

        // Update global temperature and log to file
        updateGlobalTemperature();
        System.out.printf("Global Temperature after step: %.2f%n", globalTemperature);
        fileService.writeToCSV(NUM_STEPS, globalTemperature, blackDaisies, whiteDaisies,
                solarLuminosity, ALBEDO_OF_BLACKS, ALBEDO_OF_WHITES, ALBEDO_OF_SURFACE);

        updateGridDisplay();  // Refresh the GUI grid
        NUM_STEPS++;
    }


    // DaisySimulationGUI.java
// This class implements the main GUI and control logic for the Daisyworld ecological simulation.

    private void createAndShowGUI() {
        // Initializes and displays the main simulation GUI window

        frame = new JFrame("Daisy Simulation"); // Main application frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximize window

        // Create the grid panel for displaying patches
        JPanel gridPanel = new JPanel(new GridLayout(ROWS, COLS)); // Grid layout for environment
        buttons = new JButton[ROWS][COLS]; // Grid of buttons representing environment patches

        // Initialize grid buttons and add to grid panel
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col] = new JButton(); // Create button
                buttons[row][col].setPreferredSize(new Dimension(40, 40));
                buttons[row][col].setEnabled(false); // Buttons not interactive
                gridPanel.add(buttons[row][col]); // Add button to grid
            }
        }

        frame.add(gridPanel, BorderLayout.CENTER); // Add grid to center of frame

        // Global temperature display at the bottom
        temperatureLabel = new JLabel("Global Temperature: 0.00", SwingConstants.CENTER);
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(temperatureLabel, BorderLayout.SOUTH);

        // Solar luminosity selection panel
        JPanel luminosityPanel = new JPanel();
        luminosityPanel.setLayout(new BoxLayout(luminosityPanel, BoxLayout.Y_AXIS));
        String[] luminosityOptions = {"Low Luminosity ", "Our Luminosity ", "High Luminosity "};
        luminosityComboBox = new JComboBox<>(luminosityOptions); // Dropdown for solar input
        luminosityPanel.add(new JLabel("Select Solar Luminosity:"));
        luminosityPanel.add(luminosityComboBox);
        luminosityComboBox.setSelectedIndex(0); // Default selection

        // Slider to set black daisy population percentage
        blackDaisySlider = new JSlider(0, 50, PERCENTAGE_OF_BLACKS);
        blackDaisySlider.setMajorTickSpacing(10);
        blackDaisySlider.setPaintTicks(true);
        blackDaisySlider.setPaintLabels(true);
        blackDaisyLabel = new JLabel("Black Daisy Percentage: " + PERCENTAGE_OF_BLACKS + "%");

        // Update label dynamically on slider movement
        blackDaisySlider.addChangeListener(e ->
                blackDaisyLabel.setText("Black Daisy Percentage: " + blackDaisySlider.getValue() + "%"));

        // Slider to set white daisy population percentage
        whiteDaisySlider = new JSlider(0, 50, PERCENTAGE_OF_WHITE);
        whiteDaisySlider.setMajorTickSpacing(10);
        whiteDaisySlider.setPaintTicks(true);
        whiteDaisySlider.setPaintLabels(true);
        whiteDaisyLabel = new JLabel("White Daisy Percentage: " + PERCENTAGE_OF_WHITE + "%");

        whiteDaisySlider.addChangeListener(e ->
                whiteDaisyLabel.setText("White Daisy Percentage: " + whiteDaisySlider.getValue() + "%"));

        // Add daisy population sliders to panel
        luminosityPanel.add(blackDaisyLabel);
        luminosityPanel.add(blackDaisySlider);
        luminosityPanel.add(whiteDaisyLabel);
        luminosityPanel.add(whiteDaisySlider);

        // Slider for black daisy albedo setting (0.0 - 1.0 range mapped from 0-100)
        blackDaisyAlbedoSlider = new JSlider(0, 100, ALBEDO_OF_BLACKS_100);
        blackDaisyAlbedoSlider.setMajorTickSpacing(10);
        blackDaisyAlbedoSlider.setPaintTicks(true);
        blackDaisyAlbedoSlider.setPaintLabels(true);
        blackDaisyAlbedoLabel = new JLabel("Black Daisy Albedo: " + ALBEDO_OF_BLACKS);

        blackDaisyAlbedoSlider.addChangeListener(e ->
                blackDaisyAlbedoLabel.setText("Black Daisy Albedo: " + getDoubleFromSlider(blackDaisyAlbedoSlider.getValue())));

        // Slider for white daisy albedo setting
        whiteDaisyAlbedoSlider = new JSlider(0, 100, ALBEDO_OF_WHITES_100);
        whiteDaisyAlbedoSlider.setMajorTickSpacing(10);
        whiteDaisyAlbedoSlider.setPaintTicks(true);
        whiteDaisyAlbedoSlider.setPaintLabels(true);
        whiteDaisyAlbedoLabel = new JLabel("White Daisy Albedo: " + ALBEDO_OF_WHITES);

        whiteDaisyAlbedoSlider.addChangeListener(e ->
                whiteDaisyAlbedoLabel.setText("White Daisy Albedo: " + getDoubleFromSlider(whiteDaisyAlbedoSlider.getValue())));

        // Add albedo sliders to panel
        luminosityPanel.add(blackDaisyAlbedoLabel);
        luminosityPanel.add(blackDaisyAlbedoSlider);
        luminosityPanel.add(whiteDaisyAlbedoLabel);
        luminosityPanel.add(whiteDaisyAlbedoSlider);

        // Button to setup the simulation (initial placement, reset)
        setupButton = new JButton("Setup Simulation");
        setupButton.setPreferredSize(new Dimension(100, 150));
        setupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Initialize simulation state based on user input
                startButtonEnabled = true;
                stopButtonEnabled = false;

                // Read values from sliders and combo box
                PERCENTAGE_OF_BLACKS = blackDaisySlider.getValue();
                PERCENTAGE_OF_WHITE = whiteDaisySlider.getValue();
                ALBEDO_OF_BLACKS_100 = blackDaisyAlbedoSlider.getValue();
                ALBEDO_OF_BLACKS = getDoubleFromSlider(blackDaisyAlbedoSlider.getValue());
                ALBEDO_OF_WHITES_100 = whiteDaisyAlbedoSlider.getValue();
                ALBEDO_OF_WHITES = getDoubleFromSlider(whiteDaisyAlbedoSlider.getValue());

                int option = luminosityComboBox.getSelectedIndex(); // 0 = low, 1 = normal, 2 = high
                daisies.clear(); // Clear existing daisies
                frame.dispose(); // Close old frame
                initialize(); // Reinitialize simulation state
                setup(option); // Setup with new parameters
                updateGridDisplay(); // Refresh display
            }
        });

        // Button to start running the simulation in a separate thread
        startButton = new JButton("Start Simulation");
        startButton.setPreferredSize(new Dimension(100, 150));
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                setupButton.setEnabled(false);

                // Update simulation parameters
                PERCENTAGE_OF_BLACKS = blackDaisySlider.getValue();
                PERCENTAGE_OF_WHITE = whiteDaisySlider.getValue();
                ALBEDO_OF_BLACKS = getDoubleFromSlider(blackDaisyAlbedoSlider.getValue());
                ALBEDO_OF_WHITES = getDoubleFromSlider(whiteDaisyAlbedoSlider.getValue());

                // Set solar luminosity
                switch (luminosityComboBox.getSelectedIndex()) {
                    case 0:
                        solarLuminosity = 0.6;
                        break;
                    case 1:
                        solarLuminosity = 1.0;
                        break;
                    case 2:
                        solarLuminosity = 1.4;
                        break;
                    default:
                        solarLuminosity = 0.6;
                        break;
                }

                running = true; // Start simulation loop
                new Thread(() -> {
                    while (running) {
                        simulateStep(); // Perform simulation step
                        try {
                            Thread.sleep(100); // Delay between steps
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }).start();
            }
        });

        // Button to stop simulation
        stopButton = new JButton("Stop Simulation");
        stopButton.setPreferredSize(new Dimension(100, 150));
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                running = false; // Stop loop
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                setupButton.setEnabled(true);
                System.out.println("Simulation stopped.");
            }
        });
        stopButton.setEnabled(stopButtonEnabled);

        // Panel containing control buttons and sliders
        JPanel ControlPanel = new JPanel();
        ControlPanel.setLayout(new BoxLayout(ControlPanel, BoxLayout.Y_AXIS));
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(setupButton);
        ControlPanel.add(luminosityPanel, BorderLayout.SOUTH);
        startButton.setEnabled(startButtonEnabled);

        frame.add(ControlPanel, BorderLayout.WEST); // Add control panel to frame

        // Display the GUI
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Updates the display of the grid with current daisy and pollution data.
     */
    private void updateGridDisplay() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (patches[row][col].hasDaisy()) {
                    Daisy d = patches[row][col].getDaisy();
                    buttons[row][col].setText(d.getColor() == 1 ? "B" : "W"); // B for black, W for white
                    buttons[row][col].setBackground(d.getColor() == 1 ? Color.BLACK : Color.WHITE);
                } else {
                    buttons[row][col].setText(""); // Clear text
                    double pollution = patches[row][col].getSoilPollution();
                    int gray = (int) (200 - pollution * 150); // Higher pollution → darker gray
                    buttons[row][col].setBackground(new Color(gray, gray, gray)); // Background color
                }
            }
        }
    }

    /**
     * Converts a slider value (0-100) to a double in range [0.0, 1.0].
     *
     * @param sliderValue Slider value from JSlider
     * @return Mapped double value
     */
    private double getDoubleFromSlider(int sliderValue) {
        return 0.0 + (sliderValue / 100.0);
    }

    /**
     * Entry point to launch the GUI.
     * Ensures that GUI updates are performed on the Event Dispatch Thread.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(DaisySimulationGUI::new);
    }
}
