package com.brodaywalker.ca_strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import java.lang.Math;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * <p>
 * The Model class is responsible for holding all model parameters as well as
 * the cellular automaton grid. The model grid is a two-dimensional array of
 * cell objects, which hold two pieces of information: which phase the cell
 * is in and how many days it has been in that phase.
 * </p>
 * 
 * <p>
 * All logic acting on the grid is contained in an object that implements the
 * Strategy interface. 
 * </p>
 */
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

    /**
     * The default constructor sets all parameters automatically, handles the creation
     * of the CA grid, and creates a new Moore object, which is the default strategy.
     * 
     * TODO: read initial parameters from config file
     */
    Model() {
        this.padSize = 1; // specifies a 1-cell border around the usable cells
        this.dim = 50; // specifies that we want a 50 x 50 grid of usable cells
        this.pop = 2500;
        // this is the dimension of the square with the border added in
        this.effectiveDim = this.dim + this.padSize + this.padSize; 
        this.initialInfectious = 1;
        this.daysLatent = 1;
        this.daysInfectious = 1;
        //
        //
        this.chanceToInfect = 0.3;
        //
        //
        // The default strategy surveys the Moore neighborhood
        this.strategy = new Moore(this.daysLatent, this.daysInfectious, this.chanceToInfect);

        buildGrid();
        setInitialInfectious();
        copyGridDeep();
        updateStatistics();
    }

    /**
     * This optional constructor allows all parameters to be set manually.
     * @param pop - The target population
     * @param initialInfectious - Number of cells which start as infectious
     * @param daysLatent - Number of days a cell stays in the latent phase
     * @param daysInfectious - Number of days a cell remains in the infectious phase
     * @param chanceToInfect - How likely an infectious cell is to infect a susceptible cell
     * @param pad - Surround the 2D grid with a border of susceptible cells?
     * @param strat - The logic flavor to be applied
     */
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
        // This could be set when the Strategy subtype is created in the Model arguments instead
        this.strategy.setDaysLatent(this.daysLatent);
        this.strategy.setDaysInfectious(this.daysInfectious);
        this.strategy.setChanceInfected(this.chanceToInfect);

        buildGrid();
        setInitialInfectious();
        copyGridDeep();
        updateStatistics();
    }

    /**
     * Builds two ArrayLists of Lists of Cell objects: the original and a copy for
     * use when performing logic to ensure no intermediate results affect surrounding
     * cells.
     */
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

    /**
     * Creates this.initialInfectious infected individual cells before the
     * SLIR model begins running. Cells are selected at random.
     */
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

    /**
     * Using the strategy design pattern, different logic can be swapped in and out easily
     * The default logic uses the Moore neighborhood, but the Model object may be instantiated
     * with a von Neumann strategy if selected by the user. Either way, the code doesn't care
     * because every strategy implements the Strategy interface, so the doLogic method in the
     * appropriate concrete class is executed.
     */
    private void performLogic() {
        this.strategy.doLogic(this.padSize, this.dim, this.effectiveDim, this.grid, this.copyGrid);
    }

    /**
     * Performs a deep copy from this.grid to this.copyGrid. There are only two variables to
     * copy: phase and daysInPhase.
     */ 
    private void copyGridDeep() {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                this.copyGrid.get(i).get(j).setPhase(this.grid.get(i).get(j).phase);
                this.copyGrid.get(i).get(j).setDaysInPhase(this.grid.get(i).get(j).daysInPhase);
            }
        }
    }

    /**
     * Traverses the grid counting the number of cells in each phase of the SLIR model.
     * Updates countSusceptible, countLatent, countInfectious, and countRecovered.
     */
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

    
    /** 
     * The writeStatistics method is used to write the number of cells in
     * each phase of the SLIR model to a file.
     * @param writer
     * @throws IOException
     */
    private void writeStatistics(BufferedWriter writer) throws IOException {
        writer.append(this.countSusceptible + ", " + this.countLatent + ", " + 
                      this.countInfectious + ", " + this.countRecovered + '\n');
    }

    
    /** 
     * printGrid is used to print any 2D grid of Model.Cell objects. This can be
     * used for debugging or display purposes.
     * @param grid
     */
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

    /**
     * Prints a 2D matrix of integers corresponding to each cell's phase. This
     * is primarily a debugging feature.
     */
    public void printPhase() {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                System.out.print(this.grid.get(i).get(j).phase.ordinal() + " ");
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    /**
     * Prints a 2D matrix of integers corresponding to each cell's count of days 
     * in the current phase. This is primarily a debugging feature.
     */
    public void printDaysInPhase() {
        for(int i = this.padSize; i < this.effectiveDim - this.padSize; i++) {
            for(int j = this.padSize; j < this.effectiveDim - this.padSize; j++) {
                System.out.print(this.grid.get(i).get(j).daysInPhase + " ");
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    /**
     * <p>
     * Performs game logic and changing of cells from phase to phase. This should
     * be wrapped by a method that controls the exit conditions of the simulation.
     * </p>
     * 
     * Logical flow:
     * 1. copy previous day's grid
     * 2. perform logic on the grid; this logic is fully contained in a
     * concrete class that implements Strategy
     * 3. update the object's counts for each phase
     */
    public void simulateDay() {
        // while countLatent != 0 and countInfectious != 0
        // clone the board
        copyGridDeep();
        // do the logic
        performLogic();
        // update statistics
        updateStatistics();
    }

    
    /** 
     * defaultRun() wraps the simulateDay() method. When using defaultRun to
     * run the model, each day's statistics are written to a file, but
     * the grid will not be printed. Use debugRun() to see each individual
     * day's grid.
     * @throws IOException
     */
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

    
    /** 
     * debugRun() wraps the simulateDay() method. Each day's SLIR statistics are
     * written to a file and each day's grid is printed to the console.
     * @throws IOException
     */
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

    public int getPadSize() { return this.padSize; }
    public int getDim() { return this.dim; }
    public int getEffectiveDim() { return this.dim; }
    public int getInitialInfectious() { return this.initialInfectious; }
    public int getPopulation() { return this.pop; }
    public int getDaysLatent() { return this.daysLatent; }
    public int getDaysInfectious() { return this.daysInfectious; }
    public int getCountSusceptible() { return this.countSusceptible; }
    public int getCountLatent() { return this.countLatent; }
    public int getCountInfectious() { return this.countInfectious; }
    public int getCountRecovered() { return this.countRecovered; }
}

