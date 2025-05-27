# DaisyWorld Simulation

## Overview

This project simulates **DaisyWorld**, an ecological model demonstrating how living organisms (black and white daisies) regulate planetary temperature through reflectivity (albedo). The simulation runs on a **30x30 grid**, includes a **Java Swing GUI**, and logs data to a **CSV file** for further analysis.

---

## Features

- Black and white daisies grow, reproduce, and die based on temperature and soil pollution.
- Adjustable parameters: **solar luminosity**, **daisy percentages**, and **albedo values**.
- Real-time GUI updates show daisies and pollution on the grid.
- Data output to `simulation_output.csv`.

---

## Class Overview

- **DaisySimulationGUI**  
  Handles the main simulation logic and GUI controls.

- **Daisy**  
  Represents a single daisy, including its age, color, and albedo.

- **Patch**  
  Represents a grid cell with temperature, pollution, and showing whether a daisy living on it.

- **FileService**  
  Writes simulation results to a CSV file.

---

## Prerequisites

- Java 8 or higher
- Any IDE (e.g., IntelliJ, Eclipse) or terminal

---

## How to Run

### Run the Main Function
- Execute the main function in DaisySimulationGUI.java

### Interact with the GUI
- Setup: Adjust sliders for daisy percentages (0–50%) and albedo (0.0–1.0), select luminosity, and click "Setup Simulation."
- Start: Click "Start Simulation" to run the simulation. The grid updates to show daisies and pollution.
- Stop: Click "Stop Simulation" to pause.
- Temperature: Global temperature is displayed at the bottom.

## Outputs

### CSV File
- Location: simulation_output.csv in the project directory. 
- Content: Logs step number, global temperature, daisy counts, luminosity, and albedo values. 
- Usage: Analyze with tools like Excel or Python (e.g., pandas).

