package com.brodaywalker.ca_strategy;

import java.util.List;
import java.util.Random;

class VonNeumann implements Strategy {
    private int daysLatent, daysInfectious;
    private double chanceInfected;

    VonNeumann() {
        this.daysLatent = 1;
        this.daysInfectious = 1;
        this.chanceInfected = 0.3;
    }

    VonNeumann(int daysLatent, int daysInfectious, double chanceInfected) {
        this.daysLatent = daysLatent;
        this.daysInfectious = daysInfectious;
        this.chanceInfected = chanceInfected;
    }

    // TODO: Split into individual methods for processing each phase. 
    public void doLogic(int padSize, int dim, int effectiveDim, 
        List<List<Model.Cell>> grid, List<List<Model.Cell>> copyGrid) {

        // Traverse the copyGrid
        // The copyGrid is not changed during the traversal. All changes
        // are made to the original grid. 
        for(int i = padSize; i < effectiveDim - padSize; i++) {
            for(int j = padSize; j < effectiveDim - padSize; j++) {
                // Get this cell's phase
                Phase currentPhase = copyGrid.get(i).get(j).phase;
                
                // Cells that are susceptible survey their Moore neighborhood
                // and roll random floating point numbers, potentially changing
                // to the latent phase if the roll succeeds
                if (currentPhase == Phase.SUSCEPTIBLE) {
                    Random rand = new Random();
                    double random;
                    int localInfectious = 0;

                    // First, count the number of infectious in the neighborhood
                    // surrounding this cell. This cell is the center of a cross (4 neighbors)
                    // There isn't a very clean way to check the cross neighborhood

                    // top cell
                    if(copyGrid.get(i).get(j - 1).phase == Phase.INFECTIOUS) {
                        localInfectious++;
                    }

                    // left cell
                    if(copyGrid.get(i - 1).get(j).phase == Phase.INFECTIOUS) {
                        localInfectious++;
                    }

                    // right cell
                    if(copyGrid.get(i + 1).get(j).phase == Phase.INFECTIOUS) {
                        localInfectious++;
                    }

                    // bottom cell
                    if(copyGrid.get(i).get(j + 1).phase == Phase.INFECTIOUS) {
                        localInfectious++;
                    }

                    // Roll countInfectious random numbers, comparing to the chanceInfected
                    // variable, which is a double representing the likelihood a cell
                    // will contract the disease and become latent. If chanceInfected is 0.3,
                    // there is a 30% chance of turning. 
                    for(int z = 0; z < localInfectious; z++) {
                        random = rand.nextDouble();

                        if(random < this.chanceInfected) {
                            // Change the cell to become latent
                            // Make sure the change is reflected in the original grid and
                            // not the copyGrid or the logic used to process other cells
                            // will be affected 
                            grid.get(i).get(j).setPhase(Phase.LATENT);
                            grid.get(i).get(j).setDaysInPhase(0);
                        }
                    }
                }
                else if (currentPhase == Phase.LATENT) {
                    // If the cell has finished it's latent period, move to
                    // the infectious phase
                    if (copyGrid.get(i).get(j).daysInPhase >= this.daysLatent) {
                        grid.get(i).get(j).setPhase(Phase.INFECTIOUS);
                        grid.get(i).get(j).setDaysInPhase(0);
                    }
                    else {
                        grid.get(i).get(j).setDaysInPhase(++grid.get(i).get(j).daysInPhase);
                    }
                }
                else if (currentPhase == Phase.INFECTIOUS) {
                    // If the cell has finished the infectious period, move
                    // to the recovered phase
                    if (copyGrid.get(i).get(j).daysInPhase >= this.daysInfectious) {
                        grid.get(i).get(j).setPhase(Phase.RECOVERED);
                        grid.get(i).get(j).setDaysInPhase(0);
                    }
                    else {
                        grid.get(i).get(j).setDaysInPhase(++grid.get(i).get(j).daysInPhase);
                    }
                }
            }
        }
    }
    
    public void setDaysLatent(int days) { this.daysLatent = days; }
    public void setDaysInfectious(int days) { this.daysInfectious = days; }
    public void setChanceInfected(double chance) { this.chanceInfected = chance; }
}