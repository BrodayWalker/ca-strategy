package com.brodaywalker.ca_strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.IOError;
import java.io.IOException;
import java.lang.Math;
import java.io.BufferedWriter;
import java.io.FileWriter;

class Model {
    private int padSize, dim, effectiveDim, initialInfectious, pop, daysLatent, 
                daysInfectious, countSusceptible, countLatent, countInfectious, 
                countRecovered;
    private double chanceToInfect;
    private List<List<Model.Cell>> grid, copyGrid;
    private Strategy strategy;

    class Cell {
        Phase phase; 
        int daysInPhase;

        Cell() {
            this.phase = Phase.SUSCEPTIBLE;
            this.daysInPhase = 0;
        }

        public Phase getPhase() { return this.phase; }
        public int getDaysInPhase() { return this.daysInPhase; }
        public void setPhase(Phase phase) { this.phase = phase; }
        public void setDaysInPhase(int daysInPhase) { this.daysInPhase = daysInPhase; }
        public String toString() {
            return "Phase: " + this.phase + ' ' +
                   "Days in phase: " + this.daysInPhase + '\n';
        }
    }

    Model() {
        this.padSize = 1; // specifies a 1-cell border around the usable cells
        this.dim = 10; // specifies that we want a 10 x 10 grid of usable cells
        this.pop = 100;
        // this is the dimension of the square with the border added in
        this.effectiveDim = this.dim + this.padSize + this.padSize; 
        this.initialInfectious = 1;
        this.daysLatent = 1;
        this.daysInfectious = 1;
        //
        //
        this.chanceToInfect = 1.0;
        //
        //
        // The default strategy surveys the Moore neighborhood
        this.strategy = new Moore(this.daysLatent, this.daysInfectious, this.chanceToInfect);

        buildGrid();
        setInitialInfectious();
        copyGridDeep();
        updateStatistics();
    }

    Model(int pop, int initialInfectious, int daysLatent, int daysInfectious, 
        double chanceToInfect, boolean pad, Strategy strat) {
        // Padding will probably always be used, but give the option
        // to not use it 
        if (pad) {
            this.padSize = 1;
        }
        else {
            this.padSize = 0;
        }
        
        // Grid parameters
        this.pop = pop;
        this.dim = (int)Math.sqrt(pop);
        this.effectiveDim = this.dim + this.padSize + this.padSize;

        // Population parameters
        this.initialInfectious = initialInfectious;
        this.daysLatent = daysLatent;
        this.daysInfectious = daysInfectious;
        this.chanceToInfect = chanceToInfect;

        // Strategy parameters
        this.strategy = strat;
        // This could be set in the Model arguments instead
        this.strategy.setDaysLatent(this.daysLatent);
        this.strategy.setDaysInfectious(this.daysInfectious);
        this.strategy.setChanceInfected(this.chanceToInfect);

        buildGrid();
        setInitialInfectious();
        copyGridDeep();
        updateStatistics();
    }

    // This builds both the grid and copy
    private void buildGrid() {
        // A nice resource for building 2D Lists/ArrayLists in Java
        // https://stackoverflow.com/questions/16956720/how-to-create-an-2d-arraylist-in-java

        // Build the first dimension
        // Even though we give an initial size in the constructor, the elements are empty
        this.grid = new ArrayList<List<Model.Cell>>(this.effectiveDim);
        this.copyGrid = new ArrayList<List<Model.Cell>>(this.effectiveDim);

        // For this reason, we must traverse the first dimension and put lists in each element
        // And, of course, those lists are of size this.effectiveDim, 
        // but the elements are also empty
        for(int i = 0; i < this.effectiveDim; i++) {
            this.grid.add(new ArrayList<Model.Cell>(this.effectiveDim));
            this.copyGrid.add(new ArrayList<Model.Cell>(this.effectiveDim));
        }

        // Fill each ArrayList with default Cell objects
        for(int i = 0; i < this.effectiveDim; i++) {
            for(int j = 0; j < this.effectiveDim; j++) {
                this.grid.get(i).add(new Model.Cell());
                this.copyGrid.get(i).add(new Model.Cell());
            }
        }
    }

    private void setInitialInfectious() {
        Random rand = new Random(); // random number generator
        int j, k; // used for random index

        // Make sure the initial count of infectious cells does not exceed
        // the size of the grid
        if (this.initialInfectious <= this.pop) {
            for(int i = 0; i < this.initialInfectious; i++) {
                // Choose a random cell object in the grid
                // If this cell is already infectious, continue to roll random
                // numbers until a 
                do {
                    j = rand.nextInt(this.effectiveDim - this.padSize - this.padSize) + 1;
                    k = rand.nextInt(this.effectiveDim - this.padSize - this.padSize) + 1;
    
                } while(this.grid.get(j).get(k).getPhase() != Phase.SUSCEPTIBLE);
                
                // Set the cell to infectious
                this.grid.get(j).get(k).setPhase(Phase.INFECTIOUS);
            }
        }
        // If the user input for the initial count of infectious cells exceeds
        // the size of the grid, set the default amount of infectious cells,
        // which is 1
        else {
            j = rand.nextInt(this.effectiveDim - this.padSize - this.padSize) + 1;
            k = rand.nextInt(this.effectiveDim - this.padSize - this.padSize) + 1;

            // Set the cell to infectious
            this.grid.get(j).get(k).setPhase(Phase.INFECTIOUS);
        }
    }

    // Using the strategy design pattern, different logic can be swapped in and out easily
    // The default logic uses the Moore neighborhood, but the Model object may be instantiated
    // with a von Neumann strategy if selected by the user. Either way, the code doesn't care
    // because every strategy implements the Strategy interface, so the doLogic method in the
    // appropriate concrete class is executed.
    private void performLogic() {
        this.strategy.doLogic(this.padSize, this.dim, this.effectiveDim, this.grid, this.copyGrid);
    }

    // Copy the original board 
    private void copyGridDeep() {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                this.copyGrid.get(i).get(j).setPhase(this.grid.get(i).get(j).phase);
                this.copyGrid.get(i).get(j).setDaysInPhase(this.grid.get(i).get(j).daysInPhase);
            }
        }
    }

    private void updateStatistics() {
        // Create an array that is the same size as the enumerated list in Phase
        // For this model, the counts array is 4 elements, each corresponding to a phase
        // in the SLIR model
        int [] counts= new int[Phase.values().length];

        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                // Does this look long? It feels long and it looks intimidating, but
                // it's really just logic stolen from a counting sort so we can avoid
                // doing any comparisons.
                // Each phase in SLIR is enumerated, so the ordinal() method can be used,
                // which gives us the phase as an integer number, allowing us to increment 
                // the appropriate counter element in the counts array. The susceptible 
                // phase corresponds to counts[0], so if a cell object is in the susceptible 
                // phase, the command inside the square brackets evaluates to 0, therefore 
                // incrementing counts[0] by one. Thank you, enum class.
                // Even better, the order of the elements in the Phase enum class can change
                // and it will not affect the logic in the command below.
                counts[this.grid.get(i).get(j).phase.ordinal()]++;
            }
        }

        this.countSusceptible = counts[Phase.SUSCEPTIBLE.ordinal()];
        this.countLatent = counts[Phase.LATENT.ordinal()];
        this.countInfectious = counts[Phase.INFECTIOUS.ordinal()];
        this.countRecovered = counts[Phase.RECOVERED.ordinal()];
    }

    private void writeStatistics(BufferedWriter writer) throws IOException {
        writer.append(this.countSusceptible + ", " + this.countLatent + ", " + 
                      this.countInfectious + ", " + this.countRecovered + '\n');
    }

    // Print the grid
    public void printGrid(List<List<Model.Cell>> grid) {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                // This is ugly because Java does not support operator overloading,
                // therefore I cannot subscript my Frankenstein's monster list.
                // This can be thought of as grid[i][j].toString()
                System.out.print(grid.get(i).get(j).toString());
            }
            System.out.print('\n');
        }
    }

    public void printPhase() {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                System.out.print(this.grid.get(i).get(j).phase.ordinal() + " ");
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    public void printDaysInPhase() {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                System.out.print(this.grid.get(i).get(j).daysInPhase + " ");
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    // Performs game logic and changing of cells from phase to phase
    public void simulateDay() {
        // while countLatent != 0 and countInfectious != 0
        // clone the board
        copyGridDeep();
        // do the logic
        performLogic();
        // update statistics
        updateStatistics();
    }

    // Run the model without printing any diagnostic/debug information
    public void defaultRun() throws IOException {
        // Create a buffered writer to write statistics to
        BufferedWriter writer = new BufferedWriter(new FileWriter("output.csv"));
        writeStatistics(writer);

        while(this.countLatent > 0 || this.countInfectious > 0) {
            simulateDay();
            writeStatistics(writer);
        }

        writer.close();
    }

    // Run the model, printing the grid for each day
    public void debugRun() throws IOException {
        int countDays = 0;
        // Create a buffered writer to write statistics to
        BufferedWriter writer = new BufferedWriter(new FileWriter("output.csv"));

        System.out.println("DAY " + countDays);

        writeStatistics(writer);
        printPhase();

        while(this.countLatent > 0 || this.countInfectious > 0) {
            simulateDay();
            writeStatistics(writer);

            System.out.println("DAY " + ++countDays);
            printPhase();
        }

        writer.close();
    }
}

