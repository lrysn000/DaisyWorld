// Patch class representing each patch of temperature, soil condition, and possible daisy occupation
class Patch {

    // Direction arrays used to locate the 4 direct neighbors (up, down, left, right)
    private final int[] dirx = {-1, 1, 0, 0};
    private final int[] diry = {0, 0, -1, 1};

    // The row and column index of this patch in the grid
    private final int row;
    private final int col;

    // Current temperature of this patch
    private double temperature = 0;

    // Daisy object representing the daisy on this patch, if any
    private Daisy daisy = null;

    // Pollution level of the soil on this patch, range is limited to [0, 1]
    private double soilPollution;

    /**
     * Constructor for Patch.
     * Initializes position and sets initial soil pollution to 0.
     * @param row Row index of the patch
     * @param col Column index of the patch
     */
    public Patch(int row, int col) {
        this.row = row;
        this.col = col;
        this.soilPollution = 0.0;
    }

    /**
     * Calculates the temperature of the patch based on solar luminosity.
     * If a daisy is present, its albedo affects the absorbed luminosity.
     * Temperature is adjusted by averaging current and calculated heating.
     * @param solarLuminosity The current solar luminosity value
     */
    public void calculateTemperature(double solarLuminosity) {
        double absorbedLuminosity;

        if (!hasDaisy()) {
            // No daisy: use default surface albedo to compute absorbed light
            absorbedLuminosity = (1 - DaisySimulationGUI.ALBEDO_OF_SURFACE) * solarLuminosity;
        } else {
            // With daisy: use the daisy's albedo
            Daisy currentDaisy = daisy;
            absorbedLuminosity = (1 - currentDaisy.getAlbedo()) * solarLuminosity;
        }

        // Convert absorbed luminosity into local temperature effect
        double localHeating;
        if (absorbedLuminosity > 0) {
            localHeating = 72 * Math.log(absorbedLuminosity) + 80;
        } else {
            // Fallback if luminosity is too low or negative
            localHeating = 80;
        }

        // Smooth temperature change by averaging with existing value
        temperature = (temperature + localHeating) / 2;
    }

    /**
     * Diffuses part of this patch's temperature to its 4 direct neighbors.
     * The amount diffused is determined by DIFFUSE_FACTOR.
     * Temperature is reduced locally and distributed equally to adjacent patches.
     */
    public void diffuse() {
        double df = DaisySimulationGUI.DIFFUSE_FACTOR;
        double df_amount = temperature * df;

        // Reduce local temperature by diffusion factor
        temperature = temperature * df;

        // Distribute temperature equally to 4 neighboring patches
        for (int i = 0; i < 4; i++) {
            int newRow = row + dirx[i];
            int newCol = col + diry[i];

            // Only diffuse to valid (in-bound) neighbors
            if (newRow >= 0 && newRow < DaisySimulationGUI.ROWS &&
                    newCol >= 0 && newCol < DaisySimulationGUI.COLS) {

                DaisySimulationGUI.patches[newRow][newCol].setTemperature(
                        DaisySimulationGUI.patches[newRow][newCol].getTemperature() + (df_amount / 4));
            }
        }
    }

    // ===== Getter and Setter methods =====

    // Returns the daisy currently occupying the patch (if any)
    public Daisy getDaisy() {
        return daisy;
    }

    // Returns the current temperature of the patch
    public double getTemperature() {
        return temperature;
    }

    // Sets the temperature of the patch to a specified value
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    // Places a daisy on this patch
    public void setDaisy(Daisy daisy) {
        this.daisy = daisy;
    }

    // Checks whether the patch currently has a daisy
    public boolean hasDaisy() {
        return daisy != null;
    }

    // Returns the current soil pollution level (range 0 to 1)
    public double getSoilPollution() {
        return soilPollution;
    }

    /**
     * Sets the soil pollution level for the patch.
     * Value is clamped to stay within [0, 1].
     * @param value Pollution level to set
     */
    public void setSoilPollution(double value) {
        this.soilPollution = Math.max(0, Math.min(1, value));
    }

}
