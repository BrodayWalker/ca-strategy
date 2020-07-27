package com.brodaywalker.ca_strategy;

import java.io.IOException;
import java.util.Scanner;

class Main {
    public static void main(String args[]){
        // 1. Show the menu
        // 2. Run the model
        // 3. Check results in output.csv
        try {
            Model testModel = displayMenu();
            testModel.defaultRun();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        System.out.println("Model run complete. Check output.csv in the root folder for results.");
    }

    // TODO: separate the menu display from Model and Strategy object creation
    public static Model displayMenu() throws IOException {
        String answer;
        int popSize, initInfected, initDaysLatent, initDaysInfectious, selectedStrategy; 
        double initInfectChance;
        boolean pad = true; // Don't give this option yet; further testing is needed.
        Model customModel = null;
        Scanner scanner = new Scanner(System.in);

        do {
            // Prompt user to select model
            System.out.println("Cellular Automaton Menu\n"
                + "Would you like to set the model parameters?\n"
                + "The default model will be run otherwise. (Y/n)");

            // Read input
            answer = scanner.next();
            answer = answer.toLowerCase();

            if(answer.equals("y")) {
                // Set population size
                System.out.println("Please set the parameters for a custom model below.\n"
                    + "Population size (integer): ");
                popSize = scanner.nextInt();

                // Set count initially infected
                System.out.println("Initially infected (integer): ");
                initInfected = scanner.nextInt();

                // Set number of days latent
                System.out.println("Days latent (integer): ");
                initDaysLatent = scanner.nextInt();

                // Set number of days infectious
                System.out.println("Days infectious (integer): ");
                initDaysInfectious = scanner.nextInt();

                // Set chance to infect other cells
                System.out.println("Chance to infect other cells (float/double): ");
                initInfectChance = scanner.nextDouble();

                // Select a strategy
                System.out.println("Please select a strategy using an integer number:\n"
                    + "1. Moore neighborhood\n"
                    + "2. von Neumann neighborhood");
                selectedStrategy = scanner.nextInt();

                // Create the model
                // TODO: make a factory method for this
                switch (selectedStrategy) {
                    case 1:
                        customModel = new Model(popSize, initInfected, initDaysLatent, initDaysInfectious, initInfectChance, pad, new Moore());
                        break;
                    case 2:
                        customModel = new Model(popSize, initInfected, initDaysLatent, initDaysInfectious, initInfectChance, pad, new VonNeumann());
                }
            }
            else if (answer.equals("n")) {
                // Create the defaul model using the default strategy
                customModel = new Model();
            }
        } while (!answer.equals("y") && !answer.equals("n"));
        
        return customModel;
    }
}