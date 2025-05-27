import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DaisySimulationGUI {
    static int NUM_STEPS = 0;
    public static final String CSV_FILE_PATH = "simulation_output.csv"; // Path for the CSV output file

    // Constants (static ones)
    public static final int MAX_AGE = 25;  // Maximum age of daisies
    public static final int ROWS = 30;      // Number of rows in the patch grid
    public static final int COLS = 30;      // Number of columns in the patch grid
    public static double ALBEDO_OF_BLACKS = 0.25;
    public static double ALBEDO_OF_WHITES = 0.75;
    private static int ALBEDO_OF_BLACKS_100 = 25; // jus for slider use
    private static int ALBEDO_OF_WHITES_100 = 75; // jus for slider use
    public static double ALBEDO_OF_SURFACE = 0.4;
    public static int PERCENTAGE_OF_BLACKS = 20;
    public static int PERCENTAGE_OF_WHITE = 20;
    public static final double DIFFUSE_FACTOR = 0.5;
    public static int whiteDaisies = 0;
    public static int blackDaisies = 0;
    public static Patch[][] patches;  // 2D array of patches

    // Non-static ones
    private double globalTemperature = 0;
    private double solarLuminosity = 0.6;
    private List<Daisy> daisies;
    private ExecutorService executor; // Executor service for concurrent execution
    private boolean startButtonEnabled = false;
    private boolean stopButtonEnabled = false;

    private volatile boolean running; // Used to control the simulation loop
    private FileService fileService = new FileService();

    // GUI Components
    private JFrame frame;
    private JButton[][] buttons; // Buttons representing the grid
    private JLabel temperatureLabel; // Label to display the global temperature
    private JComboBox<String> luminosityComboBox;
    private JSlider blackDaisySlider; // Slider for black daisies
    private JSlider whiteDaisySlider; // Slider for white daisies
    private JLabel blackDaisyLabel; // Label for black daisy percentage
    private JLabel whiteDaisyLabel; // Label for white daisy percentage
    private JSlider blackDaisyAlbedoSlider; // Slider for black daisies
    private JSlider whiteDaisyAlbedoSlider; // Slider for white daisies
    private JLabel blackDaisyAlbedoLabel; // Label for black daisy albedo value
    private JLabel whiteDaisyAlbedoLabel; // Label for white daisy albedo value
    private JButton startButton;
    private JButton stopButton;
    private JButton setupButton;

    public DaisySimulationGUI() {
        initialize();
    }

    public void initialize() {
        if (daisies != null)
            daisies.clear();

        whiteDaisies = 0;
        blackDaisies = 0;
        NUM_STEPS = 0;

        this.daisies = new ArrayList<>();
        patches = new Patch[ROWS][COLS];  // Initialize the patch grid
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                patches[row][col] = new Patch(row, col);
                if (Math.random() < 0.30) {
                    patches[row][col].setSoilPollution(0.4 + Math.random() * 0.6); // 0.4~1.0
                }
            }
        }
        executor = Executors.newFixedThreadPool(ROWS * COLS); // Create a thread pool
        createAndShowGUI(); // Create the GUI
    }

    // Setup initial conditions for the simulation
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

        seedBlacksRandomly();
        seedWhitesRandomly();
        calculateTemperature();
        System.out.println("Initial Global Temperature: " + globalTemperature);
        fileService.writeCSVHeader();
        fileService.writeToCSV(0, globalTemperature,blackDaisies,whiteDaisies,
                solarLuminosity,ALBEDO_OF_BLACKS,ALBEDO_OF_WHITES,ALBEDO_OF_SURFACE);  // Write the initial state to CSV
    }

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

    private void calculateTemperature() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                patches[row][col].calculateTemperature(solarLuminosity);
            }
        }
        updateGlobalTemperature();
    }

    private void updateGlobalTemperature() {
        double sum = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                sum += patches[row][col].getTemperature();
            }
        }
        globalTemperature = sum / (ROWS * COLS);
        temperatureLabel.setText(String.format("Global Temperature: %.2f", globalTemperature)); // Update the label
    }

    public void simulateStep() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                patches[row][col].calculateTemperature(solarLuminosity);
            }
        }

        // Concurrent Temp Diffusion
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final int r = row;
                final int c = col;
                executor.submit(() -> patches[r][c].diffuse());
            }
        }

        // Concurrent Breeding
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final int r = row;
                final int c = col;
                executor.submit(() -> {
                    if (Math.random() < 0.3 && NUM_STEPS % 20 == 0) {
                        double pollutionValue = 0.4 + Math.random() * 0.6;
                        if (pollutionValue > patches[r][c].getSoilPollution()) {
                            patches[r][c].setSoilPollution(pollutionValue); // 0.4~1.0
                        }
                    }
                    if (patches[r][c].hasDaisy()) {
                        // Cleaning the patch per step
                        // comment the line to disable it
//                        patches[r][c].setSoilPollution(patches[r][c].getSoilPollution() - 0.005);
                        patches[r][c].getDaisy().checkSurvivability();
                    }
                    else {
                        // patches self-cleaning
                        patches[r][c].setSoilPollution(patches[r][c].getSoilPollution() - 0.0001);
                    }


                });
            }
        }

        updateGlobalTemperature();
        System.out.printf("Global Temperature after step: %.2f%n", globalTemperature);
        fileService.writeToCSV(NUM_STEPS, globalTemperature,blackDaisies,whiteDaisies,
                solarLuminosity,ALBEDO_OF_BLACKS,ALBEDO_OF_WHITES,ALBEDO_OF_SURFACE); // Log step and temperature to CSV
        updateGridDisplay(); // Update GUI representation after each step
        NUM_STEPS++;
    }



    private void createAndShowGUI() {
        frame = new JFrame("Daisy Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create panel for the grid
        JPanel gridPanel = new JPanel(new GridLayout(ROWS, COLS));
        buttons = new JButton[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col] = new JButton(); // Initialize buttons for the grid
                buttons[row][col].setPreferredSize(new Dimension(40, 40));
                buttons[row][col].setEnabled(false);
                gridPanel.add(buttons[row][col]); // Add button to the grid panel
            }
        }

        frame.add(gridPanel, BorderLayout.CENTER);

        // Create label to display global temperature
        temperatureLabel = new JLabel("Global Temperature: 0.00", SwingConstants.CENTER);
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(temperatureLabel, BorderLayout.SOUTH);

        // Luminosity Selection Panel
        JPanel luminosityPanel = new JPanel();
        luminosityPanel.setLayout(new BoxLayout(luminosityPanel, BoxLayout.Y_AXIS));
        String[] luminosityOptions = {"Low Luminosity ", "Our Luminosity ", "High Luminosity "};
        luminosityComboBox = new JComboBox<>(luminosityOptions); // Initialize ComboBox
        luminosityPanel.add(new JLabel("Select Solar Luminosity:"));
        luminosityPanel.add(luminosityComboBox); // Add ComboBox to panel
        luminosityComboBox.setSelectedIndex(0);

        // Black Daisy Slider
        blackDaisySlider = new JSlider(0, 50, PERCENTAGE_OF_BLACKS);
        blackDaisySlider.setMajorTickSpacing(10);
        blackDaisySlider.setPaintTicks(true);
        blackDaisySlider.setPaintLabels(true);
        blackDaisyLabel = new JLabel("Black Daisy Percentage: " + PERCENTAGE_OF_BLACKS + "%");
        blackDaisySlider.addChangeListener(e -> blackDaisyLabel.setText("Black Daisy Percentage: " + blackDaisySlider.getValue() + "%"));

        // White Daisy Slider
        whiteDaisySlider = new JSlider(0, 50, PERCENTAGE_OF_WHITE);
        whiteDaisySlider.setMajorTickSpacing(10);
        whiteDaisySlider.setPaintTicks(true);
        whiteDaisySlider.setPaintLabels(true);
        whiteDaisyLabel = new JLabel("White Daisy Percentage: " + PERCENTAGE_OF_WHITE + "%");
        whiteDaisySlider.addChangeListener(e -> whiteDaisyLabel.setText("White Daisy Percentage: " + whiteDaisySlider.getValue() + "%"));

        luminosityPanel.add(blackDaisyLabel);
        luminosityPanel.add(blackDaisySlider);
        luminosityPanel.add(whiteDaisyLabel);
        luminosityPanel.add(whiteDaisySlider);

        // Black Daisy Albedo Slider
        blackDaisyAlbedoSlider = new JSlider(0, 100, ALBEDO_OF_BLACKS_100);
        blackDaisyAlbedoSlider.setMajorTickSpacing(10);
        blackDaisyAlbedoSlider.setPaintTicks(true);
        blackDaisyAlbedoSlider.setPaintLabels(true);
        blackDaisyAlbedoLabel = new JLabel("Black Daisy Albedo: " + ALBEDO_OF_BLACKS);
        blackDaisyAlbedoSlider.addChangeListener(e -> blackDaisyAlbedoLabel.setText("Black Daisy Albedo: " + getDoubleFromSlider(blackDaisyAlbedoSlider.getValue())));

        // White Daisy Albedo Slider
        whiteDaisyAlbedoSlider = new JSlider(0, 100, ALBEDO_OF_WHITES_100);
        whiteDaisyAlbedoSlider.setMajorTickSpacing(10);
        whiteDaisyAlbedoSlider.setPaintTicks(true);
        whiteDaisyAlbedoSlider.setPaintLabels(true);
        whiteDaisyAlbedoLabel = new JLabel("White Daisy Albedo: " + ALBEDO_OF_WHITES);
        whiteDaisyAlbedoSlider.addChangeListener(e -> whiteDaisyAlbedoLabel.setText("White Daisy Albedo: " + getDoubleFromSlider(whiteDaisyAlbedoSlider.getValue())));

        luminosityPanel.add(blackDaisyAlbedoLabel);
        luminosityPanel.add(blackDaisyAlbedoSlider);
        luminosityPanel.add(whiteDaisyAlbedoLabel);
        luminosityPanel.add(whiteDaisyAlbedoSlider);

        // Setup Button
        setupButton = new JButton("Setup Simulation");
        setupButton.setPreferredSize(new Dimension(100, 150));
        setupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // For interface updates only
                startButtonEnabled = true;
                stopButtonEnabled = false;
                PERCENTAGE_OF_BLACKS = blackDaisySlider.getValue();
                PERCENTAGE_OF_WHITE = whiteDaisySlider.getValue();
                ALBEDO_OF_BLACKS_100 = blackDaisyAlbedoSlider.getValue();
                ALBEDO_OF_BLACKS = getDoubleFromSlider(blackDaisyAlbedoSlider.getValue());
                ALBEDO_OF_WHITES_100 = whiteDaisyAlbedoSlider.getValue();
                ALBEDO_OF_WHITES = getDoubleFromSlider(whiteDaisyAlbedoSlider.getValue());

                int option = luminosityComboBox.getSelectedIndex();
                daisies.clear();
                frame.dispose();
                initialize();
                setup(option); // Call setup method when the button is pressed
                updateGridDisplay(); // Refresh the grid display after setup
            }
        });

        startButton = new JButton("Start Simulation");
        startButton.setPreferredSize(new Dimension(100, 150));
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                setupButton.setEnabled(false);

                PERCENTAGE_OF_BLACKS = blackDaisySlider.getValue();
                PERCENTAGE_OF_WHITE = whiteDaisySlider.getValue();
                ALBEDO_OF_BLACKS = getDoubleFromSlider(blackDaisyAlbedoSlider.getValue());
                ALBEDO_OF_WHITES = getDoubleFromSlider(whiteDaisyAlbedoSlider.getValue());

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
                running = true; // Start the simulation
                new Thread(() -> {
                    while (running) {
                        simulateStep(); // Perform simulation
                        try {
                            Thread.sleep(100); // Pause for milliseconds between steps
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break; // Exit loop if interrupted
                        }
                    }
                }).start();
            }
        });

        // Stop Button
        stopButton = new JButton("Stop Simulation");
        stopButton.setPreferredSize(new Dimension(100, 150));
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                running = false; // Stop the simulation
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                setupButton.setEnabled(true);
                System.out.println("Simulation stopped.");
            }
        });
        stopButton.setEnabled(stopButtonEnabled);

        JPanel ControlPanel = new JPanel();
        ControlPanel.setLayout(new BoxLayout(ControlPanel, BoxLayout.Y_AXIS));
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(setupButton);
        ControlPanel.add(luminosityPanel, BorderLayout.SOUTH);
        startButton.setEnabled(startButtonEnabled);
        frame.add(ControlPanel, BorderLayout.WEST);

        // Setup frame properties and display
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateGridDisplay() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (patches[row][col].hasDaisy()) {
                    Daisy d = patches[row][col].getDaisy();
                    buttons[row][col].setText(d.getColor() == 1 ? "B" : "W"); // Show initial of the color
                    buttons[row][col].setBackground(d.getColor() == 1 ? Color.BLACK : Color.WHITE);
                } else {
                    buttons[row][col].setText(""); // Clear text if no daisy is present
                    double pollution = patches[row][col].getSoilPollution();
                    int gray = (int)(200 - pollution * 150); // 污染越高越深
                    buttons[row][col].setBackground(new Color(gray,gray,gray)); // Set a light gray background for empty patches
                }
            }
        }
    }

    // Convert slider integer value (0-100) to double value (0.0-1.0)
    private double getDoubleFromSlider(int sliderValue) {
        return 0.0 + (sliderValue / 100.0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DaisySimulationGUI::new); // Start the GUI on the Event Dispatch Thread
    }
}