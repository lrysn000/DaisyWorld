// Daisy class representing individual daisies
class Daisy {
    private int age;
    private final int color; // 1 for black, 0 for white !!!!
    private final double albedo;
    private final int row; // Row of the patch where this daisy is located
    private final int col; // Column of the patch where this daisy is located
    private final int [] dirx = {-1,1,0,0};
    private final int [] diry = {0,0,-1,1};

    public Daisy(int color, int row, int col, int age) {
        this.color = color;
        this.albedo = color==1 ? DaisySimulationGUI.ALBEDO_OF_BLACKS : DaisySimulationGUI.ALBEDO_OF_WHITES;
        this.age = age;
        this.row = row;
        this.col = col;
    }

    public void incrementAge() {
        this.age++;
    }

    public boolean isAlive() {
        return age < DaisySimulationGUI.MAX_AGE;
    }

    public void checkSurvivability() {
        incrementAge(); // Increment the age of the daisy

        int seedingPlacexCord=-1;
        int seedingPlaceyCord=-1;

        if (isAlive()) {
           double temp = DaisySimulationGUI.patches[row][col].getTemperature();
           double seedThreshold = (0.1457 * temp) - (0.0032 * Math.pow(temp, 2)) - 0.6443;
            seedThreshold *= (1 - DaisySimulationGUI.patches[row][col].getSoilPollution());
            // Probability check for seeding based on the seedThreshold
            if (Math.random() < seedThreshold) {

                for(int i=0;i<4;i++)
                {
                    int newRow = row + dirx[i];
                    int newCol = col + diry[i];
                    if(newRow >= 0 && newRow < DaisySimulationGUI.ROWS && newCol >= 0 && newCol < DaisySimulationGUI.COLS)
                    {
                        if (!DaisySimulationGUI.patches[newRow][newCol].hasDaisy()) {
                            seedingPlacexCord = newRow;
                            seedingPlaceyCord = newCol;
                            break;
                        }
                    }
                }

                // If a seeding place is found, seed a new daisy
                if (seedingPlaceyCord != -1) {
                    if (color==0 && !DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].hasDaisy()) {
                        DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].setDaisy(new Daisy(0,
                                seedingPlacexCord,seedingPlaceyCord, 0)); // Create a white daisy

                        DaisySimulationGUI.whiteDaisies++;
                    } else if (color==1 && !DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].hasDaisy()) {
                        DaisySimulationGUI.patches[seedingPlacexCord][seedingPlaceyCord].setDaisy(new Daisy(1,
                                seedingPlacexCord, seedingPlaceyCord, 0)); // Create a black daisy

                        DaisySimulationGUI.blackDaisies++;
                    }
                }
            }
        } else {
            if(color==1) DaisySimulationGUI.blackDaisies--;
            else if(color==0) DaisySimulationGUI.whiteDaisies--;
            DaisySimulationGUI.patches[row][col].setSoilPollution(
                    DaisySimulationGUI.patches[row][col].getSoilPollution() + 0.1
            );
            DaisySimulationGUI.patches[row][col].setDaisy(null); // Set the patch to empty (no daisy)

        }
    }

    // Getters & Setters
    public double getAlbedo() {return albedo;}
    public int getColor() {return color;}
    // Convert slider integer value (0-100) to double value (0.0-1.0)


}
