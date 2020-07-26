package com.brodaywalker.ca_strategy;

import java.io.IOException;

class Main {
    public static void main(String args[]){
        // TODO: Prompt user with menu
        // TODO: Selections: Moore neighborhood, von Neumann neighborhood, random

        Model testModel = new Model();

        // Run the model
        try {
            testModel.defaultRun();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void displayMenu() {

    }
}