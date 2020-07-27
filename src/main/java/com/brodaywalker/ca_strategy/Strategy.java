package com.brodaywalker.ca_strategy;

import java.util.List;

/**
 * Each class performing logic on a Model object's 2D grid must implement
 * Strategy. 
 */
interface Strategy {
    public void doLogic(int padSize, int dim, int effectiveDim, 
        List<List<Model.Cell>> grid, List<List<Model.Cell>> copyGrid);

    public void setDaysLatent(int days);
    
    public void setDaysInfectious(int days);

    public void setChanceInfected(double chance);
}