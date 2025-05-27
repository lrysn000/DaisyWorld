// Daisy class representing individual daisies in the simulation.
// Each daisy has a color (black or white), an age, an albedo, and a position on the grid.
class Daisy {
    private int age; // Current age of the daisy
    private final int color; // 1 for black, 0 for white
    private final double albedo; // Reflectivity based on color
    private final int row; // Row of the patch where this daisy is located
    private final int col; // Column of the patch where this daisy is located

    // Directions used for checking adjacent patches: up, down, left, right
    private final int[] dirx = {-1, 1, 0, 0};
    private final int[] diry = {0, 0, -1, 1};

    /**
     * Constructor to initialize a daisy with its color, position, and age.
     * Albedo is determined based on color using predefined constants.
     */
    public Daisy(int color, int row, int col, int age) {
        this.color = color;
        this.albedo = color == 1 ? DaisySimulationGUI.ALBEDO_OF_BLACKS : DaisySimulationGUI.ALBEDO_OF_WHITES;
        this.age = age;
        this.row = row;
        this.col = col;
    }

    /**
     * Increments the age of the daisy by one step.
     */
    public void incrementAge() {
        this.age++;
    }

    /**
     * Checks whether the daisy is still alive based on the maximum allowed age.
     * @return true if daisy is younger than MAX_AGE; false otherwise.
     */
    public boolean isAlive() {
        return age < DaisySimulationGUI.MAX_AGE;
    }

    /**
     * Checks survivability of the daisy and handles its reproduction or death.
     * - If alive, attempts to seed a new daisy in adjacent empty patch based on temperature.
     * - If dead, removes itself from the patch and updates daisy counters.
     */
    public void checkSurvivability() {
        incrementAge(); // Increment the age of the daisy

        int seedingPlacexCord = -1;
        int seedingPlaceyCord = -1;

        if (isAlive()) {
            // Calculate local temperature and seed probability threshold
            double temp = DaisySimulationGUI.patches[row][col].getTemperature();
            double seedThreshold = (0.1457 * temp) - (0.0032 * Math.pow(temp, 2)) - 0.6443;

            // Probability check for seeding based on the seedThreshold
            if (Math.random() < seedThreshold) {
                // Try to find a neighboring patch to seed into
                for (int i = 0; i < 4; i++) {
                    int newRow = row + dirx[i];
                    int newCol = col + diry[i];
                    if (newRow >= 0 && newRow < DaisySimulationGUI.ROWS && newCol >= 0 && newCol < DaisySimulationGUI.COLS) {
                        if (!DaisySimulationGUI.patches[newRow][newCol].hasDaisy()) {
                            seedingPlacexCord = newRow;
                            seedingPlaceyCord = newCol;
                            break;
                        }
                    }
                }

                // If a valid place is found, seed a new daisy of the same color
                if (seedingPlaceyCord != -1) {
                    if (color == 0 && !DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].hasDaisy()) {
                        DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].setDaisy(new Daisy(0,
                                seedingPlacexCord, seedingPlaceyCord, 0)); // Create a white daisy
                        DaisySimulationGUI.whiteDaisies++;
                    } else if (color == 1 && !DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].hasDaisy()) {
                        DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].setDaisy(new Daisy(1,
                                seedingPlacexCord, seedingPlaceyCord, 0)); // Create a black daisy
                        DaisySimulationGUI.blackDaisies++;
                    }
                }
            }
        } else {
            // Daisy has died, update counters and clear the patch
            if (color == 1) DaisySimulationGUI.blackDaisies--;
            else if (color == 0) DaisySimulationGUI.whiteDaisies--;

            DaisySimulationGUI.patches[row][col].setDaisy(null); // Remove daisy from patch
        }
    }

    // Getter for the daisy's albedo
    public double getAlbedo() {
        return albedo;
    }

    // Getter for the daisy's color (1 for black, 0 for white)
    public int getColor() {
        return color;
    }
}
