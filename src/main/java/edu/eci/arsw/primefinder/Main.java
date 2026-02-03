package edu.eci.arsw.primefinder;

public class Main {

    public static void main(String[] args) {
        Control control = Control.newControl();
        control.start();

        try {
            control.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("El hilo principal fue interrumpido.");
        }
    }
}


