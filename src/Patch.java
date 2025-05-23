import java.util.List;

// Patch class representing each patch of temperature
class Patch {
    private final int [] dirx = {-1,1,0,0};
    private final int [] diry = {0,0,-1,1};
    private final int row;
    private final int col;
    private double temperature = 0;
    private Daisy daisy=null;

    public Patch(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public void calculateTemperature(double solarLuminosity) {
        double absorbedLuminosity;

        if (!hasDaisy()) {
            // If there are no daisies on this patch
            absorbedLuminosity = (1 - DaisySimulationGUI.ALBEDO_OF_SURFACE) * solarLuminosity; //  albedo-of-surface : 0.4
        } else {
            // If there is at least one daisy, get the albedo from the daisies here
            Daisy currentDaisy = daisy; // Assuming only one daisy is present in this patch
            absorbedLuminosity = (1 - currentDaisy.getAlbedo()) * solarLuminosity;
        }

        // Calculate local heating based on absorbed luminosity
        double localHeating;
        if (absorbedLuminosity > 0) {
            localHeating = 72 * Math.log(absorbedLuminosity) + 80;
        } else {
            localHeating = 80; // Setting a lower limit if absorbed luminosity is 0 or negative
        }

        // Set the patch temperature to the average of current temperature and local heating effect
        temperature = (temperature + localHeating) / 2;
    }

    public void diffuse()
    {
        double df=DaisySimulationGUI.DIFFUSE_FACTOR;
        double df_amount = temperature * df;
        temperature = temperature * df;
        for(int i=0;i<4;i++)
        {
            // Search in 4 dirs
            int newRow = row + dirx[i];
            int newCol = col + diry[i];
            if(newRow >= 0 && newRow < DaisySimulationGUI.ROWS && newCol >= 0 && newCol < DaisySimulationGUI.COLS)
            {
                DaisySimulationGUI.patches[newRow][newCol].setTemperature(DaisySimulationGUI.patches[newRow][newCol].getTemperature()+(df_amount/4));
            }
        }
    }

    // Getter & Setters
    public Daisy getDaisy() {
        return daisy;
    }
    public double getTemperature() {return temperature;}
    public void setTemperature(double temperature) {this.temperature = temperature;}
    public void setDaisy(Daisy daisy) {this.daisy = daisy;}
    public boolean hasDaisy() {return daisy != null;}


}