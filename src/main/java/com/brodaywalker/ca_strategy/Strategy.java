package com.brodaywalker.ca_strategy;

import java.util.List;

interface Strategy {
    public void doLogic(int padSize, int dim, int effectiveDim, 
        List<List<Model.Cell>> grid, List<List<Model.Cell>> copyGrid);
}